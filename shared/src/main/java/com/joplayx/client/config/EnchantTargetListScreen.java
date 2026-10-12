package com.joplayx.client.config;

import com.joplayx.client.VillagerRerollClient;
import com.joplayx.client.util.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import java.util.*;

/** Scrollable summaries; editing lives on a separate screen so rows never overlap the footer. */
public class EnchantTargetListScreen extends Screen {
    private final Screen parent;
    private TargetList list;
    private double savedScroll;
    private Runnable pending;
    public EnchantTargetListScreen(Screen parent) { super(Text.tr("screen.title")); this.parent = parent; }
    @Override protected void init() {
        VillagerRerollClient.CONTROLLER.pauseForConfig(minecraft);
        list = addRenderableWidget(new TargetList(width, Math.max(28, height - 140), 44));
        for (var target : RerollerConfig.get().targets) list.add(target);
        list.setScrollAmount(savedScroll);
        int gap = 4, w = (width - 24 - gap * 2) / 3;
        addButton("button.add_enchantment", 8, height - 68, w, () -> add(SearchTarget.Kind.ENCHANTMENT))
                .active = Workstation.from(RerollerConfig.get().workstation) == Workstation.LECTERN;
        addButton("wishlists.title",8,height-92,w,()->minecraft.gui.setScreen(new WishlistsScreen(this)));
        addButton("results.title",12+w,height-92,w,()->minecraft.gui.setScreen(new FoundTradeScreen(this)))
                .active=RerollerConfig.get().targets.stream().anyMatch(t->t.bestPrice>=0);
        addButton("setup.title",16+2*w,height-92,w,()->{ minecraft.gui.setScreen(null); QuickSetup.fromCrosshair(minecraft); });
        addButton("button.add_item", 12 + w, height - 68, w, () -> add(SearchTarget.Kind.ITEM));
        addButton("button.more_settings_short", 16 + 2*w, height - 68, w,
                () -> minecraft.gui.setScreen(RerollerConfigScreen.create(this)));
        addButton("session.resume", 8, height - 44, w, () -> {
            minecraft.gui.setScreen(null); VillagerRerollClient.CONTROLLER.resume(minecraft);
        }).active = minecraft.player != null;
        addButton("session.new", 12 + w, height - 44, w, () -> {
            minecraft.gui.setScreen(null); VillagerRerollClient.CONTROLLER.newSession(minecraft);
        }).active = minecraft.player != null;
        addButton("button.done", 16 + 2*w, height - 44, w, this::onClose);
        addButton("hud.move", width - 100, 20, 92, () -> minecraft.gui.setScreen(new HudPositionScreen(this)));
        var station = Workstation.from(RerollerConfig.get().workstation);
        addRenderableWidget(Button.builder(Text.tr("workstation", station.block.getName()), b -> {
            var next = Workstation.values()[(station.ordinal() + 1) % Workstation.values().length];
            RerollerConfig.get().workstation = next.id;
            RerollerConfig.get().clearLecternPos(); QuickSetup.clear();
            RerollerConfig.save();
            pending = this::rebuildWidgets;
        }).bounds(8, 20, width - 112, 20).build());
    }
    private Button addButton(String key, int x, int y, int w, Runnable action) {
        var button=addRenderableWidget(Button.builder(Text.tr(key), b -> pending = action).bounds(x,y,w,20).build());
        if(key.equals("session.resume")) button.setTooltip(Tooltip.create(net.minecraft.network.chat.Component.literal(VillagerRerollClient.CONTROLLER.getStatusMessage())));
        return button;
    }
    private void add(SearchTarget.Kind kind) {
        minecraft.gui.setScreen(new TradePickerScreen(this,kind==SearchTarget.Kind.ENCHANTMENT));
    }
    @Override public void tick() {
        if (pending != null) { var action = pending; pending = null; action.run(); }
    }
    @Override public void removed() { if (list != null) savedScroll = list.scrollAmount(); }
    @Override public void onClose() { RerollerConfig.save(); minecraft.gui.setScreen(parent); }
    @Override public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float dt) {
        super.extractRenderState(g,mx,my,dt);
        g.text(font, title, 8, 6, 0xFFFFAA00);
        var c = VillagerRerollClient.CONTROLLER;
        String stats = c.getState()==com.joplayx.client.state.RerollState.PAUSED ? c.getStatusMessage() : Text.str("session.stats", c.getAttempts(), c.getElapsedSeconds());
        g.text(font, font.plainSubstrByWidth(stats, width - 16), 8, height - 16, 0xFFAAAAAA);
    }
    private class TargetList extends ContainerObjectSelectionList<TargetRow> {
        TargetList(int w, int h, int y) { super(EnchantTargetListScreen.this.minecraft,w,h,y,46); }
        void add(RerollerConfig.EnchantTarget t) { addEntry(new TargetRow(t)); }
        @Override public int getRowWidth() { return width - 32; }
    }
    private class TargetRow extends ContainerObjectSelectionList.Entry<TargetRow> {
        private final RerollerConfig.EnchantTarget target;
        private final Button edit, remove;
        TargetRow(RerollerConfig.EnchantTarget target) {
            this.target = target;
            edit = Button.builder(Text.tr("button.edit"), b -> pending = () ->
                    minecraft.gui.setScreen(new TargetEditScreen(EnchantTargetListScreen.this,target))).bounds(0,0,56,20).build();
            remove = Button.builder(Text.tr("button.remove"), b -> pending = () -> {
                savedScroll = list.scrollAmount();
                RerollerConfig.get().targets.remove(target); RerollerConfig.save(); rebuildWidgets();
            }).bounds(0,0,62,20).build();
        }
        @Override public void extractContent(GuiGraphicsExtractor g, int mx, int my, boolean hovered, float dt) {
            int x=getContentX(), y=getContentY(), w=getContentWidth();
            edit.setX(x+w-122); edit.setY(y+12);
            remove.setX(x+w-62); remove.setY(y+12);
            String id=target.targetId().isBlank() ? Text.str("hud.not_set_short") : SearchNames.target(target);
            g.text(font, font.plainSubstrByWidth(id,Math.max(20,w-130)),x,y+3,0xFFFFFFFF);
            String status=Text.str("search.status."+target.status.name().toLowerCase(Locale.ROOT));
            if(target.bestPrice>=0) status += " | " + Text.str("search.best",target.bestPrice,target.bestQuantity);
            g.text(font,font.plainSubstrByWidth(status,Math.max(20,w-130)),x,y+17,0xFFFFCC66);
            edit.extractRenderState(g,mx,my,dt); remove.extractRenderState(g,mx,my,dt);
        }
        @Override public List<? extends GuiEventListener> children(){return List.of(edit,remove);}
        @Override public List<? extends NarratableEntry> narratables(){return List.of(edit,remove);}
    }
}
