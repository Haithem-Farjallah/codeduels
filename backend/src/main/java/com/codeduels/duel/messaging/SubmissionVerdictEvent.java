package com.codeduels.duel.messaging;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Received from match-result-queue: one judged Codeforces submission for a watched duel.
 * Contract is mirrored in the sentinel app — keep the two in sync.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubmissionVerdictEvent {
    private UUID duelId;
    private String handle;
    private String problemId;
    private long submissionId;
    private String verdict;
    private long submittedAtEpoch;
}
