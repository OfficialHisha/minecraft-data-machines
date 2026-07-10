package com.hishacorp.dataMachines.listeners;

import com.hishacorp.dataMachines.core.Machine;
import com.hishacorp.dataMachines.core.MachineManager;
import com.nexomc.nexo.api.events.furniture.NexoFurnitureBreakEvent;
import com.nexomc.nexo.api.events.furniture.NexoFurnitureInteractEvent;
import com.nexomc.nexo.api.events.furniture.NexoFurniturePlaceEvent;
import com.nexomc.nexo.mechanics.furniture.FurnitureMechanic;
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
        event.getPlayer().openInventory(machineManager.getMachine(loc).getInventory());
    }
}