# Podsjetnik: handoff for Claude Code

Personal Android reminder app. Working name "Podsjetnik". Single user, offline, Croatian UI.
This document is the source of truth. Where it and the screenshots disagree, this document wins; where the screenshots and `design/source` disagree, the screenshots win.

**Stack (decided):** Kotlin, Jetpack Compose, Material 3. Native Android only.

Contents:
1. Product
2. Tech stack and project setup
3. Domain model
4. Rules: recurrence, status, reminder schedule
5. Scheduler, notifications, permissions (the hard part)
6. Navigation and screens
7. Design system
8. Copy, dates and plurals
9. Accessibility
10. Out of scope (leave room)
11. Sample data and test scenarios
12. Open decisions (defaults chosen, confirm with Aleksandar)
13. Build order and acceptance checklist
14. Files in this package

---

## 1. Product

**Problem.** The user forgets recurring chores and one-off tasks. Normal to-do apps notify once and go quiet.
**Promise.** Podsjetnik keeps reminding until the task is explicitly marked done.

Two task types:
- **Stalni** (recurring, monthly): rule is either "svakog N." (day 1 to 28) or "zadnji dan u mjesecu". Marking done closes only the current occurrence and schedules the next one.
- **Povremeni** (one-off): one date and time. Marking done moves it to Arhiva.

Reminder schedule per task:
- **Rani podsjetnik:** X days before the due date (0 to 7, 0 = off) at a set time.
- **Na dan roka:** repeats at several set times (default 09:00, 13:00, 17:00, 20:00) until marked done.
- **Ako kasni:** the task becomes "Zakašnjelo" and keeps reminding every N hours (1, 2 or 4), more often the longer it is overdue.
- **Tihi sati:** no notifications at night (default 22:00 to 07:00).

Principles:
- Reliability of reminders is the number one feature. Everything else is secondary.
- Calm, clean, practical. Not playful.
- Urgency obvious at a glance. Status colors are fixed and semantic: overdue = red, today = amber, upcoming = neutral, done = green.
- Large touch targets (min 48dp in implementation) and readable type.

---

## 2. Tech stack and project setup

- Kotlin, Jetpack Compose, Material 3 (`androidx.compose.material3`), Navigation Compose.
- **minSdk 26** (java.time available natively). Target the latest stable SDK.
- Persistence: **Room** for tasks and completions, **DataStore (Preferences)** for settings.
- Scheduling: **AlarmManager** exact alarms + `BroadcastReceiver`s. Do not use WorkManager for reminder timing (it is not exact). WorkManager is fine for non-timed housekeeping if ever needed.
- Notifications: `NotificationCompat`, three channels (section 5.3).
- Dates: `java.time` (`LocalDate`, `LocalTime`, `ZonedDateTime`) in the device time zone. Inject a `java.time.Clock` everywhere so logic is testable.
- DI: manual `AppContainer` is enough for this size. Hilt is optional.
- Font: **Figtree** (OFL). Bundle the TTF files in `res/font` (weights 400, 500, 600, 700) so rendering is deterministic offline. Download from Google Fonts.
- **Dynamic color OFF.** Status colors are semantic and must not change with wallpaper.
- Edge to edge: draw behind status and navigation bars, apply window insets. Mockups leave the top ~36dp free for the status bar; do not draw a fake status bar.
- No network permission, no backend, no analytics.
- Tests: JUnit for the date, recurrence and reminder engine (pure Kotlin, no Android deps). Compose UI tests optional.

Suggested packages:
```
app/
  data/        Room entities, DAOs, converters, DataStore settings, backup (JSON)
  domain/      Recurrence, TaskStatus, ReminderEngine (pure Kotlin), use cases
  reminders/   AlarmScheduler, ReminderReceiver, NotificationActionReceiver,
               BootReceiver, NotificationFactory, PermissionChecker
  ui/          theme/, components/, danas/, svizadaci/, detalji/, uredi/, postavke/, onboarding/
```

---

## 3. Domain model

```kotlin
enum class TaskType { RECURRING /* Stalni */, ONE_OFF /* Povremeni */ }

enum class RuleKind { DAY_OF_MONTH, LAST_DAY_OF_MONTH }   // extensible: WEEKLY, YEARLY later

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val type: TaskType,
    val ruleKind: RuleKind?,          // RECURRING only
    val ruleDay: Int?,                // 1..28, only for DAY_OF_MONTH
    val dueDate: LocalDate,           // RECURRING: date of the current open occurrence. ONE_OFF: the date.
    val dueTime: LocalTime?,          // ONE_OFF only
    val earlyDays: Int,               // 0..7, 0 = no early reminder
    val earlyTime: LocalTime,
    val dayTimes: List<LocalTime>,    // sorted, unique, size >= 1
    val overdueIntervalHours: Int,    // 1, 2 or 4
    val note: String?,
    val snoozedUntil: Instant?,
    val createdAt: Instant,
    val archivedAt: Instant?,         // ONE_OFF done -> archived
)

@Entity(tableName = "completions")
data class CompletionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val taskId: Long,
    val occurrenceDate: LocalDate,    // the due date that was closed
    val completedAt: Instant,
)
```

