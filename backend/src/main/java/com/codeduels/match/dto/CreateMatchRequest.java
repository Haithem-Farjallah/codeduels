package com.codeduels.match.dto;

import com.codeduels.problem.model.Difficulty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Data
public class CreateMatchRequest {

    @NotNull
    private Difficulty difficulty;

    @NotNull(message = "Start delay is required")
    @Min(value = 1, message = "Duel must start in at least 1 min")
    @Max(value = 10, message = "Duel must start within 10 min ")
    private Integer startDelayInMinutes;

    @Min(value = 5,message = "Match duration must be at least 5 minutes")
    @Max(value = 45, message = "Match duration cannot exceed 45 minutes")
    private int durationInMinutes;

}
