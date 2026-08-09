package com.joplayx.client.state;

import com.joplayx.VillagerReroll;
import com.joplayx.client.VillagerRerollClient;
import com.joplayx.client.config.RerollerConfig;
import com.joplayx.client.util.HotbarUtil;
import com.joplayx.client.util.OffersStore;
import com.joplayx.client.util.TradeUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * State machine driving the reroll loop.
 *
 * API references confirmed from:
 *  - VillagerRoller.java (working meteor mod): Villager package, interact() signature,
 *    profession check, EnchantmentHelper usage
 *  - KeyMappingsTest.java: tick event pattern
 *  - HudTests.java: rendering pattern
 */
public class RerollController {

	private static final String PREFIX = "villager-reroll.";

	private RerollState state = RerollState.IDLE;
	private String statusKey = PREFIX + "status.idle";
	private Object[] statusArgs = new Object[0];
	private String lastTradeDescription = "";
	private String lastOfferSeen = "";
	private java.util.List<TradeUtil.TargetStatus> lastStatuses = java.util.List.of();
	private int attempts = 0;
	private String errorKey = "";
	private Object[] errorArgs = new Object[0];

	private int tickDelay = 0;
	private int professionWaitTicks = 0;
	private static final int MAX_PROFESSION_WAIT = 200;
	private int breakTicks = 0;
	private static final int MAX_BREAK_TICKS = 100;

	// Store by entity ID — safe across ticks on the client
	private int targetEntityId = -1;

	public RerollState getState() { return state; }
	public String getStatusMessage() { return Component.translatable(statusKey, statusArgs).getString(); }
	public String getLastTradeDescription() { return lastTradeDescription; }
	public String getLastOfferSeen() { return lastOfferSeen; }
	public java.util.List<TradeUtil.TargetStatus> getLastStatuses() { return lastStatuses; }
	public int getAttempts() { return attempts; }
	public String getErrorReason() { return Component.translatable(errorKey, errorArgs).getString(); }

	public void toggleStartStop(Minecraft client) {
		if (state == RerollState.IDLE) start(client);
		else stop(client, PREFIX + "error.stopped_by_player");
	}

	public void emergencyStop(Minecraft client) {
		stop(client, PREFIX + "error.emergency_stop");
		if (client.player != null)
			client.player.sendSystemMessage(Component.translatable(PREFIX + "chat.emergency_stopped"));
	}

