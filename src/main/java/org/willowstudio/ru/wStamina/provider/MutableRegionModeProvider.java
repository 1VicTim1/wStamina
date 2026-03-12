package org.willowstudio.ru.wStamina.provider;

import org.bukkit.entity.Player;
import org.willowstudio.ru.wStamina.stamina.RegionStaminaMode;

import java.util.Objects;
import java.util.UUID;

public final class MutableRegionModeProvider implements RegionModeProvider {
    private volatile RegionModeProvider delegate = new NoOpRegionModeProvider();

    public void setDelegate(RegionModeProvider delegate) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }

    @Override
    public RegionStaminaMode resolveMode(Player player) {
        return delegate.resolveMode(player);
    }

    @Override
    public void invalidatePlayer(UUID playerId) {
        delegate.invalidatePlayer(playerId);
    }

    @Override
    public void invalidateAll() {
        delegate.invalidateAll();
    }
}
