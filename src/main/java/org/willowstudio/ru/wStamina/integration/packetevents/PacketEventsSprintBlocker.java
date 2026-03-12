package org.willowstudio.ru.wStamina.integration.packetevents;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientEntityAction;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerInput;
import org.willowstudio.ru.wStamina.logging.DebugLogger;
import org.willowstudio.ru.wStamina.logging.DebugModule;
import org.willowstudio.ru.wStamina.stamina.StaminaService;

import java.util.UUID;

public final class PacketEventsSprintBlocker {
    private final StaminaService staminaService;
    private final DebugLogger debugLogger;

    private PacketListenerAbstract listener;
    private boolean hooked;

    public PacketEventsSprintBlocker(StaminaService staminaService, DebugLogger debugLogger) {
        this.staminaService = staminaService;
        this.debugLogger = debugLogger;
    }

    public void initialize() {
        if (hooked) {
            return;
        }
        if (PacketEvents.getAPI() == null || !PacketEvents.getAPI().isInitialized()) {
            throw new IllegalStateException("PacketEvents API is not initialized yet");
        }

        listener = new PacketListenerAbstract(PacketListenerPriority.HIGHEST) {
            @Override
            public void onPacketReceive(PacketReceiveEvent event) {
                handlePacket(event);
            }
        };
        PacketEvents.getAPI().getEventManager().registerListener(listener);
        hooked = true;
        debugLogger.log(DebugModule.HOOKS, "PacketEvents sprint blocker registered.");
    }

    public void shutdown() {
        if (listener != null && PacketEvents.getAPI() != null) {
            PacketEvents.getAPI().getEventManager().unregisterListener(listener);
        }
        listener = null;
        hooked = false;
    }

    public boolean isHooked() {
        return hooked;
    }

    private void handlePacket(PacketReceiveEvent event) {
        if (event.isCancelled()) {
            return;
        }

        if (event.getPacketType() == PacketType.Play.Client.ENTITY_ACTION) {
            handleEntityAction(event);
            return;
        }

        if (event.getPacketType() == PacketType.Play.Client.PLAYER_INPUT) {
            handlePlayerInput(event);
        }
    }

    private void handleEntityAction(PacketReceiveEvent event) {
        User user = event.getUser();
        UUID playerUuid = user.getUUID();
        if (playerUuid == null) {
            return;
        }

        WrapperPlayClientEntityAction wrapper = new WrapperPlayClientEntityAction(event);
        WrapperPlayClientEntityAction.Action action = wrapper.getAction();
        if (action == WrapperPlayClientEntityAction.Action.START_SPRINTING) {
            boolean blocked = staminaService.handlePacketSprintStart(playerUuid, resolvePlayerName(user), "entity action");
            if (blocked) {
                event.setCancelled(true);
                debugLogger.log(DebugModule.STAMINA, () ->
                        "PacketEvents cancelled START_SPRINTING for " + resolvePlayerName(user));
            }
            return;
        }

        if (action == WrapperPlayClientEntityAction.Action.STOP_SPRINTING) {
            staminaService.handlePacketSprintInput(playerUuid, resolvePlayerName(user), false, "entity action");
        }
    }

    private void handlePlayerInput(PacketReceiveEvent event) {
        User user = event.getUser();
        UUID playerUuid = user.getUUID();
        if (playerUuid == null) {
            return;
        }

        WrapperPlayClientPlayerInput wrapper = new WrapperPlayClientPlayerInput(event);
        boolean sprintInput = wrapper.isSprint();
        boolean blocked = staminaService.handlePacketSprintInput(playerUuid, resolvePlayerName(user), sprintInput, "player input");
        if (!blocked || !sprintInput) {
            return;
        }

        wrapper.setSprint(false);
        event.markForReEncode(true);
    }

    private String resolvePlayerName(User user) {
        String playerName = user.getName();
        return playerName == null || playerName.isBlank() ? String.valueOf(user.getUUID()) : playerName;
    }
}
