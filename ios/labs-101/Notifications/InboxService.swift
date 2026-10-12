import Foundation

/// A notification of the backend, the bell in the web, see NotificationController.
nonisolated struct InboxNotification: Decodable, Identifiable, Sendable {
    let id: Int
    /// INFO, SUCCESS, WARNING or ERROR
    let type: String
    let title: String
    let message: String?
    let link: String?
    let createdAt: Date
    let readAt: Date?
}

/// The notification endpoints of the backend, for the signed in user.
nonisolated struct InboxService: Sendable {
    private let client: APIClient

    init(config: AppConfig = .current, session: URLSession = .shared) {
        self.client = APIClient(config: config, session: session)
    }

    /// Newest first.
    func unread(limit: Int = 20) async throws -> [InboxNotification] {
        try await client.get("users/me/notifications", query: [
            URLQueryItem(name: "unreadOnly", value: "true"),
            URLQueryItem(name: "limit", value: String(limit)),
        ])
    }

    func markRead(_ id: Int) async throws {
        let _: InboxNotification = try await client.send("PUT", "users/me/notifications/\(id)/read")
    }

    func create(title: String, message: String?, type: String = "INFO") async throws {
        let _: InboxNotification = try await client.send("POST", "users/me/notifications",
                                                         body: ["type": type, "title": title, "message": message])
    }
}
