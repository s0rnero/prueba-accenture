package com.accenture.franchises.controllers;

import com.accenture.franchises.contracts.response.PageResponse;
import com.accenture.franchises.contracts.request.UserCreateRequest;
import com.accenture.franchises.contracts.response.UserResponse;
import com.accenture.franchises.contracts.request.UserUpdateRequest;
import com.accenture.franchises.services.UserService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService users;

    public UserController(UserService users) {
        this.users = users;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('READ')")
    public Mono<PageResponse<UserResponse>> list(@RequestParam(defaultValue = "0") int page,
                                                 @RequestParam(defaultValue = "10") int size) {
        return users.page(page, size);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('READ')")
    public Mono<UserResponse> get(@PathVariable UUID id) {
        return users.get(id);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('CREATE')")
    public Mono<ResponseEntity<UserResponse>> create(@Valid @RequestBody UserCreateRequest request,
                                                     UriComponentsBuilder location) {
        return users.create(request)
                .map(created -> ResponseEntity
                        .created(location.pathSegment(created.id().toString()).build().toUri())
                        .body(created));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('UPDATE')")
    public Mono<UserResponse> update(@PathVariable UUID id,
                                     @Valid @RequestBody UserUpdateRequest request,
                                     @AuthenticationPrincipal Jwt jwt) {
        return users.update(id, request, UUID.fromString(jwt.getSubject()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('DELETE')")
    public Mono<ResponseEntity<Void>> delete(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        return users.delete(id, UUID.fromString(jwt.getSubject()))
                .thenReturn(ResponseEntity.noContent().build());
    }
}
