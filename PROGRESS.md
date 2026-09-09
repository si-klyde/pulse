# Fork progress

Fork of [keiretrogaming/pulse](https://github.com/keiretrogaming/pulse) (upstream `main` @ `0d2893e`, v1.19.6 build 303).
Test device: Retroid Pocket 6 (QCS8550, Android 13).

## Baseline (2026-09-09)

- Upstream builds clean on JDK 17 / AGP 9.2.1 / Gradle 9.4.1.
- 386 unit tests pass, 1 skipped (ad-hoc replay, expected).
- CI only runs `assembleDebug`; tests are never run in CI.

## Branch: `fix/root-exec-hardening` — DONE, hardware-verified

Scope: `app/src/main/java/com/kei/pulse/root/` (the layer that runs commands as root through the
device's own `PServerBinder`; the app itself is still no-root).

### Changes

| Commit | What | Why |
| --- | --- | --- |
| `f0f1255` | `RootSupport.runGeneratedScript` writes the script **inside** `pServerLock` | Callers share fixed script names (`apply-frequencies.sh`, `fps-probe.sh`). Writing outside the lock let caller A execute caller B's script when two applies overlapped (tile + AutoTDP tick, etc.). |
| `f0f1255` | PServer binder cached after first successful lookup; re-probed while unavailable | `RootExec()` was rebuilt per command (reflection + `ServiceManager.getService`, ~30×/s under load) and `RootCommandRunner` latched availability once at construction. At boot PServer can come up after PULSE, leaving every root path dead until the app was reopened. |
| `f0f1255` | `RootExecutor` interface + `RootSupport.executorFactory` test seam; `unitTests.isReturnDefaultValues = true` | Lets the root layer and its callers be unit-tested on a plain JVM (no device, no Robolectric). |
| `9bde3c3` | `shellQuote()`; RGB restore whitelists `#AARRGGBB[,#AARRGGBB]` and `0..1` float, then quotes | `RgbController.off()` interpolated values read back from `Settings.System` raw into a root shell. Any app with `WRITE_SETTINGS` could plant `'; <cmd>; '` and have it run as root. |
| `9bde3c3` | Governor echo quoted; every `cat $path` collapsed onto `RootSupport.cat(path)` (quoted) | Same class of bug, lower exposure. Removes 4 duplicate `cat` helpers. |

### Tests added (13)

- `root/ShellQuoteTest` — quoting rules.
- `root/RootSupportTest` — concurrent callers each run their own script (fails without the lock, verified),
  world-readable/exec perms, executor cached once, re-probed while down, `cat` quotes path.
- `data/RgbControllerShellSafetyTest` — valid values restored quoted; hostile values never restored.
- `data/SystemTuningShellSafetyTest` — governor quoted.

Result: 399 tests, 0 failures. `lintDebug` clean. Debug APK builds.

### On-device verification (RP6, debug build, adb logcat)

| Test | Result |
| --- | --- |
| Rapid tier switching AAA→Balanced→Power Saving→AAA | 4 apply cycles in 35 s, final sysfs caps match last tier, no errors |
| RGB Manual + Heat writes, then PULSE RGB Off | Every colour landed on device; Off restored saved colour via quoted `settings put` |
| Game session (Wuthering Waves) with AutoTDP + Custom fan | Full AutoTDP loop ran (HOLD/RAISE/TRIM), fan mode writes `ok=true`, no exceptions |
| Reboot with Apply-on-boot + Fan + RGB, app never opened | Process up 4 s after boot, PServer acquired first try, tier caps + fan + RGB applied within 1 s |
| Crashes | None from PULSE code |

## Branch: `feat/quiet-instrument-theme` — DONE, on-device checked (RP6)

Direction chosen 2026-09-09: **quiet instrument**. Flat dark surfaces, one accent, tabular numerals,
thin rules, no motion. Scope: theme layer only; screen layouts untouched.

### Changes

| Commit | What |
| --- | --- |
| theme | Five animated backgrounds (`HudBackground.kt`, 751 lines, 30 Hz canvas on every screen) → flat housing colour. `PulseThemeId` enum, picker, DataStore key removed; custom-accent setting kept. |
| theme | Palette: graphite housing `#1B1A18`, panel `#232220`, raised `#2B2A27`, rule `#3A3833`, ink `#ECE8E0` / `#A39E93`, accent glass blue `#8FB8CC`. Meter ramp sage `#7FB59A` → brass `#D9A441` → brick `#D96B5C`. |
| theme | Type: IBM Plex Sans (OFL) for UI, Plex Mono for live readouts only, both `tnum`. Chakra Petch removed. Letter-spacing 0. Radii 2/4/6/8/12 dp. |
| ui | Wordmark one colour; tagline + PServer chip sentence case; all section/telemetry/dialog labels sentence case; hardcoded radii → theme shapes; translucent cards solid; telemetry values in mono readout style. |

Result: 399 tests, lint clean, APK builds, installed and screenshotted on RP6 (tuner, settings, scrolled).

### Deferred (next styling pass)
- Settings header still shows the `P.U.L.S.E.` acronym line (rename pending).
- Overlay/OSD and Quick Access bar keep their own compact styling (`QaColors`, 10 sp caps labels).
- App icon / launcher branding untouched until the name is decided.

## Branch: `feat/rp6-shell` — IN PROGRESS, on-device checked (RP6)

Design locked 2026-09-09 (canvas: https://claude.ai/code/artifact/c38dff72-c679-4408-8e7c-c58b9c2c03f4, page "RP6 design").
Device truth: the RP6 renders at ~831×467 dp (1080p, density ≈2.3), not 960×540 — fixed columns sized to that.

Done:
- Theme: true black housing, ink at three luminances, no chromatic accent (selection = inverted fill); colour only
  for the meter ramp. Bricolage Grotesque (text) + Azeret Mono (numbers), variable OFL fonts, tabular figures.
- `ui/shell/`: `RailShell` (header with title/status + `HeroTrace` + fps/ms/W readouts; six-item `Rail`; section slot;
  `LiveColumn` with clocks-over-ceiling, per-cluster CPU bar, temp gauges w/ 70°/90° ticks, fan, battery),
  `Controls` (Seg, SegRow, OptionCard, HairRow, InkToggle/PulseSwitch, FactsRow).
- `ui/sections/PowerSection` (Auto | Manual; Auto = frame rate, lean w/ watt caps, aggressive park, facts row; Manual
  hosts the existing tier/clock modules), `FanSection` (mode row + live duty, AutoTDP note, Custom editor).
- Per game → `PerAppScreen(embedded)`; Overlay / Lights / System → `SettingsScreen(embedded, only = …)`.
- ViewModel: `telemetry` StateFlow (1 s), `drawHistory` (60 samples), `fanDuty` (2 s), all WhileSubscribed.
- MainActivity: section state replaces the two screen booleans; Back returns to Power.

Next (in order):
1. FPS into the header trace + readouts: the watcher's `FpsReader` samples live in the same process — publish
   them through a process-wide flow the ViewModel can read (frame-time history, current fps, AutoTDP action).
2. Overlays (Phase C): OSD on smoke surfaces at real sizes; Quick Access as one column (brightness/volume first,
   Power, Fan, Overlay, Lights), no tab rail.
3. Remaining Material widgets: RadioButton rows → Seg, Slider colours, per-app rows → hairline list with rule
   summary, `Per game · edit` sheet per the board, Lights section per board, System `About` copy.
4. Tier cards in Manual → `OptionCard`; PolicyCard → slim slider rows.

## Pre-existing issues found (not caused by this fork; candidates for later branches)

1. **Low-memory kills.** During a heavy game Android's LMK killed PULSE 6× in 14 s (RSS ~150–167 MB,
   `oom_score_adj` 200). FGS restarts each time. A background tuner should not need 150 MB; Compose
   overlay + animated HUD live in the service process.
2. **RGB "original colour" capture is wrong after a kill.** `pulse_last_color` is written with
   `apply()` (async). If LMK kills the process before it flushes, the next start sees PULSE's own colour
   ≠ last-known and re-captures it as the user's original. Result: RGB Off restores PULSE's orange.
   Fix: `commit()` for that key, or never re-capture once captured.
3. **Telemetry cost.** ~14 root `cat` per tick + 120 ms fan loop + per-tick `fan_mode` read ≈ 25–30 shell
   spawns/s while active. Upstream comment says combined-stdout batching is unreliable on this firmware;
   alternatives: direct `File.readText()` for world-readable nodes, `Settings.System`/`ContentObserver`
   for `fan_mode`, `BatteryManager` for battery.
4. **`ForegroundAppMonitorService`** (2048 lines) has several unguarded shared-state paths
   (`boundConfig` write outside `transitionMutex`, `stepAutoTdp` mutating controller maps outside the
   mutex, QA actions launched unordered). Structural split needed.
5. **Debug flags hard-coded on** (`AUTO_DEBUG`, `DEBUG_LOG`) → extra root reads in release.
6. **CI never runs tests.**

## Planned branches

1. ~~`fix/root-exec-hardening`~~ — done.
1b. ~~`feat/quiet-instrument-theme`~~ — done.
1c. `feat/rp6-shell` — in progress (see above).
2. `feat/ci-run-tests` — add `testDebugUnitTest lintDebug` to the workflow.
3. `fix/rgb-original-capture` — issue 2 above.
4. `perf/telemetry-direct-read` — issue 3 above (battery).
5. `refactor/watcher-split` — issue 4 above.

## Dev environment

- JDK 17 (`/usr/lib/jvm/java-17-openjdk`), Android SDK at `~/Android/Sdk` (platform 34, build-tools 34/36).
- `./gradlew testDebugUnitTest lintDebug assembleDebug`
- Device logs: `adb logcat -s PulseRgb PulseFan PulseWatcher PulseAutoTdp PulseSleep PulseQA PulseFps AndroidRuntime`
