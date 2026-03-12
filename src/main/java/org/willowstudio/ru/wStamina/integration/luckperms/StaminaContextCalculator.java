package org.willowstudio.ru.wStamina.integration.luckperms;

import net.luckperms.api.context.ContextCalculator;
import net.luckperms.api.context.ContextConsumer;
import net.luckperms.api.context.ImmutableContextSet;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.willowstudio.ru.wStamina.config.PluginSettings;
import org.willowstudio.ru.wStamina.stamina.RegionStaminaMode;
import org.willowstudio.ru.wStamina.stamina.StaminaContextSnapshot;
import org.willowstudio.ru.wStamina.stamina.StaminaService;

import java.util.UUID;
import java.util.function.Supplier;

final class StaminaContextCalculator implements ContextCalculator<Player> {
    private final StaminaService staminaService;
    private final Supplier<PluginSettings.LuckPerms> settingsSupplier;

    StaminaContextCalculator(StaminaService staminaService, Supplier<PluginSettings.LuckPerms> settingsSupplier) {
        this.staminaService = staminaService;
        this.settingsSupplier = settingsSupplier;
    }

    @Override
    public void calculate(@NonNull Player target, @NonNull ContextConsumer consumer) {
        PluginSettings.LuckPerms settings = settingsSupplier.get();
        UUID uuid = target.getUniqueId();
        StaminaContextSnapshot snapshot = staminaService.contextSnapshot(uuid);
        consumer.accept(settings.stateContextKey(), snapshot.state());
        consumer.accept(settings.regionContextKey(), snapshot.region());
        consumer.accept(settings.drainContextKey(), snapshot.drain());
        consumer.accept(settings.multiplierContextKey(), snapshot.multiplier());
    }

    @Override
    public @NonNull ImmutableContextSet estimatePotentialContexts() {
        PluginSettings.LuckPerms settings = settingsSupplier.get();
        return ImmutableContextSet.builder()
                .add(settings.stateContextKey(), "normal")
                .add(settings.stateContextKey(), "exhausted")
                .add(settings.regionContextKey(), RegionStaminaMode.NORMAL.contextValue())
                .add(settings.regionContextKey(), RegionStaminaMode.NO_DRAIN.contextValue())
                .add(settings.regionContextKey(), RegionStaminaMode.FORCE_ZERO.contextValue())
                .add(settings.drainContextKey(), "active")
                .add(settings.drainContextKey(), "blocked")
                .add(settings.multiplierContextKey(), "1.0")
                .build();
    }
}
