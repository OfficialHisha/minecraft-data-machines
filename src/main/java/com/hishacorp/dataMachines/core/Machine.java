package com.hishacorp.dataMachines.core;

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

import java.util.UUID;

import java.util.List;

public class Machine {
    private UUID uuid;
    private final Location location;
    private MachineType type;
    private PersistentDataContainer container;

    private Inventory inventory;
    private Recipe activeRecipe;
    private int remainingTime;

    public Machine(Location location, MachineType type) {
        this.uuid = UUID.randomUUID();
        this.location = location;
        this.type = type;

        this.inventory = Bukkit.createInventory(new MachineInventoryHolder(this), 27, Component.text(type.name()));
        populateFillerSlots();
    }

    private void populateFillerSlots() {
        ItemStack filler = new ItemStack(org.bukkit.Material.STRUCTURE_VOID);
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

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
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
            meta.displayName(Component.text(type.progressTextures().get(bestKey)));
            item.setItemMeta(meta);
        }
    }

    public void destroy() {
        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack itemStack = inventory.getItem(i);
            if (itemStack != null && isConfiguredSlot(i)) {
                location.getWorld().dropItemNaturally(location, itemStack);
            }
        }
        inventory.clear();
    }
}
