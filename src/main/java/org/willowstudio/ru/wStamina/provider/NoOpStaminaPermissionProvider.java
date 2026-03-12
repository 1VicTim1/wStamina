package org.willowstudio.ru.wStamina.provider;

import org.bukkit.entity.Player;
import org.willowstudio.ru.wStamina.stamina.StaminaModifier;

public final class NoOpStaminaPermissionProvider implements StaminaPermissionProvider {
    @Override
    public StaminaModifier resolveModifier(Player player) {
        return StaminaModifier.DEFAULT;
    }

    @Override
    public void signalContextUpdate(Player player) {
    }
}
