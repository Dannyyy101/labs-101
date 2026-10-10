package com.labs_101.backend.security;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Profiles live in Zitadel, the backend keeps no copy: the own profile comes
 * from the userinfo endpoint with the token of the user, profile pictures from
 * a fixed URL per user (like GitHub avatars), so nothing has to be looked up.
 */
@Service
public class ZitadelClient {

    private static final Logger log = LoggerFactory.getLogger(ZitadelClient.class);

    private final RestClient restClient = RestClient.create();
    private final String userInfoUri;
    private final String issuer;
    private final String organizationId;

    ZitadelClient(@Value("${auth.userinfo-uri:}") String userInfoUri,
            @Value("${auth.issuer:}") String issuer,
            @Value("${auth.organization-id:}") String organizationId) {
        this.userInfoUri = userInfoUri;
        this.issuer = issuer.replaceAll("/+$", "");
        this.organizationId = organizationId;
    }

    /** The claims of the userinfo endpoint (name, email, picture, ...), empty when the request fails. */
    public Map<String, Object> userInfo(String accessToken) {
        if (userInfoUri.isBlank())
            return Map.of();
        try {
            Map<String, Object> info = restClient.get()
                    .uri(userInfoUri)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {
                    });
            return info != null ? info : Map.of();
        } catch (RestClientException e) {
            log.warn("Could not load the userinfo", e);
            return Map.of();
        }
    }

    /**
     * Where zitadel serves the profile picture of the user, a new picture shows up under the same URL.
     * Users without one get a 404 there, the apps show the initial then. Null without an organization id.
     */
    public String avatarUrl(String userId) {
        if (issuer.isBlank() || organizationId.isBlank() || userId == null)
            return null;
        return issuer + "/assets/v1/" + organizationId + "/users/" + userId + "/avatar";
    }
}
