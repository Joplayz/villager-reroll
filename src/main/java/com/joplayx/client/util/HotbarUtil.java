package com.joplayx.client.util;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Hotbar slot management utilities.
 *
 * Inventory.selected is a public field in 26.1.2 — confirmed not in the
 * transitive access wideners (meaning it's already accessible).
 */
public class  HotbarUtil {

	public static boolean hasItemInHotbar(Player player, Item item) {
		Inventory inv = player.getInventory();
		for (int i = 0; i < 9; i++) {
			if (inv.getItem(i).is(item)) return true;
		}
		return false;
	}

	public static void selectItem(Player player, Item item) {
		Inventory inv = player.getInventory();
		for (int i = 0; i < 9; i++) {
			if (inv.getItem(i).is(item)) {
				inv.selected = i;
				return;
			}
		}
	}

	public static void selectBestAxe(Player player) {
		Inventory inv = player.getInventory();
		for (int i = 0; i < 9; i++) {
			if (inv.getItem(i).is(ItemTags.AXES)) {
				inv.selected = i;
				return;
			}
		}
		// No axe found — leave current slot as-is
	}

    public static void selectBestTool(Player player, net.minecraft.world.level.block.state.BlockState state) {
        Inventory inv = player.getInventory();
        int best = inv.selected;
        float speed = inv.getItem(best).getDestroySpeed(state);
        for (int i = 0; i < 9; i++) {
            float candidate = inv.getItem(i).getDestroySpeed(state);
            if (candidate > speed) { best = i; speed = candidate; }
        }
        inv.selected = best;
    }

	public static boolean isSelectedItemLowDurability(Player player, int threshold) {
		ItemStack selected = player.getMainHandItem();
		if (selected.isEmpty() || !selected.isDamageableItem()) return false;
		return (selected.getMaxDamage() - selected.getDamageValue()) <= threshold;
	}
}
