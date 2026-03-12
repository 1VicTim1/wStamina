package org.willowstudio.ru.wStamina.command;

import org.bukkit.command.CommandSender;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.willowstudio.ru.wStamina.WStaminaPlugin;
import org.willowstudio.ru.wStamina.config.Lang;
import org.willowstudio.ru.wStamina.logging.DebugLogger;
import org.willowstudio.ru.wStamina.logging.DebugModule;
import org.willowstudio.ru.wStamina.stamina.StaminaPlayerSnapshot;
import org.willowstudio.ru.wStamina.stamina.StaminaService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class WStaminaCommand implements BasicCommand {
    private static final String ROOT_LABEL = "wstamina";
    private static final String RELOAD_PERMISSION = "wstamina.command.reload";

    private final WStaminaPlugin plugin;
    private final StaminaService staminaService;
    private final DebugLogger debugLogger;
    private final Lang lang;

    public WStaminaCommand(WStaminaPlugin plugin, StaminaService staminaService, DebugLogger debugLogger, Lang lang) {
        this.plugin = plugin;
        this.staminaService = staminaService;
        this.debugLogger = debugLogger;
        this.lang = lang;
    }

    @Override
    public void execute(@NotNull CommandSourceStack commandSourceStack, @NotNull String[] args) {
        CommandSender sender = commandSourceStack.getSender();
        if (args.length == 0) {
            if (sender instanceof Player player) {
                StaminaPlayerSnapshot snapshot = staminaService.snapshot(player);
                lang.send(sender, "messages.command.stamina-self", Map.of(
                        "current", format(snapshot.current()),
                        "max", format(snapshot.max()),
                        "percent", format(snapshot.percent())
                ));
                return;
            }
            lang.send(sender, "messages.command.usage", Map.of("label", ROOT_LABEL));
            return;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission(RELOAD_PERMISSION)) {
                lang.send(sender, "messages.command.no-permission");
                debugLogger.log(DebugModule.COMMANDS, () -> "Reload denied for " + sender.getName());
                return;
            }
            plugin.reloadPlugin();
            lang.send(sender, "messages.command.reloaded");
            debugLogger.log(DebugModule.COMMANDS, () -> "Reload executed by " + sender.getName());
            return;
        }

        lang.send(sender, "messages.command.usage", Map.of("label", ROOT_LABEL));
    }

    @Override
    public @NotNull Collection<String> suggest(@NotNull CommandSourceStack commandSourceStack, @NotNull String[] args) {
        if (args.length == 1
                && commandSourceStack.getSender().hasPermission(RELOAD_PERMISSION)
                && "reload".startsWith(args[0].toLowerCase(Locale.ROOT))) {
            return List.of("reload");
        }
        return List.of();
    }

    private static String format(double value) {
        return BigDecimal.valueOf(value)
                .setScale(2, RoundingMode.HALF_UP)
                .stripTrailingZeros()
                .toPlainString();
    }
}
