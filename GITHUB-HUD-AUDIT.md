> Historical notes from the earlier standalone projects. See [README.md](README.md) for the current master project, supported releases and build instructions.

# HUD correction and GitHub audit — 2026-10-09

Applies to the four main Fabric projects: Minecraft 26.3, 26.2, 26.1.2 and 1.21.11.

## HUD correction

The old HUD editor hardcoded mouse button 0. Minecraft 26.3 uses SDL and left-click is button 1; the older versions use button 0. Every project's editor now uses `InputConstants.MOUSE_BUTTON_LEFT` when grabbing, dragging and releasing the panel.

The previous test directly called the screen's mouse methods with button 0. That bypassed the actual input path and incorrectly passed on 26.3. A replacement test using Fabric's input API reproduced the failure before the fix: the saved HUD position did not move. After the correction, tests use real cursor movement and button press/release through Minecraft's mouse handler.

Every project also has a **Move HUD** button next to the workstation selector on the main configuration screen. Press **N → Move HUD**, drag the preview, and choose **Done**. The existing Settings → Display → Move HUD entry remains available. Reset returns to the upper-left corner; Cancel discards the draft. Position is saved and scales with the window.

The exact button-number defect applies to 26.3. The updated 26.2 and 26.1.2 projects are checked independently rather than assuming they share the same cause. Their release builds include the direct entry point and the same version-aware input handling.

## GitHub requests

Rechecked the repository's all-state issues, complete bodies and issue comments, PR description and discussion, nine-file PR diff, and review/inline-comment endpoints. The repository has five issues and one PR. They remain open on GitHub; this work changes the local projects and does not close issues or merge the PR.

| Request | Implementation in all four projects | Verification |
|---|---|---|
| [#1 — crash while entering an enchantment ID](https://github.com/Joplayz/villager-reroll-26.1.2/issues/1) | Typing updates suggestion data without modifying the screen's widget collection. Partial identifiers are accepted during typing and validated when saved; layout-changing actions are deferred. | Client tests type `minecraft:` and `minecraft:th`, verify widget stability, resize, and save. The issue's posted shutdown-watchdog excerpt does not establish the original crash's exact cause; testing does not reproduce that reporter's multiplayer environment. |
| [#3 — resume without losing statistics](https://github.com/Joplayz/villager-reroll-26.1.2/issues/3) | Missing resources and other recoverable interruptions pause automation with a reason. Resume/Retry retains attempts and active time; New session explicitly resets them. | Client tests pause, check counters and paused time, exhaust/refill a workstation item and resume. Unit checks cover session accumulation and reset. |
| [#4 — regular villager trades](https://github.com/Joplayz/villager-reroll-26.1.2/issues/4) | Multiple item or book targets, quantity and primary/secondary payment limits, 13 profession workstations, stop on any active match, matching offer reported in chat and saved. | Unit checks cover item, quantity, payment and secondary-cost filters. Client tests receive and match real item and enchanted-book merchant packets and verify the workstation remains intact after a match. Trades that are already locked cannot be rerolled; the mod does not level villagers or buy trades. |
| [#5 — tenth row covered by buttons](https://github.com/Joplayz/villager-reroll-26.1.2/issues/5) | Search rows use an independently scrolling list; footer controls stay outside it and editing happens on a separate screen. | Client tests create 15 targets at 640×480, scroll to the end, check footer separation and resize. Screenshots are inspected. |
| [#6 — remember results and better offers](https://github.com/Joplayz/villager-reroll-26.1.2/issues/6) | Persisted best offer and status per target; Search better, Acquired, Ignore and Reset actions. Equal/worse offers do not notify again; completed/ignored targets can be reactivated. Book editor displays the vanilla reference minimum and a discount/datapack caveat. | Unit checks cover 21→15→11 emerald improvements, repeated/worse-price rejection, acquired/ignored suppression, reset and persistence. Client tests exercise saved actions, repeated-offer rejection and Thorns III's 11-emerald base reference. |
| [PR #2 — Russian localization and fixes](https://github.com/Joplayz/villager-reroll-26.1.2/pull/2) | English/Russian translations, actual rebound key names, safe autocomplete, and immediate stop when a match is found. | Both languages contain the same 149 keys. Client tests confirm no further attempts or workstation destruction after finding a target. |

The PR's request to delete a matched target was deliberately adapted: the terminal FOUND state stops automation while retaining the target and result, as required by issue #6. The player then chooses Search better, Acquired, Ignore or Reset. This preserves both requested behaviors instead of losing the search history.

## Reference checks and test scope

Used each project's matching Fabric API reference sources, specifically its client game-test input API and mouse-handler accessors. The 26.3 references use SDL mouse input; 26.2, 26.1.2 and 1.21.11 document GLFW input. The compiled Minecraft classes confirm their button constants and screen APIs. The 26.1.2 screen entry point uses `Minecraft.setScreen`; 26.2/26.3 use `Minecraft.gui.setScreen`.

The revised HUD tests open the editor from the main configuration screen using mouse input, drag beyond the screen edge, release and move away, save, reload the config, reopen after resizing, cancel a reset, and save a reset. Other client checks cover the GitHub behaviors above in a generated integrated-server world. Existing user worlds are not used. This does not test every multiplayer plugin, datapack or workstation orientation.

The previous 26.3 native-launch flag correction remains in place. Paused multiloader experiments are outside these four Fabric releases.
