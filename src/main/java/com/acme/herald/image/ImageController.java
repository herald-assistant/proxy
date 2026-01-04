package com.acme.herald.image;

import com.acme.herald.domain.AttachmentDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping(path = "/images", produces = MediaType.APPLICATION_JSON_VALUE)
@Validated
@RequiredArgsConstructor
@Tag(name = "ImageController", description = "Attachment upload and binary streaming endpoints exposed by the Provider proxy.")
public class ImageController {

    private final ImageService service;

    @PostMapping(path = "/{issueKey}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Upload an image attachment to an issue",
            description = "Uploads a file as an attachment to the issue identified by issueKey and returns attachment metadata and URLs."
    )
    public ResponseEntity<AttachmentDto> upload(
            @Parameter(description = "Provider issue key that will receive the attachment.", example = "ABC-123")
            @PathVariable @NotBlank String issueKey,
            @RequestPart("file") MultipartFile file
    ) {
        var dto = service.upload(issueKey, file);
        return ResponseEntity.ok(dto);
    }

    @GetMapping(path = "/{attachmentId}/content", produces = MediaType.ALL_VALUE)
    @Operation(
            summary = "Stream attachment content",
            description = "Streams the binary content of an attachment. Use download=true to force download via Content-Disposition."
    )
    public ResponseEntity<byte[]> streamContent(
            @Parameter(description = "Attachment identifier in the Provider.", example = "10001")
            @PathVariable @NotBlank String attachmentId,

            @Parameter(description = "If true, sets Content-Disposition to attachment; otherwise inline.", example = "false")
            @RequestParam(name = "download", defaultValue = "false") boolean download
    ) {
        return service.streamContent(attachmentId, download);
    }

    @GetMapping(path = "/{attachmentId}/thumbnail", produces = MediaType.ALL_VALUE)
    @Operation(
            summary = "Stream attachment thumbnail",
            description = "Streams a thumbnail representation when available. If the Provider does not expose thumbnails, this may fall back to the main content."
    )
    public ResponseEntity<byte[]> streamThumbnail(
            @Parameter(description = "Attachment identifier in the Provider.", example = "10001")
            @PathVariable @NotBlank String attachmentId
    ) {
        return service.streamThumbnail(attachmentId);
    }
}
