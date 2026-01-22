package com.acme.herald.challenges;

import com.acme.herald.config.JiraConfigService;
import com.acme.herald.config.JiraProperties;
import com.acme.herald.domain.JiraModels;
import com.acme.herald.domain.dto.ChallengeDtos;
import com.acme.herald.provider.JiraProvider;
import com.acme.herald.web.error.ForbiddenException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.regex.Pattern;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Service
@RequiredArgsConstructor
public class ChallengeService {

    private static final String PROP_KEY = "herald.template-hub.challenges.v1";
    private static final String PERM_ADMIN = "ADMINISTER_PROJECTS";

    private static final Pattern SAFE_ID = Pattern.compile("^[a-zA-Z0-9_\\-]{6,64}$");

    private final JiraProvider jira;
    private final JiraProperties jiraProps;
    private final JiraConfigService jiraConfigService;
    private final JsonMapper json;

    // ───────────────────────────── Public API ─────────────────────────────

    public List<ChallengeDtos.Challenge> list() {
        var issueKey = getChallengesIssueKeyOrNull();
        if (!isNotBlank(issueKey)) return List.of(); // feature disabled

        ChallengeStore store = loadStore(issueKey.trim());
        return (store.items() != null ? store.items() : List.<ChallengeEntry>of()).stream()
                .map(this::toDto)
                .toList();
    }

    public ChallengeDtos.Challenge get(String id) {
        var issueKey = requireChallengesIssueKey();
        ChallengeStore store = loadStore(issueKey);

        ChallengeEntry found = findOrThrow(store, id);
        return toDto(found);
    }

    public ChallengeDtos.Challenge create(ChallengeDtos.CreateChallengeReq req) {
        var issueKey = requireChallengesIssueKey();
        JiraModels.UserResponse me = jira.getMe();

        String now = Instant.now().toString();
        String id = generateId();

        ChallengeEntry next = new ChallengeEntry(
                id,
                nz(req.label()),
                nz(req.deadline()),
                nz(req.description()),
                safeUserKey(me),
                nz(me != null ? me.displayName() : ""),
                now,
                now
        );

        ChallengeStore store = loadStore(issueKey);
        List<ChallengeEntry> items = new ArrayList<>(store.items() != null ? store.items() : List.of());
        items.add(next);

        saveStore(issueKey, new ChallengeStore(1, now, items));
        return toDto(next);
    }

    public ChallengeDtos.Challenge update(String id, ChallengeDtos.UpdateChallengeReq req) {
        var issueKey = requireChallengesIssueKey();
        JiraModels.UserResponse me = jira.getMe();
        String now = Instant.now().toString();

        ChallengeStore store = loadStore(issueKey);
        List<ChallengeEntry> items = new ArrayList<>(store.items() != null ? store.items() : List.of());

        int idx = indexOf(items, id);
        if (idx < 0) throw notFound("Challenge not found: " + id);

        ChallengeEntry current = items.get(idx);
        requireCanEdit(me, current);

        ChallengeEntry updated = new ChallengeEntry(
                current.id(),
                isNotBlank(req.label()) ? nz(req.label()) : current.label(),
                // deadline można wyczyścić (null/blank) -> ustawiamy ""
                req.deadline() != null ? nz(req.deadline()) : current.deadline(),
                req.description() != null ? nz(req.description()) : current.description(),
                current.authorKey(),
                current.authorDisplayName(),
                current.createdAt(),
                now
        );

        items.set(idx, updated);
        saveStore(issueKey, new ChallengeStore(1, now, items));

        return toDto(updated);
    }

    public void delete(String id) {
        var issueKey = requireChallengesIssueKey();
        JiraModels.UserResponse me = jira.getMe();
        String now = Instant.now().toString();

        ChallengeStore store = loadStore(issueKey);
        List<ChallengeEntry> items = new ArrayList<>(store.items() != null ? store.items() : List.of());

        int idx = indexOf(items, id);
        if (idx < 0) throw notFound("Challenge not found: " + id);

        ChallengeEntry current = items.get(idx);
        requireCanEdit(me, current);

        items.remove(idx);
        saveStore(issueKey, new ChallengeStore(1, now, items));
    }

    // ───────────────────────────── Storage ─────────────────────────────

    private ChallengeStore loadStore(String issueKey) {
        try {
            JsonNode v = jira.getIssueProperty(issueKey, PROP_KEY);

            if (v == null || v.isMissingNode() || v.isNull() || (v.isObject() && v.size() == 0)) {
                return new ChallengeStore(1, null, List.of());
            }

            ChallengeStore parsed = json.treeToValue(v, ChallengeStore.class);
            if (parsed == null) return new ChallengeStore(1, null, List.of());

            // minimal sanitize
            List<ChallengeEntry> sane = (parsed.items() == null ? List.<ChallengeEntry>of() : parsed.items())
                    .stream()
                    .filter(Objects::nonNull)
                    .map(this::sanitizeEntry)
                    .filter(Objects::nonNull)
                    .toList();

            return new ChallengeStore(
                    parsed.version() != null ? parsed.version() : 1,
                    nzOrNull(parsed.updatedAt()),
                    sane
            );
        } catch (Exception e) {
            return new ChallengeStore(1, null, List.of());
        }
    }

