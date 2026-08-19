package com.codeduels.problem.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
@Builder
public class ProblemCreatedResponse {
    private UUID problemId;
    private String slug;
    private String uploadUrl;
}