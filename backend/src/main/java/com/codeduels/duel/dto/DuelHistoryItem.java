package com.codeduels.duel.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@AllArgsConstructor
@Builder
public class DuelHistoryItem {
    private UUID duelId;
    private String result;
    private String opponentHandle;
    private int yourSolved;
    private long yourPenalty;
    private int opponentSolved;
    private long opponentPenalty;
    private Instant startedAt;
    private Instant endedAt;
}