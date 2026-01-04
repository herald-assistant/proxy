package com.acme.herald.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

import java.time.Instant;

public class AuthDtos {

    @Schema(description = "Request payload for wrapping an existing Provider token into an encrypted session token.")
    public record WrapReq(

            @Schema(description = "Raw Provider token to be wrapped.", example = "********")
            @NotBlank
            String token,

            @Schema(description = "Requested token TTL in days. When omitted, the server default is used.", example = "7")
            Integer ttlDays
    ) {}

    @Schema(description = "Response payload containing a wrapped token and cookie metadata.")
    public record WrapRes(

            @Schema(description = "Encrypted wrapped token value.", example = "eyJ2Ijo... (encrypted)")
            String token,

            @Schema(description = "Cookie name that should store the wrapped token.", example = "HERALD_AUTH")
            String cookieName,

            @Schema(description = "Token expiration timestamp (ISO-8601).", example = "2026-02-04T16:12:33Z")
            Instant expiresAt
    ) {}

    @Schema(description = "Request payload for creating a Provider personal access token using username and password.")
    public record LoginPatReq(

            @Schema(description = "Provider username.", example = "john.doe")
            @NotBlank
            String username,

            @Schema(description = "Provider password.", example = "********")
            @NotBlank
            String pd,

            @Schema(description = "Requested token TTL in days. Server clamps it to the configured maximum.", example = "14")
            Integer ttlDays
    ) {}
}
