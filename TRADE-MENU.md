> Historical notes from the earlier standalone projects. See [README.md](README.md) for the current master project, supported releases and build instructions.

# Trade-menu search

Implemented for Minecraft 26.3, 26.2, 26.1.2 and 1.21.11.

1. Place the workstation. When exactly one nearby novice acquires the matching profession and only one matching workstation is nearby, the mod selects the pair automatically. It waits for Minecraft to assign the job; it does not assign jobs itself.
2. Open that villager's normal trading screen and click **Find trades**. The search panel stays inside the live merchant screen, keeping the same open trading container.
3. Search by a normal name. Librarians show tradeable enchanted books. Other professions show their vanilla novice trades, including the items they buy. Armorers use blast furnaces, toolsmiths use smithing tables, and weaponsmiths use grindstones.
4. Select a result, set the minimum level/quantity and maximum price, then choose **Add to wishlist** or **Save and start**. Additional payment limits appear for two-input item trades. Existing rerolling safeguards still apply.
5. **Back to trades** returns to the same normal trading screen. Escape closes the search panel first. Resizing preserves the search and current edits.

If several workstations are nearby, use **Change jobsite** to choose the correct coordinates. Green markers show the selected block and villager. Starting is blocked until a valid workstation is selected. The client cannot prove a multiplayer villager's workstation ownership; automatic pairing uses nearby candidates and the profession reported by the server.

Locked trades cannot be rerolled. Higher-level trades are excluded because leveling the villager locks its offers. Experimental trade rebalance and server datapacks can change the vanilla suggestions; the existing custom search editor remains available.

Searches created in the trade panel are saved for that workstation/profession, and wishlist export/import preserves that association. Older and custom searches retain their existing behavior. Found results still require review before another reroll starts.

See DETAILED-HUD.md for the current 3.1.0 search-list, recovery and HUD update.
