package com.codeduels.submission.repository;

import com.codeduels.submission.model.Submission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SubmissionRepository extends JpaRepository<Submission, UUID> {
    Page<Submission> findByProblemIdAndUserIdOrderByCreatedAtDesc(UUID problemId, UUID userId, Pageable pageable);
    Page<Submission> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    List<Submission> findByMatchIdOrderByCreatedAtAsc(UUID matchId);
}
