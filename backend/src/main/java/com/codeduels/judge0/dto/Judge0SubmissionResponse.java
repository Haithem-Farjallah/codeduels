package com.codeduels.judge0.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Judge0SubmissionResponse {
    private String stdout;
    private String stderr;
    @JsonProperty("compile_output") private String compileOutput;
    private String message;
    private Double time;
    private Integer memory;
    private Judge0SubmissionStatus status;
    private String token;
}
