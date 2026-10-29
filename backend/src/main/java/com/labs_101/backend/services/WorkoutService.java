package com.labs_101.backend.services;

import com.labs_101.backend.mapper.BodyPartMapper;
import com.labs_101.backend.mapper.ExerciseMapper;
import com.labs_101.backend.mapper.WorkoutMapper;
import com.labs_101.backend.repositories.CalendarRepository;
import com.labs_101.backend.repositories.ExerciseRepository;
import com.labs_101.backend.repositories.FriendshipRepository;
import com.labs_101.backend.repositories.UserRepository;
import com.labs_101.backend.repositories.WorkoutRepository;
import com.labs_101.backend.repositories.WorkoutSessionRepository;

import jakarta.transaction.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.labs_101.backend.dtos.BodyPartDto;
import com.labs_101.backend.dtos.exercises.CreateExerciseDto;
import com.labs_101.backend.dtos.exercises.ExerciseDto;
import com.labs_101.backend.dtos.notification.CreateNotificationDto;
import com.labs_101.backend.dtos.workout.CardioItemDto;
import com.labs_101.backend.dtos.workout.CreateWorkoutDto;
import com.labs_101.backend.dtos.workout.ExerciseItem;
import com.labs_101.backend.dtos.workout.StrengthItemDto;
import com.labs_101.backend.dtos.workout.WorkoutDto;
import com.labs_101.backend.entities.workout.workoutExercises.CardioItem;
import com.labs_101.backend.entities.workout.workoutExercises.StrengthItem;
import com.labs_101.backend.security.ZitadelClient;
import com.labs_101.backend.exception.ForbiddenException;
import com.labs_101.backend.exception.NotFoundException;
import com.labs_101.backend.entities.BodyPart;
import com.labs_101.backend.entities.Exercise;
import com.labs_101.backend.entities.Friendship;
import com.labs_101.backend.entities.NotificationType;
import com.labs_101.backend.entities.User;
import com.labs_101.backend.entities.workout.Workout;
import com.labs_101.backend.entities.workout.WorkoutExercise;

@Service
public class WorkoutService {
    private final WorkoutRepository workoutRepository;
    private final ExerciseRepository exerciseRepository;
    private final WorkoutSessionRepository sessionRepository;
    private final CalendarRepository calendarRepository;
    private final UserRepository userRepository;
    private final FriendshipRepository friendshipRepository;
    private final NotificationService notificationService;
    private final ZitadelClient zitadelClient;

    WorkoutService(ExerciseRepository exerciseRepository,
            WorkoutRepository workoutRepository, WorkoutSessionRepository sessionRepository,
            CalendarRepository calendarRepository, UserRepository userRepository,
            FriendshipRepository friendshipRepository, NotificationService notificationService,
            ZitadelClient zitadelClient) {
        this.exerciseRepository = exerciseRepository;
        this.workoutRepository = workoutRepository;
        this.sessionRepository = sessionRepository;
        this.calendarRepository = calendarRepository;
        this.userRepository = userRepository;
        this.friendshipRepository = friendshipRepository;
        this.notificationService = notificationService;
        this.zitadelClient = zitadelClient;
    }

    @Transactional
    public void createExercise(CreateExerciseDto exerciseDto) {
        Exercise exercise = ExerciseMapper.fromCreateExerciseDto(exerciseDto);
        exercise.getBodyParts().forEach(part -> part.setExercise(exercise));
        exerciseRepository.save(exercise);
    }

