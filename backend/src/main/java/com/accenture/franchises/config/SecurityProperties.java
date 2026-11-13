package com.accenture.franchises.config;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.security")
public record SecurityProperties(
        String jwtSecret,
        Duration tokenTtl,
        boolean cookieSecure,
        String cookieSameSite,
        String adminEmail,
        String adminPassword,
        List<String> corsAllowedOrigins) {
}
