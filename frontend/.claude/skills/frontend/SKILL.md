---
name: frontend
description: Konventionen für das Next.js-Frontend von labs-101 (Server Actions, typisierter API-Client, Typen aus der OpenAPI-Spec). Verwenden, sobald unter frontend/ Code geschrieben oder geändert wird, vor allem bei Aufrufen des Backends.
---

# Frontend

Es gelten zuerst die Grundregeln des Skills `labs-101`: Fehler zuerst prüfen, Happy Path am Ende.
Next.js weicht hier von bekannten Versionen ab, siehe `AGENTS.md`.

## Backend nur über den generierten Client

- `lib/api/schema.d.ts` wird aus `../api/openapi.json` generiert (`npm run generate:api`,
  Skript `scripts/generate-api.mjs`). Nie von Hand ändern.
- Aufrufe gehen über `api` aus `lib/api/client.ts` (openapi-fetch). Pfad, Parameter und Body sind
  typisiert, der Token des Nutzers wird automatisch gesetzt:

  ```ts
  const { data } = await api.GET("/api/users/me/runs/{id}", { params: { path: { id } } })

  if (!data) throw new Error(`Error fetching run ${id}`)
  return data
  ```

- Actions, die nicht werfen dürfen (Navbar, Einstellungen, Freunde, ...), geben ein `Result` zurück:
  `return toResult(() => api.GET("/api/users/me/friends"), "Freunde konnten nicht geladen werden")`.
  Die lokalisierte Meldung des Backends liefert `errorMessage(error, fallback)`.
- Kein `fetch`/`BACKEND_URL` mehr für Backend-Aufrufe.

## Typen

- DTO-Typen sind Aliase auf die generierten Schemas, z. B. in `utils/types/friend.ts`:
  `export type Friend = Schemas["FriendDto"]` (`Schemas` aus `lib/api/types.ts`).
- Eigene Typen nur für Frontend-Modelle, die sich wirklich unterscheiden (z. B. `CalendarEvent`
  mit `Date`). Dann im Mapper von bzw. zu den generierten Typen umwandeln.
- Enums des Backends als Union plus Konstanten-Objekt mit `satisfies`, damit ein neuer Wert im
  Backend den Build bricht (siehe `Meal` in `utils/types/food.ts`).

## Wenn sich die Spec geändert hat

```sh
npm run generate:api
npx tsc --noEmit
```

Alle Fehler beheben und `lib/api/schema.d.ts` mitcommitten. Die CI generiert neu und schlägt fehl,
wenn die Datei veraltet ist. Docker baut nur mit `frontend/` als Kontext und nutzt die committete Datei.
