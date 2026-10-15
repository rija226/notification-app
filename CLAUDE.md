# Podsjetnik

Personal Android reminder app that keeps reminding until a task is marked done. Single user, offline, Croatian UI, sideloaded APK on one Samsung phone (Android 15+).

## Documents: what to trust for what
| Topic | Source of truth |
|---|---|
| Reminder behavior, data model, permissions, scheduling, edge cases | `docs/PRD.md` |
| Phases, tasks, library versions, checklists | `docs/PLAN.md` |
| Visual design, screens, components, tokens, copy, date formats, accessibility | `podsjetnik-handoff/podsjetnik-handoff/HANDOFF.md` §6–§9 + `design/screens/*.png` (screenshots beat `design/source`) |
| Decisions where PRD and handoff differ | `docs/PRD.md` §0.2 (H1–H17) |

If something is not covered by these, ask. Don't invent behavior.

## Stack
Kotlin, Jetpack Compose, Material 3, single module `app`, package `hr.podsjetnik`. minSdk 35, targetSdk/compileSdk 37. Room 3 (`androidx.room3`) + KSP, DataStore Preferences, Navigation Compose, WorkManager (safety net only), kotlinx.serialization, java.time. Versions live only in `gradle/libs.versions.toml`.

## Layout
```
app/src/main/java/hr/podsjetnik/
  domain/      pure Kotlin: recurrence, occurrences, status, reminder planner. NO android.* imports.
  data/        Room, DataStore, TaskRepository, backup
  reminders/   ReminderEngine, AlarmScheduler, receivers, NotificationFactory, PermissionChecker
  ui/          theme/, components/, danas/, svizadaci/, detalji/, uredi/, postavke/, onboarding/
app/src/test/         JVM unit tests
app/src/androidTest/  on-device Room tests
```

## Workflow (every phase, every task)
1. **Reconnaissance first.** Read the relevant files and PRD/PLAN/HANDOFF sections. Then state what you will change (files, functions) **before** changing anything.
2. Implement only the current phase from `docs/PLAN.md`.
3. Run unit tests (`.\gradlew test`); fix failures before reporting.
4. Report: what changed, test results, the phase's manual checklist for me to run on the phone. Then **stop** and wait.

## Commands (Windows, PowerShell, repo root)
```powershell
.\gradlew assembleDebug          # build debug APK
.\gradlew installDebug           # install on the connected phone
.\gradlew test                   # JVM unit tests
.\gradlew connectedAndroidTest   # on-device tests (phone connected)
adb devices
adb shell dumpsys alarm | Select-String hr.podsjetnik -Context 0,4
adb logcat -s Podsjetnik
```
I build and run from Android Studio. CLI commands are for verification.

## Rules
- All reminder timing goes through `ReminderEngine.refresh()` → planner → **one** exact alarm (PRD §8.1). Every data or settings change calls refresh.
- All time logic uses java.time with an injected `Clock`. Wall-clock fields (`LocalDate`, `LocalTime`) stay wall-clock; real moments are `Instant`.
- Every planner behavior change comes with a unit test. PRD §8.5 timelines must stay green.
- Every Room schema change ships a migration and an updated exported schema in `app/schemas/`.
- All UI text in `res/values/strings.xml` (Croatian), plurals via `<plurals>` + `pluralStringResource` / `getQuantityString`.
- Colors only from `PodsjetnikColors` tokens; typography only from the theme. Dynamic color stays off.
- Helper functions go in a separate `*Helpers.kt` file next to their screen/feature, never in the same file as a composable or screen.
- Follow `CODING_STANDARDS.md`.

## Prohibitions
- **No `INTERNET` permission**, no network code, analytics, crash reporting or third-party SDKs.
- **No `android.*` imports in `domain/`.** No `LocalDate.now()`, `Instant.now()`, `System.currentTimeMillis()` outside the injected `Clock`.
- **No WorkManager, `Handler` or coroutine `delay` for reminder timing.** Only the single exact alarm. WorkManager is the 6-hour safety net only.
- **No per-task alarms**, no `setRepeating`, no `setAlarmClock` in product code (spike comparison only).
- **No foreground services, no wake locks.**
- **No `fallbackToDestructiveMigration`.** Never drop user data.
- **Never change a notification channel's ID or settings after Phase 1.** A changed channel needs a new ID.
- **No hardcoded UI strings or colors** in composables.
- **No Hilt/Koin/DI framework.** Wiring lives in `AppContainer`.
- **No new dependency** without asking first and adding it to `libs.versions.toml` and `docs/PLAN.md` §2.
- **Don't commit** `keystore.properties`, `*.jks`, `local.properties`, or build output.
- **Don't work ahead** of the current phase, and don't refactor unrelated code.
- **Don't commit or push** unless asked.
- **No `Co-Authored-By` or any Claude attribution** in commits or PRs. The only author is Aleksandar Rijić <aleksandarrijic@gmail.com>.
