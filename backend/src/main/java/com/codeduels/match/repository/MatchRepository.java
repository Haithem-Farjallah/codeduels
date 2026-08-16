package com.codeduels.match.repository;

import com.codeduels.match.model.Match;
import com.codeduels.match.model.MatchStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MatchRepository extends JpaRepository<Match, UUID> {
    boolean existsByRoomCode(String roomCode);
    Optional<Match> findByRoomCode(String roomCode);
    List<Match> findAllByStatusAndScheduledAtBefore(MatchStatus status , Instant scheduledAt);

    @Query("""
        SELECT m FROM Match m
        WHERE (m.playerOneId = :userId OR m.playerTwoId = :userId)
          AND m.status IN :statuses
        """)
    Page<Match> findUserMatches(@Param("userId") UUID userId,
                                @Param("statuses") Collection<MatchStatus> statuses,
                                Pageable pageable);

    @Query("""
        SELECT m FROM Match m
        WHERE (m.playerOneId = :userId OR m.playerTwoId = :userId)
          AND m.status = 'COMPLETED'
          AND m.winnerId = :userId
        """)
    Page<Match> findUserWins(@Param("userId") UUID userId, Pageable pageable);

    @Query("""
        SELECT m FROM Match m
        WHERE (m.playerOneId = :userId OR m.playerTwoId = :userId)
          AND m.status = 'COMPLETED'
          AND m.winnerId IS NOT NULL
          AND m.winnerId <> :userId
        """)
    Page<Match> findUserLosses(@Param("userId") UUID userId, Pageable pageable);

    @Query("""
        SELECT m FROM Match m
        WHERE (m.playerOneId = :userId OR m.playerTwoId = :userId)
          AND m.status = 'COMPLETED'
          AND m.winnerId IS NULL
        """)
    Page<Match> findUserDraws(@Param("userId") UUID userId, Pageable pageable);

}
