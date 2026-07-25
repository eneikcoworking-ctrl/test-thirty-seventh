package com.eneik.generated;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;

@Configuration
@EnableCaching
public class CacheConfig implements CachingConfigurer {

    private static final Logger log = LoggerFactory.getLogger(CacheConfig.class);

    @Bean
    @Primary
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        try {
            log.info("Attempting to connect and ping Redis during bootstrap...");
            connectionFactory.getConnection().ping();
            log.info("Redis connection successful. Configuring RedisCacheManager.");
            return RedisCacheManager.builder(connectionFactory).build();
        } catch (Exception e) {
            log.warn("Redis connection failed during bootstrap. Falling back to in-memory ConcurrentMapCacheManager. Error: {}", e.getMessage(), e);
            return new ConcurrentMapCacheManager();
        }
    }

    @Override
    @Bean
    public CacheErrorHandler errorHandler() {
        return new LoggingCacheErrorHandler();
    }

    public static class LoggingCacheErrorHandler implements CacheErrorHandler {

        private static final Logger log = LoggerFactory.getLogger(LoggingCacheErrorHandler.class);

        @Override
        public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
            log.warn("Cache GET failed for cache '{}' with key '{}'. Proceeding to underlying data source. Error: {}",
                     cache.getName(), key, exception.getMessage(), exception);
        }

        @Override
        public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
            log.warn("Cache PUT failed for cache '{}' with key '{}'. Error: {}",
                     cache.getName(), key, exception.getMessage(), exception);
        }

        @Override
        public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
            log.warn("Cache EVICT failed for cache '{}' with key '{}'. Error: {}",
                     cache.getName(), key, exception.getMessage(), exception);
        }

        @Override
        public void handleCacheClearError(RuntimeException exception, Cache cache) {
            log.warn("Cache CLEAR failed for cache '{}'. Error: {}",
                     cache.getName(), exception.getMessage(), exception);
        }
    }
}
