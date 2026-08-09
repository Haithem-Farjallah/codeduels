package com.codeduels.problem.controller;

import com.codeduels.common.api.ApiPath;
import com.codeduels.common.api.StandardResponse;
import com.codeduels.common.security.Authz;
import com.codeduels.problem.dto.ProblemFilter;
import com.codeduels.problem.dto.ProblemRequest;
import com.codeduels.problem.dto.ProblemResponse;
import com.codeduels.problem.dto.ProblemSummaryResponse;
import com.codeduels.problem.service.ProblemService;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping(ApiPath.PROBLEMS)
@RequiredArgsConstructor
@RestController
public class ProblemController {
    private final ProblemService problemService;

    @PreAuthorize(Authz.PROBLEM_READ)
    @GetMapping(ApiPath.PATH_SLUG)
    public ResponseEntity<StandardResponse<ProblemResponse>> getProblem(
            @PathVariable String slug) {
            return ResponseEntity.ok(StandardResponse.success(
                   problemService.getProblemBySlug(slug)
            ));
    }

    @PreAuthorize(Authz.PROBLEM_READ)
    @GetMapping
    public ResponseEntity<StandardResponse<Page<ProblemSummaryResponse>>> listProblems(
            ProblemFilter filter,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return ResponseEntity.ok(StandardResponse.success(problemService.getFilteredProblems(filter, pageable)));
    }

    @PreAuthorize(Authz.PROBLEM_CREATE)
    @PostMapping
    public ResponseEntity<StandardResponse<ProblemResponse>> createProblem(
            @Valid @RequestBody ProblemRequest problemRequest
            ){
        return ResponseEntity.status(HttpStatus.CREATED).body(StandardResponse.success(problemService.createProblem(problemRequest)));
    }

    @PreAuthorize(Authz.PROBLEM_UPDATE)
    @PutMapping(ApiPath.PATH_SLUG)
    public ResponseEntity<StandardResponse<ProblemResponse>> updateProblem(
            @PathVariable String slug,
            @Valid @RequestBody ProblemRequest request) {

        return ResponseEntity.ok(
                StandardResponse.success(problemService.updateProblem(slug, request)));
    }

    @PreAuthorize(Authz.PROBLEM_DELETE)
    @DeleteMapping(ApiPath.PATH_SLUG)
    public ResponseEntity<StandardResponse<Void>> deleteProblem(@PathVariable String slug) {
        problemService.delete(slug);
        return ResponseEntity.ok(StandardResponse.success(null));
    }

    
}
