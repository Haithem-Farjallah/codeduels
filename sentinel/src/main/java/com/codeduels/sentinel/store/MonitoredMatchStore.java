package com.codeduels.sentinel.store;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * The Sentinel's own Redis:
 *   sentinel:match:{duelId}               JSON MonitoredMatch
 *   sentinel:targets:{duelId}             SET of handles still being watched
 *   sentinel:seen:{duelId}:{handle}       SET of Codeforces submission ids already reported
 *   sentinel:interval:{duelId}:{handle}   current backoff interval, seconds
 *   sentinel:due                          ZSET of encoded WatchTargets scored by next poll (epoch ms)
 */
@Repository
@RequiredArgsConstructor
@Slf4j
public class MonitoredMatchStore {

    private static final String MATCH_KEY = "sentinel:match:";
    private static final String TARGETS_KEY = "sentinel:targets:";
    private static final String SEEN_KEY = "sentinel:seen:";
    private static final String INTERVAL_KEY = "sentinel:interval:";
    private static final String DUE_KEY = "sentinel:due";

    /** Everything about a duel expires on its own an hour after it ends, in case cleanup never runs */
    private static final Duration RETENTION_AFTER_END = Duration.ofHours(1);

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public void save(MonitoredMatch match) {
        try {
            redis.opsForValue().set(MATCH_KEY + match.getDuelId(),
                    objectMapper.writeValueAsString(match), ttl(match));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not serialize monitored match", e);
        }
    }

    public Optional<MonitoredMatch> find(UUID duelId) {
        String json = redis.opsForValue().get(MATCH_KEY + duelId);
        if (json == null) return Optional.empty();
        try {
            return Optional.of(objectMapper.readValue(json, MonitoredMatch.class));
        } catch (JsonProcessingException e) {
            log.error("Corrupt monitored match {}", duelId, e);
            return Optional.empty();
        }
    }

    /** Registers the handle and schedules its first poll — unless it is already being watched */
    public void watch(MonitoredMatch match, WatchTarget target, long firstPollAtMs, long initialIntervalSeconds) {
        redis.opsForSet().add(TARGETS_KEY + match.getDuelId(), target.handle());
        redis.expire(TARGETS_KEY + match.getDuelId(), ttl(match));
        redis.opsForValue().setIfAbsent(intervalKey(target), String.valueOf(initialIntervalSeconds), ttl(match));
        redis.opsForZSet().addIfAbsent(DUE_KEY, target.encode(), firstPollAtMs);
    }

    /** Stops watching one handle; once none remain, drops the duel entirely */
    public void unwatch(WatchTarget target) {
        redis.opsForZSet().remove(DUE_KEY, target.encode());
        redis.opsForSet().remove(TARGETS_KEY + target.duelId(), target.handle());
        redis.delete(List.of(intervalKey(target), seenKey(target)));

        Long remaining = redis.opsForSet().size(TARGETS_KEY + target.duelId());
        if (remaining == null || remaining == 0) {
            redis.delete(List.of(MATCH_KEY + target.duelId(), TARGETS_KEY + target.duelId()));
            log.info("Stopped monitoring duel {}", target.duelId());
        }
    }

    public List<WatchTarget> dueTargets(long nowMs, int limit) {
        Set<String> due = redis.opsForZSet().rangeByScore(DUE_KEY, 0, nowMs, 0, limit);
        return due == null ? List.of() : due.stream().map(WatchTarget::decode).toList();
    }

    public void reschedule(WatchTarget target, long nextPollAtMs) {
        redis.opsForZSet().add(DUE_KEY, target.encode(), nextPollAtMs);
    }

    public long getInterval(WatchTarget target, long fallbackSeconds) {
        String value = redis.opsForValue().get(intervalKey(target));
        return value == null ? fallbackSeconds : Long.parseLong(value);
    }

    public void setInterval(WatchTarget target, long seconds, MonitoredMatch match) {
        redis.opsForValue().set(intervalKey(target), String.valueOf(seconds), ttl(match));
    }

    public boolean isSeen(WatchTarget target, long submissionId) {
        return Boolean.TRUE.equals(redis.opsForSet().isMember(seenKey(target), String.valueOf(submissionId)));
    }

    public void markSeen(WatchTarget target, long submissionId, MonitoredMatch match) {
        redis.opsForSet().add(seenKey(target), String.valueOf(submissionId));
        redis.expire(seenKey(target), ttl(match));
    }

    private String intervalKey(WatchTarget target) {
        return INTERVAL_KEY + target.duelId() + ":" + target.handle();
    }

    private String seenKey(WatchTarget target) {
        return SEEN_KEY + target.duelId() + ":" + target.handle();
    }

    private Duration ttl(MonitoredMatch match) {
        long secondsLeft = match.getEndEpoch() - Instant.now().getEpochSecond();
        return Duration.ofSeconds(Math.max(secondsLeft, 0)).plus(RETENTION_AFTER_END);
    }
}
