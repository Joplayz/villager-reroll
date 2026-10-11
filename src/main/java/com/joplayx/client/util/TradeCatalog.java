package com.joplayx.client.util;

import com.google.gson.Gson;
import com.joplayx.client.config.*;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.EnchantmentTags;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Vanilla novice suggestions. Custom servers may differ; the editor remains unrestricted. */
public final class TradeCatalog {
    public record Entry(String profession,String item,String payment,int cost,int quantity,String second,int secondCost) {
        public RerollerConfig.EnchantTarget target() {
            var t=new RerollerConfig.EnchantTarget(); t.kind=SearchTarget.Kind.ITEM;
            t.itemId=item; t.paymentItemId=payment; t.maxEmeraldCost=cost;
            t.minQuantity=quantity; t.secondPaymentItemId=second; t.maxSecondPayment=Math.max(1,secondCost); return t;
        }
        public String label() {
            return cost+" "+SearchNames.name(payment,false)+(second.isEmpty()?"":" + "+secondCost+" "+SearchNames.name(second,false))
                    +" → "+quantity+" "+SearchNames.name(item,false);
        }
    }
    private static List<Entry> entries;
    public static List<Entry> forStation(Workstation station) {
        if(entries==null) {
            try(var in=TradeCatalog.class.getResourceAsStream("/assets/villager-reroll/novice-trades.json")) {
                if(in==null) throw new IOException("Missing novice catalog");
                entries=List.of(new Gson().fromJson(new InputStreamReader(in,StandardCharsets.UTF_8),Entry[].class));
            } catch(IOException|RuntimeException e) { entries=List.of(); }
        }
        return entries.stream().filter(e->e.profession().equals(station.profession)).toList();
    }
    public static List<String> enchantments() {
        var mc=Minecraft.getInstance();
        if(mc.level==null) return TargetEditScreen.VANILLA_ENCHANTMENTS.stream().filter(s->!Set.of("soul_speed","swift_sneak","wind_burst").contains(s)).map(s->"minecraft:"+s).toList();
        return mc.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).listElements()
                .filter(e->e.is(EnchantmentTags.TRADEABLE)).map(e->e.key().identifier().toString()).sorted().toList();
    }
}
