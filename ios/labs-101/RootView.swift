import SwiftUI

struct RootView: View {
    var body: some View {
        TabView {
            Tab("Food", systemImage: "fork.knife") {
                if AppConfig.current.isComplete {
                    FoodTrackerView()
                } else {
                    MissingConfigurationView()
                }
            }
            Tab("Health", systemImage: "heart.fill") {
                if AppConfig.current.isComplete {
                    HealthSyncView()
                } else {
                    MissingConfigurationView()
                }
            }
        }
    }
}

/// Shown until API key and user are set in Config/Secrets.xcconfig.
private struct MissingConfigurationView: View {
    var body: some View {
        ContentUnavailableView(
            "Nicht konfiguriert",
            systemImage: "key.slash",
            description: Text("Trage API_KEY und USER_ID in ios/Config/Secrets.xcconfig ein und baue die App neu.")
        )
    }
}

#Preview {
    RootView()
}