	public void tick(Minecraft client) {
		if (state == RerollState.IDLE || state == RerollState.FOUND || state == RerollState.ERROR) return;
		if (tickDelay > 0) { tickDelay--; return; }

		LocalPlayer player = client.player;
		ClientLevel level = client.level;
		if (player == null || level == null) { stop(client, PREFIX + "error.null_player"); return; }

		switch (state) {

			case PRE_CHECK -> {
				setStatus(PREFIX + "status.checking");
				if (!HotbarUtil.hasItemInHotbar(player, Items.LECTERN)) {
					stop(client, PREFIX + "error.no_lectern"); return;
				}
				if (RerollerConfig.get().targets.isEmpty()) {
					stop(client, PREFIX + "error.no_targets"); return;
				}
				if (!RerollerConfig.get().hasLecternPos()) {
				stop(client, PREFIX + "error.no_position",
						VillagerRerollClient.setPositionKey.getTranslatedKeyMessage().getString()); return;
				}
				Villager v = findNearestVillager(player, level);
				if (v == null) {
					stop(client, PREFIX + "error.no_villager"); return;
				}
				targetEntityId = v.getId();
				setState(RerollState.PLACE_LECTERN, PREFIX + "status.placing_lectern", 5);
			}

			case PLACE_LECTERN -> {
				BlockPos pos = RerollerConfig.get().lecternPos();
				if (!level.getBlockState(pos).isAir() && !level.getBlockState(pos).is(Blocks.LECTERN)) {
					stop(client, PREFIX + "error.position_blocked"); return;
				}
				HotbarUtil.selectItem(player, Items.LECTERN);
				BlockHitResult hit = new BlockHitResult(
						Vec3.atBottomCenterOf(pos), Direction.UP, pos.below(), false
				);
				if (client.gameMode != null) {
					client.gameMode.useItemOn(player, InteractionHand.MAIN_HAND, hit);
				}
				professionWaitTicks = 0;
				setState(RerollState.WAIT_FOR_PROFESSION, PREFIX + "status.waiting_for_librarian", 20);
			}

			case WAIT_FOR_PROFESSION -> {
				professionWaitTicks++;
				Entity entity = level.getEntity(targetEntityId);
				if (entity == null || !entity.isAlive()) {
					stop(client, PREFIX + "error.villager_disappeared"); return;
				}

				if (entity instanceof Villager villager) {
					// Profession check confirmed from working mod (VillagerRoller.java line 895)
					// unwrapKey() returns ResourceKey<VillagerProfession>
					// "none" path = unemployed/no profession
					villager.getVillagerData().profession().unwrapKey().ifPresent(profKey -> {
					if (!profKey.identifier().getPath().equals("none")) {
							setState(RerollState.OPEN_VILLAGER, PREFIX + "status.librarian_found", 10);
						}
					});
				}

				if (professionWaitTicks >= MAX_PROFESSION_WAIT) {
					setState(RerollState.BREAK_LECTERN, PREFIX + "status.profession_timeout", 5);
				}
			}

			case OPEN_VILLAGER -> {
				Entity entity = level.getEntity(targetEntityId);
				if (entity == null || !entity.isAlive()) {
					stop(client, PREFIX + "error.villager_disappeared"); return;
				}
				if (client.gameMode != null) {
					// 4-arg interact() confirmed from working mod (VillagerRoller.java)
					EntityHitResult entityHit = new EntityHitResult(entity);
					client.gameMode.interact(player, entity, entityHit, InteractionHand.MAIN_HAND);
				}
				setState(RerollState.WAIT_FOR_SCREEN, PREFIX + "status.waiting_for_screen", 10);
			}

		case WAIT_FOR_SCREEN -> {
				// Wait for the packet mixin to populate OffersStore
				if (OffersStore.get() != null) {
					setState(RerollState.READ_TRADES, PREFIX + "status.reading_trades", 2);
				} else {
					tickDelay = 5;
				}
			}

			case READ_TRADES -> {
				TradeUtil.TradeResult result = TradeUtil.checkTrades(RerollerConfig.get());
				attempts++;
				lastStatuses = result.statuses();
				lastOfferSeen = result.lastOfferSeen();

				if (result.anyFound()) {
					RerollerConfig.Config cfg = RerollerConfig.get();
					RerollerConfig.EnchantTarget found = result.foundTarget();
					String desc = result.descriptionFor(found);
					lastTradeDescription = desc;

					setStatus(PREFIX + "status.found", desc);
					player.closeContainer();
					player.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);

					// Wishlist model: you can't hold multiple simultaneous matches on one
					// villager (rerolling wipes the whole trade list), so the first target
					// found stops the reroller AND gets removed from the list - the next
					// time you start it (presumably on a different villager), it's only
					// looking for whatever targets are left.
					cfg.targets.remove(found);
					RerollerConfig.save();

					if (cfg.targets.isEmpty()) {
						player.sendSystemMessage(Component.translatable(
								PREFIX + "chat.found_all", desc, String.valueOf(attempts)));
					} else {
						player.sendSystemMessage(Component.translatable(
								PREFIX + "chat.found_some", desc, String.valueOf(attempts),
								String.valueOf(cfg.targets.size())));
					}
				} else {
					setState(RerollState.CLOSE_SCREEN, PREFIX + "status.no_match", 2);
				}
			}

			case CLOSE_SCREEN -> {
				if (player.containerMenu instanceof MerchantMenu) player.closeContainer();
				setState(RerollState.WAIT_AFTER_CLOSE, PREFIX + "status.waiting_before_break", RerollerConfig.get().closeDelayTicks);
			}

			case WAIT_AFTER_CLOSE -> {
				breakTicks = 0; // Reset so BREAK_LECTERN starts fresh on tick 0
				setState(RerollState.BREAK_LECTERN, PREFIX + "status.breaking_lectern", 2);
			}

			case BREAK_LECTERN -> {
				BlockPos pos = RerollerConfig.get().lecternPos();
				statusKey = PREFIX + "status.breaking_lectern";
				statusArgs = new Object[0];

				// Block already gone — move on
				if (level.getBlockState(pos).isAir()) {
					breakTicks = 0;
					if (client.gameMode != null) client.gameMode.stopDestroyBlock();
					setState(RerollState.WAIT_BEFORE_RETRY, PREFIX + "status.lectern_broken", RerollerConfig.get().retryDelayTicks);
					return;
				}

				if (client.gameMode != null) {
					if (breakTicks == 0) {
						// First tick — select axe once and start breaking
						HotbarUtil.selectBestAxe(player);
						client.gameMode.startDestroyBlock(pos, Direction.UP);
					} else {
						// Subsequent ticks — continue breaking without re-selecting
						client.gameMode.continueDestroyBlock(pos, Direction.UP);
					}
				}

				if (++breakTicks >= MAX_BREAK_TICKS) stop(client, PREFIX + "error.cannot_break");
			}

			case WAIT_BREAK_COMPLETE ->
				setState(RerollState.WAIT_BEFORE_RETRY, PREFIX + "status.waiting_before_retry", RerollerConfig.get().retryDelayTicks);

			case WAIT_BEFORE_RETRY -> {
				if (!HotbarUtil.hasItemInHotbar(player, Items.LECTERN)) {
					stop(client, PREFIX + "error.no_lecterns"); return;
				}
				setState(RerollState.PLACE_LECTERN, PREFIX + "status.attempt", 5, String.valueOf(attempts + 1));
			}

			default -> stop(client, PREFIX + "error.unknown_state", String.valueOf(state));
		}
	}

	private void start(Minecraft client) {
		if (client.level == null || client.player == null) return;
		attempts = 0;
		errorKey = "";
		errorArgs = new Object[0];
		lastTradeDescription = "";
		targetEntityId = -1;
		OffersStore.clear();
		setState(RerollState.PRE_CHECK, PREFIX + "status.starting", 2);
		client.player.sendSystemMessage(Component.translatable(PREFIX + "chat.started",
				VillagerRerollClient.emergencyStopKey.getTranslatedKeyMessage().getString()));
	}

	private void stop(Minecraft client, String key, Object... args) {
		errorKey = key;
		errorArgs = args;
		statusKey = key;
		statusArgs = args;
		boolean isPlayerStop = key.equals(PREFIX + "error.stopped_by_player") || key.equals(PREFIX + "error.emergency_stop");
		state = isPlayerStop ? RerollState.IDLE : RerollState.ERROR;
		tickDelay = 0;
		VillagerReroll.LOGGER.info("[Reroller] Stopped: {}", Component.translatable(key, args).getString());
		if (client.player != null)
			client.player.sendSystemMessage(Component.translatable(PREFIX + "chat.stopped",
					Component.translatable(key, args).getString()));
	}

	private void setState(RerollState newState, String key, int delay, Object... args) {
		state = newState;
		statusKey = key;
		statusArgs = args;
		tickDelay = delay;
		VillagerReroll.LOGGER.debug("[Reroller] -> {} | {}", newState, Component.translatable(key, args).getString());
	}

	private void setStatus(String key, Object... args) {
		statusKey = key;
		statusArgs = args;
	}

	private Villager findNearestVillager(LocalPlayer player, ClientLevel level) {
		// Villager package: net.minecraft.world.entity.npc.villager.Villager
		// Confirmed from working mod (VillagerRoller.java import line)
		AABB box = player.getBoundingBox().inflate(6.0);
		List<Entity> entities = level.getEntities(player, box,
				e -> e instanceof Villager && e.isAlive());
		return entities.stream()
				.map(e -> (Villager) e)
				.min((a, b) -> Double.compare(
						a.position().distanceToSqr(player.position()),
						b.position().distanceToSqr(player.position())))
				.orElse(null);
	}
}
