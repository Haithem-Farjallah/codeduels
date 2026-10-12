package com.codeduels.duel.service;

public interface CodeforcesApiService {
    List<CfSubmission> getUserStatus(String handle, int count);
    boolean handleExists(String handle);
}