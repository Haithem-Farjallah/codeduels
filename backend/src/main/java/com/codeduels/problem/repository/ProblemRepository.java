package com.codeduels.problem.repository;

import com.codeduels.problem.model.Problem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProblemRepository extends JpaRepository<Problem, UUID>, JpaSpecificationExecutor<Problem> {
    Optional<Problem> findBySlug(String slug);

    boolean existsBySlug(String slug);

    @Query(value = """
        SELECT p.* FROM problem p
        WHERE p.status = 'PUBLISHED'
          AND p.difficulty = :difficulty
          AND p.id NOT IN (
              SELECT s.problem_id FROM submission s
              WHERE s.status = 'ACCEPTED'
                AND s.user_id IN (:playerOneId, :playerTwoId)
          )
        ORDER BY random()
        LIMIT 1
        """, nativeQuery = true)
    Optional<Problem> pickRandomUnsolvedByEither(@Param("difficulty") String difficulty,
                                                 @Param("playerOneId") UUID playerOneId,
                                                 @Param("playerTwoId") UUID playerTwoId);
}
