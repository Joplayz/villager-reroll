package com.joplayx.client;

import com.joplayx.VillagerReroll;
import com.joplayx.client.config.RerollerConfig;
import com.joplayx.client.hud.RerollerHud;
import com.joplayx.client.state.RerollController;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.KeyMapping;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;

public class VillagerRerollClient implements ClientModInitializer {

	private static final String PREFIX = "villager-reroll.";

	public static KeyMapping startStopKey;
	public static KeyMapping emergencyStopKey;
	public static KeyMapping setPositionKey;

	private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
			Identifier.fromNamespaceAndPath(VillagerReroll.MOD_ID, "villager_reroller")
	);

	public static final RerollController CONTROLLER = new RerollController();

	@Override
	public void onInitializeClient() {
		RerollerConfig.load();

		// J — Start / Stop
		startStopKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.villager-reroll.start_stop",
				InputConstants.Type.KEYSYM,
				GLFW.GLFW_KEY_J,
				CATEGORY
		));

		// K — Emergency Stop
		emergencyStopKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.villager-reroll.emergency_stop",
				InputConstants.Type.KEYSYM,
				GLFW.GLFW_KEY_K,
				CATEGORY
		));

		// L — Set lectern position to where player is standing
		setPositionKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.villager-reroll.set_position",
				InputConstants.Type.KEYSYM,
				GLFW.GLFW_KEY_L,
				CATEGORY
		));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (client.player == null) return;

			while (startStopKey.consumeClick()) {
				CONTROLLER.toggleStartStop(client);
			}

			while (emergencyStopKey.consumeClick()) {
				CONTROLLER.emergencyStop(client);
			}

			while (setPositionKey.consumeClick()) {
				// Use the block the player is looking at (crosshair hit result)
				if (client.hitResult instanceof net.minecraft.world.phys.BlockHitResult blockHit) {
					BlockPos pos = blockHit.getBlockPos();
					RerollerConfig.get().setLecternPos(pos);
					RerollerConfig.save();
					client.player.sendSystemMessage(Component.translatable(
							PREFIX + "chat.position_set",
							String.valueOf(pos.getX()),
							String.valueOf(pos.getY()),
							String.valueOf(pos.getZ())
					));
				} else {
					client.player.sendSystemMessage(Component.translatable(
							PREFIX + "chat.look_at_block",
							setPositionKey.getTranslatedKeyMessage().getString()
					));
				}
			}

			CONTROLLER.tick(client);
		});

		HudElementRegistry.attachElementBefore(
				VanillaHudElements.CHAT,
				Identifier.fromNamespaceAndPath(VillagerReroll.MOD_ID, "reroller_hud"),
				RerollerHud::extractRenderState
		);

		VillagerReroll.LOGGER.info("[VillagerReroll] Client initialized.");
	}
}
