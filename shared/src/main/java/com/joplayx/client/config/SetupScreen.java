package com.joplayx.client.config;

import com.joplayx.client.util.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.entity.npc.villager.Villager;
import java.util.List;

public class SetupScreen extends Screen {
    private final Screen parent;
    private final Villager villager;
    private final List<QuickSetup.Candidate> candidates;
    private int page, selected=-1;
    private boolean preview;
    private int previewTicks;
    private Runnable pending;
    private String error="";
    public SetupScreen(Screen parent,Villager v,List<QuickSetup.Candidate> candidates) {
        super(Text.tr("setup.title"));this.parent=parent;villager=v;this.candidates=candidates;
        if(candidates.size()==1) selected=0;
    }
    @Override protected void init() {
        if(preview){
            add("setup.confirm",8,height-28,(width-20)/2,this::confirm);
            add("picker.previous",12+(width-20)/2,height-28,(width-20)/2,()->{preview=false;rebuildWidgets();});
            return;
        }
        int rows=Math.max(1,(height-120)/24),from=page*rows;
        for(int i=from;i<Math.min(candidates.size(),from+rows);i++) {
            final int index=i;var c=candidates.get(i);var p=c.pos();
            String name=(selected==i?"✓ ":"")+c.station().block.getName().getString()+"  "+p.getX()+", "+p.getY()+", "+p.getZ();
            addRenderableWidget(Button.builder(net.minecraft.network.chat.Component.literal(name),b->pending=()->{
                selected=index;QuickSetup.preview(villager,c.pos());rebuildWidgets();
            }).bounds(8,66+(i-from)*24,width-16,20).build());
        }
        add("picker.previous",8,height-48,68,()->{page=Math.max(0,page-1);rebuildWidgets();}).active=page>0;
        add("picker.next",80,height-48,68,()->{page++;rebuildWidgets();}).active=from+rows<candidates.size();
        add("setup.preview",width-156,height-48,148,()->{
            if(selected>=0){QuickSetup.preview(villager,candidates.get(selected).pos());preview=true;rebuildWidgets();}
        }).active=selected>=0;
        add("setup.confirm",8,height-24,(width-20)/2,this::confirm).active=selected>=0;
        add("button.cancel",12+(width-20)/2,height-24,(width-20)/2,this::onClose);
    }
    private void confirm(){
        if(selected>=0&&QuickSetup.confirm(minecraft,villager,candidates.get(selected))) minecraft.gui.setScreen(new EnchantTargetListScreen(parent));
        else {error=Text.str("setup.changed");preview=false;rebuildWidgets();}
    }
    private Button add(String key,int x,int y,int w,Runnable action) {
        return addRenderableWidget(Button.builder(Text.tr(key),b->pending=action).bounds(x,y,w,20).build());
    }
    @Override public void tick(){if(pending!=null){var r=pending;pending=null;r.run();}if(preview&&selected>=0&&previewTicks++%40==0)QuickSetup.preview(villager,candidates.get(selected).pos());}
    @Override public boolean isPauseScreen(){return false;}
    @Override public boolean isInGameUi(){return preview;}
    @Override public void onClose(){minecraft.gui.setScreen(parent);}
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float dt) {
        if(preview){
            g.fill(4,4,width-4,26,0xCC101418);
            g.text(font,font.plainSubstrByWidth(Text.str("setup.preview_note"),width-16),8,10,0xFFFFFFFF);
            for(var child:children())if(child instanceof Button b)b.extractRenderState(g,mx,my,dt);
            return;
        }
        super.extractRenderState(g,mx,my,dt);
        g.text(font,title,8,8,0xFFFFCC66);
        String line=Text.str(candidates.isEmpty()?"setup.none":"setup.note");
        g.text(font,font.plainSubstrByWidth(line,width-16),8,26,0xFFFFFFFF);
        g.text(font,font.plainSubstrByWidth(Text.str("setup.ownership"),width-16),8,40,0xFFBBBBBB);
        if(!error.isEmpty())g.text(font,font.plainSubstrByWidth(error,width-16),8,54,0xFFFF7777);
    }
}
