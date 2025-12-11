package com.acme.herald.auth;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Service;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class CryptoService {
    private final byte[] secret;
    private static final String CIPHER = "AES/GCM/NoPadding";

    public CryptoService(HeraldAuthProps props) {
        this.secret = Base64.getDecoder().decode(props.getSecretB64());
        if (secret.length < 32) throw new IllegalStateException("Use 32-byte secret for AES-256-GCM");
    }

    public String encrypt(byte[] plain) {
        try {
            byte[] iv = new byte[12];
            new SecureRandom().nextBytes(iv);
            Cipher c = Cipher.getInstance(CIPHER);
            c.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(secret, "AES"), new GCMParameterSpec(128, iv));
            byte[] enc = c.doFinal(plain);
            byte[] out = new byte[iv.length + enc.length];
            System.arraycopy(iv, 0, out, 0, iv.length);
            System.arraycopy(enc, 0, out, iv.length, enc.length);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(out);
        } catch (Exception e) { throw new IllegalStateException(e); }
    }

    public byte[] decrypt(String token) {
        try {
            byte[] all = Base64.getUrlDecoder().decode(token);
            byte[] iv = new byte[12]; System.arraycopy(all, 0, iv, 0, 12);
            byte[] enc = new byte[all.length - 12]; System.arraycopy(all, 12, enc, 0, enc.length);
            Cipher c = Cipher.getInstance(CIPHER);
            c.init(Cipher.DECRYPT_MODE, new SecretKeySpec(secret, "AES"), new GCMParameterSpec(128, iv));
            return c.doFinal(enc);
        } catch (Exception e) { throw new IllegalStateException(e); }
    }
}
