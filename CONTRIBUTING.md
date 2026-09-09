# Contributing

Thanks for looking. This fork is small and opinionated; here is how work happens so a change lands cleanly.

## Ground rules

- **Attribution stays.** `NOTICE.md` and the credits in `README.md` are not editable except to add names.
- **GPL v2 or later.** Anything you contribute is under the same licence.
- **One device.** The Retroid Pocket 6 is the target and the only hardware changes are verified on. Odin 3
  and Thor code paths are kept working where cheap, not tested.

## Workflow

1. Branch from `main`: `feat/<slug>` or `fix/<slug>`. Never commit on `main`.
2. **Tests first** for anything with logic. Pure decision code lives apart from Android (see
   `ChargingGuard`, `ForegroundTracker`, `GameSession`) so it runs on a plain JVM. The root layer is
   testable through `RootSupport.executorFactory`; assert the exact commands (see
   `ChargingControllerTest`, `RgbControllerShellSafetyTest`).
3. Build and run everything before a PR:
   ```bash
   ./gradlew testDebugUnitTest lintDebug assembleDebug
   ```
4. **Verify on the device** for anything the watcher, the root layer or the overlays touch. Say in the PR
   what you ran and what you saw (`adb logcat -s PulseWatcher PulseAutoTdp PulseFan PulseCharge`).
5. Open a PR to `main`. Title `[Main]-<branch-name>`. Description has four sections: Overview, Changes
   (Before → After per user-visible change), Details (only the non-obvious), Testing (one row per change,
   real numbers). No AI attribution lines in commits or PRs.
6. Update `PROGRESS.md` when a branch changes what the app does; it is the record §2(a) of the GPL asks for.

## Design vocabulary

The UI has a small, deliberate vocabulary; new screens use it rather than Material defaults.

- Surfaces: black housing in the app, smoke over games. Hairlines (`Rule`, `Rule2`) instead of cards.
- Selection: inverted ink fill; unselected: hairline outline. Controls in `ui/shell/Controls.kt`
  (`Seg`, `SegRow`, `OptionCard`, `Chip`, `HairRow`, `InkToggle` / `PulseSwitch`, `FactsRow`).
- Colour means something: temperature and load use `overlay/MeterColors`; nothing else is coloured.
- Type: Bricolage Grotesque for words, Azeret Mono (`Readout*` styles) for live numbers. No all-caps
  labels, no letter-spacing.
- Copy: plain sentences, sentence case, say what happens ("Off hands every control back…").

## Root layer

Every string that reaches a root shell goes through `shellQuote()` or `RootSupport.cat()`. Scripts are
written and executed under `pServerLock` via `runGeneratedScript`. Do not add `runRootCommand` calls that
interpolate anything a user or another app can influence.

## Syncing with upstream

`upstream` is https://github.com/keiretrogaming/pulse. Upstream pushes squash dumps, so merges are
manual: cherry-pick behaviour changes into a `fix/` or `feat/` branch with tests, and credit the upstream
commit in the message.
