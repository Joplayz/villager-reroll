package com.joplayx.client.config;

import com.joplayx.client.VillagerRerollClient;
import com.joplayx.client.util.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.item.*;
import java.util.*;

public class FoundTradeScreen extends Screen {
    private final Screen parent;
    private int page;
    private Runnable pending;
    private List<RerollerConfig.EnchantTarget> results;
    public FoundTradeScreen(Screen parent){super(Text.tr("results.title"));this.parent=parent;}
    @Override protected void init(){
        results=RerollerConfig.get().targets.stream().filter(t->t.bestPrice>=0).toList();page=Math.max(0,Math.min(page,results.size()-1));
        int w=(width-20)/2;
        button("results.keep",8,height-76,w,()->action(SearchTarget.Status.ACQUIRED)).active=!results.isEmpty();
        button("search.continue",12+w,height-76,w,()->action(SearchTarget.Status.ACTIVE)).active=!results.isEmpty();
        button("picker.previous",8,height-52,70,()->{page--;rebuildWidgets();}).active=page>0;
        button("picker.next",82,height-52,70,()->{page++;rebuildWidgets();}).active=page+1<results.size();
        button("results.next_villager",156,height-52,width-164,()->{
            VillagerRerollClient.CONTROLLER.pauseForConfig(minecraft);QuickSetup.clear();RerollerConfig.get().clearLecternPos();RerollerConfig.save();
            minecraft.gui.setScreen(null);
            if(minecraft.player!=null)minecraft.player.sendSystemMessage(Text.tr("results.aim_next",VillagerRerollClient.setPositionKey.getTranslatedKeyMessage()));
        }).active=RerollerConfig.get().targets.stream().noneMatch(t->t.status==SearchTarget.Status.FOUND);
        button("button.done",8,height-28,width-16,this::onClose);
    }
    private void action(SearchTarget.Status status){results.get(page).status=status;RerollerConfig.save();rebuildWidgets();}
    private Button button(String key,int x,int y,int w,Runnable r){return addRenderableWidget(Button.builder(Text.tr(key),b->pending=r).bounds(x,y,w,20).build());}
    @Override public void tick(){if(pending!=null){var r=pending;pending=null;r.run();}}
    @Override public void onClose(){minecraft.gui.setScreen(parent);}
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float dt){
        super.extractRenderState(g,mx,my,dt);g.text(font,title,8,8,0xFFFFCC66);
        if(!results.isEmpty()){
            var t=results.get(page);var icon=t.kind==SearchTarget.Kind.ENCHANTMENT?SearchNames.item("minecraft:enchanted_book"):SearchNames.item(t.itemId);
            g.item(icon,8,30);g.text(font,font.plainSubstrByWidth(SearchNames.found(t),width-40),30,34,0xFFFFFFFF);
            var lines=List.of(t.kind==SearchTarget.Kind.ENCHANTMENT?Text.str("results.quantity",t.bestQuantity,t.bestLevel):Text.str("results.item_quantity",t.bestQuantity),
                Text.str("results.price",t.bestPrice,SearchNames.name(t.kind==SearchTarget.Kind.ENCHANTMENT?"minecraft:emerald":t.paymentItemId,false))
                    +(t.bestSecondPrice>0?" + "+t.bestSecondPrice+" "+SearchNames.name(t.kind==SearchTarget.Kind.ENCHANTMENT?"minecraft:book":t.secondPaymentItemId,false):""),
                t.previousBestPrice<0?Text.str("results.first"):Text.str("results.previous",t.previousBestPrice,t.previousBestQuantity),
                Text.str("search.status."+t.status.name().toLowerCase(Locale.ROOT)));
            for(int i=0;i<lines.size();i++)g.text(font,font.plainSubstrByWidth(lines.get(i),width-16),8,56+i*14,0xFFDDDDDD);
        }
        g.text(font,font.plainSubstrByWidth(Text.str("results.lock_note"),width-16),8,height-104,0xFFFFCC66);
        g.text(font,font.plainSubstrByWidth(Text.str("results.action_note"),width-16),8,height-90,0xFFBBBBBB);
    }
}
