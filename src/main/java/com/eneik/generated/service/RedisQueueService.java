package com.eneik.generated.service;

import com.eneik.generated.config.RedisConfig.RedisAvailabilityChecker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RedisQueueService {

    private static final Logger log = LoggerFactory.getLogger(RedisQueueService.class);
    private static final String QUEUE_KEY_PREFIX = "campaign:dispatch:queue:";

    private final RedisTemplate<String, String> redisTemplate;
    private final RedisAvailabilityChecker availabilityChecker;

    public RedisQueueService(RedisTemplate<String, String> redisTemplate,
                             RedisAvailabilityChecker availabilityChecker) {
        this.redisTemplate = redisTemplate;
        this.availabilityChecker = availabilityChecker;
    }

    /**
     * Pushes a dispatch message payload onto a campaign-specific queue in Redis.
     * Payload: "recipientPhoneOrUsername:text" or similar format.
     */
    public boolean pushToQueue(String campaignId, String recipient, String text) {
        if (!availabilityChecker.isAvailable()) {
            return false;
        }
        try {
            String key = QUEUE_KEY_PREFIX + campaignId;
            String value = recipient + "::" + text;
            redisTemplate.opsForList().rightPush(key, value);
            log.info("Pushed action to Redis queue key: {}, value: {}", key, value);
            return true;
        } catch (Exception e) {
            log.error("Failed to push to Redis-backed queue. Marking Redis as unavailable. Error: {}", e.getMessage());
            availabilityChecker.markAsUnavailable();
            return false;
        }
    }

    /**
     * Pops a dispatch message from the campaign-specific queue.
     */
    public String popFromQueue(String campaignId) {
        if (!availabilityChecker.isAvailable()) {
            return null;
        }
        try {
            String key = QUEUE_KEY_PREFIX + campaignId;
            return redisTemplate.opsForList().leftPop(key);
        } catch (Exception e) {
            log.error("Failed to pop from Redis-backed queue. Marking Redis as unavailable. Error: {}", e.getMessage());
            availabilityChecker.markAsUnavailable();
            return null;
        }
    }

    /**
     * Gets size of the queue.
     */
    public long getQueueSize(String campaignId) {
        if (!availabilityChecker.isAvailable()) {
            return 0;
        }
        try {
            String key = QUEUE_KEY_PREFIX + campaignId;
            Long size = redisTemplate.opsForList().size(key);
            return size != null ? size : 0;
        } catch (Exception e) {
            return 0;
        }
    }
}
