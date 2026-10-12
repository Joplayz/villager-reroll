package com.joplayx;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.loader.api.FabricLoader;

/** Verifies that release tests load the universal artifact and its correct nested implementation. */
public class UniversalSelectionClientTest implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext context) {
        if (!Boolean.getBoolean("villager-reroll.testUniversal")) return;
        var loader=FabricLoader.getInstance();
        var minecraft=loader.getModContainer("minecraft").orElseThrow().getMetadata().getVersion().getFriendlyString();
        var selected=loader.getModContainer("villager-reroll").orElseThrow();
        var bundle=loader.getModContainer("villager-reroll-bundle").orElseThrow();
        String expected=bundle.getMetadata().getVersion().getFriendlyString()+"+mc"+minecraft;
        if (!selected.getMetadata().getVersion().getFriendlyString().equals(expected))
            throw new AssertionError("Wrong implementation selected for Minecraft "+minecraft);
        if (!selected.getContainingMod().orElseThrow().getMetadata().getId().equals("villager-reroll-bundle"))
            throw new AssertionError("Test must use the nested release implementation");
        VillagerReroll.LOGGER.info("[Universal test] Verified packaged implementation {}",expected);
    }
}
