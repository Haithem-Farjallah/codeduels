package com.codeduels.match.service;

import com.codeduels.match.dto.CreateMatchRequest;
import com.codeduels.match.dto.CreateMatchResponse;
import com.codeduels.match.dto.JoinMatchResponse;

public interface MatchService {
    CreateMatchResponse createMatch(CreateMatchRequest createDuelRequest);
    JoinMatchResponse joinMatch(String roomCode);
}
