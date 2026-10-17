package com.codeduels.duel.repository;

import com.codeduels.duel.model.DuelHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DuelHistoryRepository extends JpaRepository<DuelHistory, UUID> {
    Optional<DuelHistory> findByDuelId(UUID duelId);
    boolean existsByDuelId(UUID duelId);
    Page<DuelHistory> findByPlayerOneIdOrPlayerTwoId(UUID playerOneId, UUID playerTwoId, Pageable pageable);
}
