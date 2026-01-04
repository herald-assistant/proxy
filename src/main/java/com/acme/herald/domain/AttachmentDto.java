package com.acme.herald.domain;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Attachment metadata returned after uploading a file to the Provider.")
public record AttachmentDto(

        @Schema(description = "Attachment identifier in the Provider.", example = "10001")
        String id,

        @Schema(description = "Original file name.", example = "diagram.png")
        String filename,

        @Schema(description = "File size in bytes.", example = "245901")
        long size,

        @Schema(description = "Detected MIME type of the attachment.", example = "image/png")
        String mimeType,

        @Schema(description = "URL that can be used directly as an <img src> (may be proxied by this API).", example = "/images/10001/content")
        String url,

        @Schema(description = "URL for downloading the attachment (may be the same as url).", example = "/images/10001/content?download=true")
        String downloadUrl,

        @Schema(description = "Optional thumbnail URL if the Provider exposes one or if proxying is enabled.", example = "/images/10001/thumbnail")
        String thumbnailUrl
) {}
