package com.codeduels.problem.dto;

import com.codeduels.problem.model.Difficulty;
import com.codeduels.problem.model.ProblemStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Data
public class ProblemFilter {
    private Difficulty difficulty;
    private String title;
    private String slug;
    private Integer minPoints;
    private Integer maxPoints;
    private ProblemStatus status;
}
