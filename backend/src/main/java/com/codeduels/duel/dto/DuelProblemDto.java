package com.codeduels.duel.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
public class DuelProblemDto {
    private String problemId;
    private String url;
    private String name;
}