package com.codeduels.submission.service;

import com.codeduels.common.exception.ConflictException;
import com.codeduels.common.exception.RessourceNotFoundException;
import com.codeduels.common.security.CurrentUser;
import com.codeduels.judge0.service.Judge0Service;
import com.codeduels.problem.model.Problem;
import com.codeduels.problem.model.ProblemStatus;
import com.codeduels.problem.repository.ProblemRepository;
import com.codeduels.submission.dto.SubmissionDetailsDTO;
import com.codeduels.submission.dto.SubmissionRequest;
import com.codeduels.submission.dto.SubmissionResponse;
import com.codeduels.submission.dto.SubmissionResult;
import com.codeduels.submission.model.Language;
import com.codeduels.submission.model.Submission;
import com.codeduels.submission.model.SubmissionStatus;
import com.codeduels.submission.repository.SubmissionRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SubmissionService {

    private final SubmissionRepository submissionRepository;
    private final ProblemRepository problemRepository;
    private final CurrentUser currentUser;
    private final Judge0Service judge0Service;

    @Transactional
    public SubmissionResponse create(SubmissionRequest request) {

        Problem problem = problemRepository.findById(request.getProblemId())
                .orElseThrow(() -> new RessourceNotFoundException("Problem not found"));

        if (problem.getStatus() != ProblemStatus.PUBLISHED) {
            throw new ConflictException("Submissions are only allowed for published problems");
        }
        //TODO: match validation block deferred — practice mode only for now


        Submission submission = Submission.builder()
                .userId(currentUser.getId())
                .problemId(request.getProblemId())
                .matchId(request.getMatchId())
                .code(request.getCode())
                .language(Language.fromSlug(request.getLanguage()))
                .status(SubmissionStatus.PENDING)
                .build();

        Submission savedSubmission = submissionRepository.save(submission);

        SubmissionResult submissionResult = judge0Service.executeCode(
                savedSubmission.getCode(),
                problem.getSampleTestCases(),
                savedSubmission.getLanguage().getSlug(),
                savedSubmission.getMatchId()
        );
        savedSubmission.setStatus(submissionResult.getStatus());
        savedSubmission.setRuntimeMs(submissionResult.getRuntimeMs());
        savedSubmission.setMemoryKb(submissionResult.getMemoryKb());
        savedSubmission.setStdout(submissionResult.getStdout());
        savedSubmission.setStderr(submissionResult.getStderr());

        return SubmissionResponse.builder()
                .submissionId(savedSubmission.getId())
                .build();
        //TODO: Step 2 inserts the judge call right here
    }

    @Transactional(readOnly = true)
    public Page<SubmissionResult> getUserSubmissionsForProblem(UUID problemId, Pageable pageable) {
        return submissionRepository
                .findByProblemIdAndUserIdOrderByCreatedAtDesc(problemId, currentUser.getId(), pageable)
                .map(SubmissionResult::fromSubmission);
    }

    public SubmissionDetailsDTO getSubmissionDetails(UUID submissionId){
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new EntityNotFoundException("Submission not found with id: " + submissionId));

        if (!submission.getUserId().equals(currentUser.getId())) {
            throw new RessourceNotFoundException("Submission not found");
        }
        Problem problem = problemRepository.findById(submission.getProblemId())
                .orElseThrow(() -> new EntityNotFoundException("Problem not found with id: " + submission.getProblemId()));

        return SubmissionDetailsDTO.builder()
                .id(submission.getId())
                .problemId(problem.getId())
                .matchId(submission.getMatchId())
                .problemTitle(problem.getTitle())
                .problemSlug(problem.getSlug())
                .status(submission.getStatus())
                .language(submission.getLanguage().name())
                .code(submission.getCode())
                .runtimeMs(submission.getRuntimeMs())
                .memoryKb(submission.getMemoryKb())
                .stdout(submission.getStdout())
                .stderr(submission.getStderr())
                .createdAt(submission.getCreatedAt())
                .build();
    }

}
