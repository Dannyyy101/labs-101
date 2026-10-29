package com.labs_101.backend;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import tools.jackson.databind.json.JsonMapper;

/**
 * Writes the OpenAPI spec of the controllers to {@code api/openapi.json}, the
 * frontend and the iOS app generate their clients from it, so a changed DTO breaks
 * their build instead of failing at runtime.
 * <p>
 * Locally the file is just updated (commit it with the change), in CI
 * ({@code CI=true}) the test fails when the committed file is out of date.
 */
@SpringBootTest
public class OpenApiSpecTest {
    private static final Path SPEC = Path.of("..", "api", "openapi.json");

    @Autowired
    private WebApplicationContext context;
    // the real one would load the keys from zitadel
    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void testSpecIsUpToDate() throws Exception {
        String json = MockMvcBuilders.webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build()
                .perform(get("/v3/api-docs").with(jwt()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonMapper mapper = JsonMapper.builder().build();
        String spec = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(mapper.readTree(json)) + "\n";

        String committed = Files.exists(SPEC) ? Files.readString(SPEC) : "";
        if (spec.equals(committed)) {
            return;
        }
        if ("true".equals(System.getenv("CI"))) {
            throw new AssertionError("api/openapi.json is out of date, run the backend tests locally and commit it");
        }
        Files.writeString(SPEC, spec);
    }
}
