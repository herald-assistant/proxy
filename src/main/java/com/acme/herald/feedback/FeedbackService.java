package com.acme.herald.feedback;

import com.acme.herald.config.JiraConfigService;
import com.acme.herald.config.JiraProperties;
import com.acme.herald.domain.JiraModels;
import com.acme.herald.domain.dto.FeedbackDtos;
import com.acme.herald.provider.JiraProvider;
import com.acme.herald.web.error.ForbiddenException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.*;
import java.util.regex.Pattern;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Service
@RequiredArgsConstructor
public class FeedbackService {

    /**
     * Storage: Jira issue property (na issue wskazanym w admin config).
     * Możesz trzymać to na tym samym issue co challenges, ale w innym PROP_KEY.
     */
    private static final String PROP_KEY = "herald.template-hub.feedback.v1";

    /**
     * Uwaga: tu (dla minimalnych zmian) wykorzystuję istniejące cfg.challengesIssueKey()
     * jako "storage issue" – dokładnie tak jak challenges. Jeśli chcesz mieć osobne pole
     * feedbackIssueKey, dopisz je do JiraConfigService i podmień getter tutaj.
     */
    private static final String PERM_ADMIN = "ADMINISTER_PROJECTS";
    private static final Pattern SAFE_ID = Pattern.compile("^[a-zA-Z0-9_\\-]{6,64}$");

    private static final Set<String> ALLOWED_STATUS = Set.of("TODO", "IN_PROGRESS", "DONE", "REJECTED");
    private static final Set<String> ALLOWED_TYPE = Set.of("BUG", "IDEA");

    private final JiraProvider jira;
    private final JiraProperties jiraProps;
    private final JiraConfigService jiraConfigService;
    private final JsonMapper json;

    // ───────────────────────────── Public API ─────────────────────────────

    public List<FeedbackDtos.Feedback> list(String type, String status, boolean mine) {
        var issueKey = getFeedbackIssueKeyOrNull();
        if (!isNotBlank(issueKey)) return List.of(); // feature disabled

        JiraModels.UserResponse me = mine ? jira.getMe() : null;
        String myKey = mine ? safeUserKey(me) : null;

        String wantType = normalizeTypeForFilter(type);
        String wantStatus = normalizeStatusForFilter(status);

        FeedbackStore store = loadStore(issueKey.trim());

        return (store.items() != null ? store.items() : List.<FeedbackEntry>of()).stream()
                .filter(Objects::nonNull)
                .filter(e -> wantType == null || wantType.equals(nz(e.type())))
                .filter(e -> wantStatus == null || wantStatus.equals(nz(e.status())))
                .filter(e -> !mine || (isNotBlank(myKey) && myKey.equals(nz(e.authorKey()))))
                .sorted((a, b) -> {
                    // najpierw updatedAt desc, potem createdAt desc
                    int c = nz(b.updatedAt()).compareTo(nz(a.updatedAt()));
                    if (c != 0) return c;
                    return nz(b.createdAt()).compareTo(nz(a.createdAt()));
                })
                .map(this::toDto)
                .toList();
    }

    public FeedbackDtos.Feedback get(String id) {
        var issueKey = requireFeedbackIssueKey();
        FeedbackStore store = loadStore(issueKey);

        FeedbackEntry found = findOrThrow(store, id);
        return toDto(found);
    }

    public FeedbackDtos.FeedbackStats stats() {
        var issueKey = getFeedbackIssueKeyOrNull();
        if (!isNotBlank(issueKey)) {
            return new FeedbackDtos.FeedbackStats(0,0,0,0,0,0,0);
        }

        FeedbackStore store = loadStore(issueKey.trim());
        List<FeedbackEntry> items = store.items() != null ? store.items() : List.of();

        long total = 0, bugs = 0, ideas = 0, todo = 0, inProg = 0, done = 0, rej = 0;

        for (FeedbackEntry e : items) {
            if (e == null) continue;
            total++;

            String t = nz(e.type());
            if ("BUG".equals(t)) bugs++;
            if ("IDEA".equals(t)) ideas++;

            String s = nz(e.status());
            if ("TODO".equals(s)) todo++;
            if ("IN_PROGRESS".equals(s)) inProg++;
            if ("DONE".equals(s)) done++;
            if ("REJECTED".equals(s)) rej++;
        }

        return new FeedbackDtos.FeedbackStats(total, bugs, ideas, todo, inProg, done, rej);
    }

    public FeedbackDtos.Feedback create(FeedbackDtos.CreateFeedbackReq req) {
        var issueKey = requireFeedbackIssueKey();
        JiraModels.UserResponse me = jira.getMe();

        String now = Instant.now().toString();
        String id = generateId();

        String type = normalizeTypeForWrite(req.type());
        String status = "TODO"; // default

        FeedbackEntry next = new FeedbackEntry(
                id,
                type,
                status,
                nz(req.summary()),
                nz(req.description()),
                safeUserKey(me),
                nz(me != null ? me.displayName() : ""),
                now,
                now
        );

        FeedbackStore store = loadStore(issueKey);
        List<FeedbackEntry> items = new ArrayList<>(store.items() != null ? store.items() : List.of());
        items.add(next);

        saveStore(issueKey, new FeedbackStore(1, now, items));
        return toDto(next);
    }

