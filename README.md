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
- **Daily goals & remaining macros** — set calorie/protein/carb/fat targets and
  track today's progress on the home screen.
- **Light & dark** — a calm teal palette that follows your phone's dark-mode
  setting, or pin Light/Dark in Settings. No white flash on launch either way.
- **On-device & private** — API key and goals live in `EncryptedSharedPreferences`,
  meals persist in Room. No accounts, no servers, no sharing.
- **Runs on `gemini-3.5-flash-lite`** (Gemini free tier ≈ 500 requests/day),
  with automatic retry + backoff on transient API throttles.

## Install

The current stable build is **GaaS 1.0.0**:

1. On your phone's browser, open the release page:
   https://github.com/bruhjeshhh/GaaS/releases/tag/v1.0.0
2. Download **GaaS-1.0.0.apk** and tap it. Android will warn about installing
   from unknown sources — allow it for your browser (Settings → "Install
   unknown apps").
3. Open GaaS. First launch asks for a Gemini API key (free at
   aistudio.google.com/apikey) and your daily goal.

Want the bleeding-edge build? Fetch **app-debug.apk** from the rolling
**debug-build** release, auto-rebuilt on every push. Both are signed with the
same key, so they install over each other as clean updates.

## Build it yourself

Requirements: JDK 17, Android SDK (API 26+), and either Android Studio
(Koala+) or a command line.

```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

The first Gradle run pulls Compose, Room, OkHttp, and kotlinx.serialization.

## How it works

1. **Onboarding** collects your Gemini API key and daily
   calorie/protein/carb/fat goal — stored in `EncryptedSharedPreferences`
   (`SettingsStore.kt`).
2. **Logging a meal**: `GeminiClient.kt` sends your description to Gemini with
   a prompt written specifically for vague quantities, asking for structured
   JSON (items + totals + an assumptions note).
3. **Review step**: the parsed breakdown is shown before anything saves so you
   can catch bad guesses (`MacroViewModel.kt`).
4. **Home screen**: today's meals and remaining macros, computed by summing
   Room-stored entries (`MealEntry.kt`) against the goal.

### Tech stack

Kotlin + Jetpack Compose (Material 3), lifecycle-compose + navigation,
Room (KSP), OkHttp, kotlinx-serialization, androidx.security
EncryptedSharedPreferences.

### Project layout

```
app/src/main/java/com/brajesh/gaas/
├── MainActivity.kt          App entry / navigation host
├── ui/                      Compose screens (Onboarding, Home, AddMeal summary)
├── network/GeminiClient.kt  Gemini API call + tolerant JSON parsing
├── viewmodel/MacroViewModel.kt  UI state + estimation flow
├── repository/MacroRepository.kt  Bridge between Room and screens
└── data/                    Room DB (MealEntry, MealDao) + SettingsStore
```

## Releasing

CI (`.github/workflows/build.yml`) builds `main` on every push and publishes a
rolling **debug-build** release. Pushing any tag like `v1.1.0` additionally
publishes a stable release with a `GaaS-1.1.0.apk` asset.

## Roadmap / known gaps

- Edit-goal screen (repository method `updateGoal` exists, not yet wired into
  the nav graph).
- History/trends beyond "today" — `MealDao.allMeals()` and `allDays()` are ready
  to build on.
- The API key is stored encrypted but is still extractable on a rooted device.
  If this ever gets real users, custody should move server-side.

## Privacy & costs

Your meal descriptions are sent to the Gemini API using the key *you* provide —
there's no middleman account. The free tier of `gemini-3.5-flash-lite` covers
roughly 500 requests/day, which is hundreds of meals. Your key is never
committed to this repository.