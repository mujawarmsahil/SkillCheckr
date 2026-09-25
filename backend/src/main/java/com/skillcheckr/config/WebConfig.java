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

@Configuration
public class WebConfig {

    @Value("${cors.allowed-origins:https://skill-checkr.vercel.app,http://localhost:5173,http://localhost:3000,http://localhost:8080,http://127.0.0.1:5173}")
    private String allowedOrigins;

    @Value("${frontend.url:${FRONTEND_URL:https://skill-checkr.vercel.app}}")
    private String frontendUrl;

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

        if (frontendUrl != null && !frontendUrl.trim().isEmpty()) {
            String clean = frontendUrl.trim();
            if (clean.endsWith("/")) {
                clean = clean.substring(0, clean.length() - 1);
            }
            origins.add(clean);
        }

        // Always guarantee production frontend and local development support
        origins.add("https://skill-checkr.vercel.app");
        origins.add("http://localhost:*");
        origins.add("http://127.0.0.1:*");

        return new ArrayList<>(origins);
    }

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();

        config.setAllowCredentials(true);

        List<String> patterns = getAllowedOriginPatterns();
        for (String pattern : patterns) {
            config.addAllowedOriginPattern(pattern);
        }

        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "HEAD"));
        config.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "Accept", "X-Requested-With", "Origin", "*"));
        config.setExposedHeaders(Arrays.asList("Authorization", "Content-Disposition", "*"));
        config.setMaxAge(3600L);

        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }

}
