package org.willowstudio.ru.wStamina.stamina;

final class PlayerStaminaState {
    private double stamina;
    private double maxStamina;
    private double multiplier;
    private boolean noDrainPermission;
    private RegionStaminaMode regionMode;
    private long regenBlockedUntilTick;
    private boolean wasSprinting;
    private volatile StaminaContextSnapshot contextSnapshot;

    PlayerStaminaState(double maxStamina) {
        this.stamina = maxStamina;
        this.maxStamina = maxStamina;
        this.multiplier = 1.0D;
        this.noDrainPermission = false;
        this.regionMode = RegionStaminaMode.NORMAL;
        this.regenBlockedUntilTick = 0L;
        this.wasSprinting = false;
        this.contextSnapshot = new StaminaContextSnapshot("normal", RegionStaminaMode.NORMAL.contextValue(), "active", "1.0");
    }

    double stamina() {
        return stamina;
    }

    void stamina(double stamina) {
        this.stamina = stamina;
    }

    double maxStamina() {
        return maxStamina;
    }

    void maxStamina(double maxStamina) {
        this.maxStamina = maxStamina;
    }

    double multiplier() {
        return multiplier;
    }

    void multiplier(double multiplier) {
        this.multiplier = multiplier;
    }

    boolean noDrainPermission() {
        return noDrainPermission;
    }

    void noDrainPermission(boolean noDrainPermission) {
        this.noDrainPermission = noDrainPermission;
    }

    RegionStaminaMode regionMode() {
        return regionMode;
    }

    void regionMode(RegionStaminaMode regionMode) {
        this.regionMode = regionMode;
    }

    long regenBlockedUntilTick() {
        return regenBlockedUntilTick;
    }

    void regenBlockedUntilTick(long regenBlockedUntilTick) {
        this.regenBlockedUntilTick = regenBlockedUntilTick;
    }

    boolean wasSprinting() {
        return wasSprinting;
    }

    void wasSprinting(boolean wasSprinting) {
        this.wasSprinting = wasSprinting;
    }

    StaminaContextSnapshot contextSnapshot() {
        return contextSnapshot;
    }

    void contextSnapshot(StaminaContextSnapshot contextSnapshot) {
        this.contextSnapshot = contextSnapshot;
    }
}
