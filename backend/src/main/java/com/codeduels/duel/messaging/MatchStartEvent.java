package com.codeduels.duel.messaging;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Sent to match-watch-queue when a duel goes ACTIVE. Tells the Sentinel which handles to
 * poll, which problems count, and the window in which submissions count.
 * Contract is mirrored in the sentinel app — keep the two in sync.
 */
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
