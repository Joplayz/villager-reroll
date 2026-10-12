# Release 4.0.0 — October 12, 2026

The master project now builds version 4.0.0 for all six supported Minecraft versions, both individually and in the universal JAR. This change updates the release version and documentation; gameplay code is unchanged.

`buildAll` passed. All 36 unit tests passed, and release checks verified nested metadata, exact Minecraft constraints, Java/Mixin targets and legacy remapping. An additional archive check confirmed the outer version is 4.0.0, each inner version is 4.0.0+mc<version>, and the six individual JARs match their bundled counterparts byte-for-byte.

All seven 4.0.0 JARs were copied to C:/Users/josia/Desktop/Mods/Mod Builds and checksum-verified. SHA-256 checksums are recorded in build/releases/checksums.json.

Universal JAR SHA-256: `371594eabc3202ff1ceadefe02c2b66c9f54b55e06340238f4e24f0127bf711d`.

The in-game suites were last run on the identical gameplay code at version 3.1.0, as recorded below; they were not rerun solely for this version-number change.

---

# Historical 3.1.0 validation — October 12, 2026

The master project is the main development project. Release version remains 3.1.0.

| Minecraft | Runtime tested | Unit tests | Packaged client suites |
| --- | --- | --- | --- |
| 1.21.11 | Java 21 | 6 passed | 6 passed |
| 26.1 | Java 25 | 6 passed | 6 passed |
| 26.1.1 | Java 25 | 6 passed | 6 passed |
| 26.1.2 | Java 25 | 6 passed | 6 passed |
| 26.2 | Java 25 | 6 passed | 6 passed |
| 26.3 | Java 25 | 6 passed | 6 passed |

The same final universal JAR was installed in every client test. Each run verified the selected nested mod's version and that it came from the universal bundle, then exercised HUD scaling/snapping, scrolling/detailed HUD, merchant-menu searches, convenience features and reroll/recovery behavior. Client tests use isolated worlds under each module's build/run/universal-test directory.

The normal buildAll task passed for all six modules. Packaging checks verified exact Minecraft constraints, Java class versions, Mixin compatibility levels, absence of test classes, and intermediary remapping for 1.21.11. No environment-specific remapping workaround is required for this project.

The two newly supported releases (26.1 and 26.1.1) use novice catalogs extracted from their own vanilla game data. Shared changes, including the latest scalable HUD and optional snapping, are present in every implementation.

The universal JAR and six individual JARs are in build/releases and were copied to C:/Users/josia/Desktop/Mods/Mod Builds. Copies were checksum-verified. Earlier Mod Builds copies were backed up in the Codex task's work/master/before-release-jars directory. Nothing was pushed or published.

Universal JAR SHA-256: `40e9a30a023ef35ca395f7b388650b9dbb1df40541a4b0889e693f2a35f760ae`.

See README.md for IntelliJ setup, dependency requirements and the supported stable-release list. The old standalone projects remain intact; future changes belong in this master project's shared/src tree.

## Future-version structure validation

The root project now coordinates builds; every game target, including 26.3, lives under versions/. All 47 original shared source/resource files were preserved byte-for-byte under shared/src/. Version-specific resources moved into their own modules.

After restructuring, buildAll and testUniversalAll passed together (4 minutes 51 seconds). The packaged client suites ran on all six versions, including 26.3 from its new module. All seven generated JARs exactly match the previously delivered Mod Builds files, including the universal SHA-256 above. The project version remains 3.1.0.

Six isolated checks exercised the actual Gradle settings/matrix scripts: registration of every current target, exclusion of an unavailable future target, rejection of an unknown enabled adapter, default exclusion of a development target, explicit opt-in to that target, and rejection of a missing release decision. All passed. The fake future target existed only in a temporary validation fixture; no 26.4 support is claimed in this project.

The version list drives module registration, release packaging, test tasks and IntelliJ launch shortcuts. Development targets require -PincludeDevelopment=true and remain excluded from universal/release packaging until release is set to true. ADDING-VERSIONS.md documents the prerequisite checks, porting workflow and release validation.
