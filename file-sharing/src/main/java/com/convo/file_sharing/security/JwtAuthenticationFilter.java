package com.convo.file_sharing.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.DecodingException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

/**
 * Validates the HS-signed JWTs issued by convo-backend's JwtService (same
 * app.jwt.secret / JWT_SECRET). This service never issues tokens itself —
 * it only trusts the "uid" claim convo-backend embeds, which is the same
 * UUID exposed as a user's id everywhere else in the app. Controllers read
 * it back via CurrentUser instead of trusting a client-supplied
 * userId/senderId in the request body.
 *
 * @EnableConfigurationProperties is declared here (not relying solely on
 * FileSharingApplication's @ConfigurationPropertiesScan) because
 * @WebMvcTest slices — e.g. KeyControllerTest — still construct WebConfig's
 * SecurityFilterChain bean, which depends on this filter, but they don't
 * run component/properties scanning; without this, JwtProperties has no
 * bean definition in that slice and the filter fails to construct.
 */
@Component
@EnableConfigurationProperties(JwtProperties.class)
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final SecretKey signingKey;

    public JwtAuthenticationFilter(JwtProperties jwtProperties) {
        this.signingKey = buildSigningKey(jwtProperties.getSecret());
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                     @NonNull HttpServletResponse response,
                                     @NonNull FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                Claims claims = Jwts.parser()
                        .verifyWith(signingKey)
                        .build()
                        .parseSignedClaims(token)
                        .getPayload();

                UUID userId = UUID.fromString(claims.get("uid", String.class));
                var authentication = new UsernamePasswordAuthenticationToken(userId, null, List.of());
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (JwtException | IllegalArgumentException expiredOrMalformed) {
                // Leave the SecurityContext empty — WebConfig's
                // .anyRequest().authenticated() turns that into a 401.
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }

    private SecretKey buildSigningKey(String secret) {
        try {
            byte[] decoded = Decoders.BASE64.decode(secret);
            if (decoded.length >= 32) {
                return Keys.hmacShaKeyFor(decoded);
            }
        } catch (DecodingException | IllegalArgumentException notBase64) {
            // fall back to raw bytes below — matches convo-backend's JwtService
        }
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}
