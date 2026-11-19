# labs-101 Android

Kotlin + Jetpack Compose, the counterpart of the iOS app for food and workout tracking. Android 9 (API 28) or newer.

| Tab | What it does |
| --- | --- |
| Übersicht | Recovery, strain, sleep and activity of a day (the data comes from Apple Health, so it stays empty for Android users) |
| Food | Day view with calorie ring and macros, search with paging, barcode scanner, free text ("2 Eier und eine Banane"), change and delete entries |
| Training | Templates, free workout, running workout with autosave, "last time" hints, rest timer, cardio, history |
| Profil | Name, link to the Zitadel profile, sign out |

Not there yet: Health Connect sync (the Android side of HealthKit), notifications, a Wear OS app, friends and calendar.

## Setup

Requires JDK 21 and the Android SDK, either through Android Studio or:

```sh
brew install --cask android-commandlinetools
brew install openjdk@21
sdkmanager "platform-tools" "platforms;android-36" "build-tools;36.0.0"
```

`local.properties` (git ignored) points Gradle at the SDK, e.g. `sdk.dir=/opt/homebrew/share/android-commandlinetools`. Android Studio writes it by itself.

```sh
cp secrets.example.properties secrets.properties
```

and fill in:

- `API_BASE_URL`: the backend including `/api`. It has to be reachable from the phone, so not `localhost` (the emulator reaches the Mac at `10.0.2.2`). A `http://` URL turns on cleartext traffic.
- `AUTH_ISSUER` and `AUTH_CLIENT_ID`: the same native app in Zitadel as iOS, redirect URI `labs101://auth/callback`.

The values are compiled into the APK, so build again after changing them.

## Build

```sh
./gradlew assembleDebug     # app/build/outputs/apk/debug/app-debug.apk, about 65 MB
./gradlew assembleRelease   # app/build/outputs/apk/release/app-release.apk, about 4 MB (shrunk by R8)
./gradlew installDebug      # onto a connected phone with USB debugging
```

## API client

Like the frontend and iOS, the app never writes the types of the backend by hand: `openApiGenerate` creates a Retrofit client with kotlinx.serialization from `../api/openapi.json` into `app/build/generated/openapi` before every build, so a changed DTO breaks the build.

The Kotlin generator only understands OpenAPI 3.0, so [`buildSrc/src/main/kotlin/OpenApiNormalizer.kt`](buildSrc/src/main/kotlin/OpenApiNormalizer.kt) first translates the 3.1 spec of springdoc: `type: ["string", "null"]` becomes `nullable`, nullable references (`type: ["object", "null"]` with `allOf`) stay nullable, and the `oneOf` of strength and cardio items becomes one class whose fields are only required if both variants require them. Without it the generator makes `portion`, `lastEntry` or `cardio` non-null and the app crashes on the first `null`.

## Sharing the APK

Send `app-release.apk` (via messenger, cloud link, …). On the phone, allow "Install unknown apps" for the app that opens it. There is no store, no developer account and no expiry.

- Without your own key the release APK is signed with the debug key of the machine that built it (`~/.android/debug.keystore`). Updates only install over the old version if they come from the same machine. For a key of your own, create one with `keytool -genkeypair -v -keystore labs101-release.jks -alias labs101 -keyalg RSA -keysize 4096 -validity 10000` and enter it in `secrets.properties` (see `secrets.example.properties`). Keep the key, without it no update installs over the app.
- Raise `versionCode` in `app/build.gradle.kts` for every update you send around.
- Registration in Zitadel is turned off, so every user needs an account created in the Zitadel console first.
- Google is rolling out developer verification for sideloaded apps from 2026 on (first in Brazil, Indonesia, Singapore and Thailand, worldwide planned for 2027). Until it applies in Germany nothing changes; after that, a free account with limited distribution should cover a few friends.

## Structure

```
app/src/main/java/tech/project101/labs/
├── LabsApp.kt, MainActivity.kt   services of the app, login redirect
├── auth/      Zitadel login with PKCE, tokens encrypted with a key of the Android keystore
├── core/      config, Retrofit client with bearer token, formatting
├── food/      food tracker, search, barcode, text input, entry
├── workout/   templates, running workout, history
├── overview/  health overview of a day
├── profile/   profile and sign out
└── ui/        navigation, theme (the tokens of the web), shared components
```
