package com.labs_101.backend.calendarImport;

import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * Reads the calendars of an iCloud account over CalDAV, signed in with the Apple ID and an app-specific
 * password (appleid.apple.com → Sign-In and Security → App-Specific Passwords). Only hosts allowed by
 * {@link IcsClient} are requested, the password must not go anywhere else.
 */
@Component
public class CalDavClient {

    static final URI ICLOUD = URI.create("https://caldav.icloud.com/");
    private static final String DAV = "DAV:";
    private static final String CALDAV = "urn:ietf:params:xml:ns:caldav";
    private static final int MAX_REDIRECTS = 3;
    private static final DateTimeFormatter UTC = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'")
            .withZone(ZoneOffset.UTC);

    /** Apple ID or password are wrong, or the password was revoked. */
    public static class UnauthorizedException extends RuntimeException {
        UnauthorizedException() {
            super("Apple ID or app-specific password wrong");
        }
    }

    public record DiscoveredCalendar(String name, String url) {
    }

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();
    private final IcsClient icsClient;

    CalDavClient(IcsClient icsClient) {
        this.icsClient = icsClient;
    }

    /** The calendars with events of the account, reminder lists are left out. */
    public List<DiscoveredCalendar> discover(String appleId, String password) {
        Response principal = send("PROPFIND", ICLOUD, "0", """
                <d:propfind xmlns:d="DAV:"><d:prop><d:current-user-principal/></d:prop></d:propfind>
                """, appleId, password);
        URI principalUrl = principal.uri().resolve(href(principal.body(), DAV, "current-user-principal"));

        Response home = send("PROPFIND", principalUrl, "0", """
                <d:propfind xmlns:d="DAV:" xmlns:c="urn:ietf:params:xml:ns:caldav">
                  <d:prop><c:calendar-home-set/></d:prop>
                </d:propfind>
                """, appleId, password);
        URI homeUrl = home.uri().resolve(href(home.body(), CALDAV, "calendar-home-set"));

        Response calendars = send("PROPFIND", homeUrl, "1", """
                <d:propfind xmlns:d="DAV:" xmlns:c="urn:ietf:params:xml:ns:caldav">
                  <d:prop><d:displayname/><d:resourcetype/><c:supported-calendar-component-set/></d:prop>
                </d:propfind>
                """, appleId, password);
        return calendars(calendars.body(), calendars.uri());
    }

    /** The iCalendar texts of the events in the window, a repeating event with all its occurrences. */
    public List<String> events(URI calendarUrl, String appleId, String password, Instant start, Instant end) {
        Response response = send("REPORT", calendarUrl, "1", """
                <c:calendar-query xmlns:d="DAV:" xmlns:c="urn:ietf:params:xml:ns:caldav">
                  <d:prop><c:calendar-data/></d:prop>
                  <c:filter>
                    <c:comp-filter name="VCALENDAR">
                      <c:comp-filter name="VEVENT"><c:time-range start="%s" end="%s"/></c:comp-filter>
                    </c:comp-filter>
                  </c:filter>
                </c:calendar-query>
                """.formatted(UTC.format(start), UTC.format(end)), appleId, password);
        return calendarData(response.body());
    }

    private record Response(URI uri, String body) {
    }

    private Response send(String method, URI uri, String depth, String body, String appleId, String password) {
        String credentials = Base64.getEncoder()
                .encodeToString((appleId + ":" + password).getBytes(StandardCharsets.UTF_8));
        URI current = uri;
        try {
            for (int redirects = 0; redirects <= MAX_REDIRECTS; redirects++) {
                if (!icsClient.isAllowed(current)) {
                    throw new IllegalStateException("Host not allowed: " + current.getHost());
                }
                HttpRequest request = HttpRequest.newBuilder(current).timeout(Duration.ofSeconds(20))
                        .header("Authorization", "Basic " + credentials)
                        .header("Depth", depth)
                        .header("Content-Type", "application/xml; charset=utf-8")
                        .method(method, HttpRequest.BodyPublishers.ofString(body)).build();
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                int status = response.statusCode();
                if (status == 401 || status == 403) {
                    throw new UnauthorizedException();
                }
                if (status >= 300 && status < 400) {
                    current = current.resolve(response.headers().firstValue("Location")
                            .orElseThrow(() -> new IllegalStateException("Redirect without location")));
                    continue;
                }
                if (status != 207 && status != 200) {
                    throw new IllegalStateException(method + " " + current.getPath() + ": HTTP " + status);
                }
                return new Response(current, response.body());
            }
            throw new IllegalStateException("Too many redirects");
        } catch (IOException e) {
            throw new IllegalStateException("iCloud not reachable: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted", e);
        }
    }

    /** The first href in the property, e.g. the principal of current-user-principal. */
    static String href(String xml, String namespace, String property) {
        NodeList properties = parse(xml).getElementsByTagNameNS(namespace, property);
        for (int i = 0; i < properties.getLength(); i++) {
            NodeList hrefs = ((Element) properties.item(i)).getElementsByTagNameNS(DAV, "href");
            if (hrefs.getLength() > 0) {
                return hrefs.item(0).getTextContent().strip();
            }
        }
        throw new IllegalStateException("No " + property + " in the answer");
    }

    static List<DiscoveredCalendar> calendars(String xml, URI base) {
        List<DiscoveredCalendar> calendars = new ArrayList<>();
        NodeList responses = parse(xml).getElementsByTagNameNS(DAV, "response");
        for (int i = 0; i < responses.getLength(); i++) {
            Element response = (Element) responses.item(i);
            NodeList types = response.getElementsByTagNameNS(DAV, "resourcetype");
            boolean calendar = types.getLength() > 0
                    && ((Element) types.item(0)).getElementsByTagNameNS(CALDAV, "calendar").getLength() > 0;
            if (!calendar || !hasEvents(response)) {
                continue;
            }
            String href = response.getElementsByTagNameNS(DAV, "href").item(0).getTextContent().strip();
            NodeList names = response.getElementsByTagNameNS(DAV, "displayname");
            String name = names.getLength() == 0 ? "" : names.item(0).getTextContent().strip();
            calendars.add(new DiscoveredCalendar(name.isEmpty() ? "Kalender" : name, base.resolve(href).toString()));
        }
        return calendars;
    }

    /** Without the property a calendar may have all components. */
    private static boolean hasEvents(Element response) {
        NodeList sets = response.getElementsByTagNameNS(CALDAV, "supported-calendar-component-set");
        if (sets.getLength() == 0) {
            return true;
        }
        NodeList components = ((Element) sets.item(0)).getElementsByTagNameNS(CALDAV, "comp");
        if (components.getLength() == 0) {
            return true;
        }
        for (int i = 0; i < components.getLength(); i++) {
            if ("VEVENT".equalsIgnoreCase(((Element) components.item(i)).getAttribute("name"))) {
                return true;
            }
        }
        return false;
    }

    static List<String> calendarData(String xml) {
        List<String> texts = new ArrayList<>();
        NodeList data = parse(xml).getElementsByTagNameNS(CALDAV, "calendar-data");
        for (int i = 0; i < data.getLength(); i++) {
            String text = data.item(i).getTextContent();
            if (text != null && !text.isBlank()) {
                texts.add(text);
            }
        }
        return texts;
    }

    private static Document parse(String xml) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            // the answer comes from the internet, no external entities
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            return builder.parse(new InputSource(new StringReader(xml)));
        } catch (Exception e) {
            throw new IllegalStateException("Unreadable answer of iCloud: " + e.getMessage(), e);
        }
    }
}
