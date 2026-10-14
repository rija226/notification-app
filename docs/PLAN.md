# Podsjetnik — Implementation Plan

| | |
|---|---|
| Status | Draft v0.2, written against PRD v0.2 + design handoff, awaiting approval |
| Date | 2026-10-14 |
| Inputs | [PRD.md](PRD.md) (behavior, data model) · [HANDOFF.md](../podsjetnik-handoff/podsjetnik-handoff/HANDOFF.md) (design, screens, copy, test data) |

Handoff paths below are relative to `podsjetnik-handoff/podsjetnik-handoff/` (abbreviated `handoff/`).

---

## 1. Approach

- **Riskiest first.** Phase 1 is a spike that proves, on the real Samsung, that exact alarms fire with the app closed, that ongoing notifications with buttons work, that swiped notifications come back, and that everything survives a reboot. The handoff's build order puts reminders after the screens. This plan deliberately does not, because the brief asks for the risk to be retired first.
- **Pure logic before wiring.** Recurrence and the reminder planner are plain Kotlin with an injected `Clock`. They are tested against PRD §8.5 and HANDOFF §11 before any real notification depends on them.
- **Every phase ends with an APK installed and checked on the phone** using its manual checklist.
- **Every phase starts with reconnaissance:** read the relevant files and the handoff sections, then state what will change before changing anything.

| Phase | Result on the phone | Size |
|---|---|---|
| 0 | Empty app with the real theme (Figtree, tokens, light/dark), signed with my key | S |
| 1 | **Spike:** test reminders fire reliably with the app closed, through Doze and reboot; buttons, Poništi and re-post work | M |
| 2 | Create Stalni/Povremeni tasks, see them in Svi zadaci with correct dates; data persists | M |
| 3 | Detalji shows "sljedeći podsjetnik" from the real planner | M |
| 4 | **Live reminders**: all stages, actions, Poništi, snooze, quiet hours, morning summary | L |
| 5 | All screens matching the screenshots, light and dark | L |
| 6 | Postavke, onboarding (3 steps incl. Samsung), permissions, Dijagnostika | M |
| 7 | Backup export/import | S |
| 8 | Polish, accessibility, 7-day soak test → **v1** | M |

---

## 2. Toolchain and libraries

Verified 2026-10-14 from the official release pages in §8. Phase 0 creates the project from Android Studio's current *Empty Activity* template; if the template pins something newer, keep the newer version.

**▸ Android:** *Gradle* is the build tool (npm scripts + bundler); *AGP* is its Android plugin. Dependencies live in a *version catalog* `gradle/libs.versions.toml`, the counterpart of the dependency list in `package.json`. *KSP* is a compile-time code generator (like a Babel plugin) used by Room.

| Tool / library | Version | Purpose |
|---|---|---|
| Android Studio | latest stable (Rabbit 1, 2026.2.1 at time of writing) | IDE, APK install |
| Android Gradle Plugin | **9.4.0** | Build; supports API 37; needs Gradle 9.6.0 and JDK 17 (bundled with Studio). AGP 9 has **built-in Kotlin**, so there is no separate `kotlin-android` plugin. |
| Gradle | **9.8.0** (wrapper) | AGP 9.4 needs ≥ 9.6.0; 9.8.0 was already on the machine |
| Kotlin | **2.4.21** | Also the version of the Compose compiler and serialization plugins |
| KSP | **2.3.12** | Room code generation |
| Compose BOM | **2026.09.00** | Compose UI + Material 3 versions |
| androidx.activity:activity-compose | **1.13.0** | Single Activity hosting Compose, edge-to-edge |
| androidx.lifecycle (viewmodel-compose, runtime-compose) | **2.11.0** | ViewModels, state collection |
| androidx.navigation:navigation-compose | **2.10.2** | Routes from HANDOFF §6 |
| androidx.room3 (runtime, compiler, gradle plugin) | **3.0.3** | Database. Kotlin-first, KSP-only, coroutine-first. Fallback: Room 2.8.5 (nearly identical API, package `androidx.room`). |
| androidx.datastore:datastore-preferences | **1.2.1** | Settings (PRD FR-8) |
| androidx.work:work-runtime | **2.12.0** | 6-hour safety-net refresh |
| androidx.core:core-ktx | **1.19.1** | `NotificationCompat`, helpers |
| kotlinx-serialization-json | **1.11.0** | Backup, rule/time-list columns (1.12.0 is still RC) |
| kotlinx-coroutines-test | **1.11.0** | Tests for suspend code |
| JUnit | **4.13.2** | JVM unit tests |
| androidx.test.ext:junit | **1.3.0** | On-device Room tests |
| Figtree font (Google Fonts, OFL) | weights 400/500/600/700 | Bundled TTF in `res/font` (HANDOFF §2) |

