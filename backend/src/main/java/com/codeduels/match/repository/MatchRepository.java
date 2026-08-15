package com.codeduels.match.repository;

import com.codeduels.match.model.Match;
import com.codeduels.match.model.MatchStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MatchRepository extends JpaRepository<Match, UUID> {
    boolean existsByRoomCode(String roomCode);
    Optional<Match> findByRoomCode(String roomCode);
    List<Match> findAllByStatusAndScheduledAtBefore(MatchStatus status , Instant scheduledAt);
}
