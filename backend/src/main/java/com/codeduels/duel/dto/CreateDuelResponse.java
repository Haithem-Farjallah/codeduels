package com.codeduels.duel.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
@Builder
public class CreateDuelResponse {
    private UUID duelId;
    private String roomCode;
    private String shareableLink;
}