package com.joplayx.client.config;

import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.IntegerFieldControllerBuilder;
import dev.isxander.yacl3.api.controller.IntegerSliderControllerBuilder;
import dev.isxander.yacl3.api.controller.TickBoxControllerBuilder;
import com.joplayx.client.VillagerRerollClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

public class RerollerConfigScreen {

    public static Screen create(Screen parent) {
        RerollerConfig.Config cfg = RerollerConfig.get();
        Minecraft mc = Minecraft.getInstance();

        return YetAnotherConfigLib.createBuilder()
                .title(Component.translatable(VillagerRerollClient.PREFIX + "screen.title"))
                .save(RerollerConfig::save)

                // -------------------------------------------------------
                // Category 1: Lectern Setup
                // -------------------------------------------------------
                .category(ConfigCategory.createBuilder()
                        .name(Component.translatable(VillagerRerollClient.PREFIX + "category.lectern_setup"))
                        .tooltip(Component.translatable(VillagerRerollClient.PREFIX + "category.lectern_setup.tooltip"))

                        // "Use My Position" button
                        .option(ButtonOption.createBuilder()
                                .name(Component.translatable(VillagerRerollClient.PREFIX + "button.use_my_position"))
                                .description(OptionDescription.of(Component.translatable(
                                    VillagerRerollClient.PREFIX + "button.use_my_position.desc",
                                    VillagerRerollClient.setPositionKey.getTranslatedKeyMessage().getString()
                                )))
                                .text(Component.translatable(VillagerRerollClient.PREFIX + "button.use_my_position.set.no_pos"))
                                .action((screen, opt) -> {
                                if (mc.player != null && mc.hitResult instanceof net.minecraft.world.phys.BlockHitResult blockHit) {
                                BlockPos pos = blockHit.getBlockPos();
                                cfg.setLecternPos(pos);
                                RerollerConfig.save();
                                mc.setScreen(create(parent));
                                } else if (mc.player != null) {
                                 mc.player.sendSystemMessage(Component.translatable(
                                    VillagerRerollClient.PREFIX + "chat.aim_first"
							));
						}
					})
                                .build()
                        )

                        // "Clear Position" button
                        .option(ButtonOption.createBuilder()
                                .name(Component.translatable(VillagerRerollClient.PREFIX + "button.clear_position"))
                                .description(OptionDescription.of(Component.translatable(
                                        VillagerRerollClient.PREFIX + "button.clear_position.desc"
                                )))
                                .text(cfg.hasLecternPos()
                                        ? Component.translatable(VillagerRerollClient.PREFIX + "button.clear_position.current", cfg.lecternPosString())
                                        : Component.translatable(VillagerRerollClient.PREFIX + "button.clear_position.not_set")
                                )
                                .action((screen, opt) -> {
                                    cfg.clearLecternPos();
                                    RerollerConfig.save();
                                    mc.setScreen(create(parent));
                                })
                                .build()
                        )

                        // Manual X input
                        .option(Option.<Integer>createBuilder()
                                .name(Component.translatable(VillagerRerollClient.PREFIX + "option.lectern_x"))
                                .description(OptionDescription.of(Component.translatable(
                                        VillagerRerollClient.PREFIX + "option.lectern_x.desc"
                                )))
                                .binding(
                                        0,
                                        () -> cfg.lecternX == Integer.MIN_VALUE ? 0 : cfg.lecternX,
                                        val -> cfg.lecternX = val
                                )
                                .controller(IntegerFieldControllerBuilder::create)
                                .build()
                        )

                        // Manual Y input
                        .option(Option.<Integer>createBuilder()
                                .name(Component.translatable(VillagerRerollClient.PREFIX + "option.lectern_y"))
                                .description(OptionDescription.of(Component.translatable(
                                        VillagerRerollClient.PREFIX + "option.lectern_y.desc"
                                )))
                                .binding(
                                        64,
                                        () -> cfg.lecternY == Integer.MIN_VALUE ? 64 : cfg.lecternY,
                                        val -> cfg.lecternY = val
                                )
                                .controller(IntegerFieldControllerBuilder::create)
                                .build()
                        )

                        // Manual Z input
                        .option(Option.<Integer>createBuilder()
                                .name(Component.translatable(VillagerRerollClient.PREFIX + "option.lectern_z"))
                                .description(OptionDescription.of(Component.translatable(
                                        VillagerRerollClient.PREFIX + "option.lectern_z.desc"
                                )))
                                .binding(
                                        0,
                                        () -> cfg.lecternZ == Integer.MIN_VALUE ? 0 : cfg.lecternZ,
                                        val -> cfg.lecternZ = val
                                )
                                .controller(IntegerFieldControllerBuilder::create)
                                .build()
                        )

                        .build()
                )

                // -------------------------------------------------------
                // Category 2: Timing
                // -------------------------------------------------------
                .category(ConfigCategory.createBuilder()
                        .name(Component.translatable(VillagerRerollClient.PREFIX + "category.timing"))
                        .tooltip(Component.translatable(VillagerRerollClient.PREFIX + "category.timing.tooltip"))

                        .option(Option.<Integer>createBuilder()
                                .name(Component.translatable(VillagerRerollClient.PREFIX + "option.close_delay"))
                                .description(OptionDescription.of(Component.translatable(
                                        VillagerRerollClient.PREFIX + "option.close_delay.desc"
                                )))
                                .binding(20, () -> cfg.closeDelayTicks, val -> cfg.closeDelayTicks = val)
                                .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(5, 100).step(5))
                                .build()
                        )

                        .option(Option.<Integer>createBuilder()
                                .name(Component.translatable(VillagerRerollClient.PREFIX + "option.retry_delay"))
                                .description(OptionDescription.of(Component.translatable(
                                        VillagerRerollClient.PREFIX + "option.retry_delay.desc"
                                )))
                                .binding(40, () -> cfg.retryDelayTicks, val -> cfg.retryDelayTicks = val)
                                .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(10, 200).step(10))
                                .build()
                        )

