package com.codeduels.duel.state;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProblemStats {

    private String problemId;

    @Builder.Default
    private boolean solved = false;

    /** Wrong attempts that count for penalty — before the first accepted, or all of them while unsolved */
    @Builder.Default
    private int attempts = 0;

    private Long solvedAtEpoch;
    private Long penalty;

    /**
     * Raw submission times of every penalised verdict. Verdicts can arrive out of order
     * (SQS standard queues, slow judging), so attempts/penalty are always recomputed from
     * these instead of being incremented in place.
     */
    @Builder.Default
    private List<Long> wrongAttemptEpochs = new ArrayList<>();
}
