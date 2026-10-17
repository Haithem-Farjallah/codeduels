package com.codeduels.duel.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
public class DuelSubmissionDto {
    private long submissionId;
    private String handle;
    private String problemId;
    private String verdict;
    private long submittedAtEpoch;
    /** false for verdicts ICPC doesn't penalise, e.g. compilation errors */
    private boolean penalised;
}
