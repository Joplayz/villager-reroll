package com.joplayx.client.config;

import com.joplayx.client.util.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.*;
import net.minecraft.resources.Identifier;
import java.util.*;

/** Text responders only update suggestion data. They never mutate the screen's widget collection. */
public class TargetEditScreen extends Screen {
    private final Screen parent;
    private final RerollerConfig.EnchantTarget target;
    private EditBox id, level, price, payment, second, secondPrice;
    private final List<Label> labels = new ArrayList<>();
    private List<String> ids = List.of(), suggestions = List.of();
    private EditBox suggestionOwner;
    private int dropdownX, dropdownY, dropdownW;
    private String error = "";
    private Runnable pending;
    private boolean initialized;
    private record Label(String key, int x, int y) {}
    public static final List<String> VANILLA_ENCHANTMENTS = List.of(
        "aqua_affinity", "bane_of_arthropods", "binding_curse", "blast_protection", "breach",
        "channeling", "density", "depth_strider", "efficiency", "feather_falling", "fire_aspect",
        "fire_protection", "flame", "fortune", "frost_walker", "impaling", "infinity", "knockback",
        "looting", "loyalty", "luck_of_the_sea", "lure", "lunge", "mending", "multishot", "piercing",
        "power", "projectile_protection", "protection", "punch", "quick_charge", "respiration",
        "riptide", "sharpness", "silk_touch", "smite", "soul_speed", "sweeping_edge", "swift_sneak",
        "thorns", "unbreaking", "vanishing_curse", "wind_burst");
    public TargetEditScreen(Screen parent, RerollerConfig.EnchantTarget target) {
        super(Text.tr(target.kind == SearchTarget.Kind.ENCHANTMENT ? "button.add_enchantment" : "button.add_item"));
        this.parent=parent; this.target=target;
    }
    @Override protected void init() {
        // Preserve drafts through GUI resizes without committing partial text to the config.
        String oldId=initialized?id.getValue():target.targetId();
        String oldLevel=initialized?level.getValue():String.valueOf(target.kind==SearchTarget.Kind.ITEM?target.minQuantity:target.minLevel);
        String oldPrice=initialized?price.getValue():String.valueOf(target.maxEmeraldCost);
        String oldPay=payment!=null?payment.getValue():target.paymentItemId;
        String oldSecond=second!=null?second.getValue():target.secondPaymentItemId;
        String oldSecondPrice=secondPrice!=null?secondPrice.getValue():String.valueOf(target.maxSecondPayment);
        labels.clear(); suggestions=List.of(); suggestionOwner=null;
        int col=(width-24)/2;
        id=field(target.kind==SearchTarget.Kind.ENCHANTMENT?"editbox.enchantment_id":"search.output",8,32,width-16,oldId);
        level=field(target.kind==SearchTarget.Kind.ENCHANTMENT?"editbox.min_level":"search.quantity",8,66,col,oldLevel);
        price=field("search.max_price",16+col,66,col,oldPrice);
        if(target.kind==SearchTarget.Kind.ITEM) {
            payment=field("search.payment",8,100,width-16,oldPay);
            second=field("search.second",8,134,col,oldSecond);
            secondPrice=field("search.second_max",16+col,134,col,oldSecondPrice);
        }
        ids=target.kind==SearchTarget.Kind.ITEM ? itemIds() : minecraft.level==null
                ? VANILLA_ENCHANTMENTS.stream().map(s->"minecraft:"+s).sorted().toList()
                : minecraft.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).listElements()
                    .map(ref->ref.key().identifier().toString()).sorted().toList();
        id.setResponder(s->suggest(id,s,ids));
        if(payment!=null) payment.setResponder(s->suggest(payment,s,itemIds()));
        if(second!=null) second.setResponder(s->suggest(second,s,itemIds()));
        int bw=(width-24)/3;
        button("search.continue",8,height-66,bw,()->finish(SearchTarget.Status.ACTIVE,false));
        button("search.acquired",12+bw,height-66,bw,()->finish(SearchTarget.Status.ACQUIRED,false));
        button("search.ignore",16+2*bw,height-66,bw,()->finish(SearchTarget.Status.IGNORED,false));
        button("search.reset",8,height-42,bw,()->finish(SearchTarget.Status.ACTIVE,true));
        button("button.done",12+bw,height-42,bw,()->{if(save()) minecraft.setScreen(parent);});
        button("button.cancel",16+2*bw,height-42,bw,()->minecraft.setScreen(parent));
        initialized=true;
    }
    private List<String> itemIds() { return BuiltInRegistries.ITEM.keySet().stream().map(Object::toString).sorted().toList(); }
    private EditBox field(String label,int x,int y,int w,String value) {
        labels.add(new Label(label,x,y-10));
        var box=new EditBox(font,x,y,w,20,Text.tr(label));
        box.setMaxLength(256); box.setValue(value);
        return addRenderableWidget(box);
    }
    private void button(String key,int x,int y,int w,Runnable action) {
        addRenderableWidget(Button.builder(Text.tr(key),b->pending=action).bounds(x,y,w,20).build());
    }
    private void finish(SearchTarget.Status status,boolean reset) {
        if(!save()) return;
        if(reset) target.reset(); else target.status=status;
        RerollerConfig.save(); minecraft.setScreen(parent);
    }
    private boolean save() {
        var identifier=Identifier.tryParse(SearchNames.resolve(id.getValue(),ids,target.kind==SearchTarget.Kind.ENCHANTMENT));
        try {
            if(identifier==null || !ids.contains(identifier.toString())) throw new IllegalArgumentException();
            int lv=positive(level.getValue()), cost=positive(price.getValue());
            String pay=target.paymentItemId, sec=target.secondPaymentItemId;
            int secMax=target.maxSecondPayment;
            if(target.kind==SearchTarget.Kind.ITEM) {
                var payId=Identifier.tryParse(SearchNames.resolve(payment.getValue(),itemIds(),false));
                if(payId==null || !BuiltInRegistries.ITEM.containsKey(payId)) throw new IllegalArgumentException();
                pay=payId.toString(); sec=SearchNames.resolve(second.getValue(),itemIds(),false);
                if(!sec.isEmpty()) {
                    var secId=Identifier.tryParse(sec);
                    if(secId==null || !BuiltInRegistries.ITEM.containsKey(secId)) throw new IllegalArgumentException();
                    sec=secId.toString();
                }
                secMax=positive(secondPrice.getValue());
            }
            boolean changed=!identifier.toString().equals(target.targetId()) || cost!=target.maxEmeraldCost
                || lv!=(target.kind==SearchTarget.Kind.ITEM?target.minQuantity:target.minLevel)
                || !pay.equals(target.paymentItemId) || !sec.equals(target.secondPaymentItemId) || secMax!=target.maxSecondPayment;
            if(changed) target.reset();
            if(target.kind==SearchTarget.Kind.ITEM) {target.itemId=identifier.toString();target.minQuantity=lv;}
            else {target.enchantmentId=identifier.toString();target.minLevel=lv;}
            target.maxEmeraldCost=cost;target.paymentItemId=pay;target.secondPaymentItemId=sec;target.maxSecondPayment=secMax;
            if(!RerollerConfig.get().targets.contains(target)) RerollerConfig.get().targets.add(target);
            RerollerConfig.save(); return true;
        } catch(IllegalArgumentException e) {error=Text.str("search.invalid");return false;}
    }
    private static int positive(String text) {
        int n=Integer.parseInt(text.trim()); if(n<1 || n>9999) throw new IllegalArgumentException(); return n;
    }
    private void suggest(EditBox owner,String value,List<String> candidates) {
        suggestionOwner=owner;
        String q=value.trim().toLowerCase(Locale.ROOT);
        // Empty and partial namespace input is valid while typing; no parsing or widget mutation here.
        suggestions=q.isEmpty() || candidates.contains(q) ? List.of() : candidates.stream().filter(s->SearchNames.matches(s,q,owner==id&&target.kind==SearchTarget.Kind.ENCHANTMENT)).limit(5).toList();
    }
    private void layoutSuggestions() {
        if(suggestionOwner==null || !suggestionOwner.isFocused()) {suggestions=List.of();return;}
        dropdownX=suggestionOwner.getX();dropdownW=suggestionOwner.getWidth();
        int count=Math.min(suggestions.size(),Math.max(0,(height-96)/20));
        suggestions=suggestions.subList(0,count);
        dropdownY=suggestionOwner.getY()+22;
        if(dropdownY+count*20>height-70) dropdownY=Math.max(18,suggestionOwner.getY()-count*20-2);
    }
    @Override public boolean mouseClicked(MouseButtonEvent e,boolean doubleClick) {
        layoutSuggestions();
        int index=(int)((e.y()-dropdownY)/20);
        if(e.button()==com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT && !suggestions.isEmpty()
                && e.x()>=dropdownX && e.x()<dropdownX+dropdownW && e.y()>=dropdownY
                && index>=0 && index<suggestions.size()) {
            String value=suggestions.get(index); suggestionOwner.setValue(value); suggestions=List.of();return true;
        }
        suggestions=List.of();
        return super.mouseClicked(e,doubleClick);
    }
    @Override public void tick() {if(pending!=null){var action=pending;pending=null;action.run();}}
    @Override public void onClose() {minecraft.setScreen(parent);}
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float dt) {
        super.extractRenderState(g,mx,my,dt);
        g.text(font,title,8,6,0xFFFFAA00);
        for(var label:labels)g.text(font,font.plainSubstrByWidth(Text.str(label.key()),(width-24)/2),label.x(),label.y(),0xFFAAAAAA);
        String result=target.bestPrice<0?Text.str("search.no_best"):Text.str("search.best",target.bestPrice,target.bestQuantity);
        if(target.kind==SearchTarget.Kind.ENCHANTMENT && target.bestPrice==1) result+=" | "+Text.str("search.lowest");
        if(target.kind==SearchTarget.Kind.ENCHANTMENT && target.vanillaBaseMinimum>0) {
            String key=target.bestPrice<=target.vanillaBaseMinimum?"search.base_reached":"search.base_minimum";
            g.text(font,font.plainSubstrByWidth(Text.str(key,target.vanillaBaseMinimum),width-16),8,100,0xFF88DD88);
            g.text(font,font.plainSubstrByWidth(Text.str("search.base_note"),width-16),8,114,0xFFAAAAAA);
        }
        g.text(font,font.plainSubstrByWidth(error.isEmpty()?result:error,width-16),8,height-78,error.isEmpty()?0xFFFFCC66:0xFFFF5555);
        g.text(font,font.plainSubstrByWidth(Text.str("search.help"),width-16),8,height-14,0xFFAAAAAA);
        layoutSuggestions();
        if(!suggestions.isEmpty()) {
            g.fill(dropdownX-1,dropdownY-1,dropdownX+dropdownW+1,dropdownY+suggestions.size()*20+1,0xFF222222);
            for(int i=0;i<suggestions.size();i++) {
                int y=dropdownY+i*20;
                if(mx>=dropdownX&&mx<dropdownX+dropdownW&&my>=y&&my<y+20)g.fill(dropdownX,y,dropdownX+dropdownW,y+20,0xFF445577);
                boolean enchantment=suggestionOwner==id&&target.kind==SearchTarget.Kind.ENCHANTMENT;
                var icon=enchantment?SearchNames.item("minecraft:enchanted_book"):SearchNames.item(suggestions.get(i));
                g.item(icon,dropdownX+2,y+2);
                g.text(font,font.plainSubstrByWidth(SearchNames.name(suggestions.get(i),enchantment),dropdownW-26),dropdownX+22,y+6,0xFFFFFFFF);
            }
        }
    }
}
