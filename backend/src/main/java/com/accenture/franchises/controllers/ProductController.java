package com.accenture.franchises.controllers;

import com.accenture.franchises.contracts.request.ProductRequest;
import com.accenture.franchises.contracts.response.PageResponse;
import com.accenture.franchises.contracts.response.ProductResponse;
import com.accenture.franchises.security.Actor;
import com.accenture.franchises.services.ProductService;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/franchises/{fid}")
public class ProductController {

    private final ProductService products;

    public ProductController(ProductService products) {
        this.products = products;
    }

    @GetMapping("/products")
    @PreAuthorize("hasAuthority('READ')")
    public Mono<PageResponse<ProductResponse>> list(@PathVariable UUID fid,
                                                    @RequestParam(defaultValue = "0") int page,
                                                    @RequestParam(defaultValue = "10") int size,
                                                    @RequestParam(defaultValue = "") String search,
                                                    Authentication authentication) {
        return products.page(fid, page, size, search, Actor.of(authentication));
    }

    @PostMapping("/branches/{bid}/products")
    @PreAuthorize("hasAuthority('CREATE')")
    public Mono<ResponseEntity<ProductResponse>> create(@PathVariable UUID fid,
                                                        @PathVariable UUID bid,
                                                        @Valid @RequestBody ProductRequest request,
                                                        UriComponentsBuilder location) {
        return products.create(fid, bid, request.name(), request.stock(), request.price())
                .map(created -> ResponseEntity
                        .created(location.pathSegment(created.id().toString()).build().toUri())
                        .body(created));
    }

    @PatchMapping("/branches/{bid}/products/{pid}")
    @PreAuthorize("hasAuthority('UPDATE')")
    public Mono<ProductResponse> update(@PathVariable UUID fid, @PathVariable UUID bid,
                                        @PathVariable UUID pid,
                                        @Valid @RequestBody ProductRequest request) {
        return products.update(fid, bid, pid, request.name(), request.stock(),
                request.price(), request.version());
    }

    @PatchMapping("/branches/{bid}/products/{pid}/stock")
    @PreAuthorize("hasAuthority('UPDATE')")
    public Mono<ProductResponse> updateStock(@PathVariable UUID fid, @PathVariable UUID bid,
                                             @PathVariable UUID pid,
                                             @Valid @RequestBody ProductRequest request) {
        return products.updateStock(fid, bid, pid, request.stock(), request.version());
    }

    @DeleteMapping("/branches/{bid}/products/{pid}")
    @PreAuthorize("hasAuthority('DELETE')")
    public Mono<ResponseEntity<Void>> delete(@PathVariable UUID fid, @PathVariable UUID bid,
                                             @PathVariable UUID pid) {
        return products.delete(fid, bid, pid).thenReturn(ResponseEntity.noContent().build());
    }
}
