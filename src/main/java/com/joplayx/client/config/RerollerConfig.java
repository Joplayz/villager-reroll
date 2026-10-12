package com.joplayx.client.config;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.GsonBuilder;
import com.joplayx.VillagerReroll;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class RerollerConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance()
            .getConfigDir().resolve("villager-reroll.json");

    private static Config instance = new Config();

    public static Config get() { return instance; }

    public static void load() {
        if (Files.exists(CONFIG_PATH)) {
            try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
                instance = GSON.fromJson(reader, Config.class);
                if (instance == null) instance = new Config();
                if (instance.targets == null) instance.targets = new ArrayList<>();
                instance.targets.removeIf(java.util.Objects::isNull);
                instance.targets.forEach(SearchTarget::normalize);
                instance.hudScale = Double.isFinite(instance.hudScale) ? Math.clamp(instance.hudScale, 0.5, 1.5) : 1.0;
                if (instance.workstation == null) instance.workstation = "lectern";
                if (instance.wishlists == null) instance.wishlists = new ArrayList<>();
                instance.wishlists.removeIf(p -> p == null || p.targets == null || p.name == null);
                VillagerReroll.LOGGER.info("[Reroller] Config loaded.");
            } catch (IOException | JsonParseException e) {
                VillagerReroll.LOGGER.error("[Reroller] Failed to load config.", e);
                instance = new Config();
            }
        } else {
            instance = new Config();
            save();
        }
    }

    public static void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
            GSON.toJson(instance, writer);
            }
            VillagerReroll.LOGGER.info("[Reroller] Config saved.");
        } catch (IOException e) {
            VillagerReroll.LOGGER.error("[Reroller] Failed to save config.", e);
        }
    }

    public static class Config {
        // Any active search may stop the reroller. Results remain until explicitly reset.
        public List<EnchantTarget> targets = new ArrayList<>();
        public String workstation = "lectern";
        public List<WishlistStore.Wishlist> wishlists = new ArrayList<>();
        public boolean foundSoundEnabled = true;
        public boolean foundToastEnabled = true;
        public boolean hudDetails = false;
        public double hudScale = 1.0;
        public boolean hudSnap = false;

        // Lectern position stored as separate ints so YACL can edit them directly
        // Integer.MIN_VALUE means "not set"
        public int lecternX = Integer.MIN_VALUE;
        public int lecternY = Integer.MIN_VALUE;
        public int lecternZ = Integer.MIN_VALUE;

        // Timing
        public int closeDelayTicks = 20;
        public int retryDelayTicks = 40;

        // Display
        public boolean hudEnabled = true;
        // Relative position within the available screen space, retained across GUI scales.
        public double hudX = 0;
        public double hudY = 0;

        /**
         * Returns the lectern BlockPos, or null if not set.
         */
        public BlockPos lecternPos() {
            if (lecternX == Integer.MIN_VALUE) return null;
            return new BlockPos(lecternX, lecternY, lecternZ);
        }

        /**
         * Returns true if the lectern position has been set.
         */
        public boolean hasLecternPos() {
            return lecternX != Integer.MIN_VALUE;
        }

        /**
         * Sets the lectern position from a BlockPos.
         */
        public void setLecternPos(BlockPos pos) {
            lecternX = pos.getX();
            lecternY = pos.getY();
            lecternZ = pos.getZ();
        }

        /**
         * Clears the lectern position.
         */
        public void clearLecternPos() {
            lecternX = Integer.MIN_VALUE;
            lecternY = Integer.MIN_VALUE;
            lecternZ = Integer.MIN_VALUE;
        }

        /**
         * Returns a human-readable string of the lectern position.
         */
        public String lecternPosString() {
            if (!hasLecternPos()) return com.joplayx.client.util.Text.str("button.clear_position.not_set");
            return "X: " + lecternX + "  Y: " + lecternY + "  Z: " + lecternZ;
        }
    }

    /**
     * One enchantment the reroller should look for, with its own level/price criteria.
     * A trade must meet ALL THREE (enchantment id, minLevel, maxEmeraldCost) to count
     * as satisfying this target.
     */
    public static class EnchantTarget extends SearchTarget {

        public EnchantTarget() {
        }

        public EnchantTarget(String enchantmentId, int minLevel, int maxEmeraldCost) {
            this.enchantmentId = enchantmentId;
            this.minLevel = minLevel;
            this.maxEmeraldCost = maxEmeraldCost;
        }
    }
}
