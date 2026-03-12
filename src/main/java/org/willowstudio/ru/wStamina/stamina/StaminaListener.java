package org.willowstudio.ru.wStamina.stamina;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerToggleSprintEvent;
import org.willowstudio.ru.wStamina.logging.DebugLogger;
import org.willowstudio.ru.wStamina.logging.DebugModule;

public final class StaminaListener implements Listener {
    private final StaminaService staminaService;
    private final DebugLogger debugLogger;

    public StaminaListener(StaminaService staminaService, DebugLogger debugLogger) {
        this.staminaService = staminaService;
        this.debugLogger = debugLogger;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        staminaService.handleJoin(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        staminaService.handleQuit(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(PlayerRespawnEvent event) {
        staminaService.handleRespawn(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onToggleSprint(PlayerToggleSprintEvent event) {
        Player player = event.getPlayer();
        if (!event.isSprinting()) {
            staminaService.handleSprintStop(player);
            return;
        }

        if (staminaService.canStartSprint(player)) {
            return;
        }

        event.setCancelled(true);
        player.setSprinting(false);
        staminaService.handleSprintStartDenied(player);
        debugLogger.log(DebugModule.STAMINA, () -> "Sprint start denied for " + player.getName());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        staminaService.enforceSprintLock(player);
    }
}
