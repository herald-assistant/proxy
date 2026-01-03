package com.acme.herald.web;

import com.acme.herald.service.AuthService;
import com.acme.herald.web.admin.AuthDtos;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(path = "/proxy/auth", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class AuthController {
    private final AuthService service;

    @PostMapping("/wrap")
    public AuthDtos.WrapRes wrap(@RequestBody AuthDtos.WrapReq req) {
        return service.wrap(req);
    }


    @PostMapping(value = "/pat", consumes = MediaType.APPLICATION_JSON_VALUE)
    public AuthDtos.WrapRes createPat(@RequestBody AuthDtos.LoginPatReq req) {
        return service.createPat(req);
    }

    @DeleteMapping("/logout")
    public void logout() {
        service.revokeCurrentPat();
    }
}
