import Foundation

/// Settings from `Config/Config.xcconfig`, which end up in the Info.plist at build time.
nonisolated struct AppConfig: Sendable {
    let apiBaseURL: URL
    /// Zitadel, e.g. https://auth.project101.tech
    let authIssuer: URL?
    /// Client id of the native app in Zitadel
    let authClientID: String

    static let current = AppConfig(bundle: .main)

    init(apiBaseURL: URL, authIssuer: URL?, authClientID: String) {
        self.apiBaseURL = apiBaseURL
        self.authIssuer = authIssuer
        self.authClientID = authClientID
    }

    init(bundle: Bundle) {
        func value(_ key: String) -> String {
            (bundle.object(forInfoDictionaryKey: key) as? String)?.trimmingCharacters(in: .whitespaces) ?? ""
        }
        let issuer = value("AuthIssuer")
        self.init(
            apiBaseURL: URL(string: value("APIBaseURL")) ?? URL(string: "http://localhost:8080/api")!,
            authIssuer: issuer.isEmpty ? nil : URL(string: issuer),
            authClientID: value("AuthClientID")
        )
    }

    /// Without issuer and client id nobody can sign in.
    var isComplete: Bool {
        authIssuer != nil && !authClientID.isEmpty
    }
}
