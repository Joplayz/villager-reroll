package com.joplayx.client.hud;

import com.joplayx.client.VillagerRerollClient;
import com.joplayx.client.config.RerollerConfig;
import com.joplayx.client.state.RerollState;
import com.joplayx.client.util.Text;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.List;
import java.util.Locale;

public class RerollerHud {
    public static void extractRenderState(GuiGraphicsExtractor g, DeltaTracker delta) {
        var cfg = RerollerConfig.get();
        var controller = VillagerRerollClient.CONTROLLER;
        var mc = Minecraft.getInstance();
        var state = controller.getState();
        if (!cfg.hudEnabled || state == RerollState.IDLE || mc.options.hideGui
                || mc.screen instanceof com.joplayx.client.config.HudPositionScreen) return;

        boolean found = state == RerollState.FOUND;
        boolean paused = state == RerollState.PAUSED || state == RerollState.ERROR;
        String status = Text.str(found ? "hud.found" : paused ? "hud.paused" : "hud.running");
        long seconds = controller.getElapsedSeconds();
        String time = String.format(Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60);
        String settingsKey = VillagerRerollClient.openConfigKey.getTranslatedKeyMessage().getString();
        String toggleKey = VillagerRerollClient.startStopKey.getTranslatedKeyMessage().getString();
        String controls = found ? Text.str("hud.review", settingsKey)
                : Text.str(paused ? "hud.resume_controls" : "hud.pause_controls", toggleKey, settingsKey);
        List<String> lines = new java.util.ArrayList<>(List.of(Text.str("hud.header"), status,
                Text.str("hud.stats", controller.getAttempts(), time), controls));
        if(cfg.hudDetails) lines.add(3,details(found));

        drawPanel(g, lines, cfg.hudX, cfg.hudY, found ? 0xFF8DD99B : paused ? 0xFFE7C67A : 0xFF9DBBCB);
    }

    public static String details(boolean found) {
        var targets=RerollerConfig.get().targets.stream().filter(t->found || t.appliesTo(RerollerConfig.get().workstation)).filter(t->found?t.status==com.joplayx.client.config.SearchTarget.Status.FOUND:t.active()).toList();
        if(targets.size()!=1)return Text.str("hud.search_count",targets.size());
        var t=targets.getFirst();String price=String.valueOf(found?t.bestPrice:t.maxEmeraldCost);
        String label=found?com.joplayx.client.util.SearchNames.found(t):com.joplayx.client.util.SearchNames.target(t);
        String payment=com.joplayx.client.util.SearchNames.name(t.kind==com.joplayx.client.config.SearchTarget.Kind.ENCHANTMENT?"minecraft:emerald":t.paymentItemId,false);
        return Text.str(found?"hud.detail_found":"hud.detail_search",label,price,payment);
    }
    public static int panelHeight(){return RerollerConfig.get().hudDetails?70:58;}
    public static final int HEIGHT = 58;
    public static int panelWidth(int screenWidth) { return Math.max(1, Math.min(200, screenWidth - 12)); }
    public static int position(double fraction, int screenSize, int panelSize) {
        if (!Double.isFinite(fraction)) fraction = 0;
        return 6 + (int)Math.round(Math.clamp(fraction, 0, 1) * Math.max(0, screenSize - panelSize - 12));
    }
    public static void drawPanel(GuiGraphicsExtractor g, List<String> lines, double px, double py, int accent) {
        var mc = Minecraft.getInstance();
        int width = panelWidth(g.guiWidth()), height = 10+lines.size()*12;
        int x = position(px, g.guiWidth(), width), y = position(py, g.guiHeight(), height);
        g.fill(x, y, x + width, y + 1, accent);
        g.fill(x, y + height - 1, x + width, y + height, accent);
        g.fill(x, y + 1, x + 1, y + height - 1, accent);
        g.fill(x + width - 1, y + 1, x + width, y + height - 1, accent);
        g.fill(x + 1, y + 1, x + width - 1, y + height - 1, 0xD0181C20);
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            int available = width - 16;
            if (mc.font.width(line) > available) {
                line = mc.font.plainSubstrByWidth(line, available - mc.font.width("...")) + "...";
            }
            int color = i == 1 ? accent : i == 3 ? 0xFFB8BDC5 : 0xFFF1F1F1;
            g.text(mc.font, line, x + 8, y + 6 + i * 12, color);
        }
    }
}
