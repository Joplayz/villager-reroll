package com.joplayx;
import com.joplayx.client.VillagerRerollClient;
import com.joplayx.client.config.*;
import com.joplayx.client.hud.RerollerHud;
import com.joplayx.client.state.RerollState;
import com.joplayx.client.util.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.*;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.*;
import net.minecraft.world.item.trading.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.levelgen.*;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.block.Blocks;
import java.util.concurrent.*;
import java.util.function.*;
import java.nio.file.*;
/** Packaged-client integration tests for releases predating Fabric's client gametest API. */
public final class LegacyClientTest implements ClientModInitializer {
    private static final Minecraft MC=Minecraft.getInstance();
    private static final long STARTED_AT=System.nanoTime();
    private static volatile long ticks;private boolean started;
    private static void check(boolean value,String why){if(!value)throw new AssertionError(why);}
    private static <T>T client(Supplier<T> action){try{return MC.submit(action).get(180,TimeUnit.SECONDS);}catch(Exception e){throw new RuntimeException(e);}}
    private static void run(Runnable action){client(()->{action.run();return null;});}
    private static void until(BooleanSupplier condition){long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(180);while(!client(condition::getAsBoolean)){if(System.nanoTime()>end)throw new AssertionError("Timed out: "+client(()->VillagerRerollClient.CONTROLLER.getStatusMessage()));try{Thread.sleep(25);}catch(InterruptedException e){throw new RuntimeException(e);}}}
    private static void waitTicks(int n){long target=ticks+n;until(()->ticks>=target);}
    private static void screen(Supplier<Screen> factory){run(()->MC.setScreen(factory.get()));waitTicks(2);}
    private static void click(String key){run(()->{String label=net.minecraft.network.chat.Component.translatable(key).getString();var b=(Button)MC.screen.children().stream().filter(c->c instanceof Button button&&button.visible&&button.active&&button.getMessage().getString().equals(label)).findFirst().orElseThrow();check(b.getY()+b.getHeight()<=MC.screen.height,"Button within window: "+label);MC.screen.mouseClicked(b.getX()+b.getWidth()/2.0,b.getY()+10,0);MC.screen.mouseReleased(b.getX()+b.getWidth()/2.0,b.getY()+10,0);});waitTicks(2);}
    private static void shot(String name){run(()->net.minecraft.client.Screenshot.grab(MC.gameDirectory,name+".png",MC.getMainRenderTarget(),message->{}));}
    @Override public void onInitializeClient(){
        ClientTickEvents.END_CLIENT_TICK.register(mc->{ticks++;if(!started&&System.nanoTime()-STARTED_AT>TimeUnit.SECONDS.toNanos(180))throw new AssertionError("Test startup timed out");if(!started&&mc.screen!=null&&mc.getOverlay()==null){started=true;mc.setScreen(new TitleScreen());new Thread(()->{
            try{test();Files.writeString(Path.of("legacy-client-test-passed.txt"),"Passed UI, HUD, merchant integration, search results, resume, and catalog checks.");VillagerReroll.LOGGER.info("[Legacy client test] ALL CHECKS PASSED");run(MC::stop);}
            catch(Throwable e){VillagerReroll.LOGGER.error("[Legacy client test] FAILED",e);System.exit(1);}
        },"Reroller integration tests").start();}});
    }
    private static void test() throws Exception {
        var loader=FabricLoader.getInstance();String game=loader.getModContainer("minecraft").orElseThrow().getMetadata().getVersion().getFriendlyString();
        check(loader.getModContainer("villager-reroll").orElseThrow().getMetadata().getVersion().getFriendlyString().endsWith("+mc"+game),"Correct packaged implementation");
        if(Boolean.getBoolean("villager-reroll.testUniversal"))check(loader.getModContainer("villager-reroll").orElseThrow().getContainingMod().orElseThrow().getMetadata().getId().equals("villager-reroll-bundle"),"Nested universal implementation");
        run(()->{MC.options.guiScale().set(2);MC.resizeDisplay();var cfg=RerollerConfig.get();cfg.targets.clear();cfg.workstation="lectern";cfg.hudDetails=true;cfg.hudX=0;cfg.hudY=0;cfg.hudScale=cfg.hudScaleX=cfg.hudScaleY=1;cfg.targets.add(new RerollerConfig.EnchantTarget("minecraft:thorns",3,21));cfg.targets.get(0).record(15,1,3,1,"Thorns III");});
        screen(()->new HudPositionScreen(new TitleScreen()));
        click("villager-reroll.hud.axis_all");for(int i=0;i<4;i++)click("villager-reroll.hud.larger");click("villager-reroll.hud.axis_width");for(int i=0;i<4;i++)click("villager-reroll.hud.smaller");
        run(()->check(RerollerConfig.get().hudScaleX==1,"HUD edits are drafts"));shot("legacy-compact-detailed-hud");click("villager-reroll.button.done");
        run(()->{RerollerConfig.load();check(RerollerConfig.get().hudScaleX==1.2&&RerollerConfig.get().hudScaleY==0.8,"Independent axis settings saved");check(RerollerHud.panelHeight()<100,"Detailed HUD is compact");});
        screen(()->new HudPositionScreen(new TitleScreen()));click("villager-reroll.hud.smaller");click("villager-reroll.button.cancel");run(()->check(RerollerConfig.get().hudScale==1,"Cancel leaves saved size unchanged"));
        run(()->{var cfg=RerollerConfig.get();cfg.targets.clear();for(int i=0;i<16;i++)cfg.targets.add(new RerollerConfig.EnchantTarget("minecraft:thorns",3,i+1));});
        screen(()->new EnchantTargetListScreen(new TitleScreen()));
        run(()->{var list=(ContainerObjectSelectionList<?>)MC.screen.children().stream().filter(c->c instanceof ContainerObjectSelectionList).findFirst().orElseThrow();list.setScrollAmount(100000);});waitTicks(3);shot("legacy-scroll-sixteen-targets");
        run(()->{var list=(ContainerObjectSelectionList<?>)MC.screen.children().stream().filter(c->c instanceof ContainerObjectSelectionList).findFirst().orElseThrow();var entry=list.children().get(15);var button=(Button)entry.children().get(0);check(button.getY()+20<MC.screen.height-92,"Last row above footer");MC.screen.mouseClicked(button.getX()+10,button.getY()+10,0);MC.screen.mouseReleased(button.getX()+10,button.getY()+10,0);});waitTicks(3);run(()->check(MC.screen instanceof TargetEditScreen,"Sixteenth target editable"));
        screen(TitleScreen::new);
        run(()->MC.createWorldOpenFlows().createFreshLevel("reroll-integration-"+System.currentTimeMillis(),new LevelSettings("Reroller integration",GameType.CREATIVE,false,net.minecraft.world.Difficulty.PEACEFUL,true,new GameRules(),WorldDataConfiguration.DEFAULT),new WorldOptions(1L,false,false),access->access.registryOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.FLAT).createWorldDimensions()));
        until(()->MC.level!=null&&MC.player!=null&&MC.getSingleplayerServer()!=null);waitTicks(20);screen(()->null);
        var server=client(MC::getSingleplayerServer);
        server.submit(()->NoviceCatalogProbe.run(server.overworld())).get(60,TimeUnit.SECONDS);
        var pos=client(()->MC.player.blockPosition().east());
        var villager=server.submit(()->{var level=server.overworld();level.setBlockAndUpdate(pos,Blocks.LECTERN.defaultBlockState());var v=new Villager(net.minecraft.world.entity.EntityType.VILLAGER,level);v.moveTo(server.getPlayerList().getPlayers().get(0).position().add(2,0,0));v.setNoAi(true);v.setVillagerData(v.getVillagerData().setProfession(VillagerProfession.LIBRARIAN));var offers=new MerchantOffers();offers.add(new MerchantOffer(new ItemCost(Items.EMERALD,5),new ItemStack(Items.GLASS,4),12,1,0));v.setOffers(offers);v.setVillagerXp(0);level.addFreshEntity(v);return v;}).get();
        until(()->MC.level.getEntity(villager.getId())!=null&&MC.level.getBlockState(pos).is(Blocks.LECTERN));
        run(()->{var v=(Villager)MC.level.getEntity(villager.getId());var candidate=QuickSetup.candidates(MC,v).stream().filter(c->c.pos().equals(pos)).findFirst().orElseThrow();check(QuickSetup.confirm(MC,v,candidate),"Workstation and villager detected");var cfg=RerollerConfig.get();cfg.targets.clear();var t=new RerollerConfig.EnchantTarget();t.kind=SearchTarget.Kind.ITEM;t.itemId="minecraft:glass";t.minQuantity=4;t.maxEmeraldCost=5;cfg.targets.add(t);cfg.targets.add(new RerollerConfig.EnchantTarget("minecraft:thorns",3,21));VillagerRerollClient.CONTROLLER.newSession(MC);});
        until(()->VillagerRerollClient.CONTROLLER.getState()==RerollState.FOUND);
        run(()->{check(VillagerRerollClient.CONTROLLER.getAttempts()==1,"Merchant packet processed once");check(RerollerConfig.get().targets.get(0).bestPrice==5,"Found actual item trade");check(RerollerConfig.get().targets.get(1).active(),"Any matching target stops search");MC.player.closeContainer();RerollerConfig.get().targets.get(0).status=SearchTarget.Status.ACTIVE;VillagerRerollClient.CONTROLLER.resume(MC);});
        until(()->VillagerRerollClient.CONTROLLER.getAttempts()==2);
        run(()->{check(VillagerRerollClient.CONTROLLER.getState()!=RerollState.FOUND,"Same price not reported twice");VillagerRerollClient.CONTROLLER.emergencyStop(MC);MC.player.closeContainer();});
        long seconds=client(()->VillagerRerollClient.CONTROLLER.getElapsedSeconds());waitTicks(25);run(()->check(VillagerRerollClient.CONTROLLER.getElapsedSeconds()==seconds,"Paused time excluded"));
        server.submit(()->{server.overworld().setBlockAndUpdate(pos,Blocks.LECTERN.defaultBlockState());var book=new ItemStack(Items.ENCHANTED_BOOK);book.enchant(server.overworld().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(net.minecraft.world.item.enchantment.Enchantments.THORNS),3);var offers=new MerchantOffers();offers.add(new MerchantOffer(new ItemCost(Items.EMERALD,11),java.util.Optional.of(new ItemCost(Items.BOOK)),book,12,1,0));villager.setOffers(offers);}).get();
        until(()->MC.level.getBlockState(pos).is(Blocks.LECTERN));run(()->{RerollerConfig.get().targets.clear();RerollerConfig.get().targets.add(new RerollerConfig.EnchantTarget("minecraft:thorns",3,21));VillagerRerollClient.CONTROLLER.resume(MC);});until(()->VillagerRerollClient.CONTROLLER.getState()==RerollState.FOUND);
        run(()->{var t=RerollerConfig.get().targets.get(0);check(t.bestPrice==11&&t.bestLevel==3&&t.vanillaBaseMinimum==11,"Enchanted book and price floor detected");MC.player.closeContainer();});shot("legacy-found-book");
        // Open the embedded search panel in an actual server-backed merchant menu.
        run(()->{var v=(Villager)MC.level.getEntity(villager.getId());net.fabricmc.fabric.api.event.player.UseEntityCallback.EVENT.invoker().interact(MC.player,MC.level,net.minecraft.world.InteractionHand.MAIN_HAND,v,null);MC.gameMode.interact(MC.player,v,net.minecraft.world.InteractionHand.MAIN_HAND);});until(()->MC.screen instanceof MerchantScreen);
        click("villager-reroll.merchant.open");run(()->{check(MerchantSearch.panel().open(),"Search panel opens inside trade screen");((EditBox)MerchantSearch.panel().children().stream().filter(c->c instanceof EditBox).findFirst().orElseThrow()).setValue("Mending");});waitTicks(2);click("Mending");shot("legacy-merchant-search");click("villager-reroll.merchant.add");run(()->check(RerollerConfig.get().targets.stream().anyMatch(t->t.enchantmentId.equals("minecraft:mending")),"Merchant search adds requested book"));
        run(()->{MerchantSearch.panel().hide();MC.player.closeContainer();VillagerRerollClient.CONTROLLER.emergencyStop(MC);});
    }
}
