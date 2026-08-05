package com.convo.file_sharing.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Mirrors convo-backend's JwtProperties (same app.jwt.secret / JWT_SECRET
 * env var) — this service only ever verifies tokens, so it has no need for
 * an expiration-ms setting; jjwt already rejects expired tokens on parse.
 */
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {

    private String secret;

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }
}
