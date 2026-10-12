package com.codeduels.duel.repository;

import com.codeduels.duel.state.DuelData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Repository
@RequiredArgsConstructor
@Slf4j
public class DuelStateRepositoryImpl implements DuelStateRepository {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String DUEL_KEY = "duel:data:";
    private static final String CODE_KEY = "duel:code:";

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
    }
}
