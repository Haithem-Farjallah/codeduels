package com.codeduels.submission.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
public class SubmissionRequest {
    @NotNull
    private UUID problemId;
    private UUID matchId;
    @NotBlank
    private String code;
    @NotBlank
    private String language;
}