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

	private static final int MIN_DURABILITY = 10;

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

    /** Returns false when breaking the block would ruin a tool or lose the block's drop. */
    public static boolean selectBestTool(Player player, net.minecraft.world.level.block.state.BlockState state) {
        Inventory inv = player.getInventory();
        int current = inv.selected;
        int best = -1;
        float speed = -1;
        if (usable(player, inv.getItem(current), state)) { best = current; speed = inv.getItem(current).getDestroySpeed(state); }
        for (int i = 0; i < 9; i++) {
            float candidate = inv.getItem(i).getDestroySpeed(state);
            if (candidate > speed && usable(player, inv.getItem(i), state)) { best = i; speed = candidate; }
        }
        if (best < 0) return false;
        inv.selected = best;
        return true;
    }

    private static boolean usable(Player player, ItemStack stack, net.minecraft.world.level.block.state.BlockState state) {
        // Creative mining costs no durability and never drops the block.
        if (player.getAbilities().instabuild) return true;
        if (state.requiresCorrectToolForDrops() && !stack.isCorrectToolForDrops(state)) return false;
        return !isLowDurability(stack);
    }

	public static boolean isLowDurability(ItemStack stack) {
		if (stack.isEmpty() || !stack.isDamageableItem()) return false;
		return (stack.getMaxDamage() - stack.getDamageValue()) <= MIN_DURABILITY;
	}
}
