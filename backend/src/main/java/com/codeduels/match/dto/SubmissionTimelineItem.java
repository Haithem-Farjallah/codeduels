package com.codeduels.match.dto;

import com.codeduels.submission.model.SubmissionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@AllArgsConstructor
@Builder
public class SubmissionTimelineItem {
    private UUID submissionId;
    private SubmissionStatus status;
    private String language;
    private Integer runtimeMs;
    private Instant createdAt;
}