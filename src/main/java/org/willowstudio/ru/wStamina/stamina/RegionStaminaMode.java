package org.willowstudio.ru.wStamina.stamina;

public enum RegionStaminaMode {
    NORMAL("normal"),
    NO_DRAIN("no_drain"),
    FORCE_ZERO("force_zero");

    private final String contextValue;

    RegionStaminaMode(String contextValue) {
        this.contextValue = contextValue;
    }

    public String contextValue() {
        return contextValue;
    }

    public boolean drainBlocked() {
        return this != NORMAL;
    }
}
