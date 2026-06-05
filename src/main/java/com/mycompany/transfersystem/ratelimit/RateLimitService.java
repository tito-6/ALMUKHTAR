package com.mycompany.transfersystem.ratelimit;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitService {

    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    public Bucket resolveBucket(String key, int requestsPerMinute) {
        return buckets.computeIfAbsent(key, k -> {
            Bandwidth limit = Bandwidth.classic(requestsPerMinute,
                    Refill.intervally(requestsPerMinute, Duration.ofMinutes(1)));
            return Bucket.builder().addLimit(limit).build();
        });
    }

    public boolean tryConsume(String key, int requestsPerMinute) {
        return resolveBucket(key, requestsPerMinute).tryConsume(1);
    }
}
