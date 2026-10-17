package com.codeduels.sentinel.messaging;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/** Mirror of the backend's com.codeduels.duel.messaging.MatchStartEvent — keep the two in sync. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MatchStartEvent {
    private UUID duelId;
    private List<String> handles;
    private List<String> problemIds;
    private long startEpoch;
    private long endEpoch;
}
