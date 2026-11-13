package com.accenture.franchises.controllers;

import com.accenture.franchises.contracts.request.BranchRequest;
import com.accenture.franchises.contracts.response.BranchResponse;
import com.accenture.franchises.security.Actor;
import com.accenture.franchises.services.FranchiseService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/franchises/{fid}/branches")
public class BranchController {

    private final FranchiseService franchises;

    public BranchController(FranchiseService franchises) {
        this.franchises = franchises;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('READ')")
    public Mono<List<BranchResponse>> list(@PathVariable UUID fid,
                                            Authentication authentication) {
        return franchises.branches(fid, Actor.of(authentication));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('CREATE')")
    public Mono<ResponseEntity<BranchResponse>> create(@PathVariable UUID fid,
                                                        @Valid @RequestBody BranchRequest request,
                                                        UriComponentsBuilder location) {
        return franchises.addBranch(fid, request.name())
                .map(created -> ResponseEntity
                        .created(location.pathSegment(created.id().toString()).build().toUri())
                        .body(created));
    }

    @PatchMapping("/{bid}")
    @PreAuthorize("hasAuthority('UPDATE')")
    public Mono<BranchResponse> rename(@PathVariable UUID fid,
                                       @PathVariable UUID bid,
                                       @Valid @RequestBody BranchRequest request) {
        int version = request.version() == null ? -1 : request.version();
        return franchises.renameBranch(fid, bid, request.name(), version);
    }

    @DeleteMapping("/{bid}")
    @PreAuthorize("hasAuthority('DELETE')")
    public Mono<ResponseEntity<Void>> delete(@PathVariable UUID fid, @PathVariable UUID bid) {
        return franchises.deleteBranch(fid, bid).thenReturn(ResponseEntity.noContent().build());
    }
}
