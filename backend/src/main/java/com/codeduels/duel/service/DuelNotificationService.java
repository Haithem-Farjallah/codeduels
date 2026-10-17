package com.codeduels.duel.service;

import com.codeduels.duel.dto.DuelStateResponse;
import com.codeduels.duel.dto.DuelSubmissionDto;
import com.codeduels.duel.dto.PlayerScoreDto;

import java.util.UUID;

public interface DuelNotificationService {
    void notifyPlayerJoined(UUID duelId, DuelStateResponse state);
    void notifyDuelStart(UUID duelId, DuelStateResponse state);
    void notifyScoreboardUpdate(UUID duelId, PlayerScoreDto player, DuelSubmissionDto submission);
    void notifyDuelEnd(UUID duelId, DuelStateResponse state);
    void notifyDuelCanceled(UUID duelId, String reason);
}
