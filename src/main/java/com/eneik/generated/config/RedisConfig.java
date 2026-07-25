package com.eneik.generated.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;

@Configuration
public class RedisConfig {

    private static final Logger log = LoggerFactory.getLogger(RedisConfig.class);

    @Value("${spring.data.redis.host:127.0.0.1}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        return new LettuceConnectionFactory(redisHost, redisPort);
    }

    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringSerializer);
        template.setValueSerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setHashValueSerializer(stringSerializer);
        return template;
    }

    @Bean
    public RedisAvailabilityChecker redisAvailabilityChecker(RedisConnectionFactory connectionFactory) {
        return new RedisAvailabilityChecker(redisHost, redisPort);
    }

    /**
     * Resilient circuit-breaker-like availability checker.
     * Checks if socket is open, caching the result to avoid connection overhead under load.
     */
    public static class RedisAvailabilityChecker {
        private final String host;
        private final int port;
        private final AtomicBoolean isAvailable = new AtomicBoolean(false);
        private Boolean isAvailableOverride = null; // for testing
        private long lastCheckTime = 0;

        public RedisAvailabilityChecker(String host, int port) {
            this.host = host;
            this.port = port;
            checkConnection();
        }

        private synchronized void checkConnection() {
            long now = System.currentTimeMillis();
            if (now - lastCheckTime < 5000) { // Check at most once every 5 seconds
                return;
            }
            lastCheckTime = now;
            try (Socket socket = new Socket(host, port)) {
                isAvailable.set(true);
            } catch (Exception e) {
                isAvailable.set(false);
            }
        }

        public boolean isAvailable() {
            if (isAvailableOverride != null) {
                return isAvailableOverride;
            }
            checkConnection();
            return isAvailable.get();
        }

        public void markAsUnavailable() {
            if (isAvailableOverride == null) {
                isAvailable.set(false);
            }
        }

        public void markAsAvailable() {
            if (isAvailableOverride == null) {
                isAvailable.set(true);
            }
        }

        public void setAvailableOverride(Boolean available) {
            this.isAvailableOverride = available;
        }
    }
}
