package com.codeduels.sentinel.codeforces;

import com.codeduels.sentinel.config.SentinelProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Optional;

/**
 * Codeforces rejects clients that call more than about once every two seconds, so every call
 * goes through one gate that spaces requests out. The budget is shared by every watched handle —
 * that's what the poller's adaptive backoff is rationing.
 */
@Component
@Slf4j
public class CodeforcesClient {

    private final RestClient restClient;
    private final SentinelProperties.Codeforces config;

    private long lastRequestAtMs = 0;

    public CodeforcesClient(SentinelProperties properties) {
        this.config = properties.getCodeforces();
        this.restClient = RestClient.builder().baseUrl(config.getBaseUrl()).build();
    }

    /**
     * Newest-first submissions of a handle. Empty means the call failed (rate limit, outage,
     * unknown handle) — distinct from an empty list, which means "no submissions".
     */
    public Optional<List<CfSubmission>> getUserStatus(String handle) {
        awaitRequestSlot();
        try {
            CfResponse<List<CfSubmission>> body = restClient.get()
                    .uri("/user.status?handle={handle}&from=1&count={count}", handle, config.getFetchCount())
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            if (body == null || !"OK".equals(body.getStatus()) || body.getResult() == null) {
                log.warn("Codeforces non-OK for {}: {}", handle, body != null ? body.getComment() : "empty body");
                return Optional.empty();
            }
            return Optional.of(body.getResult());

        } catch (RestClientException e) {
            log.warn("Codeforces call failed for {}: {}", handle, e.getMessage());
            return Optional.empty();
        }
    }

    private synchronized void awaitRequestSlot() {
        long wait = lastRequestAtMs + config.getMinRequestIntervalMs() - System.currentTimeMillis();
        if (wait > 0) {
            try {
                Thread.sleep(wait);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        lastRequestAtMs = System.currentTimeMillis();
    }
}
