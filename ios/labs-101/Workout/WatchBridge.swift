import Foundation
import HealthKit
import Observation
import UIKit
import WatchConnectivity

/// Mirrors the running workout to the Apple Watch and applies the changes made there.
///
/// The iPhone stays the owner of the workout, the watch only shows a copy and sends
/// `WatchAction`s. Those can arrive while the app is in the background or not running
/// at all, then the running workout is loaded from the backend first.
@Observable
final class WatchBridge: NSObject {
    static let shared = WatchBridge()

    /// the running workout, shared by `WorkoutSessionView` and the watch
    private(set) var model: WorkoutSessionModel?
    /// counts up when the watch finished or discarded a workout, so the overview reloads
    private(set) var workoutsEnded = 0

    @ObservationIgnored private var exercises: [Exercise] = []
    @ObservationIgnored private var history: [WorkoutSession] = []
    @ObservationIgnored private let service = WorkoutService()
    @ObservationIgnored private let healthStore = HKHealthStore()
    @ObservationIgnored private var pushTask: Task<Void, Never>?

    private var session: WCSession? {
        WCSession.isSupported() ? .default : nil
    }

    func activate() {
        guard let session else { return }
        session.delegate = self
        session.activate()
    }

    /// The exercises and history of the overview, for adding exercises and the "last time" hints.
    func use(exercises: [Exercise], history: [WorkoutSession]) {
        self.exercises = exercises
        self.history = history
    }

    /// The model of the workout, the same one the watch changes.
    func model(for workout: WorkoutSession, service: WorkoutService) -> WorkoutSessionModel {
        if let model, model.session.id == workout.id, !model.isClosed {
            return model
        }
        let model = WorkoutSessionModel(session: workout, service: service)
        model.onChange = { [weak self] in self?.schedulePush() }
        self.model = model
        push()
        return model
    }

    /// Opens the watch app, which starts a workout session there (heart rate, calories) and shows the sets.
    func startWatchApp() {
        guard let session, session.isPaired, session.isWatchAppInstalled else { return }
        let configuration = HKWorkoutConfiguration()
        configuration.activityType = .traditionalStrengthTraining
        configuration.locationType = .indoor
        healthStore.startWatchApp(with: configuration) { _, _ in }
    }

    // MARK: To the watch

    /// typing a weight changes the workout with every key, so the pushes are bundled
    private func schedulePush() {
        pushTask?.cancel()
        pushTask = Task { [weak self] in
            try? await Task.sleep(for: .milliseconds(250))
            guard !Task.isCancelled else { return }
            self?.push()
        }
    }

    private func push() {
        guard let model else { return }
        if model.isClosed {
            self.model = nil
            send([WatchMessage.ended: model.session.id])
        } else if let data = try? WatchMessage.encoder.encode(state(of: model)) {
            send([WatchMessage.state: data])
        }
    }

    /// The application context reaches the watch also when it isn't running right now.
    private func send(_ message: [String: Any]) {
        guard let session, session.activationState == .activated, session.isWatchAppInstalled else { return }
        try? session.updateApplicationContext(message)
        if session.isReachable {
            session.sendMessage(message, replyHandler: nil)
        }
    }

    private func state(of model: WorkoutSessionModel) -> WatchWorkoutState {
        WatchWorkoutState(
            sessionID: model.session.id,
            name: model.name,
            startedAt: model.session.startedAt,
            exercises: model.exercises.map { item in
                WatchWorkoutState.Exercise(
                    id: item.id, name: item.name, sets: item.sets.map(Self.watchSet),
                    previous: WorkoutStats.lastSets(of: item.exerciseId, in: history)?.map(Self.watchSet))
            },
            restUntil: model.rest?.until,
            restTotal: model.rest?.total)
    }

    private nonisolated static func watchSet(_ set: SessionSet) -> WatchWorkoutState.WatchSet {
        WatchWorkoutState.WatchSet(id: set.id, reps: set.reps, weightKg: set.weightKg, done: set.done, rest: set.rest)
    }

    // MARK: From the watch

    private func reply(to action: WatchAction) async -> [String: Any] {
        // the app may only run in the background for this message
        let backgroundTask = UIApplication.shared.beginBackgroundTask(withName: "watch-action")
        defer { UIApplication.shared.endBackgroundTask(backgroundTask) }

        let model: WorkoutSessionModel
        do {
            guard let current = try await currentModel() else {
                return [WatchMessage.ended: WatchMessage.noWorkout]
            }
            model = current
        } catch {
            return [WatchMessage.error: error.localizedDescription]
        }

        do {
            try await apply(action, to: model)
        } catch {
            return [WatchMessage.error: error.localizedDescription]
        }

        if model.isClosed {
            workoutsEnded += 1
            return [WatchMessage.ended: model.session.id]
        }
        await model.flush()
        guard let data = try? WatchMessage.encoder.encode(state(of: model)) else { return [:] }
        return [WatchMessage.state: data]
    }

