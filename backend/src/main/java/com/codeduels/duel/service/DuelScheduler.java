package com.codeduels.duel.service;

import com.codeduels.duel.repository.DuelStateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Server-owned duel timing. Works off the Redis start/end indexes; each transition re-checks
 * the duel's status under its lock, so overlapping ticks or multiple instances are harmless.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DuelScheduler {

    private final DuelStateRepository duelStateRepository;
    private final DuelService duelService;

    @Scheduled(fixedDelayString = "${app.duel.scheduler-interval-ms:5000}")
    public void startScheduledDuels() {
        List<UUID> due = duelStateRepository.findDueToStart(Instant.now().getEpochSecond());
        if (due.isEmpty()) return;

        log.info("DuelScheduler: {} duel(s) ready to start", due.size());
        for (UUID duelId : due) {
            try {
                duelService.startDuel(duelId);
            } catch (Exception e) {
                log.error("Failed to start duel {}; will retry next tick", duelId, e);
            }
        }
    }

    @Scheduled(fixedDelayString = "${app.duel.scheduler-interval-ms:5000}")
    public void completeFinishedDuels() {
        List<UUID> due = duelStateRepository.findDueToEnd(Instant.now().getEpochSecond());
        if (due.isEmpty()) return;

        log.info("DuelScheduler: {} duel(s) ready to complete", due.size());
        for (UUID duelId : due) {
            try {
                duelService.completeDuel(duelId);
            } catch (Exception e) {
                log.error("Failed to complete duel {}; will retry next tick", duelId, e);
            }
        }
    }
}
