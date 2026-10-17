package com.codeduels.duel.repository;

import com.codeduels.duel.state.DuelData;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

public interface DuelStateRepository {
    void save(DuelData duel, long ttlMinutes);
    Optional<DuelData> findById(UUID duelId);
    Optional<UUID> findIdByRoomCode(String roomCode);
    void saveRoomCode(String roomCode, UUID duelId, long ttlMinutes);
    void delete(UUID duelId);
    boolean existsByRoomCode(String roomCode);

    /** Runs the action while holding a short Redis lock on the duel — every read-modify-write goes through here */
    <T> T withLock(UUID duelId, Supplier<T> action);

    // Redis has no "find by status", so the scheduler works off two sorted sets scored by epoch second
    void addToStartIndex(UUID duelId, long scheduledAtEpoch);
    List<UUID> findDueToStart(long nowEpoch);
    void removeFromStartIndex(UUID duelId);

    void addToEndIndex(UUID duelId, long completeAtEpoch);
    List<UUID> findDueToEnd(long nowEpoch);
    void removeFromEndIndex(UUID duelId);
}
