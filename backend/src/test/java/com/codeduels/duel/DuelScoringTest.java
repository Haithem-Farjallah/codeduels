package com.codeduels.duel;

import com.codeduels.common.security.CurrentUser;
import com.codeduels.duel.messaging.SubmissionVerdictEvent;
import com.codeduels.duel.model.DuelHistory;
import com.codeduels.duel.model.DuelStatus;
import com.codeduels.duel.repository.DuelHistoryRepository;
import com.codeduels.duel.repository.DuelStateRepository;
import com.codeduels.duel.service.CodeforcesApiService;
import com.codeduels.duel.service.DuelNotificationService;
import com.codeduels.duel.service.DuelServiceImpl;
import com.codeduels.duel.state.DuelData;
import com.codeduels.duel.state.PlayerScore;
import com.codeduels.duel.state.ProblemStats;
import com.codeduels.duel.util.CodeforcesUrlParser;
import com.codeduels.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.*;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

class DuelScoringTest {

    private final DuelStateRepository stateRepository = mock(DuelStateRepository.class);
    private final DuelHistoryRepository historyRepository = mock(DuelHistoryRepository.class);
    private final DuelNotificationService notifications = mock(DuelNotificationService.class);

    private final DuelServiceImpl service = new DuelServiceImpl(
            stateRepository, historyRepository, mock(UserRepository.class), mock(CodeforcesApiService.class),
            new CodeforcesUrlParser(), mock(CurrentUser.class), notifications, mock(SqsTemplate.class),
            new ObjectMapper());

    private static final long START = 1_000_000;
    private final UUID duelId = UUID.randomUUID();
    private DuelData duel;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        duel = DuelData.builder()
                .duelId(duelId)
                .roomCode("ABC123")
                .status(DuelStatus.ACTIVE)
                .problemIds(new ArrayList<>(List.of("100A", "100B")))
                .problemUrls(new ArrayList<>(List.of("u1", "u2")))
                .playerOneId(UUID.randomUUID())
                .playerOneHandle("alice")
                .playerTwoId(UUID.randomUUID())
                .playerTwoHandle("bob")
                .durationMinutes(60)
                .startedAtEpoch(START)
                .endsAtEpoch(START + 3_600)
                .build();
        duel.getScoreboard().put("alice", player(duel.getPlayerOneId(), "alice"));
        duel.getScoreboard().put("bob", player(duel.getPlayerTwoId(), "bob"));

        when(stateRepository.findById(duelId)).thenAnswer(inv -> Optional.of(duel));
        when(stateRepository.withLock(any(), any())).thenAnswer(inv -> ((Supplier<?>) inv.getArgument(1)).get());
        when(historyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void acceptedCostsMinutesFromStartPlusTwentyPerEarlierWrongAttempt() {
        record("alice", "100A", 1, "WRONG_ANSWER", START + 300);
        record("alice", "100A", 2, "TIME_LIMIT_EXCEEDED", START + 420);
        record("alice", "100A", 3, "OK", START + 600);                       // minute 10

        ProblemStats a = stats("alice", "100A");
        assertThat(a.isSolved()).isTrue();
        assertThat(a.getAttempts()).isEqualTo(2);
        assertThat(a.getPenalty()).isEqualTo(10 + 2 * 20);
        assertThat(duel.getScoreboard().get("alice").getTotalPenalty()).isEqualTo(50);
    }

    @Test
    void verdictsArrivingOutOfOrderScoreTheSame() {
        record("alice", "100A", 3, "OK", START + 600);
        record("alice", "100A", 1, "WRONG_ANSWER", START + 300);             // older, arrives late
        record("alice", "100A", 4, "WRONG_ANSWER", START + 900);             // after the AC — free

        assertThat(stats("alice", "100A").getAttempts()).isEqualTo(1);
        assertThat(stats("alice", "100A").getPenalty()).isEqualTo(10 + 20);
    }

    @Test
    void compilationErrorsAreNotPenalised() {
        record("alice", "100A", 1, "COMPILATION_ERROR", START + 60);
        record("alice", "100A", 2, "OK", START + 120);

        assertThat(stats("alice", "100A").getPenalty()).isEqualTo(2);
    }

    @Test
    void duplicateDeliveryIsIgnored() {
        record("alice", "100A", 1, "WRONG_ANSWER", START + 60);
        record("alice", "100A", 1, "WRONG_ANSWER", START + 60);

        assertThat(stats("alice", "100A").getAttempts()).isEqualTo(1);
        assertThat(duel.getSubmissions()).hasSize(1);
        verify(notifications, times(1)).notifyScoreboardUpdate(any(), any(), any());
    }

    @Test
    void submissionsOutsideTheWindowOrStillTestingAreIgnored() {
        record("alice", "100A", 1, "OK", START - 1);
        record("alice", "100A", 2, "OK", START + 3_601);
        record("alice", "100A", 3, "TESTING", START + 60);

        assertThat(stats("alice", "100A").isSolved()).isFalse();
        assertThat(duel.getSubmissions()).isEmpty();
    }

    @Test
    void handleMatchingIsCaseInsensitive() {
        record("ALICE", "100A", 1, "OK", START + 60);

        assertThat(stats("alice", "100A").isSolved()).isTrue();
    }

    @Test
    void moreSolvedBeatsLowerPenalty() {
        record("alice", "100A", 1, "OK", START + 3_000);
        record("alice", "100B", 2, "OK", START + 3_500);
        record("bob", "100A", 3, "OK", START + 60);

        assertThat(completeAndGetHistory().getWinnerHandle()).isEqualTo("alice");
    }

    @Test
    void equalSolvedIsDecidedByPenalty() {
        record("alice", "100A", 1, "WRONG_ANSWER", START + 30);
        record("alice", "100A", 2, "OK", START + 60);                        // 1 + 20 = 21
        record("bob", "100A", 3, "OK", START + 600);                         // 10

        DuelHistory history = completeAndGetHistory();
        assertThat(history.getWinnerHandle()).isEqualTo("bob");
        assertThat(history.getWinnerId()).isEqualTo(duel.getPlayerTwoId());
    }

    @Test
    void nothingSolvedIsADraw() {
        record("alice", "100A", 1, "WRONG_ANSWER", START + 30);

        DuelHistory history = completeAndGetHistory();
        assertThat(history.getWinnerHandle()).isNull();
        assertThat(history.getWinnerId()).isNull();
        verify(stateRepository).delete(duelId);
        verify(notifications).notifyDuelEnd(any(), any());
    }

    private DuelHistory completeAndGetHistory() {
        service.completeDuel(duelId);
        ArgumentCaptor<DuelHistory> saved = ArgumentCaptor.forClass(DuelHistory.class);
        verify(historyRepository).save(saved.capture());
        return saved.getValue();
    }

    private void record(String handle, String problemId, long submissionId, String verdict, long at) {
        service.recordSubmission(new SubmissionVerdictEvent(duelId, handle, problemId, submissionId, verdict, at));
    }

    private ProblemStats stats(String handle, String problemId) {
        return duel.getScoreboard().get(handle).getProblems().get(problemId);
    }

    private static PlayerScore player(UUID id, String handle) {
        Map<String, ProblemStats> problems = new HashMap<>();
        for (String p : List.of("100A", "100B")) {
            problems.put(p, ProblemStats.builder().problemId(p).build());
        }
        return PlayerScore.builder().userId(id).handle(handle).problems(problems).build();
    }
}
