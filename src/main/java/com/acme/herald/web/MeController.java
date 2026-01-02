package com.acme.herald.web;

import com.acme.herald.domain.dto.MeContextDtos;
import com.acme.herald.service.MeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/me", produces = MediaType.APPLICATION_JSON_VALUE)
public class MeController {
    private final MeService service;

    @GetMapping("/context")
    public MeContextDtos.MeContext context() {
        return service.context();
    }

    @GetMapping("/profile")
    public MeContextDtos.UserProfilePrefs myProfile() {
        return service.myProfile();
    }

    @PutMapping(value = "/profile", consumes = MediaType.APPLICATION_JSON_VALUE)
    public MeContextDtos.UserProfilePrefs updateProfile(@RequestBody @Valid MeContextDtos.UpdateUserProfilePrefs req) {
        return service.updateProfile(req);
    }
}