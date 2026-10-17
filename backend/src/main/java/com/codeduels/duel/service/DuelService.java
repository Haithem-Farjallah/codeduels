package com.codeduels.duel.service;

import com.codeduels.duel.dto.*;
import com.codeduels.duel.messaging.SubmissionVerdictEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface DuelService {

    CreateDuelResponse createDuel(CreateDuelRequest request);

    JoinDuelResponse joinDuel(String roomCode, JoinDuelRequest request);

    /** Creator only, while still waiting for an opponent */
    void cancelDuel(UUID duelId);

    DuelStateResponse getDuelState(UUID duelId);

    /** SCHEDULED → ACTIVE and hand the duel to the Sentinel. Called by the scheduler. */
    void startDuel(UUID duelId);

    /** Applies one verdict from the Sentinel. Idempotent per Codeforces submission id. */
    void recordSubmission(SubmissionVerdictEvent event);

    /** ACTIVE → COMPLETED, archive to Postgres. Called by the scheduler once the duration (plus grace) is over. */
    void completeDuel(UUID duelId);

    Page<DuelHistoryItem> getDuelHistory(Pageable pageable);
}
