package org.willowstudio.ru.wStamina;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.willowstudio.ru.wStamina.command.WStaminaCommand;
import org.willowstudio.ru.wStamina.config.Lang;
import org.willowstudio.ru.wStamina.config.PluginSettings;
import org.willowstudio.ru.wStamina.integration.betterhud.BetterHudHook;
import org.willowstudio.ru.wStamina.integration.luckperms.LuckPermsHook;
import org.willowstudio.ru.wStamina.integration.placeholderapi.StaminaPlaceholderExpansion;
import org.willowstudio.ru.wStamina.integration.worldguard.WorldGuardHook;
import org.willowstudio.ru.wStamina.logging.DebugLogger;
import org.willowstudio.ru.wStamina.logging.DebugModule;
import org.willowstudio.ru.wStamina.provider.MutableRegionModeProvider;
import org.willowstudio.ru.wStamina.provider.MutableStaminaPermissionProvider;
import org.willowstudio.ru.wStamina.provider.NoOpRegionModeProvider;
import org.willowstudio.ru.wStamina.provider.NoOpStaminaPermissionProvider;
import org.willowstudio.ru.wStamina.stamina.StaminaListener;
import org.willowstudio.ru.wStamina.stamina.StaminaService;

public final class WStaminaPlugin extends JavaPlugin {
    private final MutableRegionModeProvider regionModeProvider = new MutableRegionModeProvider();
    private final MutableStaminaPermissionProvider staminaPermissionProvider = new MutableStaminaPermissionProvider();

    private Lang lang;
    private PluginSettings settings;
    private DebugLogger debugLogger;
    private StaminaService staminaService;

    private WorldGuardHook worldGuardHook;
    private LuckPermsHook luckPermsHook;
    private BetterHudHook betterHudHook;
    private StaminaPlaceholderExpansion placeholderExpansion;

    @Override
    public void onLoad() {
        saveDefaultConfig();
        reloadConfig();
        if (lang == null) {
            lang = new Lang(this);
        }
        lang.reload();
        settings = PluginSettings.load(getConfig());
        debugLogger = new DebugLogger(this, settings.debugModules());
        debugLogger.log(DebugModule.CORE, "onLoad start.");

        if (settings.worldGuard().enabled() && isPluginPresent("WorldGuard")) {
            try {
                worldGuardHook = new WorldGuardHook(debugLogger, lang, settings.worldGuard());
                worldGuardHook.registerFlagsOnLoad();
            } catch (Throwable throwable) {
                lang.warning("messages.hooks.worldguard.preload-failed", java.util.Map.of("reason", String.valueOf(throwable.getMessage())));
            }
        }
    }

    @Override
    public void onEnable() {
        if (lang == null) {
            lang = new Lang(this);
        }
        lang.reload();
        if (settings == null) {
            reloadConfig();
            settings = PluginSettings.load(getConfig());
        }
        if (debugLogger == null) {
            debugLogger = new DebugLogger(this, settings.debugModules());
        }

        if (staminaService == null) {
            staminaService = new StaminaService(this, settings.stamina(), debugLogger, regionModeProvider, staminaPermissionProvider);
            registerListenerAndCommand();
        } else {
            staminaService.updateSettings(settings.stamina());
        }

        applyWorldGuardHook();
        applyLuckPermsHook();
        applyPlaceholderHooks();

        for (Player player : Bukkit.getOnlinePlayers()) {
            staminaService.handleJoin(player);
        }
        staminaService.start();
        lang.info("messages.plugin.enabled");
        debugLogger.log(DebugModule.CORE, "Plugin enabled.");
    }

    @Override
    public void onDisable() {
        if (placeholderExpansion != null) {
            placeholderExpansion.unregister();
            placeholderExpansion = null;
        }
        if (betterHudHook != null) {
            betterHudHook.shutdown();
            betterHudHook = null;
        }
        if (luckPermsHook != null) {
            luckPermsHook.shutdown();
            luckPermsHook = null;
        }
        if (worldGuardHook != null) {
            worldGuardHook.shutdown();
        }
        if (staminaService != null) {
            staminaService.stop();
        }
        regionModeProvider.invalidateAll();
        staminaPermissionProvider.invalidateAll();
        if (lang != null) {
            lang.info("messages.plugin.disabled");
        }
    }

    public void reloadPlugin() {
        reloadConfig();
        lang.reload();
        settings = PluginSettings.load(getConfig());
        debugLogger.update(settings.debugModules());
        debugLogger.log(DebugModule.CORE, "Reload requested.");

        if (staminaService != null) {
            staminaService.updateSettings(settings.stamina());
        }
        applyWorldGuardHook();
        applyLuckPermsHook();
        applyPlaceholderHooks();
    }

