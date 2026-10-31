package com.skillcheckr.config;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.beans.factory.annotation.Value;

import com.skillcheckr.security.AuthInterceptor;

/**
 * Registers the {@code CorsFilter} applied to every route and maps the authentication
 * interceptor onto the API. Spring Security ({@link com.skillcheckr.security.SecurityConfig})
 * also installs a stateless filter chain that enforces authentication and security headers;
 * this interceptor remains the API-level access gate. Origins come from the {@code cors.*}
 * properties via {@link CorsProperties}; there is no hardcoded fallback origin.
 */
@Configuration
@EnableConfigurationProperties({CorsProperties.class, AiChatProperties.class})
public class WebConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;
    private final CorsProperties corsProperties;

    public WebConfig(AuthInterceptor authInterceptor, CorsProperties corsProperties) {
        this.authInterceptor = authInterceptor;
        this.corsProperties = corsProperties;
    }

    /**
     * Origins come from configuration only; no wildcard or loopback origins are appended in
     * code. Development origins come from the {@code dev} profile.
     *
     * <p>A trailing slash is stripped so {@code https://example.com/} matches the browser
     * {@code Origin} header {@code https://example.com}; without the strip the entry would
     * never match and CORS would fail silently for that environment.
     */
    private List<String> getAllowedOriginPatterns() {
        Set<String> origins = new LinkedHashSet<>();

        for (String origin : corsProperties.getAllowedOrigins()) {
            if (origin == null) {
                continue;
            }
            String clean = origin.trim();
            if (clean.isEmpty()) {
                continue;
            }
            if (clean.endsWith("/")) {
                clean = clean.substring(0, clean.length() - 1);
            }
            origins.add(clean);
        }

        return new ArrayList<>(origins);
    }

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();

        config.setAllowCredentials(corsProperties.isAllowCredentials());

        List<String> patterns = getAllowedOriginPatterns();
        for (String pattern : patterns) {
            config.addAllowedOriginPattern(pattern);
        }

        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "HEAD"));
        // Headers are enumerated because credentials may be enabled; a "*" entry is widened
        // by the browser to every header. This list is what the frontend actually sends.
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept",
                "X-Requested-With", "Origin"));
        config.setExposedHeaders(List.of("Content-Disposition"));
        config.setMaxAge(3600L);

        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor).addPathPatterns("/api/**");
    }

}
