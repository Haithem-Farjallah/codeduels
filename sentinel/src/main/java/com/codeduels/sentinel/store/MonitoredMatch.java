package com.codeduels.sentinel.store;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MonitoredMatch {
    private UUID duelId;
    private List<String> handles;
    private List<String> problemIds;
    private long startEpoch;
    private long endEpoch;
}
