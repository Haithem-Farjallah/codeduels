package com.codeduels.sentinel.poller;

import com.codeduels.sentinel.store.MonitoredMatchStore;
import com.codeduels.sentinel.store.WatchTarget;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Drains whichever targets are due. The Codeforces client paces the actual calls, so a backlog
 * just means targets get polled a little later than scheduled — the loop never exceeds the rate limit.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PollingLoop {

    private static final int BATCH_SIZE = 20;

    private final MonitoredMatchStore store;
    private final SubmissionPoller poller;

    @Scheduled(fixedDelayString = "${sentinel.polling.loop-delay-ms:500}")
    public void tick() {
        List<WatchTarget> due = store.dueTargets(System.currentTimeMillis(), BATCH_SIZE);
        for (WatchTarget target : due) {
            try {
                poller.poll(target);
            } catch (Exception e) {
                // never let one bad target stall the loop; push it back so it isn't retried in a tight spin
                log.error("Polling {} failed", target.encode(), e);
                store.reschedule(target, System.currentTimeMillis() + 10_000);
            }
        }
    }
}
