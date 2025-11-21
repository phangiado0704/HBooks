# Repository Guidelines

## Project Structure & Module Organization
`app/` holds the Android module, Gradle script, and Firebase config. Kotlin + Compose sources live under `app/src/main/java/com/example/hbooks` with folders such as `data`, `ui`, `services`, and `util`—mirror these when adding features. Resources stay in `app/src/main/res`; JVM tests belong in `app/src/test`, instrumentation specs in `app/src/androidTest`. Place feature assets (JSON, audio stubs) beside their owning package to keep build scripts simple.

## Build, Test, and Development Commands
`./gradlew assembleDebug` compiles the debug APK and runs Kotlin/Compose checks. `./gradlew :app:installDebug` installs on a connected device or emulator, useful for manual QA. Execute `./gradlew test` for JVM unit tests, `./gradlew connectedAndroidTest` for Espresso/Compose UI coverage, and `./gradlew lint` before review to catch XML or Kotlin style violations. When troubleshooting playback or Firebase flows, pair the Gradle task with `adb logcat | grep HBooks`.

## Coding Style & Naming Conventions
Use Kotlin idioms with 4-space indentation and trailing commas disabled. `@Composable` functions use PascalCase names, accept an optional `Modifier` first, and avoid hard-coded colors—pull from `ui/theme`. ViewModels expose immutable `StateFlow`/`UiState` data classes; mutable operations stay private. Data classes in `data/models` remain immutable, while services such as `PlaybackService` encapsulate platform APIs. Rely on Android Studio reformatting or `ktfmt` defaults so imports stay alphabetical and line length stays near 100 characters.

## Testing Guidelines
Mirroring package structure, name tests `ClassNameTest` and methods `method_underCondition_expectedResult`. Compose UI specs should rely on `createAndroidComposeRule`, while repository or util layers favor coroutine test dispatchers. Run both `test` and `connectedAndroidTest` before merging anything that touches view models, navigation, or ExoPlayer glue. Provide screenshots or `recordedOutput.md` snippets when UI assertions are not feasible.

## Commit & Pull Request Guidelines
Git history is unavailable in this snapshot, so follow concise, imperative subjects such as `Add playback notification controls`, with optional bodies that capture motivation and risk. Reference issue IDs in the body, list the Gradle tasks you ran, and mention schema or dependency changes explicitly. Pull requests should include a narrative summary, testing evidence, and screenshots for visible tweaks; request reviewers familiar with affected modules.

## Configuration & Security Notes
`google-services.json` maps Firebase credentials—use environment-specific copies and do not commit production keys. Store any additional secrets in `local.properties` or encrypted CI variables, then read them with `BuildConfig` or the Gradle catalog. Guard logs with `BuildConfig.DEBUG` and scrub Firestore IDs or Storage URLs before shipping.
