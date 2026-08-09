package com.codeduels.judge0.service;

import com.codeduels.common.exception.ConflictException;
import com.codeduels.judge0.dto.Judge0BatchSubmissionRequest;
import com.codeduels.judge0.dto.Judge0GetBatchResponse;
import com.codeduels.judge0.dto.Judge0SubmissionRequest;
import com.codeduels.judge0.dto.Judge0SubmissionResponse;
import com.codeduels.judge0.dto.Judge0Token;
import com.codeduels.problem.model.TestCase;
import com.codeduels.submission.dto.SubmissionResult;
import com.codeduels.submission.model.Language;
import com.codeduels.submission.model.SubmissionStatus;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class Judge0Service {

    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    @Value("${app.judge0.api.base-url}")
    private String judge0ApiUrl;

    @Value("${app.judge0.api.key}")
    private String judge0ApiKey;

    @Value("${app.judge0.api.host}")
    private String judge0ApiHost;


    public SubmissionResult executeCode(String sourceCode, List<TestCase>testCases, String languageSlug, UUID matchId)  {
        String executionId = UUID.randomUUID().toString().substring(0, 8);
        String logPrefix = "[JUDGE0_EXEC "+executionId +"]";
        HttpHeaders headers = new HttpHeaders();
//        headers.set("X-RapidAPI-Key", judge0ApiKey);
//        headers.set("X-RapidAPI-Host", judge0ApiHost);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        List<Judge0SubmissionRequest>submissions = testCases.stream()
                .map(tc->new Judge0SubmissionRequest(encodeBase64(sourceCode), Language.fromSlug(languageSlug).getJudge0Id(),encodeBase64(tc.getInput()),encodeBase64(tc.getExpectedOutput())))
                .toList();

        Judge0BatchSubmissionRequest batchSubmission = new Judge0BatchSubmissionRequest(submissions);

        String JsonBody;
        try {
            JsonBody=objectMapper.writeValueAsString(batchSubmission);
        }catch (Exception e){
            throw new RuntimeException(e); //TODO: update exception
        }
        HttpEntity<String> entity= new HttpEntity<>(JsonBody,headers);

        List<String>tokens;
        try {
            ResponseEntity<List<Judge0Token>> response = restTemplate.exchange(judge0ApiUrl + "/submissions/batch?base64_encoded=true&wait=false", HttpMethod.POST, entity, new org.springframework.core.ParameterizedTypeReference<>() {});
            tokens = Objects.requireNonNull(response.getBody()).stream().map(Judge0Token::token).toList();
        } catch (RestClientException e) {
            log.error(" Failed to submit batch to Judge0.",e);
            return SubmissionResult.builder().status(SubmissionStatus.INTERNAL_ERROR).stderr("Something went wrong").build();
        }

        if (tokens.isEmpty()) {
            log.error(" No tokens received from Judge0.");
            return SubmissionResult.builder().status(SubmissionStatus.INTERNAL_ERROR).stderr("Something went wrong").build();
        }

        String tokenString = String.join(",", tokens);
        log.info(" Polling for batch results with {} tokens.", tokens.size());

        String pollUrl = UriComponentsBuilder.fromHttpUrl(judge0ApiUrl + "/submissions/batch")
                .queryParam("tokens", tokenString)
                .queryParam("base64_encoded", "true")
                .queryParam("fields", "status,stdout,stderr,compile_output,time,memory,token")
                .toUriString();

        List<Judge0SubmissionResponse> pollResults = new ArrayList<>();
        int increment =0;
        boolean allDone= false;
        try{
            while (increment ++ <20){
                Thread.sleep(300);
                ResponseEntity<Judge0GetBatchResponse> pollResponse = restTemplate.exchange(pollUrl, HttpMethod.GET, new HttpEntity<>(headers), Judge0GetBatchResponse.class);
                pollResults = Objects.requireNonNull(pollResponse.getBody()).getSubmissions();
                allDone = pollResults.stream().allMatch(r -> r.getStatus() != null && r.getStatus().getId() > 2);
                if(allDone) break;
            }

            if(!allDone)  return SubmissionResult.builder().status(SubmissionStatus.INTERNAL_ERROR).stderr("Something went wrong").build();

        }catch (Exception e){
            return SubmissionResult.builder().status(SubmissionStatus.INTERNAL_ERROR).stderr("Something went wrong").build();
        }
        List<Judge0SubmissionResponse> decodedResults = pollResults.stream()
                .map(result -> new Judge0SubmissionResponse(
                        decodeBase64(result.getStdout()),
                        decodeBase64(result.getStderr()),
                        decodeBase64(result.getCompileOutput()),
                        decodeBase64(result.getMessage()),
                        result.getTime(),
                        result.getMemory(),
                        result.getStatus(),
                        result.getToken()
                ))
                .toList();

        return aggregateResults(decodedResults, logPrefix, matchId);
    }

    private String encodeBase64(String raw) {
        return raw == null ? null
                : Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    private String decodeBase64(String encoded) {
        if (encoded == null) return null;
        try {
            return new String(Base64.getDecoder().decode(encoded));
        } catch (IllegalArgumentException e) {
            return encoded;
        }
    }

    private SubmissionResult aggregateResults(List<Judge0SubmissionResponse> results, String logPrefix, UUID matchId) {
        double maxTimeInSeconds = 0;
        int maxMemoryInKb = 0;
        for (Judge0SubmissionResponse result : results) {
            int statusId = result.getStatus().getId();
            if (statusId == 6) { // Compilation Error
                log.info("{} Found 'Compilation Error'.", logPrefix);
                return SubmissionResult.builder()
                        .status(SubmissionStatus.COMPILE_ERROR)
                        .stderr(result.getCompileOutput())
                        .build();
            }
            if (statusId > 6) { // Runtime Error
                log.info("{} Found a terminal error: '{}'.", logPrefix, result.getStatus().getDescription());
                return SubmissionResult.builder().status(SubmissionStatus.RUNTIME_ERROR).stderr(result.getStderr()).build();
            }
            if (statusId == 5) { // Time Limit Exceeded
                log.info("{} Found 'Time Limit Exceeded'.", logPrefix);
                return SubmissionResult.builder().status(SubmissionStatus.TIME_LIMIT_EXCEEDED).build();
            }
            if (statusId == 4) { // Wrong Answer
                log.info("{} Found 'Wrong Answer'.", logPrefix);
                return SubmissionResult.builder().status(SubmissionStatus.WRONG_ANSWER).stderr(result.getStderr()).stdout(result.getStdout()).build();
            }
            if (result.getTime() != null && result.getTime() > maxTimeInSeconds) maxTimeInSeconds = result.getTime();
            if (result.getMemory() != null && result.getMemory() > maxMemoryInKb) maxMemoryInKb = result.getMemory();
        }
        log.info("{} <- All test cases passed. Final result: ACCEPTED", logPrefix);
        return SubmissionResult.builder()
                .status(SubmissionStatus.ACCEPTED)
                .runtimeMs((int) (maxTimeInSeconds * 1000))
                .matchId(matchId)
                .memoryKb(maxMemoryInKb)
                .build();
    }
}
