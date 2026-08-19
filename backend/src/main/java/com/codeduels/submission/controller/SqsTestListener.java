package com.codeduels.submission.controller;

import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class SqsTestListener {

    @SqsListener("codeduels-submissions")
    public void receive(String message) {
        log.info("RECEIVED FROM SQS: {}", message);
    }
}