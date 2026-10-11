package com.joplayx.client.state;

import com.joplayx.client.VillagerRerollClient;
import com.joplayx.client.config.*;
import com.joplayx.client.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.*;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.phys.*;
import java.util.*;

public class RerollController {
    private static final net.minecraft.client.gui.components.toasts.SystemToast.SystemToastId MATCH_TOAST =
            new net.minecraft.client.gui.components.toasts.SystemToast.SystemToastId(7000);
    private RerollState state = RerollState.IDLE;
    private String statusKey = "status.idle";
    private Object[] statusArgs = {};
    private final SessionStats stats = new SessionStats();
    private boolean sessionStarted;
    private ClientLevel sessionWorld;
    private UUID targetUuid;
    private int targetEntityId = -1;
    private int delay, waitTicks, breakTicks;
    private String lastTradeDescription = "", lastOfferSeen = "";
    private List<TradeUtil.TargetStatus> lastStatuses = List.of();
    private Workstation workstation = Workstation.LECTERN;
    private BlockPos workstationPos;

    public void clearVillagerSelection() { targetUuid=null;targetEntityId=-1; }
    public RerollState getState() { return state; }
    public String getStatusMessage() { return Text.str(statusKey, statusArgs); }
    public String getLastTradeDescription() { return lastTradeDescription; }
    public String getLastOfferSeen() { return lastOfferSeen; }
    public List<TradeUtil.TargetStatus> getLastStatuses() { return lastStatuses; }
    public int getAttempts() { return stats.attempts(); }
    public long getElapsedSeconds() { return stats.seconds(); }
    public boolean running() { return state != RerollState.IDLE && state != RerollState.PAUSED
            && state != RerollState.FOUND && state != RerollState.ERROR; }

