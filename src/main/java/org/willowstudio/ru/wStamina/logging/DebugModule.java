package org.willowstudio.ru.wStamina.logging;

public enum DebugModule {
    CORE("core"),
    COMMANDS("commands"),
    HOOKS("hooks"),
    PLACEHOLDERS("placeholders"),
    HEAT("heat"),
    PARTICLES("particles"),
    STAMINA("stamina"),
    WORLDGUARD("worldguard"),
    LUCKPERMS("luckperms");

    private final String configKey;

    DebugModule(String configKey) {
        this.configKey = configKey;
    }

    public String configKey() {
        return configKey;
    }
}
