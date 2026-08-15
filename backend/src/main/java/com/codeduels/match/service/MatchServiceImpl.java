package com.codeduels.match.service;

import com.codeduels.common.exception.ConflictException;
import com.codeduels.common.exception.RessourceNotFoundException;
import com.codeduels.common.security.CurrentUser;
import com.codeduels.match.dto.CreateMatchRequest;
import com.codeduels.match.dto.CreateMatchResponse;
import com.codeduels.match.dto.JoinMatchResponse;
import com.codeduels.match.model.Match;
import com.codeduels.match.model.MatchStatus;
import com.codeduels.match.repository.MatchRepository;
import com.codeduels.problem.model.Problem;
import com.codeduels.problem.repository.ProblemRepository;
import com.codeduels.problem.service.ProblemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class MatchServiceImpl implements MatchService {

    private final MatchRepository matchRepository;
    private final CurrentUser currentUser;
    private final ProblemService problemService;

    @Value("${app.frontend.url}")
    private String frontendUrl;

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

        // TODO(stomp): notifyPlayerJoined(match.getId(), joinerId)
        // TODO(stomp): notifyCountdownStarted(match.getId(), "LOBBY_COUNTDOWN_STARTED", scheduledAt)

        return JoinMatchResponse.builder()
                .matchId(match.getId())
                .scheduledAt(match.getScheduledAt())
                .durationInMinutes(match.getDurationInMinutes())
                .build();
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

}
