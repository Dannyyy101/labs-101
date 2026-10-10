package com.labs_101.backend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.labs_101.backend.security.ZitadelClient;
import com.sun.net.httpserver.HttpServer;

public class ZitadelClientTest {

    private HttpServer server;
    private final AtomicReference<String> authorization = new AtomicReference<>();

    @AfterEach
    void tearDown() {
        if (server != null)
            server.stop(0);
    }

    private static ZitadelClient client(String userInfoUri, String issuer, String organizationId) throws Exception {
        // the constructor is package private like in all beans
        Constructor<ZitadelClient> constructor = ZitadelClient.class
                .getDeclaredConstructor(String.class, String.class, String.class);
        constructor.setAccessible(true);
        return constructor.newInstance(userInfoUri, issuer, organizationId);
    }

    // answers like zitadel and remembers the request
    private String start(String path, String json) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext(path, exchange -> {
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            byte[] response = json.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        return "http://127.0.0.1:" + server.getAddress().getPort() + path;
    }

    @Test
    void testUserInfoWithTokenOfTheUser() throws Exception {
        String uri = start("/oidc/v1/userinfo", """
                {"sub":"me","name":"Max Muster","picture":"https://auth.example.com/avatar"}""");

        Map<String, Object> info = client(uri, "", "").userInfo("user-token");

        assertEquals("https://auth.example.com/avatar", info.get("picture"));
        assertEquals("Bearer user-token", authorization.get());
    }

    @Test
    void testUserInfoIsEmptyWhenZitadelFails() throws Exception {
        // nothing listens there
        assertTrue(client("http://127.0.0.1:1/oidc/v1/userinfo", "", "").userInfo("user-token").isEmpty());
    }

    @Test
    void testAvatarUrlFromUserId() throws Exception {
        ZitadelClient client = client("", "https://auth.example.com/", "org-1");

        assertEquals("https://auth.example.com/assets/v1/org-1/users/user-1/avatar", client.avatarUrl("user-1"));
    }

    @Test
    void testNoAvatarUrlWithoutOrganization() throws Exception {
        assertNull(client("", "https://auth.example.com", "").avatarUrl("user-1"));
    }
}
