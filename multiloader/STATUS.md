# Multiloader port - paused

This folder is a separate, self-contained Gradle project (Fabric + NeoForge via
Architectury). It does NOT affect the regular single-loader Fabric mod in the
parent folder - that one builds and runs on its own, completely independently
of anything in here.

## Why this is paused

Architectury Loom (the tooling this project depends on for the Fabric+NeoForge
split) cannot currently build anything targeting Minecraft 26.1+ at all. This
is a real upstream bug, not a config issue in this project:

https://github.com/architectury/architectury-loom/issues/328

Minecraft 26.1+ ships fully unobfuscated code, so there's no traditional
"mappings" to resolve anymore. Plain Fabric Loom (used by the regular
single-loader project one folder up) was updated to handle this. Architectury
Loom - a fork of Fabric Loom - has not been updated for it yet, as of the date
below. The error this throws is:

  Failed to setup Minecraft, java.lang.RuntimeException:
  Failed to find official mojang mappings for 26.2

Checked: 2026-07-25. Issue was open, unresolved, no linked PR.

## What's actually done here

The common/fabric/neoforge code split itself is real, working logic - state
machine, HUD, config, keybinds, mixins, both loaders' entrypoints. It's the
Architectury Loom *tooling* that's blocked, not the mod code. If picking this
back up:

1. Check if architectury-loom#328 has been resolved - if so, this may just work.
2. If not, the fallback plan (discussed but not started) is to drop Architectury
   entirely: use plain net.fabricmc.fabric-loom for the fabric/ subproject
   (known to work) and NeoForge's own official ModDevGradle plugin for
   neoforge/ (not Loom-based at all), replacing Architectury's
   @ExpectPlatform/KeyMappingRegistry conveniences with manual per-loader
   wiring. Most of the actual game logic in common/ would carry over as-is.

## 26.3 audit — 2026-10-06

The parent Fabric project now targets Minecraft 26.3. This independent project remains a paused 26.2 snapshot and is not a validated 26.3 release. Architectury issue #328 is still open when checked on this date. Its source and dependency versions have intentionally not been partially relabeled as 26.3.

A complete port must address:

- Architectury Loom's mappings requirement for unobfuscated Minecraft, or replace the build with Fabric Loom and NeoForge ModDevGradle.
- Minecraft, both loader versions, Architectury API, Fabric API, YACL and Mod Menu dependencies, both mod manifests, and the Gradle wrapper as a coordinated update.
- Removed GLFW imports and `InputConstants.Type.KEYSYM`: 26.3 uses `InputConstants.Type.KEYBOARD` and Minecraft key constants (or SDL scancodes).
- Removed `AxeItem`: use `ItemTags.AXES` for hotbar axe detection.
- YACL coordinates: the parent uses `dev.isxander:yet-another-config-lib`; this experiment uses old loader-suffixed artifact names and shares a Fabric-suffixed version with NeoForge. Resolve the correct artifact independently per loader.
- NeoForge HUD registration and the shared packet mixin need compilation and runtime validation against the chosen 26.3 dependencies.
- The common code is an older feature snapshot: it lacks the parent's custom enchantment target screen and N shortcut. A version bump alone does not synchronize those features.

References: https://fabricmc.net/2026/09/15/263.html and https://github.com/architectury/architectury-loom/issues/328 .
