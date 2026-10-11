package com.joplayx;
import com.joplayx.client.config.*;
import com.joplayx.client.util.*;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.TitleScreen;
public class ConvenienceClientTest implements FabricClientGameTest {
    private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    private static EditBox firstBox(net.minecraft.client.Minecraft mc){return (EditBox)mc.screen.children().stream().filter(x->x instanceof EditBox).findFirst().orElseThrow();}
    @Override public void runTest(ClientGameTestContext context){
        context.runOnClient(mc->{mc.options.guiScale().set(2);mc.resizeGui();RerollerConfig.get().targets.clear();RerollerConfig.get().wishlists.clear();RerollerConfig.get().workstation="composter";});
        context.getInput().resizeWindow(640,480);context.waitTicks(2);
        context.setScreen(()->new TradePickerScreen(new TitleScreen(),false));context.waitTicks(2);
        context.setScreen(TitleScreen::new);
        try(var world=context.worldBuilder().create()) {
        context.setScreen(()->new TradePickerScreen(new TitleScreen(),false));context.waitTicks(2);
        context.runOnClient(mc->{
            check(TradeCatalog.forStation(Workstation.COMPOSTER).stream().anyMatch(e->e.payment().equals("minecraft:carrot")),"farmer carrot trade exists");
            check(TradeCatalog.forStation(Workstation.COMPOSTER).stream().noneMatch(e->e.item().equals("minecraft:diamond_pickaxe")),"novice picker excludes impossible diamond pickaxe");
            check(!SearchNames.item("minecraft:carrot").isEmpty(),"item icons available in world");
            firstBox(mc).setValue("carrot");
        });context.waitTicks(2);context.takeScreenshot("convenience-novice-picker");
        String carrotLabel=context.computeOnClient(mc->mc.screen.children().stream().filter(x->x instanceof Button b&&b.getMessage().getString().contains("Carrot"))
                .map(x->((Button)x).getMessage().getString()).findFirst().orElseThrow());
        context.clickScreenButton(carrotLabel);context.waitTicks(2);
        context.waitForScreen(TargetEditScreen.class);
        context.runOnClient(mc->check(RerollerConfig.get().targets.isEmpty(),"picker draft not saved early"));
        context.clickScreenButton("villager-reroll.button.cancel");context.waitTicks(2);
        context.runOnClient(mc->check(RerollerConfig.get().targets.isEmpty(),"cancel does not add a target"));
        context.runOnClient(mc->RerollerConfig.get().workstation="lectern");
        context.setScreen(()->new TargetEditScreen(new TitleScreen(),new RerollerConfig.EnchantTarget("minecraft:mending",1,15)));
        context.runOnClient(mc->firstBox(mc).setValue("Mending"));
        context.clickScreenButton("villager-reroll.button.done");context.waitTicks(2);
        context.runOnClient(mc->check(RerollerConfig.get().targets.getFirst().enchantmentId.equals("minecraft:mending"),"normal enchantment name resolves"));
        context.setScreen(()->new WishlistsScreen(new TitleScreen()));
        context.runOnClient(mc->firstBox(mc).setValue("My test wishlist"));
        context.clickScreenButton("villager-reroll.wishlists.save");context.waitTicks(2);
        context.runOnClient(mc->{check(RerollerConfig.get().wishlists.size()==1,"wishlist saved");RerollerConfig.save();RerollerConfig.load();check(RerollerConfig.get().wishlists.size()==1,"wishlist survives reload");});
        context.takeScreenshot("convenience-wishlists");
        context.clickScreenButton("My test wishlist");context.waitTicks(2);
        context.clickScreenButton("villager-reroll.wishlists.duplicate");context.waitTicks(2);
        context.runOnClient(mc->check(RerollerConfig.get().wishlists.size()==2,"duplicate creates independent saved list"));
        context.clickScreenButton("villager-reroll.wishlists.add");context.waitTicks(2);
        context.runOnClient(mc->{
            check(RerollerConfig.get().targets.size()==2,"adding wishlist preserves existing target");
            RerollerConfig.get().targets.removeLast();
            RerollerConfig.get().foundSoundEnabled=false;RerollerConfig.get().foundToastEnabled=false;RerollerConfig.save();RerollerConfig.load();
            check(!RerollerConfig.get().foundSoundEnabled&&!RerollerConfig.get().foundToastEnabled,"notification preferences persist");
            RerollerConfig.get().foundSoundEnabled=true;RerollerConfig.get().foundToastEnabled=true;
        });
        context.runOnClient(mc->{var t=RerollerConfig.get().targets.getFirst();t.record(15,1,1,1,"first");t.record(12,1,1,1,"second");});
        context.setScreen(()->new FoundTradeScreen(new TitleScreen()));context.waitTick();context.takeScreenshot("convenience-found-review");
        context.clickScreenButton("villager-reroll.results.keep");context.waitTicks(2);
        context.runOnClient(mc->check(RerollerConfig.get().targets.getFirst().status==SearchTarget.Status.ACQUIRED,"keep result completes search"));
        context.clickScreenButton("villager-reroll.search.continue");context.waitTicks(2);
        context.runOnClient(mc->{check(RerollerConfig.get().targets.getFirst().active(),"search better reactivates");RerollerConfig.get().hudDetails=true;});
        context.setScreen(()->new HudPositionScreen(new TitleScreen()));context.waitTick();context.takeScreenshot("convenience-detailed-hud");
        context.runOnClient(mc->check(com.joplayx.client.hud.RerollerHud.panelHeight()==70,"optional detail fits expanded panel"));
        context.runOnClient(mc->{RerollerConfig.get().hudDetails=false;RerollerConfig.get().workstation="lectern";RerollerConfig.get().targets.clear();});
        context.setScreen(()->null);
        }
        context.setScreen(TitleScreen::new);
    }
}
