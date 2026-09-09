# PULSE (Retroid Pocket 6 fork): Notices & Attribution

This software is licensed under the **GNU General Public License v2.0 or later** (see `LICENSE`).

## Dedication

This fork is dedicated to the people whose work it stands on. PULSE is not ours; we redesigned and
hardened it for one device. The mechanism, the controllers, the ideas and most of the code are theirs:

- **keiretrogaming**: author of PULSE (https://github.com/keiretrogaming/pulse): AutoTDP, the
  closed-loop fan, the per-game engine, the overlays, the whole app this fork began from.
- **AurelioB**: author of ClusterTune (https://github.com/AurelioB/cluster-tune), from which PULSE
  itself was forked: the PServer no-root command-execution approach and the profile/apply pipeline.
- **FeralAI**: author of O2P Tweaks (https://github.com/FeralAI/o2ptweaks.app), where the PServer
  command-execution code originates.
- **TheOldTaylor** (Odin3-CPU-Underclock) and Reddit users **u/twoohfive205** and **u/JoaozaoS** in
  r/OdinHandheld, for the original underclocking idea.

## This fork

This repository (https://github.com/si-klyde/pulse) is a fork of **PULSE** by keiretrogaming, forked
2026-09-09 at upstream v1.19.6 (`0d2893e`). Modified files and dates are recorded in git history and
summarised in `PROGRESS.md`, per GPL v2 §2(a). Fork changes are © 2026 the fork author and released under
the same GPL v2.0 (or later) terms. This attribution section must remain intact in any redistribution.

Builds from this fork are signed with a different key from upstream and are not upstream releases.
Please report problems with this fork here, not to the upstream project.

## Bundled fonts

- **Bricolage Grotesque** (Atelier Triay) and **Azeret Mono** (Displaay) are licensed under the SIL Open
  Font License 1.1. Full license texts are in `licenses/OFL-BricolageGrotesque.txt` and
  `licenses/OFL-AzeretMono.txt`.

## AI use during development

Upstream PULSE was built with AI assistance and said so. This fork is too: an AI coding assistant
(Anthropic's Claude) wrote a large share of the changes under the maintainer's direction and review. Every
change is built, unit-tested and lint-checked, and every feature and fix is validated on an actual Retroid
Pocket 6, installed, exercised, and read back from the hardware, before it is committed. What was
verified, and how, is recorded in `PROGRESS.md`.
