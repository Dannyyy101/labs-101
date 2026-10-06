import Foundation
import Observation

/// A running workout, every change is saved shortly after it was made.
@Observable
final class WorkoutSessionModel {
    enum SaveState {
        case saved, pending, saving, failed
    }

    struct Rest {
        let until: Date
        let total: TimeInterval
    }

    static let restSeconds: TimeInterval = 90
    private static let saveDelay: Duration = .milliseconds(700)

    let session: WorkoutSession
    var name: String {
        didSet { scheduleSave() }
    }
    var exercises: [SessionExercise] {
        didSet { scheduleSave() }
    }
    private(set) var saveState = SaveState.saved
    private(set) var rest: Rest?
    /// counts up when a rest is over, for the haptic feedback
    private(set) var restsFinished = 0

    @ObservationIgnored private let service: WorkoutService
    @ObservationIgnored private var saveTask: Task<Void, Never>?
    @ObservationIgnored private var restTask: Task<Void, Never>?

    init(session: WorkoutSession, service: WorkoutService) {
        self.session = session
        self.service = service
        self.name = session.name
        self.exercises = session.exercises
    }

    var allSets: [SessionSet] {
        exercises.flatMap(\.sets)
    }

    var doneSets: [SessionSet] {
        allSets.filter(\.done)
    }

    // MARK: Editing

    /// Starts with the sets of last time, or three empty ones.
    func add(_ exercise: Exercise, history: [WorkoutSession]) {
        let last = WorkoutStats.lastSets(of: exercise.id, in: history) ?? []
        let sets = last.isEmpty
            ? Array(repeating: SessionSet(reps: 10, weightKg: 0), count: 3)
            : last.map { SessionSet(reps: $0.reps, weightKg: $0.weightKg) }
        exercises.append(SessionExercise(exerciseId: exercise.id, name: exercise.name, sets: sets))
    }

    func addSet(to exerciseID: SessionExercise.ID) {
        guard let index = exercises.firstIndex(where: { $0.id == exerciseID }) else { return }
        let last = exercises[index].sets.last
        exercises[index].sets.append(SessionSet(reps: last?.reps ?? 10, weightKg: last?.weightKg ?? 0))
    }

    func toggle(_ setID: SessionSet.ID, of exerciseID: SessionExercise.ID) {
        guard let e = exercises.firstIndex(where: { $0.id == exerciseID }),
              let s = exercises[e].sets.firstIndex(where: { $0.id == setID }) else { return }
        exercises[e].sets[s].done.toggle()
        if exercises[e].sets[s].done {
            startRest(Self.restSeconds)
        }
    }

    // MARK: Rest

    func startRest(_ seconds: TimeInterval) {
        rest = Rest(until: .now.addingTimeInterval(seconds), total: seconds)
        restTask?.cancel()
        restTask = Task { [weak self] in
            try? await Task.sleep(for: .seconds(seconds))
            guard !Task.isCancelled, let self else { return }
            restsFinished += 1
            // the hint disappears a few seconds after the rest is over
            try? await Task.sleep(for: .seconds(5))
            guard !Task.isCancelled else { return }
            rest = nil
        }
    }

    func adjustRest(by seconds: TimeInterval) {
        guard let rest else { return }
        let left = max(0, rest.until.timeIntervalSinceNow + seconds)
        startRest(left)
        self.rest = Rest(until: .now.addingTimeInterval(left), total: max(15, rest.total + seconds))
    }

    func endRest() {
        restTask?.cancel()
        rest = nil
    }

    // MARK: Saving

    private func scheduleSave() {
        saveState = .pending
        saveTask?.cancel()
        saveTask = Task { [weak self] in
            try? await Task.sleep(for: Self.saveDelay)
            guard !Task.isCancelled else { return }
            await self?.save()
        }
    }

    func save() async {
        let (name, exercises) = (name, exercises)
        saveState = .saving
        do {
            try await service.update(session.id, name: name, exercises: exercises)
            // only "saved" when nothing changed in the meantime
            if name == self.name && exercises == self.exercises {
                saveState = .saved
            }
        } catch is CancellationError {
        } catch let error as URLError where error.code == .cancelled {
        } catch {
            saveState = .failed
        }
    }

    /// Saves right away, e.g. when the view is closed.
    func saveNow() {
        guard saveState == .pending else { return }
        saveTask?.cancel()
        Task { await save() }
    }

    func finish() async throws {
        // the finish request carries the latest state, a save racing it would be rejected anyway
        saveTask?.cancel()
        try await service.finish(session.id, name: name, exercises: exercises)
        restTask?.cancel()
    }

    func discard() async throws {
        saveTask?.cancel()
        try await service.delete(session.id)
        restTask?.cancel()
    }
}
