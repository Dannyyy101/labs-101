---
name: labs-101
description: Grundregeln für jeden Code in labs-101 (Backend, Frontend, iOS), vor allem der Aufbau von Funktionen (Fehler zuerst, Happy Path am Ende) und der gemeinsame API-Vertrag in api/openapi.json. Verwenden, sobald Code in diesem Repo geschrieben oder geändert wird.
---

# labs-101

Monorepo mit drei Projekten, die über einen gemeinsamen API-Vertrag verbunden sind:

- `backend/`: Spring Boot (Java 25), siehe den Skill `backend`
- `frontend/`: Next.js, siehe den Skill `frontend`
- `ios/`: SwiftUI-App mit Watch-App, siehe den Skill `ios`
- `api/openapi.json`: die OpenAPI-Spec, aus der Frontend und iOS ihre Clients generieren

## 1. Aufbau einer Funktion: der Happy Path steht am Ende

Eine Funktion prüft zuerst alles, was schiefgehen kann (ungültige Eingaben, fehlende Daten,
Fehlerantworten, Sonderfälle) und verlässt sich dabei früh mit `return`/`throw`/`guard`. Der
Normalfall kommt zuletzt und ist nicht eingerückt.

```ts
// so
export async function getProfile(): Promise<Profile> {
    const { data } = await api.GET("/api/users/me/profile")

    if (!data) throw new Error("Error fetching profile")
    return data
}

// nicht so
export async function getProfile(): Promise<Profile> {
    const { data } = await api.GET("/api/users/me/profile")
    if (data) {
        return data
    }
    throw new Error("Error fetching profile")
}
```

```java
if (type == null || schema.getProperties() == null) {
    return;
}
// ... der eigentliche Fall
```

```swift
guard let meal = Meal(rawValue: dto.meal._type) else { throw APIError.invalidResponse }
self.init(...)
```

- Kein `else` nach einem `return`/`throw`.
- Mehrere Fehlerfälle nacheinander als einzelne Guards, nicht verschachtelt.
- Das gilt in allen drei Projekten.

## 2. Ein Vertrag für die API

Die Controller des Backends sind die einzige Quelle für die API. springdoc erzeugt daraus die
Spec, `OpenApiSpecTest` schreibt sie nach `api/openapi.json`. Das Frontend (openapi-typescript +
openapi-fetch) und iOS (swift-openapi-generator im Paket `ios/LabsAPI`) generieren daraus ihre
Typen und Clients. Ändert sich ein DTO, bricht der Build im Frontend bzw. in iOS.

Ablauf bei einer Änderung an Controller oder DTO:

1. Backend ändern, `cd backend && mvn test -Dtest=OpenApiSpecTest` schreibt `api/openapi.json` neu
   (braucht die Datenbank: `docker compose up -d db`).
2. `cd frontend && npm run generate:api` schreibt `lib/api/schema.d.ts` neu, dann `npx tsc --noEmit`.
3. iOS generiert bei jedem Build selbst, Xcode bzw. `xcodebuild ... -skipPackagePluginValidation`.
4. Alle Fehler beheben und `api/openapi.json` und `frontend/lib/api/schema.d.ts` mitcommitten. In
   der CI schlagen `OpenApiSpecTest` (mit `CI=true`) und der Frontend-Job fehl, wenn sie veraltet sind.

Nie Typen für Backend-Antworten von Hand schreiben, immer die generierten verwenden.

## Allgemein

- Kommentare und Bezeichner auf Englisch, Texte für Nutzer wie in der Umgebung (meist Deutsch).
- Kommentare erklären das Warum, kurz und kleingeschrieben wie im restlichen Code.
