package org.willowstudio.ru.wStamina.provider;

import org.bukkit.entity.Player;
import org.willowstudio.ru.wStamina.stamina.StaminaModifier;

import java.util.UUID;

public interface StaminaPermissionProvider {
    StaminaModifier resolveModifier(Player player);

    void signalContextUpdate(Player player);

    default void invalidatePlayer(UUID playerId) {
    }

    default void invalidateAll() {
    }
}