    public void toggleStartStop(Minecraft client) {
        if (running()) pause(client, "error.stopped_by_player");
        else resume(client);
    }
    public void emergencyStop(Minecraft client) {
        pause(client, "error.emergency_stop");
    }
    public void pauseForConfig(Minecraft client) {
        if (running()) pause(client, "session.editing");
    }
    public void onDisconnect(Minecraft client) {
        if (running()) pause(client, "error.null_player");
        sessionWorld = null;
        targetUuid = null;
        targetEntityId = -1;
        OffersStore.clear();
    }
    public void newSession(Minecraft client) {
        if (client.gameMode != null) client.gameMode.stopDestroyBlock();
        stats.reset();
        sessionStarted = true;
        targetUuid = null;
        targetEntityId = -1;
        lastOfferSeen = "";
        lastTradeDescription = "";
        lastStatuses = List.of();
        state = RerollState.PAUSED;
        resume(client);
    }
    public void resume(Minecraft client) {
        if (client.player == null || client.level == null) return;
        var cfg = RerollerConfig.get();
        if (cfg.targets.stream().anyMatch(t -> t.status == SearchTarget.Status.FOUND)) {
            setState(RerollState.FOUND, "session.choose_action", 0);
            client.setScreen(new FoundTradeScreen(new EnchantTargetListScreen(null)));
            return;
        }
        if (!sessionStarted) { stats.reset(); sessionStarted = true; }
        if (sessionWorld != client.level) { targetUuid = null; targetEntityId = -1; }
        sessionWorld = client.level;
        if (cfg.targets.stream().filter(t -> t.appliesTo(cfg.workstation)).filter(SearchTarget::active).anyMatch(t -> t.targetId().isBlank())) {
            pause(client, "search.invalid"); return;
        }
        for(var t:cfg.targets) if(t.active() && t.appliesTo(cfg.workstation)) {
            var id=net.minecraft.resources.Identifier.tryParse(t.targetId());
            if(id==null || (t.kind==SearchTarget.Kind.ENCHANTMENT
                    ? client.level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).get(id).isEmpty()
                    : !net.minecraft.core.registries.BuiltInRegistries.ITEM.containsKey(id))) {
                pause(client,"help.unavailable");return;
            }
        }
        workstation = Workstation.from(cfg.workstation);
        workstationPos = cfg.lecternPos();
        OffersStore.clear();
        waitTicks = 0; breakTicks = 0;
        if (client.player.containerMenu instanceof MerchantMenu) client.player.closeContainer();
        setState(RerollState.PRE_CHECK, "session.resuming", 2);
    }
    private void pause(Minecraft client, String key, Object... args) {
        if (client.gameMode != null) client.gameMode.stopDestroyBlock();
        OffersStore.clear();
        setState(RerollState.PAUSED, key, 0, args);
        if (client.player != null) client.player.sendSystemMessage(Text.tr("chat.stopped", Text.tr(key, args)));
    }
    private void setState(RerollState next, String key, int ticks, Object... args) {
        state = next; statusKey = key; statusArgs = args; delay = ticks;
    }
    private Villager target(ClientLevel level) {
        var entity = level.getEntity(targetEntityId);
        return entity instanceof Villager v && v.isAlive() && v.getUUID().equals(targetUuid) ? v : null;
    }
    private String profession(Villager v) {
        return v.getVillagerData().profession().unwrapKey().map(k -> k.identifier().getPath()).orElse("");
    }
    private Villager findVillager(LocalPlayer player, ClientLevel level) {
        // Prefer the villager under the crosshair, otherwise choose the nearest eligible novice.
        var hit = Minecraft.getInstance().hitResult;
        if (hit instanceof EntityHitResult e && e.getEntity() instanceof Villager v && eligible(v)) return v;
        return level.getEntities(player, player.getBoundingBox().inflate(6), e -> e instanceof Villager v && eligible(v))
                .stream().map(e -> (Villager)e)
                .min(Comparator.comparingDouble(v -> v.position().distanceToSqr(player.position()))).orElse(null);
    }
    private boolean eligible(Villager v) {
        return v.isAlive() && !v.isBaby() && v.getVillagerData().level() == 1
                && (profession(v).equals("none") || profession(v).equals(workstation.profession));
    }

    public void tick(Minecraft client) {
        if (!running()) return;
        if (client.player == null || client.level == null || client.gameMode == null) { onDisconnect(client); return; }
        if (client.level != sessionWorld) { pause(client, "session.world_changed"); return; }
        if (client.isPaused()) return;
        stats.tick();
        if (delay > 0) { delay--; return; }
        var player = client.player;
        var level = client.level;
        var cfg = RerollerConfig.get();
        if (state != RerollState.PRE_CHECK && (target(level) == null
                || target(level).position().distanceToSqr(player.position()) > 36)) {
            pause(client, "error.villager_disappeared"); return;
        }
        switch (state) {
            case PRE_CHECK -> {
                if (cfg.targets.stream().filter(t -> t.appliesTo(cfg.workstation)).noneMatch(SearchTarget::active)) { pause(client, "error.no_targets"); return; }
                if (workstationPos == null) { pause(client, "error.no_position", VillagerRerollClient.setPositionKey.getTranslatedKeyMessage()); return; }
                Villager v = QuickSetup.hasSelection() ? QuickSetup.selected(client) : target(level);
                if(v==null && !QuickSetup.hasSelection()) v=findVillager(player,level);
                if(v!=null&&!eligible(v)){pause(client,"setup.ineligible");return;}
                if (!player.isWithinBlockInteractionRange(workstationPos, 0)) { pause(client,"help.block_reach");return; }
                if(v!=null&&!player.isWithinEntityInteractionRange(v,0)){pause(client,"help.villager_reach");return;}
                if (v == null) { pause(client, "error.no_villager"); return; }
                targetUuid = v.getUUID(); targetEntityId = v.getId();
                if (!level.getBlockState(workstationPos).isAir() && !level.getBlockState(workstationPos).is(workstation.block)) {
                    pause(client, "error.position_blocked"); return;
                }
                if (level.getBlockState(workstationPos).is(workstation.block)) {
                    waitTicks = 0;
                    setState(RerollState.WAIT_FOR_PROFESSION, "status.waiting_for_librarian", 5);
                } else setState(RerollState.PLACE_LECTERN, "status.placing_lectern", 2);
            }
            case PLACE_LECTERN -> {
                if(!player.isWithinBlockInteractionRange(workstationPos,0)){pause(client,"help.block_reach");return;}
                if (!level.getBlockState(workstationPos).isAir() && !level.getBlockState(workstationPos).is(workstation.block)) {
                    pause(client, "error.position_blocked"); return;
                }
                if (level.getBlockState(workstationPos).isAir()) {
                    if (!HotbarUtil.hasItemInHotbar(player, workstation.block.asItem())) {
                        pause(client, "error.no_lecterns", workstation.block.getName()); return;
                    }
                    HotbarUtil.selectItem(player, workstation.block.asItem());
                    client.gameMode.useItemOn(player, InteractionHand.MAIN_HAND,
                            new BlockHitResult(Vec3.atBottomCenterOf(workstationPos), Direction.UP, workstationPos.below(), false));
                }
                waitTicks = 0;
                setState(RerollState.WAIT_FOR_PROFESSION, "status.waiting_for_librarian", 20);
            }
            case WAIT_FOR_PROFESSION -> {
                if (profession(target(level)).equals(workstation.profession)
                        && level.getBlockState(workstationPos).is(workstation.block)) {
                    setState(RerollState.OPEN_VILLAGER, "status.librarian_found", 10);
                } else if (++waitTicks >= 200) pause(client, "status.profession_timeout");
            }
            case OPEN_VILLAGER -> {
                OffersStore.clear(); // Never evaluate offers left over from the previous reroll.
                var v = target(level);
                if(!player.isWithinEntityInteractionRange(v,0)){pause(client,"help.villager_reach");return;}
                client.gameMode.interact(player, v, new EntityHitResult(v), InteractionHand.MAIN_HAND);
                waitTicks = 0;
                setState(RerollState.WAIT_FOR_SCREEN, "status.waiting_for_screen", 2);
            }
            case WAIT_FOR_SCREEN -> {
                if (player.containerMenu instanceof MerchantMenu menu && OffersStore.get(menu.containerId) != null)
                    setState(RerollState.READ_TRADES, "status.reading_trades", 0);
                else if (++waitTicks >= 200) pause(client, "session.trade_timeout");
            }
            case READ_TRADES -> {
                if (!(player.containerMenu instanceof MerchantMenu menu) || OffersStore.get(menu.containerId) == null) {
                    pause(client, "session.trade_timeout"); return;
                }
                var result = TradeUtil.checkTrades(cfg, OffersStore.get(menu.containerId));
                stats.attempt();
                lastStatuses = result.statuses(); lastOfferSeen = result.lastOfferSeen();
                if (result.anyFound()) {
                    client.gameMode.stopDestroyBlock();
                    setState(RerollState.FOUND, "status.found", 0, result.matches().getFirst().description());
                    for (var match : result.matches()) {
                        var offer = match.offer();
                        match.target().record(offer.price(), offer.quantity(),
                                offer.enchantments().getOrDefault(match.target().enchantmentId, 0), offer.secondPrice(), match.description());
                        match.target().vanillaBaseMinimum = TradeUtil.vanillaBaseMinimum(match.target());
                        player.sendSystemMessage(Text.tr("chat.match_saved", match.description(), stats.attempts()));
                    }
                    lastTradeDescription = result.matches().getFirst().description();
                    RerollerConfig.save();
                    if(cfg.foundSoundEnabled) player.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 1, 1);
                    if(cfg.foundToastEnabled) net.minecraft.client.gui.components.toasts.SystemToast.addOrUpdate(
                        client.getToastManager(), MATCH_TOAST, Text.tr("hud.found"),
                        net.minecraft.network.chat.Component.literal(SearchNames.found(result.matches().getFirst().target())));
                    // Keep the merchant screen open so the player can acquire the trade. FOUND never ticks.
                } else if (OffersStore.villagerXp() > 0 || target(level).getVillagerData().level() > 1) {
                    pause(client, "session.locked");
                } else setState(RerollState.CLOSE_SCREEN, "status.no_match", 2);
            }
            case CLOSE_SCREEN -> {
                if (player.containerMenu instanceof MerchantMenu) player.closeContainer();
                OffersStore.clear();
                setState(RerollState.WAIT_AFTER_CLOSE, "status.waiting_before_break", cfg.closeDelayTicks);
            }
            case WAIT_AFTER_CLOSE -> {
                breakTicks = 0;
                setState(RerollState.BREAK_LECTERN, "status.breaking_lectern", 0);
            }
            case BREAK_LECTERN -> {
                if(!player.isWithinBlockInteractionRange(workstationPos,0)){pause(client,"help.block_reach");return;}
                if (level.getBlockState(workstationPos).isAir()) {
                    client.gameMode.stopDestroyBlock();
                    setState(RerollState.WAIT_BEFORE_RETRY, "status.lectern_broken", cfg.retryDelayTicks);
                    return;
                }
                // Never destroy a replacement block placed by another player.
                if (!level.getBlockState(workstationPos).is(workstation.block)) { pause(client, "error.position_blocked"); return; }
                if (breakTicks++ == 0) {
                    HotbarUtil.selectBestTool(player, level.getBlockState(workstationPos));
                    client.gameMode.startDestroyBlock(workstationPos, Direction.UP);
                } else client.gameMode.continueDestroyBlock(workstationPos, Direction.UP);
                if (breakTicks >= 200) pause(client, "error.cannot_break");
            }
            case WAIT_BEFORE_RETRY, WAIT_BREAK_COMPLETE -> setState(RerollState.PLACE_LECTERN, "status.attempt", 0, stats.attempts() + 1);
            default -> pause(client, "error.unknown_state", state);
        }
    }
}
