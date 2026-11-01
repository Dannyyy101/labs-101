package com.labs_101.backend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import com.labs_101.backend.dtos.exercises.ExerciseDto;
import com.labs_101.backend.dtos.workout.CardioItemDto;
import com.labs_101.backend.dtos.workout.CreateWorkoutDto;
import com.labs_101.backend.dtos.workout.SetDto;
import com.labs_101.backend.dtos.workout.StrengthItemDto;
import com.labs_101.backend.dtos.workout.WorkoutDto;
import com.labs_101.backend.dtos.workout.session.CreateWorkoutSessionDto;
import com.labs_101.backend.dtos.workout.session.SessionCardioDto;
import com.labs_101.backend.dtos.workout.session.SessionExerciseDto;
import com.labs_101.backend.dtos.workout.session.SessionSetDto;
import com.labs_101.backend.dtos.workout.session.UpdateWorkoutSessionDto;
import com.labs_101.backend.dtos.workout.session.WorkoutSessionDto;
import com.labs_101.backend.entities.Exercise;
import com.labs_101.backend.entities.Friendship;
import com.labs_101.backend.entities.User;
import com.labs_101.backend.exception.BadRequestException;
import com.labs_101.backend.exception.ForbiddenException;
import com.labs_101.backend.exception.NotFoundException;
import com.labs_101.backend.repositories.ExerciseRepository;
import com.labs_101.backend.repositories.FriendshipRepository;
import com.labs_101.backend.repositories.NotificationRepository;
import com.labs_101.backend.repositories.UserRepository;
import com.labs_101.backend.security.ZitadelClient;
import com.labs_101.backend.services.NotificationService;
import com.labs_101.backend.services.WorkoutService;
import com.labs_101.backend.services.WorkoutSessionService;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = { EmbeddedPostgresConfiguration.class })
@Import({ WorkoutService.class, WorkoutSessionService.class, NotificationService.class, ZitadelClient.class })
@TestPropertySource(properties = { "auth.issuer=https://auth.example.com", "auth.organization-id=org-1" })
public class WorkoutSessionTest {

    @Autowired
    private WorkoutService workoutService;
    @Autowired
    private WorkoutSessionService sessionService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ExerciseRepository exerciseRepository;
    @Autowired
    private FriendshipRepository friendshipRepository;
    @Autowired
    private NotificationRepository notificationRepository;

    private String userId;
    private ExerciseDto bench;
    private ExerciseDto squat;

    @BeforeEach
    void setUp() {
        userId = userRepository.saveAndFlush(new User(UUID.randomUUID().toString())).getId();
        bench = exercise("Bankdrücken");
        squat = exercise("Kniebeuge");
    }

    @Test
    void testStartCopiesTemplate() {
        WorkoutDto workout = workout("Push", bench, squat);

        WorkoutSessionDto session = sessionService.start(userId, new CreateWorkoutSessionDto(workout.getId(), null));

        assertEquals("Push", session.name());
        assertEquals(workout.getId(), session.workoutId());
        assertNull(session.endedAt());
        assertEquals(List.of("Bankdrücken", "Kniebeuge"), session.exercises().stream().map(SessionExerciseDto::name).toList());
        SessionSetDto set = session.exercises().getFirst().sets().getFirst();
        assertEquals(10, set.reps());
        assertEquals(60, set.weightKg());
        assertEquals(List.of(90, 180), session.exercises().getFirst().sets().stream().map(SessionSetDto::restSeconds).toList());
        assertTrue(session.exercises().stream().flatMap((e) -> e.sets().stream()).noneMatch(SessionSetDto::done));
        assertEquals(session.id(), sessionService.getActive(userId).orElseThrow().id());
    }

    @Test
    void testOnlyOneActiveSession() {
        sessionService.start(userId, new CreateWorkoutSessionDto(null, null));
        assertThrows(BadRequestException.class, () -> sessionService.start(userId, new CreateWorkoutSessionDto(null, null)));
    }

    @Test
    void testFinishKeepsDoneSets() {
        WorkoutSessionDto session = sessionService.start(userId, new CreateWorkoutSessionDto(null, "Beine"));
        assertEquals("Beine", session.name());
        assertTrue(session.exercises().isEmpty());

        List<SessionExerciseDto> exercises = List.of(
                new SessionExerciseDto(squat.getId(), squat.getName(), null, List.of(
                        new SessionSetDto(5, 100, 8, true, 180),
                        new SessionSetDto(5, 100, null, false, null)), null),
                new SessionExerciseDto(bench.getId(), bench.getName(), null, List.of(
                        new SessionSetDto(8, 70, null, false, null)), null));
        sessionService.update(userId, session.id(), new UpdateWorkoutSessionDto(null, exercises));
        assertEquals(2, sessionService.getSession(userId, session.id()).exercises().size());

        WorkoutSessionDto finished = sessionService.finish(userId, session.id(), null);

        assertNotNull(finished.endedAt());
        assertEquals(1, finished.exercises().size());
        assertEquals(List.of(new SessionSetDto(5, 100, 8, true, 180)), finished.exercises().getFirst().sets());
        assertTrue(sessionService.getActive(userId).isEmpty());
        // a save arriving after finishing must not bring back the dropped sets
        assertThrows(BadRequestException.class,
                () -> sessionService.update(userId, session.id(), new UpdateWorkoutSessionDto(null, exercises)));

        List<WorkoutSessionDto> sessions = sessionService.getSessions(userId, Instant.now().minusSeconds(3600), null);
        assertEquals(List.of(finished.id()), sessions.stream().map(WorkoutSessionDto::id).toList());

        // a new one can be started once the last one is finished
        sessionService.start(userId, new CreateWorkoutSessionDto(null, null));
    }

