import CryptoKit
import Foundation

nonisolated enum AuthError: LocalizedError {
    case notConfigured
    case notSignedIn
    case invalidCallback
    case server(String)

    var errorDescription: String? {
        switch self {
        case .notConfigured:
            String(localized: "AUTH_ISSUER und AUTH_CLIENT_ID fehlen in der Konfiguration")
        case .notSignedIn:
            String(localized: "Du bist nicht angemeldet")
        case .invalidCallback:
            String(localized: "Die Anmeldung wurde abgebrochen")
        case .server(let message):
            message
        }
    }
}

/// Tokens of the signed in user, kept in the keychain.
nonisolated struct AuthTokens: Codable, Sendable {
    var accessToken: String
    var refreshToken: String?
    var expiresAt: Date
}

/// Sign in at Zitadel with the authorization code flow and PKCE, the app is a public client without secret.
/// Hands out access tokens for the backend and refreshes them before they run out.
actor AuthSession {
    static let shared = AuthSession(config: .current)

    static let callbackScheme = "labs101"
    static let redirectURI = "labs101://auth/callback"

    private static let tokensKey = "tokens"
    // refresh a minute early, so the token doesn't run out on the way to the backend
    private static let refreshMargin: TimeInterval = 60

    private let config: AppConfig
    private let session: URLSession
    private var tokens: AuthTokens?
    private var endpoints: Endpoints?
    // concurrent requests share one refresh, zitadel rotates the refresh token
    private var refreshTask: Task<AuthTokens, Error>?

    init(config: AppConfig, session: URLSession = .shared) {
        self.config = config
        self.session = session
        self.tokens = Keychain.read(Self.tokensKey).flatMap { try? JSONDecoder().decode(AuthTokens.self, from: $0) }
    }

    /// Without network, only whether tokens are stored.
    nonisolated static var hasStoredTokens: Bool {
        Keychain.read(tokensKey) != nil
    }

    // MARK: Sign in and out

    /// What the app has to remember while the user is on the login page.
    struct PendingSignIn: Sendable {
        let url: URL
        fileprivate let verifier: String
        fileprivate let state: String
    }

    /// The login page to open with a `WebAuthenticationSession`, then pass the callback to `completeSignIn`.
    func startSignIn() async throws -> PendingSignIn {
        let endpoints = try await discover()
        let verifier = Self.randomString()
        let state = Self.randomString()

        var components = URLComponents(url: endpoints.authorization, resolvingAgainstBaseURL: false)!
        components.queryItems = [
            URLQueryItem(name: "client_id", value: config.authClientID),
            URLQueryItem(name: "redirect_uri", value: Self.redirectURI),
            URLQueryItem(name: "response_type", value: "code"),
            // offline_access for the refresh token
            URLQueryItem(name: "scope", value: "openid profile email offline_access"),
            URLQueryItem(name: "code_challenge", value: Self.codeChallenge(for: verifier)),
            URLQueryItem(name: "code_challenge_method", value: "S256"),
            URLQueryItem(name: "state", value: state),
        ]
        return PendingSignIn(url: components.url!, verifier: verifier, state: state)
    }

    /// Exchanges the code of the URL zitadel redirected to for tokens.
    func completeSignIn(_ pending: PendingSignIn, callback: URL) async throws {
        let items = URLComponents(url: callback, resolvingAgainstBaseURL: false)?.queryItems ?? []
        guard items.first(where: { $0.name == "state" })?.value == pending.state,
              let code = items.first(where: { $0.name == "code" })?.value else {
            throw AuthError.invalidCallback
        }

        store(try await requestTokens([
            "grant_type": "authorization_code",
            "code": code,
            "redirect_uri": Self.redirectURI,
            "code_verifier": pending.verifier,
        ], previous: nil))
    }

    /// The login runs in an ephemeral browser session, so there is no session at zitadel to end.
    func signOut() async {
        refreshTask?.cancel()
        refreshTask = nil
        store(nil)
    }

    // MARK: Tokens

    /// A valid access token, refreshed if needed. Throws `notSignedIn` if the refresh token was rejected.
    func accessToken(forceRefresh: Bool = false) async throws -> String {
        guard let tokens else { throw AuthError.notSignedIn }
        if !forceRefresh && tokens.expiresAt.timeIntervalSinceNow > Self.refreshMargin {
            return tokens.accessToken
        }
        return try await refresh(tokens).accessToken
    }

    private func refresh(_ current: AuthTokens) async throws -> AuthTokens {
        if let refreshTask {
            return try await refreshTask.value
        }
        guard let refreshToken = current.refreshToken else {
            store(nil)
            throw AuthError.notSignedIn
        }

        let task = Task {
            try await requestTokens(["grant_type": "refresh_token", "refresh_token": refreshToken], previous: current)
        }
        refreshTask = task
        defer { refreshTask = nil }

        do {
            let refreshed = try await task.value
            store(refreshed)
            return refreshed
        } catch AuthError.server(let message) {
            // the refresh token expired or was revoked, the user has to sign in again
            store(nil)
            throw AuthError.server(message)
        }
    }

    private func store(_ tokens: AuthTokens?) {
        self.tokens = tokens
        if let tokens, let data = try? JSONEncoder().encode(tokens) {
            Keychain.write(data, for: Self.tokensKey)
        } else {
            Keychain.delete(Self.tokensKey)
        }
        let signedIn = tokens != nil
        Task { @MainActor in AuthState.shared.isSignedIn = signedIn }
    }

    // MARK: Requests

    private struct Endpoints: Decodable {
        let authorization: URL
        let token: URL

        enum CodingKeys: String, CodingKey {
            case authorization = "authorization_endpoint"
            case token = "token_endpoint"
        }
    }

    private func discover() async throws -> Endpoints {
        if let endpoints { return endpoints }
        guard let issuer = config.authIssuer, !config.authClientID.isEmpty else { throw AuthError.notConfigured }

        let url = issuer.appending(path: ".well-known/openid-configuration")
        let (data, _) = try await session.data(from: url)
        let discovered = try JSONDecoder().decode(Endpoints.self, from: data)
        endpoints = discovered
        return discovered
    }

    private func requestTokens(_ parameters: [String: String], previous: AuthTokens?) async throws -> AuthTokens {
        struct Response: Decodable {
            let access_token: String
            let refresh_token: String?
            let expires_in: Double?
        }
        struct ErrorResponse: Decodable {
            let error: String
            let error_description: String?
        }

        var request = URLRequest(url: try await discover().token)
        request.httpMethod = "POST"
        request.setValue("application/x-www-form-urlencoded", forHTTPHeaderField: "Content-Type")
        request.httpBody = Self.formEncoded(parameters.merging(["client_id": config.authClientID]) { $1 })

        let (data, response) = try await session.data(for: request)
        guard let http = response as? HTTPURLResponse, (200..<300).contains(http.statusCode) else {
            let error = try? JSONDecoder().decode(ErrorResponse.self, from: data)
            throw AuthError.server(error?.error_description ?? error?.error ?? String(localized: "Anmeldung fehlgeschlagen"))
        }

        let tokens = try JSONDecoder().decode(Response.self, from: data)
        return AuthTokens(
            accessToken: tokens.access_token,
            // keep the old one if zitadel sent none
            refreshToken: tokens.refresh_token ?? previous?.refreshToken,
            expiresAt: Date().addingTimeInterval(tokens.expires_in ?? 300)
        )
    }

    private nonisolated static func formEncoded(_ parameters: [String: String]) -> Data {
        // unreserved characters only, "+", "&" and "=" have a meaning in a form body
        let allowed = CharacterSet.alphanumerics.union(CharacterSet(charactersIn: "-._~"))
        func encode(_ value: String) -> String {
            value.addingPercentEncoding(withAllowedCharacters: allowed) ?? value
        }
        return Data(parameters.map { "\(encode($0.key))=\(encode($0.value))" }.joined(separator: "&").utf8)
    }

    // MARK: PKCE

    private nonisolated static func randomString() -> String {
        var bytes = [UInt8](repeating: 0, count: 32)
        _ = SecRandomCopyBytes(kSecRandomDefault, bytes.count, &bytes)
        return base64URL(Data(bytes))
    }

    private nonisolated static func codeChallenge(for verifier: String) -> String {
        base64URL(Data(SHA256.hash(data: Data(verifier.utf8))))
    }

    private nonisolated static func base64URL(_ data: Data) -> String {
        data.base64EncodedString()
            .replacingOccurrences(of: "+", with: "-")
            .replacingOccurrences(of: "/", with: "_")
            .replacingOccurrences(of: "=", with: "")
    }
}

/// Whether someone is signed in, for the views.
@Observable
final class AuthState {
    static let shared = AuthState()

    var isSignedIn = AuthSession.hasStoredTokens
}
