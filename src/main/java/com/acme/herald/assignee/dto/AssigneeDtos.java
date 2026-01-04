

package com.acme.herald.assignee.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;

import java.util.Map;

public class AssigneeDtos {

    @Schema(description = "User that can be assigned to an issue in the configured Provider.")
    public record AssignableUser(

            @Schema(description = "Provider username/login. May be null or empty depending on Provider and privacy settings.",
                    example = "jdoe")
            String name,

            @Schema(description = "Provider user key. May be null in some deployments.",
                    example = "USER12345")
            String key,

            @Schema(description = "Human-friendly display name.",
                    example = "John Doe")
            String displayName,

            @Schema(description = "User email address if exposed by the Provider. Often null/empty due to privacy settings.",
                    example = "john.doe@acme.com")
            String emailAddress,

            @Schema(description = "Avatar URLs keyed by size (Provider-specific), e.g. \"16x16\", \"24x24\", \"32x32\", \"48x48\".",
                    example = "{\"16x16\":\"https://provider.acme.com/secure/useravatar?size=xsmall&avatarId=12345\",\"48x48\":\"https://provider.acme.com/secure/useravatar?size=large&avatarId=12345\"}")
            Map<String, String> avatarUrls
    ) {
    }


    @Schema(description = "Request payload for assigning a user to an issue. Provide either 'name' or 'key' (depending on Provider version).")
    public record AssigneeReq(

            @Schema(description = "Provider username/login . Use when 'key' is not available.",
                    example = "jdoe")
            String name,

            @Schema(description = "Provider user key/identifier. Use when supported/available.",
                    example = "USER12345")
            String key
    ) {
        @AssertTrue(message = "Either 'name' or 'key' must be provided.")
        public boolean isValid() {
            return (name != null && !name.isBlank()) || (key != null && !key.isBlank());
        }
    }
}
