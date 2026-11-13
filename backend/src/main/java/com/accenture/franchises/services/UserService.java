package com.accenture.franchises.services;

import com.accenture.franchises.contracts.request.UserCreateRequest;
import com.accenture.franchises.contracts.request.UserUpdateRequest;
import com.accenture.franchises.contracts.response.PageResponse;
import com.accenture.franchises.contracts.response.UserResponse;
import com.accenture.franchises.entities.PermissionEntity;
import com.accenture.franchises.entities.RoleEntity;
import com.accenture.franchises.entities.UserEntity;
import com.accenture.franchises.entities.UserRoleEntity;
import com.accenture.franchises.exceptions.ApiException;
import com.accenture.franchises.repositories.PermissionRepository;
import com.accenture.franchises.repositories.RolePermissionRepository;
import com.accenture.franchises.repositories.RoleRepository;
import com.accenture.franchises.repositories.UserRepository;
import com.accenture.franchises.repositories.UserRoleRepository;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
public class UserService {

    public static final int MAX_PAGE_SIZE = 100;

    private final UserRepository users;
    private final UserRoleRepository userRoles;
    private final RoleRepository roles;
    private final RolePermissionRepository rolePermissions;
    private final PermissionRepository permissions;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository users, UserRoleRepository userRoles, RoleRepository roles,
                       RolePermissionRepository rolePermissions, PermissionRepository permissions,
                       PasswordEncoder passwordEncoder) {
        this.users = users;
        this.userRoles = userRoles;
        this.roles = roles;
        this.rolePermissions = rolePermissions;
        this.permissions = permissions;
        this.passwordEncoder = passwordEncoder;
    }

    public Mono<PageResponse<UserResponse>> page(int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        return users.findAllByOrderByCreatedAtDesc().collectList()
                .flatMap(list -> {
                    int from = Math.min(safePage * safeSize, list.size());
                    int to = Math.min(from + safeSize, list.size());
                    List<UserEntity> slice = list.subList(from, to);
                    return roleCodesByUser(slice.stream().map(UserEntity::getId).toList())
                            .map(byUser -> PageResponse.of(
                                    slice.stream()
                                            .map(user -> toResponse(user,
                                                    byUser.getOrDefault(user.getId(), List.of())))
                                            .toList(),
                                    safePage, safeSize, list.size()));
                });
    }

    public Mono<UserResponse> get(UUID id) {
        return users.findById(id)
                .switchIfEmpty(Mono.error(notFound()))
                .flatMap(entity -> rolesOf(id).map(roleCodes -> toResponse(entity, roleCodes)));
    }

    public Mono<UserResponse> create(UserCreateRequest request) {
        return create(request.email(), request.password(), request.displayName(), request.roles());
    }

    public Mono<UserResponse> create(String email, String rawPassword, String displayName,
                                     List<String> roleCodes) {
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        List<String> safeRoles = roleCodes == null || roleCodes.isEmpty()
                ? List.of("USER") : roleCodes;
        Mono<Void> rolesChecked = roleCodes == null || roleCodes.isEmpty()
                ? Mono.empty() : validateRoles(roleCodes);
        return rolesChecked
                .then(users.findByEmailIgnoreCase(normalized))
                .flatMap(existing -> Mono.<UserEntity>error(ApiException.conflict("email-taken",
                        "Ese correo ya esta registrado.")))
                .switchIfEmpty(hash(rawPassword).map(hash -> newEntity(normalized, hash, displayName)))
                .flatMap(users::save)
                .flatMap(saved -> replaceRoles(saved.getId(), safeRoles)
                        .then(rolesOf(saved.getId()))
                        .map(roleList -> toResponse(saved, roleList)));
    }

    public Mono<UserResponse> update(UUID id, UserUpdateRequest request, UUID actorId) {
        Mono<Void> rolesChecked = request.roles() == null ? Mono.empty() : validateRoles(request.roles());
        return rolesChecked.then(users.findById(id))
                .switchIfEmpty(Mono.error(notFound()))
                .flatMap(entity -> {
                    if (!request.version().equals(entity.getVersion())) {
                        return Mono.error(conflict());
                    }
                    if (request.displayName() != null && !request.displayName().isBlank()) {
                        entity.setDisplayName(request.displayName().trim());
                    }
                    if (request.enabled() != null) {
                        if (!request.enabled() && id.equals(actorId)) {
                            return Mono.error(ApiException.badRequest(
                                    "No puedes deshabilitar tu propia cuenta."));
                        }
                        entity.setEnabled(request.enabled());
                    }
                    entity.setUpdatedAt(Instant.now());
                    return users.save(entity)
                            .onErrorMap(OptimisticLockingFailureException.class, error -> conflict())
                            .flatMap(saved -> rolesUpdated(saved.getId(), request.roles())
                                    .then(rolesOf(saved.getId()))
                                    .map(roleList -> toResponse(saved, roleList)));
                });
    }

    public Mono<UserEntity> updateProfile(UUID id, String displayName, String rawPassword) {
        return users.findById(id)
                .switchIfEmpty(Mono.error(notFound()))
                .flatMap(entity -> {
                    if (displayName != null && !displayName.isBlank()) {
                        entity.setDisplayName(displayName.trim());
                    }
                    if (rawPassword == null || rawPassword.isBlank()) {
                        return Mono.just(entity);
                    }
                    return hash(rawPassword).map(hash -> {
                        entity.setPasswordHash(hash);
                        return entity;
                    });
                })
                .flatMap(entity -> {
                    entity.setUpdatedAt(Instant.now());
                    return users.save(entity)
                            .onErrorMap(OptimisticLockingFailureException.class, error -> conflict());
                });
    }

    public Mono<Void> delete(UUID id, UUID actorId) {
        if (id.equals(actorId)) {
            return Mono.error(ApiException.badRequest("No puedes eliminar tu propia cuenta."));
        }
        return users.findById(id)
                .switchIfEmpty(Mono.error(notFound()))
                .flatMap(users::delete);
    }

    public Mono<UserEntity> findByEmail(String email) {
        return users.findByEmailIgnoreCase(email);
    }

    public Mono<UserEntity> findById(UUID id) {
        return users.findById(id);
    }

    public Mono<List<String>> rolesOf(UUID userId) {
        return userRoles.findByUserId(userId)
                .map(UserRoleEntity::roleId)
                .collectList()
                .flatMap(ids -> ids.isEmpty()
                        ? Mono.just(List.<String>of())
                        : roles.findAllById(ids)
                                .map(RoleEntity::code)
                                .sort(Comparator.naturalOrder())
                                .collectList());
    }

    public Mono<List<String>> permissionsOf(UUID userId) {
        return userRoles.findByUserId(userId)
                .map(UserRoleEntity::roleId)
                .collectList()
                .flatMap(roleIds -> roleIds.isEmpty()
                        ? Mono.just(List.<String>of())
                        : rolePermissions.findByRoleIdIn(roleIds)
                                .map(permission -> permission.permissionId())
                                .collectList()
                                .flatMap(ids -> ids.isEmpty()
                                        ? Mono.just(List.<String>of())
                                        : permissions.findAllById(ids)
                                                .map(PermissionEntity::code)
                                                .distinct()
                                                .sort(Comparator.naturalOrder())
                                                .collectList()));
    }

    public Mono<List<String>> existingRoleCodes() {
        return roles.findAllByOrderByCodeAsc()
                .map(RoleEntity::code)
                .collectList();
    }

    public Mono<Void> replaceRoles(UUID userId, List<String> roleCodes) {
        return userRoles.deleteByUserId(userId)
                .thenMany(Flux.fromIterable(roleCodes)
                        .concatMap(code -> roles.findByCode(code)
                                .flatMap(role -> userRoles.save(new UserRoleEntity(
                                        null, userId, role.id())))))
                .then();
    }

    private Mono<Map<UUID, List<String>>> roleCodesByUser(List<UUID> userIds) {
        if (userIds.isEmpty()) {
            return Mono.just(Map.of());
        }
        return userRoles.findByUserIdIn(userIds).collectList()
                .flatMap(links -> {
                    List<UUID> roleIds = links.stream().map(UserRoleEntity::roleId).distinct().toList();
                    Mono<Map<UUID, String>> codes = roleIds.isEmpty()
                            ? Mono.just(Map.of())
                            : roles.findAllById(roleIds).collectList()
                                    .map(list -> list.stream().collect(Collectors.toMap(
                                            RoleEntity::id, RoleEntity::code)));
                    return codes.map(byId -> links.stream().collect(Collectors.groupingBy(
                            UserRoleEntity::userId,
                            Collectors.mapping(link -> byId.getOrDefault(link.roleId(), ""),
                                    Collectors.toList()))));
                });
    }

    private Mono<Void> rolesUpdated(UUID userId, List<String> roleCodes) {
        return roleCodes == null ? Mono.empty() : replaceRoles(userId, roleCodes);
    }

    private Mono<Void> validateRoles(List<String> roleCodes) {
        if (roleCodes.isEmpty()) {
            return Mono.error(ApiException.badRequest("El usuario debe tener al menos un rol."));
        }
        return existingRoleCodes().flatMap(existing -> {
            List<String> unknown = roleCodes.stream().filter(role -> !existing.contains(role)).toList();
            return unknown.isEmpty()
                    ? Mono.empty()
                    : Mono.error(ApiException.badRequest("Rol inexistente: " + String.join(", ", unknown)));
        });
    }

    private UserEntity newEntity(String email, String hash, String displayName) {
        Instant now = Instant.now();
        UserEntity entity = new UserEntity();
        entity.setId(UUID.randomUUID());
        entity.setEmail(email);
        entity.setPasswordHash(hash);
        entity.setDisplayName(displayName.trim());
        entity.setEnabled(true);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        return entity;
    }

    private Mono<String> hash(String rawPassword) {
        return Mono.fromCallable(() -> passwordEncoder.encode(rawPassword))
                .subscribeOn(Schedulers.boundedElastic());
    }

    private UserResponse toResponse(UserEntity entity, List<String> roleCodes) {
        return new UserResponse(entity.getId(), entity.getEmail(), entity.getDisplayName(),
                entity.isEnabled(), roleCodes,
                entity.getVersion() == null ? 0 : entity.getVersion(), entity.getCreatedAt());
    }

    private ApiException notFound() {
        return ApiException.notFound("Usuario no encontrado.");
    }

    private ApiException conflict() {
        return ApiException.conflict("concurrent-modification",
                "El usuario cambio desde que lo leiste. Recarga e intenta de nuevo.");
    }
}
