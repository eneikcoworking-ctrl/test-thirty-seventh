package com.eneik.generated;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.Cache.ValueWrapper;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.support.SimpleValueWrapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
@EnableCaching
public class CacheConfig implements CachingConfigurer {

    private static final Logger log = LoggerFactory.getLogger(CacheConfig.class);

    @Bean
    @Primary
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        CacheManager redisCacheManager = null;
        try {
            log.info("Attempting to connect and ping Redis during bootstrap...");
            connectionFactory.getConnection().ping();
            log.info("Redis connection successful. Creating RedisCacheManager.");

            org.springframework.data.redis.cache.RedisCacheConfiguration defaultCacheConfig =
                    org.springframework.data.redis.cache.RedisCacheConfiguration.defaultCacheConfig();

            org.springframework.data.redis.cache.RedisCacheConfiguration messagesCacheConfig =
                    org.springframework.data.redis.cache.RedisCacheConfiguration.defaultCacheConfig()
                            .entryTtl(java.time.Duration.ofMinutes(10));

            redisCacheManager = RedisCacheManager.builder(connectionFactory)
                    .cacheDefaults(defaultCacheConfig)
                    .withCacheConfiguration("messages", messagesCacheConfig)
                    .build();
        } catch (Exception e) {
            log.warn("Redis connection failed during bootstrap. Caching layer will fallback to in-memory mode. Error: {}", e.getMessage(), e);
        }

        ConcurrentMapCacheManager fallbackCacheManager = new ConcurrentMapCacheManager() {
            @Override
            protected Cache createConcurrentMapCache(String name) {
                return new org.springframework.cache.concurrent.ConcurrentMapCache(
                        name,
                        new BoundedConcurrentMap<>(1000),
                        isAllowNullValues()
                );
            }
        };

        if (redisCacheManager == null) {
            log.info("Redis is unavailable. Initializing pure in-memory Bounded CacheManager.");
            return fallbackCacheManager;
        }

