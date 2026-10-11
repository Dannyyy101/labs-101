import Foundation
import HealthKit
import Observation

/// The strength training in Health: keeps the app in front during the workout,
/// measures heart rate and calories and saves the workout at the end.
@Observable
final class WorkoutManager: NSObject {
    private(set) var isRunning = false
    private(set) var heartRate: Double?
    /// active kcal since the start
    private(set) var activeEnergy: Double = 0

    @ObservationIgnored private let store = HKHealthStore()
    @ObservationIgnored private var session: HKWorkoutSession?
    @ObservationIgnored private var builder: HKLiveWorkoutBuilder?
    /// the iPhone and the app delegate may both start the workout at the same time
    @ObservationIgnored private var isStarting = false

    /// Off while testing: the workout session still runs (heart rate, calories, app stays in front),
    /// but nothing is written to Health and write access isn't even requested.
    static let savesToHealth = false

    static let configuration: HKWorkoutConfiguration = {
        let configuration = HKWorkoutConfiguration()
        configuration.activityType = .traditionalStrengthTraining
        configuration.locationType = .indoor
        return configuration
    }()

    /// `startDate` is the start on the iPhone, so the workout in Health covers the whole training.
    func start(at startDate: Date = .now) async {
        guard !isRunning, !isStarting, HKHealthStore.isHealthDataAvailable() else { return }
        isStarting = true
        defer { isStarting = false }

        try? await store.requestAuthorization(toShare: Self.savesToHealth ? [HKObjectType.workoutType()] : [],
                                              read: [HKQuantityType(.heartRate), HKQuantityType(.activeEnergyBurned)])
        do {
            let session = try HKWorkoutSession(healthStore: store, configuration: Self.configuration)
            let builder = session.associatedWorkoutBuilder()
            builder.dataSource = HKLiveWorkoutDataSource(healthStore: store, workoutConfiguration: Self.configuration)
            session.delegate = self
            builder.delegate = self
            self.session = session
            self.builder = builder

            let start = min(startDate, .now)
            session.startActivity(with: start)
            try await builder.beginCollection(at: start)
            isRunning = true
        } catch {
            session?.end()
            reset()
        }
    }

    /// Saves the workout to Health, or drops it when the training was discarded.
    func end(save: Bool) async {
        guard let session, let builder else { return }
        session.end()
        do {
            try await builder.endCollection(at: .now)
            if save && Self.savesToHealth {
                _ = try await builder.finishWorkout()
            } else {
                builder.discardWorkout()
            }
        } catch {
            builder.discardWorkout()
        }
        reset()
    }

    private func reset() {
        session = nil
        builder = nil
        isRunning = false
        heartRate = nil
        activeEnergy = 0
    }
}

extension WorkoutManager: HKWorkoutSessionDelegate, HKLiveWorkoutBuilderDelegate {
    nonisolated func workoutSession(_ workoutSession: HKWorkoutSession, didChangeTo toState: HKWorkoutSessionState,
                                    from fromState: HKWorkoutSessionState, date: Date) {}

    nonisolated func workoutSession(_ workoutSession: HKWorkoutSession, didFailWithError error: Error) {}

    nonisolated func workoutBuilder(_ workoutBuilder: HKLiveWorkoutBuilder, didCollectDataOf collectedTypes: Set<HKSampleType>) {
        let bpm = workoutBuilder.statistics(for: HKQuantityType(.heartRate))?
            .mostRecentQuantity()?.doubleValue(for: .count().unitDivided(by: .minute()))
        let kcal = workoutBuilder.statistics(for: HKQuantityType(.activeEnergyBurned))?
            .sumQuantity()?.doubleValue(for: .kilocalorie())
        Task { @MainActor in
            if let bpm { self.heartRate = bpm }
            if let kcal { self.activeEnergy = kcal }
        }
    }

    nonisolated func workoutBuilderDidCollectEvent(_ workoutBuilder: HKLiveWorkoutBuilder) {}
}
