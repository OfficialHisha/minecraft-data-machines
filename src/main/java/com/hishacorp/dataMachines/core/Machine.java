package com.hishacorp.dataMachines.core;

import com.hishacorp.dataMachines.api.MachineType;
import com.hishacorp.dataMachines.api.Recipe;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

import java.util.List;

public class Machine {
    private UUID uuid;
    private final Location location;
    private final MachineType type;
    private PersistentDataContainer container;

    private Inventory inventory;
    private Recipe activeRecipe;
    private int remainingTime;

    public Machine(Location location, MachineType type) {
        this.uuid = UUID.randomUUID();
        this.location = location;
        this.type = type;

        this.inventory = Bukkit.createInventory(new MachineInventoryHolder(this), 27, Component.text(type.id()));
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

    public void destroy() {
        for (ItemStack itemStack : inventory.getContents()) {
            if (itemStack != null) {
                location.getWorld().dropItemNaturally(location, itemStack);
            }
        }
        inventory.clear();
    }
}