package com.eneik.generated;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.eneik.generated.config.CacheConstants;
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
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

            // Create specialized ObjectMapper for polymorphic JSON caching
            ObjectMapper objectMapper = new ObjectMapper();

            // Build secure PolymorphicTypeValidator allowing only safe whitelisted package prefixes
            com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator ptv =
                com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator.builder()
                    .allowIfBaseType(Object.class)
                    .allowIfSubType("com.eneik.generated")
                    .allowIfSubType("org.springframework.data")
                    .allowIfSubType("java.util")
                    .build();

            // Setup secure polymorphic default typing for polymorphic deserialization
            objectMapper.activateDefaultTyping(
                ptv,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
            );

            // Register class subtypes / aliases to completely decouple JSON structures from Java package paths
            objectMapper.registerSubtypes(
                new com.fasterxml.jackson.databind.jsontype.NamedType(com.eneik.generated.leadgen.model.Conversation.class, "Conversation"),
                new com.fasterxml.jackson.databind.jsontype.NamedType(com.eneik.generated.leadgen.model.ConversationMessage.class, "ConversationMessage"),
                new com.fasterxml.jackson.databind.jsontype.NamedType(com.eneik.generated.domain.Campaign.class, "Campaign"),
                new com.fasterxml.jackson.databind.jsontype.NamedType(org.springframework.data.domain.PageImpl.class, "PageImpl")
            );

            // Register custom Page serializer/deserializer to handle Spring Data PageImpl safely
            objectMapper.registerModule(new PageJacksonModule());

            // Configure Java 8 Date/Time support to handle OffsetDateTime & LocalDateTime in JSON
            objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
            objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

            GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer(objectMapper);

            RedisCacheConfiguration defaultCacheConfig = RedisCacheConfiguration.defaultCacheConfig()
                    .entryTtl(Duration.ofMinutes(30))
                    .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer));

            // Custom cache configurations aligned with centralized TTL constants
            Map<String, RedisCacheConfiguration> initialCacheConfigurations = new HashMap<>();

            initialCacheConfigurations.put(CacheConstants.CACHE_CONVERSATIONS, RedisCacheConfiguration.defaultCacheConfig()
                    .entryTtl(Duration.ofSeconds(CacheConstants.TTL_CONVERSATIONS_SEC))
                    .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer)));

            initialCacheConfigurations.put(CacheConstants.CACHE_MESSAGES, RedisCacheConfiguration.defaultCacheConfig()
                    .entryTtl(Duration.ofSeconds(CacheConstants.TTL_MESSAGES_SEC))
                    .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer)));

            initialCacheConfigurations.put(CacheConstants.CACHE_CAMPAIGNS, RedisCacheConfiguration.defaultCacheConfig()
                    .entryTtl(Duration.ofSeconds(CacheConstants.TTL_CAMPAIGNS_SEC))
                    .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer)));
            initialCacheConfigurations.put(CacheConstants.CACHE_CAMPAIGN_BY_ID, RedisCacheConfiguration.defaultCacheConfig()
                    .entryTtl(Duration.ofSeconds(CacheConstants.TTL_CAMPAIGNS_SEC))
                    .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer)));

            redisCacheManager = RedisCacheManager.builder(connectionFactory)
                    .cacheDefaults(defaultCacheConfig)
                    .withInitialCacheConfigurations(initialCacheConfigurations)
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
                return delegate.get(key);
            } catch (Exception e) {
                log.warn("Fail-safe: Cache GET failed for key '{}' in cache '{}'. Falling back to local cache. Error: {}", key, getName(), e.getMessage());
                return fallback.get(key);
            }
        }

        @Override
        public <T> T get(Object key, Class<T> type) {
            try {
                return delegate.get(key, type);
            } catch (Exception e) {
                log.warn("Fail-safe: Cache GET (typed) failed for key '{}' in cache '{}'. Falling back to local cache. Error: {}", key, getName(), e.getMessage());
                return fallback.get(key, type);
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
                return delegate.putIfAbsent(key, value);
            } catch (Exception e) {
                log.warn("Fail-safe: Cache putIfAbsent failed for key '{}' in cache '{}'. Falling back to local cache. Error: {}", key, getName(), e.getMessage());
                return fallback.putIfAbsent(key, value);
            }
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

    public static class PageJacksonModule extends com.fasterxml.jackson.databind.module.SimpleModule {
        public PageJacksonModule() {
            addSerializer(Page.class, new PageSerializer());
            addDeserializer(Page.class, new PageDeserializer());
            addDeserializer(PageImpl.class, (JsonDeserializer) new PageDeserializer());
        }
    }

    public static final int PAGE_SERIALIZATION_VERSION = 1;

    public static class PageSerializer extends JsonSerializer<Page> {
        @Override
        public void serialize(Page value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeStartObject();
            gen.writeNumberField("v", PAGE_SERIALIZATION_VERSION);
            gen.writeNumberField("totalElements", value.getTotalElements());
            gen.writeNumberField("totalPages", value.getTotalPages());
            gen.writeNumberField("page", value.getNumber());
            gen.writeNumberField("size", value.getSize());
            gen.writeFieldName("content");
            serializers.defaultSerializeValue(value.getContent(), gen);
            gen.writeEndObject();
        }

        @Override
        public void serializeWithType(Page value, JsonGenerator gen, SerializerProvider serializers,
                                      com.fasterxml.jackson.databind.jsontype.TypeSerializer typeSer) throws IOException {
            com.fasterxml.jackson.core.type.WritableTypeId typeIdDef = typeSer.writeTypePrefix(
                    gen, typeSer.typeId(value, com.fasterxml.jackson.core.JsonToken.START_OBJECT));

            gen.writeNumberField("v", PAGE_SERIALIZATION_VERSION);
            gen.writeNumberField("totalElements", value.getTotalElements());
            gen.writeNumberField("totalPages", value.getTotalPages());
            gen.writeNumberField("page", value.getNumber());
            gen.writeNumberField("size", value.getSize());
            gen.writeFieldName("content");
            serializers.defaultSerializeValue(value.getContent(), gen);

            typeSer.writeTypeSuffix(gen, typeIdDef);
        }
    }

    public static class PageDeserializer extends JsonDeserializer<Page> {
        @Override
        public Page deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            JsonNode node = p.getCodec().readTree(p);

            // Check Page deserialization version to protect against schema drift or structure changes
            JsonNode versionNode = node.get("v");
            if (versionNode == null || versionNode.asInt() != PAGE_SERIALIZATION_VERSION) {
                throw new IOException("Page serialization version mismatch or missing. Expected version: " + PAGE_SERIALIZATION_VERSION);
            }

            long totalElements = node.get("totalElements").asLong();
            int page = node.get("page").asInt();
            int size = node.get("size").asInt();

            JsonNode contentNode = node.get("content");
            List<Object> content = new ArrayList<>();
            if (contentNode != null && contentNode.isArray()) {
                JsonNode actualItemsNode = contentNode;
                // Safely unwrap polymorphic array wrapping if present (e.g. ["java.util.ArrayList", [ ... ]])
                if (contentNode.size() == 2 && contentNode.get(0).isTextual() && contentNode.get(1).isArray()) {
                    actualItemsNode = contentNode.get(1);
                }

                for (JsonNode item : actualItemsNode) {
                    if (item.has("telegramChatId")) {
                        content.add(p.getCodec().treeToValue(item, com.eneik.generated.leadgen.model.Conversation.class));
                    } else if (item.has("conversationId")) {
                        content.add(p.getCodec().treeToValue(item, com.eneik.generated.leadgen.model.ConversationMessage.class));
                    } else {
                        content.add(p.getCodec().treeToValue(item, Object.class));
                    }
                }
            }

            return new PageImpl<>(content, PageRequest.of(page, size > 0 ? size : 10), totalElements);
        }
    }
}
