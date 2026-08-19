package com.codeduels.match.service;

import com.codeduels.match.dto.MatchResultResponse;
import com.codeduels.match.dto.MatchStateResponse;

import java.time.Instant;
import java.util.UUID;

public interface MatchNotificationService {
    void notifyPlayerJoined(UUID matchId, UUID playerTwoId, Instant scheduledAt);
    void notifyMatchStart(UUID matchId, MatchStateResponse state);
    void notifyMatchUpdate(UUID matchId, int playerOnePenalties, int playerTwoPenalties);
    void notifyMatchEnd(UUID matchId, MatchResultResponse result);
    void notifyMatchCanceled(UUID matchId, String reason);
}