        log.info("Initializing resilient FailSafeCacheManager wrapping Redis and in-memory bounded fallback.");
        return new FailSafeCacheManager(redisCacheManager, fallbackCacheManager);
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
            log.warn("Cache GET failed for cache '{}' with key '{}'. Error: {}",
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

    public static class FailSafeCacheManager implements CacheManager {

        private static final Logger log = LoggerFactory.getLogger(FailSafeCacheManager.class);
        private final CacheManager delegate;
        private final CacheManager fallback;
        private final ConcurrentHashMap<String, Cache> cacheMap = new ConcurrentHashMap<>();

        public FailSafeCacheManager(CacheManager delegate, CacheManager fallback) {
            this.delegate = delegate;
            this.fallback = fallback;
        }

        @Override
        public Cache getCache(String name) {
            return cacheMap.computeIfAbsent(name, k -> {
                Cache dCache = null;
                try {
                    dCache = delegate.getCache(k);
                } catch (Exception e) {
                    log.warn("Fail-safe: Failed to retrieve delegate cache '{}'. Error: {}", k, e.getMessage());
                }
                Cache fCache = fallback.getCache(k);
                if (dCache == null) {
                    return fCache;
                }
                return new FailSafeCache(dCache, fCache);
            });
        }

        @Override
        public Collection<String> getCacheNames() {
            try {
                return delegate.getCacheNames();
            } catch (Exception e) {
                log.warn("Fail-safe: Failed to retrieve delegate cache names. Error: {}", e.getMessage());
                return fallback.getCacheNames();
            }
        }
    }

    public static class FailSafeCache implements Cache {

        private static final Logger log = LoggerFactory.getLogger(FailSafeCache.class);
        private final Cache delegate;
        private final Cache fallback;

        public FailSafeCache(Cache delegate, Cache fallback) {
            this.delegate = delegate;
            this.fallback = fallback;
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
            try {
                ValueWrapper dWrapper = delegate.get(key);
                if (dWrapper != null) {
                    Object value = dWrapper.get();
                    return new SimpleValueWrapper(value);
                }
            } catch (Exception e) {
                log.warn("Fail-safe: Cache GET/deserialization failed for key '{}' in cache '{}'. Falling back to local cache. Error: {}", key, getName(), e.getMessage(), e);
            }

            try {
                ValueWrapper fWrapper = fallback.get(key);
                if (fWrapper != null) {
                    Object value = fWrapper.get();
                    return new SimpleValueWrapper(value);
                }
            } catch (Exception e) {
                log.warn("Fail-safe: Fallback Cache GET/deserialization failed for key '{}' in cache '{}'. Error: {}", key, getName(), e.getMessage(), e);
            }

            return null;
        }

        @Override
        public <T> T get(Object key, Class<T> type) {
            try {
                return delegate.get(key, type);
            } catch (Exception e) {
                log.warn("Fail-safe: Cache GET (typed) failed for key '{}' in cache '{}'. Falling back to local cache. Error: {}", key, getName(), e.getMessage());
                try {
                    return fallback.get(key, type);
                } catch (Exception ex) {
                    log.warn("Fail-safe: Fallback Cache GET (typed) failed for key '{}' in cache '{}'. Error: {}", key, getName(), ex.getMessage());
                    return null;
                }
            }
        }

        @Override
        public <T> T get(Object key, Callable<T> valueLoader) {
            try {
                return delegate.get(key, valueLoader);
            } catch (Exception e) {
                log.warn("Fail-safe: Cache GET (loader) failed for key '{}' in cache '{}'. Falling back to local cache. Error: {}", key, getName(), e.getMessage());
                try {
                    return fallback.get(key, valueLoader);
                } catch (Exception ex) {
                    try {
                        return valueLoader.call();
                    } catch (Exception exc) {
                        throw new RuntimeException(exc);
                    }
                }
            }
        }

        @Override
        public void put(Object key, Object value) {
            try {
                delegate.put(key, value);
            } catch (Exception e) {
                log.warn("Fail-safe: Cache PUT failed for key '{}' in cache '{}'. Falling back to local cache. Error: {}", key, getName(), e.getMessage());
                try {
                    fallback.put(key, value);
                } catch (Exception ex) {
                    log.error("Fail-safe: Fallback Cache PUT also failed.", ex);
                }
            }
        }

        @Override
        public ValueWrapper putIfAbsent(Object key, Object value) {
            try {
                ValueWrapper dWrapper = delegate.putIfAbsent(key, value);
                if (dWrapper != null) {
                    return new SimpleValueWrapper(dWrapper.get());
                }
            } catch (Exception e) {
                log.warn("Fail-safe: Cache putIfAbsent failed for key '{}' in cache '{}'. Falling back to local cache. Error: {}", key, getName(), e.getMessage());
            }

            try {
                ValueWrapper fWrapper = fallback.putIfAbsent(key, value);
                if (fWrapper != null) {
                    return new SimpleValueWrapper(fWrapper.get());
                }
            } catch (Exception e) {
                log.warn("Fail-safe: Fallback Cache putIfAbsent failed for key '{}' in cache '{}'. Error: {}", key, getName(), e.getMessage());
            }

            return null;
        }

        @Override
        public void evict(Object key) {
            try {
                delegate.evict(key);
            } catch (Exception e) {
                log.warn("Fail-safe: Cache EVICT failed for key '{}' in cache '{}'. Falling back to local cache. Error: {}", key, getName(), e.getMessage());
                try {
                    fallback.evict(key);
                } catch (Exception ex) {
                    log.error("Fail-safe: Fallback Cache EVICT also failed.", ex);
                }
            }
        }

        @Override
        public boolean evictIfPresent(Object key) {
            try {
                return delegate.evictIfPresent(key);
            } catch (Exception e) {
                log.warn("Fail-safe: Cache evictIfPresent failed for key '{}' in cache '{}'. Falling back to local cache. Error: {}", key, getName(), e.getMessage());
                return fallback.evictIfPresent(key);
            }
        }

        @Override
        public void clear() {
            try {
                delegate.clear();
            } catch (Exception e) {
                log.warn("Fail-safe: Cache CLEAR failed in cache '{}'. Falling back to local cache. Error: {}", getName(), e.getMessage());
                try {
                    fallback.clear();
                } catch (Exception ex) {
                    log.error("Fail-safe: Fallback Cache CLEAR also failed.", ex);
                }
            }
        }

        @Override
        public boolean invalidate() {
            try {
                return delegate.invalidate();
            } catch (Exception e) {
                log.warn("Fail-safe: Cache invalidate failed in cache '{}'. Falling back to local cache. Error: {}", getName(), e.getMessage());
                return fallback.invalidate();
            }
        }
    }

    public static class BoundedConcurrentMap<K, V> extends ConcurrentHashMap<K, V> {
        private final int maxEntries;

        public BoundedConcurrentMap(int maxEntries) {
            super();
            this.maxEntries = maxEntries;
        }

        @Override
        public V put(K key, V value) {
            if (size() >= maxEntries) {
                clear();
            }
            return super.put(key, value);
        }

        @Override
        public V putIfAbsent(K key, V value) {
            if (size() >= maxEntries) {
                clear();
            }
            return super.putIfAbsent(key, value);
        }
    }
}
