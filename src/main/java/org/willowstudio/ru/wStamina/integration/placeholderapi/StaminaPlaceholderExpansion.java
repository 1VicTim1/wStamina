package org.willowstudio.ru.wStamina.integration.placeholderapi;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.willowstudio.ru.wStamina.WStaminaPlugin;
import org.willowstudio.ru.wStamina.logging.DebugLogger;
import org.willowstudio.ru.wStamina.logging.DebugModule;
import org.willowstudio.ru.wStamina.stamina.StaminaPlayerSnapshot;
import org.willowstudio.ru.wStamina.stamina.StaminaService;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class StaminaPlaceholderExpansion extends PlaceholderExpansion {
    private final WStaminaPlugin plugin;
    private final StaminaService staminaService;
    private final DebugLogger debugLogger;
    private final String identifier;

    public StaminaPlaceholderExpansion(
            WStaminaPlugin plugin,
            StaminaService staminaService,
            DebugLogger debugLogger,
            String identifier
    ) {
        this.plugin = plugin;
        this.staminaService = staminaService;
        this.debugLogger = debugLogger;
        this.identifier = identifier.toLowerCase();
    }

    @Override
    public @NotNull String getIdentifier() {
        return identifier;
    }

    @Override
    public @NotNull String getAuthor() {
        return String.join(", ", plugin.getPluginMeta().getAuthors());
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public @Nullable String onRequest(OfflinePlayer offlinePlayer, @NotNull String params) {
        if (offlinePlayer == null || !offlinePlayer.isOnline()) {
            return "0";
        }
        Player player = offlinePlayer.getPlayer();
        if (player == null) {
            return "0";
        }

        StaminaPlayerSnapshot snapshot = staminaService.snapshot(player);
        String key = params.toLowerCase();
        String result = switch (key) {
            case "current" -> format(snapshot.current());
            case "max" -> format(snapshot.max());
            case "percent" -> format(snapshot.percent());
            case "state" -> snapshot.exhausted() ? "exhausted" : "normal";
            case "region" -> snapshot.regionMode().contextValue();
            case "multiplier" -> format(snapshot.multiplier());
            case "nodrain" -> Boolean.toString(snapshot.noDrainPermission());
            case "exhausted" -> Boolean.toString(snapshot.exhausted());
            default -> null;
        };

        debugLogger.log(DebugModule.PLACEHOLDERS, () ->
                "PAPI request " + identifier + "_" + key + " for " + player.getName() + " -> " + result);
        return result;
    }

    private static String format(double value) {
        return BigDecimal.valueOf(value)
                .setScale(2, RoundingMode.HALF_UP)
                .stripTrailingZeros()
                .toPlainString();
    }
}
