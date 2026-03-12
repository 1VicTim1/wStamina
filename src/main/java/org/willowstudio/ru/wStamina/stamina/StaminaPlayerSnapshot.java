package org.willowstudio.ru.wStamina.stamina;

public record StaminaPlayerSnapshot(
        double current,
        double max,
        double percent,
        double multiplier,
        boolean noDrainPermission,
        RegionStaminaMode regionMode,
        boolean exhausted
) {
}