Not used: Hilt/Koin, Navigation 3, Accompanist, Robolectric, any network/analytics library.
SDK levels: `minSdk 35`, `targetSdk 37`, `compileSdk 37`.

---

## 3. Project structure

Single module `app`, package `hr.podsjetnik`, layout following HANDOFF §2.

```
app/src/main/
  AndroidManifest.xml
  java/hr/podsjetnik/
    PodsjetnikApp.kt            Application: AppContainer, channels
    AppContainer.kt             manual wiring (db, settings, repository, engine, clock)
    MainActivity.kt             Compose host, notification deep link
    domain/                     PURE KOTLIN, no android.* imports
      RecurrenceRule.kt         sealed rules + firstOnOrAfter / nextAfter
      Occurrences.kt            ensureOccurrences, done / skip / undo / pause / resume
      TaskStatus.kt             status + chip state (PRD §5.4)
      ReminderPlanner.kt        PRD §8.2: slots, grid, escalation, snooze, quiet hours, summary
      QuietHoursHelpers.kt
    data/
      AppDatabase.kt, Entities.kt, Daos.kt, Converters.kt
      SettingsStore.kt          DataStore
      TaskRepository.kt         the only data entry point for UI and engine
      backup/BackupFormat.kt, BackupRepository.kt
      DebugSeed.kt              loads sample-tasks.json (debug builds only)
    reminders/
      ReminderEngine.kt         refresh(): ensure → plan → notify → schedule
      AlarmScheduler.kt         the single exact alarm
      ReminderReceiver.kt       alarm fired
      NotificationActionReceiver.kt  Odrađeno / Poništi / Odgodi / swipe
      BootReceiver.kt           BOOT_COMPLETED, TIME_SET, TIMEZONE_CHANGED, MY_PACKAGE_REPLACED
      SafetyNetWorker.kt
      NotificationFactory.kt    channels + builders (PRD §9)
      NotificationHelpers.kt    titles, plurals, date text
      PermissionChecker.kt
    ui/
      theme/                    Color.kt (from handoff tokens), Type.kt (Figtree), Theme.kt
      components/               HANDOFF §7.4 components
      AppNavHost.kt             routes + bottom navigation
      danas/  svizadaci/  detalji/  uredi/  postavke/  onboarding/
                                each: XScreen.kt, XViewModel.kt, XHelpers.kt
  res/values/strings.xml        from handoff/android/strings.xml + new strings (PRD §14)
  res/font/                     Figtree TTFs
app/src/test/                   JVM unit tests
app/src/androidTest/            Room DAO, migration, import tests
app/schemas/                    exported Room schemas (committed)
```
Files are created when a phase needs them, not up front.

---

## 4. Testing approach

