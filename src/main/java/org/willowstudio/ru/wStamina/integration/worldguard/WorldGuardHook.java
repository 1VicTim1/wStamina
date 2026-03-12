package org.willowstudio.ru.wStamina.integration.worldguard;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagConflictException;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.willowstudio.ru.wStamina.config.Lang;
import org.willowstudio.ru.wStamina.config.PluginSettings;
import org.willowstudio.ru.wStamina.logging.DebugLogger;
import org.willowstudio.ru.wStamina.logging.DebugModule;
import org.willowstudio.ru.wStamina.provider.RegionModeProvider;
import org.willowstudio.ru.wStamina.stamina.RegionStaminaMode;

public final class WorldGuardHook implements RegionModeProvider {
    private final DebugLogger debugLogger;
    private final Lang lang;
    private PluginSettings.WorldGuard settings;

    private StateFlag noDrainFlag;
    private StateFlag forceZeroFlag;
    private WorldGuardPlugin worldGuardPlugin;
    private boolean hooked;

    public WorldGuardHook(DebugLogger debugLogger, Lang lang, PluginSettings.WorldGuard settings) {
        this.debugLogger = debugLogger;
        this.lang = lang;
        this.settings = settings;
    }

    public void registerFlagsOnLoad() {
        if (!settings.enabled()) {
            debugLogger.log(DebugModule.HOOKS, "WorldGuard hook disabled in config; skip flag registration.");
            return;
        }

        if (Bukkit.getPluginManager().getPlugin("WorldGuard") == null) {
            debugLogger.log(DebugModule.HOOKS, "WorldGuard plugin not found during onLoad; skip flag registration.");
            return;
        }

        try {
            FlagRegistry registry = WorldGuard.getInstance().getFlagRegistry();
            noDrainFlag = registerOrGetStateFlag(registry, settings.noDrainFlag());
            forceZeroFlag = registerOrGetStateFlag(registry, settings.forceZeroFlag());
            debugLogger.log(DebugModule.WORLDGUARD, () ->
                    "Flags registered: noDrain=" + noDrainFlag.getName() + ", forceZero=" + forceZeroFlag.getName());
        } catch (Exception exception) {
            lang.warning("messages.hooks.worldguard.register-flags-failed", java.util.Map.of("reason", String.valueOf(exception.getMessage())));
            debugLogger.log(DebugModule.WORLDGUARD, () -> "Flag registration error: " + exception);
        }
    }

    public void initialize() {
        hooked = false;
        if (!settings.enabled()) {
            return;
        }
        if (Bukkit.getPluginManager().getPlugin("WorldGuard") == null) {
            return;
        }

        Plugin rawPlugin = Bukkit.getPluginManager().getPlugin("WorldGuard");
        if (!(rawPlugin instanceof WorldGuardPlugin detectedWorldGuardPlugin)) {
            lang.warning("messages.hooks.worldguard.plugin-type-mismatch");
            return;
        }
        this.worldGuardPlugin = detectedWorldGuardPlugin;

        try {
            FlagRegistry registry = WorldGuard.getInstance().getFlagRegistry();
            if (noDrainFlag == null) {
                noDrainFlag = resolveStateFlag(registry, settings.noDrainFlag());
            }
            if (forceZeroFlag == null) {
                forceZeroFlag = resolveStateFlag(registry, settings.forceZeroFlag());
            }
            if (noDrainFlag == null || forceZeroFlag == null) {
                lang.warning("messages.hooks.worldguard.flags-unavailable");
                return;
            }
            hooked = true;
            debugLogger.log(DebugModule.HOOKS, "WorldGuard hook initialized.");
        } catch (Exception exception) {
            lang.warning("messages.hooks.worldguard.init-failed", java.util.Map.of("reason", String.valueOf(exception.getMessage())));
            debugLogger.log(DebugModule.WORLDGUARD, () -> "Initialization error: " + exception);
        }
    }

    public void shutdown() {
        hooked = false;
        worldGuardPlugin = null;
    }

    public void reloadSettings(PluginSettings.WorldGuard settings) {
        boolean namesChanged = !this.settings.noDrainFlag().equalsIgnoreCase(settings.noDrainFlag())
                || !this.settings.forceZeroFlag().equalsIgnoreCase(settings.forceZeroFlag());
        this.settings = settings;
        if (namesChanged) {
            lang.warning("messages.hooks.worldguard.flags-changed-restart");
        }
    }

    public boolean isHooked() {
        return hooked;
    }

    @Override
    public RegionStaminaMode resolveMode(Player player) {
        if (!hooked || worldGuardPlugin == null || noDrainFlag == null || forceZeroFlag == null) {
            return RegionStaminaMode.NORMAL;
        }

        try {
            LocalPlayer localPlayer = worldGuardPlugin.wrapPlayer(player);
            ApplicableRegionSet regions = WorldGuard.getInstance()
                    .getPlatform()
                    .getRegionContainer()
                    .createQuery()
                    .getApplicableRegions(BukkitAdapter.adapt(player.getLocation()));

            StateFlag.State forceZero = regions.queryState(localPlayer, forceZeroFlag);
            if (forceZero == StateFlag.State.ALLOW) {
                return RegionStaminaMode.FORCE_ZERO;
            }

            StateFlag.State noDrain = regions.queryState(localPlayer, noDrainFlag);
            if (noDrain == StateFlag.State.ALLOW) {
                return RegionStaminaMode.NO_DRAIN;
            }
        } catch (Exception exception) {
            debugLogger.log(DebugModule.WORLDGUARD, () ->
                    "Failed to resolve region mode for " + player.getName() + ": " + exception.getMessage());
        }

        return RegionStaminaMode.NORMAL;
    }

    private StateFlag registerOrGetStateFlag(FlagRegistry registry, String name) {
        StateFlag existing = resolveStateFlag(registry, name);
        if (existing != null) {
            return existing;
        }

        StateFlag created = new StateFlag(name, false);
        try {
            registry.register(created);
            return created;
        } catch (FlagConflictException conflictException) {
            StateFlag afterConflict = resolveStateFlag(registry, name);
            if (afterConflict != null) {
                return afterConflict;
            }
            throw conflictException;
        }
    }

    private StateFlag resolveStateFlag(FlagRegistry registry, String name) {
        Flag<?> existing = registry.get(name);
        if (existing == null) {
            return null;
        }
        if (existing instanceof StateFlag stateFlag) {
            return stateFlag;
        }
        throw new IllegalStateException("Flag '" + name + "' exists and is not a StateFlag.");
    }
}
