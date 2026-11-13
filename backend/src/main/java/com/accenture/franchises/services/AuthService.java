package com.accenture.franchises.services;

import com.accenture.franchises.contracts.request.LoginRequest;
import com.accenture.franchises.contracts.request.ProfileUpdateRequest;
import com.accenture.franchises.contracts.request.RegisterRequest;
import com.accenture.franchises.contracts.response.SessionResponse;
import com.accenture.franchises.entities.UserEntity;
import com.accenture.franchises.exceptions.ApiException;
import com.accenture.franchises.security.JwtService;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
public class AuthService {

    private final UserService users;
    private final FranchiseService franchises;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserService users, FranchiseService franchises, JwtService jwtService,
                       PasswordEncoder passwordEncoder) {
        this.users = users;
        this.franchises = franchises;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
    }

    public record AuthResult(String token, SessionResponse session) {
    }

    public Mono<AuthResult> login(LoginRequest request) {
        return users.findByEmail(request.email())
                .switchIfEmpty(Mono.error(credentialsError()))
                .flatMap(user -> {
                    if (!user.isEnabled()) {
                        return Mono.error(new ApiException(HttpStatus.FORBIDDEN, "disabled",
                                "La cuenta esta deshabilitada."));
                    }
                    return Mono.fromCallable(() -> passwordEncoder.matches(request.password(),
                                    user.getPasswordHash()))
                            .subscribeOn(Schedulers.boundedElastic())
                            .flatMap(matches -> matches ? issue(user) : Mono.error(credentialsError()));
                });
    }

    public Mono<AuthResult> register(RegisterRequest request) {
        return users.create(request.email(), request.password(), request.displayName(),
                        List.of("USER"))
                .flatMap(created -> franchises.assignFirstFranchise(created.id())
                        .then(users.findById(created.id()))
                        .flatMap(this::issue));
    }

    public Mono<AuthResult> updateProfile(UUID userId, ProfileUpdateRequest request) {
        return users.updateProfile(userId, request.displayName(), request.password())
                .flatMap(this::issue);
    }

    public Mono<SessionResponse> session(UUID userId) {
        return users.findById(userId)
                .switchIfEmpty(Mono.error(ApiException.notFound("Usuario no encontrado.")))
                .flatMap(this::sessionOf);
    }

    private Mono<AuthResult> issue(UserEntity user) {
        return sessionOf(user).map(session -> new AuthResult(
                jwtService.issue(user.getId(), user.getEmail(), user.getDisplayName(),
                        session.roles(), session.permissions()),
                session));
    }

    private Mono<SessionResponse> sessionOf(UserEntity user) {
        return Mono.zip(users.rolesOf(user.getId()), users.permissionsOf(user.getId()))
                .map(tuple -> new SessionResponse(user.getId(), user.getEmail(),
                        user.getDisplayName(), tuple.getT1(), tuple.getT2()));
    }

    private ApiException credentialsError() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "bad-credentials",
                "Correo o contrasena incorrectos.");
    }
}
