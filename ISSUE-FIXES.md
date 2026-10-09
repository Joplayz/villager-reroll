# GitHub issue and PR fixes — Minecraft 26.3

Reviewed all six entries returned by GitHub's all-state issue list: issues #1, #3, #4, #5, #6 and PR #2. All were open. Read the issue bodies and comments, PR description and conversation comment, all nine changed files, and checked the PR review and inline-comment endpoints (both empty). No GitHub issues were closed and no PR was merged or pushed.

| Report | Changes in the active Fabric project |
| --- | --- |
| [#1: crash typing enchantment ID](https://github.com/Joplayz/villager-reroll-26.1.2/issues/1) | Autocomplete now updates suggestion data without adding/removing widgets from an input callback. Partial IDs are safe while typing; validation occurs on save. Layout actions are deferred until a screen tick. Tested typing `minecraft:` and autocomplete in the real client. The posted log only shows a shutdown watchdog, so the exact cause of the reporter's original crash cannot be proved from it. |
| [#3: resume without losing statistics](https://github.com/Joplayz/villager-reroll-26.1.2/issues/3) | Pause/Resume and Retry preserve attempts and active game time. Missing workstations, lost villagers, trade timeouts and manual stops become recoverable pauses. New session resets counters explicitly. Paused time is excluded. |
| [#4: specific villager trades](https://github.com/Joplayz/villager-reroll-26.1.2/issues/4) | Added multiple regular item searches with output item, minimum quantity, primary payment item/limit and optional secondary payment. Added all 13 vanilla profession workstations and best available hotbar tool selection. Stops on any active match and reports the exact offer. Only novice/unlocked trades can be rerolled; the mod does not level up villagers or purchase trades. |
| [#5: tenth entry obscured](https://github.com/Joplayz/villager-reroll-26.1.2/issues/5) | Replaced the unbounded widget rows with a vanilla scrollable list and separate editor. Footer remains outside the list. Tested 15 entries at 640×480 and resizing to 1000×600; drafts persist through resizing. |
| [#6: remember results/better offers](https://github.com/Joplayz/villager-reroll-26.1.2/issues/6) | Saved best price, quantity, enchantment level, secondary cost, description and status per search. Explicit Search better / Acquired / Ignore / Reset actions. Equal or worse prices do not notify. Acquired/ignored searches are suppressed and can be reactivated. Shows minimum price information with a caveat for discounts/datapacks. |
| [PR #2: Russian localization and fixes](https://github.com/Joplayz/villager-reroll-26.1.2/pull/2) | Adapted English/Russian translations and translatable settings, HUD, status and chat messages, extending them for the new features. Keybinding messages use the actual bound key. Preserved a terminal FOUND state rather than copying the PR's IDLE change; tests confirm matching stops all automation. |

## Additional fixes supporting these reports

- Merchant offers are captured after vanilla applies the packet on the client thread, tied to the menu ID, and cleared before each request.
- Trade-screen waits have a timeout instead of hanging forever.
- Workstation/profession checks are specific; unexpected replacement blocks are never broken.
- Locked villager trades pause automation before workstation destruction.
- Existing enchantment configs load into the extended search model. Null target lists and invalid JSON no longer crash startup.
- The selected workstation is saved separately; the original lectern coordinate field names remain for config compatibility.

## Original 26.3 validation (backport validation is recorded in BACKPORT-NOTES.md)

- `gradlew.bat build`: passes, including Java compilation, access-widener validation, and a JUnit regression test containing 24 assertions.
- `gradlew.bat runClientGameTest`: passes with the project's Minecraft version. Tests scrolling, typing, resizing, saving, persistence, action buttons, item fields, YACL configuration, actual item/book offers, stopping after a match, rejecting an unchanged price, pause/resume counters, missing-resource pause and refill, locked-trade protection, and the Thorns III vanilla reference floor.
- English and Russian contain the same 149 translation keys with matching placeholder counts; referenced literal translation keys resolve.
- Screenshots were inspected for the small-window list, autocomplete, item editor and minimum-price display.

The test world uses controlled offers and an integrated server. No testing was performed on the reporter's multiplayer server, server plugins, every workstation orientation, or every possible datapack. The separate paused `multiloader/` experiment is not part of this build and was not rewritten.
