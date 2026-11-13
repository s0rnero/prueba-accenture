package com.accenture.franchises.security;

import com.accenture.franchises.exceptions.ApiException;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class LoginThrottle {

    static final int MAX_ATTEMPTS = 5;
    private static final Duration WINDOW = Duration.ofMinutes(15);

    private final Cache<String, AtomicInteger> failures = Caffeine.newBuilder()
            .maximumSize(10_000)
            .expireAfterWrite(WINDOW)
            .build();

    public void check(String key) {
        AtomicInteger attempts = failures.getIfPresent(key);
        if (attempts != null && attempts.get() >= MAX_ATTEMPTS) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "too-many-attempts",
                    "Demasiados intentos fallidos. Espera unos minutos e intenta de nuevo.");
        }
    }

    public void fail(String key) {
        failures.get(key, ignored -> new AtomicInteger()).incrementAndGet();
    }

    public void success(String key) {
        failures.invalidate(key);
    }
}
