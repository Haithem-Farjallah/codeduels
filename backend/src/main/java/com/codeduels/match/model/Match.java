package com.codeduels.match.model;

import com.codeduels.common.entity.UuidBaseEntity;
import com.codeduels.problem.model.Difficulty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "match", indexes = {
        @Index(name = "idx_match_room_code", columnList = "room_code"),
        @Index(name = "idx_match_player_one", columnList = "player_one_id"),
        @Index(name = "idx_match_player_two", columnList = "player_two_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Match extends UuidBaseEntity {
    @Column(name = "room_code", nullable = false, unique = true, length = 6)
    private String roomCode;

    @Column(name = "player_one_id", nullable = false)
    private UUID playerOneId;

    @Column(name = "player_two_id")
    private UUID playerTwoId;

    @Column(name = "problem_id")
    private UUID problemId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MatchStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Difficulty difficulty;

    @Column(name = "duration_in_minutes", nullable = false)
    private int durationInMinutes;

    @Column(name = "start_delay_in_minutes", nullable = false) // lobby countdown before starting match
        private int startDelayInMinutes;

    @Column(name = "winner_id")
    private UUID winnerId;

    @Column(name = "player_one_penalties")
    private int playerOnePenalties;

    @Column(name = "player_two_penalties")
    private int playerTwoPenalties;

    @Column(name = "player_one_finish_time")
    private Instant playerOneFinishTime;

    @Column(name = "player_two_finish_time")
    private Instant playerTwoFinishTime;

    @Column(name = "scheduled_at") //when match is supposed to start (when player2 joins+ delay countdown)
    private Instant scheduledAt;

    @Column(name = "started_at") // when match actually started
    private Instant startedAt;

    @Column(name = "ended_at")
    private Instant endedAt;
}