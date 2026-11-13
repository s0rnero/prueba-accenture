package com.accenture.franchises.security;

import com.accenture.franchises.config.SecurityProperties;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Set;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1000)
public class CsrfCookieWebFilter implements WebFilter {

    public static final String COOKIE = "XSRF-TOKEN";
    public static final String HEADER = "X-XSRF-TOKEN";

    private static final Set<HttpMethod> SAFE = Set.of(HttpMethod.GET, HttpMethod.HEAD, HttpMethod.OPTIONS);
    private static final Set<String> PUBLIC = Set.of(
            "/api/v1/auth/login", "/api/v1/auth/register", "/api/v1/auth/csrf");

    private final SecureRandom random = new SecureRandom();
    private final boolean cookieSecure;

    public CsrfCookieWebFilter(SecurityProperties properties) {
        this.cookieSecure = properties.cookieSecure();
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        HttpCookie cookie = exchange.getRequest().getCookies().getFirst(COOKIE);
        if (cookie == null || cookie.getValue().isBlank()) {
            exchange.getResponse().addCookie(ResponseCookie.from(COOKIE, newToken())
                    .httpOnly(false)
                    .secure(cookieSecure)
                    .sameSite("Lax")
                    .path("/")
                    .build());
        }

        if (SAFE.contains(exchange.getRequest().getMethod())
                || PUBLIC.contains(exchange.getRequest().getPath().value())) {
            return chain.filter(exchange);
        }

        String header = exchange.getRequest().getHeaders().getFirst(HEADER);
        boolean valid = cookie != null && header != null && header.equals(cookie.getValue());
        if (!valid) {
            exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
            return exchange.getResponse().setComplete();
        }
        return chain.filter(exchange);
    }

    private String newToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
