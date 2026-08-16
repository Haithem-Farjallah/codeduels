package com.codeduels.match.service;

import com.codeduels.match.model.Match;
import com.codeduels.match.model.MatchStatus;
import com.codeduels.match.repository.MatchRepository;
import com.codeduels.problem.model.Problem;
import com.codeduels.problem.service.ProblemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class MatchScheduler {
    private final MatchRepository matchRepository;
    private final ProblemService problemService;

    @Scheduled(fixedRate = 15000)
    @Transactional
    public void startScheduledMatches() {
        List<Match> matchesToStart = matchRepository.findAllByStatusAndScheduledAtBefore(
                MatchStatus.SCHEDULED,
                Instant.now()
        );

        if (matchesToStart.isEmpty()) {
            return;
        }

        log.info("Scheduler: {} match(es) ready to start", matchesToStart.size());

        for (Match match : matchesToStart) {
            try {
                Optional<Problem> problem = problemService.pickRandomProblem(match.getDifficulty(), match.getPlayerOneId(), match.getPlayerTwoId());

                if (problem.isEmpty()) {
                    log.warn("No {} problem available for match {}; canceling",
                            match.getDifficulty(), match.getId());
                    cancel(match);
                    // TODO(stomp): notifyMatchCanceled(match.getId(), "No suitable problem available")
                    continue;
                }

                match.setProblemId(problem.get().getId());
                match.setStatus(MatchStatus.ACTIVE);
                match.setStartedAt(Instant.now());

                log.info("Match {} started with problem {}", match.getId(), match.getProblemId());

                // TODO(stomp): notifyMatchStart(match.getId(), problemId, usernames)
                // TODO(stomp): notifyCountdownStarted(match.getId(), "MATCH_COUNTDOWN_STARTED", startedAt + duration)

            } catch (Exception e) {
                log.error("Failed to start match {}; canceling", match.getId(), e);
                cancel(match);
                // TODO(stomp): notifyMatchCanceled(match.getId(), "Internal error while starting the match")
            }
        }
    }

    private void cancel(Match match) {
        match.setStatus(MatchStatus.CANCELED);
        match.setEndedAt(Instant.now());
    }

}
