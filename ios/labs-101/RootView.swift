import SwiftUI

struct RootView: View {
    @State private var auth = AuthState.shared

    var body: some View {
        if !AppConfig.current.isComplete {
            MissingConfigurationView()
        } else if !auth.isSignedIn {
            SignInView()
        } else {
            TabView {
                Tab("Food", systemImage: "fork.knife") {
                    FoodTrackerView()
                }
                Tab("Training", systemImage: "dumbbell.fill") {
                    WorkoutsView()
                }
                Tab("Health", systemImage: "heart.fill") {
                    HealthSyncView()
                }
            }
        }
    }
}

/// Shown until issuer and client id are set in Config/Secrets.xcconfig.
private struct MissingConfigurationView: View {
    var body: some View {
        ContentUnavailableView(
            "Nicht konfiguriert",
            systemImage: "key.slash",
            description: Text("Trage AUTH_ISSUER und AUTH_CLIENT_ID in ios/Config/Secrets.xcconfig ein und baue die App neu.")
        )
    }
}

#Preview {
    RootView()
}
