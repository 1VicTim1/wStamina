package org.willowstudio.ru.wStamina.provider;

import org.bukkit.entity.Player;
import org.willowstudio.ru.wStamina.stamina.RegionStaminaMode;

public interface RegionModeProvider {
    RegionStaminaMode resolveMode(Player player);
}
