package com.accenture.franchises.security;

import java.util.UUID;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

public record Actor(UUID userId, boolean admin) {

    private static final GrantedAuthority ADMIN = new SimpleGrantedAuthority("ROLE_ADMIN");

    public static Actor of(Authentication authentication) {
        JwtAuthenticationToken token = (JwtAuthenticationToken) authentication;
        return new Actor(
                UUID.fromString(token.getToken().getSubject()),
                token.getAuthorities().contains(ADMIN));
    }
}
