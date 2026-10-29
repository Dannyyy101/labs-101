import Foundation
import HTTPTypes
@_exported import OpenAPIRuntime
@_exported import OpenAPIURLSession

/// An answer of the backend outside of 2xx, `body` is its JSON (e.g. `{"errorMessage": ...}`).
public struct HTTPFailure: Error, Sendable {
    public let status: Int
    public let body: Data
}

extension Client {
    /// The client for the backend at `apiBaseURL` (".../api", the paths of the spec start with /api).
    /// `token` gives the access token of the signed in user, a fresh one when `forceRefresh` is set.
    public static func backend(
        apiBaseURL: URL,
        session: URLSession = .shared,
        token: @escaping @Sendable (_ forceRefresh: Bool) async throws -> String
    ) -> Client {
        let serverURL = apiBaseURL.lastPathComponent == "api" ? apiBaseURL.deletingLastPathComponent() : apiBaseURL
        return Client(
            serverURL: serverURL,
            configuration: Configuration(dateTranscoder: InstantTranscoder()),
            transport: URLSessionTransport(configuration: .init(session: session)),
            middlewares: [AuthMiddleware(token: token)]
        )
    }
}

/// Adds the access token and turns every answer outside of 2xx into an `HTTPFailure`.
struct AuthMiddleware: ClientMiddleware {
    let token: @Sendable (_ forceRefresh: Bool) async throws -> String

    func intercept(
        _ request: HTTPRequest,
        body: HTTPBody?,
        baseURL: URL,
        operationID: String,
        next: @Sendable (HTTPRequest, HTTPBody?, URL) async throws -> (HTTPResponse, HTTPBody?)
    ) async throws -> (HTTPResponse, HTTPBody?) {
        var (response, responseBody) = try await send(request, body, baseURL, token: token(false), next: next)
        if response.status == .unauthorized {
            // the token may have been revoked before it ran out, try once with a fresh one
            (response, responseBody) = try await send(request, body, baseURL, token: token(true), next: next)
        }
        guard response.status.kind == .successful else {
            var data = Data()
            if let responseBody {
                data = try await Data(collecting: responseBody, upTo: 1024 * 1024)
            }
            throw HTTPFailure(status: response.status.code, body: data)
        }
        return (response, responseBody)
    }

    private func send(
        _ request: HTTPRequest,
        _ body: HTTPBody?,
        _ baseURL: URL,
        token: String,
        next: @Sendable (HTTPRequest, HTTPBody?, URL) async throws -> (HTTPResponse, HTTPBody?)
    ) async throws -> (HTTPResponse, HTTPBody?) {
        var request = request
        request.headerFields[.authorization] = "Bearer \(token)"
        return try await next(request, body, baseURL)
    }
}

/// Java Instants come with or without fractional seconds, they are sent with them.
struct InstantTranscoder: DateTranscoder {
    func encode(_ date: Date) throws -> String {
        date.ISO8601Format(.iso8601WithFractionalSeconds)
    }

    func decode(_ string: String) throws -> Date {
        if let date = try? Date(string, strategy: Date.ISO8601FormatStyle(includingFractionalSeconds: true)) {
            return date
        }
        if let date = try? Date(string, strategy: .iso8601) {
            return date
        }
        throw DecodingError.dataCorrupted(.init(codingPath: [], debugDescription: "Invalid date \(string)"))
    }
}

extension Date.ISO8601FormatStyle {
    fileprivate static let iso8601WithFractionalSeconds = Date.ISO8601FormatStyle(includingFractionalSeconds: true)
}
