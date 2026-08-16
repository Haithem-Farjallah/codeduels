package com.codeduels.match.dto;

import com.codeduels.match.model.MatchStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@AllArgsConstructor
@Builder
public class MatchHistoryItem {
    private UUID matchId;
    private MatchStatus status;
    private String result;                // WIN | LOSS | DRAW | EXPIRED | CANCELED
    private UUID opponentId;
    private String opponentUsername;
    private UUID problemId;
    private String problemTitle;
    private Instant startedAt;
    private Instant endedAt;
}
