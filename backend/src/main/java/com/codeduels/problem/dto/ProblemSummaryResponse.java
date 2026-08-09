package com.codeduels.problem.dto;

import com.codeduels.problem.model.Difficulty;
import com.codeduels.problem.model.Problem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProblemSummaryResponse {
    private UUID id;
    private String slug;
    private String title;
    private Difficulty difficulty;
    private int points;

    public static ProblemSummaryResponse fromEntity(Problem problem) {
        return ProblemSummaryResponse.builder()
                .id(problem.getId())
                .slug(problem.getSlug())
                .title(problem.getTitle())
                .difficulty(problem.getDifficulty())
                .points(problem.getPoints())
                .build();
    }

}
