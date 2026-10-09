package com.joplayx.client.config;

/** Persisted search criteria and results. No game dependencies, so matching can be tested independently. */
public class SearchTarget {
    public enum Kind { ENCHANTMENT, ITEM }
    public enum Status { ACTIVE, FOUND, ACQUIRED, IGNORED }
    public Kind kind = Kind.ENCHANTMENT;
    public Status status = Status.ACTIVE;
    public String enchantmentId = "";
    public int minLevel = 1;
    // Kept for compatibility with existing config files; for item searches this is the primary input limit.
    public int maxEmeraldCost = 64;
    public String itemId = "minecraft:emerald";
    public String paymentItemId = "minecraft:emerald";
    public int minQuantity = 1;
    public String secondPaymentItemId = "";
    public int maxSecondPayment = 64;
    public int bestPrice = -1;
    public int vanillaBaseMinimum = -1;
    public int bestQuantity = 1;
    public int bestLevel = 0;
    public int bestSecondPrice = 0;
    public String bestDescription = "";

    public String targetId() { return kind == Kind.ITEM ? itemId : enchantmentId; }
    public boolean active() { return status == Status.ACTIVE; }
    public boolean better(int price, int quantity, int level, int secondPrice) {
        if (bestPrice < 0) return true;
        // Never trade away enchantment quality or increase the secondary cost for a purported improvement.
        return level >= bestLevel && secondPrice <= bestSecondPrice
                && (long) price * bestQuantity < (long) bestPrice * quantity;
    }
    public void record(int price, int quantity, int level, int secondPrice, String description) {
        bestPrice = price;
        bestQuantity = quantity;
        bestLevel = level;
        bestSecondPrice = secondPrice;
        bestDescription = description;
        status = Status.FOUND;
    }
    public void reset() {
        bestPrice = -1;
        vanillaBaseMinimum = -1;
        bestQuantity = 1;
        bestLevel = 0;
        bestSecondPrice = 0;
        bestDescription = "";
        status = Status.ACTIVE;
    }
    public void normalize() {
        if (kind == null) kind = Kind.ENCHANTMENT;
        if (status == null) status = Status.ACTIVE;
        if (enchantmentId == null) enchantmentId = "";
        if (itemId == null) itemId = "minecraft:emerald";
        if (paymentItemId == null) paymentItemId = "minecraft:emerald";
        if (secondPaymentItemId == null) secondPaymentItemId = "";
        if (bestDescription == null) bestDescription = "";
        minLevel = Math.max(1, minLevel);
        minQuantity = Math.max(1, minQuantity);
        maxEmeraldCost = Math.max(1, maxEmeraldCost);
        maxSecondPayment = Math.max(1, maxSecondPayment);
        bestQuantity = Math.max(1, bestQuantity);
    }
}
