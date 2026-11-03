package com.springboot.razorpay.common.ratelimit;

import com.springboot.razorpay.common.ratelimit.RateLimitResult;
import com.springboot.razorpay.common.ratelimit.RateLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "app.rate-limit.method",
        havingValue = "bucket"
)
public class TokenBucketRateLimiter implements RateLimiter {

    /**
     * Token bucket rate limiter: capacity == maxRequests,
     * refill rate == maxRequests / windowSeconds tokens/sec.

     * Unlike the fixed window, tokens trickle back continuously instead
     * of resetting all at once at a window boundary, so it doesn't allow
     * a burst of 2x maxRequests around the edge of two windows.

     * State (tokens remaining, last refill timestamp) lives in a Redis hash;
     * refill + consume is done atomically in a single Lua script so two
     * concurrent requests for the same key can't both read the same
     * "tokens available" snapshot and both be allowed through.
     */

    private static final RedisScript<List> SCRIPT =
            new DefaultRedisScript<>("""
                    local key = KEYS[1]
                    local capacity = tonumber(ARGV[1])
                    local refillPerSec = tonumber(ARGV[2])
                    local nowMs = tonumber(ARGV[3])
                    local ttlSeconds = tonumber(ARGV[4])

                    local data = redis.call('HMGET', key, 'tokens', 'ts')
                    local tokens = tonumber(data[1])
                    local lastTs = tonumber(data[2])

                    if tokens == nil then
                        tokens = capacity
                        lastTs = nowMs
                    end

                    -- calculate elapsed time
                    local elapsedSec = math.max(0, (nowMs - lastTs) / 1000)
                    tokens = math.min(capacity, tokens + elapsedSec * refillPerSec)

                    local allowed = 0
                    
                    if tokens >= 1 then
                        tokens = tokens - 1
                        allowed = 1
                    end

                    -- persist updated bucket state
                    redis.call(
                        'HSET',
                        key,
                        'tokens', tokens,
                        'ts', nowMs
                    )

                    redis.call('EXPIRE', key, ttlSeconds)

                    -- calculate retry-after in milliseconds
                    local retryAfterMs = 0
                    if allowed == 0 then
                        retryAfterMs = math.ceil(
                            ((1 - tokens) / refillPerSec)
                        )
                    end

                    return {
                        allowed,
                        math.floor(tokens),
                        retryAfterMs
                    }
                    """, List.class);

    private final StringRedisTemplate redis;


    @Override
    public RateLimitResult check(String key, int maxRequests, int windowSeconds) {
        try {
            String redisKey = "ratelimit:bucket:" + key;
            double refillPerSec = (double) maxRequests / windowSeconds;
            // keep bucket state around for two windows of inactivity before Redis reclaims
            long ttlSeconds = windowSeconds * 2;
            List<Long> result = redis.execute(
                    SCRIPT,
                    List.of(redisKey),
                    String.valueOf(maxRequests),
                    String.valueOf(refillPerSec),
                    String.valueOf(System.currentTimeMillis()),
                    String.valueOf(ttlSeconds)
            );
            boolean allowed = result.get(0) == 1L;
            int remaining = result.get(1).intValue();
            int retryAfterMs = result.get(2).intValue();
            return allowed
                    ? RateLimitResult.allowed(remaining)
                    : RateLimitResult.denied(retryAfterMs);
        } catch (DataAccessException e) {
            log.warn("Rate limiter unavailable, failing open for key={}", key, e);
            return RateLimitResult.allowed(maxRequests);
        }
    }
}
