package com.convo.file_sharing.service;

import com.convo.file_sharing.config.InternalServiceProperties;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

// Runs against a real local HTTP server rather than mocking RestClient, so
// the actual path, header, JSON body and error handling are what's tested.
class UserLookupClientTest {

    private static final String KEY = "test-internal-service-key";

    private HttpServer server;
    private final AtomicInteger status = new AtomicInteger(200);
    private final AtomicReference<String> responseBody = new AtomicReference<>("[]");
    private final AtomicReference<String> seenPath = new AtomicReference<>();
    private final AtomicReference<String> seenKey = new AtomicReference<>();
    private final AtomicReference<String> seenBody = new AtomicReference<>();
    private final AtomicInteger calls = new AtomicInteger();

    private UserLookupClient client;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            calls.incrementAndGet();
            seenPath.set(exchange.getRequestURI().getPath());
            seenKey.set(exchange.getRequestHeaders().getFirst("X-Internal-Service-Key"));
            seenBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] bytes = responseBody.get().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status.get(), bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });
        server.start();

        InternalServiceProperties props = new InternalServiceProperties();
        props.setServiceKey(KEY);
        props.setBackendBaseUrl("http://127.0.0.1:" + server.getAddress().getPort());
        client = new UserLookupClient(props);
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    void sendsServiceKeyAndIdsToInternalBatchEndpoint() {
        UUID alice = UUID.randomUUID();
        responseBody.set("[{\"id\":\"" + alice + "\",\"displayName\":\"Alice\"}]");

        Map<UUID, String> names = client.getDisplayNames(List.of(alice));

        assertEquals(Map.of(alice, "Alice"), names);
        assertEquals("/api/backend/internal/users/batch", seenPath.get());
        assertEquals(KEY, seenKey.get());
        assertTrue(seenBody.get().contains(alice.toString()), seenBody.get());
    }

    @Test
    void userWithNullDisplayName_IsSkipped_OthersStillResolve() {
        UUID alice = UUID.randomUUID();
        UUID nameless = UUID.randomUUID();
        responseBody.set("[{\"id\":\"" + alice + "\",\"displayName\":\"Alice\"},"
                + "{\"id\":\"" + nameless + "\",\"displayName\":null}]");

        assertEquals(Map.of(alice, "Alice"), client.getDisplayNames(List.of(alice, nameless)));
    }

    @Test
    void duplicateIds_SentOnce() {
        UUID alice = UUID.randomUUID();
        responseBody.set("[]");

        client.getDisplayNames(List.of(alice, alice, alice));

        assertEquals(1, seenBody.get().split(alice.toString(), -1).length - 1, seenBody.get());
    }

    @Test
    void emptyInput_MakesNoCall() {
        assertEquals(Map.of(), client.getDisplayNames(List.of()));
        assertEquals(0, calls.get());
    }

    @Test
    void backendRejectsKey_FailsClosedToEmptyMap() {
        status.set(403);
        responseBody.set("{\"error\":\"Invalid or missing service credentials\"}");

        assertEquals(Map.of(), client.getDisplayNames(List.of(UUID.randomUUID())));
    }

    @Test
    void backendServerError_FailsClosedToEmptyMap() {
        status.set(500);
        responseBody.set("{}");

        assertEquals(Map.of(), client.getDisplayNames(List.of(UUID.randomUUID())));
    }

    @Test
    void backendUnreachable_FailsClosedToEmptyMap() {
        server.stop(0);

        assertEquals(Map.of(), client.getDisplayNames(List.of(UUID.randomUUID())));
    }
}
