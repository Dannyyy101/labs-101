import Foundation
import Observation
import WatchConnectivity
import WatchKit

/// The running workout on the watch, a copy of the one on the iPhone.
///
/// Changes are applied right away and sent to the iPhone, which answers with the
/// state it saved. While answers are outstanding, states pushed by the iPhone are
/// ignored, so a quick second tap isn't undone by the answer to the first one.
@Observable
final class WatchWorkoutModel: NSObject {
    static let shared = WatchWorkoutModel()

    nonisolated struct Summary: Equatable {
        let name: String
        let duration: TimeInterval
        let sets: Int
        let volume: Double
        let energy: Double
    }

    private(set) var state: WatchWorkoutState?
    /// the workout that was just finished, until the user closes the summary
    private(set) var summary: Summary?
    private(set) var catalog: [WatchCatalogEntry]?
    private(set) var isPhoneReachable = false
    /// while the rest after a set runs
    private(set) var isResting = false
    var error: String?

    /// heart rate, calories and the workout in Health
    let workout = WorkoutManager()

    @ObservationIgnored private var pendingReplies = 0
    @ObservationIgnored private var pendingUpdate: (exercise: UUID, set: UUID)?
    @ObservationIgnored private var updateTask: Task<Void, Never>?
    @ObservationIgnored private var restAlarm: Task<Void, Never>?

    private var session: WCSession {
        .default
    }

    func activate() {
        session.delegate = self
        session.activate()
    }

    // MARK: Actions

    func send(_ action: WatchAction) {
        flushUpdate()
        state?.apply(action)
        scheduleRestAlarm()

        switch action {
        case .finish, .discard:
            close(save: action == .finish)
        default:
            break
        }
        transmit(action)
    }

    /// Weight and reps change with every step of the Digital Crown, so they are sent once it rests.
    func update(exercise: UUID, set: UUID, reps: Int, weightKg: Double) {
        state?.apply(.update(exercise: exercise, set: set, reps: reps, weightKg: weightKg))
        if let pendingUpdate, pendingUpdate.exercise != exercise || pendingUpdate.set != set {
            flushUpdate()
        }
        pendingUpdate = (exercise, set)
        updateTask?.cancel()
        updateTask = Task { [weak self] in
            try? await Task.sleep(for: .milliseconds(600))
            guard !Task.isCancelled else { return }
            self?.flushUpdate()
        }
    }

    private func flushUpdate() {
        updateTask?.cancel()
        guard let pending = pendingUpdate else { return }
        pendingUpdate = nil
        guard let set = state?.exercises.first(where: { $0.id == pending.exercise })?.sets.first(where: { $0.id == pending.set }) else { return }
        transmit(.update(exercise: pending.exercise, set: pending.set, reps: set.reps, weightKg: set.weightKg))
    }

    func refresh() {
        transmit(.refresh)
    }

    func loadCatalog() {
        guard session.activationState == .activated, session.isReachable else {
            error = String(localized: "Das iPhone ist nicht erreichbar")
            return
        }
        session.sendMessage([WatchMessage.catalog: true]) { reply in
            let incoming = Incoming(reply)
            Task { @MainActor in self.receive(incoming) }
        } errorHandler: { error in
            let message = error.localizedDescription
            Task { @MainActor in self.error = message }
        }
    }

    func closeSummary() {
        summary = nil
    }

    /// Sends right away when the iPhone is reachable, otherwise the action is queued and delivered later.
    private func transmit(_ action: WatchAction) {
        guard session.activationState == .activated, let data = try? WatchMessage.encoder.encode(action) else { return }
        let payload = [WatchMessage.action: data]
        guard session.isReachable else {
            if action != .refresh { session.transferUserInfo(payload) }
            return
        }
        pendingReplies += 1
        session.sendMessage(payload) { reply in
            let incoming = Incoming(reply)
            Task { @MainActor in
                self.pendingReplies -= 1
                self.receive(incoming, isReply: true)
            }
        } errorHandler: { _ in
            Task { @MainActor in
                self.pendingReplies -= 1
                self.session.transferUserInfo(payload)
            }
        }
    }

    // MARK: From the iPhone

