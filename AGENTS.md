# AGENTS.md

Guidance for AI coding agents working in this repo.

## What this is

Fossify Launcher — an Android home-screen launcher app, package `org.fossify.home`.
This is a personal fork of [FossifyOrg/Launcher](https://github.com/FossifyOrg/Launcher);
CI still calls reusable workflows from `FossifyOrg/.github`. GPLv3. Single-module
Gradle project (`settings.gradle` only includes `:app`).

## Build

- Gradle 9.6.1, AGP 9.2.1, Kotlin 2.3.10, KSP 2.3.10 — versions pinned in
  `gradle/libs.versions.toml`.
- compileSdk/targetSdk 36, minSdk 26, JVM target 17.
- No standalone JDK is on `PATH` in this environment. Build with Android
  Studio's bundled JBR:
  ```bash
  JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:compileDebugKotlin
  ```
- No emulator/device is available in this environment — verify Kotlin/Compose
  changes by compiling, not by running the app. Say so explicitly rather than
  claiming a UI change was visually confirmed.
- No test source sets exist (`app/src/test`, `app/src/androidTest` are both
  absent) — there is nothing to run; don't invent a test command.

## Structure (`app/src/main/kotlin/org/fossify/home/`)

| Package | Contents |
|---|---|
| `activities/` | Screens — `MainActivity` (the launcher itself, large), per-section settings activities, `SimpleActivity` base |
| `adapters/` | RecyclerView adapters |
| `databases/` | `AppsDatabase` — single Room DB, migrations as `object : Migration(from, to)` |
| `dialogs/` | Custom dialogs |
| `extensions/` | Kotlin extensions grouped by receiver type (`Activity.kt`, `Context.kt`, `View.kt`, ...) |
| `fragments/` | `AllAppsFragment`, `WidgetsFragment`, `MyFragment` base |
| `helpers/` | `Config.kt` (prefs), `Constants.kt` (pref keys / int enums), `IconCache`, `IconPackHelper`, etc. |
| `interfaces/` | Room DAOs + listener interfaces |
| `models/` | Entities / data classes |
| `receivers/`, `services/` | `LockDeviceAdminReceiver`, `NotificationBadgeListenerService` |
| `views/` | Custom views — `HomeScreenGrid` (core grid/widget-hosting logic, large) |

## Patterns to follow

- **Settings**: every preference is a `Config.kt` property (get/put over
  `SharedPreferences`), backed by a `const val` key in `Constants.kt`. When
  renaming a setting's label/identifier, keep the underlying preference key
  string unchanged unless a migration is written — otherwise existing users'
  saved value silently resets to default.
- **ViewBinding only** — `private val binding by viewBinding(ActivityXBinding::inflate)`
  (from `org.fossify.commons.extensions`). No `findViewById`.
- **No DI framework** — objects are constructed manually; singletons use a
  companion-object `getInstance()` with double-checked locking (see
  `AppsDatabase`).
- **No MVVM/ViewModel/Compose in this module** — logic lives directly in
  Activities/Fragments/Views.
- **Threading**: `org.fossify.commons.helpers.ensureBackgroundThread { }` for
  background work, paired with `runOnUiThread { }` to hop back. No
  coroutines/RxJava in app code.
- **Reuse `org.fossify:commons` before writing new code** — it's the shared
  base library (`BaseConfig`, `viewBinding`, `ensureBackgroundThread`,
  `SimpleActivity`, color/contrast utilities like `getContrastColor()` /
  `adjustForContrast()`, dialogs like `ColorPickerDialog`/`RadioGroupDialog`,
  etc.). Check it before adding a new utility or dependency.
- Nested/child settings (a setting that only applies when a parent toggle is
  on) are done by `beVisibleIf(parentCondition)` on the child row, re-applied
  in both `onResume()`/initial setup and the parent toggle's click listener —
  not indentation. There is no existing indentation convention for this.
- Settings-screen `onResume()` typically ends with `updateTextColors(...)` /
  `darkenTextForLightMode(...)` (generic theming, recurses over all
  `TextView`s in the holder) — if a screen has a live preview with its own
  computed text color, apply/update the preview *after* those calls, not
  before, or the preview's color gets clobbered on first paint.

## Style

- Standard Kotlin camelCase/PascalCase; one top-level class per file, file
  name matches the class.
- Comments are sparse by design — only for non-obvious *why* (e.g. explaining
  a shared/aliased preference in `Config.kt`). Don't add narrative comments.
- Imports fully qualified, no wildcards, roughly alphabetical within
  `android.*` / `androidx`+`org.fossify.commons.*` / `org.fossify.home.*`
  groups.
- `.editorconfig`: LF, UTF-8, 4-space indent, `max_line_length = 160`.
  `detekt.yml` separately enforces `MaxLineLength: 120` for style checks —
  the two are inconsistent; prefer staying under 120 where practical.
- Detekt (`detekt.yml` + `app/detekt-baseline.xml`) is the enforced linter —
  watch `LongMethod` (120), `LongParameterList`, `ReturnCount` (max 4),
  `MagicNumber`.

## Resources

- Layouts: `activity_*.xml` (screens), `item_*.xml` (RecyclerView rows),
  `dialog_*.xml` (dialogs).
- Drawables: icons as `ic_<name>_vector.xml`; other drawables descriptive
  snake_case.
- `strings.xml` is a flat list loosely grouped by feature via ordering (no
  section-header comments) — add new strings near their feature's existing
  strings. Shared/common strings live in the `org.fossify:commons` library,
  not here.
- 80+ `values-<locale>/` translation directories exist — never hand-edit a
  translated string across locales; only touch the default `values/strings.xml`.

## Git

- Commit subjects: short, imperative mood, capitalized, no type prefixes
  (not Conventional Commits) — e.g. `Fix notification badge preview contrast
  on initial load`, `Add a separate icon scale setting for the home screen`.
- No issue/PR numbers in the subject line.
