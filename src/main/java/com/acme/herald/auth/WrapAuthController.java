package com.acme.herald.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.Instant;

@RestController
@RequestMapping(path = "/proxy/auth", produces = MediaType.APPLICATION_JSON_VALUE)
public class WrapAuthController {
    private final HeraldAuthProps props;
    private final CryptoService crypto;
    private final ObjectMapper om = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    public WrapAuthController(HeraldAuthProps props, CryptoService crypto) {
        this.props = props;
        this.crypto = crypto;
    }

    public record WrapReq(String token, Integer ttlDays) {
    }

    public record WrapRes(String token, String cookieName, Instant expiresAt) {
    }

    @PostMapping("/wrap")
    public WrapRes wrap(@RequestBody WrapReq req,
                        @RequestParam(defaultValue = "false") boolean setCookie,
                        HttpServletResponse res) {
        int days = (req.ttlDays() == null || req.ttlDays() <= 0) ? props.getMaxAgeDays() : req.ttlDays();
        var payload = new TokenPayload(
                req.token(),
                Instant.now().plus(Duration.ofDays(days))
        );
        try {
            byte[] json = om.writeValueAsBytes(payload);
            String enc = crypto.encrypt(json);

            if (setCookie) {
                var c = ResponseCookie.from(props.getCookieName(), enc)
                        .httpOnly(true).secure(true).sameSite("Lax")
                        .path("/").maxAge(Duration.ofDays(days)).build();
                res.addHeader("Set-Cookie", c.toString());
            }
            return new WrapRes(enc, props.getCookieName(), payload.exp());
        } catch (Exception e) {
            throw new IllegalStateException("wrap failed", e);
        }
    }
}
