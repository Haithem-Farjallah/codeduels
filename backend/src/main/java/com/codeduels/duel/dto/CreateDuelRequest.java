package com.codeduels.duel.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class CreateDuelRequest {

    @NotEmpty(message = "At least one problem link is required")
    @Size(max = 5, message = "A duel can have at most 5 problems")
    private List<@NotBlank String> problemUrls;

    @NotBlank(message = "Your Codeforces handle is required")
    private String handle;

    @Min(10) @Max(180)
    private int durationMinutes;

    @Min(0) @Max(10)
    private int startDelayMinutes;
}