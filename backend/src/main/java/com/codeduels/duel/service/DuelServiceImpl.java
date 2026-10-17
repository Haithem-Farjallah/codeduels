package com.codeduels.duel.service;

import com.codeduels.common.exception.BadRequestException;
import com.codeduels.common.exception.ConflictException;
import com.codeduels.common.exception.RessourceNotFoundException;
import com.codeduels.common.security.CurrentUser;
import com.codeduels.duel.dto.*;
import com.codeduels.duel.messaging.MatchStartEvent;
import com.codeduels.duel.messaging.SubmissionVerdictEvent;
import com.codeduels.duel.model.DuelHistory;
import com.codeduels.duel.model.DuelStatus;
import com.codeduels.duel.repository.DuelHistoryRepository;
import com.codeduels.duel.repository.DuelStateRepository;
import com.codeduels.duel.state.DuelData;
import com.codeduels.duel.state.DuelSubmission;
import com.codeduels.duel.state.PlayerScore;
import com.codeduels.duel.state.ProblemStats;
import com.codeduels.duel.util.CodeforcesUrlParser;
import com.codeduels.user.model.User;
import com.codeduels.user.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DuelServiceImpl implements DuelService {

    private final DuelStateRepository duelStateRepository;
    private final DuelHistoryRepository duelHistoryRepository;
    private final UserRepository userRepository;
    private final CodeforcesApiService cfApi;
    private final CodeforcesUrlParser urlParser;
    private final CurrentUser currentUser;
    private final DuelNotificationService duelNotificationService;
    private final SqsTemplate sqsTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Value("${app.sqs.match-watch-queue}")
    private String matchWatchQueue;

    @Value("${app.duel.completion-grace-seconds:60}")
    private long completionGraceSeconds;

    /** If the Sentinel can't be reached for this long after the scheduled start, give up on the duel */
    @Value("${app.duel.start-timeout-seconds:300}")
    private long startTimeoutSeconds;

    private static final long PENALTY_PER_WRONG_ATTEMPT = 20;   // minutes, ICPC standard

    /** Final verdicts that don't count as a wrong attempt: accepted, or not the contestant's fault */
    private static final Set<String> NOT_PENALISED = Set.of(
            "OK", "COMPILATION_ERROR", "SKIPPED", "FAILED", "CRASHED",
            "INPUT_PREPARATION_CRASHED", "REJECTED");

    // ─────────────────────────── create ───────────────────────────

    @Override
    @Transactional(readOnly = true)
    public CreateDuelResponse createDuel(CreateDuelRequest request) {

        UUID creatorId = currentUser.getId();

        String handle = cfApi.resolveHandle(request.getHandle().trim())
                .orElseThrow(() -> new BadRequestException("No Codeforces user with handle " + request.getHandle()));

        List<String> problemUrls = request.getProblemUrls().stream().map(String::trim).toList();
        List<String> problemIds = urlParser.parseAll(problemUrls);

        if (new HashSet<>(problemIds).size() != problemIds.size()) {
            throw new BadRequestException("The same problem appears more than once");
        }

        Map<String, String> allNames = cfApi.getProblemNames();
        Map<String, String> problemNames = new HashMap<>();
        problemIds.forEach(id -> {
            if (allNames.containsKey(id)) problemNames.put(id, allNames.get(id));
        });

        UUID duelId = UUID.randomUUID();
        String roomCode = generateUniqueRoomCode();

        // Mutable collections only: the Redis serializer records concrete types, and
        // List.of()/toList() immutables can't be deserialized back
        DuelData duel = DuelData.builder()
                .duelId(duelId)
                .roomCode(roomCode)
                .status(DuelStatus.WAITING_FOR_OPPONENT)
                .problemIds(new ArrayList<>(problemIds))
                .problemUrls(new ArrayList<>(problemUrls))
                .problemNames(problemNames)
                .playerOneId(creatorId)
                .playerOneHandle(handle)
                .durationMinutes(request.getDurationMinutes())
                .startDelayMinutes(request.getStartDelayMinutes())
                .scoreboard(new HashMap<>(Map.of(
                        handle, newPlayerScore(creatorId, handle, problemIds))))
                .submissions(new ArrayList<>())
                .build();

        long ttl = ttlFor(duel);
        duelStateRepository.save(duel, ttl);
        duelStateRepository.saveRoomCode(roomCode, duelId, ttl);

        log.info("Duel {} created by {} — room code {}", duelId, handle, roomCode);

        return CreateDuelResponse.builder()
                .duelId(duelId)
                .roomCode(roomCode)
                .shareableLink(frontendUrl + "/duels/join?code=" + roomCode)
                .build();
    }

    // ──────────────────────────── join ────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public JoinDuelResponse joinDuel(String roomCode, JoinDuelRequest request) {

        UUID joinerId = currentUser.getId();

        UUID duelId = duelStateRepository.findIdByRoomCode(roomCode.trim().toUpperCase())
                .orElseThrow(() -> new RessourceNotFoundException("Duel room not found"));

        // Handle lookup is a network call — do it before taking the lock
        String handle = cfApi.resolveHandle(request.getHandle().trim())
                .orElseThrow(() -> new BadRequestException("No Codeforces user with handle " + request.getHandle()));

        DuelData duel = duelStateRepository.withLock(duelId, () -> {
            DuelData current = duelStateRepository.findById(duelId)
                    .orElseThrow(() -> new RessourceNotFoundException("Duel not found"));

            if (current.getStatus() != DuelStatus.WAITING_FOR_OPPONENT) {
                throw new ConflictException("This duel is not waiting for an opponent");
            }
            if (joinerId.equals(current.getPlayerOneId())) {
                throw new ConflictException("You cannot join a duel you created");
            }
            if (handle.equalsIgnoreCase(current.getPlayerOneHandle())) {
                throw new ConflictException("That handle is already in this duel");
            }

            long scheduledAt = Instant.now().getEpochSecond() + current.getStartDelayMinutes() * 60L;

            current.setPlayerTwoId(joinerId);
            current.setPlayerTwoHandle(handle);
            current.setStatus(DuelStatus.SCHEDULED);
            current.setScheduledAtEpoch(scheduledAt);
            current.getScoreboard().put(handle, newPlayerScore(joinerId, handle, current.getProblemIds()));

            duelStateRepository.save(current, ttlFor(current));
            duelStateRepository.addToStartIndex(duelId, scheduledAt);
            return current;
        });

        log.info("{} joined duel {} — starts at epoch {}", handle, duelId, duel.getScheduledAtEpoch());

        duelNotificationService.notifyPlayerJoined(duelId, toStateResponse(duel));

        return JoinDuelResponse.builder()
                .duelId(duelId)
                .scheduledAtEpoch(duel.getScheduledAtEpoch())
                .durationMinutes(duel.getDurationMinutes())
                .build();
    }

    // ─────────────────────────── cancel ───────────────────────────

    @Override
    @Transactional
    public void cancelDuel(UUID duelId) {
        UUID callerId = currentUser.getId();

        duelStateRepository.withLock(duelId, () -> {
            DuelData duel = duelStateRepository.findById(duelId)
                    .orElseThrow(() -> new RessourceNotFoundException("Duel not found"));

            if (!callerId.equals(duel.getPlayerOneId())) {
                throw new ConflictException("Only the creator can cancel this duel");
            }
            if (duel.getStatus() != DuelStatus.WAITING_FOR_OPPONENT) {
                throw new ConflictException("Only a duel still waiting for an opponent can be canceled");
            }
            duelStateRepository.delete(duelId);
            return null;
        });

        log.info("Duel {} canceled by its creator", duelId);
        duelNotificationService.notifyDuelCanceled(duelId, "The creator canceled the duel");
    }

    // ──────────────────────────── state ───────────────────────────

    @Override
    @Transactional(readOnly = true)
    public DuelStateResponse getDuelState(UUID duelId) {

        UUID callerId = currentUser.getId();
        DuelData duel = duelStateRepository.findById(duelId).orElse(null);

        if (duel == null) {
            // live state gone — fall back to history
            DuelHistory history = duelHistoryRepository.findByDuelId(duelId)
                    .filter(h -> callerId.equals(h.getPlayerOneId()) || callerId.equals(h.getPlayerTwoId()))
                    .orElseThrow(() -> new RessourceNotFoundException("Duel not found"));
            return toStateResponse(history);
        }

        if (!callerId.equals(duel.getPlayerOneId()) && !callerId.equals(duel.getPlayerTwoId())) {
            throw new RessourceNotFoundException("Duel not found");
        }

        return toStateResponse(duel);
    }

    // ──────────────────────────── start ───────────────────────────

    @Override
    @Transactional
    public void startDuel(UUID duelId) {
        DuelStateResponse started = duelStateRepository.withLock(duelId, () -> {
            DuelData duel = duelStateRepository.findById(duelId).orElse(null);

            if (duel == null || duel.getStatus() != DuelStatus.SCHEDULED) {
                duelStateRepository.removeFromStartIndex(duelId);      // expired or already handled
                return null;
            }

            long now = Instant.now().getEpochSecond();
            long endsAt = now + duel.getDurationMinutes() * 60L;

            // Hand off to the Sentinel first: if that fails the duel stays SCHEDULED and the next tick retries
            try {
                sqsTemplate.send(matchWatchQueue, objectMapper.writeValueAsString(MatchStartEvent.builder()
                        .duelId(duelId)
                        .handles(List.of(duel.getPlayerOneHandle(), duel.getPlayerTwoHandle()))
                        .problemIds(duel.getProblemIds())
                        .startEpoch(now)
                        .endEpoch(endsAt)
                        .build()));
            } catch (JsonProcessingException | RuntimeException e) {
                if (now - duel.getScheduledAtEpoch() > startTimeoutSeconds) {
                    log.error("Duel {} could not be handed to the Sentinel for {}s; canceling",
                            duelId, startTimeoutSeconds, e);
                    duelStateRepository.delete(duelId);
                    duelNotificationService.notifyDuelCanceled(duelId, "Could not start the duel — please create a new one");
                } else {
                    log.warn("Failed to send MatchStartEvent for duel {}; will retry", duelId, e);
                }
                return null;
            }

            duel.setStatus(DuelStatus.ACTIVE);
            duel.setStartedAtEpoch(now);
            duel.setEndsAtEpoch(endsAt);
            duelStateRepository.save(duel, ttlFor(duel));

            duelStateRepository.removeFromStartIndex(duelId);
            // Completion waits a grace period so verdicts for last-second submissions can still land
            duelStateRepository.addToEndIndex(duelId, endsAt + completionGraceSeconds);

            log.info("Duel {} started — ends at epoch {}", duelId, endsAt);
            return toStateResponse(duel);
        });

        if (started != null) {
            duelNotificationService.notifyDuelStart(duelId, started);
        }
    }

    // ───────────────────────── scoring ────────────────────────────

    @Override
    @Transactional
    public void recordSubmission(SubmissionVerdictEvent event) {
        UUID duelId = event.getDuelId();
        String verdict = event.getVerdict();

        if (verdict == null || "TESTING".equals(verdict)) {
            log.warn("Ignoring non-final verdict for submission {} in duel {}", event.getSubmissionId(), duelId);
            return;
        }

        duelStateRepository.withLock(duelId, () -> {
            DuelData duel = duelStateRepository.findById(duelId).orElse(null);

            if (duel == null || duel.getStatus() != DuelStatus.ACTIVE) {
                log.warn("Ignoring submission for duel {} — not active", duelId);
                return null;
            }

            long submittedAt = event.getSubmittedAtEpoch();
            if (submittedAt < duel.getStartedAtEpoch() || submittedAt > duel.getEndsAtEpoch()) {
                log.warn("Ignoring submission {} in duel {} — outside the duel window",
                        event.getSubmissionId(), duelId);
                return null;
            }

            boolean alreadyRecorded = duel.getSubmissions().stream()
                    .anyMatch(s -> s.getSubmissionId() == event.getSubmissionId());
            if (alreadyRecorded) {
                log.debug("Duplicate delivery of submission {} for duel {}", event.getSubmissionId(), duelId);
                return null;
            }

            PlayerScore player = findPlayer(duel, event.getHandle());
            if (player == null) {
                log.warn("Handle {} is not part of duel {}", event.getHandle(), duelId);
                return null;
            }

            ProblemStats stats = player.getProblems().get(event.getProblemId());
            if (stats == null) {
                log.warn("Problem {} is not part of duel {}", event.getProblemId(), duelId);
                return null;
            }

            DuelSubmission submission = DuelSubmission.builder()
                    .submissionId(event.getSubmissionId())
                    .handle(player.getHandle())
                    .problemId(event.getProblemId())
                    .verdict(verdict)
                    .submittedAtEpoch(submittedAt)
                    .build();
            duel.getSubmissions().add(submission);

            if ("OK".equals(verdict)) {
                // keep the earliest accepted, whichever order the verdicts arrived in
                if (stats.getSolvedAtEpoch() == null || submittedAt < stats.getSolvedAtEpoch()) {
                    stats.setSolvedAtEpoch(submittedAt);
                }
            } else if (isPenalised(verdict)) {
                stats.getWrongAttemptEpochs().add(submittedAt);
            }

            recomputeScore(player, duel.getStartedAtEpoch());
            duelStateRepository.save(duel, ttlFor(duel));

            log.info("{} → {} on {} in duel {} (solved {}, penalty {})", player.getHandle(), verdict,
                    event.getProblemId(), duelId, player.getSolvedCount(), player.getTotalPenalty());

            duelNotificationService.notifyScoreboardUpdate(duelId, toPlayerDto(player), toSubmissionDto(submission));
            return null;
        });
    }

    /**
     * ICPC: each solved problem costs minutes-from-start of the first accepted submission plus
     * 20 minutes per wrong attempt made before it. Unsolved problems cost nothing.
     */
    private void recomputeScore(PlayerScore player, long startedAtEpoch) {
        int solvedCount = 0;
        long totalPenalty = 0;

        for (ProblemStats stats : player.getProblems().values()) {
            Long solvedAt = stats.getSolvedAtEpoch();
            if (solvedAt == null) {
                stats.setSolved(false);
                stats.setAttempts(stats.getWrongAttemptEpochs().size());
                stats.setPenalty(null);
                continue;
            }
            int attemptsBefore = (int) stats.getWrongAttemptEpochs().stream().filter(t -> t < solvedAt).count();
            long penalty = (solvedAt - startedAtEpoch) / 60 + attemptsBefore * PENALTY_PER_WRONG_ATTEMPT;

            stats.setSolved(true);
            stats.setAttempts(attemptsBefore);
            stats.setPenalty(penalty);

            solvedCount++;
            totalPenalty += penalty;
        }

        player.setSolvedCount(solvedCount);
        player.setTotalPenalty(totalPenalty);
    }

    private static boolean isPenalised(String verdict) {
        return !NOT_PENALISED.contains(verdict);
    }

    // ──────────────────────── completion ──────────────────────────

    // Deliberately not @Transactional: the history row must be committed by save() before the
    // live Redis state is deleted, otherwise a failed commit would lose the duel entirely
    @Override
    public void completeDuel(UUID duelId) {

        DuelStateResponse finalState = duelStateRepository.withLock(duelId, () -> {
            DuelData duel = duelStateRepository.findById(duelId).orElse(null);
            if (duel == null || duel.getStatus() != DuelStatus.ACTIVE) {
                duelStateRepository.removeFromEndIndex(duelId);
                return null;
            }

            if (duelHistoryRepository.existsByDuelId(duelId)) {
                // archived on a previous attempt that died before cleaning up Redis
                duelStateRepository.delete(duelId);
                return null;
            }

            PlayerScore p1 = duel.getScoreboard().get(duel.getPlayerOneHandle());
            PlayerScore p2 = duel.getScoreboard().get(duel.getPlayerTwoHandle());

            String winnerHandle = decideWinner(p1, p2);
            UUID winnerId = winnerHandle == null ? null
                    : winnerHandle.equals(p1.getHandle()) ? p1.getUserId() : p2.getUserId();

            duel.setStatus(DuelStatus.COMPLETED);
            duel.setWinnerHandle(winnerHandle);

            DuelHistory history = duelHistoryRepository.save(DuelHistory.builder()
                    .duelId(duelId)
                    .roomCode(duel.getRoomCode())
                    .playerOneId(duel.getPlayerOneId())
                    .playerOneHandle(duel.getPlayerOneHandle())
                    .playerTwoId(duel.getPlayerTwoId())
                    .playerTwoHandle(duel.getPlayerTwoHandle())
                    .playerOneSolved(p1.getSolvedCount())
                    .playerOnePenalty(p1.getTotalPenalty())
                    .playerTwoSolved(p2 == null ? 0 : p2.getSolvedCount())
                    .playerTwoPenalty(p2 == null ? 0 : p2.getTotalPenalty())
                    .winnerId(winnerId)
                    .winnerHandle(winnerHandle)
                    .durationMinutes(duel.getDurationMinutes())
                    .startedAt(Instant.ofEpochSecond(duel.getStartedAtEpoch()))
                    .endedAt(Instant.ofEpochSecond(duel.getEndsAtEpoch()))
                    .problemIds(duel.getProblemIds())
                    .problemUrls(duel.getProblemUrls())
                    .problemNames(duel.getProblemNames())
                    .scoreboard(duel.getScoreboard())
                    .submissions(duel.getSubmissions())
                    .build());

            log.info("Duel {} completed — winner: {}", duelId, winnerHandle != null ? winnerHandle : "DRAW");

            duelStateRepository.delete(duelId);
            return toStateResponse(history);
        });

        if (finalState != null) {
            duelNotificationService.notifyDuelEnd(duelId, finalState);
        }
    }

    private String decideWinner(PlayerScore p1, PlayerScore p2) {
        if (p1 == null || p2 == null) return null;

        if (p1.getSolvedCount() > p2.getSolvedCount()) return p1.getHandle();
        if (p2.getSolvedCount() > p1.getSolvedCount()) return p2.getHandle();

        if (p1.getSolvedCount() == 0) return null;                       // both solved nothing

        if (p1.getTotalPenalty() < p2.getTotalPenalty()) return p1.getHandle();
        if (p2.getTotalPenalty() < p1.getTotalPenalty()) return p2.getHandle();

        return null;                                                      // identical — draw
    }

    // ──────────────────────── history ─────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public Page<DuelHistoryItem> getDuelHistory(Pageable pageable) {
        UUID userId = currentUser.getId();
        return duelHistoryRepository
                .findByPlayerOneIdOrPlayerTwoId(userId, userId, pageable)
                .map(history -> toHistoryItem(history, userId));
    }

    // ──────────────────────── mapping ─────────────────────────────

    private DuelStateResponse toStateResponse(DuelData duel) {
        long now = Instant.now().getEpochSecond();
        long secondsRemaining = switch (duel.getStatus()) {
            case SCHEDULED -> Math.max(0, duel.getScheduledAtEpoch() - now);
            case ACTIVE -> Math.max(0, duel.getEndsAtEpoch() - now);
            default -> 0;
        };

        return DuelStateResponse.builder()
                .duelId(duel.getDuelId())
                .roomCode(duel.getRoomCode())
                .status(duel.getStatus())
                .problems(toProblemDtos(duel.getProblemIds(), duel.getProblemUrls(), duel.getProblemNames()))
                .playerOne(toPlayerDto(duel.getScoreboard().get(duel.getPlayerOneHandle())))
                .playerTwo(toPlayerDto(findPlayer(duel, duel.getPlayerTwoHandle())))
                .scheduledAtEpoch(duel.getScheduledAtEpoch())
                .startedAtEpoch(duel.getStartedAtEpoch())
                .endsAtEpoch(duel.getEndsAtEpoch())
                .durationMinutes(duel.getDurationMinutes())
                .startDelayMinutes(duel.getStartDelayMinutes())
                .secondsRemaining(secondsRemaining)
                .winnerHandle(duel.getWinnerHandle())
                .submissions(toSubmissionDtos(duel.getSubmissions()))
                .build();
    }

    private DuelStateResponse toStateResponse(DuelHistory history) {
        Map<String, PlayerScore> scoreboard = history.getScoreboard() != null ? history.getScoreboard() : Map.of();

        return DuelStateResponse.builder()
                .duelId(history.getDuelId())
                .roomCode(history.getRoomCode())
                .status(DuelStatus.COMPLETED)
                .problems(toProblemDtos(history.getProblemIds(), history.getProblemUrls(), history.getProblemNames()))
                .playerOne(toPlayerDto(scoreboard.get(history.getPlayerOneHandle())))
                .playerTwo(history.getPlayerTwoHandle() == null ? null
                        : toPlayerDto(scoreboard.get(history.getPlayerTwoHandle())))
                .startedAtEpoch(history.getStartedAt() != null ? history.getStartedAt().getEpochSecond() : null)
                .endsAtEpoch(history.getEndedAt() != null ? history.getEndedAt().getEpochSecond() : null)
                .durationMinutes(history.getDurationMinutes())
                .secondsRemaining(0)
                .winnerHandle(history.getWinnerHandle())
                .submissions(toSubmissionDtos(history.getSubmissions()))
                .build();
    }

    private List<DuelProblemDto> toProblemDtos(List<String> ids, List<String> urls, Map<String, String> names) {
        if (ids == null) return List.of();
        Map<String, String> safeNames = names != null ? names : Map.of();
        List<DuelProblemDto> problems = new ArrayList<>();
        for (int i = 0; i < ids.size(); i++) {
            String id = ids.get(i);
            problems.add(DuelProblemDto.builder()
                    .problemId(id)
                    .url(urls != null && i < urls.size() ? urls.get(i) : null)
                    .name(safeNames.getOrDefault(id, id))
                    .build());
        }
        return problems;
    }

    private PlayerScoreDto toPlayerDto(PlayerScore score) {
        if (score == null) return null;

        Map<String, ProblemStatusDto> problems = score.getProblems().values().stream()
                .collect(Collectors.toMap(ProblemStats::getProblemId, stats -> ProblemStatusDto.builder()
                        .solved(stats.isSolved())
                        .attempts(stats.getAttempts())
                        .penalty(stats.getPenalty())
                        .solvedAtEpoch(stats.getSolvedAtEpoch())
                        .build()));

        return PlayerScoreDto.builder()
                .userId(score.getUserId())
                .handle(score.getHandle())
                .username(score.getUsername())
                .solvedCount(score.getSolvedCount())
                .totalPenalty(score.getTotalPenalty())
                .problems(problems)
                .build();
    }

    private List<DuelSubmissionDto> toSubmissionDtos(List<DuelSubmission> submissions) {
        if (submissions == null) return List.of();
        return submissions.stream()
                .sorted(Comparator.comparingLong(DuelSubmission::getSubmittedAtEpoch).reversed())
                .map(this::toSubmissionDto)
                .toList();
    }

    private DuelSubmissionDto toSubmissionDto(DuelSubmission submission) {
        return DuelSubmissionDto.builder()
                .submissionId(submission.getSubmissionId())
                .handle(submission.getHandle())
                .problemId(submission.getProblemId())
                .verdict(submission.getVerdict())
                .submittedAtEpoch(submission.getSubmittedAtEpoch())
                .penalised(!"OK".equals(submission.getVerdict()) && isPenalised(submission.getVerdict()))
                .build();
    }

    private DuelHistoryItem toHistoryItem(DuelHistory history, UUID userId) {
        boolean isPlayerOne = userId.equals(history.getPlayerOneId());

        String result = history.getWinnerId() == null ? "DRAW"
                : history.getWinnerId().equals(userId) ? "WIN" : "LOSS";

        String opponentHandle = isPlayerOne ? history.getPlayerTwoHandle() : history.getPlayerOneHandle();
        PlayerScore opponent = history.getScoreboard() != null && opponentHandle != null
                ? history.getScoreboard().get(opponentHandle) : null;

        return DuelHistoryItem.builder()
                .duelId(history.getDuelId())
                .result(result)
                .yourHandle(isPlayerOne ? history.getPlayerOneHandle() : history.getPlayerTwoHandle())
                .opponentHandle(opponentHandle)
                .opponentUsername(opponent != null ? opponent.getUsername() : null)
                .problemCount(history.getProblemIds() != null ? history.getProblemIds().size() : 0)
                .yourSolved(isPlayerOne ? history.getPlayerOneSolved() : history.getPlayerTwoSolved())
                .yourPenalty(isPlayerOne ? history.getPlayerOnePenalty() : history.getPlayerTwoPenalty())
                .opponentSolved(isPlayerOne ? history.getPlayerTwoSolved() : history.getPlayerOneSolved())
                .opponentPenalty(isPlayerOne ? history.getPlayerTwoPenalty() : history.getPlayerOnePenalty())
                .startedAt(history.getStartedAt())
                .endedAt(history.getEndedAt())
                .build();
    }

    // ──────────────────────── helpers ─────────────────────────────

    /** Codeforces handles are case-insensitive; the scoreboard is keyed by the canonical spelling */
    private PlayerScore findPlayer(DuelData duel, String handle) {
        if (handle == null) return null;
        PlayerScore exact = duel.getScoreboard().get(handle);
        if (exact != null) return exact;
        return duel.getScoreboard().values().stream()
                .filter(p -> p.getHandle().equalsIgnoreCase(handle))
                .findFirst()
                .orElse(null);
    }

    private PlayerScore newPlayerScore(UUID userId, String handle, List<String> problemIds) {
        Map<String, ProblemStats> problems = new HashMap<>();
        problemIds.forEach(id ->
                problems.put(id, ProblemStats.builder().problemId(id).build()));

        String username = userRepository.findById(userId).map(User::getUsername).orElse(null);

        return PlayerScore.builder()
                .userId(userId)
                .handle(handle)
                .username(username)
                .problems(problems)
                .build();
    }

    private long ttlFor(DuelData duel) {
        return duel.getStartDelayMinutes() + duel.getDurationMinutes() + 30L;
    }

    private String generateUniqueRoomCode() {
        for (int i = 0; i < 10; i++) {
            String code = RandomStringUtils.secure().nextAlphanumeric(6).toUpperCase();
            if (!duelStateRepository.existsByRoomCode(code)) {
                return code;
            }
        }
        throw new IllegalStateException("Could not generate a unique room code");
    }
}
