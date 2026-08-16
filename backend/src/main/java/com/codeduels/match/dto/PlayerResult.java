package com.codeduels.match.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
@Builder
public class PlayerResult {
    private UUID userId;
    private boolean solved;
    private Instant finishTime;
    private int penalties;
    private Long effectiveTimeSeconds;    // null if never solved
    private
    List<SubmissionTimelineItem> submissions;
}