    private void saveStore(String issueKey, ChallengeStore store) {
        // request do Jiry spokojnie może być record -> serialize do JSON
        jira.setIssueProperty(issueKey, PROP_KEY, store);
    }

    private ChallengeEntry sanitizeEntry(ChallengeEntry e) {
        if (e == null) return null;
        String id = nz(e.id());
        if (!SAFE_ID.matcher(id).matches()) return null;

        return new ChallengeEntry(
                id,
                nz(e.label()),
                nz(e.deadline()),
                nz(e.description()),
                nz(e.authorKey()),
                nz(e.authorDisplayName()),
                nz(e.createdAt()),
                nz(e.updatedAt())
        );
    }

    // ───────────────────────────── Permissions ─────────────────────────────

    private void requireCanEdit(JiraModels.UserResponse me, ChallengeEntry entry) {
        if (entry == null) throw notFound("Challenge not found.");
        String myKey = safeUserKey(me);

        boolean isAuthor = isNotBlank(myKey) && myKey.equals(nz(entry.authorKey()));
        boolean isAdmin = isProjectAdmin();

        if (!isAuthor && !isAdmin) {
            throw new ForbiddenException("Brak uprawnień: tylko autor zgłoszenia lub admin może modyfikować challenge.");
        }
    }

    private boolean isProjectAdmin() {
        JiraModels.PermissionsResponse perms = jira.getMyPermissions(jiraProps.getProjectKey(), null, null);
        var map = perms != null ? perms.permissions() : null;

        return map != null
                && map.get(PERM_ADMIN) != null
                && Boolean.TRUE.equals(map.get(PERM_ADMIN).havePermission());
    }

    // ───────────────────────────── Config ─────────────────────────────

    private String getChallengesIssueKeyOrNull() {
        var cfg = jiraConfigService.getForRuntime();
        return cfg != null ? cfg.challengesIssueKey() : null;
    }

    private String requireChallengesIssueKey() {
        String k = getChallengesIssueKeyOrNull();
        if (!isNotBlank(k)) {
            throw new ResponseStatusException(
                    HttpStatus.PRECONDITION_FAILED,
                    "Challenges storage is not configured (missing challengesIssueKey in admin config)."
            );
        }
        return k.trim();
    }

    // ───────────────────────────── Helpers ─────────────────────────────

    private static int indexOf(List<ChallengeEntry> items, String id) {
        String want = nz(id);
        for (int i = 0; i < items.size(); i++) {
            if (want.equals(nz(items.get(i).id()))) return i;
        }
        return -1;
    }

    private static ChallengeEntry findOrThrow(ChallengeStore store, String id) {
        List<ChallengeEntry> items = store.items() != null ? store.items() : List.of();
        String want = nz(id);
        for (ChallengeEntry it : items) {
            if (want.equals(nz(it.id()))) return it;
        }
        throw notFound("Challenge not found: " + want);
    }

    private static ResponseStatusException notFound(String msg) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, msg);
    }

    private ChallengeDtos.Challenge toDto(ChallengeEntry e) {
        return new ChallengeDtos.Challenge(
                e.id(),
                e.label(),
                blankToNull(e.deadline()),
                blankToNull(e.description()),
                blankToNull(e.authorKey()),
                blankToNull(e.authorDisplayName()),
                blankToNull(e.createdAt()),
                blankToNull(e.updatedAt())
        );
    }

    private static String generateId() {
        // krótki, stabilny format pod URL/JSON
        byte[] b = new byte[9];
        new Random().nextBytes(b);
        return "ch_" + Base64.getUrlEncoder().withoutPadding().encodeToString(b);
    }

    private static String safeUserKey(JiraModels.UserResponse user) {
        if (user == null) return "unknown";
        if (isNotBlank(user.key())) return user.key();
        if (isNotBlank(user.name())) return user.name();
        return isNotBlank(user.displayName()) ? user.displayName() : "unknown";
    }

    private static String nz(String s) {
        return s == null ? "" : s.trim();
    }

    private static String nzOrNull(String s) {
        String v = nz(s);
        return v.isBlank() ? null : v;
    }

    private static String blankToNull(String s) {
        if (s == null) return null;
        String v = s.trim();
        return v.isBlank() ? null : v;
    }

    // ───────────────────────────── Storage model ─────────────────────────────

    public record ChallengeStore(
            Integer version,
            String updatedAt,
            List<ChallengeEntry> items
    ) {}

    public record ChallengeEntry(
            String id,
            String label,
            String deadline,
            String description,
            String authorKey,
            String authorDisplayName,
            String createdAt,
            String updatedAt
    ) {}
}
