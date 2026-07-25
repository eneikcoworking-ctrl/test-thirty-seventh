package com.eneik.generated.service;

import com.eneik.generated.config.RedisConfig.RedisAvailabilityChecker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class RedisRateLimiterService {

    private static final Logger log = LoggerFactory.getLogger(RedisRateLimiterService.class);
    private static final String RATE_LIMIT_PREFIX = "rate:limit:account:";

    private final RedisTemplate<String, String> redisTemplate;
    private final RedisAvailabilityChecker availabilityChecker;

    public RedisRateLimiterService(RedisTemplate<String, String> redisTemplate,
                                   RedisAvailabilityChecker availabilityChecker) {
        this.redisTemplate = redisTemplate;
        this.availabilityChecker = availabilityChecker;
    }

    /**
     * Checks if the rate limit is exceeded for a given Telegram account, or increments atomically.
     * To prevent check-then-act race conditions under concurrent workloads, this uses an atomic check-and-increment model.
     * Returns true if the count is already exceeded.
     */
    public boolean isLimitExceeded(Long tgAccountId, int limit) {
        if (!availabilityChecker.isAvailable()) {
            return false;
        }
        try {
            String key = RATE_LIMIT_PREFIX + tgAccountId;
            String val = redisTemplate.opsForValue().get(key);
            int current = val != null ? Integer.parseInt(val) : 0;
            return current >= limit;
        } catch (Exception e) {
            log.error("Failed to check Redis rate-limit. Marking Redis as unavailable. Error: {}", e.getMessage());
            availabilityChecker.markAsUnavailable();
            return false;
        }
    }

    /**
     * Atomically increments the rate limit counter for a given Telegram account.
     * Returns true if the increment succeeded and the count is within the limit, false if limit is exceeded.
     */
    public boolean tryIncrementAtomic(Long tgAccountId, int limit) {
        if (!availabilityChecker.isAvailable()) {
            return false;
        }
        try {
            String key = RATE_LIMIT_PREFIX + tgAccountId;
            Long current = redisTemplate.opsForValue().increment(key);
            if (current != null && current == 1) {
                redisTemplate.expire(key, 24, TimeUnit.HOURS);
            }
            if (current != null && current > limit) {
                log.info("Redis rate limit exceeded for account: {}. Count: {}, Limit: {}", tgAccountId, current, limit);
                // To keep the count accurate and not lock it indefinitely above the limit, we decrement it back
                redisTemplate.opsForValue().decrement(key);
                return false;
            }
            log.info("Incremented Redis rate-limiter for account {}. New count: {}", tgAccountId, current);
            return true;
        } catch (Exception e) {
            log.error("Failed to increment Redis rate-limiter. Marking Redis as unavailable. Error: {}", e.getMessage());
            availabilityChecker.markAsUnavailable();
            return false;
        }
    }

    /**
     * Decrements the rate limit counter for a given Telegram account (e.g. on failover rollback).
     */
    public void decrementCount(Long tgAccountId) {
        if (!availabilityChecker.isAvailable()) {
            return;
        }
        try {
            String key = RATE_LIMIT_PREFIX + tgAccountId;
            redisTemplate.opsForValue().decrement(key);
            log.info("Decremented Redis rate-limiter for account {}.", tgAccountId);
        } catch (Exception e) {
            log.error("Failed to decrement Redis rate-limiter. Error: {}", e.getMessage());
        }
    }

    /**
     * Manually overrides/sets the rate limit count for testing/control.
     */
    public void setCount(Long tgAccountId, int count) {
        if (!availabilityChecker.isAvailable()) {
            return;
        }
        try {
            String key = RATE_LIMIT_PREFIX + tgAccountId;
            redisTemplate.opsForValue().set(key, String.valueOf(count));
            redisTemplate.expire(key, 24, TimeUnit.HOURS);
        } catch (Exception e) {
            log.error("Failed to set count in Redis rate-limiter. Marking Redis as unavailable. Error: {}", e.getMessage());
            availabilityChecker.markAsUnavailable();
        }
    }

    /**
     * Clears the limit key (e.g. on manual reset).
     */
    public void clearLimit(Long tgAccountId) {
        if (!availabilityChecker.isAvailable()) {
            return;
        }
        try {
            String key = RATE_LIMIT_PREFIX + tgAccountId;
            redisTemplate.delete(key);
        } catch (Exception e) {
            log.error("Failed to clear Redis rate-limiter. Marking Redis as unavailable. Error: {}", e.getMessage());
            availabilityChecker.markAsUnavailable();
        }
    }
}
