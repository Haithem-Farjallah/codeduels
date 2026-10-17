package com.codeduels.sentinel.poller;

import com.codeduels.sentinel.config.SentinelProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BackoffPolicyTest {

    // min 5s, max 30s, ×2, endgame 120s
    private final BackoffPolicy policy = new BackoffPolicy(new SentinelProperties());

    private static final long END = 10_000;
    private static final long MID_DUEL = END - 3_600;

    @Test
    void activityResetsToMinimum() {
        assertThat(policy.nextIntervalSeconds(30, true, MID_DUEL, END)).isEqualTo(5);
    }

    @Test
    void idleHandleBacksOffUpToTheCap() {
        assertThat(policy.nextIntervalSeconds(5, false, MID_DUEL, END)).isEqualTo(10);
        assertThat(policy.nextIntervalSeconds(10, false, MID_DUEL, END)).isEqualTo(20);
        assertThat(policy.nextIntervalSeconds(20, false, MID_DUEL, END)).isEqualTo(30);
        assertThat(policy.nextIntervalSeconds(30, false, MID_DUEL, END)).isEqualTo(30);
    }

    @Test
    void endgameAndOvertimePollAtMinimum() {
        assertThat(policy.nextIntervalSeconds(30, false, END - 60, END)).isEqualTo(5);
        assertThat(policy.nextIntervalSeconds(30, false, END + 10, END)).isEqualTo(5);
    }

    @Test
    void neverSleepsPastTheStartOfTheEndgame() {
        // 135s before the end: endgame starts in 15s, so a 30s backoff is clipped to 15s
        assertThat(policy.nextIntervalSeconds(20, false, END - 135, END)).isEqualTo(15);
    }
}