    fileprivate func receive(_ incoming: Incoming?, isReply: Bool = false) {
        switch incoming {
        case .state(let state):
            // older than what is shown while changes are underway
            guard pendingReplies == 0 && pendingUpdate == nil else { return }
            self.state = state
            summary = nil
            scheduleRestAlarm()
            Task { await workout.start(at: state.startedAt) }
        case .ended(let id):
            if id == WatchMessage.noWorkout || id == state?.sessionID {
                close(save: state.map { !$0.doneSets.isEmpty } ?? false)
            }
        case .catalog(let catalog):
            self.catalog = catalog
        case .error(let message):
            error = message
            // what is shown may not have been saved
            if isReply { refresh() }
        case nil:
            break
        }
    }

    /// Ends the Health workout and shows the summary, `save` is false for a discarded workout.
    private func close(save: Bool) {
        if let state, save {
            summary = Summary(name: state.name, duration: Date.now.timeIntervalSince(state.startedAt),
                              sets: state.doneSets.count, volume: state.volume, energy: workout.activeEnergy)
        }
        state = nil
        restAlarm?.cancel()
        isResting = false
        Task { await workout.end(save: save) }
    }

    /// A tap on the wrist when the rest is over.
    private func scheduleRestAlarm() {
        restAlarm?.cancel()
        guard let until = state?.restUntil, until > .now else {
            isResting = false
            return
        }
        isResting = true
        restAlarm = Task {
            try? await Task.sleep(for: .seconds(until.timeIntervalSinceNow))
            guard !Task.isCancelled else { return }
            isResting = false
            WKInterfaceDevice.current().play(.stop)
        }
    }
}

#if DEBUG
extension WatchWorkoutModel {
    /// A made-up training for the simulator and previews, start the app with `-demoWorkout`.
    func showDemo() {
        func set(_ reps: Int, _ kg: Double, done: Bool = false) -> WatchWorkoutState.WatchSet {
            WatchWorkoutState.WatchSet(id: UUID(), reps: reps, weightKg: kg, done: done, rest: 90)
        }
        state = WatchWorkoutState(
            sessionID: 0, name: "Push", startedAt: .now.addingTimeInterval(-1_520),
            exercises: [
                .init(id: UUID(), name: "Bankdrücken", sets: [set(8, 70, done: true), set(8, 70), set(8, 70)],
                      previous: [set(8, 67.5, done: true), set(8, 67.5, done: true), set(7, 67.5, done: true)]),
                .init(id: UUID(), name: "Schulterdrücken", sets: [set(10, 20), set(10, 20)], previous: nil),
            ],
            restUntil: nil, restTotal: nil)
    }
}
#endif

// MARK: WCSessionDelegate

/// A message of the iPhone, decoded before leaving the delegate queue.
private nonisolated enum Incoming: Sendable {
    case state(WatchWorkoutState)
    case ended(Int)
    case catalog([WatchCatalogEntry])
    case error(String)

    init?(_ message: [String: Any]) {
        let decoder = WatchMessage.decoder
        if let data = message[WatchMessage.state] as? Data, let state = try? decoder.decode(WatchWorkoutState.self, from: data) {
            self = .state(state)
        } else if let id = message[WatchMessage.ended] as? Int {
            self = .ended(id)
        } else if let data = message[WatchMessage.catalog] as? Data, let catalog = try? decoder.decode([WatchCatalogEntry].self, from: data) {
            self = .catalog(catalog)
        } else if let error = message[WatchMessage.error] as? String {
            self = .error(error)
        } else {
            return nil
        }
    }
}

extension WatchWorkoutModel: WCSessionDelegate {
    nonisolated func session(_ session: WCSession, activationDidCompleteWith activationState: WCSessionActivationState, error: Error?) {
        // the last state the iPhone sent while the app wasn't running
        let incoming = Incoming(session.receivedApplicationContext)
        let reachable = session.isReachable
        Task { @MainActor in
            self.isPhoneReachable = reachable
            self.receive(incoming)
            self.refresh()
        }
    }

    nonisolated func sessionReachabilityDidChange(_ session: WCSession) {
        let reachable = session.isReachable
        Task { @MainActor in
            self.isPhoneReachable = reachable
            if reachable { self.refresh() }
        }
    }

    nonisolated func session(_ session: WCSession, didReceiveApplicationContext applicationContext: [String: Any]) {
        let incoming = Incoming(applicationContext)
        Task { @MainActor in self.receive(incoming) }
    }

    nonisolated func session(_ session: WCSession, didReceiveMessage message: [String: Any]) {
        let incoming = Incoming(message)
        Task { @MainActor in self.receive(incoming) }
    }
}
