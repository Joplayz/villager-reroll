# Convenience update

Implemented in the four Fabric projects for Minecraft 26.3, 26.2, 26.1.2 and 1.21.11.

## Using the new features

- **L: select a villager or workstation.** Aim at an adult novice. Choose a nearby workstation, preview the green markers in the world, and confirm the pair. The mod remembers the selected villager for this connection rather than switching to a closer villager when resuming. Aiming at a workstation detects its profession. Existing keybindings remain rebindable.
- **N: manage searches.** Add a book or regular trade by its displayed name. The picker shows item icons and vanilla novice trades for the selected profession. Search can match payment items too: searching for carrot shows the farmer's carrots-for-emerald trade. Custom search keeps direct item IDs and custom-server trades available. Icons are displayed after joining a world; main-menu configuration remains usable.
- **Wishlists.** Save a named list, duplicate it, add its searches to the current list, or import/export one through the clipboard. Starter lists cover armor books, tool books and emeralds for sticks. Adding a list preserves existing targets/results and asks you to select the new villager/workstation. Exports contain criteria only, not results, coordinates or villager identities. Imports are limited to 100 targets and 64 KB.
- **Saved trades.** Review the actual found level, quantity, both costs and previous best price. Keep this result completes that search; Search better keeps the record and makes the search active again. Use Resume to continue. Next villager clears the selected pair after pending results are reviewed. You still need to make a normal Minecraft trade to lock a villager's offers.
- **Optional HUD detail.** Settings > Display > Show search detail in HUD adds one line with the current target or found offer. Multiple targets show a count. The compact default is unchanged, and the larger preview remains draggable and clamped to the screen.
- **Found alerts.** The existing experience-orb pickup “ching” is preserved and enabled by default. Sound and the new found notification have independent settings.
- **Recovery.** Out-of-reach, missing-workstation-item, blocked-position, unreadable-trade and unavailable-target messages give a concrete next step. Resume / Retry preserves session statistics. Hover that button to read the full current status.

## Limits

The client cannot reliably read workstation ownership from a multiplayer server. Automatic setup therefore offers nearby candidates and a visible confirmation instead of claiming to know ownership. Check the markers, work hours, and villager access. Locked trades still cannot be rerolled.

The trade picker is a vanilla novice reference, not a guarantee of trades on a modified server or with experimental trade rebalance. Custom searches remain available. The 26.x catalogs are extracted from each version's vanilla novice trade data; 1.21.11 uses the corresponding vanilla trades with its original two-input ordering.

## Validation

Unit coverage includes legacy matching, statistics, wishlist round trips, independent copies, invalid imports and previous-best records. Client tests cover main-menu safety, icons and name search in a world, novice filtering, cancelled drafts, wishlist save/load/duplicate/add, alert preference persistence, result actions, optional HUD layout, real mouse dragging, workstation selection/preview/confirmation, actual merchant offers, and locked-trade/resource interruptions.

Test worlds and settings live in each project's build/run/clientGameTest directory. Player worlds are not used. English and Russian translation keys are kept in sync.

The release version is 3.0.0. It has not been published to GitHub or Modrinth.

## Local build notes

All four versions passed six JUnit tests and both client test suites on October 10, 2026. The 26.x projects completed normal Gradle builds. The 1.21.11 project hit the existing Java ZIP filesystem write restriction in this automated environment; its installable JAR was produced with Loom's remapper using directory output, then packaged as a ZIP/JAR. The resulting 1.21.11 JAR also passed both client suites in a production-mode installation. This workaround does not change the mod's game behavior or its normal IntelliJ build configuration.

Fresh installable JARs are in each project's `build/libs` folder, named `VillagerReroll-(Minecraft version)-3.0.0.jar`. Copies of the 3.0.0 release JARs are also in `C:/Users/josia/Desktop/Mods/Mod Builds`. Earlier release files are preserved. The new release has not been published.
