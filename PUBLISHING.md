# Publishing releases on Modrinth

Build with `./gradlew buildAll`. Upload the universal JAR from `build/releases`, using the version from `gradle.properties`. Do not rename an older JAR to make a new release.

For a universal upload:

- Select **Fabric** and exactly the release-enabled Minecraft versions in `versions.json`.
- Add **Fabric API** (`P7dR8mSH`), **YetAnotherConfigLib** (`1eAoo2KR`) and **Mod Menu** (`mOgUt4GM`) as **required project dependencies**. Do not pin one dependency file across all Minecraft versions.
- Upload only the universal JAR as the primary file. Individual version JARs belong in separate releases, each tagged with its one Minecraft version.
- Dependency requirements inside `fabric.mod.json` and the dependency list on Modrinth are separate. Verify both; an empty Modrinth dependency list can leave a fresh install missing libraries.
- The libraries' own dependencies must also be installed for the selected game, including Fabric Language Kotlin where YACL requires it and Text Placeholder API where Mod Menu requires it.
- Keep one Villager Reroll JAR in a player's mods folder. Replace the previous file rather than keeping old and new releases together.

## Unexpected Minecraft versions in a Fabric error

The universal JAR contains multiple candidates with exact Minecraft constraints. Fabric selects one compatible implementation. During a failed resolution, Fabric's **More details** section can describe another candidate while computing a repair; that does not mean it successfully loaded that candidate or changed the instance's Minecraft version.

First follow the main recommendation to install a missing library or update Fabric Loader. The minimum Loader versions are listed in README.md. In particular, Loader 0.18.3 is below this project's minimum for the older supported Minecraft releases.

The wrapper now declares common libraries directly and requires its exact release of the implementation. This improves missing-library recommendations and prevents a different standalone release from satisfying the bundle. Loader may still mention an unrelated nested candidate in its additional details. We do not modify Fabric Loader's error renderer.

## Regression checks

`./gradlew testUniversalResolution` runs the real Fabric resolver without opening Minecraft. It checks correct selection, missing libraries, older/newer standalone duplicates, unsupported Minecraft versions and insufficient Loader versions. Reports are saved under `build/reports/resolution`. These checks also run during `buildAll`.

The resolver fixtures use the real packaged metadata and simple library placeholders to isolate selection and failure reporting. They complement the existing packaged-client suites, which exercise actual libraries and gameplay.

Sources reviewed:

- [Fabric nested JAR behavior](https://docs.fabricmc.net/develop/loader/)
- [Fabric's resolver and failure-repair implementation](https://github.com/FabricMC/fabric-loader/blob/0.19.5/src/main/java/net/fabricmc/loader/impl/discovery/ModSolver.java)
- [Forgix multiversion implementation](https://github.com/PacifistMC/Forgix/blob/main/src/main/java/io/github/pacifistmc/forgix/core/Multiversion.java): its Fabric path also uses nested JARs and a wrapper dependency on the mod ID.
- [Modrinth update lookup](https://docs.modrinth.com/api/operations/getlatestversionsfromhashes/)
