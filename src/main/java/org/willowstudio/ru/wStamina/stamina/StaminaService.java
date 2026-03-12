package org.willowstudio.ru.wStamina.stamina;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.willowstudio.ru.wStamina.config.PluginSettings;
import org.willowstudio.ru.wStamina.logging.DebugLogger;
import org.willowstudio.ru.wStamina.logging.DebugModule;
import org.willowstudio.ru.wStamina.provider.RegionModeProvider;
import org.willowstudio.ru.wStamina.provider.StaminaPermissionProvider;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class StaminaService {
    private static final double EPSILON = 0.0001D;
    private static final StaminaContextSnapshot DEFAULT_CONTEXT =
            new StaminaContextSnapshot("normal", RegionStaminaMode.NORMAL.contextValue(), "active", "1.0");

    private final JavaPlugin plugin;
    private final DebugLogger debugLogger;
    private final RegionModeProvider worldGuardHook;
    private final StaminaPermissionProvider luckPermsHook;
    private final Map<UUID, PlayerStaminaState> playerState = new ConcurrentHashMap<>();

    private PluginSettings.Stamina settings;
    private BukkitTask task;
    private long logicalTick;

    public StaminaService(
            JavaPlugin plugin,
            PluginSettings.Stamina settings,
            DebugLogger debugLogger,
            RegionModeProvider worldGuardHook,
            StaminaPermissionProvider luckPermsHook
    ) {
        this.plugin = plugin;
        this.settings = settings;
        this.debugLogger = debugLogger;
        this.worldGuardHook = worldGuardHook;
        this.luckPermsHook = luckPermsHook;
        this.logicalTick = 0L;
    }

    public void start() {
        stop();
        logicalTick = 0L;
        long interval = settings.tickInterval();
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, interval, interval);
        debugLogger.log(DebugModule.CORE, () -> "Stamina task started with interval=" + interval + " ticks.");
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    public void updateSettings(PluginSettings.Stamina settings) {
        this.settings = settings;
        start();
    }

    public void handleJoin(Player player) {
        createState(player);
        debugLogger.log(DebugModule.STAMINA, () -> "Initialized state for " + player.getName());
    }

    public void handleQuit(Player player) {
        playerState.remove(player.getUniqueId());
        debugLogger.log(DebugModule.STAMINA, () -> "Removed state for " + player.getName());
    }

    public void handleRespawn(Player player) {
        PlayerStaminaState state = createState(player);
        state.stamina(state.maxStamina());
        state.wasSprinting(false);
        state.regenBlockedUntilTick(logicalTick);
        debugLogger.log(DebugModule.STAMINA, () -> "Respawn reset stamina for " + player.getName());
    }

    public void handleSprintStop(Player player) {
        PlayerStaminaState state = createState(player);
        state.wasSprinting(false);
        state.sprintInputActive(false);
        state.regenBlockedUntilTick(logicalTick + settings.regenDelayTicks());
        debugLogger.log(DebugModule.STAMINA, () -> "Sprint stop cooldown set for " + player.getName());
    }

    public boolean canStartSprint(Player player) {
        PlayerStaminaState state = createState(player);
        RegionStaminaMode mode = worldGuardHook.resolveMode(player);
        if (mode == RegionStaminaMode.FORCE_ZERO) {
            if (state.stamina() > 0.0D) {
                state.stamina(0.0D);
            }
            state.regionMode(mode);
            state.regenBlockedUntilTick(logicalTick + settings.regenDelayTicks());
            updateContextSnapshot(player, state, true);
            return false;
        }
        if (state.stamina() <= EPSILON) {
            state.stamina(0.0D);
            state.regenBlockedUntilTick(logicalTick + settings.regenDelayTicks());
            return false;
        }
        return state.stamina() > EPSILON;
    }

    public void handleSprintStartDenied(Player player) {
        PlayerStaminaState state = createState(player);
        state.wasSprinting(false);
        state.regenBlockedUntilTick(logicalTick + settings.regenDelayTicks());
    }

    public void handleInput(Player player, boolean sprintInputActive) {
        PlayerStaminaState state = createState(player);
        state.sprintInputActive(sprintInputActive);
        if (!sprintInputActive) {
            return;
        }
        if (state.stamina() <= EPSILON || state.regionMode() == RegionStaminaMode.FORCE_ZERO) {
            state.stamina(0.0D);
            state.regenBlockedUntilTick(logicalTick + settings.regenDelayTicks());
            player.setSprinting(false);
        }
    }

    public boolean enforceSprintLock(Player player) {
        if (!player.isSprinting()) {
            return false;
        }
        if (canStartSprint(player)) {
            return false;
        }

        player.setSprinting(false);
        handleSprintStartDenied(player);
        debugLogger.log(DebugModule.STAMINA, () -> "Sprint lock enforced for " + player.getName());
        return true;
    }

    public StaminaPlayerSnapshot snapshot(Player player) {
        PlayerStaminaState state = createState(player);
        double current = clampToRange(state.stamina(), state.maxStamina());
        double max = Math.max(state.maxStamina(), EPSILON);
        double percent = (current / max) * 100.0D;
        boolean exhausted = current <= EPSILON || state.regionMode() == RegionStaminaMode.FORCE_ZERO;
        return new StaminaPlayerSnapshot(
                current,
                state.maxStamina(),
                percent,
                state.multiplier(),
                state.noDrainPermission(),
                state.regionMode(),
                exhausted
        );
    }

    public StaminaContextSnapshot contextSnapshot(UUID playerUuid) {
        PlayerStaminaState state = playerState.get(playerUuid);
        if (state == null) {
            return DEFAULT_CONTEXT;
        }
        StaminaContextSnapshot snapshot = state.contextSnapshot();
        return snapshot == null ? DEFAULT_CONTEXT : snapshot;
    }

    public void setStamina(Player player, double value) {
        PlayerStaminaState state = createState(player);
        state.stamina(clampToRange(value, state.maxStamina()));
    }

    private void tick() {
        logicalTick += settings.tickInterval();
        for (Player player : Bukkit.getOnlinePlayers()) {
            tickPlayer(player);
        }
    }

    private void tickPlayer(Player player) {
        PlayerStaminaState state = createState(player);
        StaminaModifier modifier = luckPermsHook.resolveModifier(player);
        RegionStaminaMode mode = worldGuardHook.resolveMode(player);

        double effectiveMax = Math.max(EPSILON, settings.maxPoints() * modifier.multiplier());
        state.maxStamina(effectiveMax);
        state.multiplier(modifier.multiplier());
        state.noDrainPermission(modifier.noDrain());

        if (state.regionMode() != mode) {
            debugLogger.log(DebugModule.WORLDGUARD, () ->
                    "Region mode changed for " + player.getName() + ": " + state.regionMode() + " -> " + mode);
        }
        state.regionMode(mode);

        if (state.stamina() > effectiveMax) {
            state.stamina(effectiveMax);
        }

        if (mode == RegionStaminaMode.FORCE_ZERO) {
            if (state.stamina() != 0.0D) {
                state.stamina(0.0D);
                debugLogger.log(DebugModule.STAMINA, () -> "Forced zero stamina for " + player.getName());
            }
            if (player.isSprinting()) {
                player.setSprinting(false);
            }
            state.wasSprinting(false);
            state.regenBlockedUntilTick(logicalTick + settings.regenDelayTicks());
            updateContextSnapshot(player, state, true);
            return;
        }

        boolean sprinting = player.isSprinting();
        boolean exhaustedSprintAttempt = false;
        if ((sprinting || state.sprintInputActive()) && state.stamina() <= EPSILON) {
            exhaustedSprintAttempt = true;
            player.setSprinting(false);
            sprinting = false;
            state.stamina(0.0D);
        }

        boolean drainBlocked = mode == RegionStaminaMode.NO_DRAIN || modifier.noDrain();
        double beforeTick = state.stamina();

        if (sprinting && !drainBlocked) {
            double drain = settings.drainPerTick() * settings.tickInterval();
            state.stamina(Math.max(0.0D, state.stamina() - drain));
            if (state.stamina() <= EPSILON) {
                exhaustedSprintAttempt = true;
                state.stamina(0.0D);
                player.setSprinting(false);
                sprinting = false;
                debugLogger.log(DebugModule.STAMINA, () -> "Stamina exhausted; sprint stopped for " + player.getName());
            }
        }

        if (exhaustedSprintAttempt || (!sprinting && state.wasSprinting())) {
            state.regenBlockedUntilTick(logicalTick + settings.regenDelayTicks());
        }

        if (!sprinting
                && !exhaustedSprintAttempt
                && logicalTick >= state.regenBlockedUntilTick()
                && canRegenerateWhileMoving(player)) {
            double regen = settings.regenPerTick() * settings.tickInterval();
            if (regen > 0.0D) {
                state.stamina(Math.min(state.maxStamina(), state.stamina() + regen));
            }
        }

        if (Math.abs(state.stamina() - beforeTick) > EPSILON) {
            double current = state.stamina();
            debugLogger.log(DebugModule.STAMINA, () ->
                    "Tick stamina for " + player.getName() + ": " + formatDecimal(beforeTick) + " -> " + formatDecimal(current));
        }

        state.wasSprinting(sprinting);
        updateContextSnapshot(player, state, drainBlocked);
    }

    private boolean canRegenerateWhileMoving(Player player) {
        if (settings.regenWhileWalking()) {
            return true;
        }
        Vector velocity = player.getVelocity();
        double horizontalSpeedSquared = velocity.getX() * velocity.getX() + velocity.getZ() * velocity.getZ();
        double thresholdSquared = settings.movementThreshold() * settings.movementThreshold();
        return horizontalSpeedSquared <= thresholdSquared;
    }

    private PlayerStaminaState createState(Player player) {
        return playerState.computeIfAbsent(player.getUniqueId(), uuid -> new PlayerStaminaState(settings.maxPoints()));
    }

    private void updateContextSnapshot(Player player, PlayerStaminaState state, boolean drainBlocked) {
        boolean exhausted = state.stamina() <= EPSILON || state.regionMode() == RegionStaminaMode.FORCE_ZERO;
        String staminaState = exhausted ? "exhausted" : "normal";
        String drainState = drainBlocked || state.regionMode() == RegionStaminaMode.FORCE_ZERO ? "blocked" : "active";
        String multiplierValue = formatDecimal(state.multiplier());
        StaminaContextSnapshot next = new StaminaContextSnapshot(
                staminaState,
                state.regionMode().contextValue(),
                drainState,
                multiplierValue
        );

        StaminaContextSnapshot previous = state.contextSnapshot();
        if (next.equals(previous)) {
            return;
        }

        state.contextSnapshot(next);
        luckPermsHook.signalContextUpdate(player);
        debugLogger.log(DebugModule.LUCKPERMS, () ->
                "Context updated for " + player.getName() + ": " + previous + " -> " + next);
    }

    private static double clampToRange(double value, double max) {
        return Math.max(0.0D, Math.min(max, value));
    }

    private static String formatDecimal(double value) {
        BigDecimal decimal = BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros();
        return decimal.toPlainString();
    }
}
