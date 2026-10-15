# Phase 1 spike results

Fill in while running the checklist (PLAN Phase 1). Delays come from the in-app "Dnevnik alarma".

| | |
|---|---|
| Phone model | Samsung Galaxy A35 (SM-A356B) |
| One UI version | 8.5 (ro.build.version.oneui = 80500) |
| Android version | 16 |
| Date of tests | |

## Alarm accuracy

| Test | Mode A (točni alarm) | Mode B (budilica) |
|---|---|---|
| 15 min, app swiped from Recents, screen off | | |
| 15 min, `adb shell dumpsys deviceidle force-idle` | | |
| Overnight series (16×), max delay | | |
| Alarm icon / "next alarm" visible in status bar or lock screen? | n/a | |

## Behavior

| Check | Result |
|---|---|
| Reboot with pending test → fires after first unlock | |
| Odrađeno from lock screen without opening app | |
| Poništi within 10 s restores notification | |
| Odgodi from shade → back in 1 h | |
| Locked: ongoing notification can't be swiped | |
| *Clear all* keeps it | |
| Unlocked swipe → back immediately, no sound | |
| Clock moved forward past pending test → fires at once | |
| Battery dialog → App info › Battery shows "Neograničeno"/"Unrestricted"? | |
| Deep sleeping apps → test fails (negative test) | |
| Force stop → test gone; opening app reschedules | |

## Samsung menu paths (exact Croatian labels)

- Background usage limits:
- Put unused apps to sleep:
- Never auto sleeping apps:
- App battery setting:

## Emulator smoke test (2026-10-14, AVD "budget", Android 16)

- `dumpsys alarm`: RTC_WAKEUP, `window=0`, `exactAllowReason=policy_permission` → `USE_EXACT_ALARM` works as granted-at-install.
- 2-min test, app in background: scheduled 13:22:09.114, fired 13:22:09.121 (**+7 ms**). Notification received.
- Emulator says nothing about Samsung restrictions; the phone tests above are still required.

## Decision

- Mode chosen: **A** (`setExactAndAllowWhileIdle`), chosen by Aleksandar 2026-10-14. B only if the phone tests show delays.
- Notes:
