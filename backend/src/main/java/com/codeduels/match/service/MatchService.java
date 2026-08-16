package com.codeduels.match.service;

import com.codeduels.match.dto.CreateMatchRequest;
import com.codeduels.match.dto.CreateMatchResponse;
import com.codeduels.match.dto.JoinMatchResponse;
import com.codeduels.match.dto.MatchHistoryItem;
import com.codeduels.match.dto.MatchResultResponse;
import com.codeduels.match.dto.MatchStateResponse;
import com.codeduels.submission.model.SubmissionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface MatchService {
    CreateMatchResponse createMatch(CreateMatchRequest createDuelRequest);
    JoinMatchResponse joinMatch(String roomCode);

    void processSubmissionResult(UUID matchId, UUID userId, SubmissionStatus status);

    MatchStateResponse getMatchState(UUID matchId);
    MatchResultResponse getMatchResults(UUID matchId);
    Page<MatchHistoryItem> getMatchHistory(String result, Pageable pageable);
}
