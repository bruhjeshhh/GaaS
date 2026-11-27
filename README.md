# GaaS — Gains as a Service

A tiny personal macro tracker for Android. Describe a meal the way you'd
actually say it — *"3 rotis with a little ghee, homemade paneer"* or *"a bowl
of curd rice, some pickle"* — and GaaS turns it into per-item calories,
protein, carbs, and fat. It runs off **your own Gemini API key**, so there's no
backend server and no data leaves your phone except the meal description you
just typed.

## Features

- **Plain-language logging** — works with vague quantities ("a little", "some",
  "a bowl") and everyday phrasing.
- **Review before you save** — see the parsed breakdown and an "assumptions"
  note explaining what was guessed; rephrase or discard if it's off.
- **Daily goals & remaining macros** — a bold gradient home hero shows the
  calories left today plus protein/carbs/fat at a glance.
- **Bottom navigation** — Home, Log, History and Settings, with an optional
  Google account for a personal greeting.
- **First-run API key walkthrough** — four short steps for getting a free key
  from Google AI Studio, right at the start of setup.
- **Light & dark** — vivid teal/violet/amber palette that follows your phone's
  dark-mode setting, or pin Light/Dark in Settings. No white flash on launch.
- **Tells you when a new version lands** — checks GitHub for a newer tagged
  release once per launch and offers a one-tap link to it. Dismissible.
- **On-device & private** — API key, goal and optional Google profile live in
  `EncryptedSharedPreferences`; meals persist in Room. No backend, no telemetry.
- **Runs on `gemini-3.5-flash-lite`** (Gemini free tier ≈ 500 requests/day),
  with automatic retry + backoff on transient API throttles.

## Install

The last sideloaded stable build is **GaaS 1.3.0**:

1. On your phone's browser, open the release page:
   https://github.com/bruhjeshhh/GaaS/releases/tag/v1.3.0
2. Download **GaaS-1.3.0.apk** and tap it. Android will warn about installing
   from unknown sources — allow it for your browser.
3. Open GaaS and follow the four-step API key walkthrough, then enter your key
   and daily goal. Signing in with Google is optional.

Version **2.0.0** adds Google sign-in, the API key walkthrough and the new UI.
Until a `v2.0.0` tag is published, grab the rolling **debug-build** APK (auto
rebuilt on every push) — it installs over earlier builds.

## Build it yourself

Requirements: JDK 17, Android SDK (API 26+), and either Android Studio
(Koala+) or a command line.

```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

The first Gradle run pulls Compose, Room, OkHttp, Credential Manager and
kotlinx.serialization.

### Google sign-in setup (optional)

GaaS uses the platform Credential Manager, not Firebase, so there is no
`google-services.json` and no server to run.

1. In Google Cloud Console, create an OAuth client ID of type **Web
   application** (client secret not needed).
2. Put its client ID in `local.properties`:
   `google.web.client.id=YOUR_WEB_CLIENT_ID.apps.googleusercontent.com`
   — or export `GOOGLE_WEB_CLIENT_ID` instead.
3. If Cloud Console asks for a package/SHA-1 pairing, register
   `com.brajesh.gaas` with your debug keystore's SHA-1, and repeat with the
   upload key SHA-1 used for Play.

Without a client ID the app still builds and runs; the sign-in button explains
that it isn't configured. Sign-in never gates tracking.

## How it works

1. **Onboarding** starts with a short "get a free key" tutorial, then collects
   the Gemini API key and daily goal — stored in `EncryptedSharedPreferences`
   (`SettingsStore.kt`). Google sign-in is an optional first card.
2. **Logging a meal**: `GeminiClient.kt` sends your description to Gemini with
   a prompt written specifically for vague quantities, asking for structured
   JSON (items + totals + an assumptions note).
3. **Review step**: the parsed breakdown is shown before anything saves so you
   can catch bad guesses (`MacroViewModel.kt`).
4. **Home screen**: today's meals and a gradient remaining-calories hero, with
   macros computed by summing Room-stored entries (`MealEntry.kt`).
5. **Update check**: `UpdateChecker.kt` reads the repo's GitHub releases once per
   launch and compares the newest `v*` tag against the installed
   `BuildConfig.VERSION_NAME`. It ignores the rolling `debug-build` tag.

### Tech stack

Kotlin + Jetpack Compose (Material 3), lifecycle-compose, Room (KSP), OkHttp,
kotlinx-serialization, androidx.security EncryptedSharedPreferences, and
androidx.credentials for Google Sign-In. Targets **API 36** and is edge-to-edge.

### Project layout

```
app/src/main/java/com/brajesh/gaas/
├── MainActivity.kt          App entry / navigation host
├── auth/                    Google Sign-In via Credential Manager
├── ui/                      Compose screens, bottom nav and shared widgets
├── ui/theme/Theme.kt        Light/dark palette, theme + system-bar handling
├── network/GeminiClient.kt  Gemini API call + tolerant JSON parsing
├── network/UpdateChecker.kt GitHub release lookup for the update banner
├── viewmodel/MacroViewModel.kt  UI state + estimation flow
├── repository/MacroRepository.kt  Bridge between Room and screens
└── data/                    Room DB (MealEntry, MealDao) + SettingsStore
```

## Releasing

CI (`.github/workflows/build.yml`) builds `main` on every push, publishes a
rolling **debug-build** release, and uploads both the debug APK and an unsigned
release AAB as artifacts. Any `v*` tag publishes a stable APK release.

### Google Play launch checklist

1. **Developer account** — one-time **$25** registration fee, plus identity
   verification. Accounts created after 2023-11-13 need 12 closed testers for
   14 days before production; internal testing skips that.
2. **Upload key** — create a keystore and put its details in an untracked
   `keystore.properties` at the repo root:
   `storeFile=release.jks`, `storePassword=…`, `keyAlias=gaas`,
   `keyPassword=…`. Enrol in Play App Signing; the upload key can then be
   replaced if lost.
3. **Build the bundle** — `./gradlew bundleRelease` →
   `app/build/outputs/bundle/release/app-release.aab`. Upload that, not an APK.
4. **Store listing** — app name "GaaS — Gains as a Service", category
   Health & Fitness, short description: "Log meals in plain language and see
   their macros instantly." A matching 512×512 listing PNG is committed at
   `play/store-icon-512.png`.
5. **Privacy policy URL** — publish a page (GitHub Pages is free) stating what
   is collected and why. No account data is sent to GaaS servers (there are
   none); meal text goes to Gemini and an optional Google name/email is kept
   on-device only.
6. **Data Safety form** — declare that meal descriptions are transmitted to
   Google (Gemini API) and that the optional Google profile, API key, goals and
   meal history are stored on-device. No data is sold or shared with third
   parties, and there is no account deletion endpoint because there is no
   server-side account.
7. **Content rating & target API** — complete the questionnaire; targetSdk 36
   already meets the Play floor, and edge-to-edge is handled in `MainActivity`.
8. **Testing** — upload the signed AAB to the internal track first, install it
   from Play, and verify sign-in, onboarding and the update checker before
   promoting to production.

## Privacy & costs

Your meal descriptions are sent to the Gemini API using the key *you* provide —
there's no middleman account. Google sign-in uses the free Credential Manager
API; there is no Firebase bill and no backend to host. The Gemini free tier
covers roughly 500 requests/day, which is hundreds of meals. The only fixed
cost on this path is Google Play's **$25 one-time** developer fee. Your key is
never committed to this repository.
