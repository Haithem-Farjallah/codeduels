package com.codeduels.judge0.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class Judge0SubmissionRequest {
    String source_code ;
    Integer language_id;
    String stdin;
    String expected_output;
}
