import Foundation
import Observation

/// A running workout, every change is saved shortly after it was made.
@Observable
final class WorkoutSessionModel {
    enum SaveState {
        case saved, pending, saving, failed
    }

    struct Rest: Codable {
        let until: Date
        let total: TimeInterval
    }

    private static let saveDelay: Duration = .milliseconds(700)

    let session: WorkoutSession
    var name: String {
        didSet { scheduleSave() }
    }
    var exercises: [SessionExercise] {
        didSet { scheduleSave() }
    }
    private(set) var saveState = SaveState.saved
    /// stored per workout, so it survives closing the workout or the app
    private(set) var rest: Rest? {
        didSet { storeRest() }
    }
    /// counts up when a rest is over, for the haptic feedback
    private(set) var restsFinished = 0
    /// set once the workout was finished or discarded, also when that happened on the watch
    private(set) var isClosed = false

    /// called after every change, the watch bridge mirrors the workout with it
    @ObservationIgnored var onChange: (() -> Void)?

    @ObservationIgnored private let service: WorkoutService
    @ObservationIgnored private var saveTask: Task<Void, Never>?
    @ObservationIgnored private var restTask: Task<Void, Never>?

    init(session: WorkoutSession, service: WorkoutService) {
        self.session = session
        self.service = service
        self.name = session.name
        self.exercises = session.exercises
        restoreRest()
    }

    var allSets: [SessionSet] {
        exercises.flatMap(\.sets)
    }

    var doneSets: [SessionSet] {
        allSets.filter(\.done)
    }

    // MARK: Editing

    /// Starts with the sets and the note of last time, or three empty sets. Cardio starts with the minutes of last time.
    func add(_ exercise: Exercise, history: [WorkoutSession]) {
        let note = WorkoutStats.lastNote(of: exercise.id, in: history)
        if exercise.isCardio {
            let minutes = WorkoutStats.lastCardioMinutes(of: exercise.id, in: history) ?? WorkoutStats.defaultCardioMinutes
            exercises.append(SessionExercise(exerciseId: exercise.id, name: exercise.name, note: note, sets: [],
                                             cardio: SessionCardio(minutes: minutes, done: false)))
            return
        }
        let last = WorkoutStats.lastSets(of: exercise.id, in: history) ?? []
        // every set needs its own id, the same set repeated linked their weight fields and check marks
        let sets = last.isEmpty
            ? (0..<3).map { _ in SessionSet(reps: 10, weightKg: 0) }
            : last.map { SessionSet(reps: $0.reps, weightKg: $0.weightKg, restSeconds: $0.rest) }
        exercises.append(SessionExercise(exerciseId: exercise.id, name: exercise.name, note: note, sets: sets))
    }

    func addSet(to exerciseID: SessionExercise.ID) {
        // cardio has minutes, no sets
        guard let index = exercises.firstIndex(where: { $0.id == exerciseID }), exercises[index].cardio == nil else { return }
        let last = exercises[index].sets.last
        exercises[index].sets.append(SessionSet(reps: last?.reps ?? 10, weightKg: last?.weightKg ?? 0, restSeconds: last?.rest ?? WorkoutStats.defaultRest))
    }

    func toggle(_ setID: SessionSet.ID, of exerciseID: SessionExercise.ID) {
        guard let e = exercises.firstIndex(where: { $0.id == exerciseID }),
              let s = exercises[e].sets.firstIndex(where: { $0.id == setID }) else { return }
        exercises[e].sets[s].done.toggle()
        // checking a set off starts its rest
        let set = exercises[e].sets[s]
        if set.done && set.rest > 0 {
            startRest(TimeInterval(set.rest))
        }
    }

    // MARK: Rest

    func startRest(_ seconds: TimeInterval) {
        rest = Rest(until: .now.addingTimeInterval(seconds), total: seconds)
        onChange?()
        watchRest()
    }

    private func watchRest() {
        guard let until = rest?.until else { return }
        restTask?.cancel()
        restTask = Task { [weak self] in
            let left = until.timeIntervalSinceNow
            if left > 0 {
                try? await Task.sleep(for: .seconds(left))
                guard !Task.isCancelled, let self else { return }
                restsFinished += 1
            }
            // the hint disappears a few seconds after the rest is over
            try? await Task.sleep(for: .seconds(max(0, 5 + min(0, left))))
            guard !Task.isCancelled, let self else { return }
            rest = nil
            onChange?()
        }
    }

    private var restKey: String {
        "workout-rest-\(session.id)"
    }

    private func storeRest() {
        if let rest, let data = try? JSONEncoder().encode(rest) {
            UserDefaults.standard.set(data, forKey: restKey)
        } else {
            UserDefaults.standard.removeObject(forKey: restKey)
        }
    }

    /// a rest that is long over isn't worth showing anymore
    private func restoreRest() {
        guard let data = UserDefaults.standard.data(forKey: restKey),
              let stored = try? JSONDecoder().decode(Rest.self, from: data),
              stored.until.timeIntervalSinceNow > -5 else {
            UserDefaults.standard.removeObject(forKey: restKey)
            return
        }
        rest = stored
        watchRest()
    }

    func adjustRest(by seconds: TimeInterval) {
        guard let rest else { return }
        let left = max(0, rest.until.timeIntervalSinceNow + seconds)
        startRest(left)
        self.rest = Rest(until: .now.addingTimeInterval(left), total: max(15, rest.total + seconds))
        onChange?()
    }

    func endRest() {
        restTask?.cancel()
        rest = nil
        onChange?()
    }

    // MARK: Saving

    private func scheduleSave() {
        onChange?()
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

    /// Saves a pending change and waits for it, e.g. while the app only runs in the background for the watch.
    func flush() async {
        guard saveState == .pending else { return }
        saveTask?.cancel()
        await save()
    }

    func finish() async throws {
        // the finish request carries the latest state, a save racing it would be rejected anyway
        saveTask?.cancel()
        try await service.finish(session.id, name: name, exercises: exercises)
        let (session, name, exercises, service) = (session, name, exercises, service)
        Task { await AppNotifications.workoutFinished(session, name: name, exercises: exercises, service: service) }
        endRest()
        isClosed = true
        onChange?()
    }

    func discard() async throws {
        saveTask?.cancel()
        try await service.delete(session.id)
        endRest()
        isClosed = true
        onChange?()
    }
}