Settings (DataStore), defaults:

| Key | Default |
|---|---|
| quietEnabled | true |
| quietFrom / quietTo | 22:00 / 07:00 (window may cross midnight) |
| defaultDayTimes | 09:00, 13:00, 17:00, 20:00 |
| defaultEarlyDays / defaultEarlyTime | 2 / 09:00 |
| defaultOverdueIntervalHours | 2 |
| onboardingCompleted | false |

Room type converters: `LocalDate` (ISO string), `LocalTime` ("HH:mm"), `Instant` (epoch millis), `List<LocalTime>` (comma-separated "HH:mm").

"Zadnji put odrađeno" on Detalji = latest `CompletionEntity` for the task. The completions table is also the future history log.

---

## 4. Rules

### 4.1 Recurrence (Stalni)

- `DAY_OF_MONTH(d)`: `d` in 1..28. The UI blocks 29 to 31 and tells the user to use "Zadnji dan u mjesecu" instead.
- `LAST_DAY_OF_MONTH`: `YearMonth.atEndOfMonth()`.
- `nextRuleDate(after: LocalDate, inclusive: Boolean)`: first date matching the rule on or after (inclusive) / strictly after (exclusive) the given date.
- **Create:** `dueDate = nextRuleDate(today, inclusive = true)`.
- **Mark done:** insert `Completion(occurrenceDate = dueDate)`, then `dueDate = nextRuleDate(oldDueDate, inclusive = false)`. If the new date is still in the past, the task is overdue again right away (every missed month has to be closed). See open decision O3.
- **Mark done before the due date** (e.g. from Uskoro): closes the current occurrence, same rule as above.
- **Edit rule:** `dueDate = nextRuleDate(today, inclusive = true)`.

### 4.2 Status

`days = ChronoUnit.DAYS.between(today, dueDate)` in the device zone.

| Condition | Status | Chip text | Color |
|---|---|---|---|
| days < 0 | OVERDUE | "Kasni N dan/dana" | red (solid chip) |
| days == 0 | TODAY | "Danas" | amber |
| 1..7 | UPCOMING | "Za N dan/dana" | neutral |
| > 7 | LATER | "Za N dana" (only in Svi zadaci) | neutral |
| completed / archived | DONE | "Odrađeno" | green |

One-off tasks with a time are still date-based: overdue starts the next calendar day (O4).

Chips always carry an icon as well as color: overdue = alert-circle, today = clock, upcoming = no icon.

Sorting:
- Danas › Zakašnjelo: most days late first, then title.
- Danas › Danas: by next reminder time.
- Danas › Uskoro: by dueDate, then dueTime.
- Svi zadaci › Stalni / Povremeni: by dueDate ascending.
- Svi zadaci › Arhiva: by archivedAt descending.

### 4.3 Reminder schedule

Computed for the open occurrence of every active (not archived) task. All times are local.

1. **Early:** if `earlyDays > 0`, one reminder at `(dueDate - earlyDays) @ earlyTime`. Title "Za N dana: {title}".
2. **On due day:** one reminder at each `dayTimes` entry on `dueDate`. ONE_OFF also fires at `dueTime` if it is not already in the list. Title "Danas: {title}".
3. **Overdue:** from `dueDate + 1`, reminders fire on a fixed daily grid: `anchor + k × interval`. Anchor = `dueTime` for ONE_OFF, the first `dayTimes` entry (09:00 by default) for RECURRING. Grid points inside quiet hours collapse into one reminder at `quietTo` (see 4).
   Proposed escalation (O1): `interval = overdueIntervalHours` while `daysLate <= 2`; from `daysLate >= 3`, `interval = max(1, overdueIntervalHours / 2)`. The anchor stays the same when the interval changes.
   Mockup check at 5. 11. 10:24: Nazvati servis (anchor 10:00, 2 days late, every 2 h) → next 12:00. Očitati struju (anchor 09:00, 5 days late, every 1 h) → next 11:00.
   Title "Kasni N dan/dana: {title}".
4. **Quiet hours:** no notification inside `[quietFrom, quietTo)`. A reminder that falls inside moves to `quietTo`. Several moved reminders for the same task collapse into one.
5. **Snooze (Odgodi):** sets `snoozedUntil`. Every reminder before it is suppressed. At `snoozedUntil` fire once with the current status text, clear `snoozedUntil`, then continue the normal schedule (next on-day time, or overdue interval counted from the snooze time). Snooze never changes `dueDate` or status.
   - "1 sat" = now + 1h.
   - "Večeras (20:00)" = today 20:00. Hide this option when now >= 19:30 (O7).
   - "Sutra ujutro (09:00)" = tomorrow 09:00.
   - Notification action "Odgodi 1 sat" = now + 1h.
