package com.codeduels.problem.service;

import com.codeduels.auth.model.PermissionReference;
import com.codeduels.common.exception.ConflictException;
import com.codeduels.common.exception.RessourceNotFoundException;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProblemService {
    private final ProblemRepository problemRepository;
    private final TagRepository tagRepository;

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
    public ProblemResponse createProblem(ProblemRequest request) {

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

        return ProblemResponse.fromEntity(problemRepository.save(problem));
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

        return ProblemResponse.fromEntity(problem);
    }

    @Transactional
    public void delete(String slug) {
        Problem problem = problemRepository.findBySlug(slug)
                .orElseThrow(() -> new RessourceNotFoundException("Problem not found"));
        problem.setStatus(ProblemStatus.ARCHIVED);
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
}
