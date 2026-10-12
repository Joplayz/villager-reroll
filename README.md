# Villager Trade Reroller — master project

This is the main project for future development. One shared source tree builds every supported release and a single universal Fabric JAR.

## Supported Minecraft releases

All stable releases from 1.21.11 through 26.3 are included:

| Minecraft | Java minimum | Fabric Loader minimum |
| --- | --- | --- |
| 1.21.11 | 21 | 0.18.4 |
| 26.1 | 25 | 0.19.2 |
| 26.1.1 | 25 | 0.19.2 |
| 26.1.2 | 25 | 0.19.2 |
| 26.2 | 25 | 0.19.3 |
| 26.3 | 25 | 0.19.5 |

Snapshots, pre-releases and release candidates are not included. The release list is checked against [Fabric's game-version metadata](https://meta.fabricmc.net/v2/versions/game).

## Install

Use **VillagerReroll-Universal-4.0.0.jar** for any supported release. Install the Fabric API, YACL and Mod Menu downloads appropriate for your Minecraft version, plus their required dependencies. Mod Menu 17 on 1.21.11 also requires Text Placeholder API.

Install either the universal JAR or the individual JAR for your Minecraft version. Do not keep a second Villager Reroll JAR in the same mods folder.

Fabric chooses the matching implementation inside the universal file. The individual implementations have exact Minecraft constraints and keep the original `villager-reroll` mod ID and config filename, so existing searches/settings remain compatible. The wrapper contains no game classes and does not force the newer versions' Java or Loader requirements on 1.21.11.

## Features

- Multiple book or regular item targets, with quantity/level and payment limits; any matching active target can stop the reroller.
- Search inside the villager trade menu, with profession-specific novice suggestions and workstation selection.
- Saved best offers, searching for better prices, acquired/ignored statuses, reset and reactivation.
- Scrollable searches, named wishlists and import/export.
- Pause/resume preserving attempts and active elapsed time, with interruption reasons.
- Compact or detailed HUD with target status, requested prices and saved offers.
- Movable HUD, 50–150% size controls and optional edge/center snapping.
- Rebindable controls and configurable found sound/notifications.

Only unlocked novice trades can be rerolled. Automatic workstation pairing uses nearby candidates; the client cannot prove a multiplayer villager's jobsite ownership. Custom searches remain available for servers with modified trades.

## Open in IntelliJ

Open this folder as a Gradle project and select **JDK 25 or newer** for the Gradle JVM. Reload the Gradle project after changing the build configuration. The root project coordinates builds. Every Minecraft release, including 26.3, has its own `mc...` module under `versions/`.

The run menu includes **Build all releases**, **Test universal on all versions**, and **Minecraft (version)** launch shortcuts. Terminal commands also require `JAVA_HOME` to point to JDK 25 or newer.

Edit `shared/src/main/java` and `shared/src/main/resources` in this master project. All modules' Java sources are generated under their `build/generated/version-source` folders. Do not edit those generated files or copy features back into the old standalone projects.

## Build and test

From IntelliJ's terminal on Windows:

```powershell
.\gradlew.bat buildAll
```

This compiles every target marked `release: true` in `versions.json`, runs its unit tests and creates `build/releases/` containing the universal JAR and the individual release JARs. Currently there are six supported releases. The output folder is synchronized on every build so removed targets and stale JARs cannot remain in the release set. The ordinary root `build` task does the same. No separate Python script or old project folder is needed.

```powershell
.\gradlew.bat testUniversalAll
```

This launches each supported Minecraft release sequentially with the **same packaged universal JAR**, verifies its selected implementation and runs the client feature tests. Test worlds/settings are isolated under each module's `build/run/universal-test` directory. The tests exercise mouse input, so leave the test windows alone while they run.

Packaged-release tests use Java 21 for Minecraft 1.21.11 and Java 25 for 26.x. Install both JDKs for this full test command; Gradle itself still runs on JDK 25 or newer.

For one version, use `:mc26.1:testUniversalClient` or `:mc26.3:testUniversalClient`. Development runs use `:mc26.1:runClient` or `:mc26.3:runClient`. `testAllClients` runs the development client suites instead of the packaged release.

## Project layout

- `shared/src/`: shared mod, resources, unit tests and client tests, currently written against the 26.3 API.
- `versions.json`: single list of Minecraft targets, pinned dependencies, Java levels, compatibility families, remapping and release eligibility.
- `versions/<minecraft>/`: one module per Minecraft version; its `src/` holds only overrides and version-specific resources.
- `gradle/compatibility/`: an explicit source adapter for each API family. Existing families share translations in `gradle/version-sources.gradle`; new families can define their own adapter without changing older ones.
- `gradle/version.gradle`: common compilation, dependency, metadata and resource configuration.
- `gradle/production-tests.gradle`: packaged-release testing.
- `versions/<minecraft>/src/main/resources/`: verified vanilla trade catalogs and access wideners for that target. These override shared resources.
- `references/`: local source references and downloaded compatibility metadata; excluded from new Git additions.

Shared features are changed once. If a new feature calls an API that differs between Minecraft releases, update the compatibility translations and test all supported versions. Keep exact Minecraft constraints in the generated release metadata so the universal JAR cannot select the wrong implementation.

## Adding future versions

Follow [ADDING-VERSIONS.md](ADDING-VERSIONS.md) when a new Minecraft version and its required dependencies/references are available. There is no 26.4 implementation or compatibility claim yet.

New targets start with `release: false`. They are excluded from ordinary Gradle configuration, builds, universal metadata, and release downloads. Enable work on them explicitly with `-PincludeDevelopment=true`; a broken or unavailable development dependency then cannot interfere with a normal release build.

Modules, release packaging and test targets are derived from `versions.json`. Run `generateIdeRuns` after adding a version to refresh IntelliJ shortcuts; `buildAll` also refreshes them. Release candidates are promoted only after their version-specific validation, followed by tests of the actual universal JAR across every supported release.

`org.gradle.jvmargs=-Xmx3G` limits the Gradle build JVM's heap. It does not change Minecraft's memory allocation or the memory settings of players installing the mod.
