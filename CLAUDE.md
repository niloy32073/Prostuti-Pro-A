## Android rules
- ALWAYS load and strictly follow the `android-master` skill for every Android/Kotlin/Compose task — no exceptions.
- ALWAYS load and strictly follow the `work-rules` skill for every task that edits code or uses an emulator: one emulator at a time; ask before any edit the task doesn't need; report dead code.
- Stack: Compose · Navigation 3 · Koin (Compiler Plugin) · Ktor · Room · DataStore · MVVM. No Hilt, no Navigation 2, no Retrofit, no LiveData.
- Tests: follow the `prostuti-android-e2e-tests` skill.
- Before finishing, run the android-master §22 checklist and report what was verified.

## Project notes
- Adaptive layouts: read `currentWindowAdaptiveInfoV2().windowSizeClass` where needed; list column counts come from `ui/component/AdaptiveColumns.kt` (`adaptiveColumnCount()`). No hand-written dp breakpoints.
- Room: the app is not launched yet, so there are no migrations. After a schema change, uninstall and reinstall the app locally.
- Build from the CLI with `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"` (no system JDK on this machine).
