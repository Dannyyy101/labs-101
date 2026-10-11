package com.labs_101.backend.services;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.labs_101.backend.calendarImport.CalDavClient;
import com.labs_101.backend.calendarImport.CalDavClient.DiscoveredCalendar;
import com.labs_101.backend.calendarImport.CredentialCipher;
import com.labs_101.backend.calendarImport.IcsClient;
import com.labs_101.backend.calendarImport.IcsParser;
import com.labs_101.backend.calendarImport.IcsParser.Feed;
import com.labs_101.backend.calendarImport.IcsParser.ImportedEvent;
import com.labs_101.backend.dtos.calendar.ConnectAppleAccountDto;
import com.labs_101.backend.dtos.calendar.CreateExternalCalendarDto;
import com.labs_101.backend.dtos.calendar.ExternalCalendarDto;
import com.labs_101.backend.dtos.calendar.ExternalCalendarEventDto;
import com.labs_101.backend.entities.AppleAccount;
import com.labs_101.backend.entities.ExternalCalendar;
import com.labs_101.backend.exception.BadRequestException;
import com.labs_101.backend.exception.NotFoundException;
import com.labs_101.backend.repositories.AppleAccountRepository;
import com.labs_101.backend.repositories.ExternalCalendarRepository;
import com.labs_101.backend.repositories.UserRepository;

/**
 * The user's calendars of other apps: the calendars of connected iCloud accounts and shared Apple calendars.
 * What was loaded is kept in memory for {@link #MAX_AGE}, to not ask Apple on every request.
 */
@Service
public class ExternalCalendarService {

    static final Duration MAX_AGE = Duration.ofMinutes(5);
    private static final String DEFAULT_NAME = "Apple Kalender";

    /** A downloaded feed of a shared calendar, or the events of an account's calendar in one window. */
    private record Cached(Instant loadedAt, Feed feed) {
    }

    private final Logger logger = LoggerFactory.getLogger(ExternalCalendarService.class);
    private final Map<String, Cached> cache = new ConcurrentHashMap<>();
    /** Why a calendar couldn't be loaded the last time, by its id. */
    private final Map<Long, String> errors = new ConcurrentHashMap<>();
    private final ExternalCalendarRepository externalCalendarRepository;
    private final AppleAccountRepository appleAccountRepository;
    private final UserRepository userRepository;
    private final IcsClient icsClient;
    private final CalDavClient calDavClient;
    private final CredentialCipher credentialCipher;

    ExternalCalendarService(ExternalCalendarRepository externalCalendarRepository,
            AppleAccountRepository appleAccountRepository, UserRepository userRepository, IcsClient icsClient,
            CalDavClient calDavClient, CredentialCipher credentialCipher) {
        this.externalCalendarRepository = externalCalendarRepository;
        this.appleAccountRepository = appleAccountRepository;
        this.userRepository = userRepository;
        this.icsClient = icsClient;
        this.calDavClient = calDavClient;
        this.credentialCipher = credentialCipher;
    }

    @Transactional(readOnly = true)
    public List<ExternalCalendarDto> getCalendars(String userId) {
        return externalCalendarRepository.findByUser_IdOrderByCreatedAt(userId).stream().map(this::toDto).toList();
    }

    /**
     * Signs in to iCloud and adds the account's calendars that aren't there yet. Connecting an account again
     * replaces its password, e.g. after the old one was revoked.
     */
    @Transactional
    public List<ExternalCalendarDto> connectAppleAccount(String userId, ConnectAppleAccountDto dto) {
        if (!credentialCipher.isConfigured()) {
            throw BadRequestException.appleAccountDisabled();
        }
        if (!userRepository.existsById(userId)) {
            throw NotFoundException.user(userId);
        }
        String appleId = dto.appleId() == null ? "" : dto.appleId().strip();
        // Apple shows the password in groups, xxxx-xxxx-xxxx-xxxx, copied with spaces now and then
        String password = dto.password() == null ? "" : dto.password().replaceAll("\\s", "");
        if (appleId.isEmpty() || password.isEmpty()) {
            throw BadRequestException.appleAccountCredentials();
        }

        List<DiscoveredCalendar> discovered;
        try {
            discovered = calDavClient.discover(appleId, password);
        } catch (CalDavClient.UnauthorizedException e) {
            throw BadRequestException.appleAccountCredentials();
        } catch (RuntimeException e) {
            logger.warn("iCloud calendars not readable: {}", e.getMessage());
            throw BadRequestException.appleAccountUnreachable();
        }

        AppleAccount account = appleAccountRepository.findByUser_IdAndAppleIdIgnoreCase(userId, appleId)
                .orElseGet(() -> new AppleAccount(null, userRepository.getReferenceById(userId), appleId, null,
                        Instant.now()));
        account.setPassword(credentialCipher.encrypt(password));
        account = appleAccountRepository.save(account);

        Set<String> known = externalCalendarRepository.findByUser_IdOrderByCreatedAt(userId).stream()
                .map(ExternalCalendar::getUrl).collect(Collectors.toSet());
        for (DiscoveredCalendar calendar : discovered) {
            if (!known.contains(calendar.url())) {
                externalCalendarRepository.save(new ExternalCalendar(null, account.getUser(), calendar.name(),
                        calendar.url(), Instant.now(), account));
            }
        }
        // with the new password they may load again
        externalCalendarRepository.findByUser_IdOrderByCreatedAt(userId)
                .forEach((calendar) -> errors.remove(calendar.getId()));
        return getCalendars(userId);
    }

