import Foundation

/// Settings from `Config/Config.xcconfig`, which end up in the Info.plist at build time.
nonisolated struct AppConfig: Sendable {
    let apiBaseURL: URL
    let apiKey: String
    let userID: String

    static let current = AppConfig(bundle: .main)

    init(apiBaseURL: URL, apiKey: String, userID: String) {
        self.apiBaseURL = apiBaseURL
        self.apiKey = apiKey
        self.userID = userID
    }

    init(bundle: Bundle) {
        func value(_ key: String) -> String {
            (bundle.object(forInfoDictionaryKey: key) as? String)?.trimmingCharacters(in: .whitespaces) ?? ""
        }
        self.init(
            apiBaseURL: URL(string: value("APIBaseURL")) ?? URL(string: "http://localhost:8080/api")!,
            apiKey: value("APIKey"),
            userID: value("UserID")
        )
    }

    /// The backend rejects every request without API key, and tracked food needs a user.
    var isComplete: Bool {
        !apiKey.isEmpty && !userID.isEmpty
    }
}
