package com.acme.herald.auth;

import java.time.Instant;

/** Minimalny zestaw do bycia stateless */
public record TokenPayload(
        String token,            // może być: "Bearer xxx", "Basic yyy" albo goły PAT
        Instant exp
) {}
