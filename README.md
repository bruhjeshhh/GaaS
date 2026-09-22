# GaaS — Gains as a Service

Log meals in plain, verbose language ("3 rotis with a little ghee, homemade
paneer") and get macros back, with a running "remaining today" total against
a daily goal. Uses your own Gemini API key — no backend server.

## Get it on your phone (no cable, no local SDK)

1. Push a commit to `main` (or press **Run workflow** under the repo's
   **Actions → Build APK** tab). A GitHub Actions job compiles the app and
   publishes a "debug build" release on GitHub.
2. On your phone's browser, sign in to GitHub and open
   `https://github.com/bruhjeshhh/GaaS/releases` — download the
   **app-debug.apk** attached to the latest debug build.
3. Tap the downloaded file. Android will warn about installing from unknown
   sources: allow it for your browser (Settings → "Install unknown apps").
4. Open GaaS. First launch asks for a Gemini API key (free at
   aistudio.google.com/apikey) and your daily goal.

Every future push rebuilds and replaces that same APK — just re-download and
install over the old one (same signing key is reused, so it's a clean update).

## Build locally (optional)

1. Open this folder in Android Studio (Koala or newer) — or install JDK 17 +
   the Android SDK and run `./gradlew assembleDebug`.
2. Let Gradle sync — it'll pull Compose, Room, OkHttp, kotlinx.serialization.
3. Run on a device/emulator with API 26+. The debug APK lands in
   `app/build/outputs/apk/debug/app-debug.apk`.

## How it works

- **Onboarding**: enter a Gemini API key (get one free at
  aistudio.google.com/apikey) and set a daily calorie/protein/carb/fat goal.
  Stored in `EncryptedSharedPreferences` (`SettingsStore.kt`).
- **Logging a meal**: type a description of any length/casualness.
  `GeminiClient.kt` sends it straight to `gemini-3.6-flash` with a prompt
  written specifically for vague quantities ("a little", "some", "a bowl")
  and returns structured JSON (items + totals + an assumptions note so you
  can see what it guessed).
- **Review step**: before anything saves, you see the parsed breakdown and
  can discard and rephrase if the estimate looks off, rather than trusting
  it blindly.
- **Home screen**: today's meals plus remaining macros, computed by summing
  Room-stored entries (`MealEntry.kt`) against the goal.

## Known gaps to fill in yourself

- No edit-goal screen wired into nav yet (repository method exists —
  `updateGoal`).
- The API key sits in EncryptedSharedPreferences, which is fine for a
  personal build but is still extractable on a rooted device. If this ever
  gets real users, move key custody server-side.
- No history/trends view beyond "today" — `MealDao.allMeals()` and
  `allDays()` are there to build one.