6. **Done:** cancel this task's notification and pending reminders. RECURRING moves to the next occurrence (4.1). ONE_OFF gets `archivedAt = now`. "Poništi" (undo) restores the exact previous row and deletes the completion.
7. **One notification per task.** Notification id = task id. Every reminder re-posts (updates) the same notification and alerts again.

### 4.4 ReminderEngine (pure Kotlin)

```kotlin
data class ReminderInstant(val taskId: Long, val at: ZonedDateTime, val kind: Kind) {
    enum class Kind { EARLY, DUE_DAY, OVERDUE, SNOOZE_END }
}
interface ReminderEngine {
    fun nextInstant(task: Task, settings: Settings, now: ZonedDateTime): ReminderInstant?
    fun status(task: Task, today: LocalDate): TaskStatus
}
```
No Android imports. Unit-test it with the scenarios in section 11.

---

## 5. Scheduler, notifications, permissions

### 5.1 Alarm strategy

- Keep **one** pending exact alarm: the earliest `nextInstant` across all tasks. Set it with `AlarmManager.setExactAndAllowWhileIdle(RTC_WAKEUP, ...)` and a `PendingIntent` to `ReminderReceiver`.
- On fire: post or update the notification of every task whose instant is `<= now + 1 min`, persist `lastNotifiedAt` if needed, then compute and set the next alarm.
- Why one alarm: in Doze, `setExactAndAllowWhileIdle` cannot fire more than once per nine minutes per app, so separate alarms at 09:00 for three tasks would be throttled. Batching avoids lost reminders.
- If `canScheduleExactAlarms()` is false, fall back to `setAndAllowWhileIdle` (inexact) and show the problem in Postavke.
- Do not use `setAlarmClock` (shows an alarm icon in the status bar and is meant for alarm clocks).

### 5.2 Reschedule triggers

Recompute and reset the single alarm on:
- app start and every data change (create, edit, delete, done, undo, snooze, settings change, import)
- `BOOT_COMPLETED` (alarms are cleared on reboot; needs `RECEIVE_BOOT_COMPLETED`)
- `MY_PACKAGE_REPLACED`, `TIME_SET`, `TIMEZONE_CHANGED`
- `AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED` (re-check `canScheduleExactAlarms()` first)

**Catch-up:** after boot or app start, if reminders were missed while the phone was off, post the current status notification once per overdue or due-today task. Do not replay every missed reminder.

### 5.3 Notifications

Channels (user can tune them in system settings):

| Id | Name (hr) | Importance |
|---|---|---|
| `rani` | Rani podsjetnici | DEFAULT |
| `na_dan` | Podsjetnici na dan | HIGH |
| `kasni` | Zakašnjeli zadaci | HIGH |

Content (see screenshots 10a to 10c):
- Small icon: monochrome bell. `setColor`: primary for early, amber for today, red for overdue.
- Title: "Za 2 dana: Očitati struju", "Danas: Očitati struju", "Kasni 1 dan: Očitati struju".
- Text:
  - early: "Rok je subota, 31. 10. (zadnji dan u mjesecu)."
  - today: "Rok je danas. Sljedeći podsjetnik u 13:00 ako ne označiš Odrađeno." (omit the second sentence after the last on-day time)
  - overdue: "Rok je bio 31. 10. Podsjećam svaka 2 sata dok ne označiš Odrađeno."
- `setCategory(CATEGORY_REMINDER)`, `setAutoCancel(false)`.
- Actions: **Odrađeno** and **Odgodi 1 sat**. Both go to `NotificationActionReceiver` (a `BroadcastReceiver` using `goAsync()` for the DB write), so the app does not open. After Odrađeno: cancel the notification, no UI.
- Tap on the notification body opens Detalji for that task.
- **Overdue = ongoing:** `setOngoing(true)` plus a `setDeleteIntent`. Since Android 14, users can swipe away ongoing notifications (they stay non-dismissible on the lock screen). Default (O2): when the delete intent fires for an overdue task, re-post the same notification **silently** (`setOnlyAlertOnce(true)` / `setSilent(true)`), so it stays in the shade until Odrađeno.

### 5.4 Permissions

| What | Check | Fix |
|---|---|---|
| Notifications (Android 13+) | `NotificationManagerCompat.areNotificationsEnabled()` | runtime request `POST_NOTIFICATIONS`; if permanently denied, `Settings.ACTION_APP_NOTIFICATION_SETTINGS` |
| Exact alarms | `AlarmManager.canScheduleExactAlarms()` | `Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM` with `package:` URI |
| Battery optimization exemption | `PowerManager.isIgnoringBatteryOptimizations(packageName)` | see O6 |

Manifest: `POST_NOTIFICATIONS`, `SCHEDULE_EXACT_ALARM`, `RECEIVE_BOOT_COMPLETED`, and `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` if O6 keeps the direct dialog.

