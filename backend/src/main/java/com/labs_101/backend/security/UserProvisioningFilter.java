package com.labs_101.backend.security;

import java.io.IOException;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

import com.labs_101.backend.mcp.McpController;
import com.labs_101.backend.repositories.LoginDayRepository;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Creates the user of the token on their first request and marks the day for
 * the login streak. Not a bean on purpose,
 * Spring Boot would otherwise register it a second time outside the security
 * filter chain.
 */
class UserProvisioningFilter extends OncePerRequestFilter {

    // the iOS app syncs Apple Health in the background, that's no visit of the user
    private static final String HEALTH_SYNC = "/api/users/me/health/";
    // neither is Claude reading the data through the MCP server
    private static final String MCP = McpController.PATH;

    private final UserProvisioning userProvisioning;
    private final LoginDayRepository loginDays;

    UserProvisioningFilter(UserProvisioning userProvisioning, LoginDayRepository loginDays) {
        this.userProvisioning = userProvisioning;
        this.loginDays = loginDays;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication() instanceof JwtAuthenticationToken token) {
            userProvisioning.ensureUser(token.getToken());
            if (token.getToken().getSubject() != null && !request.getRequestURI().startsWith(HEALTH_SYNC)
                    && !request.getRequestURI().startsWith(MCP))
                loginDays.markToday(token.getToken().getSubject());
        }

        chain.doFilter(request, response);
    }
}
