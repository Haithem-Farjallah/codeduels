package com.codeduels.duel.model;

import com.codeduels.common.entity.UuidBaseEntity;
import com.codeduels.duel.state.DuelSubmission;
import com.codeduels.duel.state.PlayerScore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "duel_history", indexes = {
        @Index(name = "idx_duel_history_duel_id", columnList = "duel_id", unique = true),
        @Index(name = "idx_duel_player_one", columnList = "player_one_id"),
        @Index(name = "idx_duel_player_two", columnList = "player_two_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DuelHistory extends UuidBaseEntity {

    /** The live duel's id — what clients and the WebSocket topic know the duel by */
    @Column(name = "duel_id", nullable = false)
    private UUID duelId;

    @Column(name = "room_code", length = 6)
    private String roomCode;

    @Column(name = "player_one_id", nullable = false)
    private UUID playerOneId;

    @Column(name = "player_one_handle", nullable = false)
    private String playerOneHandle;

    @Column(name = "player_two_id")
    private UUID playerTwoId;

    @Column(name = "player_two_handle")
    private String playerTwoHandle;

    @Column(name = "player_one_solved")
    private int playerOneSolved;

    @Column(name = "player_one_penalty")
    private long playerOnePenalty;

    @Column(name = "player_two_solved")
    private int playerTwoSolved;

    @Column(name = "player_two_penalty")
    private long playerTwoPenalty;

    @Column(name = "winner_id")
    private UUID winnerId;

    @Column(name = "winner_handle")
    private String winnerHandle;

    @Column(name = "duration_minutes")
    private int durationMinutes;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "problem_ids", columnDefinition = "jsonb")
    private List<String> problemIds;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "problem_urls", columnDefinition = "jsonb")
    private List<String> problemUrls;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "problem_names", columnDefinition = "jsonb")
    private Map<String, String> problemNames;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "scoreboard", columnDefinition = "jsonb")
    private Map<String, PlayerScore> scoreboard;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "submissions", columnDefinition = "jsonb")
    private List<DuelSubmission> submissions;
}