- Use `SCHEDULE_EXACT_ALARM`, not `USE_EXACT_ALARM` (the latter is limited by Play policy to alarm clock / calendar apps). It is not pre-granted on fresh installs targeting Android 13+, and if the user revokes it the app is stopped and future exact alarms are cancelled, hence the reschedule trigger in 5.2.
- Battery (O6, default): the app is personal and not on Google Play, so the **Popravi / Izuzmi** button fires `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` (a one-tap system dialog). If it is ever published on Play, switch to `ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS`; Play policy only allows the direct request for specific app categories. Onboarding copy for both routes is in `strings.xml`.
- Re-check all three in `onResume` of Danas, Postavke and onboarding.

---

## 6. Navigation and screens

All mockups are 412 x 915 **dp** (1 px in the mockups = 1 dp). Tall screens (forms, Postavke) are drawn as full scroll length.

Routes:
```
onboarding/{step}        step 1..3, shown until onboardingCompleted
danas                    start destination
svi?tab={stalni|povremeni|arhiva}
zadatak/{id}             Detalji
uredi?id={id}            Novi zadatak (no id) / Uredi zadatak (id)
postavke
```
- Bottom navigation bar with 3 destinations: Danas, Svi zadaci, Postavke. Leave room for a 4th (future "Povijest").
- Extended FAB "Novi zadatak" on Danas and Svi zadaci (primaryContainer).
- Odgodi = `ModalBottomSheet`. Odrađeno confirmation = dialog. Delete confirmation = `AlertDialog` (not drawn; copy in strings).

### 6.1 Danas (screens 01, 09)

- Header: date line "Četvrtak, 5. studenoga" (14sp, muted), title "Danas" (32sp bold), summary "**2 kasne** · 1 za danas · 1 uskoro" (first part red when > 0; hide parts with 0).
- Three sections, each hidden when empty: **Zakašnjelo** (red dot, count badge), **Danas** (amber dot, count badge), **Uskoro** (neutral dot, "sljedećih 7 dana").
- TaskCard:
  - Row 1 (tappable, opens Detalji): type tile 40dp (repeat icon = Stalni, calendar-dot icon = Povremeni, content description "Stalni zadatak" / "Povremeni zadatak"), title (17sp semibold), meta line (14sp muted). Meta: Stalni = "Zadnji dan u mjesecu · sub, 31. 10." / "Svakog 5. u mjesecu · čet, 5. 11."; Povremeni = "uto, 3. 11. u 10:00".
  - Row 2: status chip, bell + next reminder ("11:00", or a date "8. 11." when not today), and the **Odrađeno** tonal button (green container) aligned right.
  - Overdue card: red-tinted surface + red outline. Today card: surface + amber outline. Upcoming: surface + neutral outline.
- Odrađeno on a card: RECURRING shows the Odrađeno dialog (6.4). ONE_OFF: the card animates out and a snackbar "Premješteno u arhivu" with "Poništi" appears (not drawn).
- **Empty state (09):** green check circle, "Sve je odrađeno", "Ništa ne kasni i ništa nije za danas. Javit ću se kad dođe vrijeme.", plus a "Sljedeće" card with the nearest future task.
- Lists must scroll; the mockup content fits but real data will not.

### 6.2 Detalji zadatka (screen 02)

- Top bar: back, edit icon (opens Uredi).
- Type line ("Stalni · zadnji dan u mjesecu"), title (28sp bold), status chip + "sljedeći podsjetnik u 11:00".
- Two-column card: **Trenutni rok** (red when overdue) | **Sljedeći termin** (Stalni only; for Povremeni show a single column).
- **Raspored podsjetnika** card, four rows:
  - Rani podsjetnik: "2 dana prije, u 09:00", status "Poslan 29. 10." (green) or the planned date.
  - Na dan roka: time chips. Past = check icon + muted, next = highlighted (primary container), future = plain. Right label "sve poslano 31. 10." when all sent.
  - Ako kasni: "Svaka 2 sata, s vremenom češće". When overdue, the row is red-tinted and adds "Sada svaki sat · sljedeći u 11:00".
  - Tihi sati: "22:00 – 07:00, bez obavijesti".
- **Zadnji put odrađeno:** "čet, 1. 10. u 08:15" / "za rok 30. 9., dan kasnije". Hide the section if never done.
- Napomena (if present): show as its own card under the schedule (not drawn).
- "Obriši zadatak" text button (red) at the end, with confirmation dialog.
- Bottom bar pinned: **Odgodi** (outlined) and **Odrađeno** (filled green, wider).

### 6.3 Odgodi bottom sheet (screen 03)

- Title "Odgodi podsjetnik", subtitle "{title} · {status}".
- Three 68dp rows: icon tile + label + resulting time:
  - 1 sat · "Danas u 11:24"
  - Večeras · "Danas u 20:00"
  - Sutra ujutro · "pet, 6. 11. u 09:00"
