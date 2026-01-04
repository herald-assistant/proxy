package com.acme.herald.auth;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(path = "/proxy/auth", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Tag(name = "AuthController", description = "Authentication endpoints for the Provider proxy.")
public class AuthController {

    private final AuthService service;

    @PostMapping(value = "/wrap", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Wrap a Provider token",
            description = "Encrypts an existing Provider token into a wrapped token suitable for storing in a cookie. The server applies TTL clamping based on configuration."
    )
    public AuthDtos.WrapRes wrap(
            @RequestBody @Valid AuthDtos.WrapReq req
    ) {
        return service.wrap(req);
    }

    @PostMapping(value = "/pat", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Create a Provider personal access token and wrap it",
            description = "Creates a Provider personal access token using username/password, then encrypts it into a wrapped token suitable for storing in a cookie. The server clamps TTL to the configured maximum."
    )
    public AuthDtos.WrapRes createPat(
            @RequestBody @Valid AuthDtos.LoginPatReq req
    ) {
        return service.createPat(req);
    }

    @DeleteMapping("/logout")
    @Operation(
            summary = "Logout and revoke current token",
            description = "Revokes the current Provider token (if revocable) and invalidates the current session context."
    )
    public ResponseEntity<Void> logout(
            @Parameter(description = "No request body.")
            @RequestBody(required = false) String ignored
    ) {
        service.revokeCurrentPat();
        return ResponseEntity.noContent().build();
    }
}
