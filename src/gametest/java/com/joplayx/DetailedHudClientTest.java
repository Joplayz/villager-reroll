package com.joplayx;
import com.joplayx.client.config.*;
import com.joplayx.client.hud.RerollerHud;
import com.joplayx.client.util.SearchNames;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.TitleScreen;
import java.util.*;
public class DetailedHudClientTest implements FabricClientGameTest {
    private static void check(boolean ok,String reason){if(!ok)throw new AssertionError(reason);}
    private static void click(ClientGameTestContext ctx,double[] xy){ctx.getInput().setCursorPos(xy[0],xy[1]);ctx.waitTick();ctx.getInput().pressMouse(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT);ctx.waitTicks(2);}
    private static double[] buttonPoint(net.minecraft.client.Minecraft mc,Button b){var s=mc.screen;return new double[]{(b.getX()+b.getWidth()/2.0)*mc.getWindow().getScreenWidth()/s.width,(b.getY()+b.getHeight()/2.0)*mc.getWindow().getScreenHeight()/s.height};}
    @Override public void runTest(ClientGameTestContext context){
        context.runOnClient(mc->{var cfg=RerollerConfig.get();cfg.hudDetails=false;cfg.targets.clear();for(int i=0;i<20;i++)cfg.targets.add(new RerollerConfig.EnchantTarget("minecraft:thorns",3,21+i));});
        for(int[] size:List.of(new int[]{640,480,2},new int[]{960,720,3},new int[]{1280,720,2})){
            context.getInput().resizeWindow(size[0],size[1]);
            context.runOnClient(mc->{mc.options.guiScale().set(size[2]);mc.resizeGui();});context.waitTicks(2);
            context.setScreen(()->new EnchantTargetListScreen(new TitleScreen()));
            context.runOnClient(mc->{var screen=mc.screen;var list=(AbstractSelectionList<?>)screen.children().stream().filter(c->c instanceof AbstractSelectionList).findFirst().orElseThrow();
                check(list.children().size()==20 && list.maxScrollAmount()>0,"all twenty entries are in scrollable list");
                check(list.getBottom()<=screen.height-96,"list is strictly above first footer button row");list.setScrollAmount(list.maxScrollAmount());});
            context.waitTicks(2);
            context.runOnClient(mc->{var list=(AbstractSelectionList<?>)mc.screen.children().stream().filter(c->c instanceof AbstractSelectionList).findFirst().orElseThrow();
                var row=(ContainerObjectSelectionList.Entry<?>)list.children().getLast();for(var child:row.children())if(child instanceof Button b)check(b.getY()>=list.getY()&&b.getBottom()<=list.getBottom(),"last entry edit/remove buttons are completely visible");});
        }
        context.takeScreenshot("twenty-searches-scrolled");
        double[] edit=context.computeOnClient(mc->{var list=(AbstractSelectionList<?>)mc.screen.children().stream().filter(c->c instanceof AbstractSelectionList).findFirst().orElseThrow();var row=(ContainerObjectSelectionList.Entry<?>)list.children().getLast();return buttonPoint(mc,(Button)row.children().getFirst());});
        click(context,edit);context.waitForScreen(TargetEditScreen.class);context.clickScreenButton("villager-reroll.button.cancel");context.waitTicks(2);context.waitForScreen(EnchantTargetListScreen.class);
        double[] remove=context.computeOnClient(mc->{var list=(AbstractSelectionList<?>)mc.screen.children().stream().filter(c->c instanceof AbstractSelectionList).findFirst().orElseThrow();var row=(ContainerObjectSelectionList.Entry<?>)list.children().getLast();return buttonPoint(mc,(Button)row.children().getLast());});
        click(context,remove);context.runOnClient(mc->check(RerollerConfig.get().targets.size()==19,"twentieth entry can be edited then removed with real mouse clicks"));
        context.runOnClient(mc->{var cfg=RerollerConfig.get();cfg.targets.clear();cfg.workstation="lectern";
            for(var id:List.of("thorns","mending","protection","unbreaking","efficiency","fortune","silk_touch","sharpness","smite","looting","power","punch","infinity","respiration","aqua_affinity"))cfg.targets.add(new RerollerConfig.EnchantTarget("minecraft:"+id,id.equals("mending")?1:3,21));
            cfg.targets.get(0).record(15,1,3,1,"Thorns III");cfg.targets.get(1).record(12,1,1,1,"Mending");cfg.targets.get(1).status=SearchTarget.Status.ACQUIRED;cfg.targets.get(2).status=SearchTarget.Status.IGNORED;
            cfg.hudDetails=true;RerollerConfig.save();RerollerConfig.load();check(RerollerConfig.get().hudDetails,"detailed HUD setting persists");
            var seen=new HashSet<String>();boolean[] prices={false};
            for(int i=0;i<20;i++){var lines=RerollerHud.lines(320,240,true);check(10+12*lines.size()<=228,"detailed HUD fits small GUI");seen.addAll(lines);if(lines.stream().anyMatch(l->l.contains("15 Emerald")))prices[0]=true;RerollerHud.nextPage();}
            for(var t:RerollerConfig.get().targets)check(seen.stream().anyMatch(l->l.contains(SearchNames.target(t))),"every target accessible through HUD pages");
            check(prices[0]&&seen.stream().anyMatch(l->l.contains("21 Emerald")),"HUD shows saved actual price and requested maximum");
            check(seen.stream().anyMatch(l->l.startsWith("Found:"))&&seen.stream().anyMatch(l->l.startsWith("Acquired:"))&&seen.stream().anyMatch(l->l.startsWith("Ignored:"))&&seen.stream().anyMatch(l->l.startsWith("Searching:")),"all search statuses are explicit");
        });
        context.getInput().resizeWindow(640,480);context.runOnClient(mc->{mc.options.guiScale().set(2);mc.resizeGui();});context.waitTicks(2);
        context.setScreen(()->new HudPositionScreen(new TitleScreen()));context.waitTicks(2);context.takeScreenshot("detailed-hud-targets");
        context.runOnClient(mc->{var screen=mc.screen;int area=screen.height-52;check(RerollerHud.panelHeight(screen.width,area)<=area-12,"preview leaves separate area for buttons");});
        context.clickScreenButton("villager-reroll.hud.next_page");context.waitTicks(2);context.takeScreenshot("detailed-hud-next-page");
        context.runOnClient(mc->{RerollerConfig.get().hudDetails=false;check(RerollerHud.panelHeight()==58,"compact HUD remains original size");RerollerConfig.get().targets.clear();RerollerConfig.save();});
        context.setScreen(TitleScreen::new);
    }
}
