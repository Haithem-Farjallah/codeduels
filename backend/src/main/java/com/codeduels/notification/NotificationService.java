package com.codeduels.notification;

import com.codeduels.submission.dto.SubmissionResult;

import java.util.UUID;

public interface NotificationService {
    void notifyUser( UUID submissionId, SubmissionResult result);
}
