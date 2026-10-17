package com.codeduels.duel.service;

import java.util.Map;
import java.util.Optional;

public interface CodeforcesApiService {

    /** The handle with Codeforces' own capitalisation, or empty if no such user exists. */
    Optional<String> resolveHandle(String handle);

    /** problemId (e.g. 2271E) → problem name. Best effort: missing entries mean "unknown". */
    Map<String, String> getProblemNames();
}
