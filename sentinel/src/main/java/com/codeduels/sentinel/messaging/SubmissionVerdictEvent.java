package com.codeduels.sentinel.messaging;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/** Mirror of the backend's com.codeduels.duel.messaging.SubmissionVerdictEvent — keep the two in sync. */
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
