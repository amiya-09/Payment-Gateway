package com.springboot.razorpay.common.ratelimit;


import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.rate-limit.method", havingValue = "sliding")
public class SlidingWindowRateLimiter implements  RateLimiter {

    private final StringRedisTemplate redis;

    @Override
    public RateLimitResult check(String key, int maxRequestAllowed, int windowSeconds) {

        long nowMs = System.currentTimeMillis();
        // Now we define the floor value which defines the range of our current window.
        long floorMs = nowMs - windowSeconds * 1000L;
        String redisKey = "ratelimit:sliding:" + key;

        var zset = redis.opsForZSet();
        // So first of all we want to remove any elements inside this Redis for this key that are before this floorMs,
        // because we don't want to count them in our current window, because they are not part of the current window,
        // so we will remove them from the Redis ZSet.
        // Remove all the requests which are older than the current window
        zset.removeRangeByScore(redisKey, Double.NEGATIVE_INFINITY, floorMs);
        // Get the current count of requests in the current window
        Long count = zset.zCard(redisKey);
        long current = count != null ? count : 0;
        if(current > maxRequestAllowed) {
            var oldest = zset.rangeWithScores(redisKey, 0, 0); // When I do start = 0, end = 0 - it means that I want to find the first number because in a set , every element is put in a sorted manner
            int retryAfter = 1;
            if(oldest != null && !oldest.isEmpty()){
                Double oldestScore = oldest.iterator().next().getScore(); // the score tells us that at which time this oldest item came into our window or into our redis here
                if(oldestScore != null){
                    long windowExpiresMs = oldestScore.longValue() + windowSeconds*1000L;
                    retryAfter = (int) Math.ceil((double) (windowExpiresMs - nowMs) / 1000.0);
                }
            }
            return RateLimitResult.denied(retryAfter);
        }

        zset.add(redisKey, UUID.randomUUID().toString(), nowMs);
        redis.expire(redisKey, Duration.ofSeconds(windowSeconds));
        return RateLimitResult.allowed(maxRequestAllowed - (int)current - 1);
    }
}
