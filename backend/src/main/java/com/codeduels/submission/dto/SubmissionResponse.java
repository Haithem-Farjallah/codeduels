package com.codeduels.submission.dto;

import lombok.Builder;

import java.util.UUID;

@Builder
public record SubmissionResponse(UUID submissionId) {
}
