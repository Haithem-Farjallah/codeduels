package com.codeduels.duel.service;

import com.codeduels.common.exception.ConflictException;
import com.codeduels.common.exception.RessourceNotFoundException;
import com.codeduels.common.security.CurrentUser;
import com.codeduels.duel.dto.CreateDuelRequest;
import com.codeduels.duel.dto.CreateDuelResponse;
import com.codeduels.duel.dto.JoinDuelRequest;
import com.codeduels.duel.dto.JoinDuelResponse;
import com.codeduels.duel.model.DuelStatus;
import com.codeduels.duel.repository.DuelStateRepository;
import com.codeduels.duel.state.DuelData;
import com.codeduels.duel.util.CodeforcesUrlParser;
import io.micrometer.core.instrument.config.validate.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

// features/duel/service/DuelServiceImpl.java
@Service
@RequiredArgsConstructor
@Slf4j
public class DuelServiceImpl implements DuelService {

    private final DuelStateRepository duelStateRepository;
    private final DuelHistoryRepository duelHistoryRepository;
    private final CodeforcesApiService cfApi;
    private final CodeforcesUrlParser urlParser;
    private final CurrentUser currentUser;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    private static final long PENALTY_PER_WRONG_ATTEMPT = 20;   // minutes, ICPC standard

    // ─────────────────────────── create ───────────────────────────

    @Override
    public CreateDuelResponse createDuel(CreateDuelRequest request) {

        UUID creatorId = currentUser.getId();

        if (!cfApi.handleExists(request.getHandle())) {
            throw new ValidationException("No Codeforces user with handle " + request.getHandle());
        }

        List<String> problemIds = urlParser.parseAll(request.getProblemUrls());

        if (new HashSet<>(problemIds).size() != problemIds.size()) {
            throw new ValidationException("The same problem appears more than once");
        }

        UUID duelId = UUID.randomUUID();
        String roomCode = generateUniqueRoomCode();

        DuelData duel = DuelData.builder()
                .duelId(duelId)
                .roomCode(roomCode)
                .status(DuelStatus.WAITING_FOR_OPPONENT)
                .problemIds(problemIds)
                .problemUrls(request.getProblemUrls())
                .playerOneId(creatorId)
                .playerOneHandle(request.getHandle())
                .durationMinutes(request.getDurationMinutes())
                .startDelayMinutes(request.getStartDelayMinutes())
                .scoreboard(new HashMap<>(Map.of(
                        request.getHandle(),
                        newPlayerScore(creatorId, request.getHandle(), problemIds))))
                .build();

        long ttl = ttlFor(duel);
        duelStateRepository.save(duel, ttl);
        duelStateRepository.saveRoomCode(roomCode, duelId, ttl);

        log.info("Duel {} created by {} — room code {}", duelId, request.getHandle(), roomCode);

        return CreateDuelResponse.builder()
                .duelId(duelId)
                .roomCode(roomCode)
                .shareableLink(frontendUrl + "/duels/join?code=" + roomCode)
                .build();
    }

    @Override
    public JoinDuelResponse joinDuel(String roomCode, JoinDuelRequest request) {

        UUID joinerId = currentUser.getId();

        UUID duelId = duelStateRepository.findIdByRoomCode(roomCode.toUpperCase())
                .orElseThrow(() -> new RessourceNotFoundException("Duel room not found"));

        DuelData duel = duelStateRepository.findById(duelId)
                .orElseThrow(() -> new RessourceNotFoundException("Duel not found"));

        if (duel.getStatus() != DuelStatus.WAITING_FOR_OPPONENT) {
            throw new ConflictException("This duel is not waiting for an opponent");
        }
        if (joinerId.equals(duel.getPlayerOneId())) {
            throw new ConflictException("You cannot join a duel you created");
        }
        if (request.getHandle().equalsIgnoreCase(duel.getPlayerOneHandle())) {
            throw new ConflictException("That handle is already in this duel");
        }
        if (!cfApi.handleExists(request.getHandle())) {
            throw new ValidationException("No Codeforces user with handle " + request.getHandle());
        }

        long scheduledAt = Instant.now().getEpochSecond()
                + duel.getStartDelayMinutes() * 60L;

        duel.setPlayerTwoId(joinerId);
        duel.setPlayerTwoHandle(request.getHandle());
        duel.setStatus(DuelStatus.SCHEDULED);
        duel.setScheduledAtEpoch(scheduledAt);
        duel.getScoreboard().put(request.getHandle(),
                newPlayerScore(joinerId, request.getHandle(), duel.getProblemIds()));

        duelStateRepository.save(duel, ttlFor(duel));

        log.info("{} joined duel {} — starts at epoch {}",
                request.getHandle(), duelId, scheduledAt);

        // TODO(stomp): notifyPlayerJoined(duelId, playerTwoHandle, scheduledAt)

        return JoinDuelResponse.builder()
                .duelId(duelId)
                .scheduledAtEpoch(scheduledAt)
                .durationMinutes(duel.getDurationMinutes())
                .build();
    }

