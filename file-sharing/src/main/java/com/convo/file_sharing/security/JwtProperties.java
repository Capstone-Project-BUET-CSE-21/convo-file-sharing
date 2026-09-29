package com.convo.file_sharing.security;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Mirrors convo-backend's JwtProperties (same app.jwt.secret / JWT_SECRET
 * env var) — this service only ever verifies tokens, so it has no need for
 * an expiration-ms setting; jjwt already rejects expired tokens on parse.
 *
 * No fallback default: a default secret would be published in the repo and
 * let anyone mint a token for any user. Fails startup instead.
 */
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {

    private String secret;

    @PostConstruct
    void validate() {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "app.jwt.secret (JWT_SECRET) must be set — it verifies the login tokens "
                            + "convo-backend issues, and must match convo-backend's own JWT_SECRET.");
        }
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }
}
