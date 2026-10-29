---
name: ios
description: Konventionen für die SwiftUI-App und Watch-App von labs-101 (Services, generierter API-Client im Paket LabsAPI, Mapping der DTOs). Verwenden, sobald unter ios/ Code geschrieben oder geändert wird, vor allem bei Aufrufen des Backends.
---

# iOS

Es gelten zuerst die Grundregeln des Skills `labs-101`: Fehler zuerst prüfen (`guard`), Happy Path am Ende.

## Backend nur über den generierten Client

- Das lokale Paket `LabsAPI` erzeugt bei jedem Build mit swift-openapi-generator den Client aus
  `LabsAPI/Sources/LabsAPI/openapi.json`, einem Symlink auf `../api/openapi.json`. Generierter
  Code liegt nicht im Repo.
- `LabsAPI.swift` im Paket setzt den Token (bei 401 einmal neu), liest Java-Instants mit und ohne
  Sekundenbruchteile und wirft Antworten außerhalb von 2xx als `HTTPFailure`.
- Die Services (`WorkoutService`, `FoodService`, ...) rufen über `APIClient.call` auf. Dort wird
  `HTTPFailure` zu `APIError`:

  ```swift
  func exercises() async throws -> [Exercise] {
      try await client.call { try await $0.workoutGetAllExercises().ok.body.json }.map(Exercise.init)
  }
  ```

- Die Methodennamen kommen aus den Operation-IDs des Backends (Controller + Methode).

## DTOs und App-Modelle

- Views arbeiten mit den App-Modellen (`Exercise`, `TrackedFood`, ...), nicht mit
  `Components.Schemas.*`.
- Die Umwandlung steht in `*Models+API.swift` als `init(_ dto:)` bzw. `var dto` und greift jedes
  Feld des DTOs an. So bricht der Build, wenn sich ein DTO ändert.
- Zahlen kommen als `Int64`/`Int32`, UUIDs als `String`, nullable Referenzen als `...Payload?`
  mit `value1`.
- Ungültige Antworten (unbekannter Enum-Wert, fehlender Pflichtwert) mit
  `throw APIError.invalidResponse` ablehnen.

## Bauen

```sh
xcodebuild -project labs-101.xcodeproj -scheme labs-101 \
  -destination 'generic/platform=iOS Simulator' -skipPackagePluginValidation build
```

In Xcode muss der Plugin "OpenAPIGenerator" beim ersten Build einmal erlaubt werden ("Trust & Enable").
