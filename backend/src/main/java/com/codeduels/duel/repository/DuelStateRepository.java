package com.codeduels.duel.repository;

import com.codeduels.duel.state.DuelData;

import java.util.Optional;
import java.util.UUID;

public interface DuelStateRepository {
    void save(DuelData duel, long ttlMinutes);
    Optional<DuelData> findById(UUID duelId);
    Optional<UUID> findIdByRoomCode(String roomCode);
    void saveRoomCode(String roomCode, UUID duelId, long ttlMinutes);
    void delete(UUID duelId);
    boolean existsByRoomCode(String roomCode);
}
