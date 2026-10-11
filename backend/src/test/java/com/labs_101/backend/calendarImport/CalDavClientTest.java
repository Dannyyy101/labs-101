package com.labs_101.backend.calendarImport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.URI;
import java.time.Instant;
import java.util.Base64;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.labs_101.backend.calendarImport.CalDavClient.DiscoveredCalendar;

public class CalDavClientTest {

    @Test
    void testPrincipalAndCalendarHome() {
        String principal = """
                <?xml version="1.0" encoding="UTF-8"?>
                <multistatus xmlns="DAV:"><response><href>/</href><propstat><prop>
                <current-user-principal><href>/123456789/principal/</href></current-user-principal>
                </prop><status>HTTP/1.1 200 OK</status></propstat></response></multistatus>
                """;
        String home = """
                <?xml version="1.0" encoding="UTF-8"?>
                <multistatus xmlns="DAV:"><response><href>/123456789/principal/</href><propstat><prop>
                <calendar-home-set xmlns="urn:ietf:params:xml:ns:caldav">
                <href xmlns="DAV:">https://p42-caldav.icloud.com:443/123456789/calendars/</href>
                </calendar-home-set></prop><status>HTTP/1.1 200 OK</status></propstat></response></multistatus>
                """;

        assertEquals("/123456789/principal/", CalDavClient.href(principal, "DAV:", "current-user-principal"));
        assertEquals("https://p42-caldav.icloud.com:443/123456789/calendars/",
                CalDavClient.href(home, "urn:ietf:params:xml:ns:caldav", "calendar-home-set"));
    }

    @Test
    void testOnlyCalendarsWithEvents() {
        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <multistatus xmlns="DAV:" xmlns:c="urn:ietf:params:xml:ns:caldav">
                <response><href>/123456789/calendars/</href><propstat><prop>
                  <resourcetype><collection/></resourcetype></prop></propstat></response>
                <response><href>/123456789/calendars/home/</href><propstat><prop>
                  <displayname>Privat</displayname><resourcetype><collection/><c:calendar/></resourcetype>
                  <c:supported-calendar-component-set><c:comp name="VEVENT"/></c:supported-calendar-component-set>
                </prop></propstat></response>
                <response><href>/123456789/calendars/tasks/</href><propstat><prop>
                  <displayname>Erinnerungen</displayname><resourcetype><collection/><c:calendar/></resourcetype>
                  <c:supported-calendar-component-set><c:comp name="VTODO"/></c:supported-calendar-component-set>
                </prop></propstat></response>
                <response><href>/123456789/calendars/inbox/</href><propstat><prop>
                  <resourcetype><collection/><c:schedule-inbox/></resourcetype></prop></propstat></response>
                </multistatus>
                """;

        assertEquals(List.of(new DiscoveredCalendar("Privat", "https://p42-caldav.icloud.com:443/123456789/calendars/home/")),
                CalDavClient.calendars(xml, URI.create("https://p42-caldav.icloud.com:443/123456789/calendars/")));
    }

    @Test
    void testCalendarData() {
        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <multistatus xmlns="DAV:"><response><href>/1/calendars/home/a.ics</href><propstat><prop>
                <calendar-data xmlns="urn:ietf:params:xml:ns:caldav"><![CDATA[BEGIN:VCALENDAR
                VERSION:2.0
                BEGIN:VEVENT
                UID:a
                SUMMARY:Chor
                DTSTART:20261007T160000Z
                DTEND:20261007T173000Z
                END:VEVENT
                END:VCALENDAR
                ]]></calendar-data></prop></propstat></response></multistatus>
                """;

        List<String> texts = CalDavClient.calendarData(xml);
        List<IcsParser.ImportedEvent> events = IcsParser.events(IcsParser.parse(texts.getFirst()),
                Instant.parse("2026-10-07T00:00:00Z"), Instant.parse("2026-10-08T00:00:00Z"));

        assertEquals(List.of("Chor"), events.stream().map(IcsParser.ImportedEvent::title).toList());
    }

    @Test
    void testNoExternalEntities() {
        String xml = """
                <?xml version="1.0"?>
                <!DOCTYPE multistatus [<!ENTITY secret SYSTEM "file:///etc/passwd">]>
                <multistatus xmlns="DAV:"><response><href>&secret;</href></response></multistatus>
                """;

        assertThrows(IllegalStateException.class, () -> CalDavClient.calendarData(xml));
    }

    @Test
    void testPasswordEncryption() {
        String key = Base64.getEncoder().encodeToString(new byte[32]);
        CredentialCipher cipher = new CredentialCipher(key);

        String encrypted = cipher.encrypt("abcd-efgh-ijkl-mnop");

        assertNotEquals("abcd-efgh-ijkl-mnop", encrypted);
        // a new iv every time
        assertNotEquals(encrypted, cipher.encrypt("abcd-efgh-ijkl-mnop"));
        assertEquals("abcd-efgh-ijkl-mnop", cipher.decrypt(encrypted));
        byte[] otherKey = new byte[32];
        otherKey[0] = 1;
        assertThrows(IllegalStateException.class,
                () -> new CredentialCipher(Base64.getEncoder().encodeToString(otherKey)).decrypt(encrypted));
    }

    @Test
    void testInvalidKeyOnlyDisablesAppleAccounts() {
        assertFalse(new CredentialCipher("zu kurz").isConfigured());
        assertFalse(new CredentialCipher(Base64.getEncoder().encodeToString(new byte[16])).isConfigured());
        assertFalse(new CredentialCipher("").isConfigured());
        assertTrue(new CredentialCipher("ab".repeat(32)).isConfigured());
        // like written in application.properties
        assertTrue(new CredentialCipher("\"" + Base64.getEncoder().encodeToString(new byte[32]) + "\"").isConfigured());
    }
}
