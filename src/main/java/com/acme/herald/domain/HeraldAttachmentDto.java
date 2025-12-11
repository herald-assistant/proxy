package com.acme.herald.domain;

// package com.acme.herald.domain;
public record HeraldAttachmentDto(
        String id,
        String filename,
        long size,
        String mimeType,
        String url,          // URL do <img src=...> (zwykle proxowany endpoint)
        String downloadUrl,  // URL do pobrania (możesz użyć ten sam co url)
        String thumbnailUrl  // jeśli Jira generuje miniatury
) {}
