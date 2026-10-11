package com.joplayx;
import com.joplayx.client.config.*;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.gui.screens.TitleScreen;
import java.util.*;
public class RerollerClientTest implements FabricClientGameTest {
    private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    private static void clickWithMouse(ClientGameTestContext context, String translationKey) {
        double[] point = context.computeOnClient(mc -> {
            var screen = mc.screen;
            String label = net.minecraft.network.chat.Component.translatable(translationKey).getString();
            var button = (Button) screen.children().stream().filter(child -> child instanceof Button b
                    && b.getMessage().getString().equals(label)).findFirst().orElseThrow();
            return new double[] {
                (button.getX() + button.getWidth() / 2.0) * mc.getWindow().getScreenWidth() / screen.width,
                (button.getY() + button.getHeight() / 2.0) * mc.getWindow().getScreenHeight() / screen.height
            };
        });
        context.getInput().setCursorPos(point[0], point[1]);
        context.waitTick();
        context.getInput().pressMouse(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT);
        context.waitTick();
    }
    @Override public void runTest(ClientGameTestContext context) {
        context.runOnClient(mc->{
            mc.options.guiScale().set(2);mc.resizeGui();
            var cfg=RerollerConfig.get();cfg.targets.clear();
            for(int i=0;i<15;i++)cfg.targets.add(new RerollerConfig.EnchantTarget("minecraft:thorns",3,21));
        });
        context.getInput().resizeWindow(640,480);context.waitTick();
        context.setScreen(()->new EnchantTargetListScreen(new TitleScreen()));
        context.waitTicks(3);
        context.runOnClient(mc->{
            var screen=mc.screen;
            var list=(AbstractSelectionList<?>)screen.children().stream().filter(x->x instanceof AbstractSelectionList).findFirst().orElseThrow();
            check(list.children().size()==15,"all 15 searches present");
            check(list.maxScrollAmount()>0,"searches scroll");
            list.setScrollAmount(list.maxScrollAmount());
            check(list.getBottom()<=screen.height-70,"list never overlaps footer");
        });
        context.waitTick();context.takeScreenshot("search-list-small-scrolled");
        context.setScreen(()->new TargetEditScreen(new EnchantTargetListScreen(null),RerollerConfig.get().targets.get(0)));
        context.runOnClient(mc->{
            var s=mc.screen;var box=(EditBox)s.children().stream().filter(x->x instanceof EditBox).findFirst().orElseThrow();
            box.setValue("");s.setFocused(box);
            int count=s.children().size();
            for(char c:"minecraft:".toCharArray()) s.charTyped(new CharacterEvent(c));
            check(box.getValue().equals("minecraft:"),"partial namespace survives typing");
            check(s.children().size()==count,"typing must not mutate widgets");
            for(char c:"th".toCharArray())s.charTyped(new CharacterEvent(c));
            check(s.children().size()==count,"autocomplete must not mutate widgets");
        });
        context.waitTicks(2);context.takeScreenshot("enchantment-autocomplete");
        context.getInput().resizeWindow(1000,600);context.waitTicks(2);
        context.runOnClient(mc->{var box=(EditBox)mc.screen.children().stream().filter(x->x instanceof EditBox).findFirst().orElseThrow();check(box.getValue().equals("minecraft:th"),"resize preserves draft");box.setValue("minecraft:thorns");});
        context.clickScreenButton("villager-reroll.button.done");context.waitTick();
        context.waitForScreen(EnchantTargetListScreen.class);
        context.runOnClient(mc->{var t=RerollerConfig.get().targets.get(0);t.record(21,1,3,1,"thorns III");});
        context.setScreen(()->new TargetEditScreen(new EnchantTargetListScreen(null),RerollerConfig.get().targets.get(0)));
        context.clickScreenButton("villager-reroll.search.continue");context.waitTick();
        context.runOnClient(mc->{var t=RerollerConfig.get().targets.get(0);check(t.active()&&t.bestPrice==21,"continue keeps saved offer");});
        context.setScreen(()->new TargetEditScreen(new EnchantTargetListScreen(null),RerollerConfig.get().targets.get(0)));
        context.clickScreenButton("villager-reroll.search.acquired");context.waitTick();
        context.runOnClient(mc->{check(RerollerConfig.get().targets.get(0).status==SearchTarget.Status.ACQUIRED,"acquire completes search");RerollerConfig.load();check(RerollerConfig.get().targets.get(0).status==SearchTarget.Status.ACQUIRED,"completed status persists");});
        context.runOnClient(mc->{var t=new RerollerConfig.EnchantTarget();t.kind=SearchTarget.Kind.ITEM;t.itemId="minecraft:emerald";t.paymentItemId="minecraft:wheat";RerollerConfig.get().targets.add(t);});
        context.getInput().resizeWindow(640,480);context.waitTick();
        context.setScreen(()->new TargetEditScreen(new EnchantTargetListScreen(null),RerollerConfig.get().targets.getLast()));
        context.waitTicks(2);context.takeScreenshot("item-search-small");
        context.clickScreenButton("villager-reroll.button.done");context.waitTick();
        context.waitForScreen(EnchantTargetListScreen.class);
        context.clickScreenButton("villager-reroll.button.more_settings_short");context.waitTicks(2);
        context.takeScreenshot("settings");
        context.runOnClient(mc -> {
            RerollerConfig.get().hudX = 0; RerollerConfig.get().hudY = 0;
        });
        context.setScreen(() -> new EnchantTargetListScreen(new TitleScreen()));
        clickWithMouse(context, "villager-reroll.hud.move");
        context.waitForScreen(HudPositionScreen.class);
        // Exercise Minecraft's input path using this version's actual left mouse button.
        context.getInput().setCursorPos(40, 40);
        context.waitTicks(2);
        context.getInput().holdMouse(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT);
        context.waitTick();
        context.getInput().setCursorPos(5000, 5000);
        context.waitTicks(2);
        context.getInput().releaseMouse(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT);
        context.waitTick();
        context.getInput().setCursorPos(40, 40);
        context.waitTicks(2); // Moving after release must not keep dragging the panel.
        context.runOnClient(mc -> check(RerollerConfig.get().hudX == 0, "drag stays a draft before Save"));
        clickWithMouse(context, "villager-reroll.button.done");
        context.runOnClient(mc -> {
            RerollerConfig.load();
            check(RerollerConfig.get().hudX == 1 && RerollerConfig.get().hudY == 1, "HUD clamps and persists at screen edge");
        });
        context.getInput().resizeWindow(1000, 600); context.waitTicks(2);
        context.setScreen(() -> new EnchantTargetListScreen(new TitleScreen()));
        clickWithMouse(context, "villager-reroll.hud.move");
        context.waitForScreen(HudPositionScreen.class);
        context.waitTick(); context.takeScreenshot("movable-hud-bottom-right");
        clickWithMouse(context, "villager-reroll.hud.reset_position");
        clickWithMouse(context, "villager-reroll.button.cancel");
        context.runOnClient(mc -> check(RerollerConfig.get().hudX == 1, "cancel discards reset"));
        context.setScreen(() -> new EnchantTargetListScreen(new TitleScreen()));
        clickWithMouse(context, "villager-reroll.hud.move");
        context.waitForScreen(HudPositionScreen.class);
        clickWithMouse(context, "villager-reroll.hud.reset_position");
        clickWithMouse(context, "villager-reroll.button.done");
        context.runOnClient(mc -> check(RerollerConfig.get().hudX == 0 && RerollerConfig.get().hudY == 0, "reset saves default"));
        context.setScreen(TitleScreen::new);
        try(var world=context.worldBuilder().adjustSettings(c->c.setGameMode(net.minecraft.client.gui.screens.worldselection.WorldCreationUiState.SelectedGameMode.CREATIVE)).create()) {
            var pos=context.computeOnClient(mc->mc.player.blockPosition().east());
            var villager=world.getServer().computeOnServer(server->{
                var level=server.overworld();
                level.setBlockAndUpdate(pos,net.minecraft.world.level.block.Blocks.LECTERN.defaultBlockState());
                var v=new net.minecraft.world.entity.npc.villager.Villager(net.minecraft.world.entity.EntityType.VILLAGER,level);
                v.snapTo(server.getPlayerList().getPlayers().getFirst().position().add(2,0,0));
                v.setNoAi(true);
                v.setVillagerData(v.getVillagerData().withProfession(level.registryAccess(),net.minecraft.world.entity.npc.villager.VillagerProfession.LIBRARIAN));
                var offers=new net.minecraft.world.item.trading.MerchantOffers();
                offers.add(new net.minecraft.world.item.trading.MerchantOffer(new net.minecraft.world.item.trading.ItemCost(net.minecraft.world.item.Items.EMERALD,5),new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.GLASS,4),12,1,0));
                v.setOffers(offers);v.setVillagerXp(0);level.addFreshEntity(v);return v;
            });
            context.waitFor(mc -> mc.level.getEntity(villager.getId()) != null);
            context.waitFor(mc->mc.level.getBlockState(pos).is(net.minecraft.world.level.block.Blocks.LECTERN));
            context.runOnClient(mc->{
                var v=(net.minecraft.world.entity.npc.villager.Villager)mc.level.getEntity(villager.getId());
                var candidates=com.joplayx.client.util.QuickSetup.candidates(mc,v);
                var candidate=candidates.stream().filter(c->c.pos().equals(pos)).findFirst().orElseThrow();
                check(candidate.station()==com.joplayx.client.util.Workstation.LECTERN,"setup detects profession and workstation");
                check(com.joplayx.client.util.QuickSetup.confirm(mc,v,candidate),"setup accepts valid pair");
                check(com.joplayx.client.util.QuickSetup.selected(mc)==v,"setup pins selected villager");
                check(!com.joplayx.client.util.QuickSetup.confirm(mc,v,new com.joplayx.client.util.QuickSetup.Candidate(pos,com.joplayx.client.util.Workstation.COMPOSTER)),"setup rejects changed workstation");
            });
            context.setScreen(()->{
                var mc=net.minecraft.client.Minecraft.getInstance();
                var selected=(net.minecraft.world.entity.npc.villager.Villager)mc.level.getEntity(villager.getId());
                return new SetupScreen(null,selected,com.joplayx.client.util.QuickSetup.candidates(mc,selected));
            });
            context.waitTicks(2);
            context.clickScreenButton("villager-reroll.setup.preview");context.waitTicks(2);
            context.runOnClient(mc->{mc.player.setYRot(-90);mc.player.setXRot(10);});
            context.waitTicks(6);context.takeScreenshot("convenience-setup-preview");
            context.clickScreenButton("villager-reroll.setup.confirm");context.waitTicks(2);
            context.waitForScreen(EnchantTargetListScreen.class);
            context.setScreen(()->null);
            context.runOnClient(mc->{
                var cfg=RerollerConfig.get();cfg.targets.clear();cfg.workstation="lectern";cfg.setLecternPos(pos);
                var t=new RerollerConfig.EnchantTarget();t.kind=SearchTarget.Kind.ITEM;t.itemId="minecraft:glass";t.minQuantity=4;t.maxEmeraldCost=5;cfg.targets.add(t);
                com.joplayx.client.VillagerRerollClient.CONTROLLER.newSession(mc);
            });
            context.waitFor(mc->com.joplayx.client.VillagerRerollClient.CONTROLLER.getState()==com.joplayx.client.state.RerollState.FOUND,400);
            context.runOnClient(mc->{
                var c=com.joplayx.client.VillagerRerollClient.CONTROLLER;
                check(c.getAttempts()==1,"actual merchant packet evaluated once");
                check(RerollerConfig.get().targets.getFirst().bestPrice==5,"actual offer price saved");
            });
            context.waitTicks(60);
            context.runOnClient(mc->{
                check(mc.level.getBlockState(pos).is(net.minecraft.world.level.block.Blocks.LECTERN),"FOUND must not destroy workstation");
                check(com.joplayx.client.VillagerRerollClient.CONTROLLER.getAttempts()==1,"FOUND must not reread trades");
                mc.player.closeContainer();
                RerollerConfig.get().targets.getFirst().status=SearchTarget.Status.ACTIVE;
                com.joplayx.client.VillagerRerollClient.CONTROLLER.resume(mc);
            });
            context.waitFor(mc->com.joplayx.client.VillagerRerollClient.CONTROLLER.getAttempts()==2,400);
            context.runOnClient(mc->{
                var c=com.joplayx.client.VillagerRerollClient.CONTROLLER;
                check(c.getState()!=com.joplayx.client.state.RerollState.FOUND,"same price must not match on resume");
                c.emergencyStop(mc);check(c.getAttempts()==2,"pause preserves attempts");
            });
            long seconds=context.computeOnClient(mc->com.joplayx.client.VillagerRerollClient.CONTROLLER.getElapsedSeconds());
            context.waitTicks(30);
            context.runOnClient(mc->{check(com.joplayx.client.VillagerRerollClient.CONTROLLER.getElapsedSeconds()==seconds,"paused time excluded");});
            world.getServer().runOnServer(server->villager.setVillagerXp(1));
            context.runOnClient(mc->com.joplayx.client.VillagerRerollClient.CONTROLLER.resume(mc));
            context.waitFor(mc->com.joplayx.client.VillagerRerollClient.CONTROLLER.getState()==com.joplayx.client.state.RerollState.PAUSED,400);
            context.runOnClient(mc->{
                var c=com.joplayx.client.VillagerRerollClient.CONTROLLER;
                check(c.getAttempts()==3,"retry preserves attempts");
                check(mc.level.getBlockState(pos).is(net.minecraft.world.level.block.Blocks.LECTERN),"locked trade never broken");
                mc.player.closeContainer();

            });
            world.getServer().runOnServer(server->{
                villager.setVillagerXp(0);
                server.overworld().removeBlock(pos,false);
                server.getPlayerList().getPlayers().getFirst().getInventory().clearContent();
            });
            context.waitFor(mc->mc.level.getBlockState(pos).isAir());
            context.runOnClient(mc->com.joplayx.client.VillagerRerollClient.CONTROLLER.resume(mc));
            context.waitFor(mc->com.joplayx.client.VillagerRerollClient.CONTROLLER.getState()==com.joplayx.client.state.RerollState.PAUSED,200);
            context.runOnClient(mc->check(com.joplayx.client.VillagerRerollClient.CONTROLLER.getAttempts()==3,"missing resource pauses without resetting"));
            world.getServer().runCommand("give @p minecraft:lectern 1");
            context.waitFor(mc->com.joplayx.client.util.HotbarUtil.hasItemInHotbar(mc.player,net.minecraft.world.item.Items.LECTERN));
            context.runOnClient(mc->com.joplayx.client.VillagerRerollClient.CONTROLLER.resume(mc));
            context.waitFor(mc->com.joplayx.client.VillagerRerollClient.CONTROLLER.getAttempts()==4,400);
            context.runOnClient(mc->{
                var c=com.joplayx.client.VillagerRerollClient.CONTROLLER;c.emergencyStop(mc);
                check(mc.level.getBlockState(pos).is(net.minecraft.world.level.block.Blocks.LECTERN),"refill and resume places workstation");
                mc.player.closeContainer();
            });
            world.getServer().runOnServer(server->{
                var level=server.overworld();
                var book=new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.ENCHANTED_BOOK);
                book.enchant(level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).getOrThrow(net.minecraft.world.item.enchantment.Enchantments.THORNS),3);
                var offers=new net.minecraft.world.item.trading.MerchantOffers();
                offers.add(new net.minecraft.world.item.trading.MerchantOffer(new net.minecraft.world.item.trading.ItemCost(net.minecraft.world.item.Items.EMERALD,11),java.util.Optional.of(new net.minecraft.world.item.trading.ItemCost(net.minecraft.world.item.Items.BOOK)),book,12,1,0));
                villager.setOffers(offers);
            });
            context.runOnClient(mc->{
                RerollerConfig.get().targets.clear();RerollerConfig.get().targets.add(new RerollerConfig.EnchantTarget("minecraft:thorns",3,21));
                com.joplayx.client.VillagerRerollClient.CONTROLLER.resume(mc);
            });
            context.waitFor(mc->com.joplayx.client.VillagerRerollClient.CONTROLLER.getState()==com.joplayx.client.state.RerollState.FOUND,400);
            context.runOnClient(mc->{
                var t=RerollerConfig.get().targets.getFirst();check(t.bestPrice==11&&t.bestLevel==3,"book result persisted");
                check(t.vanillaBaseMinimum==11,"vanilla minimum reference");mc.player.closeContainer();
            });
            context.waitTicks(2);context.takeScreenshot("compact-found-hud");
            context.setScreen(()->new TargetEditScreen(new EnchantTargetListScreen(null),RerollerConfig.get().targets.getFirst()));
            context.waitTick();context.takeScreenshot("minimum-book-offer");
            context.setScreen(()->null);
            context.runOnClient(mc->{
                var c=com.joplayx.client.VillagerRerollClient.CONTROLLER;c.newSession(mc);
                check(c.getAttempts()==0,"explicit new session resets attempts");c.emergencyStop(mc);
            });
        }
    }
}
