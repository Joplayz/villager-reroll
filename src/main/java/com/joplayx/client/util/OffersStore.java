package com.joplayx.client.util;
import net.minecraft.world.item.trading.MerchantOffers;
/** Applied after vanilla handles the packet on the client thread, tied to its menu. */
public final class OffersStore {
    private static MerchantOffers latestOffers;
    private static int containerId = -1;
    private static int villagerXp;
    public static void set(int id, MerchantOffers offers, int xp) {
        containerId = id; latestOffers = offers; villagerXp = xp;
    }
    public static MerchantOffers get(int id) { return id == containerId ? latestOffers : null; }
    public static int villagerXp() { return villagerXp; }
    public static void clear() { latestOffers = null; containerId = -1; villagerXp = 0; }
}