| Level | What | Where | Run |
|---|---|---|---|
| Unit (JVM) | Recurrence, occurrence transitions, status, planner (PRD §8.5 A–G + HANDOFF §11 adjusted for H3), quiet hours, DST `Europe/Zagreb`, helpers, backup format | `app/src/test` | Studio ▶ or `.\gradlew test` |
| Instrumented | Room DAOs, migrations, import transaction | `app/src/androidTest` | `.\gradlew connectedAndroidTest` (phone connected) |
| Manual | Alarms, Doze, reboot, notification UI, Samsung settings, visual match with screenshots | per-phase checklist | phone |

Fixed test clock from the handoff: **Thursday 2026-11-05 10:24, Europe/Zagreb**, seed `handoff/data/sample-tasks.json`.

**▸ Android:** *adb* talks to the phone over USB/Wi-Fi; it is at `%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe`.
```powershell
adb devices
adb shell dumpsys alarm | Select-String hr.podsjetnik -Context 0,4   # our pending alarm
adb shell dumpsys deviceidle force-idle                              # force Doze (screen off first)
adb shell dumpsys deviceidle unforce
adb reboot
adb logcat -s Podsjetnik
```
Time and time-zone tests: switch off automatic date/time in phone Settings and change it by hand.

---

## 5. Phases

Loop for each phase: **reconnaissance → state planned changes → implement → unit tests green → install → manual checklist → I review → next phase.**

### Phase 0 — Project setup and theme

**Goal:** an app that builds on Windows, installs on my phone, uses the real design theme, and is signed with my own key.

**Tasks**
1. Android Studio → *Empty Activity* (Compose), name Podsjetnik, package `hr.podsjetnik`, Kotlin DSL, minSdk 35; set target/compile 37; align `libs.versions.toml` with §2 (only what this phase needs).
2. **Signing key** `podsjetnik-signing.jks` **outside the repo** + a backup copy. Debug and release both use it via a gitignored `keystore.properties`. **▸ Android:** updates must be signed with the same key. The default debug key lives on one PC, so losing it means uninstalling, which deletes all data.
3. Theme: `handoff/design/tokens/Color.kt` → `ui/theme`, Figtree TTFs in `res/font`, typography per HANDOFF §7.2, dynamic color off, edge-to-edge.
4. Copy `handoff/android/strings.xml` to `res/values/strings.xml`.
5. Placeholder screen showing "Danas" title in Display style (to check font and colors).
6. `CLAUDE.md`, `CODING_STANDARDS.md` (drafted after this plan is approved), Android `.gitignore`. Repo already initialized with `git init`.

**Tests:** template unit test runs.
**Manual checklist**
- [ ] App installs from Studio and opens
- [ ] Title renders in Figtree; colors match `01-danas-light.png` background/text
- [ ] Phone in dark mode → dark tokens
- [ ] Content draws behind the status bar with correct insets (no fake status bar)
- [ ] `.\gradlew assembleRelease` APK installs over the debug build (same key)
- [ ] Keystore backup exists outside the PC

### Phase 1 — Reliability spike (riskiest part)

**Goal:** prove on the Samsung that the reminder mechanism works, and record the Samsung settings paths. Spike UI is temporary. Channels, scheduler and receivers become production code in Phase 4.

**Tasks**
1. Manifest: `USE_EXACT_ALARM`, `POST_NOTIFICATIONS`, `RECEIVE_BOOT_COMPLETED`, `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`. No `INTERNET`.
2. `NotificationFactory`: create the **final** channels `rani`, `na_dan`, `kasni`, `sazetak` (PRD §9.1). Channel settings stick on the phone once created.
3. Room with `AlarmLogEntity` only. Spike "test reminders" are AlarmLog rows not yet fired.
4. `AlarmScheduler`: the earliest unfired row is **the single alarm**. Mode A `setExactAndAllowWhileIdle` (product); mode B `setAlarmClock` (comparison only).
5. `ReminderReceiver`: record `firedAt`, post an **ongoing** notification on `kasni` with **Odrađeno**, **Odgodi 1 sat** and a delete intent, then schedule the next row.
6. `NotificationActionReceiver` (with `goAsync()`): Odrađeno → "Odrađeno · Poništi" for 10 s; Poništi → re-post; Odgodi → new row in 2 min (spike value); swipe → immediate silent re-post.
7. `BootReceiver`: boot / time / time zone / package replaced → reschedule.
8. Spike screen: *Test za 2 min*, *Test za 15 min*, *Noćni niz (svakih 30 min × 16)*, mode A/B switch, status rows (notifications, exact alarms, battery) with fix buttons, log list with **delay = firedAt − scheduledFor**.

