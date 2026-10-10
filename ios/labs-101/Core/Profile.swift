import Foundation
import SwiftUI

/// The signed in user, the backend asks Zitadel for it (see UserController). Name and picture are changed in Zitadel.
nonisolated struct UserProfile: Codable, Hashable, Sendable {
    let id: String
    let name: String?
    let email: String?
    let image: URL?

    /// "M" for the placeholder without a picture
    var initial: String {
        (name?.trimmingCharacters(in: .whitespaces).first).map { String($0).uppercased() } ?? "?"
    }
}

nonisolated struct ProfileService: Sendable {
    private let client: APIClient

    init(config: AppConfig = .current, session: URLSession = .shared) {
        self.client = APIClient(config: config, session: session)
    }

    func profile() async throws -> UserProfile {
        try await client.get("users/me")
    }

    /// The own profile in the Zitadel console, where name and picture are changed.
    static func editURL(config: AppConfig = .current) -> URL? {
        config.authIssuer?.appending(path: "ui/console/users/me")
    }
}

/// Round profile picture, the first letter of the name without one.
struct ProfileImage: View {
    let profile: UserProfile?
    var size: CGFloat = 44

    var body: some View {
        AsyncImage(url: profile?.image) { image in
            image.resizable().scaledToFill()
        } placeholder: {
            Text(profile?.initial ?? "")
                .font(.system(size: size * 0.42, weight: .semibold))
                .foregroundStyle(.white)
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .background(Color.secondary)
        }
        .frame(width: size, height: size)
        .clipShape(.circle)
        .accessibilityHidden(true)
    }
}
