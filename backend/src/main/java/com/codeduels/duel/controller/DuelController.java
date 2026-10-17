package com.codeduels.duel.controller;

import com.codeduels.common.api.ApiPath;
import com.codeduels.common.api.StandardResponse;
import com.codeduels.duel.dto.CreateDuelRequest;
import com.codeduels.duel.dto.CreateDuelResponse;
import com.codeduels.duel.dto.DuelHistoryItem;
import com.codeduels.duel.dto.DuelStateResponse;
import com.codeduels.duel.dto.JoinDuelRequest;
import com.codeduels.duel.dto.JoinDuelResponse;
import com.codeduels.duel.service.DuelService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping(ApiPath.DUELS)
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

    @GetMapping("/history")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<StandardResponse<Page<DuelHistoryItem>>> history(
            @PageableDefault(size = 10, sort = "endedAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return ResponseEntity.ok(StandardResponse.success(duelService.getDuelHistory(pageable)));
    }

    @GetMapping("/{duelId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<StandardResponse<DuelStateResponse>> state(@PathVariable UUID duelId) {
        return ResponseEntity.ok(StandardResponse.success(duelService.getDuelState(duelId)));
    }

    @DeleteMapping("/{duelId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<StandardResponse<Void>> cancel(@PathVariable UUID duelId) {
        duelService.cancelDuel(duelId);
        return ResponseEntity.ok(StandardResponse.success(null));
    }
}
