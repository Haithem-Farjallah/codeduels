package com.codeduels.duel.service;

import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CodeforcesApiServiceImpl implements CodeforcesApiService {

    private final RestTemplate restTemplate;

    @Value("${app.codeforces.base-url}")
    private String baseUrl;

    @Override
    public List<CfSubmission> getUserStatus(String handle, int count) {
        try {
            ResponseEntity<CfResponse<CfSubmission>> response = restTemplate.exchange(
                    baseUrl + "/user.status?handle={handle}&from=1&count={count}",
                    HttpMethod.GET, null,
                    new ParameterizedTypeReference<>() {},
                    handle, count);

            CfResponse<CfSubmission> body = response.getBody();
            if (body == null || !"OK".equals(body.getStatus())) {
                log.warn("Codeforces non-OK for {}: {}", handle,
                        body != null ? body.getComment() : "empty body");
                return List.of();
            }
            return body.getResult();

        } catch (RestClientException e) {
            log.error("Failed to fetch submissions for {}", handle, e);
            return List.of();
        }
    }

    @Override
    public boolean handleExists(String handle) {
        try {
            ResponseEntity<CfResponse<CfUser>> response = restTemplate.exchange(
                    baseUrl + "/user.info?handles={handle}",
                    HttpMethod.GET, null,
                    new ParameterizedTypeReference<>() {},
                    handle);

            CfResponse<CfUser> body = response.getBody();
            return body != null && "OK".equals(body.getStatus())
                    && body.getResult() != null && !body.getResult().isEmpty();

        } catch (RestClientException e) {
            log.warn("Handle lookup failed for {}", handle, e);
            return false;
        }
    }
}