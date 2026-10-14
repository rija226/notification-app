# Podsjetnik handoff package

Everything Claude Code needs to build the Podsjetnik Android app (Kotlin + Jetpack Compose).
Start with `HANDOFF.md`; it is the source of truth.

## How to hand it over

1. Create an empty folder for the project, e.g. `podsjetnik/`, and run `git init` in it.
2. Copy this whole folder into it as `docs/handoff/` (so the spec is at `podsjetnik/docs/handoff/HANDOFF.md`).
3. Open a terminal in `podsjetnik/` and start Claude Code.
4. Paste the first prompt below.
5. Approve the plan, then let it work phase by phase (HANDOFF.md section 13).

## First prompt (paste into Claude Code)

```
Read docs/handoff/HANDOFF.md completely and look at every screenshot in
docs/handoff/design/screens/. This is a new native Android app: Kotlin,
Jetpack Compose, Material 3.

Then:
1. Create CLAUDE.md in the repo root with the stack, package layout,
   conventions and the rule that docs/handoff/HANDOFF.md is the source of
   truth for behavior and design.
2. Propose the Gradle project skeleton and a concrete plan for phase 1 and
   phase 2 (section 13). Wait for my OK before writing code.

Rules for the whole project:
- Use docs/handoff/design/tokens/Color.kt and docs/handoff/android/strings.xml
  as the starting point for theme and copy. All UI text stays Croatian.
- The reminder engine is pure Kotlin with an injected Clock and must be covered
  by the unit tests listed in section 11 before any notification code.
- When you reach an open decision from section 12, use the default and tell me.
- After each phase: run the tests, summarize what changed, and stop.
```

## What is inside

| Path | What |
|---|---|
| `HANDOFF.md` | full spec: model, rules, reminder engine, Android permissions, screens, design system, build order |
| `design/screens/` | PNG of every screen, light and dark |
| `design/source/` | original design files with exact sizes (px = dp) |
| `design/tokens/` | `Color.kt` (Compose) and `tokens.json` |
| `android/strings.xml` | all Croatian strings and plurals |
| `data/sample-tasks.json` | seed data and expected results for the fixed test clock |

Live design canvas (private, for you): the Podsjetnik Design artifact in Claude.
