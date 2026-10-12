package com.joplayx;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VillagerReroll implements ModInitializer {

	public static final String MOD_ID = "villager-reroll";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// Server-side init — nothing needed for this client-only mod.
		LOGGER.info("[VillagerReroll] Mod loaded.");
	}
}
