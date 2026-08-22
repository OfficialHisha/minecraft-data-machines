package com.hishacorp.dataMachines.core;

import com.hishacorp.dataMachines.DataMachines;
import com.hishacorp.dataMachines.api.MachineType;
import com.hishacorp.dataMachines.api.Recipe;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;

import java.util.List;

public class Machine {
    private UUID uuid;
    private final Location location;
    private MachineType type;
    private PersistentDataContainer container;

    private final Inventory inventory;
    private Recipe activeRecipe;
    private int remainingTime;
    private int fuelTimer;
    private int maxFuelTimer;

    public Machine(Location location, MachineType type) {
        this.uuid = UUID.randomUUID();
        this.location = location;
        this.type = type;

        this.inventory = Bukkit.createInventory(new MachineInventoryHolder(this), 27, Component.text(type.name()));
        populateFillerSlots();
    }

    private void populateFillerSlots() {
        ItemStack filler = DataMachines.getNexoItem("filler_item");

        if (filler == null) return;

        for (int i = 0; i < 27; i++) {
            if (!isConfiguredSlot(i)) {
                inventory.setItem(i, filler);
            }
        }
    }

    private boolean isConfiguredSlot(int slot) {
        return type.inputSlots().contains(slot) || 
               type.outputSlots().contains(slot) || 
               type.fuelSlots().contains(slot);
    }

    public record MachineInventoryHolder(Machine machine) implements InventoryHolder {

        @Override
            public @NotNull Inventory getInventory() {
                return machine.getInventory();
            }
        }

    public Location getLocation() {
        return location;
    }

    public MachineType getType() {
        return type;
    }

    public void setType(MachineType type) {
        this.type = type;
    }

    public Inventory getInventory() {
        return inventory;
    }

    public List<Integer> getInputSlots() {
        return type.inputSlots();
    }

    public List<Integer> getOutputSlots() {
        return type.outputSlots();
    }

    public List<Integer> getFuelSlots() {
        return type.fuelSlots();
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public PersistentDataContainer getContainer() {
        return container;
    }

    public void setContainer(PersistentDataContainer container) {
        this.container = container;
    }

    public Recipe getActiveRecipe() {
        return activeRecipe;
    }

    public void setActiveRecipe(Recipe activeRecipe) {
        this.activeRecipe = activeRecipe;
    }

    public int getRemainingTime() {
        return remainingTime;
    }

    public void setRemainingTime(int remainingTime) {
        this.remainingTime = remainingTime;
    }

    public int getFuelTimer() {
        return fuelTimer;
    }

    public void setFuelTimer(int fuelTimer) {
        this.fuelTimer = fuelTimer;
    }

    public int getMaxFuelTimer() {
        return this.maxFuelTimer;
    }

    public void setMaxFuelTimer(int maxFuelTimer) {
        this.maxFuelTimer = maxFuelTimer;
    }

    public void updateProgressItem() {
        if (type.progressSlot() == null) return;

        int slot = type.progressSlot();
        ItemStack item = inventory.getItem(slot);
        if (item == null) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        int progress = 0;
        if (activeRecipe != null) {
            progress = Math.toIntExact(Math.round((1.0 - (double) remainingTime / activeRecipe.processingTime()) * 100));
        }

        Integer bestKey = null;
        for (Integer key : type.progressTextures().keySet()) {
            if (key <= progress) {
                if (bestKey == null || key > bestKey) {
                    bestKey = key;
                }
            }
        }

        if (bestKey != null) {
            ItemStack progressItem = DataMachines.getNexoItem(type.progressTextures().get(bestKey));

            if (progressItem == null) return;

            inventory.setItem(slot, progressItem);
        }
    }

    public void updateFuelProgressItem() {
        if (type.fuelProgressSlot() == null) return;

        int slot = type.fuelProgressSlot();
        ItemStack item = inventory.getItem(slot);
        if (item == null) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        int progress = 0;
        if (maxFuelTimer > 0) {
            progress = Math.toIntExact(Math.round((double) fuelTimer / maxFuelTimer * 100));
        }

        Integer bestKey = null;
        for (Integer key : type.fuelProgressTextures().keySet()) {
            if (key <= progress) {
                if (bestKey == null || key > bestKey) {
                    bestKey = key;
                }
            }
        }

        if (bestKey != null) {
            ItemStack fuelProgressItem = DataMachines.getNexoItem(type.fuelProgressTextures().get(bestKey));

            if (fuelProgressItem == null) return;

            inventory.setItem(slot, fuelProgressItem);
        }
    }

    public void destroy() {
        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack itemStack = inventory.getItem(i);
            if (itemStack != null && isConfiguredSlot(i)) {
                location.getWorld().dropItemNaturally(location, itemStack);
            }
        }
    }

    public boolean tryInsert(Inventory source, ItemStack item, List<Integer> slots) {
        if (slots == null || slots.isEmpty()) return false;

        // Find the item in the source (hopper)
        int sourceSlot = -1;
        for (int i = 0; i < source.getSize(); i++) {
            ItemStack stack = source.getItem(i);
            if (stack != null && stack.isSimilar(item)) {
                sourceSlot = i;
                break;
            }
        }

        if (sourceSlot == -1) return false;

        ItemStack sourceStack = source.getItem(sourceSlot).clone();
        int amountToMove = sourceStack.getAmount();

        // Try to find space in destination slots
        int moved = 0;
        for (int slot : slots) {
            ItemStack destStack = inventory.getItem(slot);
            int space = 0;
            if (destStack == null || destStack.getType().isAir()) {
                space = item.getMaxStackSize();
            } else if (destStack.isSimilar(item)) {
                space = destStack.getMaxStackSize() - destStack.getAmount();
            }

            if (space > 0) {
                int toAdd = Math.min(space, amountToMove - moved);
                if (destStack == null || destStack.getType().isAir()) {
                    ItemStack newStack = item.clone();
                    newStack.setAmount(toAdd);
                    inventory.setItem(slot, newStack);
                } else {
                    destStack.setAmount(destStack.getAmount() + toAdd);
                }
                moved += toAdd;
            }
            if (moved >= amountToMove) break;
        }

        if (moved > 0) {
            ItemStack remaining = source.getItem(sourceSlot);
            if (remaining != null) {
                if (remaining.getAmount() <= moved) {
                    source.setItem(sourceSlot, null);
                } else {
                    remaining.setAmount(remaining.getAmount() - moved);
                }
            }
            return true;
        }

        return false;
    }

    public boolean isFuelItem(ItemStack item) {
        Map<String, Integer> fuelItems = type.properties().fuelItems();
        if (fuelItems == null || fuelItems.isEmpty()) return true;
        for (String fuelId : fuelItems.keySet()) {
            if (DataMachines.isItemMatch(item, fuelId)) return true;
        }
        return false;
    }
}
