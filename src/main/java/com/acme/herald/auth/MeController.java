package com.acme.herald.auth;

import com.acme.herald.domain.dto.MeContextDtos;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/me", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "MeController", description = "Endpoints for retrieving the current user context and profile preferences.")
public class MeController {

    private final MeService service;

    @GetMapping("/context")
    @Operation(
            summary = "Get current user context",
            description = "Returns the current user, configured project key, resolved permissions, and stored profile preferences."
    )
    public MeContextDtos.MeContext context() {
        return service.context();
    }

    @GetMapping("/profile")
    @Operation(
            summary = "Get current user profile preferences",
            description = "Returns profile preferences stored for the current user."
    )
    public MeContextDtos.UserProfilePrefs myProfile() {
        return service.myProfile();
    }

    @PutMapping(value = "/profile", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Update current user profile preferences",
            description = "Updates profile preferences stored for the current user and returns the updated preferences."
    )
    public MeContextDtos.UserProfilePrefs updateProfile(
            @RequestBody @Valid MeContextDtos.UpdateUserProfilePrefs req
    ) {
        return service.updateProfile(req);
    }
}
