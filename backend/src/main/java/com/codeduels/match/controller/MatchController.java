package com.codeduels.match.controller;

import com.codeduels.common.api.ApiPath;
import com.codeduels.common.api.StandardResponse;
import com.codeduels.match.dto.CreateMatchRequest;
import com.codeduels.match.dto.CreateMatchResponse;
import com.codeduels.match.dto.JoinMatchResponse;
import com.codeduels.match.dto.MatchHistoryItem;
import com.codeduels.match.dto.MatchResultResponse;
import com.codeduels.match.dto.MatchStateResponse;
import com.codeduels.match.service.MatchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping(ApiPath.MATCH)
@RequiredArgsConstructor
public class MatchController {

    private final MatchService matchService;


    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<StandardResponse<CreateMatchResponse>> createDuel(
            @Valid @RequestBody CreateMatchRequest matchRequest
    ) {
        CreateMatchResponse response = matchService.createMatch(matchRequest);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(StandardResponse.success(response));
    }

    @PostMapping("/join/{roomCode}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<StandardResponse<JoinMatchResponse>> joinMatch(@PathVariable String roomCode) {
        return ResponseEntity.ok(StandardResponse.success(matchService.joinMatch(roomCode)));
    }

    @GetMapping("/{matchId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<StandardResponse<MatchStateResponse>> getMatchState(@PathVariable UUID matchId) {
        return ResponseEntity.ok(StandardResponse.success(matchService.getMatchState(matchId)));
    }

    @GetMapping("/{matchId}/results")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<StandardResponse<MatchResultResponse>> getMatchResults(@PathVariable UUID matchId) {
        return ResponseEntity.ok(StandardResponse.success(matchService.getMatchResults(matchId)));
    }

    @GetMapping("/history")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<StandardResponse<Page<MatchHistoryItem>>> getMatchHistory(
            @RequestParam(required = false) String result,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {

        return ResponseEntity.ok(StandardResponse.success(
                matchService.getMatchHistory(result, pageable)));
    }
}
