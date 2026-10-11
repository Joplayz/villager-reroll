package com.joplayx.client.config;

import com.joplayx.client.VillagerRerollClient;
import com.joplayx.client.util.*;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import java.util.*;

/** A modal search panel within the existing merchant screen: the live trade menu stays open. */
public final class MerchantSearchPanel extends Screen {
    private record Choice(String name,ItemStack icon,RerollerConfig.EnchantTarget target) {}
    private final MerchantScreen merchant;
    private final Villager villager;
    private final Workstation station;
    private final Map<AbstractWidget,Boolean> previousVisibility=new IdentityHashMap<>();
    private final List<AbstractWidget> attached=new ArrayList<>();
    private List<QuickSetup.Candidate> candidates=List.of();
    private List<Choice> choices=List.of(),visible=List.of();
    private int selectedStation=-1,page;
    private boolean open;
    private String query="",message="";
    private Choice selected;
    private EditBox search,quantity,price,secondPrice;
    private Runnable pending;
    private Button toggle;
    public MerchantSearchPanel(MerchantScreen merchant,Villager villager,Workstation station){
        super(Text.tr("merchant.title"));this.merchant=merchant;this.villager=villager;this.station=station;
    }
    public MerchantScreen merchant(){return merchant;}
    public boolean open(){return open;}
    public void attach(int w,int h){
        toggle=Button.builder(Text.tr("merchant.open"),b->pending=this::show).bounds(Math.max(4,(w-276)/2),Math.max(2,(h-166)/2-22),110,20).build();
        Screens.getWidgets(merchant).add(toggle);
    }
    public void resizePanel(int w,int h){
        var oldPos=selectedStation>=0&&selectedStation<candidates.size()?candidates.get(selectedStation).pos():null;
        String oldQuantity=quantity==null?null:quantity.getValue(),oldPrice=price==null?null:price.getValue(),oldSecond=secondPrice==null?null:secondPrice.getValue();
        attached.clear();previousVisibility.clear();attach(w,h);
        if(open){open=false;show();
            if(oldPos!=null)for(int i=0;i<candidates.size();i++)if(candidates.get(i).pos().equals(oldPos))selectedStation=i;
            if(selected!=null){if(oldQuantity!=null)quantity.setValue(oldQuantity);if(oldPrice!=null)price.setValue(oldPrice);if(oldSecond!=null&&secondPrice!=null)secondPrice.setValue(oldSecond);}
        }
    }
    public void show(){
        if(minecraft.player==null || minecraft.player.containerMenu!=merchant.getMenu())return;
        VillagerRerollClient.CONTROLLER.pauseForConfig(minecraft);
        candidates=QuickSetup.candidates(minecraft,villager);
        selectedStation=candidates.size()==1?0:-1;
        if(selectedStation==0 && !locked()) QuickSetup.confirm(minecraft,villager,candidates.getFirst());
        for(var widget:Screens.getWidgets(merchant)){previousVisibility.put(widget,widget.visible);widget.visible=false;}
        open=true;init(merchant.width,merchant.height);
        if(selected==null)setFocused(search);
        merchant.setFocused(getFocused());
    }
    public void hide(){
        Screens.getWidgets(merchant).removeAll(attached);attached.clear();
        previousVisibility.forEach((w,v)->w.visible=v);previousVisibility.clear();
        merchant.setFocused(null);open=false;
    }
    @Override public void onClose(){hide();}
    @Override public boolean isPauseScreen(){return false;}
    private boolean locked(){return merchant.getMenu().getTraderXp()>0 || villager.getVillagerData().level()!=1;}
    @Override protected void init(){
        Screens.getWidgets(merchant).removeAll(attached);attached.clear();
        int inner=width-32;
        button("merchant.station",width-124,30,108,()->{
            candidates=QuickSetup.candidates(minecraft,villager);
            selectedStation=candidates.isEmpty()?-1:(selectedStation+1)%candidates.size();
            if(selectedStation>=0)QuickSetup.preview(villager,candidates.get(selectedStation).pos());
        });
        if(selected==null){
            choices=station==Workstation.LECTERN?TradeCatalog.enchantments().stream().map(id->new Choice(SearchNames.name(id,true),SearchNames.item("minecraft:enchanted_book"),new RerollerConfig.EnchantTarget(id,1,64))).toList()
                :TradeCatalog.forStation(station).stream().map(e->new Choice(e.label(),SearchNames.item(e.item()),e.target())).toList();
            search=addRenderableWidget(new EditBox(font,16,72,inner,20,Text.tr("picker.search")));
            search.setMaxLength(128);search.setValue(query);search.setHint(Text.tr("picker.search"));
            search.setResponder(s->{query=s;page=0;pending=this::refreshRows;});
            refreshRows();setFocused(search);merchant.setFocused(search);
        }else{
            var target=selected.target();
            quantity=field(16,104,(inner-8)/2,String.valueOf(target.kind==SearchTarget.Kind.ENCHANTMENT?target.minLevel:target.minQuantity));
            price=field(20+inner/2,104,(inner-8)/2,String.valueOf(target.maxEmeraldCost));
            if(!target.secondPaymentItemId.isEmpty())secondPrice=field(16,142,inner,String.valueOf(target.maxSecondPayment));else secondPrice=null;
            button("merchant.add",16,height-54,(inner-8)/2,()->save(false));
            button("merchant.start",20+inner/2,height-54,(inner-8)/2,()->save(true)).active=!locked();
            button("picker.previous",16,height-28,(inner-8)/2,()->{selected=null;message="";rebuildWidgets();});
            button("merchant.back",20+inner/2,height-28,(inner-8)/2,this::hide);
        }
        syncWidgets();
    }
    private EditBox field(int x,int y,int w,String value){var box=addRenderableWidget(new EditBox(font,x,y,w,20,Component.empty()));box.setMaxLength(4);box.setValue(value);return box;}
    private final List<Button> rows=new ArrayList<>();
    private Button previous,next,back;
    private void refreshRows(){
        rows.forEach(this::removeWidget);rows.clear();
        if(previous!=null){removeWidget(previous);removeWidget(next);removeWidget(back);}
        var q=query.trim().toLowerCase(Locale.ROOT);
        visible=choices.stream().filter(c->c.name().toLowerCase(Locale.ROOT).contains(q)||c.target().targetId().contains(q)||c.target().paymentItemId.contains(q)).toList();
        int count=Math.max(1,(height-132)/22),start=page*count;
        if(start>=visible.size()&&page>0){page=0;start=0;}
        for(int i=start;i<Math.min(visible.size(),start+count);i++){
            var choice=visible.get(i);
            rows.add(addRenderableWidget(Button.builder(Component.literal(font.plainSubstrByWidth(choice.name(),width-68)),b->pending=()->{selected=choice;message="";rebuildWidgets();}).bounds(36,98+(i-start)*22,width-52,20).build()));
        }
        previous=button("picker.previous",16,height-28,65,()->{page--;refreshRows();});previous.active=page>0;
        next=button("picker.next",85,height-28,65,()->{page++;refreshRows();});next.active=start+count<visible.size();
        back=button("merchant.back",154,height-28,width-170,this::hide);
        syncWidgets();
    }
    private void syncWidgets(){
        Screens.getWidgets(merchant).removeAll(attached);attached.clear();
        for(var child:children())if(child instanceof AbstractWidget widget)attached.add(widget);
        Screens.getWidgets(merchant).addAll(attached);
    }
    private Button button(String key,int x,int y,int w,Runnable action){return addRenderableWidget(Button.builder(Text.tr(key),b->pending=action).bounds(x,y,w,20).build());}
    private void save(boolean start){
        try{
            int amount=Integer.parseInt(quantity.getValue()),cost=Integer.parseInt(price.getValue());
            int second=secondPrice==null?64:Integer.parseInt(secondPrice.getValue());
            if(amount<1||amount>9999||cost<1||cost>9999||second<1||second>9999)throw new NumberFormatException();
            var target=WishlistStore.criteria(selected.target());
            if(target.kind==SearchTarget.Kind.ENCHANTMENT){
                int max=minecraft.level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).get(net.minecraft.resources.Identifier.parse(target.enchantmentId)).orElseThrow().value().getMaxLevel();
                if(amount>max){message=Text.str("merchant.level_limit",max);return;}
                target.minLevel=amount;
            }else target.minQuantity=amount;
            target.maxEmeraldCost=cost;target.maxSecondPayment=second;target.workstation=station.id;
            if(start){
                if(locked()){message=Text.str("merchant.locked");return;}
                if(selectedStation<0 || selectedStation>=candidates.size() || !QuickSetup.confirm(minecraft,villager,candidates.get(selectedStation))){message=Text.str("merchant.choose_station");return;}
            }
            RerollerConfig.get().targets.add(target);RerollerConfig.save();
            if(start){hide();VillagerRerollClient.CONTROLLER.resume(minecraft);}
            else{selected=null;message=Text.str("merchant.added");rebuildWidgets();}
        }catch(NumberFormatException e){message=Text.str("merchant.numbers");}
    }
    @Override public void tick(){
        if(pending!=null){var action=pending;pending=null;action.run();}
        if(!open)return;
        if(minecraft.player==null || minecraft.level==null || minecraft.player.containerMenu!=merchant.getMenu() || !villager.isAlive()){hide();return;}
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float dt){
        g.fill(8,8,width-8,height-4,0xFFF0EDE5);g.fill(10,10,width-10,height-6,0xFF202020);
        g.text(font,title,16,16,0xFFFFCC66);
        String position=selectedStation<0?Text.str("merchant.unset"):candidates.get(selectedStation).pos().toShortString();
        g.text(font,font.plainSubstrByWidth(station.block.getName().getString()+" · "+position,width-152),16,34,0xFFFFFFFF);
        String status=locked()?Text.str("merchant.locked"):message.isEmpty()?Text.str(candidates.isEmpty()?"merchant.missing":candidates.size()>1?"merchant.ambiguous":"merchant.novice"):message;
        g.text(font,font.plainSubstrByWidth(status,width-32),16,56,0xFFFFCC66);
        if(selected==null){
            int count=Math.max(1,(height-132)/22),start=page*count;
            for(int i=start;i<Math.min(visible.size(),start+count);i++)g.item(visible.get(i).icon(),17,100+(i-start)*22);
            if(visible.isEmpty())g.text(font,Text.tr("picker.empty"),16,101,0xFFFFFFFF);
        }else{
            g.item(selected.icon(),16,72);g.text(font,font.plainSubstrByWidth(selected.name(),width-58),38,76,0xFFFFFFFF);
            g.text(font,Text.tr(selected.target().kind==SearchTarget.Kind.ENCHANTMENT?"editbox.min_level":"search.quantity"),16,94,0xFFFFFFFF);
            g.text(font,Text.tr("search.max_price"),20+(width-32)/2,94,0xFFFFFFFF);
            if(secondPrice!=null)g.text(font,Text.tr("search.second_max"),16,132,0xFFFFFFFF);
            String payment=SearchNames.name(selected.target().paymentItemId,false);
            if(selected.target().kind==SearchTarget.Kind.ENCHANTMENT)payment=SearchNames.name("minecraft:emerald",false);
            g.text(font,font.plainSubstrByWidth(Text.str("merchant.payment",payment),width-32),16,height-68,0xFFBBBBBB);
        }
        for(var child:children())if(child instanceof AbstractWidget widget)widget.extractRenderState(g,mx,my,dt);
    }
}
