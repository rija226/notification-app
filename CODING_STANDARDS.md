# Coding standards

Base: the official [Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html) and [Android Kotlin style guide](https://developer.android.com/kotlin/style-guide). This file adds project rules on top. Where they conflict, this file wins.

## General
- 4-space indent, max line length 120, trailing commas in multi-line lists.
- Prefer `val`, immutable data classes, and pure functions. Mutate state only in ViewModels, the repository and the engine.
- No `!!`. Handle null explicitly or make the type non-null.
- No wildcard imports.
- Name things in English in code; Croatian appears only in `strings.xml` and test task titles.
- Comments explain *why*, not *what*. Mark deliberate simplifications with `// ponytail:` and their known limit.
- Small files with one clear responsibility. No speculative interfaces (an interface needs ≥ 2 implementations, or a test fake that truly needs it).

## Files and naming
| Kind | Name | Example |
|---|---|---|
| Screen composable | `XScreen.kt` | `DanasScreen.kt` |
| ViewModel | `XViewModel.kt` | `DanasViewModel.kt` |
| Helpers for a screen/feature | `XHelpers.kt` | `DanasHelpers.kt`, `TaskCardHelpers.kt` |
| Shared composable | `ui/components/Name.kt` | `StatusChip.kt` |
| Room entity | `XEntity` | `TaskEntity` |
| Test | `XTest.kt` mirroring the package | `ReminderPlannerTest.kt` |

**Helper rule:** non-composable helper functions (formatting, grouping, mapping, validation) never live in the same file as a composable or screen. They go in `*Helpers.kt` next to it, are top-level functions, are pure, and are unit-tested.

## Domain (`domain/`)
- Pure Kotlin only: no `android.*`, no Room, no Compose imports.
- Time comes from parameters (`now: Instant`, `zone: ZoneId`, `today: LocalDate`) or an injected `Clock`. Never call `now()` without a clock.
- `RecurrenceRule` is a `@Serializable sealed interface`. A new rule = new subtype + tests. No `when` over rule types outside the rule itself.
- Functions return new values. No hidden side effects.

## Data (`data/`)
- `TaskRepository` is the only entry point for UI and engine. DAOs are not used directly elsewhere.
- Reads return `Flow`. Writes are `suspend` and run in a transaction when they touch more than one row.
- Converters: `LocalDate` / `LocalTime` → ISO string, `Instant` → epoch millis, lists/rules → JSON.
- Every schema change: bump version, add a `Migration`, export the schema, add a migration test.

## UI (Compose)
- One ViewModel per screen. It exposes a single `StateFlow<XUiState>` (immutable data class) and functions for events. Screens collect with `collectAsStateWithLifecycle()`.
- Split each screen into a stateful `XScreen(viewModel)` and a stateless `XContent(state, onEvent…)`. Previews and UI logic use the stateless one.
- Composables: PascalCase, a `modifier: Modifier = Modifier` parameter as the first optional parameter, no business logic, no direct repository access.
- Colors from `LocalPodsjetnikColors` / `MaterialTheme.colorScheme`; text styles from `MaterialTheme.typography`; sizes from the handoff tokens. No inline hex colors.
- Strings via `stringResource` / `pluralStringResource`. Dates and times formatted by helpers (HANDOFF §8), tabular figures for numbers.
- Every icon-only control has a `contentDescription`; touch targets ≥ 48 dp.
- Add `@Preview` for light and dark on each shared component and screen content.

## Android glue (`reminders/`)
- Receivers do minimal work: `goAsync()`, call the repository/engine in a coroutine, `finish()` in `finally`.
- PendingIntents are `FLAG_IMMUTABLE` with stable request codes.
- Log with tag `Podsjetnik` via a single logging helper; no logging of note contents.

## Coroutines
- `viewModelScope` in ViewModels; an application scope from `AppContainer` for receivers and the engine. No `GlobalScope`.
- Database and file I/O on `Dispatchers.IO` (Room 3 suspend functions handle this themselves).

## Tests
- JUnit 4 + kotlinx-coroutines-test. Test names in backticks describing behavior: `` `done early closes occurrence and creates next` ``.
- Domain tests use `Clock.fixed(...)` with `ZoneId.of("Europe/Zagreb")`.
- One test per PRD §8.5 timeline; one per HANDOFF §11 scenario (adjusted per PRD H3).
- No mocking libraries. Use small hand-written fakes when needed.

## Git
- Small commits per logical step; message in imperative English (`Add recurrence rules with tests`).
- Never commit secrets, keystores, `local.properties` or build output.
