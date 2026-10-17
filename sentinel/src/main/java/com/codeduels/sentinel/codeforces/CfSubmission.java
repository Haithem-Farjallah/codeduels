package com.codeduels.sentinel.codeforces;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class CfSubmission {

    private long id;
    private Integer contestId;
    private long creationTimeSeconds;
    private CfProblem problem;

    /** Absent while the submission is in the queue, TESTING while it runs */
    private String verdict;

    /** Same shape the backend's CodeforcesUrlParser produces, e.g. 2271E */
    public String problemKey() {
        if (problem == null || problem.getIndex() == null) return null;
        Integer contest = problem.getContestId() != null ? problem.getContestId() : contestId;
        return contest == null ? null : contest + problem.getIndex().toUpperCase();
    }

    public boolean isJudged() {
        return verdict != null && !"TESTING".equals(verdict);
    }
}
