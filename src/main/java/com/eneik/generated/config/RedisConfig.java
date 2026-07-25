package com.eneik.generated.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;

@Configuration
@EnableCaching
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
    public CacheManager cacheManager(RedisConnectionFactory redisConnectionFactory, RedisAvailabilityChecker checker) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.registerModule(new Jdk8Module()); // Support for Optional
        PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
                .allowIfBaseType("com.eneik.generated.")
                .allowIfBaseType("java.util.")
                .allowIfBaseType("java.time.")
                .allowIfBaseType("java.lang.")
                .build();
        mapper.activateDefaultTyping(ptv, ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);

        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(mapper);

        RedisCacheConfiguration cacheConfiguration = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                // Allow caching null values because Optional.empty() is evaluated as null and causes IllegalArgumentException if disabled
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer));

        RedisCacheManager delegate = RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(cacheConfiguration)
                .transactionAware()
                .build();

        return new FallbackCacheManager(delegate, checker);
    }

    public static class FallbackCacheManager implements CacheManager {
        private final CacheManager delegate;
        private final RedisAvailabilityChecker checker;

        public FallbackCacheManager(CacheManager delegate, RedisAvailabilityChecker checker) {
            this.delegate = delegate;
            this.checker = checker;
        }

        @Override
        public Cache getCache(String name) {
            Cache cache = delegate.getCache(name);
            if (cache == null) return null;
            return new FallbackCache(cache, checker);
        }

        @Override
        public Collection<String> getCacheNames() {
            return delegate.getCacheNames();
        }
    }

    public static class FallbackCache implements Cache {
        private static final Logger fallbackLog = LoggerFactory.getLogger(FallbackCache.class);
        private final Cache delegate;
        private final RedisAvailabilityChecker checker;

        public FallbackCache(Cache delegate, RedisAvailabilityChecker checker) {
            this.delegate = delegate;
            this.checker = checker;
        }

        @Override
        public String getName() {
            return delegate.getName();
        }

        @Override
        public Object getNativeCache() {
            return delegate.getNativeCache();
        }

        @Override
        public ValueWrapper get(Object key) {
            if (!checker.isAvailable()) return null;
            try {
                return delegate.get(key);
            } catch (Exception e) {
                fallbackLog.warn("Failed to get cache value for key {}. Marking Redis unavailable.", key, e);
                checker.markAsUnavailable();
                return null;
            }
        }

        @Override
        public <T> T get(Object key, Class<T> type) {
            if (!checker.isAvailable()) return null;
            try {
                return delegate.get(key, type);
            } catch (Exception e) {
                fallbackLog.warn("Failed to get cache value for key {}. Marking Redis unavailable.", key, e);
                checker.markAsUnavailable();
                return null;
            }
        }

        @Override
        public <T> T get(Object key, Callable<T> valueLoader) {
            if (!checker.isAvailable()) {
                try {
                    return valueLoader.call();
                } catch (Exception e) {
                    throw new ValueRetrievalException(key, valueLoader, e);
                }
            }
            try {
                return delegate.get(key, valueLoader);
            } catch (Exception e) {
                fallbackLog.warn("Failed to get cache value for key {}. Marking Redis unavailable.", key, e);
                checker.markAsUnavailable();
                try {
                    return valueLoader.call();
                } catch (Exception ex) {
                    throw new ValueRetrievalException(key, valueLoader, ex);
                }
            }
        }

        @Override
        public void put(Object key, Object value) {
            if (!checker.isAvailable()) return;
            try {
                delegate.put(key, value);
            } catch (Exception e) {
                fallbackLog.warn("Failed to put cache value for key {}. Marking Redis unavailable.", key, e);
                checker.markAsUnavailable();
            }
        }

        @Override
        public void evict(Object key) {
            if (!checker.isAvailable()) return;
            try {
                delegate.evict(key);
            } catch (Exception e) {
                fallbackLog.warn("Failed to evict cache value for key {}. Marking Redis unavailable.", key, e);
                checker.markAsUnavailable();
            }
        }

        @Override
        public void clear() {
            if (!checker.isAvailable()) return;
            try {
                delegate.clear();
            } catch (Exception e) {
                fallbackLog.warn("Failed to clear cache. Marking Redis unavailable.", e);
                checker.markAsUnavailable();
            }
        }
    }


    @Bean
    public RedisAvailabilityChecker redisAvailabilityChecker(RedisConnectionFactory connectionFactory) {
        return new RedisAvailabilityChecker(connectionFactory);
    }

    public static class RedisAvailabilityChecker {
        private final RedisConnectionFactory connectionFactory;
        private final AtomicBoolean isAvailable = new AtomicBoolean(false);
        private Boolean isAvailableOverride = null;
        private long lastCheckTime = 0;

        public RedisAvailabilityChecker(RedisConnectionFactory connectionFactory) {
            this.connectionFactory = connectionFactory;
            checkConnection();
        }

        private synchronized void checkConnection() {
            long now = System.currentTimeMillis();
            if (now - lastCheckTime < 5000) {
                return;
            }
            lastCheckTime = now;
            try (RedisConnection connection = connectionFactory.getConnection()) {
                String response = connection.ping();
                isAvailable.set("PONG".equalsIgnoreCase(response));
            } catch (Exception e) {
                log.debug("Redis ping connection check failed: {}", e.getMessage());
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
