package com.codeduels.match.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Builder
@AllArgsConstructor
@Data
public class JoinMatchResponse {
    private UUID matchId;
    private Instant scheduledAt;
    private int durationInMinutes;
}
