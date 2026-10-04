package com.labs_101.backend.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Every request has to send the shared API key in the {@value #HEADER} header,
 * until the backend gets a real user authentication.
 * Without a configured key all requests are rejected, so a forgotten
 * {@code API_KEY} never leaves the backend open.
 */
@Component
// runs after the CORS filter, so rejected requests still carry the CORS headers
@Order(Ordered.LOWEST_PRECEDENCE)
public class ApiKeyFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-API-Key";

    private static final Logger log = LoggerFactory.getLogger(ApiKeyFilter.class);

    private final byte[] apiKey;

    public ApiKeyFilter(@Value("${api.key:}") String apiKey) {
        this.apiKey = apiKey.getBytes(StandardCharsets.UTF_8);
        if (apiKey.isBlank())
            log.warn("No API key configured (API_KEY), all requests will be rejected");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        // CORS preflight requests never carry custom headers
        if (HttpMethod.OPTIONS.matches(request.getMethod()) || isValid(request.getHeader(HEADER))) {
            chain.doFilter(request, response);
            return;
        }

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("""
                {"status":401,"error":"Unauthorized","errorMessage":"Missing or invalid API key","path":"%s"}"""
                .formatted(request.getRequestURI().replace("\"", "")));
    }

    private boolean isValid(String key) {
        if (apiKey.length == 0 || key == null)
            return false;
        // constant time, so the key can't be guessed by timing the responses
        return MessageDigest.isEqual(apiKey, key.getBytes(StandardCharsets.UTF_8));
    }
}
