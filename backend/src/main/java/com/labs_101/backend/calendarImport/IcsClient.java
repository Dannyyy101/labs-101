package com.labs_101.backend.calendarImport;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Downloads iCalendar feeds. The urls come from the users, so only https and the hosts of
 * {@code calendar.import.allowed-hosts} (and their subdomains) are requested, redirects are checked the same way.
 * Otherwise the server could be made to request addresses of its own network.
 */
@Component
public class IcsClient {

    // a calendar with years of events has a few MB
    static final int MAX_BYTES = 10 * 1024 * 1024;
    private static final int MAX_REDIRECTS = 3;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();
    private final List<String> allowedHosts;

    IcsClient(@Value("${calendar.import.allowed-hosts:icloud.com}") String allowedHosts) {
        this.allowedHosts = Arrays.stream(allowedHosts.split(",")).map(String::strip)
                .map((host) -> host.toLowerCase(Locale.ROOT)).filter((host) -> !host.isEmpty()).toList();
    }

    /** The https url of a webcal or https link, null when it's no url of an allowed host. */
    public URI normalize(String url) {
        if (url == null) {
            return null;
        }
        String trimmed = url.strip();
        if (trimmed.regionMatches(true, 0, "webcal://", 0, 9)) {
            trimmed = "https://" + trimmed.substring(9);
        }
        try {
            URI uri = new URI(trimmed);
            return isAllowed(uri) ? uri : null;
        } catch (URISyntaxException e) {
            return null;
        }
    }

    public boolean isAllowed(URI uri) {
        if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null || uri.getUserInfo() != null) {
            return false;
        }
        String host = uri.getHost().toLowerCase(Locale.ROOT);
        return allowedHosts.stream().anyMatch((allowed) -> host.equals(allowed) || host.endsWith("." + allowed));
    }

    /** The feed as text, throws when it can't be downloaded. */
    public String download(URI uri) {
        URI current = uri;
        try {
            for (int redirects = 0; redirects <= MAX_REDIRECTS; redirects++) {
                if (!isAllowed(current)) {
                    throw new IllegalStateException("Host not allowed: " + current.getHost());
                }
                HttpRequest request = HttpRequest.newBuilder(current).timeout(Duration.ofSeconds(20))
                        .header("Accept", "text/calendar").GET().build();
                HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
                try (InputStream body = response.body()) {
                    int status = response.statusCode();
                    if (status >= 300 && status < 400) {
                        String location = response.headers().firstValue("Location")
                                .orElseThrow(() -> new IllegalStateException("Redirect without location"));
                        current = current.resolve(location);
                        continue;
                    }
                    if (status != 200) {
                        throw new IllegalStateException("HTTP " + status);
                    }
                    byte[] bytes = body.readNBytes(MAX_BYTES + 1);
                    if (bytes.length > MAX_BYTES) {
                        throw new IllegalStateException("Calendar larger than " + MAX_BYTES + " bytes");
                    }
                    return new String(bytes, StandardCharsets.UTF_8);
                }
            }
            throw new IllegalStateException("Too many redirects");
        } catch (IOException e) {
            throw new IllegalStateException("Calendar not reachable: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted", e);
        }
    }
}
