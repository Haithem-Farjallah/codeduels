package com.codeduels.duel.codeforces;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CfProblem {
    private Integer contestId;
    private String index;
    private String name;

    /** Same shape CodeforcesUrlParser produces, e.g. 2271E */
    public String key() {
        return contestId + index;
    }
}