**Tests:** unit test for the delay-format helper.
**Manual checklist** (results go into `docs/spike-results.md`)
- [ ] One UI + Android versions recorded
- [ ] Mode A, app swiped from Recents, screen off: *Test za 15 min* within 1 min
- [ ] Mode A, `force-idle`: fires on time
- [ ] Mode A, overnight series, phone idle on the desk: all 16 fire, max delay recorded
- [ ] Mode B, same three tests + whether an alarm icon / "next alarm" appears
- [ ] Reboot with a pending test → fires after first unlock
- [ ] Odrađeno from the **lock screen** works without opening the app; Poništi within 10 s restores it
- [ ] Odgodi from the shade works
- [ ] Locked: ongoing notification can't be swiped; *Clear all* keeps it
- [ ] Unlocked: swipe → immediately back, **no sound**
- [ ] Clock moved forward past a pending test → fires at once
- [ ] Battery dialog grants the exemption; App info → Battery shows "Unrestricted"?
- [ ] Croatian menu paths recorded: *Background usage limits*, *Never auto sleeping apps*, *Put unused apps to sleep*
- [ ] Negative test: app in *Deep sleeping apps* → 15-min test fails; then fix and confirm it works
- [ ] Force stop → test gone; opening the app reschedules

**Exit criteria:** mode A confirmed (or B adopted, PRD §10.2 updated); worst delay ≤ 2 min with recommended settings; Samsung paths written into PRD §10.5 and the onboarding copy.
**If it fails:** stop and re-plan before building anything else.

### Phase 2 — Data, recurrence, task list

**Goal:** real tasks with correct dates, persisted.

