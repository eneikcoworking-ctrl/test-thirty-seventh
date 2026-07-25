package com.eneik.generated.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Configuration
@EnableCaching
public class CacheConfig implements CachingConfigurer {

    private static final Logger log = LoggerFactory.getLogger(CacheConfig.class);

    @Override
    public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException exception, org.springframework.cache.Cache cache, Object key) {
                log.warn("Redis Cache GET failed for key {}. Falling back to database. Error: {}", key, exception.getMessage());
            }

            @Override
            public void handleCachePutError(RuntimeException exception, org.springframework.cache.Cache cache, Object key, Object value) {
                log.warn("Redis Cache PUT failed for key {}. Error: {}", key, exception.getMessage());
            }

            @Override
            public void handleCacheEvictError(RuntimeException exception, org.springframework.cache.Cache cache, Object key) {
                log.warn("Redis Cache EVICT failed for key {}. Error: {}", key, exception.getMessage());
            }

            @Override
            public void handleCacheClearError(RuntimeException exception, org.springframework.cache.Cache cache) {
                log.warn("Redis Cache CLEAR failed. Error: {}", exception.getMessage());
            }
        };
    }

    @Bean
    @ConditionalOnProperty(name = "spring.cache.type", havingValue = "redis")
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // Create specialized ObjectMapper for polymorphic JSON caching
        ObjectMapper objectMapper = new ObjectMapper();

        // Build secure PolymorphicTypeValidator allowing only safe whitelisted package prefixes
        com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator ptv =
            com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator.builder()
                .allowIfBaseType("com.eneik.generated")
                .allowIfSubType("com.eneik.generated")
                .allowIfBaseType("org.springframework.data")
                .allowIfSubType("org.springframework.data")
                .allowIfBaseType("java.util")
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

        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(30))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer));

        Map<String, RedisCacheConfiguration> initialCacheConfigurations = new HashMap<>();

        // Short TTL for conversations to allow fast dynamic updates without thrashing eviction
        initialCacheConfigurations.put(CacheConstants.CACHE_CONVERSATIONS, RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofSeconds(CacheConstants.TTL_CONVERSATIONS_SEC))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer)));

        // TTL for messages
        initialCacheConfigurations.put(CacheConstants.CACHE_MESSAGES, RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofSeconds(CacheConstants.TTL_MESSAGES_SEC))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer)));

        // TTL for campaigns
        initialCacheConfigurations.put(CacheConstants.CACHE_CAMPAIGNS, RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofSeconds(CacheConstants.TTL_CAMPAIGNS_SEC))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer)));
        initialCacheConfigurations.put(CacheConstants.CACHE_CAMPAIGN_BY_ID, RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofSeconds(CacheConstants.TTL_CAMPAIGNS_SEC))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(initialCacheConfigurations)
                .build();
    }

    public static class PageJacksonModule extends com.fasterxml.jackson.databind.module.SimpleModule {
        public PageJacksonModule() {
            addSerializer(Page.class, new PageSerializer());
            addDeserializer(Page.class, new PageDeserializer());
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
                for (JsonNode item : contentNode) {
                    content.add(p.getCodec().treeToValue(item, Object.class));
                }
            }

            return new PageImpl<>(content, PageRequest.of(page, size > 0 ? size : 10), totalElements);
        }
    }
}
