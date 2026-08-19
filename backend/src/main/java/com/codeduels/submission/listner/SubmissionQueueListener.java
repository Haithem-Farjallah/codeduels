package com.codeduels.submission.listner;

import com.codeduels.submission.service.SubmissionService;
import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class SubmissionQueueListener {

    private final SubmissionService submissionService;

    @SqsListener("${app.sqs.submission-queue}")
    public void receiveMessage(String message) {
        UUID submissionId;
        try {
            submissionId = UUID.fromString(message);
        } catch (IllegalArgumentException e) {
            log.error("Discarding malformed SQS message: {}", message);
            return;
        }

        log.info("Processing submission {} from queue", submissionId);
        submissionService.processSubmission(submissionId);
    }
}