- Info box: overdue: "Zadatak i dalje kasni. Odgoda samo utišava podsjetnike do odabranog vremena." Otherwise: "Rok se ne mijenja. Odgoda samo utišava podsjetnike do odabranog vremena."
- "Odustani" text button.

### 6.4 Odrađeno confirmation (screen 04)

- Dialog over a scrim: green check circle, "Odrađeno", task title.
- Inner box: "Sljedeći termin", big date "30. 11.", "ponedjeljak · zadnji dan u mjesecu", bell line "Rani podsjetnik 28. 11. u 09:00".
- Buttons: "Poništi" (undo) and "U redu". Auto-dismiss after ~4 s if untouched.
- Only for RECURRING. ONE_OFF uses the snackbar from 6.1.

### 6.5 Svi zadaci (screens 05a, 05b, 05c)

- Title "Svi zadaci", primary tabs **Stalni (3) · Povremeni (2) · Arhiva**.
- Row card: type tile, title, line 2 (rule for Stalni, date + time for Povremeni), line 3 status chip + date ("rok sub, 31. 10."), chevron. Tap opens Detalji. No Odrađeno button here.
- Arhiva rows (not drawn): green "Odrađeno" chip + completion date; tap opens a read-only Detalji with "Vrati" (restore) as an optional extra.
- **Arhiva empty (05c):** archive icon, "Arhiva je prazna", "Kad povremeni zadatak označiš kao odrađen, spremit će se ovdje."

### 6.6 Novi / Uredi zadatak (screens 06a, 06b)

Full-screen form, top bar with close (X) and title, pinned full-width primary button at the bottom ("Spremi zadatak" / "Spremi promjene").

Fields in order:
1. **Naziv:** outlined text field, required.
2. **Vrsta:** 2-segment button Stalni / Povremeni + helper text per type.
3. Stalni only, **Ponavljanje:** radio "Određeni dan u mjesecu" with a 7-column day grid 1..28 (selected = filled primary circle, label "Svakog 15." on the right), helper "Za 29., 30. ili 31. odaberi „Zadnji dan u mjesecu”…"; radio "Zadnji dan u mjesecu" ("28., 29., 30. ili 31., ovisno o mjesecu"); footer "Sljedeći termin: ned, 15. 11." computed live.
4. Povremeni only, **Datum i vrijeme:** date field + time field, and an inline month calendar (Monday first, today ringed, selected filled). Time opens the M3 TimePicker.
5. **Rani podsjetnik:** stepper (−, "2 dana prije", +), range 0..7, 0 shows "Bez ranog"; time button "u 09:00". Helper "Od 0 do 7 dana. Na 0 nema ranog podsjetnika."
6. **Podsjetnici na dan:** wrapping chips "09:00 ×" + dashed "Dodaj vrijeme" (opens TimePicker). Keep sorted, unique, at least one (disable × on the last chip).
7. **Ako kasni, podsjeti svakih:** 3-segment 1 sat / 2 sata / 4 sata. Helper "Što duže kasni, to češće podsjeća. Tihi sati se poštuju."
8. **Napomena (neobavezno):** multiline field.

New tasks prefill from settings defaults. Validation: empty title shows "Upiši naziv zadatka.". Switching type keeps shared fields.

### 6.7 Postavke (screen 07)

Sections:
- **Dozvole:** header with "1 traži pažnju" (red) when anything is missing. Three rows (Obavijesti, Točni alarmi, Optimizacija baterije). OK rows: muted status + green "U redu" with check. Problem row: red-tinted, explanation text, red **Popravi** button. Each row has its own fix action (5.4).
- **Tihi sati:** switch "Bez obavijesti noću" + two time buttons **Od** / **Do**. Helper "Podsjetnici koji padnu u tihe sate stižu u 07:00, zajedno." (time from setting).
- **Zadano za nove zadatke:** day-time chips (same component as the form), "Rani podsjetnik" row (opens a small dialog with the stepper + time), "Ako kasni" segmented.
- **Sigurnosna kopija:** "Izvezi podatke" / "Uvezi podatke". Use the Storage Access Framework (`ACTION_CREATE_DOCUMENT` / `ACTION_OPEN_DOCUMENT`), JSON format:
  ```json
  { "format": "podsjetnik-backup", "version": 1, "exportedAt": "...", "settings": {}, "tasks": [], "completions": [] }
  ```
  Import replaces all data after a confirmation dialog (O9), then reschedules.

### 6.8 First launch (screens 08a to 08c)

Three steps with a 3-segment progress indicator and "N od 3":
1. **Dopusti obavijesti:** illustration = sample notification card. Button "Dopusti obavijesti" (runtime request).
2. **Uključi točne alarme:** comparison card. Button "Otvori postavke" (exact alarm settings). Helper "Otvorit će se postavke sustava. Uključi „Alarmi i podsjetnici”."
3. **Izuzmi iz štednje baterije:** radio-list illustration. Button "Izuzmi iz optimizacije". Copy depends on O6.

