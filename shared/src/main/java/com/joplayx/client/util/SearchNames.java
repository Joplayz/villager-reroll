package com.joplayx.client.util;

import com.joplayx.client.config.SearchTarget;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.*;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.*;
import java.util.*;

public final class SearchNames {
    public static ItemStack item(String id) {
        if(Minecraft.getInstance().level==null) return ItemStack.EMPTY;
        var key=Identifier.tryParse(id);
        return key!=null && BuiltInRegistries.ITEM.containsKey(key)
                ? new ItemStack(BuiltInRegistries.ITEM.getValue(key)) : ItemStack.EMPTY;
    }
    public static String name(String id, boolean enchantment) {
        var key=Identifier.tryParse(id);
        var mc=Minecraft.getInstance();
        if(enchantment && key!=null && mc.level!=null) {
            var entry=mc.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(key);
            if(entry.isPresent()) return entry.get().value().description().getString();
        }
        if(!enchantment && key!=null && BuiltInRegistries.ITEM.containsKey(key)) return net.minecraft.network.chat.Component.translatable(BuiltInRegistries.ITEM.getValue(key).getDescriptionId()).getString();
        String value=key==null?id:key.getPath();
        return Arrays.stream(value.split("_")).map(s->s.isEmpty()?s:Character.toUpperCase(s.charAt(0))+s.substring(1))
                .collect(java.util.stream.Collectors.joining(" "));
    }
    public static String target(SearchTarget t) {
        return name(t.targetId(),t.kind==SearchTarget.Kind.ENCHANTMENT)
                +(t.kind==SearchTarget.Kind.ENCHANTMENT?" "+level(t.minLevel):"");
    }
    public static String found(SearchTarget t) {
        return name(t.targetId(),t.kind==SearchTarget.Kind.ENCHANTMENT)
                +(t.kind==SearchTarget.Kind.ENCHANTMENT?" "+level(t.bestLevel):"");
    }
    private static String level(int level) {
        return net.minecraft.network.chat.Component.translatable("enchantment.level."+level).getString();
    }
    public static boolean matches(String id,String query,boolean enchantment) {
        String q=query.trim().toLowerCase(Locale.ROOT);
        return id.toLowerCase(Locale.ROOT).contains(q) || name(id,enchantment).toLowerCase(Locale.ROOT).contains(q);
    }
    public static String resolve(String input,List<String> ids,boolean enchantment) {
        var key=Identifier.tryParse(input.trim());
        if(key!=null && ids.contains(key.toString())) return key.toString();
        var matches=ids.stream().filter(s->name(s,enchantment).equalsIgnoreCase(input.trim())).toList();
        return matches.size()==1?matches.getFirst():input.trim();
    }
}
