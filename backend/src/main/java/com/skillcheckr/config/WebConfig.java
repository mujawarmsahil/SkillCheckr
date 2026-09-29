package com.skillcheckr.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.skillcheckr.security.AuthInterceptor;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;

    public WebConfig(AuthInterceptor authInterceptor) {
        this.authInterceptor = authInterceptor;
    }

    @Value("${cors.allowed-origins:https://skill-checkr.vercel.app}")
    private String allowedOrigins;

    @Value("${cors.allow-credentials:true}")
    private boolean allowCredentials;

    /**
     * Builds the origin patterns the CORS filter will accept.
     *
     * <p>The list comes from configuration only. Wildcard and loopback origins are no longer
     * appended unconditionally: a previous version added {@code http://localhost:*} and
     * {@code http://127.0.0.1:*} here, which meant every deployment, including production,
     * accepted a CORS request from any loopback port on any machine while credentials were
     * enabled. Local development origins now come from the {@code dev} profile.
     */
    private List<String> getAllowedOriginPatterns() {
        Set<String> origins = new LinkedHashSet<>();

        if (allowedOrigins != null && !allowedOrigins.trim().isEmpty()) {
            Arrays.stream(allowedOrigins.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .forEach(origin -> {
                        String clean = origin.endsWith("/") ? origin.substring(0, origin.length() - 1) : origin;
                        origins.add(clean);
                    });
        }

        return new ArrayList<>(origins);
    }

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();

        config.setAllowCredentials(allowCredentials);

        List<String> patterns = getAllowedOriginPatterns();
        for (String pattern : patterns) {
            config.addAllowedOriginPattern(pattern);
        }

        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "HEAD"));
        // The wildcard entries that used to terminate both lists are gone. Allowed headers are
        // enumerated because credentials are enabled, and a "*" entry is silently widened by
        // the browser to every header; the explicit list is what the frontend actually sends.
        config.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "Accept",
                "X-Requested-With", "Origin"));
        config.setExposedHeaders(Arrays.asList("Content-Disposition"));
        config.setMaxAge(3600L);

        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor).addPathPatterns("/api/**");
    }

}
