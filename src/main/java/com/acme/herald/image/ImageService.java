package com.acme.herald.image;

import com.acme.herald.config.AdminJiraConfigService;
import com.acme.herald.domain.HeraldAttachmentDto;
import com.acme.herald.provider.JiraProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ImageService {

    private static final Set<String> ALLOWED_MIME = Set.of(
            "image/png", "image/jpeg", "image/webp", "image/gif", "image/svg+xml"
    );

    private final JiraProvider jira;
    private final AdminJiraConfigService jiraAdminCfg;

    public HeraldAttachmentDto upload(String issueKey, MultipartFile file) {
        // (opcjonalnie) walidacja mime
        if (file.getContentType() != null && !ALLOWED_MIME.contains(file.getContentType())) {
            throw new IllegalArgumentException("Nieobsługiwany typ pliku: " + file.getContentType());
        }

        var a = jira.attachAndReturnMeta(issueKey, file);

        var cfg = jiraAdminCfg.getForRuntime();
        boolean useProxy = Boolean.TRUE.equals(cfg.options().proxyAttachmentContent());

        String base = "/images/" + a.id();

        String url = useProxy
                ? base + "/content"
                : (a.content() != null && !a.content().isBlank() ? a.content() : base + "/content");

        String thumb = useProxy
                ? base + "/thumbnail"
                : (a.thumbnail() != null && !a.thumbnail().isBlank() ? a.thumbnail() : null);

        return new HeraldAttachmentDto(
                a.id(),
                a.filename(),
                a.size(),
                a.mimeType(),
                url,
                url,
                thumb
        );
    }

    public ResponseEntity<byte[]> streamContent(String attachmentId, boolean download) {
        var stream = jira.downloadAttachment(attachmentId);
        var meta = jira.getAttachment(attachmentId);

        MediaType ct = parseMedia(meta.mimeType());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(ct != null ? ct : MediaType.APPLICATION_OCTET_STREAM);
        headers.setCacheControl(CacheControl.noCache().getHeaderValue());

        if (download) {
            String filename = meta.filename() != null ? meta.filename() : ("attachment-" + meta.id());
            headers.setContentDisposition(ContentDisposition.attachment()
                    .filename(filename, StandardCharsets.UTF_8).build());
        } else {
            headers.setContentDisposition(ContentDisposition.inline()
                    .filename(meta.filename(), StandardCharsets.UTF_8).build());
        }

        return new ResponseEntity<>(stream, headers, HttpStatus.OK);
    }

    public ResponseEntity<byte[]> streamThumbnail(String attachmentId) {
        var thumb = jira.downloadAttachmentThumbnail(attachmentId);
        var meta = jira.getAttachment(attachmentId);

        MediaType ct = parseMedia(meta.mimeType());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(ct != null ? ct : MediaType.APPLICATION_OCTET_STREAM);
        headers.setCacheControl(CacheControl.noCache().getHeaderValue());
        headers.setContentDisposition(ContentDisposition.inline()
                .filename("thumb-" + meta.filename(), StandardCharsets.UTF_8).build());
        return new ResponseEntity<>(thumb, headers, HttpStatus.OK);
    }

    private static MediaType parseMedia(String s) {
        try {
            return (s != null) ? MediaType.parseMediaType(s) : null;
        } catch (Exception e) {
            return null;
        }
    }
}
