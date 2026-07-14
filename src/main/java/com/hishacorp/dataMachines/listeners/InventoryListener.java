package com.hishacorp.dataMachines.listeners;

import com.hishacorp.dataMachines.core.Machine;
import com.nexomc.nexo.api.NexoItems;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.function.Predicate;

public class InventoryListener implements Listener {
    private static final Logger log = LoggerFactory.getLogger(InventoryListener.class);

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        log.debug("onInventoryClick");
        if (!(event.getInventory().getHolder() instanceof Machine.MachineInventoryHolder(Machine machine))) return;

        String action = event.getAction().name();
        int slot = event.getRawSlot();
        ItemStack cursorItem = event.getCursor();

        List<Integer> fuelSlots = machine.getFuelSlots();
        List<String> fuelItems = machine.getType().properties().fuelItems();

        if (slot >= 0 && slot < 27) {
            if (!isConfiguredSlot(machine, slot)) {
                event.setCancelled(true);
                return;
            }

            if (action.startsWith("PLACE_")) {
                if (fuelSlots.contains(slot)) {
                    if (!isFuelItem(cursorItem, fuelItems)) {
                        event.setCancelled(true);
                        return;
                    }
                }

                if (machine.getOutputSlots().contains(slot)) {
                    event.setCancelled(true);
                    return;
                }
            }
        }

        Inventory clickedInventory = event.getClickedInventory();

        if (clickedInventory != null && clickedInventory.getType() == InventoryType.PLAYER && "MOVE_TO_OTHER_INVENTORY".equals(action)) {
            ItemStack moving = event.getCurrentItem();
            if (moving == null || moving.getType().isAir())
                return;

            event.setCancelled(true);

            ItemStack remaining = moving.clone();

            // First try fuel slots
            remaining = insertIntoSlots(
                    event.getInventory(),
                    remaining,
                    fuelSlots,
                    item -> isFuelItem(item, fuelItems)
            );

            // Then try input slots
            remaining = insertIntoSlots(
                    event.getInventory(),
                    remaining,
                    machine.getInputSlots(),
                    _ -> true
            );

            if (remaining.getAmount() == 0) {
                clickedInventory.setItem(event.getSlot(), null);
            } else {
                clickedInventory.setItem(event.getSlot(), remaining);
            }

            return;
        }
    }

    private boolean isConfiguredSlot(Machine machine, int slot) {
        return machine.getType().inputSlots().contains(slot) || 
               machine.getType().outputSlots().contains(slot) || 
               machine.getType().fuelSlots().contains(slot);
    }

    private boolean isFuelItem(ItemStack item, List<String> fuelItems) {
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

    private ItemStack insertIntoSlots(Inventory inventory, ItemStack stack, List<Integer> slots, Predicate<ItemStack> filter) {
        if (!filter.test(stack)) return stack;

        ItemStack remaining = stack.clone();

        // Try to merge into existing stacks
        for (int slot : slots) {
            if (remaining.getAmount() == 0)
                return remaining;

            ItemStack existing = inventory.getItem(slot);

            if (existing == null || existing.getType().isAir())
                continue;

            if (!existing.isSimilar(remaining))
                continue;

            int max = Math.min(existing.getMaxStackSize(), inventory.getMaxStackSize());
            int space = max - existing.getAmount();

            if (space <= 0)
                continue;

            int move = Math.min(space, remaining.getAmount());

            existing.setAmount(existing.getAmount() + move);
            remaining.setAmount(remaining.getAmount() - move);
        }

        // Then fill empty slots
        for (int slot : slots) {
            if (remaining.getAmount() == 0)
                return remaining;

            ItemStack existing = inventory.getItem(slot);

            if (existing != null && !existing.getType().isAir())
                continue;

            int max = Math.min(remaining.getMaxStackSize(), inventory.getMaxStackSize());
            int move = Math.min(max, remaining.getAmount());

            ItemStack placed = remaining.clone();
            placed.setAmount(move);

            inventory.setItem(slot, placed);
            remaining.setAmount(remaining.getAmount() - move);
        }

        return remaining;
    }
}