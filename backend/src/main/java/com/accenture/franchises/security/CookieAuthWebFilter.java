package com.accenture.franchises.security;

import java.util.ArrayList;
import java.util.List;
import org.springframework.http.HttpCookie;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

public class CookieAuthWebFilter implements WebFilter {

    public static final String COOKIE = "access_token";

    private final JwtService jwtService;

    public CookieAuthWebFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        HttpCookie cookie = exchange.getRequest().getCookies().getFirst(COOKIE);
        if (cookie == null || cookie.getValue().isBlank()) {
            return chain.filter(exchange);
        }

        Mono<JwtAuthenticationToken> authentication = Mono.defer(() -> jwtService.decoder().decode(cookie.getValue()))
                .onErrorResume(error -> Mono.empty())
                .map(this::authentication)
                .cache();
        Mono<Void> proceed = chain.filter(exchange);

        return authentication
                .map(auth -> proceed.contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth)))
                .defaultIfEmpty(proceed)
                .flatMap(result -> result);
    }

    private JwtAuthenticationToken authentication(Jwt jwt) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        for (String permission : claims(jwt, "permissions")) {
            authorities.add(new SimpleGrantedAuthority(permission));
        }
        for (String role : claims(jwt, "roles")) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
        }
        return new JwtAuthenticationToken(jwt, authorities, jwt.getSubject());
    }

    private List<String> claims(Jwt jwt, String name) {
        List<String> values = jwt.getClaimAsStringList(name);
        return values == null ? List.of() : values;
    }
}
