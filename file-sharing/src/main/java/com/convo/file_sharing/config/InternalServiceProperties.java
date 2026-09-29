package com.convo.file_sharing.config;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.lang.NonNull;

import java.util.Objects;

// Server-to-server call to convo-backend's internal user-lookup API (see
// UserLookupClient) — backs embedding real display names directly in this
// service's chain-history/downloads responses. Required with no fallback
// default, but an EMPTY default (the trailing ':') on its
// application.properties placeholder on purpose: with no default at all,
// Spring binds an unset variable as the literal text "${VAR}", which would
// pass the blank check below. app.jwt.secret follows the same rule.
@ConfigurationProperties(prefix = "app.internal")
public class InternalServiceProperties {

    private String serviceKey;
    private String backendBaseUrl;

    @PostConstruct
    void validate() {
        if (serviceKey == null || serviceKey.isBlank()) {
            throw new IllegalStateException(
                    "app.internal.service-key (INTERNAL_SERVICE_KEY) must be set — "
                            + "it authenticates this service's calls to convo-backend's internal API.");
        }
        if (backendBaseUrl == null || backendBaseUrl.isBlank()) {
            throw new IllegalStateException(
                    "app.internal.backend-base-url (CONVO_BACKEND_URL) must be set — "
                            + "the base URL of convo-backend's internal API.");
        }
    }

    @NonNull
    public String getServiceKey() {
        return Objects.requireNonNull(serviceKey, "serviceKey read before validate() ran");
    }

    public void setServiceKey(String serviceKey) {
        this.serviceKey = serviceKey;
    }

    @NonNull
    public String getBackendBaseUrl() {
        return Objects.requireNonNull(backendBaseUrl, "backendBaseUrl read before validate() ran");
    }

    public void setBackendBaseUrl(String backendBaseUrl) {
        this.backendBaseUrl = backendBaseUrl;
    }
}
