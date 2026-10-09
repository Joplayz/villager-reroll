package com.joplayx.client.util;

import com.joplayx.client.config.SearchTarget;
import java.util.Map;

public final class TradeMatcher {
    private TradeMatcher() {}
    public record Offer(String item, int quantity, String payment, int price,
                        String secondPayment, int secondPrice, Map<String, Integer> enchantments) {}

    public static boolean matches(SearchTarget target, Offer offer) {
        if (!target.active() || offer.price() > target.maxEmeraldCost) return false;
        int level = 0;
        if (target.kind == SearchTarget.Kind.ENCHANTMENT) {
            level = offer.enchantments().getOrDefault(target.enchantmentId, 0);
            if (!offer.item().equals("minecraft:enchanted_book") || !offer.payment().equals("minecraft:emerald")
                    || level < target.minLevel) return false;
            if (offer.secondPrice() > 0 && !offer.secondPayment().equals("minecraft:book")) return false;
        } else {
            if (!offer.item().equals(target.itemId) || !offer.payment().equals(target.paymentItemId)
                    || offer.quantity() < target.minQuantity) return false;
            if (target.secondPaymentItemId.isBlank()) {
                if (offer.secondPrice() != 0) return false;
            } else if (!offer.secondPayment().equals(target.secondPaymentItemId)
                    || offer.secondPrice() > target.maxSecondPayment) return false;
        }
        return target.better(offer.price(), offer.quantity(), level, offer.secondPrice());
    }
}
