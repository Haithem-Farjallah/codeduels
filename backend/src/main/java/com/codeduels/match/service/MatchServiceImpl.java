package com.codeduels.match.service;

import com.codeduels.common.exception.ConflictException;
import com.codeduels.common.exception.RessourceNotFoundException;
import com.codeduels.common.security.CurrentUser;
import com.codeduels.match.dto.CreateMatchRequest;
import com.codeduels.match.dto.CreateMatchResponse;
import com.codeduels.match.dto.JoinMatchResponse;
import com.codeduels.match.dto.MatchHistoryItem;
import com.codeduels.match.dto.MatchResultResponse;
import com.codeduels.match.dto.MatchStateResponse;
import com.codeduels.match.dto.PlayerResult;
import com.codeduels.match.dto.SubmissionTimelineItem;
import com.codeduels.match.model.Match;
import com.codeduels.match.model.MatchStatus;
import com.codeduels.match.repository.MatchRepository;
import com.codeduels.problem.dto.ProblemResponse;
import com.codeduels.problem.model.Problem;
import com.codeduels.problem.repository.ProblemRepository;
import com.codeduels.problem.service.ProblemService;
import com.codeduels.submission.model.Submission;
import com.codeduels.submission.model.SubmissionStatus;
import com.codeduels.submission.repository.SubmissionRepository;
import com.codeduels.user.model.User;
import com.codeduels.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class MatchServiceImpl implements MatchService {

    private final MatchRepository matchRepository;
    private final CurrentUser currentUser;
    private final ProblemService problemService;
    private final SubmissionRepository submissionRepository;
    private final ProblemRepository problemRepository;
    private final UserRepository userRepository;
    private final MatchNotificationService matchNotificationService;


    @Value("${app.frontend.url}")
    private String frontendUrl;

    private static final int PENALTY_MINUTES=5;

    @Override
    public CreateMatchResponse createMatch(CreateMatchRequest matchRequest){

        UUID creatorId = currentUser.getId();

        String roomCode = generateUniqueRoomCode();

        Match match = Match.builder()
                .roomCode(roomCode)
                .playerOneId(creatorId)
                .status(MatchStatus.WAITING_FOR_OPPONENT)
                .difficulty(matchRequest.getDifficulty())
                .startDelayInMinutes(matchRequest.getStartDelayInMinutes())
                .durationInMinutes(matchRequest.getDurationInMinutes())
                .build();

        Match saved = matchRepository.save(match);
        log.info("Match {} created by user {} with room code {}", saved.getId(), creatorId, roomCode);

        return CreateMatchResponse.builder()
                .matchId(saved.getId())
                .roomCode(roomCode)
                .shareableLink(frontendUrl + "/match/join?roomCode=" + roomCode)
                .build();
    }

    @Override
    @Transactional
    public JoinMatchResponse joinMatch(String roomCode) {
        UUID joinerId = currentUser.getId();

        Match match = matchRepository.findByRoomCode(roomCode.toUpperCase()) // since i'm storing it in uppercase in db
                .orElseThrow(()-> new RessourceNotFoundException("Match room not found"));

        if (match.getStatus() != MatchStatus.WAITING_FOR_OPPONENT) {
            throw new ConflictException("This match is not waiting for an opponent");
        }
        if (joinerId.equals(match.getPlayerOneId())) {
            throw new ConflictException("You cannot join a match you created");
        }

        match.setPlayerTwoId(joinerId);
        match.setStatus(MatchStatus.SCHEDULED);
        Instant scheduledTime = Instant.now().plus(match.getStartDelayInMinutes(), ChronoUnit.MINUTES);
        match.setScheduledAt(scheduledTime);

        log.info("User {} joined match {}; scheduled to start at {}",
                joinerId, match.getId(), match.getScheduledAt());

       matchNotificationService.notifyPlayerJoined(match.getId(),joinerId, scheduledTime);

        return JoinMatchResponse.builder()
                .matchId(match.getId())
                .scheduledAt(match.getScheduledAt())
                .durationInMinutes(match.getDurationInMinutes())
                .build();
    }

    @Override
    @Transactional
    public void processSubmissionResult(UUID matchId, UUID userId, SubmissionStatus status) {

        Match match = matchRepository.findById(matchId).orElse(null);

        if (match == null || match.getStatus() != MatchStatus.ACTIVE) {
            log.warn("Ignoring submission result for match {} — not active", matchId);
            return;
        }

        boolean isPlayerOne = userId.equals(match.getPlayerOneId());

        if (status == SubmissionStatus.ACCEPTED) {
            Instant now = Instant.now();
            if (isPlayerOne && match.getPlayerOneFinishTime() == null) {
                match.setPlayerOneFinishTime(now);
            } else if (!isPlayerOne && match.getPlayerTwoFinishTime() == null) {
                match.setPlayerTwoFinishTime(now);
            }
            log.info("Player {} solved match {} — completing", userId, matchId);
            completeMatch(matchId);
        } else if (status == SubmissionStatus.WRONG_ANSWER
                || status == SubmissionStatus.TIME_LIMIT_EXCEEDED
                || status == SubmissionStatus.RUNTIME_ERROR) {
            if (isPlayerOne) {
                match.setPlayerOnePenalties(match.getPlayerOnePenalties() + 1);
            } else {
                match.setPlayerTwoPenalties(match.getPlayerTwoPenalties() + 1);
            }
            log.info("Player {} penalty on match {} (verdict {})", userId, matchId, status);
            matchNotificationService.notifyMatchUpdate(matchId,
                    match.getPlayerOnePenalties(),
                    match.getPlayerTwoPenalties());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public MatchStateResponse getMatchState(UUID matchId) {

        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new RessourceNotFoundException("Match not found"));

        UUID callerId = currentUser.getId();
        if (!callerId.equals(match.getPlayerOneId()) && !callerId.equals(match.getPlayerTwoId())) {
            throw new RessourceNotFoundException("Match not found");
        }

        return buildState(match);
    }

    @Override
    public MatchStateResponse buildState(Match match) {

        ProblemResponse problem = null;
        if (match.getProblemId() != null) {
            problem = problemService.getById(match.getProblemId());
        }

        long secondsRemaining = 0;
        if (match.getStatus() == MatchStatus.ACTIVE && match.getStartedAt() != null) {
            Instant deadline = match.getStartedAt()
                    .plus(match.getDurationInMinutes(), ChronoUnit.MINUTES);
            secondsRemaining = Math.max(0, Duration.between(Instant.now(), deadline).getSeconds());
        }

        return MatchStateResponse.builder()
                .matchId(match.getId())
                .status(match.getStatus())
                .problem(problem)
                .playerOneId(match.getPlayerOneId())
                .playerOnePenalties(match.getPlayerOnePenalties())
                .playerOneFinishTime(match.getPlayerOneFinishTime())
                .playerTwoId(match.getPlayerTwoId())
                .playerTwoPenalties(match.getPlayerTwoPenalties())
                .playerTwoFinishTime(match.getPlayerTwoFinishTime())
                .scheduledAt(match.getScheduledAt())
                .startedAt(match.getStartedAt())
                .durationInMinutes(match.getDurationInMinutes())
                .secondsRemaining(secondsRemaining)
                .winnerId(match.getWinnerId())
                .build();
    }

    public void completeMatch(UUID matchId) {
        Match match = matchRepository.findById(matchId).orElseThrow(()-> new RessourceNotFoundException("Match not found"));
        if(match.getStatus() == MatchStatus.COMPLETED) {
            log.info("Match {} completed", matchId);
            return;
        }
        Duration p1Time = effectiveTime(match.getStartedAt(),
                match.getPlayerOneFinishTime(), match.getPlayerOnePenalties());
        Duration p2Time = effectiveTime(match.getStartedAt(),
                match.getPlayerTwoFinishTime(), match.getPlayerTwoPenalties());

        UUID winnerId = null;
        if (p1Time != null && (p2Time == null || p1Time.compareTo(p2Time) < 0)) {
            winnerId = match.getPlayerOneId();
        } else if (p2Time != null && (p1Time == null || p2Time.compareTo(p1Time) < 0)) {
            winnerId = match.getPlayerTwoId();
        }
        match.setStatus(MatchStatus.COMPLETED);
        match.setEndedAt(Instant.now());
        match.setWinnerId(winnerId);
        log.info("Match {} completed. Winner: {}", matchId, winnerId != null ? winnerId : "DRAW");
        matchNotificationService.notifyMatchEnd(match.getId(), buildResults(match));
    }

    @Override
    @Transactional(readOnly = true)
    public MatchResultResponse getMatchResults(UUID matchId) {

        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new RessourceNotFoundException("Match not found"));

        UUID callerId = currentUser.getId();
        if (!callerId.equals(match.getPlayerOneId()) && !callerId.equals(match.getPlayerTwoId())) {
            throw new RessourceNotFoundException("Match not found");
        }

        if (match.getStatus() != MatchStatus.COMPLETED) {
            throw new ConflictException("Results are not available until the match is completed");
        }

        return buildResults(match);
    }

    private MatchResultResponse buildResults(Match match) {

        List<Submission> submissions =
                submissionRepository.findByMatchIdOrderByCreatedAtAsc(match.getId());

        String problemTitle = problemRepository.findById(match.getProblemId())
                .map(Problem::getTitle)
                .orElse("Unknown problem");

        String outcome;
        if (match.getWinnerId() == null) {
            outcome = "DRAW";
        } else if (match.getWinnerId().equals(match.getPlayerOneId())) {
            outcome = "PLAYER_ONE_WIN";
        } else {
            outcome = "PLAYER_TWO_WIN";
        }

        UUID winningSubmissionId = null;
        if (match.getWinnerId() != null) {
            winningSubmissionId = submissions.stream()
                    .filter(s -> s.getUserId().equals(match.getWinnerId()))
                    .filter(s -> s.getStatus() == SubmissionStatus.ACCEPTED)
                    .findFirst()
                    .map(Submission::getId)
                    .orElse(null);
        }

        return MatchResultResponse.builder()
                .matchId(match.getId())
                .problemId(match.getProblemId())
                .problemTitle(problemTitle)
                .outcome(outcome)
                .winnerId(match.getWinnerId())
                .winningSubmissionId(winningSubmissionId)
                .playerOne(buildPlayerResult(match, match.getPlayerOneId(),
                        match.getPlayerOneFinishTime(), match.getPlayerOnePenalties(), submissions))
                .playerTwo(buildPlayerResult(match, match.getPlayerTwoId(),
                        match.getPlayerTwoFinishTime(), match.getPlayerTwoPenalties(), submissions))
                .startedAt(match.getStartedAt())
                .endedAt(match.getEndedAt())
                .build();
    }

    private PlayerResult buildPlayerResult(Match match, UUID userId, Instant finishTime,
                                           int penalties, List<Submission> allSubmissions) {
        if (userId == null) {
            return null;
        }

        Duration effective = effectiveTime(match.getStartedAt(), finishTime, penalties);

        List<SubmissionTimelineItem> timeline = allSubmissions.stream()
                .filter(s -> s.getUserId().equals(userId))
                .map(s -> SubmissionTimelineItem.builder()
                        .submissionId(s.getId())
                        .status(s.getStatus())
                        .language(s.getLanguage().getSlug())
                        .runtimeMs(s.getRuntimeMs())
                        .createdAt(s.getCreatedAt())
                        .build())
                .toList();

        return PlayerResult.builder()
                .userId(userId)
                .solved(finishTime != null)
                .finishTime(finishTime)
                .penalties(penalties)
                .effectiveTimeSeconds(effective != null ? effective.getSeconds() : null)
                .submissions(timeline)
                .build();
    }


    private Duration effectiveTime(Instant startedAt, Instant finishTime, int penalties) {
        if (finishTime == null || startedAt == null) {
            return null;
        }
        return Duration.between(startedAt, finishTime)
                .plusMinutes(penalties * PENALTY_MINUTES);
    }


    private String generateUniqueRoomCode() {
        for (int i = 0; i < 10; i++) {
            String code = RandomStringUtils.randomAlphanumeric(6).toUpperCase();
            if (!matchRepository.existsByRoomCode(code)) {
                return code;
            }
        }
        throw new IllegalStateException("Could not generate a unique room code");
    }


    private static final Set<MatchStatus> PAST_STATUSES = EnumSet.of(
            MatchStatus.COMPLETED, MatchStatus.CANCELED, MatchStatus.EXPIRED);

    @Override
    @Transactional(readOnly = true)
    public Page<MatchHistoryItem> getMatchHistory(String result, Pageable pageable) {

        UUID userId = currentUser.getId();

        Page<Match> matches = switch (result == null ? "ALL" : result.toUpperCase()) {
            case "WIN"  -> matchRepository.findUserWins(userId, pageable);
            case "LOSS" -> matchRepository.findUserLosses(userId, pageable);
            case "DRAW" -> matchRepository.findUserDraws(userId, pageable);
            default     -> matchRepository.findUserMatches(userId, PAST_STATUSES, pageable);
        };

        if (matches.isEmpty()) {
            return matches.map(m -> null);
        }

        Set<UUID> opponentIds = matches.getContent().stream()
                .map(m -> opponentOf(m, userId))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Set<UUID> problemIds = matches.getContent().stream()
                .map(Match::getProblemId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<UUID, String> usernames = userRepository.findAllById(opponentIds).stream()
                .collect(Collectors.toMap(User::getId, User::getUsername));

        Map<UUID, String> problemTitles = problemRepository.findAllById(problemIds).stream()
                .collect(Collectors.toMap(Problem::getId, Problem::getTitle));

        return matches.map(match -> {
            UUID opponentId = opponentOf(match, userId);
            return MatchHistoryItem.builder()
                    .matchId(match.getId())
                    .status(match.getStatus())
                    .result(resultFor(match, userId))
                    .opponentId(opponentId)
                    .opponentUsername(opponentId != null ? usernames.get(opponentId) : null)
                    .problemId(match.getProblemId())
                    .problemTitle(match.getProblemId() != null
                            ? problemTitles.get(match.getProblemId()) : null)
                    .startedAt(match.getStartedAt())
                    .endedAt(match.getEndedAt())
                    .build();
        });
    }

    private UUID opponentOf(Match match, UUID userId) {
        return userId.equals(match.getPlayerOneId())
                ? match.getPlayerTwoId()
                : match.getPlayerOneId();
    }

    private String resultFor(Match match, UUID userId) {
        return switch (match.getStatus()) {
            case COMPLETED -> match.getWinnerId() == null ? "DRAW"
                    : match.getWinnerId().equals(userId) ? "WIN" : "LOSS";
            case CANCELED -> "CANCELED";
            case EXPIRED -> "EXPIRED";
            default -> "UNKNOWN";
        };
    }

}