Each step has "Kasnije". Skip a step automatically if its permission is already granted. Finish → Danas, set `onboardingCompleted = true`. Missing permissions remain visible in Postavke.

### 6.9 Notifications (screens 10a to 10c)

See 5.3. The mockups are illustrative; the system draws the actual shade. Use the copy and the action set exactly.

---

## 7. Design system

Exact values are in `design/tokens/Color.kt` and `design/tokens/tokens.json`. The `.dc.html` sources contain every spacing and size as inline styles.

### 7.1 Color tokens

| Token | Light | Dark | Use |
|---|---|---|---|
| bg | #F5F6F8 | #0F1115 | screen background |
| card | #FFFFFF | #191C21 | cards, fields |
| c1 | #EDEFF3 | #22262C | tiles, inactive chips, info boxes |
| c2 | #E2E6EC | #2C3138 | inactive progress, switch track off |
| line | #DDE1E7 | #2F343C | card borders, dividers |
| outline | #B9BEC8 | #4A505A | field and button outlines |
| text | #171A1F | #E3E6EB | primary text |
| muted | #4A505B | #B0B6C0 | secondary text |
| faint | #666C77 | #8D94A0 | tertiary text, helper text |
| pri / onPri | #2E55A5 / #FFFFFF | #ADC4FF / #0D2860 | primary actions, selection |
| priC / onPriC | #DCE4F7 / #132F69 | #27427D / #DAE3FF | FAB, selected segments, time chips |
| red | #B3261E | #FFB4AB | overdue text, destructive |
| redS / onRedS | #B3261E / #FFFFFF | #FFB4AB / #5C1210 | solid overdue chip, Popravi |
| redCard / redLine | #FDF2F0 / #F1C9C3 | #241819 / #55302C | overdue card surface / border |
| redC / onRedC | #FBE1DD / #7A1913 | #4F1D19 / #FFD9D3 | overdue count badge, icon tile |
| amb | #865700 | #F0C25A | "Danas" section label |
| ambDot | #E09B00 | #F0C25A | today dot, notification accent |
| ambLine | #EED9A4 | #4A3C17 | today card border |
| ambC / onAmbC | #FFE8AE / #563900 | #453300 / #FFE1A0 | today chip, badge |
| neuC / onNeuC | #E8EBEF / #3E444F | #2A2F36 / #C4C9D2 | upcoming chip |
| grn | #1C773D | #8FD5A4 | done text, "U redu" |
| grnS / onGrnS | #1C773D / #FFFFFF | #8FD5A4 / #0B3A1D | filled Odrađeno button |
| grnC / onGrnC | #D5F0DC / #0F4F27 | #163D24 / #BCEDCA | tonal Odrađeno button, success circle |
| nav / navInd | #ECEFF3 / #D5DEF4 | #16191D / #2B4170 | navigation bar / active indicator |
| sheet | #FFFFFF | #1D2026 | bottom sheet, dialog |
| scrim | rgba(15,17,21,0.5) | rgba(0,0,0,0.62) | modal scrim |

Text contrast was checked at 4.5:1 or better for all text pairs above.

### 7.2 Typography (Figtree)

| Style | Size / line | Weight | Used for |
|---|---|---|---|
| Display | 32 / 38 | 700 | "Danas" title |
| Headline | 28 / 34-36 | 700 | screen titles, Detalji title, onboarding |
| Big number | 32 / 38 | 700, tabular | Odrađeno "30. 11." |
| Title | 22 / 28 | 700 | form/sheet titles |
| Card title | 17 / 22 | 600 | task titles, list rows |
| Body | 16 / 24 | 400-600 | body copy, field values |
| Body small | 14 / 20 | 400-500 | meta lines, helper |
| Label | 15 / 20 | 700 | section headers |
| Caption | 13 / 18 | 500-600 | chip text, small labels |

All times and dates use tabular figures (`fontFeatureSettings = "tnum"`).

### 7.3 Shape, spacing, sizes

- Radii: card 20, list card 20, icon tile 12 (36dp tile: 10), status chip 8, time chip 12, buttons and segmented controls fully rounded, bottom sheet top 28, dialog 28, FAB 16, text field 12.
- Screen horizontal padding 16 (cards) and 20 (headers). Section gap 16 (Danas) / 24 (forms, Postavke). Card padding 12-14. Gap between cards 8.
- Heights: bottom nav 80, FAB 56, primary button 52-56, segmented 48, list row min 60-72, time chip 44 (implement as 48), stepper buttons 44 (implement as 48).

### 7.4 Components to build

