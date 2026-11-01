import Foundation
import LabsAPI

nonisolated enum APIError: LocalizedError {
    case invalidResponse
    case unauthorized
    /// `message` is the localized error message of the backend, if it sent one
    case server(status: Int, message: String?)

    var errorDescription: String? {
        switch self {
        case .invalidResponse:
            String(localized: "Ungültige Antwort vom Server")
        case .unauthorized:
            String(localized: "Die Anmeldung ist abgelaufen, bitte melde dich neu an")
        case .server(let status, let message):
            message ?? String(localized: "Serverfehler (\(status))")
        }
    }
}

/// The client of the backend generated from api/openapi.json (package LabsAPI), every request carries
/// the access token of the signed in user. A changed DTO breaks the build of the app.
nonisolated struct APIClient: Sendable {
    private let client: Client

    init(config: AppConfig, auth: AuthSession = .shared, session: URLSession = .shared) {
        self.client = .backend(apiBaseURL: config.apiBaseURL, session: session) { forceRefresh in
            try await auth.accessToken(forceRefresh: forceRefresh)
        }
    }

    /// Calls the backend, answers outside of 2xx are thrown as `APIError`.
    func call<Result>(_ operation: (Client) async throws -> Result) async throws -> Result {
        do {
            return try await operation(client)
        } catch let error as ClientError {
            throw Self.apiError(error.underlyingError)
        }
    }

    private static func apiError(_ error: any Error) -> any Error {
        guard let failure = error as? HTTPFailure else { return error }
        if failure.status == 401 {
            return APIError.unauthorized
        }
        return APIError.server(status: failure.status, message: errorMessage(from: failure.body))
    }

    // the backend answers with {"errorMessage": ...} for known errors and {"message": ...} otherwise
    private static func errorMessage(from data: Data) -> String? {
        struct Body: Decodable {
            let errorMessage: String?
            let message: String?
        }
        guard let body = try? JSONDecoder().decode(Body.self, from: data) else { return nil }
        return [body.errorMessage, body.message].compactMap { $0 }.first { !$0.isEmpty }
    }
}
