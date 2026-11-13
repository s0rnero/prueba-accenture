package com.accenture.franchises.security;

import com.accenture.franchises.config.SecurityProperties;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.stereotype.Component;

@Component
public class JwtService {

    private final SecurityProperties properties;
    private final SecretKey key;

    public JwtService(SecurityProperties properties) {
        this.properties = properties;
        String secret = properties.jwtSecret() == null ? "" : properties.jwtSecret();
        if (secret.getBytes().length < 32) {
            throw new IllegalStateException("JWT_SECRET debe tener al menos 32 caracteres");
        }
        this.key = new SecretKeySpec(secret.getBytes(), "HmacSHA256");
    }

    public String issue(UUID userId, String email, String displayName, List<String> roles,
                        List<String> permissions) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("acento")
                .issuedAt(now)
                .expiresAt(now.plus(properties.tokenTtl()))
                .subject(userId.toString())
                .claim("email", email)
                .claim("name", displayName)
                .claim("roles", roles)
                .claim("permissions", permissions)
                .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder().encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    public ReactiveJwtDecoder decoder() {
        return NimbusReactiveJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
    }

    private JwtEncoder encoder() {
        return new NimbusJwtEncoder(new ImmutableSecret<>(key));
    }
}
