package com.labs_101.backend.security;

import java.io.IOException;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Creates the user of the token on their first request. Not a bean on purpose,
 * Spring Boot would otherwise register it a second time outside the security
 * filter chain.
 */
class UserProvisioningFilter extends OncePerRequestFilter {

    private final UserProvisioning userProvisioning;

    UserProvisioningFilter(UserProvisioning userProvisioning) {
        this.userProvisioning = userProvisioning;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication() instanceof JwtAuthenticationToken token)
            userProvisioning.ensureUser(token.getToken());

        chain.doFilter(request, response);
    }
}
