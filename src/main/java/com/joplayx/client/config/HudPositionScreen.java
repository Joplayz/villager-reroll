package com.joplayx.client.config;

import com.joplayx.client.VillagerRerollClient;
import com.mojang.blaze3d.platform.InputConstants;
import com.joplayx.client.hud.RerollerHud;
import com.joplayx.client.util.Text;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import java.util.List;

/** Draft-only positioning: Done saves, Escape and Cancel leave the previous position intact. */
public class HudPositionScreen extends Screen {
    private final Screen parent;
    private double px, py, grabX, grabY;
    private boolean dragging;
    public HudPositionScreen(Screen parent) {
        super(Text.tr("hud.move"));
        this.parent = parent;
        px = RerollerConfig.get().hudX; py = RerollerConfig.get().hudY;
    }
    @Override protected void init() {
        dragging = false;
        VillagerRerollClient.CONTROLLER.pauseForConfig(minecraft);
        int w = Math.min(100, (width - 24) / 3), left = (width - 3 * w - 8) / 2;
        addRenderableWidget(Button.builder(Text.tr("button.done"), b -> {
            RerollerConfig.get().hudX = px; RerollerConfig.get().hudY = py;
            RerollerConfig.save(); onClose();
        }).bounds(left, height - 28, w, 20).build());
        addRenderableWidget(Button.builder(Text.tr("hud.reset_position"), b -> { px = 0; py = 0; })
                .bounds(left + w + 4, height - 28, w, 20).build());
        addRenderableWidget(Button.builder(Text.tr("button.cancel"), b -> onClose())
                .bounds(left + 2 * w + 8, height - 28, w, 20).build());
    }
    @Override public void onClose() { minecraft.setScreen(parent); }
    @Override public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        // Buttons take priority when the preview is dragged over the footer.
        if (super.mouseClicked(event, doubleClick)) return true;
        int x = RerollerHud.position(px, width, RerollerHud.panelWidth(width));
        int y = RerollerHud.position(py, height, RerollerHud.HEIGHT);
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT && event.x() >= x && event.x() < x + RerollerHud.panelWidth(width)
                && event.y() >= y && event.y() < y + RerollerHud.HEIGHT) {
            dragging = true; grabX = event.x() - x; grabY = event.y() - y; return true;
        }
        return false;
    }
    @Override public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (!dragging || event.button() != InputConstants.MOUSE_BUTTON_LEFT) return super.mouseDragged(event, dx, dy);
        px = Math.clamp((event.x() - grabX - 6) / Math.max(1, width - RerollerHud.panelWidth(width) - 12), 0, 1);
        py = Math.clamp((event.y() - grabY - 6) / Math.max(1, height - RerollerHud.HEIGHT - 12), 0, 1);
        return true;
    }
    @Override public boolean mouseReleased(MouseButtonEvent event) {
        if (dragging && event.button() == InputConstants.MOUSE_BUTTON_LEFT) { dragging = false; return true; }
        return super.mouseReleased(event);
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float dt) {
        g.fill(0, 0, width, height, 0x88101418);
        RerollerHud.drawPanel(g, List.of(Text.str("hud.header"), Text.str("hud.running"),
                Text.str("hud.stats", 12, "0:45"), Text.str("hud.pause_controls",
                VillagerRerollClient.startStopKey.getTranslatedKeyMessage().getString(),
                VillagerRerollClient.openConfigKey.getTranslatedKeyMessage().getString())), px, py, 0xFF9DBBCB);
        boolean nearFooter = RerollerHud.position(py, height, RerollerHud.HEIGHT)
                + RerollerHud.HEIGHT > height - 52;
        int buttonY = nearFooter ? 8 : height - 28;
        int hintY = nearFooter ? 34 : height - 44;
        String hint = Text.str("hud.drag_hint");
        g.text(font, font.plainSubstrByWidth(hint, width - 16), 8, hintY, 0xFFFFFFFF);
        // Draw controls last so the preview cannot obscure them.
        for (var child : children()) if (child instanceof Button b) { b.setY(buttonY); b.extractRenderState(g, mx, my, dt); }
    }
}
