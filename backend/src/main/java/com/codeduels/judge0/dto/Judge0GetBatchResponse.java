package com.codeduels.judge0.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor
@Data
@AllArgsConstructor
public class Judge0GetBatchResponse {
    @JsonProperty private List<Judge0SubmissionResponse> submissions;
}
