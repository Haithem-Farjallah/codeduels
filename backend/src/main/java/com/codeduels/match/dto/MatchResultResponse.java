package com.codeduels.match.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@AllArgsConstructor
@Builder
public class MatchResultResponse {
    private UUID matchId;
    private UUID problemId;
    private String problemTitle;

    private String outcome;               // PLAYER_ONE_WIN | PLAYER_TWO_WIN | DRAW
    private UUID winnerId;
    private UUID winningSubmissionId;

    private PlayerResult playerOne;
    private PlayerResult playerTwo;

    private Instant startedAt;
    private Instant endedAt;
}
