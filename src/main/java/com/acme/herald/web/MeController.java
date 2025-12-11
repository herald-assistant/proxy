package com.acme.herald.web;

import com.acme.herald.domain.JiraModels;
import com.acme.herald.provider.JiraProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/me", produces = MediaType.APPLICATION_JSON_VALUE)
public class MeController {
    private final JiraProvider jira;

    @GetMapping()
    public JiraModels.UserResponse me() {
        return jira.getMe();
    }
}