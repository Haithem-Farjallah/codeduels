package com.codeduels.judge0.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Judge0BatchSubmissionRequest {
    @JsonProperty("submissions") private List<Judge0SubmissionRequest> submissions;
}
