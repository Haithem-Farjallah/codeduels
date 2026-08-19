package com.codeduels.submission.service;

import com.codeduels.common.AWS.service.S3Service;
import com.codeduels.common.exception.ConflictException;
import com.codeduels.common.exception.RessourceNotFoundException;
import com.codeduels.common.security.CurrentUser;
import com.codeduels.judge0.service.Judge0Service;
import com.codeduels.match.model.Match;
import com.codeduels.match.model.MatchStatus;
import com.codeduels.match.repository.MatchRepository;
import com.codeduels.match.service.MatchService;
import com.codeduels.notification.NotificationService;
import com.codeduels.problem.model.Problem;
import com.codeduels.problem.model.ProblemStatus;
import com.codeduels.problem.model.TestCase;
import com.codeduels.problem.repository.ProblemRepository;
import com.codeduels.problem.service.ProblemService;
import com.codeduels.submission.dto.SubmissionDetailsDTO;
import com.codeduels.submission.dto.SubmissionRequest;
import com.codeduels.submission.dto.SubmissionResponse;
import com.codeduels.submission.dto.SubmissionResult;
import com.codeduels.submission.events.SubmissionCreatedEvent;
import com.codeduels.submission.model.Language;
import com.codeduels.submission.model.Submission;
import com.codeduels.submission.model.SubmissionStatus;
import com.codeduels.submission.repository.SubmissionRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubmissionService {

    private final SubmissionRepository submissionRepository;
    private final ProblemRepository problemRepository;
    private final CurrentUser currentUser;
    private final Judge0Service judge0Service;
    private final MatchRepository matchRepository;
    private final MatchService matchService;
    private final ApplicationEventPublisher eventPublisher;
    private final NotificationService notificationService;
    private final S3Service s3Service;
    private final ProblemService problemService;


    @Transactional
    public SubmissionResponse create(SubmissionRequest request) {

        Problem problem = problemRepository.findById(request.getProblemId())
                .orElseThrow(() -> new RessourceNotFoundException("Problem not found"));

        if (problem.getStatus() != ProblemStatus.PUBLISHED) {
            throw new ConflictException("Submissions are only allowed for published problems");
        }
        Match match = matchRepository.findById(request.getMatchId())
                .orElseThrow(() -> new RessourceNotFoundException("Match not found"));
        if (match.getStatus() != MatchStatus.ACTIVE) {
            throw new ConflictException("This match is not active");
        }
        UUID userId = currentUser.getId();
        if (!userId.equals(match.getPlayerOneId()) && !userId.equals(match.getPlayerTwoId())) {
            throw new ConflictException("You are not a participant in this match");
        }
        if (!request.getProblemId().equals(match.getProblemId())) {
            throw new ConflictException("This problem does not belong to the match");
        }
        Submission submission = Submission.builder()
                .userId(currentUser.getId())
                .problemId(request.getProblemId())
                .matchId(request.getMatchId())
                .code(request.getCode())
                .language(Language.fromSlug(request.getLanguage()))
                .status(SubmissionStatus.PENDING)
                .build();

        Submission savedSubmission = submissionRepository.save(submission);

        eventPublisher.publishEvent(new SubmissionCreatedEvent(this,savedSubmission.getId()));

        return SubmissionResponse.builder()
                .submissionId(savedSubmission.getId())
                .build();
    }

    @Transactional
    public void processSubmission(UUID submissionId) {

        Submission submission = submissionRepository.findById(submissionId).orElse(null);
        if (submission == null) {
            log.error("Submission {} not found — aborting", submissionId);
            return;
        }

        submission.setStatus(SubmissionStatus.PROCESSING);

        try {
            Problem problem = problemRepository.findById(submission.getProblemId())
                    .orElseThrow(() -> new IllegalStateException(
                            "Problem not found: " + submission.getProblemId()));

            List<TestCase> allTestCases = problemService.getAllTestCases(problem);

            if (allTestCases.isEmpty()) {
                throw new IllegalStateException("No test cases for problem " + problem.getId());
            }

            SubmissionResult result = judge0Service.executeCode(
                    submission.getCode(),
                    allTestCases,
                    submission.getLanguage().getSlug(),
                    submission.getMatchId());

            submission.setStatus(result.getStatus());
            submission.setRuntimeMs(result.getRuntimeMs());
            submission.setMemoryKb(result.getMemoryKb());
            submission.setStdout(result.getStdout());
            submission.setStderr(result.getStderr());

            log.info("Submission {} judged: {}", submissionId, result.getStatus());

            matchService.processSubmissionResult(
                    submission.getMatchId(),
                    submission.getUserId(),
                    submission.getStatus());

            notificationService.notifyUser(
                    submission.getId(),
                    SubmissionResult.fromSubmission(submission)
            );

        }  catch (Exception e) {
            log.error("Judging failed for submission {}", submissionId, e);
            submission.setStatus(SubmissionStatus.INTERNAL_ERROR);
            submission.setStderr("Something went wrong");

            notificationService.notifyUser(                        // just in case the judge0 is down or some error happened
                    submission.getId(),
                    SubmissionResult.fromSubmission(submission));
        }
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
