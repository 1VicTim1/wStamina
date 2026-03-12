package org.willowstudio.ru.wStamina.config;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public final class Lang {
    private static final String DEFAULT_PREFIX = "&7[&awStamina&7] ";
    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.legacyAmpersand();
    private static final PlainTextComponentSerializer PLAIN_TEXT_SERIALIZER = PlainTextComponentSerializer.plainText();

    private final JavaPlugin plugin;
    private FileConfiguration config;

    public Lang(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists() && !dataFolder.mkdirs()) {
            plugin.getLogger().warning("Failed to create plugin data folder: " + dataFolder.getAbsolutePath());
        }

        File langFile = new File(dataFolder, "lang.yml");
        if (!langFile.exists()) {
            plugin.saveResource("lang.yml", false);
        }

        YamlConfiguration loaded = YamlConfiguration.loadConfiguration(langFile);
        try (InputStream input = plugin.getResource("lang.yml")) {
            if (input != null) {
                YamlConfiguration defaults = YamlConfiguration.loadConfiguration(new InputStreamReader(input, StandardCharsets.UTF_8));
                loaded.setDefaults(defaults);
                loaded.options().copyDefaults(true);
            }
        } catch (Exception ignored) {
            // Runtime fallback to loaded values only.
        }

        this.config = loaded;
    }

    public void send(CommandSender sender, String path) {
        sender.sendMessage(renderComponent(path, Map.of()));
    }

    public void send(CommandSender sender, String path, Map<String, String> placeholders) {
        sender.sendMessage(renderComponent(path, placeholders));
    }

    public void info(String path) {
        plugin.getLogger().info(toPlainText(renderComponent(path, Map.of())));
    }

    public void info(String path, Map<String, String> placeholders) {
        plugin.getLogger().info(toPlainText(renderComponent(path, placeholders)));
    }

    public void warning(String path) {
        plugin.getLogger().warning(toPlainText(renderComponent(path, Map.of())));
    }

    public void warning(String path, Map<String, String> placeholders) {
        plugin.getLogger().warning(toPlainText(renderComponent(path, placeholders)));
    }

    public Component renderComponent(String path, Map<String, String> placeholders) {
        String prefix = message("prefix", DEFAULT_PREFIX);
        String raw = message(path, "&cMissing lang key: " + path);

        String result = raw.replace("%prefix%", prefix);
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            String placeholder = "%" + entry.getKey() + "%";
            result = result.replace(placeholder, entry.getValue());
        }
        return LEGACY_SERIALIZER.deserialize(result);
    }

    private String message(String path, String fallback) {
        if (config == null) {
            return fallback;
        }
        String value = config.getString(path);
        return value == null ? fallback : value;
    }

    private static String toPlainText(Component component) {
        return PLAIN_TEXT_SERIALIZER.serialize(component);
    }
}
