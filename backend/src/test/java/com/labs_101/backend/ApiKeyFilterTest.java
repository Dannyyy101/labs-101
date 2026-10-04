package com.labs_101.backend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.labs_101.backend.config.ApiKeyFilter;

public class ApiKeyFilterTest {

    private static MockFilterChain filter(String configuredKey, MockHttpServletRequest request,
            MockHttpServletResponse response) throws Exception {
        MockFilterChain chain = new MockFilterChain();
        new ApiKeyFilter(configuredKey).doFilter(request, response, chain);
        return chain;
    }

    private static MockHttpServletRequest request(String method, String key) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, "/api/foods");
        if (key != null)
            request.addHeader(ApiKeyFilter.HEADER, key);
        return request;
    }

    @Test
    void testPassesWithValidKey() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = filter("secret", request("GET", "secret"), response);

        assertNotNull(chain.getRequest());
        assertEquals(200, response.getStatus());
    }

    @Test
    void testRejectsMissingOrWrongKey() throws Exception {
        for (String key : new String[] { null, "", "wrong", "secret " }) {
            MockHttpServletResponse response = new MockHttpServletResponse();
            MockFilterChain chain = filter("secret", request("GET", key), response);

            assertNull(chain.getRequest(), "key " + key);
            assertEquals(401, response.getStatus(), "key " + key);
        }
    }

    @Test
    void testRejectsEverythingWithoutConfiguredKey() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = filter("", request("GET", ""), response);

        assertNull(chain.getRequest());
        assertEquals(401, response.getStatus());
    }

    @Test
    void testPassesCorsPreflightWithoutKey() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = filter("secret", request("OPTIONS", null), response);

        assertNotNull(chain.getRequest());
    }
}
