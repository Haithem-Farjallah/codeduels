package com.codeduels.duel.state;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProblemStats {

    private String problemId;

    @Builder.Default
    private boolean solved = false;

    @Builder.Default
    private int attempts = 0;

    private Long solvedAtEpoch;
    private Long penalty;
}