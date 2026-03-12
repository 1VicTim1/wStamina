package org.willowstudio.ru.wStamina.integration.betterhud;

import kr.toxicity.hud.api.BetterHud;
import kr.toxicity.hud.api.manager.PlaceholderManager;
import kr.toxicity.hud.api.placeholder.HudPlaceholder;
import kr.toxicity.hud.api.placeholder.PlaceholderContainer;
import kr.toxicity.hud.api.player.HudPlayer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.willowstudio.ru.wStamina.config.Lang;
import org.willowstudio.ru.wStamina.config.PluginSettings;
import org.willowstudio.ru.wStamina.logging.DebugLogger;
import org.willowstudio.ru.wStamina.logging.DebugModule;
import org.willowstudio.ru.wStamina.stamina.RegionStaminaMode;
import org.willowstudio.ru.wStamina.stamina.StaminaPlayerSnapshot;
import org.willowstudio.ru.wStamina.stamina.StaminaService;

import java.util.Locale;

public final class BetterHudHook {
    private final DebugLogger debugLogger;
    private final Lang lang;
    private PluginSettings.Placeholders settings;
    private StaminaService staminaService;
    private boolean hooked;

    public BetterHudHook(DebugLogger debugLogger, Lang lang, PluginSettings.Placeholders settings) {
        this.debugLogger = debugLogger;
        this.lang = lang;
        this.settings = settings;
    }

    public void initialize(StaminaService staminaService) {
        this.staminaService = staminaService;
        hooked = false;

        if (!settings.betterHudEnabled()) {
            debugLogger.log(DebugModule.HOOKS, "BetterHud hook disabled in config.");
            return;
        }
        if (Bukkit.getPluginManager().getPlugin("BetterHud") == null) {
            debugLogger.log(DebugModule.HOOKS, "BetterHud plugin not found.");
            return;
        }

        try {
            PlaceholderManager manager = BetterHud.getInstance().getPlaceholderManager();
            String namespace = settings.betterHudNamespace().toLowerCase(Locale.ROOT);

            register(manager.getNumberContainer(), namespace + "_current", directPlaceholder(this::current));
            register(manager.getNumberContainer(), namespace + "_max", directPlaceholder(this::max));
            register(manager.getNumberContainer(), namespace + "_percent", directPlaceholder(this::percent));
            register(manager.getNumberContainer(), namespace + "_multiplier", directPlaceholder(this::multiplier));

            register(manager.getBooleanContainer(), namespace + "_exhausted", directPlaceholder(this::exhausted));
            register(manager.getBooleanContainer(), namespace + "_nodrain", directPlaceholder(this::noDrain));

            register(manager.getStringContainer(), namespace + "_state", directPlaceholder(this::state));
            register(manager.getStringContainer(), namespace + "_region", directPlaceholder(this::region));

            hooked = true;
            debugLogger.log(DebugModule.HOOKS, "BetterHud hook initialized.");
        } catch (Exception exception) {
            lang.warning("messages.hooks.betterhud.init-failed", java.util.Map.of("reason", String.valueOf(exception.getMessage())));
            debugLogger.log(DebugModule.HOOKS, () -> "BetterHud init error: " + exception);
        }
    }

    public void shutdown() {
        hooked = false;
    }

    public void reloadSettings(PluginSettings.Placeholders settings, StaminaService staminaService) {
        this.settings = settings;
        initialize(staminaService);
    }

    public boolean isHooked() {
        return hooked;
    }

    private <T> void register(PlaceholderContainer<T> container, String id, HudPlaceholder<T> placeholder) {
        if (container.getAllPlaceholders().containsKey(id)) {
            debugLogger.log(DebugModule.PLACEHOLDERS, () -> "BetterHud placeholder already exists: " + id);
            return;
        }
        container.addPlaceholder(id, placeholder);
        debugLogger.log(DebugModule.PLACEHOLDERS, () -> "Registered BetterHud placeholder: " + id);
    }

    private <T> HudPlaceholder<T> directPlaceholder(java.util.function.Function<HudPlayer, T> function) {
        return HudPlaceholder.of(HudPlaceholder.PlaceholderFunction.of(function));
    }

    private Number current(HudPlayer hudPlayer) {
        return snapshot(hudPlayer).current();
    }

    private Number max(HudPlayer hudPlayer) {
        return snapshot(hudPlayer).max();
    }

    private Number percent(HudPlayer hudPlayer) {
        return snapshot(hudPlayer).percent();
    }

    private Number multiplier(HudPlayer hudPlayer) {
        return snapshot(hudPlayer).multiplier();
    }

    private Boolean exhausted(HudPlayer hudPlayer) {
        return snapshot(hudPlayer).exhausted();
    }

    private Boolean noDrain(HudPlayer hudPlayer) {
        return snapshot(hudPlayer).noDrainPermission();
    }

    private String state(HudPlayer hudPlayer) {
        return snapshot(hudPlayer).exhausted() ? "exhausted" : "normal";
    }

    private String region(HudPlayer hudPlayer) {
        return snapshot(hudPlayer).regionMode().contextValue();
    }

    private StaminaPlayerSnapshot snapshot(HudPlayer hudPlayer) {
        Object handle = hudPlayer.handle();
        if (!(handle instanceof Player player) || staminaService == null) {
            return new StaminaPlayerSnapshot(0.0D, 0.0D, 0.0D, 1.0D, false, RegionStaminaMode.NORMAL, true);
        }
        StaminaPlayerSnapshot snapshot = staminaService.snapshot(player);
        debugLogger.log(DebugModule.PLACEHOLDERS, () ->
                "BetterHud request for " + player.getName() + ": " + snapshot.current() + "/" + snapshot.max());
        return snapshot;
    }
}
