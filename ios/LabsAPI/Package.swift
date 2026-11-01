// swift-tools-version: 6.0
import PackageDescription

// The client of the backend, generated from api/openapi.json on every build
// (Sources/LabsAPI/openapi.json links to it), so a changed DTO breaks the build of the app.
let package = Package(
    name: "LabsAPI",
    platforms: [.iOS(.v17), .watchOS(.v10), .macOS(.v14)],
    products: [
        .library(name: "LabsAPI", targets: ["LabsAPI"]),
    ],
    dependencies: [
        .package(url: "https://github.com/apple/swift-openapi-generator", from: "1.14.3"),
        .package(url: "https://github.com/apple/swift-openapi-runtime", from: "1.13.3"),
        .package(url: "https://github.com/apple/swift-openapi-urlsession", from: "1.4.0"),
        .package(url: "https://github.com/apple/swift-http-types", from: "1.0.0"),
    ],
    targets: [
        .target(
            name: "LabsAPI",
            dependencies: [
                .product(name: "OpenAPIRuntime", package: "swift-openapi-runtime"),
                .product(name: "OpenAPIURLSession", package: "swift-openapi-urlsession"),
                .product(name: "HTTPTypes", package: "swift-http-types"),
            ],
            plugins: [
                .plugin(name: "OpenAPIGenerator", package: "swift-openapi-generator"),
            ]
        ),
    ]
)
