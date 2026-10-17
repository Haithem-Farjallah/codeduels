package com.codeduels.duel.listener;

import com.codeduels.duel.messaging.SubmissionVerdictEvent;
import com.codeduels.duel.service.DuelService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Verdicts the Sentinel found on Codeforces. Exceptions propagate so the message isn't
 * acknowledged and SQS redelivers it; recordSubmission is idempotent per submission id.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MatchResultQueueListener {

    private final DuelService duelService;
    private final ObjectMapper objectMapper;

    @SqsListener("${app.sqs.match-result-queue}")
    public void receiveMessage(String message) {
        SubmissionVerdictEvent event;
        try {
            event = objectMapper.readValue(message, SubmissionVerdictEvent.class);
        } catch (JsonProcessingException e) {
            log.error("Discarding malformed verdict message: {}", message);
            return;
        }

        if (event.getDuelId() == null || event.getHandle() == null || event.getProblemId() == null) {
            log.error("Discarding incomplete verdict message: {}", message);
            return;
        }

        log.info("Verdict {} for {} on {} (duel {})",
                event.getVerdict(), event.getHandle(), event.getProblemId(), event.getDuelId());
        duelService.recordSubmission(event);
    }
}
