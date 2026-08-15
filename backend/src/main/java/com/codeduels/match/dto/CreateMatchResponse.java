package com.codeduels.match.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import java.util.UUID;

@Data
@AllArgsConstructor
@Builder
public class CreateMatchResponse {
    private UUID matchId;
    private String roomCode;
    private String shareableLink;
}
