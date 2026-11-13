package com.accenture.franchises.controllers;

import com.accenture.franchises.config.SecurityProperties;
import com.accenture.franchises.contracts.request.LoginRequest;
import com.accenture.franchises.contracts.request.ProfileUpdateRequest;
import com.accenture.franchises.contracts.request.RegisterRequest;
import com.accenture.franchises.contracts.response.SessionResponse;
import com.accenture.franchises.exceptions.ApiException;
import com.accenture.franchises.security.CookieAuthWebFilter;
import com.accenture.franchises.security.LoginThrottle;
import com.accenture.franchises.services.AuthService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final SecurityProperties properties;
    private final LoginThrottle throttle;

    public AuthController(AuthService authService, SecurityProperties properties, LoginThrottle throttle) {
        this.authService = authService;
        this.properties = properties;
        this.throttle = throttle;
    }

    @PostMapping("/login")
    public Mono<ResponseEntity<SessionResponse>> login(@Valid @RequestBody LoginRequest request,
                                                       ServerWebExchange exchange,
                                                       ServerHttpResponse response) {
        String key = request.email().trim().toLowerCase() + "|" + clientIp(exchange);
        throttle.check(key);
        return authService.login(request)
                .doOnNext(result -> throttle.success(key))
                .doOnError(error -> {
                    if (error instanceof ApiException api && api.status() == HttpStatus.UNAUTHORIZED) {
                        throttle.fail(key);
                    }
                })
                .map(result -> {
                    response.addCookie(cookie(result.token()));
                    return ResponseEntity.ok(result.session());
                });
    }

    @PostMapping("/register")
    public Mono<ResponseEntity<SessionResponse>> register(@Valid @RequestBody RegisterRequest request,
                                                          ServerHttpResponse response) {
        return authService.register(request).map(result -> {
            response.addCookie(cookie(result.token()));
            return ResponseEntity.status(HttpStatus.CREATED).body(result.session());
        });
    }

    @PostMapping("/logout")
    public Mono<ResponseEntity<Void>> logout(ServerHttpResponse response) {
        response.addCookie(ResponseCookie.from(CookieAuthWebFilter.COOKIE, "")
                .httpOnly(true).secure(properties.cookieSecure())
                .sameSite(properties.cookieSameSite()).path("/").maxAge(0).build());
        return Mono.just(ResponseEntity.noContent().build());
    }

    @GetMapping("/me")
    public Mono<SessionResponse> me(@AuthenticationPrincipal Jwt jwt) {
        return authService.session(UUID.fromString(jwt.getSubject()));
    }

    @PatchMapping("/me")
    public Mono<ResponseEntity<SessionResponse>> updateMe(@AuthenticationPrincipal Jwt jwt,
                                                          @Valid @RequestBody ProfileUpdateRequest request,
                                                          ServerHttpResponse response) {
        return authService.updateProfile(UUID.fromString(jwt.getSubject()), request)
                .map(result -> {
                    response.addCookie(cookie(result.token()));
                    return ResponseEntity.ok(result.session());
                });
    }

    @GetMapping("/csrf")
    public Mono<ResponseEntity<Void>> csrf() {
        return Mono.just(ResponseEntity.noContent().build());
    }

    private String clientIp(ServerWebExchange exchange) {
        String forwarded = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        var remote = exchange.getRequest().getRemoteAddress();
        if (remote == null) {
            return "desconocida";
        }

        if (remote.getAddress() != null) {
            return remote.getAddress().getHostAddress();
        }
        return remote.getHostString();
    }

    private ResponseCookie cookie(String token) {
        return ResponseCookie.from(CookieAuthWebFilter.COOKIE, token)
                .httpOnly(true)
                .secure(properties.cookieSecure())
                .sameSite(properties.cookieSameSite())
                .path("/")
                .maxAge(properties.tokenTtl().getSeconds())
                .build();
    }
}
