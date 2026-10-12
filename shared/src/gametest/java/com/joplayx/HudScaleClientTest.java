package com.joplayx;
import com.joplayx.client.config.*;
import com.joplayx.client.hud.RerollerHud;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.screens.TitleScreen;
public class HudScaleClientTest implements FabricClientGameTest {
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    @Override public void runTest(ClientGameTestContext context){
        context.getInput().resizeWindow(960,720);
        context.runOnClient(mc->{mc.options.guiScale().set(2);mc.resizeGui();var cfg=RerollerConfig.get();cfg.hudDetails=true;cfg.hudScale=1;cfg.hudSnap=false;cfg.hudX=0;cfg.hudY=0;cfg.targets.clear();cfg.targets.add(new RerollerConfig.EnchantTarget("minecraft:thorns",3,21));cfg.targets.getFirst().record(15,1,3,1,"Thorns III");});
        context.setScreen(()->new HudPositionScreen(new TitleScreen()));
        for(int i=0;i<5;i++)context.clickScreenButton("villager-reroll.hud.smaller");
        context.clickScreenButton("villager-reroll.hud.snap_off");
        context.runOnClient(mc->check(RerollerConfig.get().hudScale==1&&!RerollerConfig.get().hudSnap,"size and snap stay drafts"));
        context.takeScreenshot("hud-size-75-snap-guides");
        context.clickScreenButton("villager-reroll.button.cancel");
        context.runOnClient(mc->check(RerollerConfig.get().hudScale==1&&!RerollerConfig.get().hudSnap,"Cancel discards size and snapping"));
        context.setScreen(()->new HudPositionScreen(new TitleScreen()));
        for(int i=0;i<10;i++)context.clickScreenButton("villager-reroll.hud.smaller");
        context.clickScreenButton("villager-reroll.hud.snap_off");
        // Drag through Minecraft's real mouse path to within three pixels of center.
        context.getInput().setCursorPos(32,32);context.waitTick();context.getInput().holdMouse(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT);context.waitTick();
        double[] destination=context.computeOnClient(mc->{int w=mc.gui.screen().width,h=mc.gui.screen().height-76;int pw=RerollerHud.panelWidth(w,h,0.5),ph=RerollerHud.panelHeight(w,h,0.5);return new double[]{2*(16+(w-pw-12)/2.0+3),2*(16+(h-ph-12)/2.0+3)};});
        context.getInput().setCursorPos(destination[0],destination[1]);context.waitTicks(2);context.getInput().releaseMouse(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT);context.waitTick();
        context.takeScreenshot("hud-size-50-centered");context.clickScreenButton("villager-reroll.button.done");
        context.runOnClient(mc->{RerollerConfig.load();var cfg=RerollerConfig.get();check(cfg.hudScale==0.5&&cfg.hudSnap,"size and snapping persist");check(cfg.hudX==0.5&&cfg.hudY==0.5,"real dragging snaps both axes to center");
            check(HudPositionScreen.alignedFraction(0.49,200,false)==0.49,"snap off preserves free placement");check(HudPositionScreen.alignedFraction(0.03,200,true)==0,"snap near leading edge");check(HudPositionScreen.alignedFraction(0.97,200,true)==1,"snap near trailing edge");
            for(boolean detail:new boolean[]{false,true}){cfg.hudDetails=detail;for(double scale:new double[]{0.5,0.75,1,1.5}){check(RerollerHud.panelWidth(320,164,scale)<=308,"scaled width fits small preview");check(RerollerHud.panelHeight(320,164,scale)<=152,"scaled height fits above footer");}}
            cfg.hudDetails=true;
        });
        context.setScreen(()->new HudPositionScreen(new TitleScreen()));
        for(int i=0;i<20;i++)context.clickScreenButton("villager-reroll.hud.larger");
        context.clickScreenButton("villager-reroll.button.done");
        context.runOnClient(mc->{RerollerConfig.load();check(RerollerConfig.get().hudScale==1.5,"150 percent saves");});
        context.setScreen(()->new HudPositionScreen(new TitleScreen()));context.takeScreenshot("hud-size-150");context.clickScreenButton("villager-reroll.button.cancel");
        context.runOnClient(mc->{var cfg=RerollerConfig.get();cfg.hudScale=1;cfg.hudSnap=false;cfg.hudX=0;cfg.hudY=0;cfg.hudDetails=false;cfg.targets.clear();RerollerConfig.save();});
        context.setScreen(TitleScreen::new);
    }
}
