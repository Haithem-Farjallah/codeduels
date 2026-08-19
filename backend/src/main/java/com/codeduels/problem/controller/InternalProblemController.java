package com.codeduels.problem.controller;

import com.codeduels.problem.service.ProblemService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/internal/problems")
@RequiredArgsConstructor
public class InternalProblemController {
    private final ProblemService problemService;

    @PostMapping("/{problemId}/finalize")
    public ResponseEntity<Void> finalizeProblem(
            @PathVariable UUID problemId,
            @RequestHeader("X-Internal-Secret") String secret) {

        problemService.finalizeProblem(problemId, secret);
        return ResponseEntity.ok().build();
    }
}