    /** The account's calendars are removed with it. */
    @Transactional
    public void disconnectAppleAccount(String userId, Long accountId) {
        AppleAccount account = appleAccountRepository.findByIdAndUser_Id(accountId, userId)
                .orElseThrow(() -> NotFoundException.appleAccount(accountId));
        externalCalendarRepository.findByUser_IdOrderByCreatedAt(userId).stream()
                .filter((calendar) -> calendar.getAccount() != null
                        && calendar.getAccount().getId().equals(accountId))
                .forEach(this::delete);
        appleAccountRepository.delete(account);
    }

    /** The feed is loaded once, a link that doesn't give a calendar isn't saved. */
    @Transactional
    public ExternalCalendarDto addCalendar(String userId, CreateExternalCalendarDto dto) {
        if (!userRepository.existsById(userId)) {
            throw NotFoundException.user(userId);
        }
        URI uri = icsClient.normalize(dto.url());
        if (uri == null) {
            throw BadRequestException.externalCalendarUrl();
        }
        Feed feed;
        try {
            feed = IcsParser.parse(icsClient.download(uri));
        } catch (RuntimeException e) {
            logger.info("External calendar not readable: {}", e.getMessage());
            throw BadRequestException.externalCalendarUnreadable();
        }

        String name = dto.name() != null && !dto.name().isBlank() ? dto.name().strip()
                : feed.name() != null && !feed.name().isBlank() ? feed.name().strip() : DEFAULT_NAME;
        ExternalCalendar calendar = externalCalendarRepository.save(new ExternalCalendar(null,
                userRepository.getReferenceById(userId), name, uri.toString(), Instant.now(), null));
        cache.put(feedKey(calendar), new Cached(Instant.now(), feed));
        return toDto(calendar);
    }

    /** A calendar of an account stays hidden until the account is connected again. */
    @Transactional
    public void deleteCalendar(String userId, Long id) {
        delete(externalCalendarRepository.findByIdAndUser_Id(id, userId)
                .orElseThrow(() -> NotFoundException.externalCalendar(id)));
    }

    /** The events of all calendars of the user in the window, a calendar that can't be read is left out. */
    @Transactional(readOnly = true)
    public List<ExternalCalendarEventDto> getEvents(String userId, Instant startDate, Instant endDate) {
        List<ExternalCalendarEventDto> events = new ArrayList<>();
        for (ExternalCalendar calendar : externalCalendarRepository.findByUser_IdOrderByCreatedAt(userId)) {
            Feed feed = feed(calendar, startDate, endDate);
            if (feed == null) {
                continue;
            }
            for (ImportedEvent event : IcsParser.events(feed, startDate, endDate)) {
                events.add(new ExternalCalendarEventDto(calendar.getId(), calendar.getName(), event.uid(),
                        event.title(), event.startDate(), event.endDate(), event.allDay()));
            }
        }
        events.sort(Comparator.comparing(ExternalCalendarEventDto::startDate));
        return events;
    }

    /** A shared calendar is loaded as a whole, of an account's calendar only the events of the window. */
    private Feed feed(ExternalCalendar calendar, Instant startDate, Instant endDate) {
        String key = calendar.getAccount() == null ? feedKey(calendar)
                : calendar.getId() + "@" + startDate + "/" + endDate;
        Cached cached = cache.get(key);
        if (cached != null && cached.loadedAt().plus(MAX_AGE).isAfter(Instant.now())) {
            return cached.feed();
        }
        try {
            Feed feed = load(calendar, startDate, endDate);
            removeExpired();
            cache.put(key, new Cached(Instant.now(), feed));
            errors.remove(calendar.getId());
            return feed;
        } catch (RuntimeException e) {
            logger.warn("External calendar {} not readable: {}", calendar.getId(), e.getMessage());
            errors.put(calendar.getId(), e instanceof CalDavClient.UnauthorizedException
                    ? "Das app-spezifische Passwort gilt nicht mehr, bitte die Apple-ID neu verbinden"
                    : "Konnte zuletzt nicht geladen werden");
            // the last events are still shown, Apple is down now and then
            return cached == null ? null : cached.feed();
        }
    }

    private Feed load(ExternalCalendar calendar, Instant startDate, Instant endDate) {
        URI uri = calendar.getAccount() == null ? icsClient.normalize(calendar.getUrl()) : URI.create(calendar.getUrl());
        if (uri == null || !icsClient.isAllowed(uri)) {
            throw new IllegalStateException("Url not allowed");
        }
        if (calendar.getAccount() == null) {
            return IcsParser.parse(icsClient.download(uri));
        }
        AppleAccount account = calendar.getAccount();
        List<String> texts = calDavClient.events(uri, account.getAppleId(),
                credentialCipher.decrypt(account.getPassword()), startDate, endDate);
        return new Feed(calendar.getName(), texts.stream().flatMap((text) -> IcsParser.parse(text).calendars().stream()).toList());
    }

    /** Every week the planner shows is a window of its own, old ones would pile up. */
    private void removeExpired() {
        Instant oldest = Instant.now().minus(MAX_AGE);
        cache.entrySet().removeIf((entry) -> entry.getValue().loadedAt().isBefore(oldest));
    }

    private void delete(ExternalCalendar calendar) {
        externalCalendarRepository.delete(calendar);
        cache.keySet().removeIf((key) -> key.startsWith(calendar.getId() + "@"));
        errors.remove(calendar.getId());
    }

    private static String feedKey(ExternalCalendar calendar) {
        return calendar.getId() + "@feed";
    }

    private ExternalCalendarDto toDto(ExternalCalendar calendar) {
        AppleAccount account = calendar.getAccount();
        return new ExternalCalendarDto(calendar.getId(), calendar.getName(), calendar.getUrl(),
                errors.get(calendar.getId()), account == null ? null : account.getId(),
                account == null ? null : account.getAppleId());
    }
}
