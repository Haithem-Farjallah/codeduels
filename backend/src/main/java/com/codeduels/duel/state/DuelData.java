package com.codeduels.duel.state;

import com.codeduels.duel.model.DuelStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DuelData {

    private UUID duelId;
    private String roomCode;
    private DuelStatus status;

    private List<String> problemIds;
    private List<String> problemUrls;

    private UUID playerOneId;
    private String playerOneHandle;
    private UUID playerTwoId;
    private String playerTwoHandle;

    private int durationMinutes;
    private int startDelayMinutes;

    private Long scheduledAtEpoch;
    private Long startedAtEpoch;
    private Long endsAtEpoch;

    private String winnerHandle;

    @Builder.Default
    private Map<String, PlayerScore> scoreboard = new HashMap<>();
}