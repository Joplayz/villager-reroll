> Historical notes from the earlier standalone projects. See [README.md](README.md) for the current master project, supported releases and build instructions.

# Search lists, trade targets, recovery and detailed HUD

Available in the Fabric projects for Minecraft 26.3, 26.2, 26.1.2 and 1.21.11.

- **Scrollable searches:** Open Manage searches (default N). The list scrolls independently above the action buttons. Entries at the bottom can still be edited and removed.
- **Regular trades:** Add several book or item targets. The reroller stops when any active target matches its minimum quantity/level and maximum payment limits. Two-input trades support a second payment limit. Review the found result to see the item and actual price. The profession picker offers vanilla novice trades: leveling or trading with a villager locks its offers.
- **Resume:** Missing workstation items and other recoverable interruptions pause the session and show the reason. Resolve the problem, then use Resume / Retry (or the start/stop key). Attempts and active elapsed time are preserved; paused time is excluded. New session deliberately resets the statistics.
- **Detailed HUD:** Open the mod settings through Mod Menu, then Display > Detailed HUD. It shows the workstation, attempts, elapsed time, each target's status and price limit, and its saved best offer. Searching, Found, Acquired and Ignored have distinct labels. Compact mode remains the default.
- **More targets:** Press H to move through HUD pages. Rebind Next HUD page in Minecraft's Controls menu if needed. New found results automatically bring the relevant page into view.
- **Move the HUD:** Use the HUD positioning screen. Its preview uses the same layout, stays above the buttons, and has a Next page button when detailed mode is enabled.

The existing saved-result actions remain available: Search better keeps the best offer and only reports an improvement; Keep this result marks the search acquired. Ignore, reactivate and reset are available in the search editor. These actions do not make a Minecraft trade for the player.

## Validation

The added client test uses 20 search entries and three window/UI-scale combinations, scrolls to the last entry, opens its editor and removes it. It also checks saved HUD preferences, pagination, status/price text and positioning-screen bounds. Existing client tests exercise real merchant offers, an item match alongside a nonmatching book target, missing-resource recovery, preserved attempts/time and deliberate session reset.

Release files keep the current 3.1.0 version and the name `VillagerReroll-(Minecraft version)-3.1.0.jar`.

## Completed checks — October 11, 2026

All four versions passed six JUnit tests and all four client test suites (detailed HUD/list, merchant interface, convenience features and reroller behavior). The 26.x projects completed normal builds. For 1.21.11, this environment's Java ZIP-filesystem restriction required Loom directory remapping and JAR packaging; all four client suites then passed against that packaged JAR in a production-mode installation. Normal project build configuration was unchanged.

Updated 3.1.0 JARs are in each project's build/libs and in C:/Users/josia/Desktop/Mods/Mod Builds. Copy checksums match. Prior copies were backed up in the Codex task's work/detailed-hud/before-release-jars directory.
