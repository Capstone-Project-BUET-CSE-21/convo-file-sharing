package com.convo.file_sharing.config;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.lang.NonNull;

import java.util.Objects;

// Config for calling convo-backend's internal, server-to-server API
// (InternalMeetingController) — see SessionParticipantService. serviceKey
// must match convo-backend's own app.internal.service-key exactly; no
// fallback default, same reasoning as convo-backend's own
// InternalServiceProperties (fail loudly at startup rather than silently
// sending an empty/guessable header).
@ConfigurationProperties(prefix = "app.internal")
public class InternalServiceProperties {

    private String serviceKey;
    private String backendBaseUrl;

    @PostConstruct
    void validate() {
        if (serviceKey == null || serviceKey.isBlank()) {
            throw new IllegalStateException(
                    "app.internal.service-key (INTERNAL_SERVICE_KEY) must be set — "
                            + "it authenticates this service's calls to convo-backend's internal API, "
                            + "and must match convo-backend's own app.internal.service-key.");
        }
        if (backendBaseUrl == null || backendBaseUrl.isBlank()) {
            throw new IllegalStateException(
                    "app.internal.backend-base-url (CONVO_BACKEND_URL) must be set — "
                            + "it's where this service sends its internal API calls to convo-backend.");
        }
    }

    // @NonNull + requireNonNull here document and enforce a guarantee
    // validate() above establishes, not the field's type itself — the
    // field can be null/blank up until @PostConstruct runs, but Spring
    // completes that before this bean is ever handed to another bean's
    // constructor. requireNonNull is a fail-fast backstop, not dead code:
    // if that lifecycle guarantee is ever broken by a future refactor
    // (e.g. a scope change), this throws a clear NPE here instead of
    // letting a silent null reach RestClient.
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
