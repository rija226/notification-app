# Podsjetnik — Product Requirements Document

| | |
|---|---|
| Status | Draft v0.2, all open decisions resolved (2026-10-14), awaiting approval |
| Platform | Android 15+ (Samsung, One UI), sideloaded APK |
| Related | [PLAN.md](PLAN.md) · Design handoff: [HANDOFF.md](../podsjetnik-handoff/podsjetnik-handoff/HANDOFF.md) + `design/`, `android/strings.xml`, `data/sample-tasks.json` in the same folder |

Android-specific terms are explained in a short **▸ Android:** note the first time they appear. Platform facts that change over time are cited in [§15 Sources](#15-sources), all checked on 2026-10-14.

---

## 0. How this PRD relates to the design handoff

The handoff package (from Claude Design) defines screens, components, design tokens, copy and test data. In several places it also defines behavior that differs from the original brief. Each difference was decided on 2026-10-14 (see §0.2).

### 0.1 Source of truth
| Topic | Source of truth |
|---|---|
| Visual design, layout, components, tokens, typography, copy (`strings.xml`), date formats, accessibility | **HANDOFF.md** §6–§9 + screenshots (screenshots beat `design/source`) |
| Reminder behavior, data model, permissions, scheduling, edge cases | **This PRD** |
| Implementation order and versions | **PLAN.md** |

Where HANDOFF.md describes behavior and this PRD does not contradict it, HANDOFF.md applies.

### 0.2 Decisions log (2026-10-14)

| # | Topic | Decision | Effect on the handoff |
|---|---|---|---|
| H1 | Data model | **One row per occurrence**: a missed month and the new month are open and shown at the same time. Skip, lateness and v2 value/photo are stored on the occurrence. | Replaces handoff §3 model (`dueDate` on the task + `completions`). UI unchanged. |
| H2 | Overdue cadence | **Handoff:** per-task interval 1 / 2 / 4 h (default 2 h), halved (min 1 h) from day 3 late, on a fixed daily grid. | Brief's "4 h then 3 h" dropped. |
| H3 | Overdue moment | **With a due time: overdue at that time.** Without: from 00:00 the next day. | Replaces handoff O4 (next calendar day). Adds chip state "Rok istekao" (overdue on the due day itself). |
| H4 | Swiped-away overdue notification | **Re-posted immediately, silently.** | Handoff O2 kept. Brief's "back after 30 min" dropped. |
| H5 | Odrađeno from the notification | Notification turns into "Odrađeno: ‹naslov›" with **Poništi for 10 s**. | Replaces handoff "cancel, no UI". |
| H6 | Skip a month / Pause | **Both in v1**, in a ⋮ menu on Detalji (Stalni only). Built from existing components and tokens. | Addition (not in handoff). |
| H7 | Pausing a task with an open overdue occurrence | Occurrence **stays open and silent**; reminds again after resume. | Addition. |
| H8 | Morning summary | **Yes.** One per day at the end of quiet hours. When several notifications arrive together, **only the most urgent makes a sound**. | Addition: 4th notification channel. |
| H9 | Channels | Early **silent**, due day **normal sound**, overdue **sound + vibration + heads-up**, summary normal. Notification sound stream (not alarm stream). | Replaces handoff §5.3 importances (early was audible). |
| H10 | Exact alarm permission | **`USE_EXACT_ALARM`** (granted at install). Onboarding step "Točni alarmi" is replaced by a **Samsung** step ("Ne uspavljuj aplikaciju"). | Replaces handoff §5.4 `SCHEDULE_EXACT_ALARM` and screen 08b content. |
| H11 | minSdk | **35** (Android 15). | Replaces handoff minSdk 26. |
| H12 | Diagnostics | **Alarm log** as one row in Postavke → simple list screen. | Addition. |
| H13 | Delete a Stalni task | Deletes the task **and its history** (with confirmation). | Same as handoff. |
| H14 | Arhiva | View and delete only, **no "Vrati"**. | Handoff's optional "Vrati" dropped. |
| H15 | Application ID | **`hr.podsjetnik`** | — |
| H16 | Repo | `git init` in this folder. Handoff package stays where it is (`podsjetnik-handoff/podsjetnik-handoff/`). | — |
| H17 | Stack details | **Accepted from the handoff:** settings in DataStore, Navigation Compose, Figtree font bundled, dynamic color off, edge-to-edge, bottom navigation, quiet-hours on/off switch, default early reminder 2 days, "Večeras" hidden from 19:30, Odrađeno dialog (auto-dismiss ~4 s) for Stalni and snackbar for Povremeni, backup format, tests with fixed clock 2026-11-05 10:24. | — |

---

## 1. Problem and goals

**Problem.** I forget recurring chores (meter reading, bills) and one-off tasks. Normal to-do apps notify once and go quiet.

**Goal.** A personal reminder app that **keeps reminding until I explicitly mark a task done**, with escalating urgency, respect for sleep, and reliable delivery on a Samsung phone even when the app is closed. Reliability is feature #1.

**Success criteria (v1)**
1. Over a 7-day test on my phone, no reminder is lost, and every reminder outside quiet hours arrives within 2 minutes of its planned time (measured by the alarm log, §8.6).
2. An open, overdue, non-paused task is never silent for longer than its overdue interval outside quiet hours.
3. Marking done from the notification takes one tap and never opens the app.
4. Reminders survive app close, reboot, time change and time zone change.

**Non-goals (v1):** accounts, server, cloud sync, sharing, iOS, Google Play. The app declares **no `INTERNET` permission**.

---

## 2. User and context
- One user, one Samsung phone, Android 15+, Croatian UI.
- Sideloaded APK, never on Google Play, so Play policies (e.g. on `USE_EXACT_ALARM`) don't apply.
- Developed on Windows in Android Studio by a React/TypeScript developer new to Kotlin/Android.

---

## 3. Key technical decisions

| # | Decision | Why | Details |
|---|---|---|---|
| D1 | minSdk 35, targetSdk/compileSdk 37 | Only my Android 15+ phone; API 37 is the newest AGP 9.4 supports | §10.1 |
| D2 | `USE_EXACT_ALARM` + `setExactAndAllowWhileIdle()` | Granted at install; fires in Doze | §10.2 |
| D3 | **One pending alarm for the whole app** | A pure function computes the next moment anything must happen; every change runs the same idempotent refresh. Simultaneous reminders are batched, which also avoids the Doze limit for exact alarms (once per 9 min per app). | §8.1 |
| D4 | One notification **per open occurrence**, updated in place | Escalates early → due → overdue under one ID. Two open months = two notifications. | §9 |
| D5 | Occurrence row = completion record | v2 history needs no migration | §11 |
| D6 | Recurrence rule stored as JSON of a sealed type | New rule types in v2 need no migration | §6 FR-2, §11 |
| D7 | No DI framework; one manual `AppContainer` | ~10 objects to wire | §10.6 |

---

## 4. User stories

| ID | As the user, I want… | So that… |
|---|---|---|
| US-1 | a recurring task on a fixed day of month or the last day of month | monthly chores repeat by themselves |
| US-2 | a one-off task with a date and optional time | I can track single tasks |
| US-3 | a quiet early reminder a few days ahead | I can plan |
| US-4 | repeated reminders on the due day until done | one notification isn't enough to forget |
| US-5 | more frequent, louder reminders the longer a task is overdue | overdue tasks get my attention |
| US-6 | to mark done from the notification and undo an accidental tap | it's fast and safe |
| US-7 | to snooze (1 sat, Večeras, Sutra ujutro) | I can defer without losing it |
| US-8 | silence at night and one morning summary | my sleep isn't disturbed |
| US-9 | to skip one month or pause a recurring task | I can handle exceptions |
| US-10 | today's, overdue and upcoming tasks on one screen | I know what to do today |
| US-11 | the app to tell me when a phone setting will block reminders | I can trust it |
| US-12 | export/import of all data | I don't lose data on reinstall or a new phone |

---

## 5. Concepts and definitions

### 5.1 Task and occurrence
- **Task (Zadatak):** title, optional note, type, reminder settings.
  - **Stalni** (recurring): has a recurrence rule.
  - **Povremeni** (one-off): has exactly one occurrence.
- **Occurrence (Pojava):** one concrete due instance, e.g. "Očitati struju, rok 31. 10. 2026". Reminders, snooze, done and skip apply to an **occurrence**. Edit, pause and delete apply to the **task**.
- Occurrence status: `OPEN`, `DONE`, `SKIPPED`.

### 5.2 Dates and times
Due date (calendar date) per occurrence. Optional due time per task (Povremeni only, as in the handoff form). All dates and times are **local wall-clock**, never fixed UTC. "09:00" stays 09:00 after a time zone change.

### 5.3 Overdue moment (H3)
- **With a due time:** overdue from that time on the due date.
- **Without a due time:** overdue from 00:00 the next day (inside default quiet hours, so the first overdue notification arrives at quiet-hours end). Done at 23:59 on the due day counts as on time.
- **Days late** = calendar days between the due date and today (0 on the due day itself).

### 5.4 Status (extends HANDOFF §4.2)

| Condition | Status | Chip |
|---|---|---|
| Overdue moment passed, days late ≥ 1 | OVERDUE | "Kasni N dan/dana" (red) |
| Overdue moment passed, days late = 0 (time passed today) | OVERDUE | **"Rok istekao"** (red) *new* |
| Due today, not overdue | TODAY | "Danas" (amber) |
| Due in 1–7 days | UPCOMING | "Za N dan/dana" |
| Due in > 7 days | LATER | "Za N dana" (Svi zadaci only) |
| Done / archived | DONE | "Odrađeno" (green) |
| Task paused | — | **"Pauzirano"** badge (neutral) *new*, Svi zadaci only |

### 5.5 Reminder stages

| Stage | When | Channel |
|---|---|---|
| Early (Rani) | `earlyDays` before due date at `earlyTime` (`earlyDays` 0–7, 0 = off) | `rani`, silent |
| Due day (Na dan roka) | each of the task's day times on the due date, until the overdue moment | `na_dan`, sound |
| Overdue (Kasni) | grid from the overdue moment until done (§8.2) | `kasni`, sound + vibration + heads-up, ongoing |

### 5.6 Quiet hours (Tihi sati)
Default **on**, 22:00–07:00, may cross midnight, can be switched off in Postavke. While on, nothing is posted inside the window; reminders inside it move to the end time ("Do") and collapse into one per occurrence. Start must differ from end.

---

## 6. Functional requirements

### FR-1 Task management
Fields and UI as in HANDOFF §6.6: title (required), type (fixed after creation), rule (Stalni), date + optional time (Povremeni), early reminder (0–7 days + time), day times (1–6, sorted, unique), overdue interval (1 / 2 / 4 h), note. New tasks prefill from settings defaults.
- **AC-1.1** A new Stalni task's first occurrence is the first rule date **on or after today** (created 5. 11. with "svakog 5." → 5. 11.; created 6. 11. → 5. 12.).
- **AC-1.2** Empty title → "Upiši naziv zadatka.", save blocked.
- **AC-1.3** Delete asks for confirmation, removes the task, all its occurrences (history included) and its notifications (H13).
- **AC-1.4** Editing schedule fields takes effect immediately for open occurrences, and nothing fires for the old schedule (§8.4).

### FR-2 Recurrence rules (v1)
- **Svakog N.** (1–28) and **Zadnji dan u mjesecu** (its own rule, never stored as 28–31).
- Isolated module: a sealed type with `firstOnOrAfter(date)` and `nextAfter(date)`, pure and fully unit-tested.
- **AC-2.1** Last day: 31. 1., 28. 2. 2027, 29. 2. 2028, 28. 2. 2100, 30. 4., 31. 12.
- **AC-2.2** The UI offers 1–28 and "Zadnji dan u mjesecu"; 29–31 cannot be selected.

### FR-3 Occurrence lifecycle
- **Odrađeno:** closes that occurrence (`DONE`, `completedAt`, `lateMinutes`). Stalni → the next occurrence is created immediately (`nextAfter(dueDate)`, or later if that date already exists) and shown in the Odrađeno dialog. Povremeni → task gets `archivedAt`, snackbar "Premješteno u arhivu" + Poništi.
- **Done early** (from Uskoro or Detalji): same; no further reminders for that occurrence.
- **Next occurrence by time:** if a Stalni occurrence is still open the day after its due date, the next occurrence is created that day. Both are open and shown: the old one in Zakašnjelo, the new one by its own date. Every missed month must be closed separately (handoff O3).
- **Preskoči ovaj mjesec** (⋮ menu, Stalni, H6): closes the occurrence as `SKIPPED` (no lateness), creates the next one. Applies to the oldest open occurrence of the task.
- **Pauziraj / Nastavi** (⋮ menu, Stalni, H6, H7): paused → no new occurrences, no reminders, notifications removed, hidden from Danas, "Pauzirano" badge in Svi zadaci. Open occurrences stay open and silent. Resume → reminders continue. If no open occurrence is due on or after today, one is created with `firstOnOrAfter(today)`. Months during the pause are not recorded.
- **Poništi:** reverts the last Done/Skip on that occurrence: back to `OPEN`, the auto-created next occurrence is deleted if untouched (still open, never snoozed), and a Povremeni leaves Arhiva.
- **AC-3.1** Done on 28. 10. for a last-day task → no more October reminders; next occurrence 30. 11.
- **AC-3.2** October still open on 1. 11. → Danas › Zakašnjelo shows October; Svi zadaci shows November's date.
- **AC-3.3** Poništi fully restores the previous state, including reminders.
- **AC-3.4** A paused task produces no notifications until resumed.

### FR-4 Reminder schedule
Algorithm in §8.2. Snooze per HANDOFF §4.3 point 5: **1 sat**, **Večeras (20:00)** (hidden from 19:30), **Sutra ujutro (09:00)**. The notification action is "Odgodi 1 sat". Snooze never changes the due date or status.
- **AC-4.1** Each timeline in §8.5 and each scenario in HANDOFF §11 is reproduced by a planner unit test. Where they conflict, §8.5 wins (H3).
- **AC-4.2** While quiet hours are on, nothing is posted inside them.
- **AC-4.3** Notifications posted in the same moment produce **one** sound (H8).

### FR-5 Notifications
Spec in §9.
- **AC-5.1** "Odrađeno" and "Odgodi 1 sat" work from the shade and the lock screen without opening the app.
- **AC-5.2** After "Odrađeno" the notification shows "Odrađeno: ‹naslov›" + **Poništi** for 10 s, then disappears (H5).
- **AC-5.3** An overdue notification can't be swiped while locked. Swiped while unlocked, it is re-posted immediately and silently (H4).
- **AC-5.4** Tapping a notification opens Detalji for that task.

### FR-6 Morning summary (Jutarnji sažetak, H8)
- One notification per day at the quiet-hours end time ("Do", default 07:00). If quiet hours are switched off, it still uses that time.
- Counts open occurrences **due today** and **overdue**, excluding paused tasks. Text: "Danas imaš 3 zadatka, 1 kasni" / "Danas imaš 2 zadatka" / "Kasni ti 1 zadatak" (Croatian plurals, **▸ Android:** `plurals` resources choose the word form per number, like `Intl.PluralRules`). Expanded view lists up to 5 titles. Tap opens Danas.
- **AC-6.1** Not sent when nothing is due today or overdue.
- **AC-6.2** At most once per day, also after reboot or time change.

### FR-7 Screens
As in HANDOFF §6 (screens 01–10, light and dark), with these changes:

| Screen | Change |
|---|---|
| Detalji (02) | Top bar gets a **⋮ menu** for Stalni: "Preskoči ovaj mjesec", "Pauziraj" / "Nastavi". When a task has two open occurrences, the "Trenutni rok" card refers to the **oldest**, and the bottom bar acts on it. |
| Detalji (02), Raspored | "Ako kasni" row text follows H2 ("Svaka 2 sata, s vremenom češće"). The row still shows "sljedeći u 11:00" from the planner. |
| Danas (01) | Overdue-on-due-day cards show the "Rok istekao" chip. A slim red banner with "Popravi" appears when a blocking problem exists (handoff O5). Paused tasks are not shown. |
| Svi zadaci (05a) | Paused Stalni rows show a "Pauzirano" badge. |
| Arhiva (05c) | Rows: "Odrađeno" chip + completion date, tap opens read-only Detalji with only "Obriši zadatak" (H14). |
| Onboarding (08a–08c) | Steps: **1 Obavijesti**, **2 Izuzmi iz štednje baterije**, **3 Samsung: ne uspavljuj aplikaciju** (new content in the 08b/08c layout: short explanation, numbered steps from §10.5, button "Otvori postavke", checkbox "Napravio sam ovo"). Step "Točni alarmi" is dropped (H10). |
| Postavke (07) | Dozvole section: Obavijesti, Točni alarmi (status only, always OK), Optimizacija baterije, **Samsung uspavljivanje** (manual checkbox, warning only). "Zadano za nove zadatke" as in the handoff. New row **Dijagnostika** → alarm log screen (H12). |
| Dijagnostika *(new)* | Simple list: planned time, actual time, delay, number of notifications. Warning line at top if anything was > 5 min late in the last 7 days. Uses existing list/card components. |

### FR-8 Settings (DataStore)

| Key | Default |
|---|---|
| quietEnabled | true |
| quietFrom / quietTo | 22:00 / 07:00 |
| defaultDayTimes | 09:00, 13:00, 17:00, 20:00 |
| defaultEarlyDays / defaultEarlyTime | 2 / 09:00 |
| defaultOverdueIntervalHours | 2 (1, 2 or 4) |
| onboardingCompleted | false |
| samsungSetupConfirmed | false |
| lastSummaryDate | null |

**▸ Android:** *DataStore* is a small key-value store for settings that exposes changes as a `Flow` (an observable stream), roughly `localStorage` with subscriptions.
- **AC-8.1** Any settings change reschedules all reminders immediately.

### FR-9 Permissions and device health

| Item | Check | Fix | Blocking (Danas banner) |
|---|---|---|---|
| Obavijesti | `areNotificationsEnabled()` + `POST_NOTIFICATIONS` | Runtime request, then app notification settings | Yes |
| Channels `na_dan`, `kasni` enabled | channel importance ≠ none | Channel settings | Yes |
| Točni alarmi | `canScheduleExactAlarms()` | (always granted with `USE_EXACT_ALARM`; if ever false: fall back to inexact `setAndAllowWhileIdle` and show the problem) | Yes |
| Optimizacija baterije | `isIgnoringBatteryOptimizations()` | `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` dialog | Yes |
| Samsung uspavljivanje | no public API, manual checkbox | Opens app info + steps (§10.5) | No (warning) |
| Točnost alarma | alarm log: > 5 min late in last 7 days | Opens Dijagnostika | No (warning) |

All re-checked in `onResume` of Danas, Postavke and onboarding. **▸ Android:** a *runtime permission* is granted in a system dialog while the app runs (like the browser notification prompt). A *special access* such as battery optimization lives on a system settings screen the app can open but not toggle.

### FR-10 Backup (Sigurnosna kopija)
- Export/import through the **Storage Access Framework** (**▸ Android:** the system "Save as / Open" picker; no storage permission needed).
- Format (extends HANDOFF §6.7): `{ "format": "podsjetnik-backup", "version": 1, "exportedAt": "…", "settings": {…}, "tasks": […], "occurrences": […] }`. ISO-8601 dates. The alarm log is not exported.
- Import: validate → "Ovo će zamijeniti sve postojeće podatke (N zadataka). Nastaviti?" → replace in one transaction → cancel all notifications → refresh.
- **AC-10.1** Export → uninstall → reinstall → import restores tasks, history and settings; reminders resume.
- **AC-10.2** An invalid or newer-version file is rejected with a message, and existing data is untouched.

---

## 7. Notification actions without opening the app

**▸ Android:** a *PendingIntent* is a token the app hands to the system ("run this on my behalf later"), used for alarms and notification buttons. A *BroadcastReceiver* is a small entry point the system calls with it, even when the UI is closed. It has ~10 s, so it uses `goAsync()` to finish a short database write in a coroutine.

| Action | Effect |
|---|---|
| Odrađeno | Mark done (FR-3) → notification becomes "Odrađeno: ‹naslov›" + Poništi, silent, `setTimeoutAfter(10 s)` → refresh |
| Poništi | Undo (FR-3) → refresh (the reminder returns if still due) |
| Odgodi 1 sat | `snoozedUntil = now + 1 h` → notification removed → refresh |
| Swipe on overdue (delete intent) | Re-post the same notification immediately, silently (H4) |
| Tap | Open Detalji |

A stale action (occurrence already closed in the app) only removes the notification.

---

## 8. Reminder logic

### 8.1 One alarm, one refresh

**▸ Android:** `AlarmManager` wakes the app at a set time even when it isn't running. *Doze* is Android's deep idle mode; normal alarms wait in Doze, but "allow while idle" exact alarms still fire.

```
trigger ──► ReminderEngine.refresh(now)
             1. ensureOccurrences()      create next occurrences where due (FR-3)
             2. planner(now)             pure function: tasks + occurrences + settings
                  → notifications to post / update / cancel, nextWakeAt
             3. post / cancel            at most one alerting post per refresh (H8)
             4. persist lastNotifiedAt, snoozedUntil cleared, lastSummaryDate
             5. replace THE single exact alarm with nextWakeAt
```
Triggers: alarm fired; any data or settings change; app opened; `BOOT_COMPLETED`; `TIME_SET`; `TIMEZONE_CHANGED`; `MY_PACKAGE_REPLACED`; a WorkManager safety-net job every 6 h (**▸ Android:** *WorkManager* runs deferrable background jobs that survive reboot but may be delayed, so it's fine as a safety net and never used for reminder timing).

### 8.2 Planner rules per open occurrence
1. **Early slot:** `(dueDate − earlyDays) @ earlyTime` if `earlyDays > 0`.
2. **Due-day slots:** each day time on `dueDate` before the overdue moment.
3. **Overdue slots:** a fixed daily wall-clock grid `anchor + k × interval`, from the overdue moment onward. Anchor = due time for a Povremeni with a time, otherwise the first day time (09:00 default). The overdue moment itself is a slot when a due time exists ("Rok je istekao").
   - interval = task's `overdueIntervalHours` while days late ≤ 2; from days late ≥ 3, `max(1, interval / 2)`. The anchor stays the same (HANDOFF §4.3).
4. **Snooze:** slots before `snoozedUntil` are dropped. One slot at `snoozedUntil`. Afterwards: due-day slots continue normally; overdue grid restarts from `snoozedUntil + k × interval`.
5. **Quiet hours** (if on): a slot inside the window moves to its end; several collapse into one.
6. **Fire rule:** the next slot is the earliest slot later than `lastNotifiedAt` (or the earliest overall if never notified). If it is ≤ now + 1 min → post with the stage valid *now* and set `lastNotifiedAt = now`. Several missed slots (phone off) → **one** post.
7. Paused task or closed occurrence → no slots; its notification is cancelled.
8. **Morning summary slot:** daily at `quietTo`, once per date (`lastSummaryDate`).

`nextWakeAt` = the earliest future slot across all occurrences and the summary.

### 8.3 Sound policy (H8, H9)
One refresh = at most one alerting notification, the most urgent: overdue > due > summary > early. All others in that refresh are posted silently. A repeat reminder for the same stage alerts again (that's its purpose). A re-post after a swipe never alerts (H4).

### 8.4 Changes while reminders are pending
- **Edit task:** open occurrences get `lastNotifiedAt = now` and `snoozedUntil = null`. Nothing fires for past slots, and the next future slot follows the new schedule. Rule change → the newest open, not-yet-overdue occurrence is regenerated with the new rule. Overdue occurrences keep their dates.
- **Settings change, delete, pause:** refresh. Everything is derived.

### 8.5 Timelines

Setup "Očitati struju": Stalni, **Zadnji dan u mjesecu**, early 2 days at 09:00, day times 09/13/17/20, overdue every **2 h**, quiet hours 22–07 on. 31. 10. 2026 is a Saturday.

**A. Done on time**

| When | What happens |
|---|---|
| Thu 29. 10. 09:00 | Early, silent: "Za 2 dana: Očitati struju" |
| Sat 31. 10. 07:00 | Summary (sound): "Danas imaš 1 zadatak" |
| 09:00 | Notification becomes due, with sound: "Danas: Očitati struju" |
| 13:00 | Repeated, with sound |
| 13:40 | Odrađeno in the notification → `DONE`, lateMinutes 0. "Odrađeno: Očitati struju · Poništi" for 10 s. Occurrence for **Mon 30. 11.** created. |
| Sat 28. 11. 09:00 | Next early reminder |

**B. Done early:** Wed 28. 10., Odrađeno from Danas › Uskoro → `DONE`. Dialog: "Sljedeći termin 30. 11., ponedjeljak · zadnji dan u mjesecu, Rani podsjetnik 28. 11. u 09:00". No reminders on 29. or 31. 10.

**C. Done 2 days late**

| When | What happens |
|---|---|
| 29. 10. and 31. 10. | As in A, never tapped |
| Sun 1. 11. 00:00 | Overdue starts. November occurrence created. Grid slots 01, 03, 05 are in quiet hours. |
| 07:00 | Collapsed overdue reminder, **ongoing, sound + vibration**: "Kasni 1 dan: Očitati struju (listopad)". Summary "Kasni ti 1 zadatak" posted silently (overdue wins the sound). |
| 09:00, 11:00 … 21:00 | Every 2 h on the 09:00 grid |
| 23:00, 01:00 … | In quiet hours → 07:00 |
| Mon 2. 11. 07:00 | Summary + "Kasni 2 dana" (days late 2 → still every 2 h) |
| 09:00, 11:00, 13:00 | Repeats |
| 13:20 | Odrađeno → `DONE`, lateMinutes 2240 (37 h 20 min after 1. 11. 00:00), Detalji › Zadnji put odrađeno shows "za rok 31. 10., 2 dana kasnije" (calendar days) |
| *(if not done)* Tue 3. 11. | Days late 3 → **every hour** on the grid: 07:00, 08:00, 09:00 … 21:00 |

**D. Swiped away:** Sun 1. 11. 11:00 overdue posted; 11:05 swiped (unlocked) → immediately back, silent. 13:00 next reminder with sound. While locked it can't be swiped, and "Clear all" doesn't remove it.

**E. Month skipped:** Fri 20. 11., Detalji ⋮ → Preskoči ovaj mjesec on November → `SKIPPED`. December occurrence (Thu 31. 12.) created. No reminders 28. or 30. 11. Next: early reminder Tue 29. 12. 09:00.

**F. Snooze**

| When | What happens |
|---|---|
| Sat 31. 10. 13:00 | Due reminder → Odgodi 1 sat |
| 14:00 | Reminder again |
| 15:00 | In the app: Odgodi → Sutra ujutro (until Sun 1. 11. 09:00) |
| 17:00, 20:00, 07:00 | Suppressed. 07:00 summary with sound: "Kasni ti 1 zadatak" |
| Sun 1. 11. 09:00 | Overdue reminder (snooze slot); grid continues 11:00, 13:00 … |

**G. One-off with a time:** "Nazvati servis za klimu", Povremeni, due Tue 3. 11. 2026 at 10:00, every 2 h (matches handoff sample data).

| When | What happens |
|---|---|
| 3. 11. 09:00 | Due reminder (only day time before 10:00) |
| 10:00 | Overdue moment: "Rok je istekao: Nazvati servis za klimu", chip "Rok istekao" |
| 12:00 … 20:00 | Every 2 h on the 10:00 grid |
| 22:00 → Wed 4. 11. 07:00 | Collapsed (22, 00, 02, 04, 06) |
| 08:00, 10:00 … | Every 2 h |
| Thu 5. 11. 10:24 | Next reminder **12:00** (days late 2), as in HANDOFF §11 |
| Fri 6. 11. | Days late 3 → every hour |
| Odrađeno | Snackbar "Premješteno u arhivu" + Poništi; task in Arhiva |

### 8.6 Diagnostics (alarm log)
Each alarm fire stores `scheduledFor`, `firedAt` and the number of posts. The last 500 entries are kept, shown in Postavke › Dijagnostika (H12). This measures success criterion 1.

---

## 9. Notifications

### 9.1 Channels (H9)
**▸ Android:** every notification belongs to a *channel* that fixes sound, vibration and importance. The user can change channels in system settings; the app **cannot** change a channel's sound after creating it, so changing behavior later needs a new channel ID.

| ID | Name | Importance | Sound / vibration | Heads-up |
|---|---|---|---|---|
| `rani` | Rani podsjetnici | LOW | none | no |
| `na_dan` | Podsjetnici na dan | DEFAULT | default sound | no |
| `kasni` | Zakašnjeli zadaci | HIGH | default sound + strong vibration | yes |
| `sazetak` | Jutarnji sažetak | DEFAULT | default sound | no |

Volume is the phone's notification volume (notification stream, not alarm stream).

### 9.2 Content
Copy, icon, accent colors and actions as in HANDOFF §5.3, plus:
- Notification ID = **occurrence ID** (not task ID). When a task has two open occurrences, the month is appended: "Očitati struju (listopad)".
- Overdue text uses the current interval: "…Podsjećam svaki sat dok ne označiš Odrađeno." from day 3.
- Overdue on the due day (H3): title "Rok je istekao: ‹naslov›".
- Done state (H5): title "Odrađeno: ‹naslov›", text "Sljedeći termin 30. 11." (Stalni), action Poništi, silent, 10 s timeout.
- Summary: title per FR-6, inbox style, no actions.
- Overdue: `setOngoing(true)` + `setDeleteIntent`. Since Android 14 users can swipe ongoing notifications except when the phone is locked, and "Clear all" doesn't remove them [S4]. The delete intent fires only on user dismissal, so re-posting from it doesn't loop on our own cancels.
- Four or more notifications are bundled by the system; no custom grouping in v1.

---

## 10. Platform decisions

### 10.1 SDK levels
- **minSdk 35** (H11): one Android 15+ phone, so no compatibility branches.
- **targetSdk/compileSdk 37**: Android 17 is released, and AGP 9.4 supports up to API 37 [S12]. Android 15–17 behavior changes were checked [S5–S9]. Relevant: the Android 15 *stopped state* change (§10.2), and the Android 17 *background audio hardening* (restricts the app's own audio playback, not channel sounds played by the system). Nothing else affects alarms or notifications.

### 10.2 Exact alarms (H10)
- **`USE_EXACT_ALARM`**: same capability as `SCHEDULE_EXACT_ALARM`, but a normal permission **granted at install**. `SCHEDULE_EXACT_ALARM` is denied by default since Android 14 and must be enabled on a settings screen [S2, S3]. Play restricts `USE_EXACT_ALARM` to alarm/calendar apps, which doesn't apply to a sideloaded APK. `canScheduleExactAlarms()` is still checked, with an inexact fallback (`setAndAllowWhileIdle`) if it is ever false.
- **`setExactAndAllowWhileIdle(RTC_WAKEUP)`** fires on time in Doze, limited to once per 9 min per app [S11], which the single batched alarm avoids.
- **`setAlarmClock()`** (fully exempt from Doze, but shows an alarm icon and "next alarm") is **not** used in the product. The spike measures it only as a fallback if mode A turns out unreliable on the Samsung.
- **Force stop** on Android 15+ cancels all pending alarms until the app is opened again [S9]. The Samsung step tells me never to force-stop.

### 10.3 Reboot, time and time zone
Alarms are cleared on reboot. `BOOT_COMPLETED`, `TIME_SET` and `TIMEZONE_CHANGED` may be received by manifest receivers [S13] and trigger refresh. `BOOT_COMPLETED` arrives after first unlock, when the database is readable. `MY_PACKAGE_REPLACED` refreshes after installing a new APK.

### 10.4 Battery optimization
`isIgnoringBatteryOptimizations()` + the `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` dialog (needs `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`; Play restricts it, sideloading doesn't) [S11]. The spike checks that this shows as "Unrestricted" in One UI app battery settings.

### 10.5 Samsung One UI sleeping apps
Samsung adds its own limits independent of Android's battery exemption [S14, S15]:
- **Settings → Battery → Background usage limits** (older: *Battery and device care → Battery → App power management*):
  - **Put unused apps to sleep**: off, or at least Podsjetnik in the never-sleep list.
  - **Sleeping apps** / **Deep sleeping apps**: Podsjetnik must not be in them.
  - **Never auto sleeping apps** (on some versions "Never sleeping apps"): **add Podsjetnik**.
- **Settings → Apps → Podsjetnik → Battery → Unrestricted.**
- No public API reads these lists, so the onboarding step and Postavke row show the steps and I confirm with a checkbox. The exact Croatian labels on my One UI version are recorded during the spike.

### 10.6 Architecture
- **Single Gradle module.** Packages as in HANDOFF §2: `data`, `domain`, `reminders`, `ui`. Screens are **Jetpack Compose** functions (**▸ Android:** Compose is declarative UI, close to React function components; state changes trigger *recomposition* instead of re-render) with a **ViewModel** each (**▸ Android:** holds screen state and survives rotation, like a custom hook whose state lives outside the component).
- `domain` is pure Kotlin (no Android imports) with an injected `java.time.Clock`.
- No DI framework: one `AppContainer` in the `Application` class.

---

## 11. Data model (H1)

**▸ Android:** *Room* is a typed layer over SQLite: entities are data classes (tables), DAOs are interfaces with compile-time-checked SQL, and queries can return a `Flow` that re-emits on change (a live query).

### TaskEntity
| Field | Type | Notes |
|---|---|---|
| id | Long PK | |
| title | String | required |
| type | `RECURRING` / `ONE_OFF` | immutable |
| recurrence | RecurrenceRule? (JSON) | `{"type":"dayOfMonth","day":5}` / `{"type":"lastDayOfMonth"}`; null for ONE_OFF |
| dueTime | LocalTime? | ONE_OFF only |
| earlyDays | Int | 0–7, 0 = off |
| earlyTime | LocalTime | |
| dayTimes | List<LocalTime> | sorted, unique, 1–6 |
| overdueIntervalHours | Int | 1, 2 or 4 |
| note | String? | |
| pausedAt | Instant? | RECURRING only |
| archivedAt | Instant? | ONE_OFF done |
| categoryId | Long? | **v2**, unused in v1 UI |
| createdAt, updatedAt | Instant | |

### OccurrenceEntity (also the completion/skip record)
| Field | Type | Notes |
|---|---|---|
| id | Long PK | also the notification ID |
| taskId | Long FK → Task, cascade delete | |
| dueDate | LocalDate | unique with taskId |
| status | `OPEN` / `DONE` / `SKIPPED` | |
| completedAt | Instant? | DONE and SKIPPED |
| lateMinutes | Int? | DONE only: max(0, completedAt − overdue moment), frozen at completion |
| value | Double? | **v2** (e.g. meter reading) |
| photoUri | String? | **v2** |
| snoozedUntil | Instant? | |
| lastNotifiedAt | Instant? | last reminder slot posted |
| createdAt | Instant | lets Poništi delete only the occurrence its Done auto-created (FR-3) |

"Zadnji put odrađeno" on Detalji = the latest DONE occurrence.

### AlarmLogEntity (not exported)
id, scheduledFor, firedAt, postedCount. Trimmed to 500 rows.

### Conventions
`LocalDate` and `LocalTime` are stored as ISO strings, `Instant` as epoch millis, and lists and rules as JSON (kotlinx.serialization) through Room type converters. Room schemas are exported to the repo. Every schema change ships a migration, and destructive migration is never enabled.

### v2 readiness
| v2 feature | Supported by |
|---|---|
| History (when, how late) | DONE/SKIPPED occurrences with completedAt, lateMinutes |
| Value / photo + chart | Occurrence.value, photoUri |
| Weekly, every N months, yearly, first Monday | new `RecurrenceRule` subtypes (JSON), no migration |
| Categories | Task.categoryId |
| Widget | same queries as Danas; bottom nav has room for a 4th tab |

---

## 12. Edge cases

| Case | Behavior |
|---|---|
| Month end | 29–31 not offered; "Zadnji dan u mjesecu" handles it |
| Leap years | 29. 2. 2028; 28. 2. 2027 and 2100. Unit-tested. |
| DST (Croatia: last Sunday of March 02→03, of October 03→02) | Inside default quiet hours. Grid and day times are wall-clock, so they stay at their local time. A slot in the skipped spring hour moves forward by an hour (java.time gap rule). In the repeated autumn hour the earlier instance is used, and `lastNotifiedAt` prevents a duplicate. |
| Phone off at reminder time | No alarm fires. On boot, refresh posts one notification per occurrence in its current stage (or defers it to quiet end). Missed months are generated as open occurrences. |
| Reboot | `BOOT_COMPLETED` → refresh |
| Force stop | Android 15+ cancels alarms until the app is opened; the Samsung step warns |
| Time zone change | Wall-clock times stay local in the new zone. Snooze instants stay absolute (accepted). |
| Manual clock change | Forward: one catch-up post. Backward: no duplicates (`lastNotifiedAt`). |
| Edit with pending reminders | §8.4 |
| Quiet hours vs reminders | Deferred to the end, collapsed; one sound (§8.3) |
| Quiet hours off | Reminders fire at night too; the summary still comes at "Do" time |
| Snooze into quiet hours | Fires at quiet-hours end |
| Two open occurrences of one task | Two notifications with month in the title; Detalji acts on the oldest |
| Done on a stale notification | Notification removed, nothing else |
| Undo after the next occurrence was touched | The next occurrence is kept |
| Notification permission or channel disabled | Engine still runs and logs; banner on Danas |
| Paused task with an overdue occurrence | Silent until resume (H7) |
| Import while notifications are shown | All cancelled, data replaced, refresh |
| APK signed with a different key | Android refuses the update; uninstall loses data. One signing key from day one (PLAN Phase 0) + backups. |

---

## 13. Non-functional requirements

| Area | Requirement |
|---|---|
| Reliability | §1 success criteria; refresh is idempotent (running it twice changes nothing) |
| Battery | No foreground service; wake-ups only at real slots + WorkManager every 6 h |
| Performance | Cold start to Danas < 1 s with 200 tasks / 5 000 occurrences; refresh < 200 ms |
| Data safety | Migrations for every schema change, exported schemas, backup round-trip test, confirmations for delete/import |
| Privacy | No `INTERNET`, no analytics, no third-party SDKs |
| Testability | Pure `domain` with injected `Clock`. Required tests: recurrence (all month lengths, 2027/2028/2100), every §8.5 timeline, HANDOFF §11 scenarios (adjusted for H3), quiet-hours wrap and switch-off, DST in `Europe/Zagreb`, catch-up after phone off, edit with pending reminders, sound policy, idempotency |
| Accessibility | HANDOFF §9 |
| Localization | Croatian only; plurals via quantity strings; date formats per HANDOFF §8 |
| Design | Light and dark match the screenshots; Figtree bundled; dynamic color off; edge-to-edge |

---

## 14. Scope

| Feature | v1 | v2 |
|---|---|---|
| Stalni (1–28, last day) and Povremeni | ✅ | |
| Early / due-day / overdue reminders, escalation, quiet hours, morning summary | ✅ | |
| Notification actions, Poništi, re-post after swipe | ✅ | |
| Snooze, skip a month, pause | ✅ | |
| Screens from the handoff + ⋮ menu, Dijagnostika, Samsung onboarding step | ✅ | |
| Permission and Samsung guidance | ✅ | |
| JSON backup | ✅ | |
| Value/photo **fields** on occurrences | ✅ stored | UI: history, value/photo entry, chart |
| More recurrence rules | | ✅ |
| Widget | | ✅ |
| Categories | field only | ✅ |

New strings not in the handoff's `strings.xml` (to add, same style): ⋮ menu items, "Pauzirano", "Rok istekao", "Rok je istekao: %s", "Odrađeno: %s", summary plurals, Samsung onboarding step, Dijagnostika screen, channel `sazetak` name.

---

## 15. Sources

All accessed 2026-10-14.
- [S1] Schedule alarms: https://developer.android.com/develop/background-work/services/alarms
- [S2] Android 14 exact alarms denied by default, `USE_EXACT_ALARM`: https://developer.android.com/about/versions/14/changes/schedule-exact-alarms
- [S3] `AlarmManager` reference: https://developer.android.com/reference/android/app/AlarmManager
- [S4] Android 14 behavior changes, all apps (non-dismissible notifications): https://developer.android.com/about/versions/14/behavior-changes-all
- [S5] Android 15 behavior changes (targeting 15): https://developer.android.com/about/versions/15/behavior-changes-15
- [S6] Android 16 behavior changes, all apps: https://developer.android.com/about/versions/16/behavior-changes-all
- [S7] Android 16 behavior changes (targeting 16): https://developer.android.com/about/versions/16/behavior-changes-16
- [S8] Android 17 behavior changes: https://developer.android.com/about/versions/17/behavior-changes-all · https://developer.android.com/about/versions/17/behavior-changes-17
- [S9] Android 15 behavior changes, all apps (stopped state cancels pending intents): https://developer.android.com/about/versions/15/behavior-changes-all
- [S10] Notification channels: https://developer.android.com/develop/ui/views/notifications/channels · Create a notification: https://developer.android.com/develop/ui/views/notifications/build-notification
- [S11] Doze and App Standby (9-minute limit, `setAlarmClock`, battery exemption): https://developer.android.com/training/monitoring-device-state/doze-standby
- [S12] AGP 9.4.0 release notes: https://developer.android.com/build/releases/agp-9-4-0-release-notes
- [S13] Implicit broadcast exceptions: https://developer.android.com/develop/background-work/background-tasks/broadcasts/broadcast-exceptions
- [S14] Don't kill my app, Samsung: https://dontkillmyapp.com/samsung
- [S15] Samsung: manage sleeping apps: https://www.samsung.com/ae/support/mobile-devices/how-to-manage-sleeping-apps-to-save-battery-life-on-your-galaxy-device/
