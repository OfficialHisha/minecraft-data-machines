package com.hishacorp.dataMachines.listeners;

import com.hishacorp.dataMachines.core.Machine;
import com.hishacorp.dataMachines.core.MachineManager;
import com.nexomc.nexo.api.NexoItems;
import com.nexomc.nexo.api.events.furniture.NexoFurnitureBreakEvent;
import com.nexomc.nexo.api.events.furniture.NexoFurnitureInteractEvent;
import com.nexomc.nexo.api.events.furniture.NexoFurniturePlaceEvent;
import com.nexomc.nexo.mechanics.furniture.FurnitureMechanic;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InventoryListener implements Listener {
    private static final Logger log = LoggerFactory.getLogger(InventoryListener.class);

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        log.debug("onInventoryClick");
        if (!(event.getInventory().getHolder() instanceof Machine.MachineInventoryHolder(Machine machine))) return;

        int slot = event.getRawSlot();
        
        if (slot < 0 || slot >= 27) return;
        
        if (machine.getFuelSlots().contains(slot)) {
            ItemStack item = event.getCursor();
            if (!isFuelItem(item, machine.getType().properties().fuelItems())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        log.debug("onInventoryDrag");
        if (!(event.getInventory().getHolder() instanceof Machine.MachineInventoryHolder(Machine machine))) return;

        java.util.List<String> fuelItems = machine.getType().properties().fuelItems();
        
        for (int slot : event.getRawSlots()) {
            if (slot >= 0 && slot < 27 && machine.getFuelSlots().contains(slot)) {
                ItemStack item = event.getWhoClicked().getActiveItem();
                if (!isFuelItem(item, fuelItems)) {
                    event.setCancelled(true);
                    break;
                }
            }
        }
    }

    private boolean isFuelItem(ItemStack item, java.util.List<String> fuelItems) {
        if (fuelItems == null || fuelItems.isEmpty()) return true;
        for (String fuelId : fuelItems) {
            if (isItemMatch(item, fuelId)) return true;
        }
        return false;
    }

    private boolean isItemMatch(ItemStack item, String itemID) {
        if (itemID.startsWith("nexo:")) {
            String nexoId = itemID.substring(5);
            return NexoItems.itemFromId(nexoId).build().isSimilar(item);
        }
        return item.getType().name().equalsIgnoreCase(itemID);
    }
}