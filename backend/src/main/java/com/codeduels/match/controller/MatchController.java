package com.codeduels.match.controller;

import com.codeduels.common.api.ApiPath;
import com.codeduels.common.api.StandardResponse;
import com.codeduels.match.dto.CreateMatchRequest;
import com.codeduels.match.dto.CreateMatchResponse;
import com.codeduels.match.dto.JoinMatchResponse;
import com.codeduels.match.service.MatchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

}
