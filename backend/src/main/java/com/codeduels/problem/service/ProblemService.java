package com.codeduels.problem.service;

import com.codeduels.auth.model.PermissionReference;
import com.codeduels.common.AWS.service.S3Service;
import com.codeduels.common.exception.ConflictException;
import com.codeduels.common.exception.RessourceNotFoundException;
import com.codeduels.common.exception.UnauthorisedException;
import com.codeduels.problem.dto.ProblemCreatedResponse;
import com.codeduels.problem.dto.ProblemFilter;
import com.codeduels.problem.dto.ProblemRequest;
import com.codeduels.problem.dto.ProblemResponse;
import com.codeduels.problem.dto.ProblemSummaryResponse;
import com.codeduels.problem.dto.TestCaseDto;
import com.codeduels.problem.model.Difficulty;
import com.codeduels.problem.model.Problem;
import com.codeduels.problem.model.ProblemStatus;
import com.codeduels.problem.model.Tag;
import com.codeduels.problem.model.TestCase;
import com.codeduels.problem.repository.ProblemRepository;
import com.codeduels.problem.repository.ProblemSpecifications;
import com.codeduels.problem.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProblemService {
    private final ProblemRepository problemRepository;
    private final TagRepository tagRepository;
    private final S3Service s3Service;
    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${app.internal.secret}")
    private String internalSecret;
    @Value("${app.cache.test-cases-ttl-minutes}")
    private long ttlMinutes;

    private static final String TEST_CASES_KEY_PREFIX = "testcases:problem:";

    public ProblemResponse getProblemBySlug(String slug){
        Problem problem = problemRepository.findBySlug(slug)
                .orElseThrow(()->new RessourceNotFoundException("Problem not found"));
        if(problem.getStatus() == ProblemStatus.DRAFT && !canGetDrafts()){
            throw new RessourceNotFoundException("Problem not found");
        }
        return ProblemResponse.fromEntity(problem);
    }

    public Page<ProblemSummaryResponse> getFilteredProblems(ProblemFilter filter , Pageable pageable)  {
        ProblemStatus status = canGetDrafts() ? filter.getStatus() : ProblemStatus.PUBLISHED;

        Specification<Problem> specification = Specification
                .where(ProblemSpecifications.hasStatus(status))
                .and(ProblemSpecifications.hasDifficulty(filter.getDifficulty()))
                .and(ProblemSpecifications.likeTitle(filter.getTitle()))
                .and(ProblemSpecifications.slugContains(filter.getSlug()))
                .and(ProblemSpecifications.minPoints(filter.getMinPoints()))
                .and(ProblemSpecifications.maxPoints(filter.getMaxPoints()));

        return problemRepository.findAll(specification,pageable).map(ProblemSummaryResponse::fromEntity);
    }

    @Transactional
    public ProblemCreatedResponse createProblem(ProblemRequest request) {

        String slug = generateSlug(request.getTitle());
        if (problemRepository.existsBySlug(slug)) {
            throw new ConflictException("A problem with a similar title already exists");
        }

        Problem problem = Problem.builder()
                .slug(slug)
                .title(request.getTitle())
                .description(request.getDescription())
                .constraints(request.getConstraints())
                .difficulty(request.getDifficulty())
                .points(request.getPoints())
                .timeLimitMs(request.getTimeLimitMs())
                .memoryLimitKb(request.getMemoryLimitKb())
                .sampleTestCases(TestCaseDto.toTestCase(request.getSampleTestCases()))
                .status(ProblemStatus.DRAFT)
                .tags(resolveTags(request.getTags()))
                .build();

        Problem saved = problemRepository.save(problem);

        String s3Key = "uploads/pending/" + saved.getId() + "/testcases.zip";
        String uploadUrl = s3Service.generatePresignedUploadUrl(s3Key);

        return ProblemCreatedResponse.builder()
                .problemId(saved.getId())
                .slug(saved.getSlug())
                .uploadUrl(uploadUrl)
                .build();
    }


    @SuppressWarnings("unchecked")
    public List<TestCase> getAllTestCases(Problem problem) {

        String cacheKey = TEST_CASES_KEY_PREFIX + problem.getId();

        List<TestCase> cached = (List<TestCase>) redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            log.info("CACHE HIT for problem {} — {} test cases", problem.getId(), cached.size());
            return cached;
        }

        log.info("CACHE MISS for problem {} — fetching from source", problem.getId());

        List<TestCase> all = new ArrayList<>(problem.getSampleTestCases());
        if (problem.getHiddenTestCasesS3Key() != null && !problem.getHiddenTestCasesS3Key().isBlank()) {
            all.addAll(s3Service.downloadAndParseTestCases(problem.getHiddenTestCasesS3Key()));
        }

        if (!all.isEmpty()) {
            redisTemplate.opsForValue().set(cacheKey, all, ttlMinutes, TimeUnit.MINUTES);
        }
        return all;
    }

    public void invalidateTestCases(UUID problemId) {
        redisTemplate.delete(TEST_CASES_KEY_PREFIX + problemId);
        log.info("Evicted test-case cache for problem {}", problemId);
    }

    @Transactional
    public void finalizeProblem(UUID problemId, String providedSecret) {

        if (!internalSecret.equals(providedSecret)) {
            throw new UnauthorisedException("Invalid internal secret");
        }

        Problem problem = problemRepository.findById(problemId)
                .orElseThrow(() -> new RessourceNotFoundException("Problem not found"));

        if (problem.getStatus() != ProblemStatus.DRAFT) {
            log.warn("Problem {} already finalized (status {})", problemId, problem.getStatus());
            return;
        }

        String sourceKey = "uploads/pending/" + problemId + "/testcases.zip";
        if (!s3Service.doesObjectExist(sourceKey)) {
            throw new ConflictException("No uploaded test cases found for this problem");
        }

        String destinationKey = "published-test-cases/" + problemId + "/testcases.zip";
        String finalKey = s3Service.moveObject(sourceKey, destinationKey);

        problem.setHiddenTestCasesS3Key(finalKey);
        problem.setStatus(ProblemStatus.PUBLISHED);
        invalidateTestCases(problemId);
        log.info("Problem {} finalized and published", problemId);
    }

    @Transactional
    public ProblemResponse updateProblem(String slug, ProblemRequest request) {

        Problem problem = problemRepository.findBySlug(slug)
                .orElseThrow(() -> new RessourceNotFoundException("Problem not found"));

        problem.setTitle(request.getTitle());
        problem.setDescription(request.getDescription());
        problem.setConstraints(request.getConstraints());
        problem.setDifficulty(request.getDifficulty());
        problem.setPoints(request.getPoints());
        problem.setTimeLimitMs(request.getTimeLimitMs());
        problem.setMemoryLimitKb(request.getMemoryLimitKb());
        problem.setSampleTestCases(TestCaseDto.toTestCase(request.getSampleTestCases()));

        problem.getTags().clear();
        problem.getTags().addAll(resolveTags(request.getTags()));

        invalidateTestCases(problem.getId());

        return ProblemResponse.fromEntity(problem);
    }

    @Transactional
    public void delete(String slug) {
        Problem problem = problemRepository.findBySlug(slug)
                .orElseThrow(() -> new RessourceNotFoundException("Problem not found"));
        problem.setStatus(ProblemStatus.ARCHIVED);

        invalidateTestCases(problem.getId());
    }

    @Transactional(readOnly = true)
    public Optional<Problem> pickRandomProblem(Difficulty difficulty, UUID playerOneId, UUID playerTwoId) {
        return problemRepository.pickRandomUnsolvedByEither(difficulty.name(), playerOneId, playerTwoId);
    }


    private boolean canGetDrafts(){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth.getAuthorities().stream()
                .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals(PermissionReference.READ_DRAFT_PROBLEM.name()));
    }

    private Set<Tag> resolveTags(Set<String> tags) {
        return tags.stream()
                .map(String::toLowerCase)
                .map(name -> tagRepository.findByName(name)
                        .orElseGet(() -> tagRepository.save(new Tag(name))))
                .collect(Collectors.toSet());
    }

    private String generateSlug(String title) {
        return title.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .trim()
                .replaceAll("\\s+", "-");
    }


    public ProblemResponse getById(UUID problemId) {
        Problem problem = problemRepository.findById(problemId).orElseThrow(() -> new RessourceNotFoundException("Problem not found"));
        return ProblemResponse.fromEntity(problem);
    }
}
