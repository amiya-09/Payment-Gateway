package com.springboot.razorpay.common.ratelimit;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.rate-limit.method", havingValue = "fixed")
public class FixedWindowRateLimiter implements RateLimiter {

    private final StringRedisTemplate redis;

    @Override
    public RateLimitResult check(String key, int maxRequestAllowed, int windowSeconds) {
        // First, we have defined a redis key and for this key , we are going to store value (integer) which denotes how many
        // requests have been processed within a windowSeconds of time, because after windowSeconds, the bucket itself will get deleted
        // so we just increment it because we are using redis as a cache, and we are not storing it in a database, so we can just increment it and if the key does not exist, it will be created with value 1
        String redisKey = "ratelimit:fixed:"+key;
        Long count = redis.opsForValue().increment(redisKey); // Increments the counter for the given key, counts how many requests have passed within the window
        if(count == null) return RateLimitResult.allowed(maxRequestAllowed); // This means that redis cache is unavailable, and in that case
        // we can make a request, and we can make as many request as is allowed by our system because there is no one to check for any window
        // there is no window so we can definitely make these many requests
        if(count == 1) { // if count is 1 then that means, this is the first time the window is being created for this api key
            redis.expire(redisKey, Duration.ofSeconds(windowSeconds)); // Set the expiration time for the key
        }
        if(count > maxRequestAllowed) {
            Long ttl = redis.getExpire(redisKey, TimeUnit.SECONDS); // Get the remaining time to live for the key
            int retryAfter = (ttl != null && ttl > 0) ? ttl.intValue() : (int) windowSeconds; // If the key has expired, we can retry after the window seconds
            return RateLimitResult.denied(retryAfter);
        }
        return RateLimitResult.allowed(maxRequestAllowed - count.intValue()); // Otherwise, allow the request and return the remaining requests
    }
}
