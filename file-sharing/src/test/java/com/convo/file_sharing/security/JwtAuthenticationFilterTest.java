package com.convo.file_sharing.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Full filter chain (WebConfig + JwtAuthenticationFilter), not a slice with
// filters disabled — the point is to check what an actual request gets.
// Tokens are minted exactly the way convo-backend's JwtService does: HS
// key from the shared secret, "uid" claim carrying the user's id.
@SpringBootTest
@AutoConfigureMockMvc
class JwtAuthenticationFilterTest {

    // Must match app.jwt.secret in src/test/resources/application.properties.
    private static final SecretKey KEY =
            Keys.hmacShaKeyFor("test-only-secret-key-at-least-32-bytes-long".getBytes(StandardCharsets.UTF_8));
    private static final SecretKey WRONG_KEY =
            Keys.hmacShaKeyFor("a-completely-different-secret-32-bytes!!".getBytes(StandardCharsets.UTF_8));

    // Any authenticated endpoint works; this one is read-only and, for an
    // unknown hash, touches nothing beyond an empty H2 query.
    private static final String PROTECTED = "/api/file-sharing/downloads/no-such-hash";

    @Autowired
    private MockMvc mvc;

    private static String token(SecretKey key, String uid, Date expiry) {
        var builder = Jwts.builder().subject("someone@example.com").issuedAt(new Date()).expiration(expiry);
        if (uid != null) {
            builder.claim("uid", uid);
        }
        return builder.signWith(key).compact();
    }

    private static Date inOneHour() {
        return new Date(System.currentTimeMillis() + 3_600_000);
    }

    @Test
    void validToken_IsAccepted() throws Exception {
        mvc.perform(get(PROTECTED).header("Authorization", "Bearer " + token(KEY, UUID.randomUUID().toString(), inOneHour())))
                .andExpect(status().isOk());
    }

    @Test
    void noToken_Is401() throws Exception {
        mvc.perform(get(PROTECTED)).andExpect(status().isUnauthorized());
    }

    @Test
    void garbageToken_Is401() throws Exception {
        mvc.perform(get(PROTECTED).header("Authorization", "Bearer not.a.jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenSignedWithWrongSecret_Is401() throws Exception {
        mvc.perform(get(PROTECTED).header("Authorization", "Bearer " + token(WRONG_KEY, UUID.randomUUID().toString(), inOneHour())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void expiredToken_Is401() throws Exception {
        Date past = new Date(System.currentTimeMillis() - 60_000);
        mvc.perform(get(PROTECTED).header("Authorization", "Bearer " + token(KEY, UUID.randomUUID().toString(), past)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void validSignatureButMissingUidClaim_Is401NotServerError() throws Exception {
        mvc.perform(get(PROTECTED).header("Authorization", "Bearer " + token(KEY, null, inOneHour())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void validSignatureButNonUuidUid_Is401() throws Exception {
        mvc.perform(get(PROTECTED).header("Authorization", "Bearer " + token(KEY, "not-a-uuid", inOneHour())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void healthEndpoint_IsPublic() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }
}
