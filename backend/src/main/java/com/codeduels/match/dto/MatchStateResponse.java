package com.codeduels.match.dto;

import com.codeduels.match.model.MatchStatus;
import com.codeduels.problem.dto.ProblemResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@AllArgsConstructor
@Builder
public class MatchStateResponse {
    private UUID matchId;
    private MatchStatus status;
    private ProblemResponse problem;

    private UUID playerOneId;
    private int playerOnePenalties;
    private Instant playerOneFinishTime;

    private UUID playerTwoId;
    private int playerTwoPenalties;
    private Instant playerTwoFinishTime;

    private Instant scheduledAt;
    private Instant startedAt;
    private int durationInMinutes;
    private long secondsRemaining;
    private UUID winnerId;
}
