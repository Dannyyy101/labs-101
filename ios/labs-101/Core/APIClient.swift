import Foundation

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
            String(localized: "Der API-Key wurde vom Server abgelehnt")
        case .server(let status, let message):
            message ?? String(localized: "Serverfehler (\(status))")
        }
    }
}

/// Thin JSON client for the backend, every request carries the API key.
nonisolated struct APIClient: Sendable {
    static let apiKeyHeader = "X-API-Key"

    private let baseURL: URL
    private let apiKey: String
    private let session: URLSession

    init(config: AppConfig, session: URLSession = .shared) {
        self.baseURL = config.apiBaseURL
        self.apiKey = config.apiKey
        self.session = session
    }

    func get<Response: Decodable>(_ path: String, query: [URLQueryItem] = []) async throws -> Response {
        let data = try await perform("GET", path, query: query)
        return try Self.decoder.decode(Response.self, from: data)
    }

    func send<Response: Decodable>(_ method: String, _ path: String, body: (some Encodable)? = nil as Never?) async throws -> Response {
        let data = try await perform(method, path, body: body.map(Self.encoder.encode))
        return try Self.decoder.decode(Response.self, from: data)
    }

    /// For endpoints that answer without body (204 / empty 200).
    func send(_ method: String, _ path: String, body: (some Encodable)? = nil as Never?) async throws {
        _ = try await perform(method, path, body: body.map(Self.encoder.encode))
    }

    private func perform(_ method: String, _ path: String, query: [URLQueryItem] = [], body: Data? = nil) async throws -> Data {
        var url = baseURL.appending(path: path)
        if !query.isEmpty {
            url.append(queryItems: query)
        }

        var request = URLRequest(url: url)
        request.httpMethod = method
        request.setValue(apiKey, forHTTPHeaderField: Self.apiKeyHeader)
        request.setValue("application/json", forHTTPHeaderField: "Accept")
        if let body {
            request.httpBody = body
            request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        }

        let (data, response) = try await session.data(for: request)
        guard let http = response as? HTTPURLResponse else { throw APIError.invalidResponse }

        switch http.statusCode {
        case 200..<300:
            return data
        case 401:
            throw APIError.unauthorized
        default:
            throw APIError.server(status: http.statusCode, message: Self.errorMessage(from: data))
        }
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

    private static let encoder: JSONEncoder = {
        let encoder = JSONEncoder()
        // Java Instants, the default would be seconds since 2001
        encoder.dateEncodingStrategy = .custom { date, encoder in
            var container = encoder.singleValueContainer()
            try container.encode(date.ISO8601Format(.iso8601WithFractionalSeconds))
        }
        return encoder
    }()

    private static let decoder: JSONDecoder = {
        let decoder = JSONDecoder()
        // Java Instants come with or without fractional seconds
        decoder.dateDecodingStrategy = .custom { decoder in
            let string = try decoder.singleValueContainer().decode(String.self)
            if let date = try? Date(string, strategy: Date.ISO8601FormatStyle(includingFractionalSeconds: true)) {
                return date
            }
            if let date = try? Date(string, strategy: .iso8601) {
                return date
            }
            throw DecodingError.dataCorrupted(.init(codingPath: decoder.codingPath, debugDescription: "Invalid date \(string)"))
        }
        return decoder
    }()
}
