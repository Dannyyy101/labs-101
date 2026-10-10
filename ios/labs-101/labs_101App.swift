//
//  labs_101App.swift
//  labs-101
//
//  Created by Daniel Stöcklein on 21.08.26.
//

import SwiftUI
import UIKit

@main
struct labs_101App: App {
    @UIApplicationDelegateAdaptor private var appDelegate: AppDelegate
    @Environment(\.scenePhase) private var scenePhase

    var body: some Scene {
        WindowGroup {
            RootView()
        }
        .onChange(of: scenePhase) { _, phase in
            guard AppConfig.current.isComplete, AuthSession.hasStoredTokens, HealthAuthorization.wasRequested else { return }
            switch phase {
            case .active:
                Task { await HealthSyncEngine.shared.sync() }
            case .background:
                HealthBackgroundSync.scheduleBackgroundTasks()
            default:
                break
            }
        }
    }
}

final class AppDelegate: NSObject, UIApplicationDelegate {
    func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil) -> Bool {
        // also when HealthKit launches the app in the background for new samples
        if AppConfig.current.isComplete {
            HealthBackgroundSync.registerAtLaunch()
        }
        return true
    }
}
