package com.apigateway;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class RateLimiterService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final int MAX_REQUESTS_PER_MINUTE = 10;

    public boolean isAllowed(String username) {
        String key = "rate_limit:" + username;

        Long currentCount = redisTemplate.opsForValue().increment(key);

        if (currentCount == 1) {
            redisTemplate.expire(key, 1, TimeUnit.MINUTES);
        }

        return currentCount <= MAX_REQUESTS_PER_MINUTE;
    }
}