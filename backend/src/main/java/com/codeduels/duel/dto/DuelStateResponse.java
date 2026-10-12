package com.codeduels.duel.dto;

import com.codeduels.duel.model.DuelStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
@Builder
public class DuelStateResponse {

    private UUID duelId;
    private String roomCode;
    private DuelStatus status;

    private List<DuelProblemDto> problems;

    private PlayerScoreDto playerOne;
    private PlayerScoreDto playerTwo;

    private Long scheduledAtEpoch;
    private Long startedAtEpoch;
    private Long endsAtEpoch;
    private int durationMinutes;
    private long secondsRemaining;

    private String winnerHandle;
}