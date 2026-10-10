package com.labs_101.backend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;

import com.labs_101.backend.entities.User;
import com.labs_101.backend.repositories.UserRepository;
import com.labs_101.backend.security.UserProvisioning;
import com.sun.net.httpserver.HttpServer;

@ExtendWith(MockitoExtension.class)
public class UserProvisioningTest {

    @Mock
    private UserRepository userRepository;

    private HttpServer userInfoServer;

    @AfterEach
    void tearDown() {
        if (userInfoServer != null)
            userInfoServer.stop(0);
    }

    private UserProvisioning provisioning(String userInfoUri) throws Exception {
        // the constructor is package private like in all beans
        Constructor<UserProvisioning> constructor = UserProvisioning.class
                .getDeclaredConstructor(UserRepository.class, String.class);
        constructor.setAccessible(true);
        return constructor.newInstance(userRepository, userInfoUri);
    }

    private static Jwt token(String subject) {
        return Jwt.withTokenValue("token-of-" + subject).header("alg", "none").subject(subject).build();
    }

    // answers like the userinfo endpoint of zitadel and remembers the Authorization header
    private String startUserInfo(String json, AtomicReference<String> authorization) throws IOException {
        userInfoServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        userInfoServer.createContext("/oidc/v1/userinfo", exchange -> {
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            byte[] body = json.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        userInfoServer.start();
        return "http://127.0.0.1:" + userInfoServer.getAddress().getPort() + "/oidc/v1/userinfo";
    }

    @Test
    void testCreatesNewUserFromUserInfo() throws Exception {
        AtomicReference<String> authorization = new AtomicReference<>();
        String uri = startUserInfo("""
                {"sub":"new-user","name":"Max Muster","email":"max@example.com","email_verified":true}""",
                authorization);
        when(userRepository.existsById("new-user")).thenReturn(false);

        provisioning(uri).ensureUser(token("new-user"));

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertEquals("new-user", saved.getValue().getId());
        assertEquals("Max Muster", saved.getValue().getName());
        assertEquals("max@example.com", saved.getValue().getEmail());
        assertEquals(true, saved.getValue().getEmailVerified());
        assertEquals("Bearer token-of-new-user", authorization.get());
    }

    @Test
    void testCreatesUserWithIdOnlyWhenUserInfoFails() throws Exception {
        when(userRepository.existsById("new-user")).thenReturn(false);

        // nothing listens there
        provisioning("http://127.0.0.1:1/oidc/v1/userinfo").ensureUser(token("new-user"));

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertEquals("new-user", saved.getValue().getId());
        assertEquals("new-user", saved.getValue().getName());
        assertEquals("", saved.getValue().getEmail());
    }

    @Test
    void testKeepsExistingUser() throws Exception {
        when(userRepository.existsById("known-user")).thenReturn(true);

        provisioning("").ensureUser(token("known-user"));

        verify(userRepository, never()).save(any());
    }

    @Test
    void testChecksDatabaseOnlyOncePerUser() throws Exception {
        when(userRepository.existsById("known-user")).thenReturn(true);
        UserProvisioning provisioning = provisioning("");

        provisioning.ensureUser(token("known-user"));
        provisioning.ensureUser(token("known-user"));

        verify(userRepository, times(1)).existsById("known-user");
    }
}
