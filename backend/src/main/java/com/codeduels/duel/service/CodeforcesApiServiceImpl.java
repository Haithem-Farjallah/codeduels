package com.codeduels.duel.service;

import com.codeduels.duel.codeforces.CfProblem;
import com.codeduels.duel.codeforces.CfProblemset;
import com.codeduels.duel.codeforces.CfResponse;
import com.codeduels.duel.codeforces.CfUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CodeforcesApiServiceImpl implements CodeforcesApiService {

    private static final Duration PROBLEMSET_TTL = Duration.ofHours(12);

    private final RestTemplate restTemplate;

    @Value("${app.codeforces.base-url}")
    private String baseUrl;

    // The full problemset is one ~2MB call — fetched lazily, kept in memory for 12h
    private volatile Map<String, String> problemNames = Map.of();
    private volatile Instant problemNamesFetchedAt = Instant.EPOCH;

    @Override
    public Optional<String> resolveHandle(String handle) {
        try {
            ResponseEntity<CfResponse<List<CfUser>>> response = restTemplate.exchange(
                    baseUrl + "/user.info?handles={handle}",
                    HttpMethod.GET, null,
                    new ParameterizedTypeReference<>() {},
                    handle);

            CfResponse<List<CfUser>> body = response.getBody();
            if (body == null || !"OK".equals(body.getStatus())
                    || body.getResult() == null || body.getResult().isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(body.getResult().get(0).getHandle());

        } catch (RestClientException e) {
            // Codeforces answers 400 with status=FAILED for unknown handles
            log.warn("Handle lookup failed for {}: {}", handle, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public Map<String, String> getProblemNames() {
        if (Instant.now().isAfter(problemNamesFetchedAt.plus(PROBLEMSET_TTL))) {
            refreshProblemNames();
        }
        return problemNames;
    }

    private synchronized void refreshProblemNames() {
        if (Instant.now().isBefore(problemNamesFetchedAt.plus(PROBLEMSET_TTL))) {
            return;                                    // another thread just refreshed
        }
        try {
            ResponseEntity<CfResponse<CfProblemset>> response = restTemplate.exchange(
                    baseUrl + "/problemset.problems",
                    HttpMethod.GET, null,
                    new ParameterizedTypeReference<>() {});

            CfResponse<CfProblemset> body = response.getBody();
            if (body == null || !"OK".equals(body.getStatus()) || body.getResult() == null) {
                log.warn("Codeforces problemset non-OK: {}", body != null ? body.getComment() : "empty body");
                return;
            }
            problemNames = body.getResult().getProblems().stream()
                    .filter(p -> p.getContestId() != null)
                    .collect(Collectors.toUnmodifiableMap(CfProblem::key, CfProblem::getName, (a, b) -> a));
            problemNamesFetchedAt = Instant.now();
            log.info("Loaded {} Codeforces problem names", problemNames.size());

        } catch (RestClientException e) {
            log.warn("Failed to fetch Codeforces problemset: {}", e.getMessage());
        }
    }
}
