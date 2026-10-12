package com.joplayx.client.config;

import com.joplayx.client.util.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import java.util.*;

public class TradePickerScreen extends Screen {
    private record Choice(String label,ItemStack icon,RerollerConfig.EnchantTarget target) {}
    private final Screen parent;
    private final boolean books;
    private List<Choice> choices=List.of(),visible=List.of();
    private EditBox search;
    private String query="";
    private int page;
    private Runnable pending;
    public TradePickerScreen(Screen parent,boolean books){super(Text.tr(books?"picker.books":"picker.trades"));this.parent=parent;this.books=books;}
    @Override protected void init() {
        var station=Workstation.from(RerollerConfig.get().workstation);
        choices=books?TradeCatalog.enchantments().stream().map(id->new Choice(SearchNames.name(id,true),
                SearchNames.item("minecraft:enchanted_book"),new RerollerConfig.EnchantTarget(id,1,64))).toList()
                :TradeCatalog.forStation(station).stream().filter(e->!e.item().equals("minecraft:enchanted_book"))
                .map(e->new Choice(e.label(),SearchNames.item(e.item()),e.target())).toList();
        search=addRenderableWidget(new EditBox(font,8,28,width-16,20,Text.tr("picker.search")));
        search.setMaxLength(128);search.setValue(query);
        search.setHint(Text.tr("picker.search"));
        search.setResponder(s->{query=s;page=0;pending=this::refresh;});
        refresh();
    }
    private List<Button> rows=new ArrayList<>();
    private Button previous,next,custom,done;
    private void refresh() {
        rows.forEach(this::removeWidget);rows.clear();
        if(previous!=null){removeWidget(previous);removeWidget(next);removeWidget(custom);removeWidget(done);}
        String q=query.toLowerCase(Locale.ROOT).trim();
        visible=choices.stream().filter(c->c.label().toLowerCase(Locale.ROOT).contains(q)||c.target().targetId().contains(q)
                ||c.target().paymentItemId.contains(q)).toList();
        int count=Math.max(1,(height-116)/24),from=page*count;
        if(from>=visible.size()&&page>0){page=0;from=0;}
        for(int i=from;i<Math.min(visible.size(),from+count);i++) {
            var c=visible.get(i);String label=font.plainSubstrByWidth(c.label(),width-66);
            rows.add(addRenderableWidget(Button.builder(Component.literal(label),b->pending=()->
                    minecraft.gui.setScreen(new TargetEditScreen(parent,c.target()))).bounds(30,54+(i-from)*24,width-38,20).build()));
        }
        previous=button("picker.previous",8,height-52,70,()->{page--;refresh();});previous.active=page>0;
        next=button("picker.next",82,height-52,70,()->{page++;refresh();});next.active=from+count<visible.size();
        custom=button("picker.custom",156,height-52,width-164,()->{
            var t=new RerollerConfig.EnchantTarget("",1,64);t.kind=books?SearchTarget.Kind.ENCHANTMENT:SearchTarget.Kind.ITEM;
            minecraft.gui.setScreen(new TargetEditScreen(parent,t));
        });
        done=button("button.cancel",8,height-26,width-16,this::onClose);
    }
    private Button button(String key,int x,int y,int w,Runnable action){return addRenderableWidget(Button.builder(Text.tr(key),b->pending=action).bounds(x,y,w,20).build());}
    @Override public void tick(){if(pending!=null){var r=pending;pending=null;r.run();}}
    @Override public void onClose(){minecraft.gui.setScreen(parent);}
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float dt){
        super.extractRenderState(g,mx,my,dt);g.text(font,title,8,8,0xFFFFCC66);
        int count=Math.max(1,(height-116)/24),from=page*count;
        for(int i=from;i<Math.min(visible.size(),from+count);i++)g.item(visible.get(i).icon(),9,56+(i-from)*24);
        if(visible.isEmpty())g.text(font,Text.tr("picker.empty"),8,58,0xFFBBBBBB);
        g.text(font,font.plainSubstrByWidth(Text.str("picker.note"),width-16),8,height-65,0xFFBBBBBB);
    }
}
