package com.labs_101.backend.services;

import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.labs_101.backend.dtos.workout.session.CreateWorkoutSessionDto;
import com.labs_101.backend.dtos.workout.session.UpdateWorkoutSessionDto;
import com.labs_101.backend.dtos.workout.session.WorkoutSessionDto;
import com.labs_101.backend.entities.workout.Workout;
import com.labs_101.backend.entities.workout.WorkoutSession;
import com.labs_101.backend.entities.workout.workoutExercises.CardioItem;
import com.labs_101.backend.entities.workout.workoutExercises.StrengthItem;
import com.labs_101.backend.exception.BadRequestException;
import com.labs_101.backend.exception.NotFoundException;
import com.labs_101.backend.mapper.WorkoutSessionMapper;
import com.labs_101.backend.repositories.UserRepository;
import com.labs_101.backend.repositories.WorkoutSessionRepository;

/**
 * Workouts a user does: started from a template (or empty), saved while
 * training and finished, only one can be running at a time.
 */
@Service
public class WorkoutSessionService {
    private static final String DEFAULT_NAME = "Training";

    private final WorkoutSessionRepository sessionRepository;
    private final WorkoutService workoutService;
    private final UserRepository userRepository;

    WorkoutSessionService(WorkoutSessionRepository sessionRepository, WorkoutService workoutService,
            UserRepository userRepository) {
        this.sessionRepository = sessionRepository;
        this.workoutService = workoutService;
        this.userRepository = userRepository;
    }

    /** Finished sessions started in the range, newest first. */
    @Transactional(readOnly = true)
    public List<WorkoutSessionDto> getSessions(String userId, Instant from, Instant to) {
        return sessionRepository.findByUser_IdAndEndedAtIsNotNullAndStartedAtBetweenOrderByStartedAtDesc(userId,
                from != null ? from : Instant.EPOCH, to != null ? to : Instant.now())
                .stream().map(WorkoutSessionMapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public Optional<WorkoutSessionDto> getActive(String userId) {
        return sessionRepository.findFirstByUser_IdAndEndedAtIsNullOrderByStartedAtDesc(userId)
                .map(WorkoutSessionMapper::toDto);
    }

    @Transactional(readOnly = true)
    public WorkoutSessionDto getSession(String userId, Long id) {
        return WorkoutSessionMapper.toDto(find(userId, id));
    }

    /** Starts a workout with the sets of the template, none of them done yet, and the notes of last time. */
    @Transactional
    public WorkoutSessionDto start(String userId, CreateWorkoutSessionDto request) {
        sessionRepository.findFirstByUser_IdAndEndedAtIsNullOrderByStartedAtDesc(userId).ifPresent((active) -> {
            throw BadRequestException.workoutSessionActive(active.getId());
        });

        WorkoutSession session = new WorkoutSession();
        session.setUser(userRepository.getReferenceById(userId));
        session.setStartedAt(Instant.now());
        session.setName(DEFAULT_NAME);

        if (request.workoutId() != null) {
            // the own templates and the ones shared with the user
            Workout workout = workoutService.findVisible(userId, request.workoutId());
            session.setWorkout(workout);
            session.setName(workout.getName());
            Map<Long, String> notes = lastNotes(userId);
            session.setLog(new WorkoutSession.Log(workout.getExercises().stream()
                    .map((item) -> new WorkoutSession.Entry(item.getExercise().getId(), item.getExercise().getName(),
                            notes.get(item.getExercise().getId()),
                            item instanceof StrengthItem strength
                                    ? strength.getSettings().sets().stream()
                                            .map((set) -> new WorkoutSession.Set(set.reps(), set.weightKg(), set.rpe(), false, set.restSeconds()))
                                            .toList()
                                    : List.of(),
                            item instanceof CardioItem cardio
                                    ? new WorkoutSession.Cardio(cardio.getSettings().minutes(), false)
                                    : null))
                    .toList()));
        }
        if (request.name() != null && !request.name().isBlank()) {
            session.setName(request.name().trim());
        }

        return WorkoutSessionMapper.toDto(sessionRepository.save(session));
    }

    /** Only while it is running, so a late save can't bring back the sets dropped when finishing. */
    @Transactional
    public WorkoutSessionDto update(String userId, Long id, UpdateWorkoutSessionDto request) {
        WorkoutSession session = find(userId, id);
        if (session.getEndedAt() != null) {
            throw BadRequestException.workoutSessionFinished(id);
        }
        apply(session, request);
        return WorkoutSessionMapper.toDto(sessionRepository.save(session));
    }

    /** Saves the last state and ends the workout, sets and cardio that weren't checked off are dropped. */
    @Transactional
    public WorkoutSessionDto finish(String userId, Long id, UpdateWorkoutSessionDto request) {
        WorkoutSession session = find(userId, id);
        if (request != null) {
            apply(session, request);
        }
        session.setLog(new WorkoutSession.Log(session.getLog().exercises().stream()
                .filter(WorkoutSession.Entry::isDone)
                .map((entry) -> new WorkoutSession.Entry(entry.exerciseId(), entry.name(), entry.note(),
                        entry.sets().stream().filter(WorkoutSession.Set::done).toList(), entry.cardio()))
                .toList()));
        if (session.getEndedAt() == null) {
            session.setEndedAt(Instant.now());
        }
        return WorkoutSessionMapper.toDto(sessionRepository.save(session));
    }

    @Transactional
    public void delete(String userId, Long id) {
        sessionRepository.delete(find(userId, id));
    }

    /**
     * The note of the last finished session with the exercise, per exercise. An exercise whose note
     * was removed last time has none, so a removed note doesn't come back.
     */
    private Map<Long, String> lastNotes(String userId) {
        Map<Long, String> notes = new HashMap<>();
        Set<Long> seen = new HashSet<>();
        for (WorkoutSession previous : sessionRepository.findTop50ByUser_IdAndEndedAtIsNotNullOrderByStartedAtDesc(userId)) {
            for (WorkoutSession.Entry entry : previous.getLog().exercises()) {
                if (entry.exerciseId() != null && seen.add(entry.exerciseId()) && entry.note() != null) {
                    notes.put(entry.exerciseId(), entry.note());
                }
            }
        }
        return notes;
    }

    private WorkoutSession find(String userId, Long id) {
        return sessionRepository.findByIdAndUser_Id(id, userId)
                .orElseThrow(() -> NotFoundException.workoutSession(id));
    }

    private static void apply(WorkoutSession session, UpdateWorkoutSessionDto request) {
        if (request.name() != null && !request.name().isBlank()) {
            session.setName(request.name().trim());
        }
        if (request.exercises() != null) {
            session.setLog(WorkoutSessionMapper.toLog(request.exercises()));
        }
    }
}
