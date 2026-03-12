package org.willowstudio.ru.wStamina.provider;

import org.bukkit.entity.Player;
import org.willowstudio.ru.wStamina.stamina.StaminaModifier;

public interface StaminaPermissionProvider {
    StaminaModifier resolveModifier(Player player);

    void signalContextUpdate(Player player);
}
