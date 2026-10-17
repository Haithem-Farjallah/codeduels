package com.codeduels.duel.state;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** One Codeforces submission the Sentinel reported for this duel, kept for the live feed and the archive */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DuelSubmission {
    private long submissionId;
    private String handle;
    private String problemId;
    private String verdict;
    private long submittedAtEpoch;
}
