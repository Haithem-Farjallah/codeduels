package com.codeduels.notification;

import com.codeduels.submission.dto.SubmissionResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void notifyUser( UUID submissionId, SubmissionResult result) {
        String logPrefix = String.format("[WS_NOTIFY submissionId=%s]", submissionId);
        final String destination = "/topic/submission-result/" + submissionId;

        log.info("{} -> Attempting to send notification for {} destination: {}",
                logPrefix, result.getStatus(), destination);
        try {
            messagingTemplate.convertAndSend(destination, result);
            log.info("{} <- Successfully sent WebSocket notification.", logPrefix);
        } catch (Exception e) {
            log.error("{} Failed to send WebSocket notification.", logPrefix, e);
        }
    }
}
