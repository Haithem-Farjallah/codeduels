package com.codeduels.duel.service;

import com.codeduels.duel.dto.DuelStateResponse;
import com.codeduels.duel.dto.DuelSubmissionDto;
import com.codeduels.duel.dto.PlayerScoreDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

/**
 * Pushes to /topic/duel/{duelId}. Phase transitions carry the full state,
 * scoreboard updates carry only the player that changed plus the submission that changed it.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DuelNotificationServiceImpl implements DuelNotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    private String topic(UUID duelId) {
        return "/topic/duel/" + duelId;
    }

    @Override
    public void notifyPlayerJoined(UUID duelId, DuelStateResponse state) {
        send(duelId, Map.of("eventType", "PLAYER_JOINED", "state", state));
    }

    @Override
    public void notifyDuelStart(UUID duelId, DuelStateResponse state) {
        send(duelId, Map.of("eventType", "DUEL_START", "state", state));
    }

    @Override
    public void notifyScoreboardUpdate(UUID duelId, PlayerScoreDto player, DuelSubmissionDto submission) {
        send(duelId, Map.of(
                "eventType", "SCOREBOARD_UPDATE",
                "player", player,
                "submission", submission));
    }

    @Override
    public void notifyDuelEnd(UUID duelId, DuelStateResponse state) {
        send(duelId, Map.of("eventType", "DUEL_END", "state", state));
    }

    @Override
    public void notifyDuelCanceled(UUID duelId, String reason) {
        send(duelId, Map.of("eventType", "DUEL_CANCELED", "reason", reason));
    }

    private void send(UUID duelId, Map<String, Object> payload) {
        try {
            messagingTemplate.convertAndSend(topic(duelId), payload);
            log.info("[WS] {} → duel {}", payload.get("eventType"), duelId);
        } catch (Exception e) {
            log.error("[WS] Failed to push to duel {}", duelId, e);
        }
    }
}
