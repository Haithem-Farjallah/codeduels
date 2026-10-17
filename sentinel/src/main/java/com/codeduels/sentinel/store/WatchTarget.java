package com.codeduels.sentinel.store;

import java.util.UUID;

/** One handle being watched for one duel — the unit the poller schedules. */
public record WatchTarget(UUID duelId, String handle) {

    private static final String SEPARATOR = "|";

    public String encode() {
        return duelId + SEPARATOR + handle;
    }

    public static WatchTarget decode(String value) {
        int split = value.indexOf(SEPARATOR);
        return new WatchTarget(UUID.fromString(value.substring(0, split)), value.substring(split + 1));
    }
}