    @Test
    void testStartTakesNotesOfLastTime() {
        WorkoutDto workout = workout("Push", bench, squat);
        WorkoutSessionDto first = sessionService.start(userId, new CreateWorkoutSessionDto(workout.getId(), null));
        assertTrue(first.exercises().stream().allMatch((e) -> e.note() == null));
        sessionService.finish(userId, first.id(), new UpdateWorkoutSessionDto(null, List.of(
                new SessionExerciseDto(bench.getId(), bench.getName(), "  Sitz auf Stufe 4 ", List.of(new SessionSetDto(8, 70, null, true, null)), null),
                new SessionExerciseDto(squat.getId(), squat.getName(), "Knie schmerzt", List.of(new SessionSetDto(5, 100, null, true, null)), null))));

        WorkoutSessionDto second = sessionService.start(userId, new CreateWorkoutSessionDto(workout.getId(), null));
        assertEquals(Arrays.asList("Sitz auf Stufe 4", "Knie schmerzt"), second.exercises().stream().map(SessionExerciseDto::note).toList());
        // a note removed last time doesn't come back
        sessionService.finish(userId, second.id(), new UpdateWorkoutSessionDto(null, List.of(
                new SessionExerciseDto(bench.getId(), bench.getName(), "Sitz auf Stufe 4", List.of(new SessionSetDto(8, 70, null, true, null)), null),
                new SessionExerciseDto(squat.getId(), squat.getName(), " ", List.of(new SessionSetDto(5, 100, null, true, null)), null))));

        WorkoutSessionDto third = sessionService.start(userId, new CreateWorkoutSessionDto(workout.getId(), null));
        assertEquals(Arrays.asList("Sitz auf Stufe 4", null), third.exercises().stream().map(SessionExerciseDto::note).toList());
    }

    @Test
    void testSessionsOfOtherUsersAreHidden() {
        WorkoutSessionDto session = sessionService.start(userId, new CreateWorkoutSessionDto(null, null));
        String other = userRepository.saveAndFlush(new User(UUID.randomUUID().toString())).getId();

        assertThrows(NotFoundException.class, () -> sessionService.getSession(other, session.id()));
        assertThrows(NotFoundException.class, () -> sessionService.delete(other, session.id()));
        assertTrue(sessionService.getActive(other).isEmpty());
    }

    @Test
    void testUpdateAndDeleteWorkout() {
        WorkoutDto workout = workout("Push", bench);
        WorkoutDto updated = workoutService.update(userId, workout.getId(), new CreateWorkoutDto("Push B", null, List.of(
                new StrengthItemDto(squat, List.of(new SetDto(0, 5, 120, null, null))),
                new StrengthItemDto(bench, List.of()))));

        assertEquals("Push B", updated.getName());
        assertEquals(List.of(squat.getId(), bench.getId()),
                updated.getWorkoutExercises().stream().map((e) -> e.exercise().getId()).toList());

        WorkoutSessionDto session = sessionService.start(userId, new CreateWorkoutSessionDto(workout.getId(), null));
        sessionService.finish(userId, session.id(), null);
        workoutService.delete(userId, workout.getId());

        // the history stays, without the template
        assertThrows(NotFoundException.class, () -> workoutService.getById(userId, workout.getId()));
        assertNull(sessionService.getSession(userId, session.id()).workoutId());
    }

    @Test
    void testWorkoutsOfOthersAreHidden() {
        WorkoutDto workout = workout("Push", bench);
        String other = user();

        assertTrue(workout.isEditable());
        assertEquals(List.of(workout.getId()), workoutService.getAll(userId).stream().map(WorkoutDto::getId).toList());
        assertTrue(workoutService.getAll(other).isEmpty());
        assertThrows(NotFoundException.class, () -> workoutService.getById(other, workout.getId()));
        assertThrows(NotFoundException.class, () -> workoutService.update(other, workout.getId(),
                new CreateWorkoutDto("Pull", null, List.of())));
        assertThrows(NotFoundException.class, () -> workoutService.delete(other, workout.getId()));
        assertThrows(NotFoundException.class,
                () -> sessionService.start(other, new CreateWorkoutSessionDto(workout.getId(), null)));
    }

