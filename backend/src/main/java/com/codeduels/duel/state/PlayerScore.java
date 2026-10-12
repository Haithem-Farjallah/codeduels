package com.codeduels.duel.state;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlayerScore {

    private UUID userId;
    private String handle;
    private String username;

    @Builder.Default
    private int solvedCount = 0;

    @Builder.Default
    private long totalPenalty = 0;

    @Builder.Default
    private Map<String, ProblemStats> problems = new HashMap<>();
}