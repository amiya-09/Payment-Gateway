package com.springboot.razorpay.merchant.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.Optional;

@Component
@Slf4j
@RequiredArgsConstructor
public class RedisApiKeyCache implements ApiKeyCache {

    private static final String PREFIX = "apikey:";
    private static final Duration TTL = Duration.ofMinutes(5);

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public Optional<ApiKeyCacheEntry> get(String keyId) {
        try {
            String json = stringRedisTemplate.opsForValue().get(PREFIX + keyId);
            if (json != null) {
                return Optional.of(objectMapper.readValue(json, ApiKeyCacheEntry.class));
            } else {
                return Optional.empty();
            }
        } catch (Exception e) {
            log.warn("Error occurred while fetching API key from cache", e);
            return Optional.empty();
        }
    }

    @Override
    public void put(String keyId, ApiKeyCacheEntry entry) {
        try {
            stringRedisTemplate.opsForValue().set(PREFIX + keyId, objectMapper.writeValueAsString(entry), TTL);
        } catch (Exception e) {
            log.warn("Error occurred while putting API key in cache", e);
        }
    }

    @Override
    public void evict(String keyId) {
        stringRedisTemplate.delete(PREFIX + keyId);
    }
}
