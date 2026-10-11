import HealthKit
import SwiftUI
import WatchKit

@main
struct LabsWatchApp: App {
    @WKApplicationDelegateAdaptor private var appDelegate: WatchAppDelegate

    var body: some Scene {
        WindowGroup {
            WatchRootView()
        }
    }
}

final class WatchAppDelegate: NSObject, WKApplicationDelegate {
    func applicationDidFinishLaunching() {
        WatchWorkoutModel.shared.activate()
        #if DEBUG
        if ProcessInfo.processInfo.arguments.contains("-demoWorkout") {
            WatchWorkoutModel.shared.showDemo()
        }
        #endif
    }

    /// The iPhone opens the app this way when a training is started there.
    func handle(_ workoutConfiguration: HKWorkoutConfiguration) {
        Task { await WatchWorkoutModel.shared.workout.start() }
        WatchWorkoutModel.shared.refresh()
    }
}
