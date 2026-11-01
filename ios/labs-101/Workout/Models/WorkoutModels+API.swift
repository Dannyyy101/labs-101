import Foundation
import LabsAPI

// the DTOs of the backend (generated from api/openapi.json) as the models of the app

nonisolated extension Exercise {
    init(_ dto: Components.Schemas.ExerciseDto) {
        self.init(id: Int(dto.id), name: dto.name, type: dto._type, bodyParts: dto.bodyParts.map { BodyPart(slug: $0.slug) })
    }
}

nonisolated extension WorkoutTemplate {
    init(_ dto: Components.Schemas.WorkoutDto) {
        self.init(id: Int(dto.id), name: dto.name, workoutExercises: dto.workoutExercises.map(Item.init),
                  ownerName: dto.ownerName, sharedWithMe: dto.sharedWithMe)
    }
}

nonisolated extension WorkoutTemplate.Item {
    init(_ dto: Components.Schemas.WorkoutDto.WorkoutExercisesPayloadPayload) {
        switch dto {
        case .StrengthItemDto(let strength):
            self.init(type: strength.value1._type, exercise: Exercise(strength.value2.exercise),
                      sets: strength.value2.sets.map(WorkoutTemplate.TemplateSet.init), minutes: nil)
        case .CardioItemDto(let cardio):
            self.init(type: cardio.value1._type, exercise: Exercise(cardio.value2.exercise),
                      sets: cardio.value2.sets.map(WorkoutTemplate.TemplateSet.init), minutes: Int(cardio.value2.minutes))
        }
    }
}

nonisolated extension WorkoutTemplate.TemplateSet {
    init(_ dto: Components.Schemas.SetDto) {
        self.init(reps: Int(dto.reps), weightKg: dto.weightKg)
    }
}

nonisolated extension WorkoutSession {
    init(_ dto: Components.Schemas.WorkoutSessionDto) {
        self.init(id: Int(dto.id), workoutId: dto.workoutId.map(Int.init), name: dto.name, startedAt: dto.startedAt,
                  endedAt: dto.endedAt, exercises: dto.exercises.map(SessionExercise.init))
    }
}

nonisolated extension SessionExercise {
    init(_ dto: Components.Schemas.SessionExerciseDto) {
        self.init(exerciseId: dto.exerciseId.map(Int.init), name: dto.name, note: dto.note,
                  sets: dto.sets.map(SessionSet.init),
                  cardio: dto.cardio.map { SessionCardio(minutes: Int($0.value1.minutes), done: $0.value1.done) })
    }

    var dto: Components.Schemas.SessionExerciseDto {
        .init(cardio: cardio.map { .init(value1: .init(done: $0.done, minutes: Int32($0.minutes))) },
              exerciseId: exerciseId.map(Int64.init), name: name, note: note, sets: sets.map(\.dto))
    }
}

nonisolated extension SessionSet {
    init(_ dto: Components.Schemas.SessionSetDto) {
        self.init(reps: Int(dto.reps), weightKg: dto.weightKg, rpe: dto.rpe.map(Int.init), done: dto.done,
                  restSeconds: dto.restSeconds.map(Int.init))
    }

    var dto: Components.Schemas.SessionSetDto {
        .init(done: done, reps: Int32(reps), restSeconds: restSeconds.map(Int32.init), rpe: rpe.map(Int32.init), weightKg: weightKg)
    }
}
