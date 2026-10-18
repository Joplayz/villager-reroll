package com.joplayx;
import com.google.gson.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import java.nio.file.*;
import java.util.*;
/** Collects the novice catalog from the running game's own trade factories. Test artifact only. */
public final class NoviceCatalogProbe {
    private static String id(ItemStack stack){return stack.isEmpty()?"":BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();}
    public static void run(ServerLevel level) {
        var rows=new TreeMap<String,JsonObject>();
        for(var profession:VillagerTrades.TRADES.entrySet()) {
            Object key=profession.getKey();
            String name=key instanceof net.minecraft.resources.ResourceKey<?> k?k.location().getPath():BuiltInRegistries.VILLAGER_PROFESSION.getKey((net.minecraft.world.entity.npc.VillagerProfession)key).getPath();
            var novice=profession.getValue().get(1);if(novice==null)continue;
            var v=new Villager(net.minecraft.world.entity.EntityType.VILLAGER,level);
            for(var listing:novice)for(int sample=0;sample<64;sample++) {
                var offer=listing.getOffer(v,net.minecraft.util.RandomSource.create(sample));
                if(offer==null||offer.getResult().is(Items.ENCHANTED_BOOK))continue;
                var row=new JsonObject();row.addProperty("profession",name);row.addProperty("item",id(offer.getResult()));
                row.addProperty("payment",id(offer.getCostA()));row.addProperty("cost",offer.getCostA().getCount());row.addProperty("quantity",offer.getResult().getCount());
                row.addProperty("second",id(offer.getCostB()));row.addProperty("secondCost",offer.getCostB().getCount());
                rows.put(row.toString(),row);
            }
        }
        if(rows.size()<35)throw new AssertionError("Incomplete novice catalog: "+rows.size());
        try { Files.writeString(Path.of("novice-trades-verified.json"),new GsonBuilder().setPrettyPrinting().create().toJson(rows.values())); }
        catch(java.io.IOException e){throw new RuntimeException(e);}
        VillagerReroll.LOGGER.info("[Catalog test] Extracted {} actual novice offers",rows.size());
    }
}
