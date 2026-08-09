package com.codeduels.submission.controller;

import com.codeduels.common.api.ApiPath;
import com.codeduels.common.api.StandardResponse;
import com.codeduels.submission.dto.SubmissionDetailsDTO;
import com.codeduels.submission.dto.SubmissionRequest;
import com.codeduels.submission.dto.SubmissionResponse;
import com.codeduels.submission.dto.SubmissionResult;
import com.codeduels.submission.service.SubmissionService;
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
@RequestMapping(ApiPath.SUBMISSIONS)
@RequiredArgsConstructor
public class SubmissionController {

    private final SubmissionService submissionService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<StandardResponse<SubmissionResponse>> submit(
            @Valid @RequestBody SubmissionRequest request
           ) {
        SubmissionResponse created = submissionService.create(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(StandardResponse.success(created));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<StandardResponse<Page<SubmissionResult>>> getSubmissions(
            @RequestParam UUID problemId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {

        return ResponseEntity.ok(StandardResponse.success(
                submissionService.getUserSubmissionsForProblem(problemId, pageable)));
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping(ApiPath.PATH_ID)
    public ResponseEntity<StandardResponse<SubmissionDetailsDTO>> getSubmissionById(@PathVariable UUID id) {
        SubmissionDetailsDTO submissionDetails = submissionService.getSubmissionDetails(id);

        return  ResponseEntity.ok(StandardResponse.success(
                submissionDetails
        ));
    }
}
