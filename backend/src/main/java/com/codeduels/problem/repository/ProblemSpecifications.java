package com.codeduels.problem.repository;

import com.codeduels.problem.model.Difficulty;
import com.codeduels.problem.model.Problem;
import com.codeduels.problem.model.ProblemStatus;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.Specification;

@NoArgsConstructor
public class ProblemSpecifications {
    public static Specification<Problem> hasStatus(ProblemStatus status) {
        return status == null ? null
                : (root,query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<Problem> hasDifficulty(Difficulty difficulty) {
        return difficulty==null ? null
                :(root,query, cb) -> cb.equal(root.get("difficulty"), difficulty);
    }

    public static Specification<Problem> likeTitle(String title) {
        return title == null ? null
                : (root,query, cb) -> cb.like(cb.lower(root.get("title")), "%" + title.toLowerCase() + "%");
    }

    public static Specification<Problem> slugContains(String text) {
        return (text == null || text.isBlank()) ? null
                : (root, query, cb) -> cb.like(root.get("slug"), "%" + text.toLowerCase() + "%");
    }

    public static Specification<Problem> minPoints(Integer min) {
        return min == null ? null
                : (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("points"), min);
    }

    public static Specification<Problem> maxPoints(Integer max) {
        return max == null ? null
                : (root, query, cb) -> cb.lessThanOrEqualTo(root.get("points"), max);
    }
}
