package com.codeduels.duel.controller;

import com.codeduels.common.api.StandardResponse;
import com.codeduels.duel.dto.CreateDuelRequest;
import com.codeduels.duel.dto.CreateDuelResponse;
import com.codeduels.duel.dto.JoinDuelRequest;
import com.codeduels.duel.dto.JoinDuelResponse;
import com.codeduels.duel.service.DuelService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;


@RestController
@RequestMapping("/api/duels")
@RequiredArgsConstructor
public class DuelController {

    private final DuelService duelService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<StandardResponse<CreateDuelResponse>> create(
            @Valid @RequestBody CreateDuelRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(StandardResponse.success(duelService.createDuel(request)));
    }

    @PostMapping("/join/{roomCode}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<StandardResponse<JoinDuelResponse>> join(
            @PathVariable String roomCode,
            @Valid @RequestBody JoinDuelRequest request) {
        return ResponseEntity.ok(StandardResponse.success(
                duelService.joinDuel(roomCode, request)));
    }
}