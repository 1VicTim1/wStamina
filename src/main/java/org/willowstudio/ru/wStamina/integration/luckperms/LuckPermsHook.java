package org.willowstudio.ru.wStamina.integration.luckperms;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.cacheddata.CachedPermissionData;
import net.luckperms.api.platform.PlayerAdapter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.willowstudio.ru.wStamina.config.Lang;
import org.willowstudio.ru.wStamina.config.PluginSettings;
import org.willowstudio.ru.wStamina.logging.DebugLogger;
import org.willowstudio.ru.wStamina.logging.DebugModule;
import org.willowstudio.ru.wStamina.provider.StaminaPermissionProvider;
import org.willowstudio.ru.wStamina.stamina.StaminaModifier;
import org.willowstudio.ru.wStamina.stamina.StaminaService;

import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class LuckPermsHook implements StaminaPermissionProvider {
    private static final double EPSILON = 0.0001D;

    private final DebugLogger debugLogger;
    private final Lang lang;
    private final Map<UUID, Double> lastLoggedMultiplier = new ConcurrentHashMap<>();
    private final Set<String> invalidMultiplierPermissions = ConcurrentHashMap.newKeySet();
    private PluginSettings.LuckPerms settings;

    private LuckPerms luckPerms;
    private PlayerAdapter<Player> playerAdapter;
    private StaminaContextCalculator contextCalculator;
    private boolean hooked;

    public LuckPermsHook(DebugLogger debugLogger, Lang lang, PluginSettings.LuckPerms settings) {
        this.debugLogger = debugLogger;
        this.lang = lang;
        this.settings = settings;
    }

    public void initialize(StaminaService staminaService) {
        hooked = false;
        if (!settings.enabled()) {
            debugLogger.log(DebugModule.HOOKS, "LuckPerms hook disabled in config.");
            return;
        }
        if (Bukkit.getPluginManager().getPlugin("LuckPerms") == null) {
            debugLogger.log(DebugModule.HOOKS, "LuckPerms plugin not found.");
            return;
        }

        try {
            luckPerms = LuckPermsProvider.get();
            playerAdapter = luckPerms.getPlayerAdapter(Player.class);
            contextCalculator = new StaminaContextCalculator(staminaService, this::settings);
            luckPerms.getContextManager().registerCalculator(contextCalculator);
            hooked = true;
            debugLogger.log(DebugModule.HOOKS, "LuckPerms hook initialized.");
        } catch (Exception exception) {
            lang.warning("messages.hooks.luckperms.init-failed", Map.of("reason", String.valueOf(exception.getMessage())));
            debugLogger.log(DebugModule.LUCKPERMS, () -> "Initialization error: " + exception);
        }
    }

    public void shutdown() {
        if (hooked && luckPerms != null && contextCalculator != null) {
            luckPerms.getContextManager().unregisterCalculator(contextCalculator);
            debugLogger.log(DebugModule.HOOKS, "LuckPerms context calculator unregistered.");
        }
        hooked = false;
        contextCalculator = null;
        playerAdapter = null;
        luckPerms = null;
        invalidateAll();
    }

    public void reloadSettings(PluginSettings.LuckPerms settings, StaminaService staminaService) {
        this.settings = settings;
        shutdown();
        initialize(staminaService);
    }

    public boolean isHooked() {
        return hooked;
    }

    public PluginSettings.LuckPerms settings() {
        return settings;
    }

    @Override
    public void signalContextUpdate(Player player) {
        if (!hooked || luckPerms == null) {
            return;
        }
        luckPerms.getContextManager().signalContextUpdate(player);
        debugLogger.log(DebugModule.LUCKPERMS, () -> "Context signal sent for " + player.getName());
    }

    @Override
    public StaminaModifier resolveModifier(Player player) {
        if (!hooked || playerAdapter == null) {
            return resolveFallbackModifier(player);
        }

        try {
            CachedPermissionData permissionData = playerAdapter.getPermissionData(player);
            boolean noDrain = permissionData.checkPermission(settings.noDrainPermission()).asBoolean();
            double multiplier = resolveMultiplier(permissionData.getPermissionMap(), player.getUniqueId(), player.getName());
            return new StaminaModifier(multiplier, noDrain);
        } catch (Exception exception) {
            debugLogger.log(DebugModule.LUCKPERMS, () ->
                    "Failed to resolve LuckPerms data for " + player.getName() + ": " + exception.getMessage());
            return resolveFallbackModifier(player);
        }
    }

    private StaminaModifier resolveFallbackModifier(Player player) {
        boolean noDrain = player.hasPermission(settings.noDrainPermission());
        return new StaminaModifier(1.0D, noDrain);
    }

    private double resolveMultiplier(Map<String, Boolean> permissionMap, UUID playerId, String playerName) {
        String prefix = settings.multiplierPermissionPrefix();
        String lowerPrefix = prefix.toLowerCase(Locale.ROOT);
        double selected = 1.0D;

        for (Map.Entry<String, Boolean> entry : permissionMap.entrySet()) {
            if (!Boolean.TRUE.equals(entry.getValue())) {
                continue;
            }

            String permission = entry.getKey();
            String lowered = permission.toLowerCase(Locale.ROOT);
            if (!lowered.startsWith(lowerPrefix)) {
                continue;
            }

            String rawValue = permission.substring(prefix.length()).trim();
            Double parsed = parseMultiplier(rawValue);
            if (parsed == null) {
                String normalizedPermission = permission.toLowerCase(Locale.ROOT);
                if (invalidMultiplierPermissions.add(normalizedPermission)) {
                    debugLogger.log(DebugModule.STAMINA, () ->
                            "Cannot parse multiplier permission '" + permission + "' for " + playerName);
                }
                continue;
            }
            if (parsed > selected) {
                selected = parsed;
            }
        }

        double clamped = clamp(selected, settings.minMultiplier(), settings.maxMultiplier());
        Double previous = lastLoggedMultiplier.put(playerId, clamped);
        if (previous == null || Math.abs(previous - clamped) > EPSILON) {
            double selectedResolved = selected;
            debugLogger.log(DebugModule.STAMINA, () ->
                    "Resolved multiplier for " + playerName + ": " + selectedResolved + " -> " + clamped);
        }
        return clamped;
    }

    private static Double parseMultiplier(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }
        String normalized = rawValue
                .replace('_', '.')
                .replace(',', '.')
                .replace('x', ' ')
                .trim();
        if (normalized.isEmpty()) {
            return null;
        }
        try {
            double parsed = Double.parseDouble(normalized);
            if (!Double.isFinite(parsed) || parsed <= 0.0D) {
                return null;
            }
            return parsed;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    @Override
    public void invalidatePlayer(UUID playerId) {
        lastLoggedMultiplier.remove(playerId);
    }

    @Override
    public void invalidateAll() {
        lastLoggedMultiplier.clear();
        invalidMultiplierPermissions.clear();
    }
}
