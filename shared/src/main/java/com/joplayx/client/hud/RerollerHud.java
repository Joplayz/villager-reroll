package com.joplayx.client.hud;

import com.joplayx.client.VillagerRerollClient;
import com.joplayx.client.config.*;
import com.joplayx.client.state.RerollState;
import com.joplayx.client.util.*;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import java.util.*;

public class RerollerHud {
    private static int page;
    private static String lastFound="";
    public static final int HEIGHT=58;
    public static void nextPage(){page++;}
    public static void extractRenderState(GuiGraphicsExtractor g,DeltaTracker delta){
        var cfg=RerollerConfig.get();var mc=Minecraft.getInstance();var state=VillagerRerollClient.CONTROLLER.getState();
        if(!cfg.hudEnabled || state==RerollState.IDLE || mc.gui.hud.isHidden() || mc.gui.screen() instanceof HudPositionScreen)return;
        drawPanel(g,lines(g.guiWidth(),g.guiHeight(),false),cfg.hudX,cfg.hudY,accent());
    }
    private static int accent(){
        var state=VillagerRerollClient.CONTROLLER.getState();
        return state==RerollState.FOUND?0xFF8DD99B:state==RerollState.PAUSED||state==RerollState.ERROR?0xFFE7C67A:0xFF9DBBCB;
    }
    /** The same layout is used by the HUD, positioning preview, and size calculations. */
    public static List<String> lines(int screenWidth,int screenHeight,boolean preview){
        return lines(screenWidth,screenHeight,preview,RerollerConfig.get().hudScale);
    }
    public static List<String> lines(int screenWidth,int screenHeight,boolean preview,double requestedScale){
        double scale=effectiveScale(screenWidth,screenHeight,requestedScale);
        return layout((int)((screenWidth-12)/(scale*validScale(RerollerConfig.get().hudScaleX)))+12,(int)(screenHeight/(scale*validScale(RerollerConfig.get().hudScaleY))),preview);
    }
    private static List<String> layout(int screenWidth,int screenHeight,boolean preview){
        var cfg=RerollerConfig.get();var controller=VillagerRerollClient.CONTROLLER;var state=controller.getState();
        boolean found=state==RerollState.FOUND,paused=state==RerollState.PAUSED||state==RerollState.ERROR;
        String status=Text.str(found?"hud.found":paused?"hud.paused":"hud.running");
        long seconds=preview?45:controller.getElapsedSeconds();
        String time=String.format(Locale.ROOT,"%d:%02d",seconds/60,seconds%60);
        String configKey=VillagerRerollClient.openConfigKey.getTranslatedKeyMessage().getString();
        String toggleKey=VillagerRerollClient.startStopKey.getTranslatedKeyMessage().getString();
        String controls=found?Text.str("hud.review",configKey):Text.str(paused?"hud.resume_controls":"hud.pause_controls",toggleKey,configKey);
        var result=new ArrayList<>(List.of(Text.str("hud.header"),status,Text.str("hud.stats",preview?12:controller.getAttempts(),time)));
        if(!cfg.hudDetails){result.add(controls);return result;}
        if(paused&&!preview)result.set(1,status+": "+controller.getStatusMessage());
        var station=Workstation.from(cfg.workstation);
        result.add(station.block.getName().getString()+(cfg.lecternPos()==null?"":" · "+cfg.lecternPos().toShortString()));
        result.clear();
        result.add(Text.str("hud.header")+" | "+status);
        result.add(Text.str("hud.stats",preview?12:controller.getAttempts(),time)+" | "+station.block.getName().getString()
                +(cfg.lecternPos()==null?"":" · "+cfg.lecternPos().toShortString()));
        if(paused&&!preview)result.addAll(wrap(controller.getStatusMessage(),logicalWidth(screenWidth)-12));
        int capacity=Math.max(1,(screenHeight-20)/10-result.size()-1);
        var targets=cfg.targets.stream().filter(t->t.appliesTo(cfg.workstation)).toList();
        var pages=new ArrayList<List<String>>();var current=new ArrayList<String>();int entries=0,foundPage=-1;
        StringBuilder signature=new StringBuilder();
        for(int i=0;i<targets.size();i++){
            var target=targets.get(i);var rows=targetLines(target,logicalWidth(screenWidth)-16);
            if(!current.isEmpty()&&(current.size()+rows.size()>capacity||entries>=6)){pages.add(current);current=new ArrayList<>();entries=0;}
            if(target.status==SearchTarget.Status.FOUND){if(foundPage<0)foundPage=pages.size();signature.append(i).append(':').append(target.bestPrice).append(';');}
            current.addAll(rows);entries++;
        }
        if(!current.isEmpty())pages.add(current);
        if(pages.isEmpty())pages.add(List.of(Text.str("hud.no_targets")));
        if(!signature.toString().equals(lastFound)){
            if(foundPage>=0)page=foundPage;
            lastFound=signature.toString();
        }
        page=Math.floorMod(page,pages.size());result.addAll(pages.get(page));
        String nextKey=VillagerRerollClient.nextHudPageKey.getTranslatedKeyMessage().getString();
        result.add((pages.size()>1?Text.str("hud.page",page+1,pages.size(),nextKey)+" | ":"")+controls);
        return result;
    }
    private static List<String> targetLines(SearchTarget t,int width){
        String name=SearchNames.target(t);
        if(t.kind==SearchTarget.Kind.ITEM)name=t.minQuantity+" × "+name;
        String status=Text.str("hud.target."+t.status.name().toLowerCase(Locale.ROOT));
        String payment=SearchNames.name(t.kind==SearchTarget.Kind.ENCHANTMENT?"minecraft:emerald":t.paymentItemId,false);
        String limit=t.maxEmeraldCost+" "+payment;
        if(t.kind==SearchTarget.Kind.ITEM&&!t.secondPaymentItemId.isBlank())limit+=" + "+t.maxSecondPayment+" "+SearchNames.name(t.secondPaymentItemId,false);
        String best=Text.str("hud.not_found");
        if(t.bestPrice>=0){
            String cost=t.bestPrice+" "+payment;
            if(t.bestSecondPrice>0)cost+=" + "+t.bestSecondPrice+" "+SearchNames.name(t.kind==SearchTarget.Kind.ENCHANTMENT?"minecraft:book":t.secondPaymentItemId,false);
            String actual="";
            if(t.kind==SearchTarget.Kind.ITEM&&t.bestQuantity!=t.minQuantity)actual=t.bestQuantity+" × "+SearchNames.found(t)+": ";
            if(t.kind==SearchTarget.Kind.ENCHANTMENT&&t.bestLevel!=t.minLevel)actual=SearchNames.found(t)+": ";
            best=Text.str("hud.best_short",actual+cost);
        }
        return wrap(Text.str("hud.target_row",status,name)+" | "+Text.str("hud.limit_short",limit)+" | "+best,width);
    }
    private static List<String> wrap(String text,int width){
        var font=Minecraft.getInstance().font;var result=new ArrayList<String>();
        while(!text.isEmpty()){
            String line=font.plainSubstrByWidth(text,Math.max(12,width));
            if(line.isEmpty())break;
            if(line.length()<text.length()){
                int space=line.lastIndexOf(' ');if(space>0)line=line.substring(0,space);
            }
            result.add(line);text=text.substring(line.length()).stripLeading();
        }
        return result;
    }
    public static double validScale(double value){return Double.isFinite(value)?Math.clamp(value,0.5,1.5):1;}
    public static double effectiveScale(int width,int height,double requested){
        double scale=validScale(requested);
        // A complete target must fit, including wrapped prices, above the fixed controls.
        for(int pass=0;pass<4;pass++){
            int minimumHeight=HEIGHT;
            if(RerollerConfig.get().hudDetails){
                int rows=1,available=logicalWidth((int)((width-12)/(scale*validScale(RerollerConfig.get().hudScaleX)))+12)-16;
                for(var target:RerollerConfig.get().targets)if(target.appliesTo(RerollerConfig.get().workstation))rows=Math.max(rows,targetLines(target,available).size());
                minimumHeight=8+10*(3+rows);
            }
            double fitted=Math.min(scale,Math.max(0.1,(height-12.0)/(minimumHeight*validScale(RerollerConfig.get().hudScaleY))));
            if(fitted==scale)break;scale=fitted;
        }
        return scale;
    }
    public static int panelHeight(){var w=Minecraft.getInstance().getWindow();return panelHeight(w.getGuiScaledWidth(),w.getGuiScaledHeight());}
    public static int panelHeight(int width,int height){return panelHeight(width,height,RerollerConfig.get().hudScale);}
    public static int panelHeight(int width,int height,double scale){return (int)Math.ceil(logicalHeight(lines(width,height,true,scale).size())*effectiveScale(width,height,scale)*validScale(RerollerConfig.get().hudScaleY));}
    private static int rowHeight(){return RerollerConfig.get().hudDetails?10:12;}
    private static int logicalHeight(int rows){return (RerollerConfig.get().hudDetails?8:10)+rowHeight()*rows;}
    private static int logicalWidth(int screenWidth){return Math.max(1,Math.min(RerollerConfig.get().hudDetails?360:200,screenWidth-12));}
    public static int panelWidth(int width){return panelWidth(width,Minecraft.getInstance().getWindow().getGuiScaledHeight(),RerollerConfig.get().hudScale);}
    public static int panelWidth(int width,int height,double requested){double scale=effectiveScale(width,height,requested);double sx=scale*validScale(RerollerConfig.get().hudScaleX);return (int)Math.ceil(logicalWidth((int)((width-12)/sx)+12)*sx);}
    public static int position(double fraction,int screenSize,int panelSize){
        if(!Double.isFinite(fraction))fraction=0;
        return 6+(int)Math.round(Math.clamp(fraction,0,1)*Math.max(0,screenSize-panelSize-12));
    }
    public static void drawPanel(GuiGraphicsExtractor g,List<String> lines,double px,double py,int accent){drawPanel(g,lines,px,py,accent,g.guiHeight());}
    public static void drawPanel(GuiGraphicsExtractor g,List<String> lines,double px,double py,int accent,int areaHeight){
        drawPanel(g,lines,px,py,accent,areaHeight,RerollerConfig.get().hudScale);
    }
    public static void drawPanel(GuiGraphicsExtractor g,List<String> lines,double px,double py,int accent,int areaHeight,double requested){
        double scale=effectiveScale(g.guiWidth(),areaHeight,requested);
        double sx=scale*validScale(RerollerConfig.get().hudScaleX),sy=scale*validScale(RerollerConfig.get().hudScaleY);
        var font=Minecraft.getInstance().font;int width=logicalWidth((int)((g.guiWidth()-12)/sx)+12),height=logicalHeight(lines.size());
        int originX=position(px,g.guiWidth(),(int)Math.ceil(width*sx)),originY=position(py,areaHeight,(int)Math.ceil(height*sy));
        g.pose().pushMatrix();
        g.pose().translate(originX,originY);g.pose().scale((float)sx,(float)sy);
        int x=0,y=0;
        g.fill(x,y,x+width,y+1,accent);g.fill(x,y+height-1,x+width,y+height,accent);
        g.fill(x,y+1,x+1,y+height-1,accent);g.fill(x+width-1,y+1,x+width,y+height-1,accent);
        g.fill(x+1,y+1,x+width-1,y+height-1,0xE0181C20);
        for(int i=0;i<lines.size();i++){
            String line=lines.get(i);int available=width-16;
            if(font.width(line)>available)line=font.plainSubstrByWidth(line,Math.max(0,available-font.width("...")))+"...";
            int color=i==1?accent:i==lines.size()-1?0xFFB8BDC5:0xFFF1F1F1;
            if(line.startsWith(Text.str("hud.target.found")+":"))color=0xFF8DD99B;
            if(line.startsWith(Text.str("hud.target.acquired")+":"))color=0xFF88C8EF;
            if(line.startsWith(Text.str("hud.target.ignored")+":"))color=0xFFAAAAAA;
            g.text(font,line,x+8,y+(RerollerConfig.get().hudDetails?4:6)+i*rowHeight(),color);
        }
        g.pose().popMatrix();
    }
}
