package com.codeduels.duel.repository;

import com.codeduels.duel.state.DuelData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Repository
@RequiredArgsConstructor
@Slf4j
public class DuelStateRepositoryImpl implements DuelStateRepository {

    private final RedisTemplate<String, Object> redisTemplate;
    private final StringRedisTemplate stringRedisTemplate;

    private static final String DUEL_KEY = "duel:data:";
    private static final String CODE_KEY = "duel:code:";
    private static final String LOCK_KEY = "duel:lock:";
    private static final String START_INDEX = "duel:index:start";
    private static final String END_INDEX = "duel:index:end";

    private static final Duration LOCK_TTL = Duration.ofSeconds(10);
    private static final long LOCK_WAIT_MS = 5_000;
    private static final long LOCK_RETRY_MS = 25;

    // Only delete the lock if we still own it — it may have expired and been taken by someone else
    private static final DefaultRedisScript<Long> RELEASE_LOCK = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end",
            Long.class);

    @Override
    public void save(DuelData duel, long ttlMinutes) {
        redisTemplate.opsForValue().set(
                DUEL_KEY + duel.getDuelId(), duel, ttlMinutes, TimeUnit.MINUTES);
    }

    @Override
    public Optional<DuelData> findById(UUID duelId) {
        Object value = redisTemplate.opsForValue().get(DUEL_KEY + duelId);
        return Optional.ofNullable((DuelData) value);
    }

    @Override
    public void saveRoomCode(String roomCode, UUID duelId, long ttlMinutes) {
        redisTemplate.opsForValue().set(
                CODE_KEY + roomCode, duelId.toString(), ttlMinutes, TimeUnit.MINUTES);
    }

    @Override
    public Optional<UUID> findIdByRoomCode(String roomCode) {
        Object value = redisTemplate.opsForValue().get(CODE_KEY + roomCode);
        return Optional.ofNullable(value).map(v -> UUID.fromString(v.toString()));
    }

    @Override
    public boolean existsByRoomCode(String roomCode) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(CODE_KEY + roomCode));
    }

    @Override
    public void delete(UUID duelId) {
        findById(duelId).ifPresent(duel ->
                redisTemplate.delete(CODE_KEY + duel.getRoomCode()));
        redisTemplate.delete(DUEL_KEY + duelId);
        removeFromStartIndex(duelId);
        removeFromEndIndex(duelId);
    }

    // ───────────────────────────── locking ─────────────────────────────

    @Override
    public <T> T withLock(UUID duelId, Supplier<T> action) {
        String key = LOCK_KEY + duelId;
        String token = UUID.randomUUID().toString();
        long deadline = System.currentTimeMillis() + LOCK_WAIT_MS;

        while (!Boolean.TRUE.equals(stringRedisTemplate.opsForValue().setIfAbsent(key, token, LOCK_TTL))) {
            if (System.currentTimeMillis() > deadline) {
                throw new IllegalStateException("Timed out waiting for lock on duel " + duelId);
            }
            try {
                Thread.sleep(LOCK_RETRY_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted waiting for lock on duel " + duelId, e);
            }
        }

        try {
            return action.get();
        } finally {
            stringRedisTemplate.execute(RELEASE_LOCK, List.of(key), token);
        }
    }

    // ───────────────────────────── indexes ─────────────────────────────

    @Override
    public void addToStartIndex(UUID duelId, long scheduledAtEpoch) {
        stringRedisTemplate.opsForZSet().add(START_INDEX, duelId.toString(), scheduledAtEpoch);
    }

    @Override
    public List<UUID> findDueToStart(long nowEpoch) {
        return due(START_INDEX, nowEpoch);
    }

    @Override
    public void removeFromStartIndex(UUID duelId) {
        stringRedisTemplate.opsForZSet().remove(START_INDEX, duelId.toString());
    }

    @Override
    public void addToEndIndex(UUID duelId, long completeAtEpoch) {
        stringRedisTemplate.opsForZSet().add(END_INDEX, duelId.toString(), completeAtEpoch);
    }

    @Override
    public List<UUID> findDueToEnd(long nowEpoch) {
        return due(END_INDEX, nowEpoch);
    }

    @Override
    public void removeFromEndIndex(UUID duelId) {
        stringRedisTemplate.opsForZSet().remove(END_INDEX, duelId.toString());
    }

    private List<UUID> due(String index, long nowEpoch) {
        Set<String> ids = stringRedisTemplate.opsForZSet().rangeByScore(index, 0, nowEpoch);
        return ids == null ? List.of() : ids.stream().map(UUID::fromString).toList();
    }
}
