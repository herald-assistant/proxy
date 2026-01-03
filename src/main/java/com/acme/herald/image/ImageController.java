package com.acme.herald.image;

// package com.acme.herald.web;

import com.acme.herald.domain.HeraldAttachmentDto;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping(path = "/images", produces = MediaType.APPLICATION_JSON_VALUE)
@Validated
@RequiredArgsConstructor
public class ImageController {
    private final ImageService service;

    /** Upload pliku jako załącznik do Issue (Case) i zwrócenie metadanych/URL do użycia w <img src>. */
    @PostMapping(path = "/{issueKey}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<HeraldAttachmentDto> upload(
            @PathVariable @NotBlank String issueKey,
            @RequestPart("file") MultipartFile file
    ) {
        var dto = service.upload(issueKey, file);
        return ResponseEntity.ok(dto);
    }

    /** Proxy do strumieniowania zawartości załącznika (dla <img src="/api/herald/images/{id}/content">). */
    @GetMapping(path = "/{attachmentId}/content", produces = MediaType.ALL_VALUE)
    public ResponseEntity<byte[]> streamContent(
            @PathVariable @NotBlank String attachmentId,
            @RequestParam(name = "download", defaultValue = "false") boolean download
    ) {
        return service.streamContent(attachmentId, download);
    }

    /** Opcjonalnie: proxowany podgląd miniatury, jeśli Jira ją udostępnia. */
    @GetMapping(path = "/{attachmentId}/thumbnail", produces = MediaType.ALL_VALUE)
    public ResponseEntity<byte[]> streamThumbnail(@PathVariable String attachmentId) {
        return service.streamThumbnail(attachmentId);
    }
}
