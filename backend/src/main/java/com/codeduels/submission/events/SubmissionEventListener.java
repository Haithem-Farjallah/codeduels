package com.codeduels.submission.events;

import io.awspring.cloud.sqs.operations.SqsTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.UUID;

@Component
@Slf4j
@RequiredArgsConstructor
public class SubmissionEventListener {

    private final SqsTemplate sqsTemplate;
    @Value("${app.sqs.submission-queue}")
    private String queue;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSubmissionCreated(SubmissionCreatedEvent event) {
        UUID submissionId = event.getSubmissionId();
        String logPrefix = "[POST_COMMIT_SQS submissionId=" + submissionId + "]";
        log.info("{} Transaction committed. Now sending message to SQS.", logPrefix);
        try {
            sqsTemplate.send(queue,submissionId.toString());
            log.info("{} Message sent successfully.", logPrefix);
        } catch (Exception e) {
            log.error("{} CRITICAL: Failed to send SQS message post-commit.", logPrefix, e);
        }
    }
}