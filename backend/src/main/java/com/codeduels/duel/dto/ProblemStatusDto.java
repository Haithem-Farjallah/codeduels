package com.codeduels.duel.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
public class ProblemStatusDto {
    private boolean solved;
    private int attempts;
    private Long penalty;
    private Long solvedAtEpoch;
}