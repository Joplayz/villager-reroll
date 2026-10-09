package com.joplayx.client.util;
import com.joplayx.client.config.RerollerConfig;
import com.joplayx.client.config.SearchTarget;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.trading.MerchantOffers;
import java.util.*;
public final class TradeUtil {
    public record TargetStatus(RerollerConfig.EnchantTarget target, boolean satisfied, String description) {}
    public record Match(RerollerConfig.EnchantTarget target, TradeMatcher.Offer offer, String description) {}
    public record TradeResult(List<Match> matches, List<TargetStatus> statuses, String lastOfferSeen) {
        public boolean anyFound() { return !matches.isEmpty(); }
    }
    public static TradeResult checkTrades(RerollerConfig.Config cfg, MerchantOffers offers) {
        List<TradeMatcher.Offer> seen = new ArrayList<>();
        for (var offer : offers) {
            if (offer.isOutOfStock()) continue;
            var result = offer.getResult();
            Map<String, Integer> enchantments = new HashMap<>();
            for (var entry : EnchantmentHelper.getEnchantmentsForCrafting(result).entrySet()) {
                entry.getKey().unwrapKey().ifPresent(key -> enchantments.put(key.identifier().toString(), entry.getIntValue()));
            }
            seen.add(new TradeMatcher.Offer(id(result), result.getCount(), id(offer.getCostA()),
                    offer.getCostA().getCount(), id(offer.getCostB()), offer.getCostB().getCount(), enchantments));
        }
        List<Match> matches = new ArrayList<>();
        List<TargetStatus> statuses = new ArrayList<>();
        for (var target : cfg.targets) {
            TradeMatcher.Offer best = null;
            for (var offer : seen) {
                if (TradeMatcher.matches(target, offer) && (best == null
                        || (long) offer.price() * best.quantity() < (long) best.price() * offer.quantity())) best = offer;
            }
            String description = best == null ? Text.str("trade.not_offered") : describe(best, target);
            statuses.add(new TargetStatus(target, best != null, description));
            if (best != null) matches.add(new Match(target, best, description));
        }
        return new TradeResult(matches, statuses, seen.isEmpty() ? Text.str("trade.no_trades")
                : describe(seen.getLast(), null));
    }
    /** Reference floor for vanilla book trades; server datapacks and discounts may change actual prices. */
    public static int vanillaBaseMinimum(SearchTarget t) {
        var mc=net.minecraft.client.Minecraft.getInstance();
        var id=net.minecraft.resources.Identifier.tryParse(t.enchantmentId);
        if(t.kind!=SearchTarget.Kind.ENCHANTMENT || id==null || !id.getNamespace().equals("minecraft") || mc.level==null) return -1;
        var holder=mc.level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).get(id);
        if(holder.isEmpty()) return -1;
        int multiplier=holder.get().is(net.minecraft.tags.EnchantmentTags.DOUBLE_TRADE_PRICE)?2:1;
        return Math.min(64,(2+3*t.bestLevel)*multiplier);
    }
    private static String id(ItemStack stack) {
        return stack.isEmpty() ? "" : BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }
    private static String describe(TradeMatcher.Offer o, SearchTarget target) {
        String result = o.item();
        if (target != null && target.kind == SearchTarget.Kind.ENCHANTMENT)
            result = target.enchantmentId + " " + o.enchantments().getOrDefault(target.enchantmentId, 0);
        else if (!o.enchantments().isEmpty()) result += " " + o.enchantments();
        String second = o.secondPrice() == 0 ? "" : " + " + o.secondPrice() + " " + o.secondPayment();
        return Text.str("trade.item_description", o.quantity(), result, o.price(), o.payment(), second);
    }
}
