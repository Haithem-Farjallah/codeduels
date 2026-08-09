package com.codeduels.problem.dto;

import com.codeduels.problem.model.Difficulty;
import com.codeduels.problem.model.Problem;
import com.codeduels.problem.model.ProblemStatus;
import com.codeduels.problem.model.Tag;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProblemResponse {
    private UUID id;
    private String slug;
    private String title;
    private String description;
    private String constraints;
    private ProblemStatus status;
    private Difficulty difficulty;
    private int points;
    private int timeLimitMs;
    private int memoryLimitKb;
    private List<TestCaseDto> sampleTestCases;
    private Set<String> tags;
    private Instant createdAt;

public static ProblemResponse fromEntity(Problem problem){

    return ProblemResponse.builder()
            .id(problem.getId())
            .slug(problem.getSlug())
            .title(problem.getTitle())
            .description(problem.getDescription())
            .constraints(problem.getConstraints())
            .status(problem.getStatus())
            .difficulty(problem.getDifficulty())
            .points(problem.getPoints())
            .timeLimitMs(problem.getTimeLimitMs())
            .memoryLimitKb(problem.getMemoryLimitKb())
            .sampleTestCases(problem.getSampleTestCases().stream()
                    .map(p->new TestCaseDto(p.getInput(),p.getExpectedOutput(),p.getExplanation()))
                    .collect(Collectors.toList()))
            .tags(problem.getTags().stream().map(Tag::getName).collect(Collectors.toSet()))
            .createdAt(problem.getCreatedAt())
            .build();

}
}
