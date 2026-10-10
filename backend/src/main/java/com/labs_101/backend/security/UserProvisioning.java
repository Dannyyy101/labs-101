package com.labs_101.backend.security;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.labs_101.backend.entities.User;
import com.labs_101.backend.repositories.UserRepository;

/**
 * The users live in Zitadel, the backend keeps a copy in the {@code user} table
 * because all the app data references it. The access token only carries the
 * id, so name and email of a new user come from the userinfo endpoint.
 */
@Service
public class UserProvisioning {

    private static final Logger log = LoggerFactory.getLogger(UserProvisioning.class);

    private final UserRepository userRepository;
    private final RestClient restClient;
    private final String userInfoUri;

    // ids known to exist, so only the first request of a user hits the database
    private final Set<String> knownUsers = ConcurrentHashMap.newKeySet();

    UserProvisioning(UserRepository userRepository, @Value("${auth.userinfo-uri:}") String userInfoUri) {
        this.userRepository = userRepository;
        this.restClient = RestClient.create();
        this.userInfoUri = userInfoUri;
    }

    public void ensureUser(Jwt token) {
        String id = token.getSubject();
        if (id == null || knownUsers.contains(id))
            return;

        if (!userRepository.existsById(id)) {
            userRepository.save(newUser(token));
            log.info("Created user {}", id);
        }
        knownUsers.add(id);
    }

    private User newUser(Jwt token) {
        Map<String, Object> info = userInfo(token);
        String email = string(info, "email", token.getClaimAsString("email"));
        String name = string(info, "name", string(info, "preferred_username", email != null ? email : token.getSubject()));

        Instant now = Instant.now();
        User user = new User(token.getSubject());
        user.setName(name);
        user.setEmail(email != null ? email : "");
        user.setEmailVerified(Boolean.TRUE.equals(info.get("email_verified")));
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        return user;
    }

    // a failing userinfo request must not lock the user out, they get created with the id only
    private Map<String, Object> userInfo(Jwt token) {
        if (userInfoUri.isBlank())
            return Map.of();
        try {
            Map<String, Object> info = restClient.get()
                    .uri(userInfoUri)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token.getTokenValue())
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {
                    });
            return info != null ? info : Map.of();
        } catch (RestClientException e) {
            log.warn("Could not load the userinfo of {}", token.getSubject(), e);
            return Map.of();
        }
    }

    private static String string(Map<String, Object> info, String key, String fallback) {
        return info.get(key) instanceof String value && !value.isBlank() ? value : fallback;
    }
}
