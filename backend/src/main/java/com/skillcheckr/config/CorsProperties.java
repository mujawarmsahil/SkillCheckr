package com.skillcheckr.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * CORS settings bound once from the {@code cors.*} properties. {@link WebConfig} builds the
 * {@code CorsFilter} from this bean and {@link DeploymentConfigurationValidator} enforces the
 * startup rules against the same instance, so the validated values are the served values.
 * Registered by {@link WebConfig} via {@code @EnableConfigurationProperties}; the no-wildcard
 * invariant stays in the startup validator, which produces the operator-facing error message.
 */
@ConfigurationProperties(prefix = "cors")
public class CorsProperties {

    /**
     * Exact origins, one per entry. An empty list denies every cross-origin request; there is
     * no wildcard default.
     */
    private List<String> allowedOrigins = new ArrayList<>();

    /**
     * Whether the browser may attach credentials to allowed cross-origin requests. The API
     * authenticates with a bearer header, but the flag changes the browser contract, so it
     * stays configurable and defaults to the deployed behavior.
     */
    private boolean allowCredentials = true;

    public List<String> getAllowedOrigins() {
        return allowedOrigins;
    }

    public void setAllowedOrigins(List<String> allowedOrigins) {
        this.allowedOrigins = allowedOrigins == null ? new ArrayList<>() : allowedOrigins;
    }

    public boolean isAllowCredentials() {
        return allowCredentials;
    }

    public void setAllowCredentials(boolean allowCredentials) {
        this.allowCredentials = allowCredentials;
    }
}
