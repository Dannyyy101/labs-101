package com.labs_101.backend.services;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.labs_101.backend.dtos.workout.session.CreateWorkoutSessionDto;
import com.labs_101.backend.dtos.workout.session.UpdateWorkoutSessionDto;
import com.labs_101.backend.dtos.workout.session.WorkoutSessionDto;
import com.labs_101.backend.entities.workout.Workout;
import com.labs_101.backend.entities.workout.WorkoutSession;
import com.labs_101.backend.entities.workout.workoutExercises.StrengthItem;
import com.labs_101.backend.exception.BadRequestException;
import com.labs_101.backend.exception.NotFoundException;
import com.labs_101.backend.mapper.WorkoutSessionMapper;
import com.labs_101.backend.repositories.UserRepository;
import com.labs_101.backend.repositories.WorkoutRepository;
import com.labs_101.backend.repositories.WorkoutSessionRepository;

/**
 * Workouts a user does: started from a template (or empty), saved while
 * training and finished, only one can be running at a time.
 */
@Service
public class WorkoutSessionService {
    private static final String DEFAULT_NAME = "Training";

    private final WorkoutSessionRepository sessionRepository;
    private final WorkoutRepository workoutRepository;
    private final UserRepository userRepository;

    WorkoutSessionService(WorkoutSessionRepository sessionRepository, WorkoutRepository workoutRepository,
            UserRepository userRepository) {
        this.sessionRepository = sessionRepository;
        this.workoutRepository = workoutRepository;
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

    /** Starts a workout with the sets of the template, none of them done yet. */
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
            Workout workout = workoutRepository.findById(request.workoutId())
                    .orElseThrow(() -> NotFoundException.workout(request.workoutId()));
            session.setWorkout(workout);
            session.setName(workout.getName());
            session.setLog(new WorkoutSession.Log(workout.getExercises().stream()
                    .map((item) -> new WorkoutSession.Entry(item.getExercise().getId(), item.getExercise().getName(),
                            item instanceof StrengthItem strength
                                    ? strength.getSettings().sets().stream()
                                            .map((set) -> new WorkoutSession.Set(set.reps(), set.weightKg(), set.rpe(), false, set.restSeconds()))
                                            .toList()
                                    : List.of()))
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

    /** Saves the last state and ends the workout, sets that weren't checked off are dropped. */
    @Transactional
    public WorkoutSessionDto finish(String userId, Long id, UpdateWorkoutSessionDto request) {
        WorkoutSession session = find(userId, id);
        if (request != null) {
            apply(session, request);
        }
        session.setLog(new WorkoutSession.Log(session.getLog().exercises().stream()
                .map((entry) -> new WorkoutSession.Entry(entry.exerciseId(), entry.name(),
                        entry.sets().stream().filter(WorkoutSession.Set::done).toList()))
                .filter((entry) -> !entry.sets().isEmpty())
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
