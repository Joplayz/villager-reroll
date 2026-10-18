# Adding a Minecraft version

The root project is a build coordinator, not a Minecraft implementation. Every supported version has a folder under `versions/`. Shared features live under `shared/src/`; older standalone projects are not build inputs.

## 1. Confirm prerequisites

Do not add claimed support simply because a version number has been announced. For 26.4 or any later version, first verify:

- A stable Minecraft release and downloadable development artifacts from the [Mojang version manifest](https://piston-meta.mojang.com/mc/game/version_manifest_v2.json).
- Support in [Fabric game/loader metadata](https://meta.fabricmc.net/v2/versions/game), a compatible Fabric Loader and Fabric Loom, and the required Java version.
- Fabric API, YACL, Mod Menu and all required transitive dependencies explicitly supporting that game version. Check maintainers' metadata, not only a version string that looks similar.
- Usable API/source references for Minecraft, Fabric and the configuration/menu libraries. Store reference provenance under `references/`; Minecraft binaries and decompiled sources remain in local development caches, not release artifacts.

Useful primary sources: [Fabric API](https://github.com/FabricMC/fabric), [Fabric Loader](https://github.com/FabricMC/fabric-loader), [Fabric Loom](https://github.com/FabricMC/fabric-loom), [YACL](https://github.com/isXander/YetAnotherConfigLib), [Mod Menu](https://github.com/TerraformersMC/ModMenu).

## 2. Register a development target

Add the Minecraft version as a key in `versions.json`, with verified values for:

| Field | Meaning |
| --- | --- |
| `java` | Required Java major version, as a number |
| `loader` | Pinned numeric Fabric Loader release |
| `fabricApi`, `yacl`, `modMenu` | Pinned compatible dependency versions |
| `placeholder` | Optional Text Placeholder API dependency, only if needed |
| `family` | Name of an existing verified adapter or a new adapter |
| `remap` | `true` for obfuscated targets requiring intermediary remapping; otherwise `false` |
| `release` | Start with `false`; development targets are excluded from normal releases |

Create `versions/<minecraft>/build.gradle` containing:

```groovy
apply from: rootProject.file('gradle/version.gradle')
```

Create that module's `src/main/resources/villager-reroll.accesswidener` and `src/main/resources/assets/villager-reroll/novice-trades.json` from the new version's APIs/game data. Verify trade data against the new game rather than assuming the previous version's trades match.

No root build or settings version list needs editing. Development targets are loaded only when `-PincludeDevelopment=true` is present. Normal builds remain usable while porting.

## 3. Implement compatibility

Shared sources currently use the 26.3 API. Reuse a family only after confirming its API compatibility. Otherwise add `gradle/compatibility/<family>.gradle`, defining:

```groovy
ext.adaptSource = { String text ->
    // Translate shared source calls to this API family.
    return text
}
```

The identity adapter above is a starting point, not a compatibility assertion. Compile and exercise every relevant feature before accepting it.

For differences too complex for source translation, put a replacement Java file at the same package path in `versions/<minecraft>/src/main/java/`. Only that version uses the replacement. Overrides in `src/test/` and `src/gametest/` work the same way. Resource overrides also use this source layout. Avoid duplicating whole source trees.

Generation first copies shared sources, applies the selected adapter, then overlays exact-version files. Never edit `build/generated/version-source/`; it is recreated. Missing/unknown adapters and missing required version resources fail early for enabled targets.

## 4. Test the development target

For a future 26.4 entry, once its real dependencies exist:

```powershell
.\gradlew.bat -PincludeDevelopment=true :mc26.4:build
.\gradlew.bat -PincludeDevelopment=true :mc26.4:runClientGameTest
.\gradlew.bat -PincludeDevelopment=true :mc26.4:testVersionClient
.\gradlew.bat -PincludeDevelopment=true :mc26.4:runClient
.\gradlew.bat generateIdeRuns
```

These commands are examples for adding 26.4 later; no 26.4 target ships today. The generated development launch shortcut supplies the opt-in flag automatically. Install the target's required JDK. If Loom, Gradle or Java must be upgraded, recheck the existing targets too.

The `testVersionClient` task tests a standalone packaged JAR before release eligibility is enabled. Keep `clientTests` and `testSuite` unset for ordinary future targets using the shared Fabric test suite; the `legacy` and `older` suites are explicitly for the existing old-version ports. Automated clients are muted in their isolated test options.

Exercise actual rerolling, merchant searches, best-price history, pause/resume, scrolling, HUD movement/scaling/snapping, config persistence and notifications. Update tests for legitimate new game behavior.

## 5. Validate and deliver the release

After the development target passes, set its `release` to `true`, then run:

```powershell
.\gradlew.bat buildAll testUniversalAll
```

The universal JAR and test matrix automatically include the new release. This verifies exact Minecraft selection, Java/Mixin levels, remapping where needed, and client behavior using the packaged universal JAR in every release. If validation fails, fix the port or return it to `release: false` before distributing anything.

Update the supported-version table and validation record. Deliver the individual and universal JARs from `build/releases/`, preserving the `VillagerReroll-<minecraft>-<mod version>.jar` naming scheme. The mod version is controlled separately by `mod_version` in `gradle.properties`; adding a Minecraft target does not silently change it.

Future versions require a new tested build; an existing universal JAR only supports the exact releases packaged inside it.
