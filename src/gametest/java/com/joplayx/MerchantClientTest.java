package com.joplayx;

import com.joplayx.client.VillagerRerollClient;
import com.joplayx.client.config.*;
import com.joplayx.client.util.*;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.world.entity.npc.villager.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.*;
import net.minecraft.world.item.trading.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.EntityHitResult;

public class MerchantClientTest implements FabricClientGameTest {
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    private static void click(ClientGameTestContext ctx,String key){
        double[] xy=ctx.computeOnClient(mc->{
            String text=net.minecraft.network.chat.Component.translatable(key).getString();
            var b=(Button)mc.screen.children().stream().filter(c->c instanceof Button button&&button.visible&&button.getMessage().getString().equals(text)).findFirst().orElseThrow();
            return new double[]{(b.getX()+b.getWidth()/2.0)*mc.getWindow().getScreenWidth()/mc.screen.width,(b.getY()+b.getHeight()/2.0)*mc.getWindow().getScreenHeight()/mc.screen.height};
        });
        ctx.getInput().setCursorPos(xy[0],xy[1]);ctx.waitTick();
        ctx.getInput().pressMouse(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT);ctx.waitTicks(2);
    }
    @Override public void runTest(ClientGameTestContext context){
        context.runOnClient(mc->{mc.options.guiScale().set(2);mc.resizeGui();RerollerConfig.get().targets.clear();QuickSetup.clear();});
        context.getInput().resizeWindow(640,480);context.waitTick();
        try(var world=context.worldBuilder().adjustSettings(s->s.setGameMode(net.minecraft.client.gui.screens.worldselection.WorldCreationUiState.SelectedGameMode.CREATIVE)).create()){
            var pos=context.computeOnClient(mc->mc.player.blockPosition().east());
            var serverVillager=world.getServer().computeOnServer(server->{
                var level=server.overworld();level.setBlockAndUpdate(pos,Blocks.LECTERN.defaultBlockState());
                var v=new Villager(net.minecraft.world.entity.EntityType.VILLAGER,level);v.snapTo(server.getPlayerList().getPlayers().getFirst().position().add(2,0,0));v.setNoAi(true);
                v.setVillagerData(v.getVillagerData().withProfession(level.registryAccess(),VillagerProfession.LIBRARIAN));
                var offers=new MerchantOffers();offers.add(new MerchantOffer(new ItemCost(Items.EMERALD,5),new ItemStack(Items.GLASS,4),12,1,0));
                v.setOffers(offers);v.setVillagerXp(0);level.addFreshEntity(v);return v;
            });
            context.waitFor(mc->mc.level.getEntity(serverVillager.getId())!=null);
            context.waitFor(mc->mc.level.getBlockState(pos).is(Blocks.LECTERN));
            context.runOnClient(mc->MerchantSearch.rememberPlacement(mc,pos,Workstation.LECTERN));
            context.waitFor(mc->QuickSetup.selected(mc)!=null,100);
            context.runOnClient(mc->check(RerollerConfig.get().lecternPos().equals(pos),"placement pairs the only villager and workstation"));
            world.getServer().runOnServer(server->server.overworld().setBlockAndUpdate(pos.north(),Blocks.LECTERN.defaultBlockState()));
            context.waitFor(mc->mc.level.getBlockState(pos.north()).is(Blocks.LECTERN));
            context.runOnClient(mc->{var v=(Villager)mc.level.getEntity(serverVillager.getId());net.fabricmc.fabric.api.event.player.UseEntityCallback.EVENT.invoker().interact(mc.player,mc.level,InteractionHand.MAIN_HAND,v,new EntityHitResult(v));mc.gameMode.interact(mc.player,v,new EntityHitResult(v),InteractionHand.MAIN_HAND);});
            context.waitForScreen(MerchantScreen.class);
            int container=context.computeOnClient(mc->mc.player.containerMenu.containerId);
            click(context,"villager-reroll.merchant.open");
            context.runOnClient(mc->{
                check(mc.screen instanceof MerchantScreen && MerchantSearch.panel().open(),"search opens inside actual merchant screen");
                var input=(EditBox)mc.screen.getFocused();input.setValue("");
                for(char ch:"mending".toCharArray())mc.screen.charTyped(new CharacterEvent(ch));
            });
            context.waitTicks(2);context.takeScreenshot("merchant-book-search");
            click(context,"Mending");
            context.runOnClient(mc->{var fields=MerchantSearch.panel().children().stream().filter(c->c instanceof EditBox).map(c->(EditBox)c).toList();fields.get(0).setValue("2");fields.get(1).setValue("12");});
            click(context,"villager-reroll.merchant.add");
            context.runOnClient(mc->check(RerollerConfig.get().targets.isEmpty(),"impossible book levels are rejected"));
            context.runOnClient(mc->((EditBox)MerchantSearch.panel().children().stream().filter(c->c instanceof EditBox).findFirst().orElseThrow()).setValue("1"));
            click(context,"villager-reroll.merchant.start");
            context.runOnClient(mc->check(!VillagerRerollClient.CONTROLLER.running()&&RerollerConfig.get().targets.isEmpty(),"ambiguous workstation requires selection before start"));
            click(context,"villager-reroll.merchant.station");
            click(context,"villager-reroll.merchant.add");
            context.runOnClient(mc->{
                var t=RerollerConfig.get().targets.getLast();check(t.enchantmentId.equals("minecraft:mending")&&t.maxEmeraldCost==12&&t.workstation.equals("lectern"),"name search saves book criteria scoped to librarian");
                check(mc.player.containerMenu.containerId==container,"saving leaves the original merchant container open");
                var copy=WishlistStore.parse(WishlistStore.export(WishlistStore.capture("Scoped","lectern",java.util.List.of(t))));
                check(copy.targets.getFirst().workstation.equals("lectern"),"wishlist round trip preserves profession scope");
                var cfg=new RerollerConfig.Config();cfg.targets.add(t);cfg.workstation="smithing_table";
                check(TradeUtil.checkTrades(cfg,((MerchantScreen)mc.screen).getMenu().getOffers()).statuses().isEmpty(),"other professions ignore scoped targets");
            });
            context.getInput().resizeWindow(1000,600);context.waitTicks(3);
            context.runOnClient(mc->check(MerchantSearch.panel().open(),"resize retains search panel"));
            click(context,"villager-reroll.merchant.back");
            context.runOnClient(mc->{check(!MerchantSearch.panel().open()&&mc.player.containerMenu.containerId==container,"back restores normal live trades");mc.player.closeContainer();});
            world.getServer().runOnServer(server->{
                var level=server.overworld();level.setBlockAndUpdate(pos,Blocks.SMITHING_TABLE.defaultBlockState());level.setBlockAndUpdate(pos.north(),Blocks.AIR.defaultBlockState());
                serverVillager.setVillagerData(serverVillager.getVillagerData().withProfession(level.registryAccess(),VillagerProfession.TOOLSMITH));
                var toolOffers=new MerchantOffers();toolOffers.add(new MerchantOffer(new ItemCost(Items.EMERALD,1),new ItemStack(Items.STONE_AXE),12,1,0));serverVillager.setOffers(toolOffers);
            });
            context.waitFor(mc->QuickSetup.profession((Villager)mc.level.getEntity(serverVillager.getId())).equals("toolsmith"));
            context.runOnClient(mc->{var v=(Villager)mc.level.getEntity(serverVillager.getId());net.fabricmc.fabric.api.event.player.UseEntityCallback.EVENT.invoker().interact(mc.player,mc.level,InteractionHand.MAIN_HAND,v,new EntityHitResult(v));mc.gameMode.interact(mc.player,v,new EntityHitResult(v),InteractionHand.MAIN_HAND);});
            context.waitForScreen(MerchantScreen.class);click(context,"villager-reroll.merchant.open");
            context.runOnClient(mc->((EditBox)mc.screen.getFocused()).setValue("axe"));context.waitTicks(2);
            context.runOnClient(mc->{
                var names=MerchantSearch.panel().children().stream().filter(c->c instanceof Button).map(c->((Button)c).getMessage().getString()).toList();
                check(names.stream().anyMatch(n->n.contains("Stone Axe")),"toolsmith suggests its novice tools");
                check(names.stream().noneMatch(n->n.contains("Mending")||n.contains("Diamond")),"toolsmith does not suggest books or locked-tier diamond gear");
            });
            context.takeScreenshot("merchant-toolsmith-search");
            String axe=context.computeOnClient(mc->MerchantSearch.panel().children().stream().filter(c->c instanceof Button b&&b.getMessage().getString().contains("Stone Axe")).map(c->((Button)c).getMessage().getString()).findFirst().orElseThrow());
            click(context,axe);click(context,"villager-reroll.merchant.start");
            context.waitFor(mc->VillagerRerollClient.CONTROLLER.getState()==com.joplayx.client.state.RerollState.FOUND,400);
            context.runOnClient(mc->{
                var target=RerollerConfig.get().targets.getLast();
                check(target.itemId.equals("minecraft:stone_axe")&&target.status==SearchTarget.Status.FOUND,"Save and start finds the selected tool in actual offers");
                check(RerollerConfig.get().targets.getFirst().status==SearchTarget.Status.ACTIVE,"tool search preserves pending librarian target");
                target.status=SearchTarget.Status.ACQUIRED;VillagerRerollClient.CONTROLLER.emergencyStop(mc);mc.player.closeContainer();
            });
            world.getServer().runOnServer(server->serverVillager.setVillagerXp(1));
            context.runOnClient(mc->{var v=(Villager)mc.level.getEntity(serverVillager.getId());net.fabricmc.fabric.api.event.player.UseEntityCallback.EVENT.invoker().interact(mc.player,mc.level,InteractionHand.MAIN_HAND,v,new EntityHitResult(v));mc.gameMode.interact(mc.player,v,new EntityHitResult(v),InteractionHand.MAIN_HAND);});
            context.waitForScreen(MerchantScreen.class);click(context,"villager-reroll.merchant.open");
            context.runOnClient(mc->{
                var choice=(Button)MerchantSearch.panel().children().stream().filter(c->c instanceof Button b&&b.getMessage().getString().contains("Stone Axe")).findFirst().orElseThrow();choice.onPress(new net.minecraft.client.input.InputWithModifiers() { public int input(){return 0;} public int modifiers(){return 0;} });
            });
            context.waitTicks(2);
            context.runOnClient(mc->{
                var start=(Button)MerchantSearch.panel().children().stream().filter(c->c instanceof Button b&&b.getMessage().getString().equals(Text.str("merchant.start"))).findFirst().orElseThrow();
                check(!start.active,"locked villager cannot start rerolling");mc.player.closeContainer();RerollerConfig.get().targets.clear();QuickSetup.clear();
            });
        }
    }
}