`StatusChip`, `SectionHeader`, `TypeTile`, `TaskCard` (with Odrađeno), `TaskListRow`, `NextReminderLabel`, `DoneButton` (tonal and filled), `SegmentedButtons` (M3 SingleChoiceSegmentedButtonRow), `TimeChip` (removable), `AddTimeChip`, `DayOfMonthGrid`, `InlineMonthCalendar`, `Stepper`, `PermissionRow`, `SettingsCard`, `EmptyState`, `SnoozeSheet`, `DoneDialog`. Use M3 `NavigationBar`, `ExtendedFloatingActionButton`, `TabRow`, `ModalBottomSheet`, `Switch`, `TimePicker`.

Motion: minimal. Card leaving a section: fade + shrink, ~200 ms. No bouncy or playful effects.

---

## 8. Copy, dates and plurals

- All UI copy is in `android/strings.xml` (Croatian). Put it in `res/values/strings.xml` (Croatian is the only language).
- Plurals use Croatian forms `one / few / other` (1 dan, 2 dana, 5 dana, 21 dan). Always use `getQuantityString`, never string concatenation.
- Date formats (custom formatter; do not rely on `FormatStyle`):
  - Short with weekday: "čet, 5. 11." (weekday abbreviations: pon, uto, sri, čet, pet, sub, ned)
  - Long with weekday: "Četvrtak, 5. studenoga" (genitive months: siječnja, veljače, ožujka, travnja, svibnja, lipnja, srpnja, kolovoza, rujna, listopada, studenoga, prosinca)
  - Date only: "30. 11." Year only when not the current year: "3. 11. 2027."
  - Time: 24h "09:00".
- Intervals: "svaki sat", "svaka 2 sata", "svaka 4 sata" (capitalize at sentence start).

---

## 9. Accessibility

- Touch targets at least 48dp.
- Status is never color only: chips have an icon and text.
- Content descriptions: type tiles ("Stalni zadatak" / "Povremeni zadatak"), icon buttons (Natrag, Uredi zadatak, Zatvori, Ukloni 09:00, Dan manje, Dan više).
- Font scaling up to 200%: no fixed card heights; text wraps; chip row wraps under the title if needed.
- Dark mode follows the system setting (no in-app toggle needed).

---

## 10. Out of scope (leave room, do not build)

History and charts, entering a value or photo when marking done, home screen widget, categories, weekly and yearly rules.
How room is left: completions table (history), nullable columns can be added for value/photo, `RuleKind` enum is extensible, bottom nav has space for a 4th tab, reminder logic lives in a widget-friendly domain layer.

---

## 11. Sample data and test scenarios

`data/sample-tasks.json` seeds the mockup state. Fixed clock: **Thursday 2026-11-05 10:24, Europe/Zagreb**.

| Task | Type | Rule / date | Expected status on 5. 11. | Next reminder |
|---|---|---|---|---|
| Očitati struju | Stalni | zadnji dan u mjesecu, open occurrence 31. 10. | Kasni 5 dana | 11:00 (hourly, escalated) |
| Nazvati servis za klimu | Povremeni | 3. 11. u 10:00 | Kasni 2 dana | 12:00 (every 2 h) |
| Poslati račun knjigovođi | Stalni | svakog 5. | Danas | 13:00 |
| Produžiti registraciju auta | Povremeni | 10. 11. u 09:00 | Za 5 dana (Uskoro) | early reminder 8. 11. 09:00 |
| Platiti režije | Stalni | svakog 15. | Za 10 dana (only in Svi zadaci) | early reminder 13. 11. 09:00 |

Unit tests to write (ReminderEngine + Recurrence):
1. The table above, all five rows (status and next instant).
2. Done on Očitati struju (open 31. 10.) → new dueDate 30. 11., early reminder 28. 11. 09:00.
3. LAST_DAY_OF_MONTH in February 2027 → 28. 2.; in February 2028 → 29. 2.
4. DAY_OF_MONTH(5) created on 5. 11. → dueDate 5. 11. (inclusive); created on 6. 11. → 5. 12.
5. Quiet hours: overdue interval landing at 23:00 moves to 07:00; two moved instants collapse into one.
6. Snooze "Sutra ujutro" at 21:00 on the due day → no reminders until 09:00 next day, then overdue schedule continues.
7. Escalation: 2 days late with base 2 h → 2 h; 3 days late → 1 h; base 1 h stays 1 h; base 4 h → 2 h.
8. DST change (last Sunday in March / October): reminders keep their local wall-clock time.
9. Early reminder with earlyDays = 0 → none.
10. One-off with dueTime 10:00 not in dayTimes → extra on-day reminder at 10:00.
11. Overdue grid: one-off with dueTime 10:00, 2 h → 08:00, 10:00, 12:00 … (06:00 falls in quiet hours and moves to 07:00); recurring with first day time 09:00, 2 h → 07:00 (moved), 09:00, 11:00 …

---

## 12. Open decisions (defaults chosen, confirm with Aleksandar)

