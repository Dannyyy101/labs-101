package com.labs_101.backend.services;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.labs_101.backend.dtos.friend.FriendDto;
import com.labs_101.backend.dtos.friend.FriendProfileDto;
import com.labs_101.backend.dtos.friend.FriendRequestDto;
import com.labs_101.backend.dtos.friend.FriendsDto;
import com.labs_101.backend.dtos.friend.TrainingDto;
import com.labs_101.backend.dtos.notification.CreateNotificationDto;
import com.labs_101.backend.entities.Cheer;
import com.labs_101.backend.entities.Friendship;
import com.labs_101.backend.entities.NotificationType;
import com.labs_101.backend.entities.User;
import com.labs_101.backend.entities.workout.WorkoutSession;
import com.labs_101.backend.exception.BadRequestException;
import com.labs_101.backend.exception.NotFoundException;
import com.labs_101.backend.mapper.CalendarMapper;
import com.labs_101.backend.repositories.CheerRepository;
import com.labs_101.backend.repositories.FriendshipRepository;
import com.labs_101.backend.repositories.UserRepository;
import com.labs_101.backend.repositories.WorkoutSessionRepository;
import com.labs_101.backend.security.ZitadelClient;

/**
 * Friend requests, the profile of a friend and cheering on a friend while they
 * train. Only friends see each other: everything about someone who isn't a
 * friend is reported as not found.
 */
@Service
public class FriendService {

    /**
     * A session running longer was most likely never finished, the friend
     * isn't shown as training then.
     */
    static final Duration MAX_TRAINING = Duration.ofHours(6);

    private final FriendshipRepository friendshipRepository;
    private final CheerRepository cheerRepository;
    private final UserRepository userRepository;
    private final WorkoutSessionRepository sessionRepository;
    private final NotificationService notificationService;
    private final ProfileService profileService;
    private final ZitadelClient zitadelClient;

    FriendService(FriendshipRepository friendshipRepository, CheerRepository cheerRepository,
            UserRepository userRepository, WorkoutSessionRepository sessionRepository,
            NotificationService notificationService, ProfileService profileService, ZitadelClient zitadelClient) {
        this.friendshipRepository = friendshipRepository;
        this.cheerRepository = cheerRepository;
        this.userRepository = userRepository;
        this.sessionRepository = sessionRepository;
        this.notificationService = notificationService;
        this.profileService = profileService;
        this.zitadelClient = zitadelClient;
    }