                        .build()
                )

                // -------------------------------------------------------
                // Category 3: Display
                // -------------------------------------------------------
                .category(ConfigCategory.createBuilder()
                        .name(Component.translatable(VillagerRerollClient.PREFIX + "category.display"))
                        .tooltip(Component.translatable(VillagerRerollClient.PREFIX + "category.display.tooltip"))

                        .option(Option.<Boolean>createBuilder()
                                .name(Component.translatable(VillagerRerollClient.PREFIX + "option.hud_overlay"))
                                .description(OptionDescription.of(Component.translatable(
                                        VillagerRerollClient.PREFIX + "option.hud_overlay.desc"
                                )))
                                .binding(true, () -> cfg.hudEnabled, val -> cfg.hudEnabled = val)
                                .controller(TickBoxControllerBuilder::create)
                                .build()
                        )

                        .option(Option.<Boolean>createBuilder()
                                .name(Component.translatable(VillagerRerollClient.PREFIX + "hud.details"))
                                .description(OptionDescription.of(Component.translatable(VillagerRerollClient.PREFIX + "hud.details.desc")))
                                .binding(false, () -> cfg.hudDetails, value -> cfg.hudDetails = value)
                                .controller(TickBoxControllerBuilder::create).build())

                        .option(Option.<Boolean>createBuilder()
                                .name(Component.translatable(VillagerRerollClient.PREFIX + "notification.sound"))
                                .description(OptionDescription.of(Component.translatable(VillagerRerollClient.PREFIX + "notification.sound.desc")))
                                .binding(true, () -> cfg.foundSoundEnabled, value -> cfg.foundSoundEnabled = value)
                                .controller(TickBoxControllerBuilder::create).build())

                        .option(Option.<Boolean>createBuilder()
                                .name(Component.translatable(VillagerRerollClient.PREFIX + "notification.toast"))
                                .description(OptionDescription.of(Component.translatable(VillagerRerollClient.PREFIX + "notification.toast.desc")))
                                .binding(true, () -> cfg.foundToastEnabled, value -> cfg.foundToastEnabled = value)
                                .controller(TickBoxControllerBuilder::create).build())

                        .option(ButtonOption.createBuilder()
                                .name(Component.translatable(VillagerRerollClient.PREFIX + "hud.move"))
                                .text(Component.translatable(VillagerRerollClient.PREFIX + "hud.move"))
                                .action((screen, opt) -> mc.setScreen(new HudPositionScreen(screen)))
                                .build())

                        .build()
                )

                .build()
                .generateScreen(parent);
    }
}
