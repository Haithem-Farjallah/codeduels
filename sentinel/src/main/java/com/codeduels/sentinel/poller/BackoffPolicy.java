package com.codeduels.sentinel.poller;

import com.codeduels.sentinel.config.SentinelProperties;
import org.springframework.stereotype.Component;

/**
 * How long to wait before polling a handle again. Every Codeforces call comes out of one shared
 * budget, so idle handles back off exponentially and hand their slots to active ones:
 *   - activity (a new verdict, a submission still judging, a failed publish) → minimum interval
 *   - inside the endgame window or after the end → minimum interval, last-second submissions matter
 *   - otherwise → previous interval × multiplier, capped
 */
@Component
public class BackoffPolicy {

    private final SentinelProperties.Polling config;

    public BackoffPolicy(SentinelProperties properties) {
        this.config = properties.getPolling();
    }

    public long nextIntervalSeconds(long currentSeconds, boolean activity, long nowEpoch, long endEpoch) {
        long min = config.getMinIntervalSeconds();

        if (activity || nowEpoch >= endEpoch - config.getEndgameSeconds()) {
            return min;
        }

        long grown = (long) Math.ceil(Math.max(currentSeconds, min) * config.getBackoffMultiplier());
        long next = Math.min(grown, config.getMaxIntervalSeconds());

        // never sleep past the start of the endgame window
        long untilEndgame = endEpoch - config.getEndgameSeconds() - nowEpoch;
        return Math.max(min, Math.min(next, untilEndgame));
    }
}