    private func apply(_ action: WatchAction, to model: WorkoutSessionModel) async throws {
        switch action {
        case .toggle(let exerciseID, let setID):
            model.toggle(setID, of: exerciseID)
        case .update(let exerciseID, let setID, let reps, let weightKg):
            guard let e = model.exercises.firstIndex(where: { $0.id == exerciseID }),
                  let s = model.exercises[e].sets.firstIndex(where: { $0.id == setID }) else { return }
            var set = model.exercises[e].sets[s]
            set.reps = max(0, reps)
            set.weightKg = max(0, weightKg)
            model.exercises[e].sets[s] = set
        case .addSet(let exerciseID):
            model.addSet(to: exerciseID)
        case .addExercise(let id):
            if let exercise = try await catalog().first(where: { $0.id == id }) {
                model.add(exercise, history: history)
            }
        case .adjustRest(let seconds):
            model.adjustRest(by: seconds)
        case .endRest:
            model.endRest()
        case .finish:
            try await model.finish()
        case .discard:
            try await model.discard()
        case .refresh:
            break
        }
    }

    /// The open workout, loaded from the backend when the app was started by the watch.
    private func currentModel() async throws -> WorkoutSessionModel? {
        if let model, !model.isClosed {
            return model
        }
        guard let active = try await service.activeSession() else { return nil }
        if history.isEmpty {
            let now = Date.now
            history = (try? await service.sessions(from: now.addingTimeInterval(-90 * 86_400), to: now.addingTimeInterval(86_400))) ?? []
        }
        return model(for: active, service: service)
    }

    private func catalog() async throws -> [Exercise] {
        if exercises.isEmpty {
            exercises = try await service.exercises()
        }
        return exercises
    }

    private func catalogReply() async -> [String: Any] {
        do {
            let recent = Set(history.flatMap { $0.exercises.compactMap(\.exerciseId) })
            let entries = try await catalog()
                .map { WatchCatalogEntry(id: $0.id, name: $0.name, recent: recent.contains($0.id)) }
                .sorted { a, b in
                    a.recent != b.recent ? a.recent : a.name.localizedStandardCompare(b.name) == .orderedAscending
                }
            return [WatchMessage.catalog: try WatchMessage.encoder.encode(entries)]
        } catch {
            return [WatchMessage.error: error.localizedDescription]
        }
    }

    private func handle(_ message: Message) async -> [String: Any] {
        switch message {
        case .action(let action): await reply(to: action)
        case .catalog: await catalogReply()
        }
    }
}

// MARK: WCSessionDelegate

extension WatchBridge: WCSessionDelegate {
    /// What the watch asked for, taken out of the dictionary before leaving the delegate queue.
    fileprivate nonisolated enum Message: Sendable {
        case action(WatchAction)
        case catalog

        init?(_ message: [String: Any]) {
            if message[WatchMessage.catalog] != nil {
                self = .catalog
            } else if let data = message[WatchMessage.action] as? Data,
                      let action = try? WatchMessage.decoder.decode(WatchAction.self, from: data) {
                self = .action(action)
            } else {
                return nil
            }
        }
    }

    nonisolated func session(_ session: WCSession, activationDidCompleteWith activationState: WCSessionActivationState, error: Error?) {
        Task { @MainActor in self.push() }
    }

    nonisolated func sessionReachabilityDidChange(_ session: WCSession) {
        guard session.isReachable else { return }
        Task { @MainActor in self.push() }
    }

    nonisolated func session(_ session: WCSession, didReceiveMessage message: [String: Any], replyHandler: @escaping ([String: Any]) -> Void) {
        let reply = UncheckedReply(send: replyHandler)
        guard let message = Message(message) else {
            reply.send([:])
            return
        }
        Task { @MainActor in
            reply.send(await self.handle(message))
        }
    }

    /// actions the watch queued while the iPhone wasn't reachable
    nonisolated func session(_ session: WCSession, didReceiveUserInfo userInfo: [String: Any] = [:]) {
        guard let message = Message(userInfo) else { return }
        Task { @MainActor in
            _ = await self.handle(message)
            self.push()
        }
    }

    nonisolated func sessionDidBecomeInactive(_ session: WCSession) {}

    /// after switching to another watch
    nonisolated func sessionDidDeactivate(_ session: WCSession) {
        session.activate()
    }
}

/// WatchConnectivity hands out a reply handler that isn't marked Sendable, it may be called from any thread.
private nonisolated struct UncheckedReply: @unchecked Sendable {
    let send: ([String: Any]) -> Void
}
