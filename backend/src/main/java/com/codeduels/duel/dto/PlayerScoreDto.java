package com.codeduels.duel.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
public class PlayerScoreDto {
    private String handle;
    private String username;
    private int solvedCount;
    private long totalPenalty;

    /** problemId → status: SOLVED | ATTEMPTED | UNTOUCHED */
    private Map<String, ProblemStatusDto> problems;
}