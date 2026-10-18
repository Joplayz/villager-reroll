package com.joplayx;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
public class NoviceCatalogClientTest implements FabricClientGameTest {
 public void runTest(ClientGameTestContext context){try(var world=context.worldBuilder().create()){world.getServer().runOnServer(server->NoviceCatalogProbe.run(server.overworld()));}}
}
