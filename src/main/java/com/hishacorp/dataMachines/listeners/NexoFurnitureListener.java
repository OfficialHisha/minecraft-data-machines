package com.hishacorp.dataMachines.listeners;

import com.hishacorp.dataMachines.core.Machine;
import com.hishacorp.dataMachines.core.MachineManager;
import com.hishacorp.dataMachines.guards.GriefPreventionGuard;
import com.nexomc.nexo.api.events.furniture.NexoFurnitureBreakEvent;
import com.nexomc.nexo.api.events.furniture.NexoFurnitureInteractEvent;
import com.nexomc.nexo.api.events.furniture.NexoFurniturePlaceEvent;
import com.nexomc.nexo.mechanics.furniture.FurnitureMechanic;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NexoFurnitureListener implements Listener {
    private static final Logger log = LoggerFactory.getLogger(NexoFurnitureListener.class);
    private final MachineManager machineManager;

    public NexoFurnitureListener(MachineManager machineManager) {
        this.machineManager = machineManager;
    }

    @EventHandler
    public void onFurniturePlace(NexoFurniturePlaceEvent event) {
        log.debug("onFurniturePlace");
        FurnitureMechanic mechanic = event.getMechanic();
        String furnitureType = mechanic.getItemID();

        machineManager.getConfigManager()
            .getMachineTypes().values().stream()
            .filter(type -> type.id().equalsIgnoreCase(furnitureType))
            .findFirst().ifPresent(machineType -> {
                Location loc = event.getBlock().getLocation();

                    Machine machine = new Machine(loc, machineType);
                    machineManager.addMachine(machine);
                    machineManager.associateWithEntity(machine, event.getBaseEntity());
            });
    }

    @EventHandler
    public void onFurnitureBreak(NexoFurnitureBreakEvent event) {
        log.debug("onFurnitureBreak");

        machineManager.removeMachine(event.getBaseEntity().getLocation().getBlock().getLocation());
    }

    @EventHandler
    public void onFurnitureInteract(NexoFurnitureInteractEvent event) {
        log.debug("onFurnitureInteract");

        Location loc = event.getBaseEntity().getLocation().getBlock().getLocation();
        Machine machine = machineManager.getMachine(loc);
        
        if (machine == null) return;

        if (!GriefPreventionGuard.canInteract(event.getPlayer(), loc)) {
            event.getPlayer().sendMessage(Component.text("You do not have permission to use this machine in this claim."));
            return;
        }

        String permission = machine.getType().properties().requiresPermission();
        if (permission != null && !permission.isEmpty() && !event.getPlayer().hasPermission(permission)) {
            event.getPlayer().sendMessage(Component.text("You are not able to use this machine."));
            return;
        }

        event.getPlayer().openInventory(machine.getInventory());
    }
}