package org.willowstudio.ru.wStamina.provider;

import org.bukkit.entity.Player;
import org.willowstudio.ru.wStamina.stamina.RegionStaminaMode;

import java.util.UUID;

public interface RegionModeProvider {
    RegionStaminaMode resolveMode(Player player);

    default void invalidatePlayer(UUID playerId) {
    }

    default void invalidateAll() {
    }
}
