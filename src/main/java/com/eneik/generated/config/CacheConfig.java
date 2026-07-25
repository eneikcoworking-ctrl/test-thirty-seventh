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

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    @ConditionalOnProperty(name = "spring.cache.type", havingValue = "redis")
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // Create specialized ObjectMapper for polymorphic JSON caching
        ObjectMapper objectMapper = new ObjectMapper();

        // Setup polymorphic default typing for polymorphic deserialization
        objectMapper.activateDefaultTyping(
            LaissezFaireSubTypeValidator.instance,
            ObjectMapper.DefaultTyping.NON_FINAL,
            JsonTypeInfo.As.PROPERTY
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

        // Short 10-second TTL for conversations to allow fast dynamic updates without thrashing eviction
        initialCacheConfigurations.put(CacheConstants.CACHE_CONVERSATIONS, RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofSeconds(10))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer)));

        // 5-minute TTL for messages
        initialCacheConfigurations.put(CacheConstants.CACHE_MESSAGES, RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(5))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer)));

        // 1-hour TTL for campaigns
        initialCacheConfigurations.put(CacheConstants.CACHE_CAMPAIGNS, RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(1))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer)));
        initialCacheConfigurations.put(CacheConstants.CACHE_CAMPAIGN_BY_ID, RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(1))
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

    public static class PageSerializer extends JsonSerializer<Page> {
        @Override
        public void serialize(Page value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeStartObject();
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
