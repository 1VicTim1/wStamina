package org.willowstudio.ru.wStamina.provider;

import org.bukkit.entity.Player;
import org.willowstudio.ru.wStamina.stamina.StaminaModifier;

import java.util.Objects;

public final class MutableStaminaPermissionProvider implements StaminaPermissionProvider {
    private volatile StaminaPermissionProvider delegate = new NoOpStaminaPermissionProvider();

    public void setDelegate(StaminaPermissionProvider delegate) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }

    @Override
    public StaminaModifier resolveModifier(Player player) {
        return delegate.resolveModifier(player);
    }

    @Override
    public void signalContextUpdate(Player player) {
        delegate.signalContextUpdate(player);
    }
}
