package com.acme.herald.auth;

public final class JiraAuthorization {
    private JiraAuthorization() {}

    /**
     * Zwraca wartość do headera Authorization dla calli do Jiry.
     * Obsługuje:
     *  - "Basic ...." (np. login:hasło / email:apiToken)
     *  - "Bearer ...." (PAT)
     *  - goły token (traktujemy jako PAT -> "Bearer <token>")
     */
    public static String auth(TokenPayload tp) {
        if (tp == null) throw new IllegalStateException("NO_AUTH_CONTEXT");
        var token = tp.token();
        if (token == null || token.isBlank()) throw new IllegalStateException("NO_TOKEN");

        // Nie dotykamy jeśli user już podał pełny scheme
        if (startsWithIgnoreCase(token, "Basic ")) return token;
        if (startsWithIgnoreCase(token, "Bearer ")) return token;

        // Kompatybilność wstecz: goły PAT
        return "Bearer " + token;
    }

    private static boolean startsWithIgnoreCase(String s, String prefix) {
        if (s.length() < prefix.length()) return false;
        return s.regionMatches(true, 0, prefix, 0, prefix.length());
    }
}
