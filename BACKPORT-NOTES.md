> Historical notes from the earlier standalone projects. See [README.md](README.md) for the current master project, supported releases and build instructions.

# Reroller and movable HUD update

Minecraft target: 26.3. The project's Minecraft version and Java bytecode target are retained.

Features carried over from the 26.3 fixes: safe enchantment autocomplete, a scrolling search list, item/payment searches, saved best offers, continue/acquired/ignored/reset actions, pause/resume without losing session statistics, explicit new-session reset, workstation selection and locked-villager protection. English and Russian translations are included.

The compact four-line HUD now has a saved, draggable position. Open the mod configuration (N by default), choose Settings > Display > Move HUD. Drag the preview and click Done to save. Reset position selects the upper-left corner; Cancel and Escape discard edits. Placement adjusts to the available screen area after resizing or changing GUI scale. The HUD remains movable even when rerolling is idle or its display is disabled.

The source changes apply to the main Fabric project. The separate multiloader experiment is unchanged. Backups of the previous source were saved in the Codex workspace before edits.

Update, 2026-10-08: the 26.3 IntelliJ launch omitted Minecraft's required -XX:StackShadowPages=32 option. The 26.3 project now supplies it in generated IDE launch files and cached delegated IntelliJ tasks. The successful terminal logs already included this option; the updated 26.3 client game test also passed. Older versions retain their own launch requirements. The user's next interactive F5 launch has not yet been observed. An unused legacy merchant accessor was removed from all four versions.

Validation: compilation, unit tests and client game tests passed. Release JAR built successfully.