    public FeedbackDtos.Feedback update(String id, FeedbackDtos.UpdateFeedbackReq req) {
        var issueKey = requireFeedbackIssueKey();
        JiraModels.UserResponse me = jira.getMe();
        String now = Instant.now().toString();

        FeedbackStore store = loadStore(issueKey);
        List<FeedbackEntry> items = new ArrayList<>(store.items() != null ? store.items() : List.of());

        int idx = indexOf(items, id);
        if (idx < 0) throw notFound("Feedback not found: " + id);

        FeedbackEntry current = items.get(idx);
        requireCanEdit(me, current);

        boolean isAdmin = isProjectAdmin();

        // status: tylko admin może zmieniać
        String nextStatus = current.status();
        if (req.status() != null) {
            if (!isAdmin) {
                throw new ForbiddenException("Brak uprawnień: tylko admin może zmieniać status zgłoszenia.");
            }
            String normalized = normalizeStatusForWrite(req.status(), null);
            if (normalized != null) nextStatus = normalized; // blank -> nie zmieniaj
        }

        FeedbackEntry updated = new FeedbackEntry(
                current.id(),
                current.type(), // type niezmienne w update (bug/idea nie powinno się flipować)
                nextStatus,
                (req.summary() != null && isNotBlank(req.summary())) ? nz(req.summary()) : current.summary(),
                (req.description() != null) ? nz(req.description()) : current.description(),
                current.authorKey(),
                current.authorDisplayName(),
                current.createdAt(),
                now
        );

        items.set(idx, updated);
        saveStore(issueKey, new FeedbackStore(1, now, items));
        return toDto(updated);
    }

    public void delete(String id) {
        var issueKey = requireFeedbackIssueKey();
        JiraModels.UserResponse me = jira.getMe();
        String now = Instant.now().toString();

        FeedbackStore store = loadStore(issueKey);
        List<FeedbackEntry> items = new ArrayList<>(store.items() != null ? store.items() : List.of());

        int idx = indexOf(items, id);
        if (idx < 0) throw notFound("Feedback not found: " + id);

        FeedbackEntry current = items.get(idx);
        requireCanEdit(me, current);

        items.remove(idx);
        saveStore(issueKey, new FeedbackStore(1, now, items));
    }

    // ───────────────────────────── Storage ─────────────────────────────

    private FeedbackStore loadStore(String issueKey) {
        try {
            JsonNode v = jira.getIssueProperty(issueKey, PROP_KEY);

            if (v == null || v.isMissingNode() || v.isNull() || (v.isObject() && v.size() == 0)) {
                return new FeedbackStore(1, null, List.of());
            }

            FeedbackStore parsed = json.treeToValue(v, FeedbackStore.class);
            if (parsed == null) return new FeedbackStore(1, null, List.of());

            List<FeedbackEntry> sane = (parsed.items() == null ? List.<FeedbackEntry>of() : parsed.items())
                    .stream()
                    .filter(Objects::nonNull)
                    .map(this::sanitizeEntry)
                    .filter(Objects::nonNull)
                    .toList();

            return new FeedbackStore(
                    parsed.version() != null ? parsed.version() : 1,
                    nzOrNull(parsed.updatedAt()),
                    sane
            );
        } catch (Exception e) {
            return new FeedbackStore(1, null, List.of());
        }
    }

    private void saveStore(String issueKey, FeedbackStore store) {
        jira.setIssueProperty(issueKey, PROP_KEY, store);
    }

    private FeedbackEntry sanitizeEntry(FeedbackEntry e) {
        if (e == null) return null;

        String id = nz(e.id());
        if (!SAFE_ID.matcher(id).matches()) return null;

        String type = nz(e.type()).toUpperCase(Locale.ROOT);
        if (!ALLOWED_TYPE.contains(type)) return null;

        String status = normalizeStatusForRead(e.status());

        return new FeedbackEntry(
                id,
                type,
                status,
                nz(e.summary()),
                nz(e.description()),
                nz(e.authorKey()),
                nz(e.authorDisplayName()),
                nz(e.createdAt()),
                nz(e.updatedAt())
        );
    }

    // ───────────────────────────── Permissions ─────────────────────────────

