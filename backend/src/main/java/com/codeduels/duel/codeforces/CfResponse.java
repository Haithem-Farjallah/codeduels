package com.codeduels.duel.codeforces;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Envelope every Codeforces API call returns: status is "OK" or "FAILED" (with a comment). */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CfResponse<T> {
    private String status;
    private String comment;
    private T result;
}
