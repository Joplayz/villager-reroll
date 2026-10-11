package com.joplayx.client.config;

import com.joplayx.client.util.Text;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.*;

public class WishlistsScreen extends Screen {
    private final Screen parent;
    private EditBox name;
    private String draft="",message="";
    private int selected=-1,page;
    private Runnable pending;
    private List<WishlistStore.Wishlist> all;
    public WishlistsScreen(Screen parent){super(Text.tr("wishlists.title"));this.parent=parent;}
    @Override protected void init(){
        all=new ArrayList<>(WishlistStore.starters());all.addAll(RerollerConfig.get().wishlists);
        name=addRenderableWidget(new EditBox(font,8,28,width-16,20,Text.tr("wishlists.name")));
        name.setMaxLength(48);name.setValue(draft);name.setHint(Text.tr("wishlists.name"));name.setResponder(s->draft=s);
        int rows=Math.max(1,(height-142)/24),from=page*rows;
        for(int i=from;i<Math.min(all.size(),from+rows);i++){
            int index=i;var p=all.get(i);
            addRenderableWidget(Button.builder(Component.literal((selected==i?"✓ ":"")+font.plainSubstrByWidth(p.name,width-64)),b->pending=()->{
                selected=index;draft=p.name;rebuildWidgets();
            }).bounds(8,54+(i-from)*24,width-16,20).build());
        }
        int w=(width-24)/3;
        button("wishlists.save",8,height-80,w,()->attempt(()->{
            if(RerollerConfig.get().wishlists.size()>=100)throw new IllegalArgumentException();
            var p=WishlistStore.capture(draft,RerollerConfig.get().workstation,RerollerConfig.get().targets);
            RerollerConfig.get().wishlists.add(p);RerollerConfig.save();message=Text.str("wishlists.saved");rebuildWidgets();
        }));
        button("wishlists.import",12+w,height-80,w,()->attempt(()->{
            if(RerollerConfig.get().wishlists.size()>=100)throw new IllegalArgumentException();
            var p=WishlistStore.parse(minecraft.keyboardHandler.getClipboard());
            RerollerConfig.get().wishlists.add(p);RerollerConfig.save();message=Text.str("wishlists.saved");rebuildWidgets();
        }));
        button("wishlists.export",16+2*w,height-80,w,()->attempt(()->{
            minecraft.keyboardHandler.setClipboard(WishlistStore.export(all.get(selected)));message=Text.str("wishlists.copied");
        })).active=selected>=0&&selected<all.size();
        button("wishlists.add",8,height-56,w,()->attempt(()->{
            var cfg=RerollerConfig.get();var p=all.get(selected);
            if(cfg.targets.size()+p.targets.size()>100)throw new IllegalArgumentException();
            p.targets.forEach(t->cfg.targets.add(WishlistStore.criteria(t)));cfg.workstation=p.workstation;
            com.joplayx.client.util.QuickSetup.clear();cfg.clearLecternPos();RerollerConfig.save();minecraft.setScreen(parent);
        })).active=selected>=0&&selected<all.size();
        button("wishlists.duplicate",12+w,height-56,w,()->attempt(()->{
            if(RerollerConfig.get().wishlists.size()>=100)throw new IllegalArgumentException();
            var p=WishlistStore.parse(WishlistStore.export(all.get(selected)));
            p.name=(!draft.isBlank()&&!draft.equals(p.name))?draft:font.plainSubstrByWidth(p.name,140)+" (copy)";
            if(p.name.length()>48)p.name=p.name.substring(0,48);
            RerollerConfig.get().wishlists.add(p);RerollerConfig.save();message=Text.str("wishlists.saved");rebuildWidgets();
        })).active=selected>=0&&selected<all.size();
        button("button.done",16+2*w,height-56,w,this::onClose);
        button("picker.previous",8,height-28,70,()->{page--;rebuildWidgets();}).active=page>0;
        button("picker.next",82,height-28,70,()->{page++;rebuildWidgets();}).active=from+rows<all.size();
    }
    private void attempt(Runnable r){try{r.run();}catch(IllegalArgumentException|IndexOutOfBoundsException e){message=Text.str("wishlists.invalid");}}
    private Button button(String key,int x,int y,int w,Runnable r){return addRenderableWidget(Button.builder(Text.tr(key),b->pending=r).bounds(x,y,w,20).build());}
    @Override public void tick(){if(pending!=null){var r=pending;pending=null;r.run();}}
    @Override public void onClose(){minecraft.setScreen(parent);}
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float dt){
        super.extractRenderState(g,mx,my,dt);g.text(font,title,8,8,0xFFFFCC66);
        g.text(font,font.plainSubstrByWidth(message.isEmpty()?Text.str("wishlists.note"):message,width-16),8,height-94,0xFFDDDDDD);
    }
}
