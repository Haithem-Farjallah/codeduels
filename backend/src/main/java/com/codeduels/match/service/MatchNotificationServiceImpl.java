package com.codeduels.match.service;

import com.codeduels.match.dto.MatchResultResponse;
import com.codeduels.match.dto.MatchStateResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MatchNotificationServiceImpl implements MatchNotificationService {
    private final SimpMessagingTemplate messagingTemplate;

    private String topic(UUID matchId){
        return "/topic/match/" + matchId;
    }

    @Override
    public void notifyPlayerJoined(UUID matchId, UUID playerTwoId, Instant scheduledAt) {
        send(matchId,Map.of(
                "eventType","PLAYER_JOINED",
                "playerTwoId",playerTwoId,
                "scheduledAt",scheduledAt
        ));
    }

    @Override
    public void notifyMatchStart(UUID matchId, MatchStateResponse state) {
        send(matchId, Map.of("eventType", "MATCH_START", "state", state));
    }

    @Override
    public void notifyMatchUpdate(UUID matchId, int playerOnePenalties, int playerTwoPenalties) {
        send(matchId, Map.of(
                "eventType", "STATE_UPDATE",
                "playerOnePenalties", playerOnePenalties,
                "playerTwoPenalties", playerTwoPenalties));
    }

    @Override
    public void notifyMatchEnd(UUID matchId, MatchResultResponse result) {
        send(matchId, Map.of("eventType", "MATCH_END", "result", result));
    }

    @Override
    public void notifyMatchCanceled(UUID matchId, String reason) {
        send(matchId, Map.of("eventType", "MATCH_CANCELED", "reason", reason));

    }

    private void send(UUID matchId, Map<String,Object>payload) {
        try{
            messagingTemplate.convertAndSend(topic(matchId), payload);
            log.info("[WS] {} → match {}", payload.get("eventType"), matchId);
        }catch (Exception e){
            log.error("[WS] Failed to push to match {}", matchId, e);
        }
    }
}
