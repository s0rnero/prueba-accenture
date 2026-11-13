package com.accenture.franchises.controllers;

import com.accenture.franchises.contracts.request.FranchiseRequest;
import com.accenture.franchises.contracts.request.MembersRequest;
import com.accenture.franchises.contracts.response.FranchiseResponse;
import com.accenture.franchises.contracts.response.PageResponse;
import com.accenture.franchises.security.Actor;
import com.accenture.franchises.services.FranchiseService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/franchises")
public class FranchiseController {

    private final FranchiseService franchises;

    public FranchiseController(FranchiseService franchises) {
        this.franchises = franchises;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('READ')")
    public Mono<PageResponse<FranchiseResponse>> list(@RequestParam(defaultValue = "0") int page,
                                                      @RequestParam(defaultValue = "10") int size,
                                                      Authentication authentication) {
        return franchises.page(page, size, Actor.of(authentication));
    }

    @GetMapping("/{fid}")
    @PreAuthorize("hasAuthority('READ')")
    public Mono<FranchiseResponse> get(@PathVariable UUID fid, Authentication authentication) {
        return franchises.get(fid, Actor.of(authentication));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('CREATE')")
    public Mono<ResponseEntity<FranchiseResponse>> create(
            @Valid @RequestBody FranchiseRequest request, UriComponentsBuilder location) {
        return franchises.create(request.name())
                .map(created -> ResponseEntity
                        .created(location.pathSegment(created.id().toString()).build().toUri())
                        .body(created));
    }

    @PatchMapping("/{fid}")
    @PreAuthorize("hasAuthority('UPDATE')")
    public Mono<FranchiseResponse> rename(@PathVariable UUID fid,
                                          @RequestParam(defaultValue = "-1") int version,
                                          @Valid @RequestBody FranchiseRequest request) {
        return franchises.rename(fid, request.name(), version);
    }

    @DeleteMapping("/{fid}")
    @PreAuthorize("hasAuthority('DELETE')")
    public Mono<ResponseEntity<Void>> delete(@PathVariable UUID fid) {
        return franchises.delete(fid).thenReturn(ResponseEntity.noContent().build());
    }

    @PutMapping("/{fid}/members")
    @PreAuthorize("hasAuthority('UPDATE')")
    public Mono<ResponseEntity<Void>> members(@PathVariable UUID fid,
                                               @RequestBody MembersRequest request) {
        return franchises.replaceMembers(fid, request.userIds())
                .thenReturn(ResponseEntity.noContent().build());
    }
}
