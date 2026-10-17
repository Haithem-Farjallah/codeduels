package com.codeduels.duel;

import com.codeduels.config.RedisConfig;
import com.codeduels.duel.model.DuelStatus;
import com.codeduels.duel.state.DuelData;
import com.codeduels.duel.state.DuelSubmission;
import com.codeduels.duel.state.PlayerScore;
import com.codeduels.duel.state.ProblemStats;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.RedisSerializer;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/** DuelData goes through the same serializer RedisConfig builds — it must survive a round trip */
class DuelDataSerializationTest {

    @Test
    @SuppressWarnings("unchecked")
    void duelDataRoundTripsThroughTheRedisSerializer() {
        RedisSerializer<Object> serializer = (RedisSerializer<Object>) new RedisConfig()
                .redisTemplate(mock(RedisConnectionFactory.class)).getValueSerializer();

        ProblemStats stats = ProblemStats.builder().problemId("100A").build();
        stats.getWrongAttemptEpochs().add(42L);
        Map<String, ProblemStats> problems = new HashMap<>(Map.of("100A", stats));

        DuelData duel = DuelData.builder()
                .duelId(UUID.randomUUID())
                .roomCode("ABC123")
                .status(DuelStatus.ACTIVE)
                .problemIds(new ArrayList<>(List.of("100A")))
                .problemUrls(new ArrayList<>(List.of("https://codeforces.com/contest/100/problem/A")))
                .problemNames(new HashMap<>(Map.of("100A", "Theatre Square")))
                .playerOneId(UUID.randomUUID())
                .playerOneHandle("alice")
                .startedAtEpoch(1L)
                .scoreboard(new HashMap<>(Map.of("alice",
                        PlayerScore.builder().handle("alice").problems(problems).build())))
                .submissions(new ArrayList<>(List.of(
                        new DuelSubmission(7L, "alice", "100A", "OK", 99L))))
                .build();

        DuelData back = (DuelData) serializer.deserialize(serializer.serialize(duel));

        assertThat(back).isEqualTo(duel);
        assertThat(back.getScoreboard().get("alice").getProblems().get("100A").getWrongAttemptEpochs())
                .containsExactly(42L);
    }
}
