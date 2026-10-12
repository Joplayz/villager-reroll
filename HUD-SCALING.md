# HUD size and alignment

Open **Move HUD** from the search screen or the mod settings.

- **Smaller / Larger** changes the HUD size by 5%, from 50% to 150%. This applies to compact and detailed mode without changing Minecraft's overall UI scale.
- Click **Size: …%** to restore 100%.
- **Snap: On / Off** controls alignment while dragging. When enabled, guide lines show the edges and center. Drag near a guide to align the panel; away from the guides, movement stays free.
- **Done** saves position, size and snapping. **Cancel** or Escape discards those edits. Resizing the window keeps the draft.
- **Reset position** restores the upper-left position without changing the selected size.
- Detailed HUD pages remain available through **Next page** and the configured HUD-page key (H by default).

The preview reserves room for its buttons. In a small window, the HUD automatically fits a complete target and its wrapped prices into the available height. The selected percentage remains saved and is used where space permits.

Implemented in the Fabric projects for Minecraft 1.21.11, 26.1.2, 26.2 and 26.3. The separate master-project consolidation is deferred.

## Validation — October 11, 2026

All four versions passed six JUnit tests and five client test suites. The new checks exercise draft/cancel/save behavior, 50% and 150% sizes, compact/detailed layout bounds, and real mouse dragging with center snapping. Existing tests cover HUD positioning, search scrolling, trade matching and pause/resume. The 1.21.11 client tests ran against the remapped production JAR; the 26.x projects completed normal builds. Refreshed 3.1.0 JARs are in Mod Builds; prior copies were backed up in this Codex task.
