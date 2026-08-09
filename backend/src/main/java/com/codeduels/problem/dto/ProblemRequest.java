package com.codeduels.problem.dto;

import com.codeduels.problem.model.Difficulty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProblemRequest {

    @NotBlank
    @Size(max = 150)
    private String title;

    @NotBlank
    private String description;

    private String constraints;

    @NotNull
    private Difficulty difficulty;

    @Min(1) @Max(100)
    private int points;

    @Min(100) @Max(20_000)
    private int timeLimitMs;

    @Min(1024) @Max(1_048_576)
    private int memoryLimitKb;

    @Valid
    @Size(min = 1, max = 5)
    private List<TestCaseDto> sampleTestCases;

    @Size(max = 5)
    private Set<@NotBlank String> tags;

}