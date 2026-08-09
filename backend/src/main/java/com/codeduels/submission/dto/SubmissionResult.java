package com.codeduels.submission.dto;

import com.codeduels.submission.model.Submission;
import com.codeduels.submission.model.SubmissionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubmissionResult {
    private UUID id;
    private UUID problemId;
    private UUID matchId;
    private SubmissionStatus status;
    private String language;
    private Integer runtimeMs;
    private Integer memoryKb;
    private String stdout;
    private String stderr;
    private Instant createdAt;


    public static SubmissionResult fromSubmission(Submission s) {
        return SubmissionResult.builder()
                .id(s.getId())
                .problemId(s.getProblemId())
                .matchId(s.getMatchId())
                .status(s.getStatus())
                .language(s.getLanguage().name())
                .runtimeMs(s.getRuntimeMs())
                .memoryKb(s.getMemoryKb())
                .stdout(s.getStdout())
                .stderr(s.getStderr())
                .createdAt(s.getCreatedAt())
                .build();
    }

}
