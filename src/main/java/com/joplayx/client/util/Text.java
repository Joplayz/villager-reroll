package com.joplayx.client.util;

import net.minecraft.network.chat.Component;

public final class Text {
    private Text() {}
    public static Component tr(String key, Object... args) {
        return Component.translatable("villager-reroll." + key, args);
    }
    public static String str(String key, Object... args) { return tr(key, args).getString(); }
}
