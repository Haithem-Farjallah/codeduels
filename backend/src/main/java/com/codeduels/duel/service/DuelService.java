package com.codeduels.duel.service;

import com.codeduels.duel.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface DuelService {

    CreateDuelResponse createDuel(CreateDuelRequest request);

    JoinDuelResponse joinDuel(String roomCode, JoinDuelRequest request);

    DuelStateResponse getDuelState(UUID duelId);

    void recordSubmission(UUID duelId, String handle, String problemId,
                          String verdict, long submittedAtEpoch);

    void completeDuel(UUID duelId);

    Page<DuelHistoryItem> getDuelHistory(Pageable pageable);
}