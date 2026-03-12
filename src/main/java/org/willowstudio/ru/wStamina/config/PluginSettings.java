package org.willowstudio.ru.wStamina.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.willowstudio.ru.wStamina.logging.DebugModule;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

public final class PluginSettings {
    private final Stamina stamina;
    private final LuckPerms luckPerms;
    private final WorldGuard worldGuard;
    private final Placeholders placeholders;
    private final Map<DebugModule, Boolean> debugModules;

    private PluginSettings(
            Stamina stamina,
            LuckPerms luckPerms,
            WorldGuard worldGuard,
            Placeholders placeholders,
            Map<DebugModule, Boolean> debugModules
    ) {
        this.stamina = stamina;
        this.luckPerms = luckPerms;
        this.worldGuard = worldGuard;
        this.placeholders = placeholders;
        this.debugModules = debugModules;
    }

    public static PluginSettings load(FileConfiguration config) {
        ConfigurationSection staminaSection = config.getConfigurationSection("stamina");
        double maxPoints = Math.max(1.0D, getDouble(staminaSection, "max-points", 100.0D));
        double drainPerTick = Math.max(0.0D, getDouble(staminaSection, "drain-per-tick", 1.0D));
        double regenPerTick = Math.max(0.0D, getDouble(staminaSection, "regen-per-tick", 1.0D));
        int regenDelayTicks = Math.max(0, staminaSection == null ? 40 : staminaSection.getInt("regen-delay-ticks", 40));
        boolean regenWhileWalking = getBoolean(staminaSection, "regen-while-walking", true);
        double movementThreshold = Math.max(0.0D, getDouble(staminaSection, "movement-threshold", 0.02D));
        long tickInterval = Math.max(1L, staminaSection == null ? 1L : staminaSection.getLong("tick-interval", 1L));
        Stamina stamina = new Stamina(
                maxPoints,
                drainPerTick,
                regenPerTick,
                regenDelayTicks,
                regenWhileWalking,
                movementThreshold,
                tickInterval
        );

        ConfigurationSection lpSection = config.getConfigurationSection("luckperms");
        boolean lpEnabled = getBoolean(lpSection, "enabled", true);
        ConfigurationSection lpPermissions = lpSection == null ? null : lpSection.getConfigurationSection("permissions");
        String noDrainPermission = getString(lpPermissions, "no-drain", "wstamina.nodrain");
        String multiplierPrefix = getString(lpPermissions, "multiplier-prefix", "wstamina.multiplier.");
        if (!multiplierPrefix.endsWith(".")) {
            multiplierPrefix = multiplierPrefix + ".";
        }
        ConfigurationSection lpMultiplier = lpSection == null ? null : lpSection.getConfigurationSection("multiplier");
        double minMultiplier = Math.max(0.01D, getDouble(lpMultiplier, "min", 0.1D));
        double maxMultiplier = Math.max(minMultiplier, getDouble(lpMultiplier, "max", 10.0D));
        ConfigurationSection lpContexts = lpSection == null ? null : lpSection.getConfigurationSection("contexts");
        String stateContextKey = getString(lpContexts, "state-key", "stamina_state");
        String regionContextKey = getString(lpContexts, "region-key", "stamina_region");
        String drainContextKey = getString(lpContexts, "drain-key", "stamina_drain");
        String multiplierContextKey = getString(lpContexts, "multiplier-key", "stamina_multiplier");
        LuckPerms luckPerms = new LuckPerms(
                lpEnabled,
                noDrainPermission,
                multiplierPrefix,
                minMultiplier,
                maxMultiplier,
                stateContextKey,
                regionContextKey,
                drainContextKey,
                multiplierContextKey
        );

        ConfigurationSection wgSection = config.getConfigurationSection("worldguard");
        boolean wgEnabled = getBoolean(wgSection, "enabled", true);
        ConfigurationSection wgFlags = wgSection == null ? null : wgSection.getConfigurationSection("flags");
        WorldGuard worldGuard = new WorldGuard(
                wgEnabled,
                getString(wgFlags, "no-drain", "wstamina-no-drain").toLowerCase(Locale.ROOT),
                getString(wgFlags, "force-zero", "wstamina-force-zero").toLowerCase(Locale.ROOT)
        );

        ConfigurationSection placeholdersSection = config.getConfigurationSection("placeholders");
        ConfigurationSection papiSection = placeholdersSection == null ? null : placeholdersSection.getConfigurationSection("papi");
        ConfigurationSection betterHudSection = placeholdersSection == null ? null : placeholdersSection.getConfigurationSection("betterhud");
        Placeholders placeholders = new Placeholders(
                getBoolean(papiSection, "enabled", true),
                getString(papiSection, "identifier", "wstamina").toLowerCase(Locale.ROOT),
                getBoolean(betterHudSection, "enabled", true),
                getString(betterHudSection, "namespace", "wstamina").toLowerCase(Locale.ROOT)
        );

        EnumMap<DebugModule, Boolean> debugMap = new EnumMap<>(DebugModule.class);
        ConfigurationSection debugSection = config.getConfigurationSection("debug");
        for (DebugModule module : DebugModule.values()) {
            debugMap.put(module, getBoolean(debugSection, module.configKey(), false));
        }

        return new PluginSettings(stamina, luckPerms, worldGuard, placeholders, Map.copyOf(debugMap));
    }

    public Stamina stamina() {
        return stamina;
    }

    public LuckPerms luckPerms() {
        return luckPerms;
    }

    public WorldGuard worldGuard() {
        return worldGuard;
    }

    public Placeholders placeholders() {
        return placeholders;
    }

    public Map<DebugModule, Boolean> debugModules() {
        return debugModules;
    }

    private static String getString(ConfigurationSection section, String path, String fallback) {
        if (section == null) {
            return fallback;
        }
        String value = section.getString(path);
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value;
    }

    private static boolean getBoolean(ConfigurationSection section, String path, boolean fallback) {
        if (section == null) {
            return fallback;
        }
        return section.getBoolean(path, fallback);
    }

    private static double getDouble(ConfigurationSection section, String path, double fallback) {
        if (section == null) {
            return fallback;
        }
        return section.getDouble(path, fallback);
    }

    public record Stamina(
            double maxPoints,
            double drainPerTick,
            double regenPerTick,
            int regenDelayTicks,
            boolean regenWhileWalking,
            double movementThreshold,
            long tickInterval
    ) {
    }

    public record LuckPerms(
            boolean enabled,
            String noDrainPermission,
            String multiplierPermissionPrefix,
            double minMultiplier,
            double maxMultiplier,
            String stateContextKey,
            String regionContextKey,
            String drainContextKey,
            String multiplierContextKey
    ) {
    }

    public record WorldGuard(boolean enabled, String noDrainFlag, String forceZeroFlag) {
    }

    public record Placeholders(
            boolean papiEnabled,
            String papiIdentifier,
            boolean betterHudEnabled,
            String betterHudNamespace
    ) {
    }
}
