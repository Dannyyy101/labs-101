import AuthenticationServices
import SwiftUI

/// Sign in at Zitadel, with password, passkey or Google, whatever the login page offers.
struct SignInView: View {
    @Environment(\.webAuthenticationSession) private var webAuthenticationSession
    @State private var isSigningIn = false
    @State private var error: String?

    var body: some View {
        ContentUnavailableView {
            Label("labs-101", systemImage: "person.crop.circle")
        } description: {
            Text("Melde dich an, um Essen, Trainings und Health-Daten zu synchronisieren.")
            if let error {
                Text(error).foregroundStyle(.red)
            }
        } actions: {
            Button {
                Task { await signIn() }
            } label: {
                if isSigningIn {
                    ProgressView()
                } else {
                    Text("Anmelden")
                }
            }
            .buttonStyle(.borderedProminent)
            .disabled(isSigningIn)
        }
    }

    private func signIn() async {
        isSigningIn = true
        defer { isSigningIn = false }
        error = nil

        do {
            let pending = try await AuthSession.shared.startSignIn()
            // ephemeral: no cookies in safari, signing out in the app is enough
            let callback = try await webAuthenticationSession.authenticate(
                using: pending.url,
                callbackURLScheme: AuthSession.callbackScheme,
                preferredBrowserSession: .ephemeral
            )
            try await AuthSession.shared.completeSignIn(pending, callback: callback)
        } catch let error as ASWebAuthenticationSessionError where error.code == .canceledLogin {
            // closed by the user, nothing to show
        } catch {
            self.error = error.localizedDescription
        }
    }
}

#Preview {
    SignInView()
}
