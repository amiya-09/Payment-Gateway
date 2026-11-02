package com.springboot.razorpay.common.ratelimit;

public interface RateLimiter {

    RateLimitResult check(String key, int maxRequestAllowed, int windowSeconds);
}
