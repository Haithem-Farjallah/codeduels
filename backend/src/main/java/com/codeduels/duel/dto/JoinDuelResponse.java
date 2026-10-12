package com.codeduels.duel.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
@Builder
public class JoinDuelResponse {
    private UUID duelId;
    private Long scheduledAtEpoch;
    private int durationMinutes;
}
