---
name: backend
description: Konventionen für das Spring-Boot-Backend von labs-101 (Controller, DTOs, OpenAPI-Spec, Tests). Verwenden, sobald unter backend/ Code geschrieben oder geändert wird, vor allem bei Controllern und DTOs.
---

# Backend

Es gelten zuerst die Grundregeln des Skills `labs-101`: Fehler zuerst prüfen, Happy Path am Ende.

## Controller und DTOs sind der API-Vertrag

Aus den Controllern erzeugt springdoc die Spec (`/v3/api-docs`, Konfiguration in
`config/OpenApiConfig.java`). `OpenApiSpecTest` schreibt sie nach `../api/openapi.json`.

- **DTOs sind Records** (oder Lombok-Klassen) in `com.labs_101.backend.dtos`. Nur dort sucht
  `OpenApiConfig` nach `@Nullable`.
- **Jedes Feld ist Pflicht**, außer es ist mit `org.jspecify.annotations.Nullable` markiert:
  `public record FriendDto(String id, @Nullable String name, ...)`. Jackson schreibt immer alle
  Felder, in Antworten sind nullable Felder daher vorhanden und dürfen `null` sein. In DTOs, die
  nur an das Backend geschickt werden, dürfen sie fehlen.
- Ein Feld, das `null` sein kann, **muss** `@Nullable` bekommen, sonst lügt die Spec und Swift
  scheitert beim Dekodieren.
- **Statuscodes ohne 200** dokumentieren: `@ApiResponse(responseCode = "204")` bei
  `ResponseEntity.noContent()`, `"201"` bei `CREATED`, `"202"` bei `ACCEPTED`. Bei 200 oder 204
  beide angeben, für 204 mit `content = @Content`.
- Subtypen über `@JsonTypeInfo`/`@JsonSubTypes` an einem sealed interface, das Mapping der
  Diskriminator-Werte übernimmt `OpenApiConfig`.
- `Pageable`-Parameter mit `@ParameterObject`.
- `@CurrentUser` taucht nicht in der Spec auf, die ID kommt aus dem Token.
- Endpunkte, die nicht für Frontend oder iOS gedacht sind (z. B. MCP), mit `@Hidden` ausblenden.
- Die Operation-IDs (Methodennamen im Swift-Client) sind Controller + Methode,
  z. B. `friendGetProfile`. Methoden umbenennen ändert also den iOS-Client.

## Nach jeder Änderung an Controller oder DTO

```sh
docker compose up -d db          # im Repo-Root
cd backend && mvn test -Dtest=OpenApiSpecTest
```

Danach Frontend (`npm run generate:api`) und iOS anpassen und `api/openapi.json` mitcommitten.
In der CI (`CI=true`) schlägt der Test fehl, wenn die committete Spec nicht zu den Controllern passt.

## Tests

`mvn test` braucht die Datenbank aus `docker compose`. Bei "Unresolved compilation problems" hat
VS Code in `target/` kompiliert: `mvn clean test`.