| # | Question | Default in this spec |
|---|---|---|
| O1 | Exact overdue escalation rule | base interval until 2 days late, then halved (min 1 h) |
| O2 | Overdue notification swiped away on Android 14+ | re-post silently right away |
| O3 | Recurring task overdue for more than a month | each missed occurrence must be closed separately |
| O4 | When does a one-off with a time become overdue | next calendar day (date-based, like Stalni) |
| O5 | Warning banner on Danas when a permission is missing | yes, a slim red banner with "Popravi" (not drawn) |
| O6 | Battery exemption route | direct system dialog (app is not on Play) |
| O7 | Hide "Večeras (20:00)" late in the day | hide from 19:30 |
| O8 | One-off done confirmation | snackbar "Premješteno u arhivu" + Poništi |
| O9 | Import behavior | replace all data after confirmation |

---

## 13. Build order and acceptance checklist

Build in this order, one phase per PR/commit series. Run tests at the end of each phase.

1. **Setup:** project, Compose + M3, theme from `Color.kt`, Figtree, strings, edge-to-edge, navigation skeleton with bottom bar.
2. **Domain + data:** Room, DataStore, Recurrence, TaskStatus, ReminderEngine with all unit tests from section 11. Seed loader for `sample-tasks.json` (debug builds only).
3. **Screens with real data:** Danas (+ empty state), Svi zadaci (3 tabs), Detalji, Novi/Uredi form, Odgodi sheet, Odrađeno dialog, delete dialog. Match screenshots 01 to 06 in light and dark.
4. **Reminders:** AlarmScheduler (single alarm), ReminderReceiver, NotificationFactory (3 channels), NotificationActionReceiver (Odrađeno, Odgodi 1 sat), BootReceiver and other triggers, catch-up, ongoing overdue + delete intent.
5. **Permissions:** PermissionChecker, onboarding (08a to 08c), Postavke permission block (07), optional Danas banner.
6. **Postavke rest + backup:** quiet hours, defaults, export/import JSON.
7. **Polish:** accessibility pass, font scaling, dark mode check against `*-dark.png`, animations.

Acceptance checklist:
- [ ] A task due today notifies at every day time until Odrađeno, from the notification and from the app.
- [ ] Overdue notifications continue after a reboot, respect quiet hours and escalate.
- [ ] Odrađeno from the notification works with the app closed and updates the UI on next open.
- [ ] Stalni done shows "Sljedeći termin" and the next occurrence is correct (incl. last day of month).
- [ ] Povremeni done moves the task to Arhiva; Poništi restores it.
- [ ] Snooze options pause reminders without changing status.
- [ ] Revoking exact alarms or notifications is shown in Postavke with a working Popravi.
- [ ] Export then import on a clean install restores all tasks and settings.
- [ ] Light and dark match the screenshots; status colors are identical across screens.
- [ ] All engine unit tests pass with the fixed clock.

---

## 14. Files in this package

```
HANDOFF.md                 this spec
README.md                  how to start Claude Code with this package
design/screens/*.png       screenshots of every screen, light and dark (2x)
design/source/*.dc.html    original design source: exact px (= dp) values as inline styles;
                           {{t.x}} = color token x (see the script block at the bottom of each file);
                           <sc-if>/<sc-for>/<dc-import> are template constructs, ignore them
design/tokens/Color.kt     Compose color tokens (light + dark) and M3 scheme mapping
design/tokens/tokens.json  same tokens + type and shape scale as JSON
android/strings.xml        all Croatian UI strings and plurals, ready for res/values/
data/sample-tasks.json     seed data matching the mockups, with the fixed clock
```

Screenshot index:

| File | Screen |
|---|---|
| 01-danas-light / -dark | Danas (home) |
| 02-detalji-light / -dark | Detalji zadatka (overdue Stalni) |
| 03-odgodi-light / -dark | Odgodi bottom sheet |
| 04-odradjeno-light / -dark | Odrađeno confirmation (Stalni) |
| 05a-svi-zadaci-stalni-light / -dark | Svi zadaci, Stalni tab |
| 05b-svi-zadaci-povremeni-light | Svi zadaci, Povremeni tab |
| 05c-arhiva-empty-light | Arhiva empty state |
| 06a-novi-zadatak-stalni-light / -dark | Novi zadatak, Stalni (full scroll) |
| 06b-uredi-zadatak-povremeni-light | Uredi zadatak, Povremeni (full scroll) |
| 07-postavke-light / -dark | Postavke (full scroll) |
| 08a / 08b / 08c-onboarding-* | First launch: notifications, exact alarms, battery |
| 09-danas-empty-light | Danas empty state |
| 10a / 10b / 10c-notification-* | Notifications: early, due today, overdue (+ dark) |

References used for the Android behavior in section 5:
- Schedule alarms: https://developer.android.com/training/scheduling/alarms
- Doze and App Standby, battery exemptions: https://developer.android.com/training/monitoring-device-state/doze-standby
- Android 14 behavior changes (dismissible ongoing notifications, exact alarms denied by default): https://developer.android.com/about/versions/14/behavior-changes-all