    private void requireCanEdit(JiraModels.UserResponse me, FeedbackEntry entry) {
        if (entry == null) throw notFound("Feedback not found.");
        String myKey = safeUserKey(me);

        boolean isAuthor = isNotBlank(myKey) && myKey.equals(nz(entry.authorKey()));
        boolean isAdmin = isProjectAdmin();

        if (!isAuthor && !isAdmin) {
            throw new ForbiddenException("Brak uprawnień: tylko autor zgłoszenia lub admin może modyfikować wpis.");
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

    private String getFeedbackIssueKeyOrNull() {
        var cfg = jiraConfigService.getForRuntime();
        // Minimal-change: używamy tego samego storage issue co challenges.
        // Jeśli wolisz osobny klucz, dodaj feedbackIssueKey do JiraConfig i użyj go tutaj.
        return cfg != null ? cfg.feedbackIssueKey() : null;
    }

    private String requireFeedbackIssueKey() {
        String k = getFeedbackIssueKeyOrNull();
        if (!isNotBlank(k)) {
            throw new ResponseStatusException(
                    HttpStatus.PRECONDITION_FAILED,
                    "Feedback storage is not configured (missing challengesIssueKey in admin config)."
            );
        }
        return k.trim();
    }

    // ───────────────────────────── Helpers ─────────────────────────────

    private static int indexOf(List<FeedbackEntry> items, String id) {
        String want = nz(id);
        for (int i = 0; i < items.size(); i++) {
            if (want.equals(nz(items.get(i).id()))) return i;
        }
        return -1;
    }

    private static FeedbackEntry findOrThrow(FeedbackStore store, String id) {
        List<FeedbackEntry> items = store.items() != null ? store.items() : List.of();
        String want = nz(id);
        for (FeedbackEntry it : items) {
            if (want.equals(nz(it.id()))) return it;
        }
        throw notFound("Feedback not found: " + want);
    }

    private static ResponseStatusException notFound(String msg) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, msg);
    }

    private FeedbackDtos.Feedback toDto(FeedbackEntry e) {
        return new FeedbackDtos.Feedback(
                e.id(),
                blankToNull(e.type()),
                blankToNull(e.status()),
                blankToNull(e.summary()),
                blankToNull(e.description()),
                blankToNull(e.authorKey()),
                blankToNull(e.authorDisplayName()),
                blankToNull(e.createdAt()),
                blankToNull(e.updatedAt())
        );
    }

    private static String generateId() {
        byte[] b = new byte[9];
        new Random().nextBytes(b);
        return "fb_" + Base64.getUrlEncoder().withoutPadding().encodeToString(b);
    }

    private static String safeUserKey(JiraModels.UserResponse user) {
        if (user == null) return "unknown";
        if (isNotBlank(user.key())) return user.key();
        if (isNotBlank(user.name())) return user.name();
        return isNotBlank(user.displayName()) ? user.displayName() : "unknown";
    }

    private static String normalizeTypeForWrite(String raw) {
        String t = nz(raw).toUpperCase(Locale.ROOT);
        if (!ALLOWED_TYPE.contains(t)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid feedback type: " + raw + " (allowed: BUG, IDEA)");
        }
        return t;
    }

    private static String normalizeTypeForFilter(String raw) {
        if (!isNotBlank(raw)) return null;
        String t = nz(raw).toUpperCase(Locale.ROOT);
        if (!ALLOWED_TYPE.contains(t)) return null; // filtr niepasujący -> jakby "brak wyników" (ale czytelniej: ignore)
        return t;
    }

    private static String normalizeStatusForFilter(String raw) {
        if (!isNotBlank(raw)) return null;
        String s = normalizeStatusForWrite(raw, null);
        if (s == null) return null;
        return ALLOWED_STATUS.contains(s) ? s : null;
    }

    private static String normalizeStatusForWrite(String raw, String fallbackIfBlank) {
        if (raw == null) return fallbackIfBlank;
        String s = nz(raw).toUpperCase(Locale.ROOT);
        if (s.isBlank()) return fallbackIfBlank;

        // tolerancja na różne zapisy
        s = s.replace('-', '_').replace(' ', '_');
        if ("INPROGRESS".equals(s) || "IN_PROGRESS".equals(s)) s = "IN_PROGRESS";

        if (!ALLOWED_STATUS.contains(s)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid status: " + raw + " (allowed: TODO, IN_PROGRESS, DONE, REJECTED)");
        }
        return s;
    }

    private static String normalizeStatusForRead(String raw) {
        try {
            String s = normalizeStatusForWrite(raw, "TODO");
            return ALLOWED_STATUS.contains(s) ? s : "TODO";
        } catch (Exception e) {
            return "TODO";
        }
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

    public record FeedbackStore(
            Integer version,
            String updatedAt,
            List<FeedbackEntry> items
    ) {}

    public record FeedbackEntry(
            String id,
            String type,    // BUG|IDEA
            String status,  // TODO|IN_PROGRESS|DONE|REJECTED
            String summary,
            String description,
            String authorKey,
            String authorDisplayName,
            String createdAt,
            String updatedAt
    ) {}
}
