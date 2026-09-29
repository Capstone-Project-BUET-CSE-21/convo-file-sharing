package com.convo.file_sharing.service;

import com.convo.file_sharing.config.InternalServiceProperties;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

// Calls convo-backend's internal, server-to-server user-batch API
// (InternalUserController) to resolve display names for the sender/
// recipient/downloader ids this service only ever holds as plain UUIDs —
// same pattern as convo-audio-watermark's MeetingParticipantClient.
//
// Eclipse's null analysis flags the RestClient body()/Map.of() calls below
// as needing "unchecked conversion" — same mechanical noise as
// TransferMetadataService's class-level suppression, not a real null risk;
// see that class's comment for the full reasoning.
@SuppressWarnings("null")
@Service
public class UserLookupClient {

    private record PublicUser(UUID id, String displayName) {}

    // Without an explicit timeout, a hung/unreachable convo-backend hangs
    // every request that needs a display name indefinitely instead of
    // failing fast (see convo-audio-watermark's MeetingParticipantClient,
    // where this was fixed after being flagged as a gap).
    private static final int CONNECT_TIMEOUT_MS = 3_000;
    private static final int READ_TIMEOUT_MS = 5_000;

    private final RestClient restClient;
    private final InternalServiceProperties properties;

    public UserLookupClient(InternalServiceProperties properties) {
        this.properties = properties;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(CONNECT_TIMEOUT_MS);
        requestFactory.setReadTimeout(READ_TIMEOUT_MS);
        this.restClient = RestClient.builder()
                .baseUrl(properties.getBackendBaseUrl())
                .requestFactory(requestFactory)
                .build();
    }

    /**
     * Resolves many ids in one round trip. Fails closed on any error
     * (network failure, non-2xx, etc.) by returning an empty map rather
     * than propagating — a missing display name should degrade to a
     * fallback label client-side, never break chain/download lookups that
     * have nothing to do with convo-backend's availability.
     */
    public Map<UUID, String> getDisplayNames(List<UUID> ids) {
        List<UUID> uniqueIds = ids.stream().distinct().toList();
        if (uniqueIds.isEmpty()) {
            return Collections.emptyMap();
        }

        try {
            PublicUser[] users = restClient.post()
                    .uri("/api/backend/internal/users/batch")
                    .header("X-Internal-Service-Key", properties.getServiceKey())
                    .body(Map.of("ids", uniqueIds))
                    .retrieve()
                    .body(PublicUser[].class);

            if (users == null) {
                return Collections.emptyMap();
            }
            return List.of(users).stream()
                    .collect(Collectors.toMap(PublicUser::id, PublicUser::displayName));
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }
}
