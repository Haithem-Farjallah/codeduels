package com.codeduels.sentinel.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "sentinel")
public class SentinelProperties {

    private Sqs sqs = new Sqs();
    private Codeforces codeforces = new Codeforces();
    private Polling polling = new Polling();

    @Data
    public static class Sqs {
        private String matchWatchQueue;
        private String matchResultQueue;
    }

    @Data
    public static class Codeforces {
        private String baseUrl = "https://codeforces.com/api";
        private long minRequestIntervalMs = 2000;
        private int fetchCount = 30;
    }

    @Data
    public static class Polling {
        private long loopDelayMs = 500;
        private long minIntervalSeconds = 5;
        private long maxIntervalSeconds = 30;
        private double backoffMultiplier = 2.0;
        private long endgameSeconds = 120;
        private long finalGraceSeconds = 45;
    }
}
