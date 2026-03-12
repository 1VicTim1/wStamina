package org.willowstudio.ru.wStamina.stamina;

public record StaminaModifier(double multiplier, boolean noDrain) {
    public static final StaminaModifier DEFAULT = new StaminaModifier(1.0D, false);
}
