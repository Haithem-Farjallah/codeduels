package com.codeduels.sentinel.poller;

import com.codeduels.sentinel.codeforces.CfSubmission;
import com.codeduels.sentinel.codeforces.CodeforcesClient;
import com.codeduels.sentinel.config.SentinelProperties;
import com.codeduels.sentinel.messaging.SubmissionVerdictEvent;
import com.codeduels.sentinel.messaging.VerdictPublisher;
import com.codeduels.sentinel.store.MonitoredMatch;
import com.codeduels.sentinel.store.MonitoredMatchStore;
import com.codeduels.sentinel.store.WatchTarget;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** One poll of one handle for one duel. */
@Service
@RequiredArgsConstructor
@Slf4j
public class SubmissionPoller {

    private final MonitoredMatchStore store;
    private final CodeforcesClient codeforces;
    private final VerdictPublisher publisher;
    private final BackoffPolicy backoff;
    private final SentinelProperties properties;

    public void poll(WatchTarget target) {
        MonitoredMatch match = store.find(target.duelId()).orElse(null);
        if (match == null) {
            log.warn("No monitored match for {} — dropping target", target.encode());
            store.unwatch(target);
            return;
        }

        long now = Instant.now().getEpochSecond();
        if (now > match.getEndEpoch() + properties.getPolling().getFinalGraceSeconds()) {
            log.info("Duel {} past its grace period — done watching {}", match.getDuelId(), target.handle());
            store.unwatch(target);
            return;
        }

        long currentInterval = store.getInterval(target, properties.getPolling().getMinIntervalSeconds());
        Optional<List<CfSubmission>> fetched = codeforces.getUserStatus(target.handle());

        if (fetched.isEmpty()) {
            // Codeforces unhappy — treat as idle so we back off instead of hammering it
            scheduleNext(target, match, currentInterval, false, now);
            return;
        }

        Set<String> problemIds = new HashSet<>(match.getProblemIds());
        List<CfSubmission> relevant = fetched.get().stream()
                .filter(s -> problemIds.contains(s.problemKey()))
                .filter(s -> s.getCreationTimeSeconds() >= match.getStartEpoch()
                        && s.getCreationTimeSeconds() <= match.getEndEpoch())
                .toList();

        // Still-judging submissions aren't marked seen: they get reported once a verdict lands
        boolean stillJudging = relevant.stream().anyMatch(s -> !s.isJudged());

        List<CfSubmission> newlyJudged = relevant.stream()
                .filter(CfSubmission::isJudged)
                .filter(s -> !store.isSeen(target, s.getId()))
                .sorted(Comparator.comparingLong(CfSubmission::getCreationTimeSeconds))
                .toList();

        boolean publishFailed = false;
        for (CfSubmission submission : newlyJudged) {
            try {
                publisher.publish(SubmissionVerdictEvent.builder()
                        .duelId(match.getDuelId())
                        .handle(target.handle())
                        .problemId(submission.problemKey())
                        .submissionId(submission.getId())
                        .verdict(submission.getVerdict())
                        .submittedAtEpoch(submission.getCreationTimeSeconds())
                        .build());
                store.markSeen(target, submission.getId(), match);
                log.info("Duel {}: {} → {} on {} (#{})", match.getDuelId(), target.handle(),
                        submission.getVerdict(), submission.problemKey(), submission.getId());
            } catch (RuntimeException e) {
                log.error("Failed to publish verdict #{} for duel {}; retrying next poll",
                        submission.getId(), match.getDuelId(), e);
                publishFailed = true;
                break;
            }
        }

        // After the end, one clean poll with nothing pending is the last one we need
        if (now > match.getEndEpoch() && !stillJudging && !publishFailed) {
            log.info("Duel {} over and fully judged for {}", match.getDuelId(), target.handle());
            store.unwatch(target);
            return;
        }

        boolean activity = !newlyJudged.isEmpty() || stillJudging || publishFailed;
        scheduleNext(target, match, currentInterval, activity, now);
    }

    private void scheduleNext(WatchTarget target, MonitoredMatch match, long currentInterval,
                              boolean activity, long now) {
        long next = backoff.nextIntervalSeconds(currentInterval, activity, now, match.getEndEpoch());
        store.setInterval(target, next, match);
        store.reschedule(target, System.currentTimeMillis() + next * 1000);
        log.debug("Next poll of {} in {}s", target.encode(), next);
    }
}
