package com.codeduels.submission.dto;

import com.codeduels.submission.model.SubmissionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmissionDetailsDTO {
    private UUID id;
    private UUID problemId;
    private UUID matchId;
    private String problemTitle;
    private String problemSlug;
    private SubmissionStatus status;
    private String language;
    private String code;
    private Integer runtimeMs;
    private Integer memoryKb;
    private String stdout;
    private String stderr;
    private Instant createdAt;
}