    @Transactional(readOnly = true)
    public FriendsDto getFriends(String userId) {
        List<Friendship> all = friendshipRepository.findAllOf(userId);
        List<Friendship> accepted = all.stream().filter(Friendship::isAccepted).toList();
        Map<String, TrainingDto> training = training(userId,
                accepted.stream().map((f) -> f.other(userId).getId()).toList());

        List<FriendDto> friends = accepted.stream()
                .map((f) -> friend(f, userId, training.get(f.other(userId).getId())))
                .sorted(Comparator.comparing((FriendDto f) -> f.training() == null)
                        .thenComparing(FriendDto::name, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .toList();
        Comparator<Friendship> newestFirst = Comparator.comparing(Friendship::getCreatedAt).reversed();
        List<FriendRequestDto> incoming = all.stream()
                .filter((f) -> !f.isAccepted() && f.getAddressee().getId().equals(userId))
                .sorted(newestFirst).map((f) -> request(f, userId)).toList();
        List<FriendRequestDto> outgoing = all.stream()
                .filter((f) -> !f.isAccepted() && f.getRequester().getId().equals(userId))
                .sorted(newestFirst).map((f) -> request(f, userId)).toList();
        return new FriendsDto(friends, incoming, outgoing);
    }

    /**
     * Sends a request to {@code friendId}. When they already sent one to the
     * user, that one is accepted instead, both want the same.
     */
    @Transactional
    public void sendRequest(String userId, String friendId) {
        if (userId.equals(friendId)) {
            throw BadRequestException.friendRequestSelf();
        }
        User friend = userRepository.findById(friendId).orElseThrow(() -> NotFoundException.user(friendId));
        Friendship existing = friendshipRepository.findBetween(userId, friendId).orElse(null);
        if (existing != null) {
            if (existing.isAccepted() || existing.getRequester().getId().equals(userId)) {
                throw BadRequestException.friendRequestExists();
            }
            accept(existing);
            return;
        }

        User user = userRepository.findById(userId).orElseThrow(() -> NotFoundException.user(userId));
        friendshipRepository.save(new Friendship(user, friend, Instant.now()));
        notificationService.create(friendId, new CreateNotificationDto(NotificationType.INFO,
                name(user) + " möchte mit dir befreundet sein", null, "/friends", "Anfrage ansehen"));
    }

    /** Only the one the request was sent to can accept it. */
    @Transactional
    public void acceptRequest(String userId, Long requestId) {
        Friendship request = friendshipRepository.findById(requestId)
                .filter((f) -> !f.isAccepted() && f.getAddressee().getId().equals(userId))
                .orElseThrow(() -> NotFoundException.friendRequest(requestId));
        accept(request);
    }

    /** Declines a request to the user or takes back one of the user. */
    @Transactional
    public void deleteRequest(String userId, Long requestId) {
        Friendship request = friendshipRepository.findById(requestId)
                .filter((f) -> !f.isAccepted() && (f.getAddressee().getId().equals(userId)
                        || f.getRequester().getId().equals(userId)))
                .orElseThrow(() -> NotFoundException.friendRequest(requestId));
        friendshipRepository.delete(request);
    }

    @Transactional
    public void removeFriend(String userId, String friendId) {
        friendshipRepository.delete(friendship(userId, friendId));
    }

    /** The same profile the friend sees on their own profile page. */
    @Transactional(readOnly = true)
    public FriendProfileDto getProfile(String userId, String friendId, LocalDate today, ZoneId zone) {
        Friendship friendship = friendship(userId, friendId);
        return new FriendProfileDto(friend(friendship, userId, training(userId, List.of(friendId)).get(friendId)),
                profileService.getProfile(friendId, today, zone));
    }

    /** Once per workout of the friend, the friend gets a notification. */
    @Transactional
    public void cheer(String userId, String friendId) {
        Friendship friendship = friendship(userId, friendId);
        WorkoutSession session = runningSessions(List.of(friendId)).stream().findFirst()
                .orElseThrow(BadRequestException::friendNotTraining);
        if (cheerRepository.existsBySender_IdAndSession_Id(userId, session.getId())) {
            throw BadRequestException.cheerSent();
        }

        User user = friendship.other(friendId);
        cheerRepository.save(new Cheer(user, session, Instant.now()));
        notificationService.create(friendId, new CreateNotificationDto(NotificationType.SUCCESS,
                "🔥 " + name(user) + " feuert dich an!",
                session.getName() != null ? "Gib alles bei " + session.getName() + "!" : "Gib alles!",
                "/workouts/session", "Zum Training"));
    }

    // MARK: helpers

    private void accept(Friendship request) {
        request.setAcceptedAt(Instant.now());
        User addressee = request.getAddressee();
        notificationService.create(request.getRequester().getId(), new CreateNotificationDto(NotificationType.SUCCESS,
                name(addressee) + " hat deine Freundschaftsanfrage angenommen", null,
                "/friends/" + addressee.getId(), "Profil ansehen"));
    }

    /** The accepted friendship of the two, not found for anyone else. */
    private Friendship friendship(String userId, String friendId) {
        return friendshipRepository.findBetween(userId, friendId).filter(Friendship::isAccepted)
                .orElseThrow(() -> NotFoundException.friend(friendId));
    }

    private List<WorkoutSession> runningSessions(List<String> userIds) {
        if (userIds.isEmpty()) {
            return List.of();
        }
        return sessionRepository.findByUser_IdInAndEndedAtIsNullAndStartedAtAfterOrderByStartedAtDesc(userIds,
                Instant.now().minus(MAX_TRAINING));
    }

    /** The running workout per friend that is training. */
    private Map<String, TrainingDto> training(String userId, List<String> friendIds) {
        Map<String, WorkoutSession> sessions = new HashMap<>();
        // newest first, an older one still open is ignored
        for (WorkoutSession session : runningSessions(friendIds)) {
            sessions.putIfAbsent(session.getUser().getId(), session);
        }
        if (sessions.isEmpty()) {
            return Map.of();
        }
        Set<Long> cheered = new HashSet<>(cheerRepository.cheeredSessionIds(userId,
                sessions.values().stream().map(WorkoutSession::getId).toList()));
        Map<String, TrainingDto> training = new HashMap<>();
        sessions.forEach((friendId, s) -> training.put(friendId,
                new TrainingDto(s.getId(), s.getName(), s.getStartedAt(), cheered.contains(s.getId()))));
        return training;
    }

    private FriendDto friend(Friendship friendship, String userId, TrainingDto training) {
        User friend = friendship.other(userId);
        return new FriendDto(friend.getId(), friend.getName(), zitadelClient.avatarUrl(friend.getId()),
                friendship.getAcceptedAt(), training);
    }

    private FriendRequestDto request(Friendship request, String userId) {
        return new FriendRequestDto(request.getId(),
                CalendarMapper.fromUser(request.other(userId), zitadelClient::avatarUrl), request.getCreatedAt());
    }

    private static String name(User user) {
        return user.getName() != null && !user.getName().isBlank() ? user.getName().strip() : "Jemand";
    }
}
