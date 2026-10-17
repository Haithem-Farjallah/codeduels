package com.codeduels.sentinel.poller;

import com.codeduels.sentinel.codeforces.CfProblem;
import com.codeduels.sentinel.codeforces.CfSubmission;
import com.codeduels.sentinel.codeforces.CodeforcesClient;
import com.codeduels.sentinel.config.SentinelProperties;
import com.codeduels.sentinel.messaging.SubmissionVerdictEvent;
import com.codeduels.sentinel.messaging.VerdictPublisher;
import com.codeduels.sentinel.store.MonitoredMatch;
import com.codeduels.sentinel.store.MonitoredMatchStore;
import com.codeduels.sentinel.store.WatchTarget;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SubmissionPollerTest {

    private final MonitoredMatchStore store = mock(MonitoredMatchStore.class);
    private final CodeforcesClient codeforces = mock(CodeforcesClient.class);
    private final VerdictPublisher publisher = mock(VerdictPublisher.class);
    private final SentinelProperties properties = new SentinelProperties();
    private final SubmissionPoller poller = new SubmissionPoller(
            store, codeforces, publisher, new BackoffPolicy(properties), properties);

    private final UUID duelId = UUID.randomUUID();
    private final WatchTarget target = new WatchTarget(duelId, "tourist");
    private long start;
    private MonitoredMatch match;

    @BeforeEach
    void setUp() {
        start = Instant.now().getEpochSecond() - 600;
        match = MonitoredMatch.builder()
                .duelId(duelId)
                .handles(List.of("tourist", "Petr"))
                .problemIds(List.of("2271E", "2271F"))
                .startEpoch(start)
                .endEpoch(start + 3_600)
                .build();
        when(store.find(duelId)).thenReturn(Optional.of(match));
        when(store.getInterval(eq(target), anyLong())).thenReturn(5L);
    }

    @Test
    void publishesOnlyNewJudgedSubmissionsOnDuelProblemsInsideTheWindow() {
        when(codeforces.getUserStatus("tourist")).thenReturn(Optional.of(List.of(
                sub(5, 2271, "F", start + 300, "OK"),
                sub(4, 2271, "E", start + 200, "WRONG_ANSWER"),
                sub(3, 2271, "E", start + 100, "OK"),                 // already reported
                sub(2, 1999, "A", start + 50, "OK"),                  // not a duel problem
                sub(1, 2271, "E", start - 10, "OK"))));               // before the duel started
        when(store.isSeen(target, 3)).thenReturn(true);

        poller.poll(target);

        ArgumentCaptor<SubmissionVerdictEvent> sent = ArgumentCaptor.forClass(SubmissionVerdictEvent.class);
        verify(publisher, times(2)).publish(sent.capture());
        assertThat(sent.getAllValues()).extracting(SubmissionVerdictEvent::getSubmissionId).containsExactly(4L, 5L);
        assertThat(sent.getAllValues().get(0).getProblemId()).isEqualTo("2271E");
        verify(store).markSeen(target, 4, match);
        verify(store).markSeen(target, 5, match);
        verify(store).setInterval(target, 5, match);                  // activity → minimum interval
    }

    @Test
    void submissionStillTestingIsNotReportedOrMarkedSeen() {
        when(codeforces.getUserStatus("tourist")).thenReturn(Optional.of(List.of(
                sub(7, 2271, "E", start + 100, "TESTING"))));

        poller.poll(target);

        verifyNoInteractions(publisher);
        verify(store, never()).markSeen(any(), anyLong(), any());
        verify(store).setInterval(target, 5, match);
    }

    @Test
    void failedPublishLeavesSubmissionUnseenForTheNextPoll() {
        when(codeforces.getUserStatus("tourist")).thenReturn(Optional.of(List.of(
                sub(8, 2271, "E", start + 100, "OK"))));
        doThrow(new RuntimeException("SQS down")).when(publisher).publish(any());

        poller.poll(target);

        verify(store, never()).markSeen(any(), anyLong(), any());
        verify(store).reschedule(eq(target), anyLong());
    }

    @Test
    void idleHandleBacksOff() {
        when(codeforces.getUserStatus("tourist")).thenReturn(Optional.of(List.of()));

        poller.poll(target);

        verify(store).setInterval(target, 10, match);
    }

    @Test
    void stopsWatchingOnceTheDuelIsOverAndNothingIsPending() {
        match.setEndEpoch(Instant.now().getEpochSecond() - 5);
        when(codeforces.getUserStatus("tourist")).thenReturn(Optional.of(List.of()));

        poller.poll(target);

        verify(store).unwatch(target);
        verify(store, never()).reschedule(any(), anyLong());
    }

    @Test
    void stopsWatchingAfterTheGracePeriodWithoutCallingCodeforces() {
        match.setEndEpoch(Instant.now().getEpochSecond() - 600);

        poller.poll(target);

        verify(store).unwatch(target);
        verifyNoInteractions(codeforces);
    }

    private static CfSubmission sub(long id, int contestId, String index, long time, String verdict) {
        CfProblem problem = new CfProblem();
        problem.setContestId(contestId);
        problem.setIndex(index);
        return CfSubmission.builder()
                .id(id).contestId(contestId).problem(problem)
                .creationTimeSeconds(time).verdict(verdict)
                .build();
    }
}
