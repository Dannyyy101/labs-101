package com.labs_101.backend;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.labs_101.backend.repositories.LoginDayRepository;
import com.labs_101.backend.repositories.UserRepository;

@SpringBootTest(properties = "mcp.resource-url=https://backend.example.com/api/mcp")
public class McpTest {

    @Autowired
    private WebApplicationContext context;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private LoginDayRepository loginDays;
    // the real one would load the keys from zitadel
    @MockitoBean
    private JwtDecoder jwtDecoder;

    private MockMvc mockMvc;
    private String userId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
        userId = "mcp-test-" + System.nanoTime();
    }

    @AfterEach
    void tearDown() {
        userRepository.deleteById(userId);
    }

    @Test
    void testTellsClaudeWhereToSignIn() throws Exception {
        mockMvc.perform(post("/api/mcp").contentType(MediaType.APPLICATION_JSON).content(request(1, "initialize", "{}")))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", containsString(
                        "resource_metadata=\"https://backend.example.com/.well-known/oauth-protected-resource/api/mcp\"")));

        mockMvc.perform(get("/.well-known/oauth-protected-resource/api/mcp"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resource").value("https://backend.example.com/api/mcp"))
                .andExpect(jsonPath("$.authorization_servers[0]").value("http://localhost:8081"))
                .andExpect(jsonPath("$.scopes_supported", hasItem("offline_access")))
                .andExpect(jsonPath("$.tls_client_certificate_bound_access_tokens").value(false));

    }

    @Test
    void testInitialize() throws Exception {
        call(request(1, "initialize", """
                {"protocolVersion": "2025-06-18", "capabilities": {}, "clientInfo": {"name": "test", "version": "1"}}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jsonrpc").value("2.0"))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.result.protocolVersion").value("2025-06-18"))
                .andExpect(jsonPath("$.result.capabilities.tools").exists())
                .andExpect(jsonPath("$.result.serverInfo.name").value("labs-101"));

        // a version we don't know gets our newest one
        call(request("a", "initialize", "{\"protocolVersion\": \"1999-01-01\"}"))
                .andExpect(jsonPath("$.id").value("a"))
                .andExpect(jsonPath("$.result.protocolVersion").value("2025-11-25"));

        call("{\"jsonrpc\": \"2.0\", \"method\": \"notifications/initialized\"}")
                .andExpect(status().isAccepted());
        call(request(2, "ping", "{}"))
                .andExpect(jsonPath("$.result").isMap());
        call(request(3, "resources/list", "{}"))
                .andExpect(jsonPath("$.error.code").value(-32601));
    }

    @Test
    void testListAndCallTools() throws Exception {
        call(request(1, "tools/list", "{}"))
                .andExpect(jsonPath("$.result.tools[*].name", hasItem("list_workouts")))
                .andExpect(jsonPath("$.result.tools[*].name", hasItem("get_nutrition")))
                .andExpect(jsonPath("$.result.tools[0].inputSchema.type").value("object"))
                .andExpect(jsonPath("$.result.tools[0].annotations.readOnlyHint").value(true));

        call(request(2, "tools/call", "{\"name\": \"list_workouts\", \"arguments\": {\"from\": \"2026-01-01\"}}"))
                .andExpect(jsonPath("$.result.isError").value(false))
                .andExpect(jsonPath("$.result.content[0].type").value("text"))
                .andExpect(jsonPath("$.result.content[0].text").value("[]"));

        call(request(3, "tools/call", "{\"name\": \"get_nutrition\", \"arguments\": {\"from\": \"2026-10-01\", \"to\": \"2026-10-02\"}}"))
                .andExpect(jsonPath("$.result.isError").value(false))
                .andExpect(jsonPath("$.result.content[0].text").value(containsString("\"date\":\"2026-10-02\"")));

        // mistakes in the arguments are shown to Claude so it can correct them
        call(request(4, "tools/call", "{\"name\": \"list_runs\", \"arguments\": {\"from\": \"gestern\"}}"))
                .andExpect(jsonPath("$.result.isError").value(true))
                .andExpect(jsonPath("$.result.content[0].text").value(containsString("from")));
        call(request(5, "tools/call", "{\"name\": \"get_nutrition\", \"arguments\": {\"from\": \"2026-01-01\", \"to\": \"2026-06-01\"}}"))
                .andExpect(jsonPath("$.result.isError").value(true));

        call(request(6, "tools/call", "{\"name\": \"delete_everything\"}"))
                .andExpect(jsonPath("$.error.code").value(-32602));
    }

    // Claude reading the data isn't a visit of the user
    @Test
    void testDoesNotCountAsLogin() throws Exception {
        call(request(1, "tools/list", "{}")).andExpect(status().isOk());

        assertTrue(loginDays.days(userId).isEmpty());
    }

    private ResultActions call(String body) throws Exception {
        return mockMvc.perform(post("/api/mcp")
                .with(jwt().jwt(token -> token.subject(userId)))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON, MediaType.TEXT_EVENT_STREAM)
                .content(body));
    }

    private static String request(Object id, String method, String params) {
        String jsonId = id instanceof String text ? "\"" + text + "\"" : id.toString();
        return "{\"jsonrpc\": \"2.0\", \"id\": " + jsonId + ", \"method\": \"" + method + "\", \"params\": " + params + "}";
    }
}