    private void registerListenerAndCommand() {
        PluginManager pluginManager = getServer().getPluginManager();
        pluginManager.registerEvents(new StaminaListener(staminaService, debugLogger), this);

        WStaminaCommand executor = new WStaminaCommand(this, staminaService, debugLogger, lang);
        registerCommand("wstamina", "Main command for wStamina", java.util.List.of(), executor);
    }

    private void applyWorldGuardHook() {
        regionModeProvider.setDelegate(new NoOpRegionModeProvider());
        if (worldGuardHook != null) {
            worldGuardHook.reloadSettings(settings.worldGuard());
        }

        if (!settings.worldGuard().enabled() || !isPluginPresent("WorldGuard")) {
            if (worldGuardHook != null) {
                worldGuardHook.shutdown();
            }
            debugLogger.log(DebugModule.HOOKS, "WorldGuard hook disabled or plugin missing.");
            return;
        }

        if (worldGuardHook == null) {
            try {
                worldGuardHook = new WorldGuardHook(debugLogger, lang, settings.worldGuard());
            } catch (Throwable throwable) {
                lang.warning("messages.hooks.worldguard.create-failed", java.util.Map.of("reason", String.valueOf(throwable.getMessage())));
                return;
            }
        }

        try {
            worldGuardHook.initialize();
            if (worldGuardHook.isHooked()) {
                regionModeProvider.setDelegate(worldGuardHook);
            }
        } catch (Throwable throwable) {
            lang.warning("messages.hooks.worldguard.runtime-error", java.util.Map.of("reason", String.valueOf(throwable.getMessage())));
            regionModeProvider.setDelegate(new NoOpRegionModeProvider());
        }
    }

    private void applyLuckPermsHook() {
        staminaPermissionProvider.setDelegate(new NoOpStaminaPermissionProvider());

        if (!settings.luckPerms().enabled() || !isPluginPresent("LuckPerms")) {
            if (luckPermsHook != null) {
                luckPermsHook.shutdown();
                luckPermsHook = null;
            }
            debugLogger.log(DebugModule.HOOKS, "LuckPerms hook disabled or plugin missing.");
            return;
        }

        if (luckPermsHook == null) {
            try {
                luckPermsHook = new LuckPermsHook(debugLogger, lang, settings.luckPerms());
                luckPermsHook.initialize(staminaService);
            } catch (Throwable throwable) {
                lang.warning("messages.hooks.luckperms.create-failed", java.util.Map.of("reason", String.valueOf(throwable.getMessage())));
                luckPermsHook = null;
            }
        } else {
            luckPermsHook.reloadSettings(settings.luckPerms(), staminaService);
        }

        if (luckPermsHook != null) {
            staminaPermissionProvider.setDelegate(luckPermsHook);
        }
        boolean lpHooked = luckPermsHook != null && luckPermsHook.isHooked();
        debugLogger.log(DebugModule.HOOKS, () -> "LuckPerms hook active=" + lpHooked);
    }

    private void applyPlaceholderHooks() {
        if (placeholderExpansion != null) {
            placeholderExpansion.unregister();
            placeholderExpansion = null;
        }

        if (settings.placeholders().papiEnabled() && isPluginPresent("PlaceholderAPI")) {
            placeholderExpansion = new StaminaPlaceholderExpansion(
                    this,
                    staminaService,
                    debugLogger,
                    settings.placeholders().papiIdentifier()
            );
            boolean registered = placeholderExpansion.register();
            debugLogger.log(DebugModule.HOOKS, () -> "PAPI placeholder registration status=" + registered);
        } else {
            debugLogger.log(DebugModule.HOOKS, "PAPI hook disabled or plugin missing.");
        }

        if (!settings.placeholders().betterHudEnabled() || !isPluginPresent("BetterHud")) {
            if (betterHudHook != null) {
                betterHudHook.shutdown();
            }
            debugLogger.log(DebugModule.HOOKS, "BetterHud hook disabled or plugin missing.");
            return;
        }

        if (betterHudHook == null) {
            try {
                betterHudHook = new BetterHudHook(debugLogger, lang, settings.placeholders());
                betterHudHook.initialize(staminaService);
            } catch (Throwable throwable) {
                lang.warning("messages.hooks.betterhud.create-failed", java.util.Map.of("reason", String.valueOf(throwable.getMessage())));
                betterHudHook = null;
            }
        } else {
            betterHudHook.reloadSettings(settings.placeholders(), staminaService);
        }
        boolean betterHudHooked = betterHudHook != null && betterHudHook.isHooked();
        debugLogger.log(DebugModule.HOOKS, () -> "BetterHud hook active=" + betterHudHooked);
    }

    private boolean isPluginPresent(String pluginName) {
        return Bukkit.getPluginManager().getPlugin(pluginName) != null;
    }
}
