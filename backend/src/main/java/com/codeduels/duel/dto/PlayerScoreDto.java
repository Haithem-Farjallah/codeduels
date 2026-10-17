package com.codeduels.duel.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.Map;
import java.util.UUID;

@Data
@AllArgsConstructor
@Builder
public class PlayerScoreDto {
    private UUID userId;
    private String handle;
    private String username;
    private int solvedCount;
    private long totalPenalty;

    /** problemId → per-problem status */
    private Map<String, ProblemStatusDto> problems;
}
