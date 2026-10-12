package com.codeduels.duel.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JoinDuelRequest {

    @NotBlank(message = "Your Codeforces handle is required")
    private String handle;
}