    public List<ExerciseDto> getAllExercises(String name, String type) {
        List<Specification<Exercise>> specs = new ArrayList<>();

        if (name != null && !name.isBlank()) {
            specs.add((root, query, cb) -> cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%"));
        }
        if (type != null && !type.isBlank()) {
            specs.add((root, query, cb) -> cb.equal(cb.lower(root.get("type")), type.toLowerCase()));
        }

        Specification<Exercise> spec = Specification.allOf(specs);

        return exerciseRepository.findAll(spec).stream()
                .map(ExerciseMapper::fromExercise)
                .toList();
    }

    @Transactional
    public WorkoutDto create(String userId, CreateWorkoutDto request) {
        Workout workout = new Workout();
        workout.setDuration(0.0);
        workout.setOwner(userRepository.getReferenceById(userId));
        apply(workout, request);
        return toDto(workoutRepository.save(workout), userId);
    }

    /** Replaces name and exercises, sessions started from the workout keep their copy. */
    @Transactional
    public WorkoutDto update(String userId, Long id, CreateWorkoutDto request) {
        Workout workout = findEditable(userId, id);
        workout.getExercises().clear();
        // removes the old rows before the new ones are inserted
        workoutRepository.flush();
        apply(workout, request);
        return toDto(workoutRepository.save(workout), userId);
    }

    @Transactional
    public void delete(String userId, Long id) {
        Workout workout = findEditable(userId, id);
        // the history stays, only the link to the template is removed
        sessionRepository.findByWorkout_Id(id).forEach((session) -> session.setWorkout(null));
        // planned events stay in the calendar without a workout
        calendarRepository.findByWorkout_Id(id).forEach((event) -> event.setWorkout(null));
        workoutRepository.delete(workout);
    }

    /** The own templates and the ones friends shared with the user. */
    @Transactional
    public List<WorkoutDto> getAll(String userId) {
        return workoutRepository.findVisibleTo(userId).stream().map((workout) -> toDto(workout, userId)).toList();
    }

    @Transactional
    public WorkoutDto getById(String userId, Long id) {
        return toDto(findVisible(userId, id), userId);
    }

    /**
     * Shares the template with exactly these friends, the ones newly added get a notification.
     * Only friends, anyone else is reported as not found like everywhere else.
     */
    @Transactional
    public WorkoutDto share(String userId, Long id, List<String> friendIds) {
        Workout workout = findEditable(userId, id);
        Set<User> friends = new LinkedHashSet<>();
        for (String friendId : new LinkedHashSet<>(friendIds == null ? List.<String>of() : friendIds)) {
            if (friendId.equals(userId)) {
                continue;
            }
            Friendship friendship = friendshipRepository.findBetween(userId, friendId).filter(Friendship::isAccepted)
                    .orElseThrow(() -> NotFoundException.friend(friendId));
            friends.add(friendship.other(userId));
        }

        Set<String> before = new HashSet<>(workout.getSharedWith().stream().map(User::getId).toList());
        workout.getSharedWith().clear();
        workout.getSharedWith().addAll(friends);
        Workout saved = workoutRepository.save(workout);

        String owner = name(userRepository.findById(userId).orElse(null));
        friends.stream().filter((friend) -> !before.contains(friend.getId())).forEach((friend) ->
                notificationService.create(friend.getId(), new CreateNotificationDto(NotificationType.INFO,
                        owner + " hat das Workout „" + saved.getName() + "“ mit dir geteilt", null,
                        "/workouts/" + saved.getId(), "Workout ansehen")));
        return toDto(saved, userId);
    }

    /** Removes a template a friend shared from the own list, the friend keeps it. */
    @Transactional
    public void leave(String userId, Long id) {
        Workout workout = findVisible(userId, id);
        if (!workout.getSharedWith().removeIf((user) -> user.getId().equals(userId))) {
            throw NotFoundException.workout(id);
        }
    }

    /** Templates the user doesn't see are reported as not found, so their ids give nothing away. */
    public Workout findVisible(String userId, Long id) {
        return workoutRepository.findById(id).filter((workout) -> workout.isVisibleTo(userId))
                .orElseThrow(() -> NotFoundException.workout(id));
    }

    private Workout findEditable(String userId, Long id) {
        Workout workout = findVisible(userId, id);
        if (!workout.isEditableBy(userId)) {
            throw ForbiddenException.workout(id);
        }
        return workout;
    }

    private WorkoutDto toDto(Workout workout, String userId) {
        return WorkoutMapper.fromWorkoutToWorkoutDto(workout, userId, zitadelClient::avatarUrl);
    }

    private static String name(User user) {
        return user != null && user.getName() != null && !user.getName().isBlank() ? user.getName().strip() : "Jemand";
    }

    private void apply(Workout workout, CreateWorkoutDto request) {
        workout.setName(request.name() == null || request.name().isBlank() ? "Workout" : request.name().trim());
        for (ExerciseItem item : request.exercises()) {
            Exercise exercise = exerciseRepository.findById(item.exercise().getId())
                    .orElseThrow(() -> NotFoundException.exercise(item.exercise().getId()));
            workout.addExercise(toEntity(exercise, item));
        }
    }

    private WorkoutExercise<?> toEntity(Exercise exercise, ExerciseItem item) {
        return switch (item) {
            case StrengthItemDto s -> new StrengthItem(
                    exercise,
                    new StrengthItem.Settings(
                            s.sets().stream()
                                    .map(d -> new StrengthItem.Set(d.order(), d.reps(), d.weightKg(), d.rpe(), d.restSeconds()))
                                    .toList()));
            case CardioItemDto c -> new CardioItem(exercise, new CardioItem.Settings(Math.max(0, c.minutes())));
        };
    }

    public void deleteExercise(Long id) {
        exerciseRepository.deleteById(id);
    }

    public ExerciseDto updateExerciseById(ExerciseDto exerciseDto) {
        Exercise model = exerciseRepository.findById(exerciseDto.getId()).orElseThrow();
        if (!exerciseDto.getName().equals(model.getName())) {
            model.setName(exerciseDto.getName());
        }
        if (!exerciseDto.getDescription().equals(model.getDescription())) {
            model.setDescription(exerciseDto.getDescription());
        }

        syncBodyParts(model, exerciseDto.getBodyParts());

        return ExerciseMapper.fromExercise(exerciseRepository.save(model));
    }

    private void syncBodyParts(Exercise exercise, List<BodyPartDto> incoming) {
        Map<Long, BodyPartDto> byId = incoming.stream()
                .filter(d -> d.getId() != null)
                .collect(Collectors.toMap(BodyPartDto::getId, d -> d));

        exercise.getBodyParts().removeIf(part -> !byId.containsKey(part.getId()));

        exercise.getBodyParts().forEach(part -> {
            BodyPartDto d = byId.get(part.getId());
            part.setColor(d.getColor());
            part.setIntensity(d.getIntensity());
        });

        incoming.stream()
                .filter(d -> d.getId() == null)
                .forEach(d -> {
                    BodyPart part = BodyPartMapper.fromDto(d);
                    part.setExercise(exercise);
                    exercise.getBodyParts().add(part);
                });
    }
}
