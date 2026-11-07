package com.labs_101.backend.security;

import java.net.URI;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.security.autoconfigure.actuate.web.servlet.EndpointRequest;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.labs_101.backend.repositories.LoginDayRepository;

/**
 * Every request needs an access token of Zitadel in the
 * {@code Authorization: Bearer} header, the backend only checks it against the
 * keys of the issuer ({@code AUTH_ISSUER}) and never sees a password.
 * <p>
 * The protected resource metadata (RFC 9728) at
 * {@code /.well-known/oauth-protected-resource} and the {@code resource_metadata}
 * in {@code WWW-Authenticate} tell MCP clients like Claude to sign in at Zitadel,
 * see {@link com.labs_101.backend.mcp.McpController}.
 */
@Configuration
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    /**
     * Health probes and metrics without a token: they only exist on the management port
     * ({@code management.server.port}), which no public route points to.
     */
    @Bean
    @Order(1)
    SecurityFilterChain actuatorFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher(EndpointRequest.toAnyEndpoint())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(requests -> requests.anyRequest().permitAll());
        return http.build();
    }

    @Bean
    @Order(2)
    SecurityFilterChain securityFilterChain(HttpSecurity http, UserProvisioning userProvisioning,
            LoginDayRepository loginDays, @Value("${mcp.resource-url}") String mcpResource,
            @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuer) throws Exception {
        // the public URL, behind the reverse proxy the request only knows http://backend:8080
        URI resource = URI.create(mcpResource);
        String metadataUrl = resource.getScheme() + "://" + resource.getRawAuthority()
                + "/.well-known/oauth-protected-resource" + resource.getRawPath();
        BearerTokenAuthenticationEntryPoint entryPoint = new BearerTokenAuthenticationEntryPoint();
        entryPoint.setResourceMetadataParameterResolver(request -> metadataUrl);
        // clients only get a bare 401, so the reason is logged here: a token that was sent but rejected (expired,
        // unknown key, wrong issuer, ...) or a request of the apps without any token. Requests without a token
        // outside /api/users (e.g. the first one of Claude at /api/mcp) are expected and not logged
        AuthenticationEntryPoint loggingEntryPoint = (request, response, exception) -> {
            if (exception instanceof OAuth2AuthenticationException rejected) {
                log.warn("Rejected token for {} {}: {}", request.getMethod(), request.getRequestURI(),
                        rejected.getError().getDescription());
            } else if (request.getRequestURI().startsWith("/api/users/")) {
                log.warn("No token for {} {} (Authorization header {}, User-Agent {})", request.getMethod(),
                        request.getRequestURI(), request.getHeader("Authorization") == null ? "missing" : "present",
                        request.getHeader("User-Agent"));
            }
            entryPoint.commence(request, response, exception);
        };

        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(server -> server
                        .jwt(Customizer.withDefaults())
                        .authenticationEntryPoint(loggingEntryPoint)
                        .protectedResourceMetadata(metadata -> metadata.protectedResourceMetadataCustomizer(builder -> builder
                                .resource(mcpResource)
                                .resourceName("labs-101")
                                .authorizationServer(issuer)
                                // Spring announces certificate bound tokens by default, Zitadel's tokens aren't
                                .tlsClientCertificateBoundAccessTokens(false)
                                // offline_access, so Claude gets a refresh token and the user stays connected
                                .scopes(scopes -> scopes.addAll(List.of("openid", "profile", "email", "offline_access"))))))
                // after the token is checked, so the user exists before any controller needs it
                .addFilterAfter(new UserProvisioningFilter(userProvisioning, loginDays), BearerTokenAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cors = new CorsConfiguration();
        cors.setAllowedOrigins(List.of("http://localhost:3000", "http://frontend:3000",
                "https://labs-101.project101.tech", "https://project101.tech"));
        cors.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        cors.setAllowedHeaders(List.of("*"));
        cors.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", cors);
        return source;
    }
}
