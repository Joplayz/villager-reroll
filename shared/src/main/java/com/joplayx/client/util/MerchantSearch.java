package com.joplayx.client.util;

import com.joplayx.client.VillagerRerollClient;
import com.joplayx.client.config.MerchantSearchPanel;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.client.screen.v1.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.BlockItem;
import net.minecraft.core.BlockPos;
import java.util.*;

/** Tracks actual client interactions; never guesses the merchant from the nearest entity. */
public final class MerchantSearch {
    private static Villager interacted;
    private static ClientLevel interactionWorld;
    private static int interactionTicks;
    private static BlockPos placed;
    private static Workstation placedStation;
    private static ClientLevel placementWorld;
    private static int placementTicks;
    private static MerchantSearchPanel panel;
    public static void register() {
        UseEntityCallback.EVENT.register((player,level,hand,entity,hit)->{
            var mc=Minecraft.getInstance();
            if(level==mc.level && player==mc.player) {
                interacted=entity instanceof Villager v?v:null;
                interactionWorld=mc.level;interactionTicks=100;
            }
            return InteractionResult.PASS;
        });
        UseBlockCallback.EVENT.register((player,level,hand,hit)->{
            var mc=Minecraft.getInstance();
            if(level==mc.level && player==mc.player && !VillagerRerollClient.CONTROLLER.running()
                    && player.getItemInHand(hand).getItem() instanceof BlockItem item) {
                for(var station:Workstation.values()) if(item.getBlock()==station.block) {
                    var pos=level.getBlockState(hit.getBlockPos()).canBeReplaced()?hit.getBlockPos():hit.getBlockPos().relative(hit.getDirection());
                    if(level.getBlockState(pos).canBeReplaced()) rememberPlacement(mc,pos,station);
                }
            }
            return InteractionResult.PASS;
        });
        ScreenEvents.AFTER_INIT.register((mc,screen,w,h)->{
            if(!(screen instanceof MerchantScreen merchant)) return;
            if(panel!=null && panel.merchant()==merchant) {panel.resizePanel(w,h);} else {
            Villager v=interactionTicks>0 && interactionWorld==mc.level?interacted:null;
            if(v==null || !v.isAlive() || mc.level.getEntity(v.getId())!=v) return;
            var station=Arrays.stream(Workstation.values()).filter(s->s.profession.equals(QuickSetup.profession(v))).findFirst().orElse(null);
            if(station==null) return;
            panel=new MerchantSearchPanel(merchant,v,station);
            panel.attach(w,h);
            }
            var current=panel;
            ScreenEvents.afterExtract(screen).register((s,g,x,y,dt)->{if(current.open())current.extractRenderState(g,x,y,dt);});
            ScreenEvents.afterTick(screen).register(s->current.tick());
            ScreenEvents.remove(screen).register(s->{current.hide();if(panel==current)panel=null;});
            ScreenMouseEvents.allowMouseClick(screen).register((s,e)->{
                if(!current.open())return true;
                current.mouseClicked(e,false);s.setFocused(current.open()?current.getFocused():null);return false;
            });
            ScreenMouseEvents.allowMouseRelease(screen).register((s,e)->{if(!current.open())return true;current.mouseReleased(e);return false;});
            ScreenMouseEvents.allowMouseDrag(screen).register((s,e,x,y)->{if(!current.open())return true;current.mouseDragged(e,x,y);return false;});
            ScreenMouseEvents.allowMouseScroll(screen).register((s,x,y,horizontal,vertical)->{if(!current.open())return true;current.mouseScrolled(x,y,horizontal,vertical);return false;});
            ScreenKeyboardEvents.allowKeyPress(screen).register((s,e)->{
                if(!current.open())return true;
                current.keyPressed(e);s.setFocused(current.open()?current.getFocused():null);return false;
            });
            ScreenKeyboardEvents.allowKeyRelease(screen).register((s,e)->{if(!current.open())return true;current.keyReleased(e);return false;});
            // Edit boxes are attached to the merchant's child list, so normal charTyped routing
            // works on every supported Fabric version without replacing Minecraft's input handler.
        });
    }
    public static void rememberPlacement(Minecraft mc,BlockPos pos,Workstation station) {
        placed=pos.immutable();placedStation=station;placementWorld=mc.level;placementTicks=200;
    }
    public static MerchantSearchPanel panel(){return panel;}
    public static void tick(Minecraft mc) {
        if(interactionTicks>0)interactionTicks--;
        if(mc.level==null || mc.level!=interactionWorld){interacted=null;interactionWorld=null;interactionTicks=0;}
        if(placementTicks<=0 || mc.level==null || mc.level!=placementWorld){placed=null;placementTicks=0;return;}
        placementTicks--;
        if(placementTicks%5!=0 || VillagerRerollClient.CONTROLLER.running() || !mc.level.getBlockState(placed).is(placedStation.block))return;
        var villagers=mc.level.getEntities(mc.player,new net.minecraft.world.phys.AABB(placed).inflate(4,2,4),e->e instanceof Villager v
                &&v.isAlive()&&!v.isBaby()&&v.getVillagerData().level()==1
                &&(QuickSetup.profession(v).equals(placedStation.profession)||QuickSetup.profession(v).equals("none")));
        if(villagers.size()!=1)return;
        var v=(Villager)villagers.getFirst();
        if(!QuickSetup.profession(v).equals(placedStation.profession))return; // Wait for the server to assign a profession.
        var candidates=QuickSetup.candidates(mc,v);
        if(candidates.size()==1 && candidates.getFirst().pos().equals(placed) && QuickSetup.confirm(mc,v,candidates.getFirst())) {
            placementTicks=0;
            mc.player.sendSystemMessage(Text.tr("merchant.detected",placedStation.block.getName()));
        }
    }
}
