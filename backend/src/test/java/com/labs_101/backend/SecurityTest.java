package com.labs_101.backend;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.labs_101.backend.repositories.UserRepository;

@SpringBootTest
public class SecurityTest {

    @Autowired
    private WebApplicationContext context;
    @Autowired
    private UserRepository userRepository;
    // the real one would load the keys from zitadel
    @MockitoBean
    private JwtDecoder jwtDecoder;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    @Test
    void testRejectsRequestWithoutToken() throws Exception {
        mockMvc.perform(get("/api/foods"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/users/me/settings"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testRejectsInvalidToken() throws Exception {
        when(jwtDecoder.decode("not-a-jwt")).thenThrow(new BadJwtException("Malformed token"));

        mockMvc.perform(get("/api/users/me/settings").header("Authorization", "Bearer not-a-jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testPassesCorsPreflightWithoutToken() throws Exception {
        mockMvc.perform(options("/api/foods")
                .header("Origin", "http://localhost:3000")
                .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk());
    }

    @Test
    void testCreatesUserOfTokenOnFirstRequest() throws Exception {
        String id = "security-test-" + System.nanoTime();

        // no settings yet, but the request got through to the controller
        mockMvc.perform(get("/api/users/me/settings").with(jwt().jwt(token -> token.subject(id))))
                .andExpect(status().isNotFound());

        assertTrue(userRepository.existsById(id));
        userRepository.deleteById(id);
    }
}
