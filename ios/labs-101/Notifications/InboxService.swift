import Foundation
import LabsAPI

/// A notification of the backend, the bell in the web, see NotificationController.
nonisolated struct InboxNotification: Identifiable, Sendable {
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
        try await client.call {
            try await $0.notificationGetNotifications(query: .init(unreadOnly: true, limit: Int32(limit))).ok.body.json
        }.map(InboxNotification.init)
    }

    func markRead(_ id: Int) async throws {
        _ = try await client.call { try await $0.notificationMarkRead(path: .init(id: Int64(id))).ok }
    }

    func create(title: String, message: String?, type: String = "INFO") async throws {
        let body = Components.Schemas.CreateNotificationDto(message: message, title: title, _type: .init(rawValue: type))
        _ = try await client.call { try await $0.notificationCreateNotification(body: .json(body)).created }
    }
}

nonisolated extension InboxNotification {
    init(_ dto: Components.Schemas.NotificationDto) {
        self.init(id: Int(dto.id), type: dto._type.rawValue, title: dto.title, message: dto.message, link: dto.link,
                  createdAt: dto.createdAt, readAt: dto.readAt)
    }
}
