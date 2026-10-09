# Villager Trade Reroller — HUD-fixed release builds, 2026-10-09

The four JARs are separate builds. Install only the JAR matching your Minecraft version, along with its dependencies. These are client-side Fabric mods. The internal mod version remains 1.0.0.

| Minecraft | Fabric Loader tested | Fabric API | YACL | Mod Menu | Java bytecode |
|---|---|---|---|---|---|
| 26.3 | 0.19.5 | 0.162.0+26.3 | 3.9.7+26.3-fabric | 21.0.0 | 25 |
| 26.2 | 0.19.3 | 0.155.2+26.2 | 3.9.5+26.2-fabric | 20.0.1 | 25 |
| 26.1.2 | 0.19.2 | 0.148.2+26.1.2 | 3.9.3+26.1-fabric | 18.0.0-alpha.8 | 25 |
| 1.21.11 | 0.18.1 | 0.141.5+1.21.11 | 3.8.2+1.21.11-fabric | 17.0.0 | 21 |

The 1.21.11 build also requires Text Placeholder API 2.8.1+1.21.10. Install dependencies required by those libraries as indicated by your mod manager, including Fabric Language Kotlin for YACL.


## Corrected mouse input and direct HUD control

This bundle supersedes the 2026-10-08 builds. All four projects now use Minecraft's
left-click constant when grabbing, dragging and releasing the HUD. In 26.3 that
constant is 1 rather than 0, which caused the reported drag failure.

Each main configuration screen now has a **Move HUD** button next to the workstation
selector. Press **N → Move HUD**, drag the preview and click **Done**. The previous
Settings → Display route remains available.

The old test called screen handlers directly and missed the 26.3 defect. The new
test reproduced that defect before the fix and now passes through Minecraft's
mouse input path in all four versions. It opens the editor, drags, releases, saves,
reloads, resizes, cancels a reset and saves a reset using real mouse events.

| Version | Compilation/unit checks | Mouse-input and trade client tests | Release JAR |
|---|---|---|---|
| 26.3 | Passed | Passed | Built |
| 26.2 | Passed | Passed | Built |
| 26.1.2 | Passed | Passed | Built |
| 1.21.11 | Passed | Passed | Built and production-client tested |

See [GITHUB-HUD-AUDIT.md](GITHUB-HUD-AUDIT.md) for the five issues and PR #2,
their implementations, tests, reference checks and remaining test limitations.
The 1.21.11 production-client test was rerun against this exact updated JAR.

## Included changes

- Safe enchantment autocomplete and a scrolling search list with a separate editor.
- Book and regular-item searches, payment limits and all 13 profession workstations.
- Saved best offers, Search better / Acquired / Ignore / Reset actions.
- Pause/resume preserves attempts and active time; New session resets them.
- Correct handling of fresh merchant packets, trade timeouts, missing resources and locked trades.
- English and Russian translations.
- A compact, outlined, four-line HUD with a saved draggable position.
- Removed an unused legacy merchant accessor from every build.

Move the HUD: press **N → Move HUD**, drag the preview, then choose **Done**. Cancel/Escape discards edits. Reset restores the upper-left corner. The panel adapts to window size and GUI scale. Its four lines show status, attempts/time and controls; target details and saved offers are available in the search screens, with matching offers reported in chat.

## 26.3 F5 launch correction

The failing IntelliJ configuration omitted `-XX:StackShadowPages=32`. Minecraft 26.3's launch metadata requires this option; [Fabric documents the native-crash fix](https://github.com/FabricMC/fabric-loom/pull/1624).

The 26.3 project now supplies the option in generated IDEA/Eclipse launch configurations and IntelliJ's delegated `net.fabricmc.devlaunchinjector.Main.main()` task. This also covers an older cached IntelliJ configuration. Reload the Gradle project, then launch Minecraft Client with F5.

The user's recorded terminal launches with and without the reroller both completed successfully with this option. The updated automated 26.3 client launch also completed successfully. The user's next interactive F5 launch has not been observed. A mod JAR cannot set Java arguments for a third-party launcher.

## Validation

All four versions passed compilation, unit regression checks and automated Minecraft client tests. The 26.3 client test was rerun after the launch correction. Final 26.x Gradle builds passed after the unused accessor was removed; 1.21.11 unit tests and release remapping passed after removal.

Client tests cover partial-ID typing, scrolling 15 targets in a small window, resizing, editing and saving searches, best-offer actions, actual item/book merchant offers in a generated world, stopping on a match, unchanged-price rejection, pause/resume, missing-workstation recovery, locked-trade protection, and HUD dragging, persistence, cancellation and reset. Screenshots were inspected on all four versions. Existing user worlds were not used.

Release JAR checks verify Minecraft metadata, Java class versions, matching translation keys, expected mixin configuration, absence of test classes and the removed accessor. The 1.21.11 JAR's Minecraft references, mixin injection target and access widener use intermediary names required for distribution. SHA-256 checksums are supplied in SHA256SUMS.txt.

The sandbox reports Java ZIP filesystems read-only despite allowing ordinary file writes. For 1.21.11, the release was remapped with Loom's own configured remapping service into a directory and packaged with standard ZIP output. It is a remapped release JAR, not a development JAR. The project's normal Gradle packaging configuration is retained for normal terminal builds.

The exact packaged 1.21.11 release JAR also passed the full client test suite through Loom's production client launcher, using intermediary Minecraft names and development mode disabled. The test entered a world, matched item and book offers, exercised the screens and HUD, and exited normally. Minecraft's test cache was prepared with Fabric's remapper using the same directory/ZIP method to avoid the environment's ZipFS issue. The separate test mod and Minecraft files are not included in this release bundle.

Testing uses controlled offers and an integrated server; it does not cover every multiplayer plugin, datapack or workstation orientation. No release was uploaded and no GitHub changes were pushed. The paused multiloader experiments are unchanged.
