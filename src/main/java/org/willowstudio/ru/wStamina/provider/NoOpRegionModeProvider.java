package org.willowstudio.ru.wStamina.provider;

import org.bukkit.entity.Player;
import org.willowstudio.ru.wStamina.stamina.RegionStaminaMode;

public final class NoOpRegionModeProvider implements RegionModeProvider {
    @Override
    public RegionStaminaMode resolveMode(Player player) {
        return RegionStaminaMode.NORMAL;
    }
}