    @Test
    void testShareWithFriend() {
        WorkoutDto workout = workout("Push", bench);
        String friend = user();
        String stranger = user();
        befriend(userId, friend);

        // only friends
        assertThrows(NotFoundException.class, () -> workoutService.share(userId, workout.getId(), List.of(stranger)));
        WorkoutDto shared = workoutService.share(userId, workout.getId(), List.of(friend));
        assertEquals(List.of(friend), shared.getSharedWith().stream().map((u) -> u.id()).toList());
        assertEquals(1, notificationRepository.findAll().stream().filter((n) -> n.getUser().getId().equals(friend)).count());

        // the friend sees and trains it, but can't change or share it
        WorkoutDto seen = workoutService.getById(friend, workout.getId());
        assertFalse(seen.isEditable());
        assertTrue(seen.isSharedWithMe());
        assertTrue(seen.getSharedWith().isEmpty());
        assertEquals(List.of(workout.getId()), workoutService.getAll(friend).stream().map(WorkoutDto::getId).toList());
        assertThrows(ForbiddenException.class, () -> workoutService.update(friend, workout.getId(),
                new CreateWorkoutDto("Pull", null, List.of())));
        assertThrows(ForbiddenException.class, () -> workoutService.share(friend, workout.getId(), List.of()));
        assertEquals(workout.getId(), sessionService.start(friend, new CreateWorkoutSessionDto(workout.getId(), null)).workoutId());

        // sharing again doesn't notify again
        workoutService.share(userId, workout.getId(), List.of(friend));
        assertEquals(1, notificationRepository.findAll().stream().filter((n) -> n.getUser().getId().equals(friend)).count());

        // the friend removes it from their list
        workoutService.leave(friend, workout.getId());
        assertTrue(workoutService.getAll(friend).isEmpty());
        assertTrue(workoutService.getById(userId, workout.getId()).getSharedWith().isEmpty());
    }

    @Test
    void testCardioInWorkout() {
        ExerciseDto rowing = exercise("Rudergerät");
        WorkoutDto workout = workoutService.create(userId, new CreateWorkoutDto("Push + Cardio", null, List.of(
                new CardioItemDto(rowing, 10, null),
                new StrengthItemDto(bench, List.of(new SetDto(0, 10, 60, null, 90))))));
        assertEquals(10, ((CardioItemDto) workout.getWorkoutExercises().getFirst()).minutes());
        assertTrue(((CardioItemDto) workout.getWorkoutExercises().getFirst()).sets().isEmpty());

        WorkoutSessionDto session = sessionService.start(userId, new CreateWorkoutSessionDto(workout.getId(), null));
        assertEquals(new SessionCardioDto(10, false), session.exercises().getFirst().cardio());
        assertTrue(session.exercises().getFirst().sets().isEmpty());
        assertNull(session.exercises().get(1).cardio());

        WorkoutSessionDto finished = sessionService.finish(userId, session.id(), new UpdateWorkoutSessionDto(null, List.of(
                new SessionExerciseDto(rowing.getId(), rowing.getName(), null, List.of(), new SessionCardioDto(12, true)),
                new SessionExerciseDto(bench.getId(), bench.getName(), null, List.of(new SessionSetDto(10, 60, null, true, null)), null),
                // not checked off, dropped like a set
                new SessionExerciseDto(rowing.getId(), rowing.getName(), null, List.of(), new SessionCardioDto(5, false)))));
        assertEquals(2, finished.exercises().size());
        assertEquals(new SessionCardioDto(12, true), finished.exercises().getFirst().cardio());
    }

    private String user() {
        return userRepository.saveAndFlush(new User(UUID.randomUUID().toString())).getId();
    }

    private void befriend(String a, String b) {
        Friendship friendship = new Friendship(userRepository.getReferenceById(a), userRepository.getReferenceById(b), Instant.now());
        friendship.setAcceptedAt(Instant.now());
        friendshipRepository.saveAndFlush(friendship);
    }

    private ExerciseDto exercise(String name) {
        Exercise exercise = exerciseRepository.saveAndFlush(new Exercise(null, name, "", "Strength Training", new ArrayList<>()));
        return new ExerciseDto(exercise.getId(), name, "", "Strength Training", new ArrayList<>());
    }

    private WorkoutDto workout(String name, ExerciseDto... exercises) {
        return workoutService.create(userId, new CreateWorkoutDto(name, null, List.of(exercises).stream()
                .map((e) -> (com.labs_101.backend.dtos.workout.ExerciseItem) new StrengthItemDto(e, List.of(
                        new SetDto(0, 10, 60, null, 90), new SetDto(1, 10, 60, null, 180))))
                .toList()));
    }
}
