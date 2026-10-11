package com.joplayx.client.config;

import com.google.gson.*;
import java.util.*;

/** Portable criteria only: results, player location and villager identities never enter exports. */
public final class WishlistStore {
    private static final Gson GSON=new GsonBuilder().setPrettyPrinting().create();
    public static class Wishlist {
        public int format=1;
        public String name="";
        public String workstation="lectern";
        public List<RerollerConfig.EnchantTarget> targets=new ArrayList<>();
    }
    public static RerollerConfig.EnchantTarget criteria(SearchTarget source) {
        var t=new RerollerConfig.EnchantTarget();
        t.workstation=source.workstation; t.kind=source.kind; t.enchantmentId=source.enchantmentId; t.minLevel=source.minLevel;
        t.maxEmeraldCost=source.maxEmeraldCost; t.itemId=source.itemId; t.paymentItemId=source.paymentItemId;
        t.minQuantity=source.minQuantity; t.secondPaymentItemId=source.secondPaymentItemId;
        t.maxSecondPayment=source.maxSecondPayment; t.normalize(); return t;
    }
    public static Wishlist capture(String name,String workstation,List<? extends SearchTarget> targets) {
        var p=new Wishlist();p.name=cleanName(name);p.workstation=workstation;
        for(var t:targets) if(!t.targetId().isBlank()) p.targets.add(criteria(t));
        if(p.targets.isEmpty()||p.targets.size()>100) throw new IllegalArgumentException("targets");
        return p;
    }
    private static String cleanName(String name) {
        if(name==null||name.isBlank()||name.length()>48||name.chars().anyMatch(Character::isISOControl))
            throw new IllegalArgumentException("name");
        return name.trim();
    }
    public static String export(Wishlist p) { return GSON.toJson(capture(p.name,p.workstation,p.targets)); }
    public static Wishlist parse(String json) {
        if(json==null||json.length()>65536) throw new IllegalArgumentException("size");
        try {
            var p=GSON.fromJson(json,Wishlist.class);
            if(p==null||p.format!=1||p.targets==null||p.targets.size()>100||p.targets.stream().anyMatch(Objects::isNull))
                throw new IllegalArgumentException("format");
            if(!Set.of("lectern","composter","barrel","fletching_table","loom","blast_furnace","smithing_table",
                    "grindstone","brewing_stand","cauldron","stonecutter","cartography_table","smoker").contains(p.workstation))
                throw new IllegalArgumentException("station");
            for(var t:p.targets) {
                t.normalize();
                if(!validId(t.targetId())||!validId(t.paymentItemId)||(!t.secondPaymentItemId.isEmpty()&&!validId(t.secondPaymentItemId))
                        ||t.minLevel>255||t.minQuantity>9999||t.maxEmeraldCost>9999||t.maxSecondPayment>9999)
                    throw new IllegalArgumentException("criteria");
            }
            return capture(p.name,p.workstation,p.targets);
        } catch(JsonParseException|NullPointerException e) { throw new IllegalArgumentException("format",e); }
    }
    private static boolean validId(String id) { return id!=null&&id.length()<=256&&id.matches("[a-z0-9_.-]+:[a-z0-9/._-]+"); }
    public static List<Wishlist> starters() {
        var armor=capture("Armor enchantments","lectern",List.of(book("protection",4),book("unbreaking",3),book("mending",1),book("feather_falling",4)));
        var tools=capture("Tool enchantments","lectern",List.of(book("efficiency",5),book("unbreaking",3),book("mending",1),book("fortune",3)));
        var sticks=new RerollerConfig.EnchantTarget(); sticks.kind=SearchTarget.Kind.ITEM; sticks.paymentItemId="minecraft:stick";sticks.maxEmeraldCost=32;
        return List.of(armor,tools,capture("Emeralds for sticks","fletching_table",List.of(sticks)));
    }
    private static RerollerConfig.EnchantTarget book(String id,int level) { return new RerollerConfig.EnchantTarget("minecraft:"+id,level,64); }
}