    // ──────────────────────────── state ───────────────────────────

    @Override
    public DuelStateResponse getDuelState(UUID duelId) {

        DuelData duel = duelStateRepository.findById(duelId).orElse(null);

        if (duel == null) {
            // live state gone — fall back to history
            DuelHistory history = duelHistoryRepository.findByDuelId(duelId)
                    .orElseThrow(() -> new ResourceNotFoundException("Duel not found"));
            return toStateResponse(history);
        }

        UUID callerId = currentUser.getId();
        if (!callerId.equals(duel.getPlayerOneId()) && !callerId.equals(duel.getPlayerTwoId())) {
            throw new ResourceNotFoundException("Duel not found");
        }

        return toStateResponse(duel);
    }

    // ───────────────────────── scoring ────────────────────────────

    @Override
    public void recordSubmission(UUID duelId, String handle, String problemId,
                                 String verdict, long submittedAtEpoch) {

        DuelData duel = duelStateRepository.findById(duelId).orElse(null);

        if (duel == null || duel.getStatus() != DuelStatus.ACTIVE) {
            log.warn("Ignoring submission for duel {} — not active", duelId);
            return;
        }

        PlayerScore player = duel.getScoreboard().get(handle);
        if (player == null) {
            log.warn("Handle {} is not part of duel {}", handle, duelId);
            return;
        }

        ProblemStats stats = player.getProblems().get(problemId);
        if (stats == null || stats.isSolved()) {
            return;                               // not our problem, or already solved
        }

        if ("OK".equals(verdict)) {
            long minutesFromStart = (submittedAtEpoch - duel.getStartedAtEpoch()) / 60;
            long penalty = minutesFromStart + stats.getAttempts() * PENALTY_PER_WRONG_ATTEMPT;

            stats.setSolved(true);
            stats.setSolvedAtEpoch(submittedAtEpoch);
            stats.setPenalty(penalty);

            player.setSolvedCount(player.getSolvedCount() + 1);
            player.setTotalPenalty(player.getTotalPenalty() + penalty);

            log.info("{} solved {} in duel {} — penalty {}", handle, problemId, duelId, penalty);
        } else {
            stats.setAttempts(stats.getAttempts() + 1);
            log.info("{} failed {} in duel {} ({})", handle, problemId, duelId, verdict);
        }

        duelStateRepository.save(duel, ttlFor(duel));

        // TODO(stomp): notifyScoreboardUpdate(duelId, scoreboard)
    }

    // ──────────────────────── completion ──────────────────────────

    @Override
    @Transactional
    public void completeDuel(UUID duelId) {

        DuelData duel = duelStateRepository.findById(duelId).orElse(null);
        if (duel == null || duel.getStatus() == DuelStatus.COMPLETED) {
            return;
        }

        PlayerScore p1 = duel.getScoreboard().get(duel.getPlayerOneHandle());
        PlayerScore p2 = duel.getScoreboard().get(duel.getPlayerTwoHandle());

        String winnerHandle = decideWinner(p1, p2);
        UUID winnerId = winnerHandle == null ? null
                : winnerHandle.equals(p1.getHandle()) ? p1.getUserId() : p2.getUserId();

        duel.setStatus(DuelStatus.COMPLETED);
        duel.setWinnerHandle(winnerHandle);

        duelHistoryRepository.save(DuelHistory.builder()
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
                .startedAt(Instant.ofEpochSecond(duel.getStartedAtEpoch()))
                .endedAt(Instant.now())
                .scoreboard(duel.getScoreboard())
                .build());

        log.info("Duel {} completed — winner: {}", duelId, winnerHandle != null ? winnerHandle : "DRAW");

        // TODO(stomp): notifyDuelEnd(duelId, results)

        duelStateRepository.delete(duelId);
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

    // ──────────────────────── helpers ─────────────────────────────

    private PlayerScore newPlayerScore(UUID userId, String handle, List<String> problemIds) {
        Map<String, ProblemStats> problems = new HashMap<>();
        problemIds.forEach(id ->
                problems.put(id, ProblemStats.builder().problemId(id).build()));

        return PlayerScore.builder()
                .userId(userId)
                .handle(handle)
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

    // toStateResponse(...) and toHistoryItem(...) — straightforward mapping, omitted
}
