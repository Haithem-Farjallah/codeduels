package com.codeduels.sentinel.listener;

import com.codeduels.sentinel.config.SentinelProperties;
import com.codeduels.sentinel.messaging.MatchStartEvent;
import com.codeduels.sentinel.store.MonitoredMatch;
import com.codeduels.sentinel.store.MonitoredMatchStore;
import com.codeduels.sentinel.store.WatchTarget;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * A duel just went ACTIVE in the backend — start watching both handles.
 * Redelivered or retried events are upserts: the match is overwritten, already-watched handles kept.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MatchWatchQueueListener {

    private final MonitoredMatchStore store;
    private final ObjectMapper objectMapper;
    private final SentinelProperties properties;

    @SqsListener("${sentinel.sqs.match-watch-queue}")
    public void receiveMessage(String message) {
        MatchStartEvent event;
        try {
            event = objectMapper.readValue(message, MatchStartEvent.class);
        } catch (JsonProcessingException e) {
            log.error("Discarding malformed watch message: {}", message);
            return;
        }

        if (event.getDuelId() == null || event.getHandles() == null || event.getHandles().isEmpty()
                || event.getProblemIds() == null || event.getProblemIds().isEmpty()) {
            log.error("Discarding incomplete watch message: {}", message);
            return;
        }

        long now = Instant.now().getEpochSecond();
        if (event.getEndEpoch() + properties.getPolling().getFinalGraceSeconds() < now) {
            log.warn("Duel {} already ended — not monitoring", event.getDuelId());
            return;
        }

        MonitoredMatch match = MonitoredMatch.builder()
                .duelId(event.getDuelId())
                .handles(event.getHandles())
                .problemIds(event.getProblemIds())
                .startEpoch(event.getStartEpoch())
                .endEpoch(event.getEndEpoch())
                .build();
        store.save(match);

        long nowMs = System.currentTimeMillis();
        for (String handle : event.getHandles()) {
            store.watch(match, new WatchTarget(event.getDuelId(), handle), nowMs,
                    properties.getPolling().getMinIntervalSeconds());
        }

        log.info("Monitoring duel {} — handles {}, problems {}, until epoch {}",
                event.getDuelId(), event.getHandles(), event.getProblemIds(), event.getEndEpoch());
    }
}
