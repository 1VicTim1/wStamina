package org.willowstudio.ru.wStamina.logging;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Supplier;

public final class DebugLogger {
    private final JavaPlugin plugin;
    private final EnumMap<DebugModule, Boolean> enabledModules = new EnumMap<>(DebugModule.class);

    public DebugLogger(JavaPlugin plugin, Map<DebugModule, Boolean> enabled) {
        this.plugin = plugin;
        update(enabled);
    }

    public void update(Map<DebugModule, Boolean> enabled) {
        enabledModules.clear();
        for (DebugModule module : DebugModule.values()) {
            enabledModules.put(module, enabled.getOrDefault(module, false));
        }
    }

    public boolean isEnabled(DebugModule module) {
        return enabledModules.getOrDefault(module, false);
    }

    public void log(DebugModule module, String message) {
        if (isEnabled(module)) {
            plugin.getLogger().info("[debug/" + module.configKey() + "] " + message);
        }
    }

    public void log(DebugModule module, Supplier<String> messageSupplier) {
        if (isEnabled(module)) {
            log(module, messageSupplier.get());
        }
    }
}
