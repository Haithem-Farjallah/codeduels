package com.codeduels.sentinel.messaging;

import com.codeduels.sentinel.config.SentinelProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class VerdictPublisher {

    private final SqsTemplate sqsTemplate;
    private final ObjectMapper objectMapper;
    private final SentinelProperties properties;

    /** Throws on failure so the caller leaves the submission unseen and retries it next poll */
    public void publish(SubmissionVerdictEvent event) {
        try {
            sqsTemplate.send(properties.getSqs().getMatchResultQueue(), objectMapper.writeValueAsString(event));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not serialize verdict event", e);
        }
    }
}