**Tasks**
1. `domain/RecurrenceRule.kt` (`@Serializable` sealed: `DayOfMonth(1..28)`, `LastDayOfMonth`).
2. `domain/Occurrences.kt`: ensure / done / skip / undo / pause / resume / lateMinutes (PRD FR-3).
3. `domain/TaskStatus.kt` (PRD §5.4 incl. "Rok istekao").
4. Room: `TaskEntity`, `OccurrenceEntity`, converters, schema export, migration 1→2 (keeps the spike's AlarmLog). `SettingsStore` (DataStore, PRD FR-8 defaults).
5. `TaskRepository` (Flows for reads, transactions for writes).
6. `AppNavHost` with bottom navigation (Danas, Svi zadaci, Postavke) and routes from HANDOFF §6.
7. First real screens, functional but not yet pixel-matched: **Svi zadaci** (Stalni / Povremeni tabs) and **Novi zadatak** (title, type, rule / date + time).
8. `DebugSeed` loads `sample-tasks.json` in debug builds (menu item in the spike screen).

**Tests (unit):** recurrence (all month lengths; 28. 2. 2027, 29. 2. 2028, 28. 2. 2100; inclusive/exclusive; year end; JSON round-trip), HANDOFF §11 tests 3 and 4, occurrence transitions (done early → next; still open the day after → next created; skip; undo deletes untouched next and keeps touched; pause/resume; 3 months off → 3 open; one-off → archived), status table incl. "Rok istekao".
**Tests (instrumented):** DAO queries; migration 1→2.
**Manual checklist**
- [ ] "Očitati struju", zadnji dan → due 31. 10. 2026
- [ ] "Svakog 10." created on 14. 10. → due 10. 11.
- [ ] Povremeni with date + time listed under Povremeni
- [ ] Empty title → "Upiši naziv zadatka."
- [ ] Debug seed loads the 5 sample tasks
- [ ] App killed and reopened → data still there; spike log survived the migration

### Phase 3 — Reminder planner + "sljedeći podsjetnik"

**Goal:** the complete reminder logic, proven by tests; visible as the next reminder time on Detalji.

**Tasks**
1. `QuietHoursHelpers` (on/off, wrap, defer, collapse).
2. `ReminderPlanner` exactly per PRD §8.2: early, day times, overdue moment (H3), grid with anchor and escalation, snooze, quiet hours, missed-slot collapse, stage, summary slot, `nextWakeAt`, sound policy.
3. Minimal **Detalji**: title, type line, status chip, "sljedeći podsjetnik u …", open occurrences.

**Tests (unit)** with `Clock.fixed` + `ZoneId.of("Europe/Zagreb")`:
- One test per PRD §8.5 timeline **A–G**, stepping the clock through each row (posted? channel? sound?).
- HANDOFF §11 tests 1, 2, 5–11. Test 10 expects a reminder at the due time, which under H3 is the first **overdue** reminder. Test 11's due-day part follows H3.
- Quiet hours: switched off; 22–07 wrap; custom 23–06; slot exactly at start/end; snooze into quiet hours.
- DST: 25. 10. 2026 and 28. 3. 2027 with quiet hours narrowed so slots hit the gap/overlap.
- Catch-up (3 missed slots → 1 post); idempotency (same `now` twice → nothing new); edit with pending reminders (PRD §8.4); paused → nothing; closed → cancel; sound policy (07:00: overdue sounds, summary silent).

**Manual checklist**
- [ ] Debug seed at real time: each task's "sljedeći podsjetnik" is plausible and never inside 22–07
- [ ] New task due today with a time 1 h ahead shows the next day time or the due time
- [ ] Changing an overdue task's interval 2 h → 1 h updates the next time

### Phase 4 — Live reminders end to end

**Goal:** real tasks produce exactly the notifications in PRD §8–9 with the app closed.

**Tasks**
1. `ReminderEngine.refresh()` (PRD §8.1), serialized with a mutex.
2. Receivers switched from spike logic to `refresh()`; actions call repository transitions.
3. `NotificationFactory` + `NotificationHelpers`: copy per HANDOFF §5.3 + PRD §9.2 (month suffix, "Rok je istekao", interval text, done state, summary inbox), accent colors, deep link to Detalji.
4. Morning summary (once per date).
5. Refresh on app resume, every repository write and settings change; `SafetyNetWorker` every 6 h.
6. Alarm log records production fires (trim to 500).

**Tests (unit):** `NotificationHelpers`: plural forms (1/2/5/21 dan; zadatak/zadatka/zadataka; kasni/kasne), interval text ("svaki sat", "svaka 2 sata"), summary variants.
**Manual checklist** (test task with day times a few minutes apart)
- [ ] Early: silent, with buttons
- [ ] Due: sound; repeats at the next day time
- [ ] Overdue: heads-up, vibration, ongoing; not swipeable when locked
- [ ] Odrađeno from the lock screen → Poništi for 10 s → gone; Detalji shows the next occurrence
- [ ] Poništi restores the reminder
- [ ] Odgodi 1 sat → back after 1 h
- [ ] Swipe overdue (unlocked) → immediately back, silent; next reminder sounds on schedule
- [ ] Quiet hours starting in 5 min: reminders collapse to the end, together with the summary, **one** sound
- [ ] No summary on a day with nothing due/overdue
- [ ] Reboot with pending reminder → arrives after unlock
- [ ] Time zone change → reminders keep local times
- [ ] Two tasks at the same minute → two notifications, one sound
- [ ] App swiped from Recents for a full day → alarm log shows every reminder on time

### Phase 5 — Screens matching the design

**Goal:** all screens from HANDOFF §6 matching screenshots 01–06, 09 in light and dark, plus the PRD FR-7 additions.

**Tasks**
1. Components from HANDOFF §7.4 in `ui/components` (StatusChip incl. "Rok istekao", TaskCard, TaskListRow, TypeTile, DoneButton, TimeChip, DayOfMonthGrid, InlineMonthCalendar, Stepper, SnoozeSheet, DoneDialog, EmptyState, …).
2. **Danas** (01, 09): header summary, three sections, sorting (HANDOFF §4.2), empty state with "Sljedeće".
3. **Svi zadaci** (05a–c): tabs with counts, "Pauzirano" badge, Arhiva rows + read-only Detalji.
4. **Detalji** (02): full layout + **⋮ menu** (Preskoči ovaj mjesec, Pauziraj/Nastavi), delete dialog, oldest-occurrence rule.
5. **Novi / Uredi** (06a, 06b): full form incl. day grid, inline calendar, stepper, time chips, interval segments, live "Sljedeći termin".
6. **Odgodi** sheet (03), **Odrađeno** dialog (04, auto-dismiss ~4 s), snackbar for Povremeni.
7. Remove the spike screen (status rows move to Postavke in Phase 6).

**Tests (unit):** helpers for Danas grouping and sorting, header summary text, form validation, Detalji texts ("2 dana prije, u 09:00", "sve poslano 31. 10.").
**Manual checklist**
- [ ] With debug seed + clock at 5. 11. 10:24 (set phone time), each screen matches its light and dark screenshot
- [ ] Empty states (Danas, Arhiva)
- [ ] Odrađeno on a Stalni card → dialog with next date → Poništi works
- [ ] Odrađeno on a Povremeni card → snackbar → Poništi works
- [ ] ⋮ Preskoči ovaj mjesec → next month; ⋮ Pauziraj → leaves Danas, notification removed; Nastavi → back
- [ ] Editing day times with a visible notification → no immediate extra post
- [ ] Notification tap opens the right Detalji; back navigation and predictive back behave
- [ ] Font size at max: no clipped text (HANDOFF §9)

### Phase 6 — Postavke, onboarding, device health

**Goal:** configuration and an app that tells me when the phone will block reminders.

**Tasks**
1. **Postavke** (07): Dozvole (+ Samsung row), Tihi sati with switch, Zadano za nove zadatke, Sigurnosna kopija placeholders, **Dijagnostika** row.
2. `PermissionChecker` + status mapping (PRD FR-9), re-checked in `onResume`.
3. **Onboarding** 3 steps: Obavijesti, Baterija, **Samsung** (new content in the 08b/08c layout, menu paths from Phase 1). Granted steps auto-skip.
4. **Dijagnostika** screen and the >5-min-late warning.
5. Danas banner for blocking problems.

**Tests (unit):** settings validation (quiet start ≠ end, 1–6 day times), permission → status mapping.
**Manual checklist**
- [ ] Fresh install → onboarding; each button opens the right system screen; status updates on return
- [ ] Notification permission revoked → Danas banner + Postavke row
- [ ] `kasni` channel disabled → warning
- [ ] Quiet hours switched off → night reminders fire; summary still at "Do" time
- [ ] Changing defaults affects only new tasks
- [ ] Dijagnostika lists real fires with delays

### Phase 7 — Backup

**Goal:** export all data to JSON and import it back.

**Tasks:** `BackupFormat` (PRD FR-10), export via `CreateDocument`, import via `OpenDocument` with validation, confirmation, single transaction, notification cleanup, refresh; Postavke rows "Izvezi podatke" / "Uvezi podatke".
**Tests:** unit round-trip, invalid JSON, `version: 2` rejected; instrumented: failed import leaves old data intact.
**Manual checklist**
- [ ] Export to Downloads, readable JSON
- [ ] Uninstall → reinstall → import → identical tasks, history, settings; reminders resume
- [ ] Corrupted file → message, data unchanged

### Phase 8 — Polish, hardening, v1

**Tasks:** dark-mode pass against all `*-dark.png`; accessibility pass (HANDOFF §9: content descriptions, 48 dp, 200 % font); motion (fade + shrink ~200 ms); remove dead code; release build with R8 verified (receivers and serialization survive shrinking); **7-day soak test** with the release APK; short `README.md` (build, install, keystore, backup advice).
**Manual checklist**
- [ ] 7 days: no missed reminders, max delay ≤ 2 min outside quiet hours (alarm log)
- [ ] All PRD acceptance criteria and the HANDOFF §13 checklist ticked
- [ ] Release APK installs over the previous build without data loss

---

## 6. Risks

| Risk | Impact | Mitigation |
|---|---|---|
| Samsung delays or kills alarms despite settings | Core value lost | Phase 1 first; mode B fallback; alarm log + warnings |
| Room 3 is a young major version | Fewer examples | Fallback Room 2.8.5, decision point in Phase 2 |
| Channel settings stick after first install | Wrong sound "forever" | Final channels defined in Phase 1; changes need new IDs |
| Signing key lost | Can't update → data loss | Own keystore + backup (Phase 0) and JSON backup (Phase 7) |
| Planner subtleties (grid × escalation × snooze × quiet hours × DST) | Wrong or missing reminders | Pure function; every PRD timeline and handoff scenario as a test (Phase 3) |
| New UI pieces not in the design (⋮ menu, Samsung step, Dijagnostika, "Rok istekao", "Pauzirano") | Inconsistent look | Built only from existing tokens/components; review screenshots in Phase 5/6 |

---

## 7. After approval
1. Draft `CLAUDE.md` (stack, package layout, source-of-truth rule from PRD §0.1, prohibitions, reconnaissance-first workflow, Windows/Android Studio commands) and `CODING_STANDARDS.md` (official Kotlin conventions + helper-file rule + Compose/ViewModel/Room/test conventions).
2. Start Phase 0.

---

## 8. Sources (versions, accessed 2026-10-14)
- AGP 9.4.0 release notes: https://developer.android.com/build/releases/agp-9-4-0-release-notes
- Built-in Kotlin (AGP 9): https://developer.android.com/build/migrate-to-built-in-kotlin
- Android Studio releases: https://developer.android.com/studio/releases
- Compose BOM: https://developer.android.com/develop/ui/compose/bom
- Room 3: https://developer.android.com/jetpack/androidx/releases/room3 · Room 2→3 migration: https://developer.android.com/training/data-storage/room/migration-2-to-3 · Room 2: https://developer.android.com/jetpack/androidx/releases/room
- Navigation: https://developer.android.com/jetpack/androidx/releases/navigation
- DataStore: https://developer.android.com/jetpack/androidx/releases/datastore
- Lifecycle: https://developer.android.com/jetpack/androidx/releases/lifecycle
- Activity: https://developer.android.com/jetpack/androidx/releases/activity
- Core: https://developer.android.com/jetpack/androidx/releases/core
- WorkManager: https://developer.android.com/jetpack/androidx/releases/work
- AndroidX Test: https://developer.android.com/jetpack/androidx/releases/test
- Kotlin: https://github.com/JetBrains/kotlin/releases · KSP: https://github.com/google/ksp/releases
- kotlinx.serialization: https://github.com/Kotlin/kotlinx.serialization/releases · kotlinx.coroutines: https://github.com/Kotlin/kotlinx.coroutines/releases
- JUnit 4: https://repo1.maven.org/maven2/junit/junit/maven-metadata.xml
- Figtree: https://fonts.google.com/specimen/Figtree
- Platform behavior: [PRD §15](PRD.md#15-sources)
