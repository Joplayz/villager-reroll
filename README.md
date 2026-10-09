# Villager-Reroll

## Setup

For setup instructions, please see the [Fabric Documentation page](https://docs.fabricmc.net/develop/getting-started/creating-a-project#setting-up) related to the IDE that you are using.

## License

This template is available under the CC0 license. Feel free to learn from it and incorporate it in your own projects.


### Search and HUD update

Includes persistent item/enchantment searches, pause/resume statistics, scrolling, safe autocomplete and Russian translations. See ISSUE-FIXES.md. To position the compact HUD: press N, open Settings, select Display, then Move HUD. Drag the preview and click Done. Cancel/Escape keeps the previous position; Reset position restores the upper-left corner.

## HUD input correction — 2026-10-09

Press **N → Move HUD** using the button next to the workstation selector. Drag the
panel and choose Done. All four versions now use their own Minecraft left-click
constant, including SDL input in 26.3. The revised mouse-input and trade tests pass
on all four versions. See GITHUB-HUD-AUDIT.md and RELEASE-VALIDATION.md for details.
