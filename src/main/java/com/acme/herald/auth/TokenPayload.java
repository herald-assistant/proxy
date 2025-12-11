package com.acme.herald.auth;

import java.time.Instant;

/** Minimalny zestaw do bycia stateless */
public record TokenPayload(
        String token,            // PAT (dla bearer) lub apiToken (dla basic)
        Instant exp              // kiedy front ma odświeżyć (np. nowy PAT po 30 dniach)
) {}
