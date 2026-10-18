# 4.1.1 universal dependency patch — October 16, 2026

Investigated the published 4.1.0 universal JAR, public Modrinth metadata, local profiles, Fabric Loader's resolver implementation and Forgix's multiversion implementation. Modrinth's update endpoint selected Universal 4.1.0 for every supported game. The locally installed 4.1.0 run on Minecraft 26.3 selected `4.1.0+mc26.3` correctly. No exact failing session from the user's reported update was recovered, so the reproduced failure is not presented as a confirmed diagnosis of that session.

Reproduced misleading nested-version messages when required libraries are missing or Loader is too old. For example, a 1.21.1 fixture on Loader 0.18.3 can mention `mc1.20.1` while recommending a Loader update. This is failure explanation, not a successfully loaded wrong implementation. Two local profile logs recorded Loader 0.18.3. The published 4.1.0 Modrinth release also had an empty dependency list at inspection time.

Changes:

- The universal wrapper declares Fabric API, YACL and Mod Menu directly, so the primary repair recommendation names the missing library.
- The wrapper requires its exact implementation release, preventing a different standalone release from satisfying it. A regression fixture reproduced the previous `>=` constraint selecting an unrelated newer standalone implementation; the exact constraint selects the bundled implementation instead.
- Added real Fabric resolver checks to `buildAll`, plus publishing instructions in PUBLISHING.md. These do not open game windows or affect user profiles.

Validation: `buildAll` passed with **244 resolver scenarios across five Fabric Loader versions**, **77 unit tests**, and archive metadata/remapping validation. Resolver reports: {"0.18.3": 12, "0.18.4": 43, "0.19.2": 58, "0.19.3": 63, "0.19.5": 68}. Fixtures include all eleven games, missing libraries, duplicate older/newer standalone releases, unsupported 1.21.9, and insufficient Loader versions. Library placeholders isolate the solver; these checks do not replace gameplay tests.

Every entry of each of the eleven implementation JARs matches published 4.1.0 byte-for-byte except `fabric.mod.json` version metadata. Gameplay was therefore not relaunched for this metadata-only patch. Existing gameplay validation is recorded below. Fabric may still mention an unrelated candidate in its additional error details; this patch does not modify Fabric Loader's error renderer or install missing libraries automatically.

All twelve 4.1.1 JARs were copied to `C:/Users/josia/Desktop/Mods/Mod Builds` and checksum-verified. Existing published 4.1.0 files were preserved. Nothing was pushed or published; user profile files were not changed. The Modrinth release still needs required project dependencies configured when uploading, as documented in PUBLISHING.md.

Universal SHA-256: `935ce7676ac7a2be6364c3a9d52eac5e28a15b22b74f0520dca59590dba364df`. Checksums: `build/releases/checksums.json`. Detailed investigation and build logs are in the Codex task's `work/universal-diagnostics` directory.

---

# Expanded 4.0.0 release validation — October 12, 2026

Added the five requested older releases: **1.20.1, 1.21.1, 1.21.5, 1.21.8 and 1.21.10**. The existing six targets remain supported. Release version remains **4.0.0**.

The detailed HUD now combines information into wider, shorter rows. Move HUD has independent overall, width and height controls, saved on Done and discarded on Cancel. Fixed malformed multiplication signs in English trade messages and added an encoding regression test. Older autocomplete suggestions and novice catalogs were checked against their actual game versions. Reference metadata and API sources are indexed in REFERENCES.md.

| Minecraft | Runtime tested | Unit tests | Packaged universal client checks |
| --- | --- | --- | --- |
| 1.20.1 | Java 17 | 7 passed | Passed |
| 1.21.1 | Java 21 | 7 passed | Passed |
| 1.21.5 | Java 21 | 7 passed | Passed |
| 1.21.8 | Java 21 | 7 passed | Passed |
| 1.21.10 | Java 21 | 7 passed | Passed |
| 1.21.11 | Java 21 | 7 passed | Passed |
| 26.1 | Java 25 | 7 passed | Passed |
| 26.1.1 | Java 25 | 7 passed | Passed |
| 26.1.2 | Java 25 | 7 passed | Passed |
| 26.2 | Java 25 | 7 passed | Passed |
| 26.3 | Java 25 | 7 passed | Passed |

`buildAll` passed, including all **77 unit tests** and packaging validation. Client checks passed on all **11** supported Minecraft versions. They cover trade matching, saved better offers, pause/resume and resource recovery, merchant-menu searches, scrolling lists, HUD layout, movement and independent scaling. The oldest two versions use a separate client runner because their Fabric APIs predate the modern client-test API. Tests ran in isolated worlds with Minecraft audio muted before launch.

The first expanded universal artifact passed every version. After correcting the five older ports' main-menu autocomplete names, the final artifact was rebuilt and those five packaged client suites passed again. SHA-256 comparison confirmed that the other six nested implementations were unchanged from their successful full runs. All eleven nested JARs match the final individual release JARs byte-for-byte. Automated checks are not a claim of compatibility with every third-party mod or server.

All **12 final JARs** were copied to `C:/Users/josia/Desktop/Mods/Mod Builds` and checksum-verified. Earlier matching 4.0.0 files were backed up in the Codex task's `work/older-versions/before-release-jars` directory. Earlier 3.1.0 builds were preserved. Nothing was pushed or published.

SHA-256 checksums are recorded in `build/releases/checksums.json`.

Universal JAR SHA-256: `276ece9dbfad3b5e54d201ba0c2c71bb0b581eebfde6f341c336c8087f94b0b9`.

Validation logs in the Codex task's `work/older-versions` directory:

- `release-validation.log`: first two universal targets passed; the following target stopped on an unavailable offline dependency.
- `release-validation-resumed.log`: remaining nine universal targets passed after resolving that dependency.
- `autocomplete-check.log`: unit checks for all five updated older ports passed.
- `final-validation.log`: final build and all five older packaged client suites passed.

---

# Historical initial 4.0.0 validation — October 12, 2026

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
