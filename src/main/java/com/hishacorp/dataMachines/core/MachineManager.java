package com.hishacorp.dataMachines.core;

import com.hishacorp.dataMachines.api.MachineType;
import com.hishacorp.dataMachines.api.Recipe;
import com.hishacorp.dataMachines.config.ConfigManager;
import com.hishacorp.dataMachines.handlers.MachineHandler;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MachineManager {
    private static final Logger log = LoggerFactory.getLogger(MachineManager.class);
    private final ConfigManager configManager;
    private final RecipeRegistry recipeRegistry;
    private final Map<String, MachineHandler<MachineType>> handlers = new HashMap<>();
    private final Map<Location, Machine> activeMachines = new ConcurrentHashMap<>();

    public static final NamespacedKey KEY_UUID = new NamespacedKey("data-machines", "machine_uuid");
    public static final NamespacedKey KEY_TYPE = new NamespacedKey("data-machines", "machine_type");
    public static final NamespacedKey KEY_RECIPE = new NamespacedKey("data-machines", "active_recipe");
    public static final NamespacedKey KEY_TIME = new NamespacedKey("data-machines", "remaining_time");
    public static final NamespacedKey KEY_FUEL_TIMER = new NamespacedKey("data-machines", "fuel_timer");
    public static final NamespacedKey KEY_MAX_FUEL_TIMER = new NamespacedKey("data-machines", "max_fuel_timer");
    public static final NamespacedKey KEY_INVENTORY = new NamespacedKey("data-machines", "inventory");

    public MachineManager(ConfigManager configManager, RecipeRegistry recipeRegistry) {
        this.configManager = configManager;
        this.recipeRegistry = recipeRegistry;
    }

    public void registerHandler(String machineTypeId, MachineHandler<MachineType> handler) {
        log.info("Registered handler for {}", machineTypeId);
        handlers.put(machineTypeId, handler);
    }

    public void reload() {
        log.info("Reloading machine and recipe configurations...");
        
        configManager.load();
        
        recipeRegistry.clear();
        configManager.getRecipes().values().forEach(recipeRegistry::register);
        
        handlers.clear();
        configManager.getMachineTypes().values().forEach(type -> 
            registerHandler(type.id(), new com.hishacorp.dataMachines.handlers.BasicMachineHandler(this, new com.hishacorp.dataMachines.handlers.BasicRecipeHandler()))
        );
        
        for (Machine machine : activeMachines.values()) {
            String typeId = machine.getType().id();
            MachineType newType = configManager.getMachineTypes().get(typeId);
            if (newType != null) {
                machine.setType(newType);
            } else {
                log.warn("Machine {} has type {} which is no longer in config", machine.getUuid(), typeId);
            }
            
            // Update Active Recipe
            Recipe currentRecipe = machine.getActiveRecipe();
            if (currentRecipe != null) {
                String recipeId = currentRecipe.id();
                
                // Try to find the updated recipe in the registry
                Recipe updatedRecipe = recipeRegistry.getAllRecipes().stream()
                        .filter(r -> r.id().equalsIgnoreCase(recipeId))
                        .findFirst()
                        .orElse(null);
                
                if (updatedRecipe != null) {
                    // Always set the recipe to ensure the reference is updated to the latest config object
                    machine.setActiveRecipe(updatedRecipe);
                    log.debug("Updated recipe for machine {}: {}", machine.getUuid(), recipeId);
                } else {
                    log.warn("Machine {} has recipe {} which is no longer in config", machine.getUuid(), recipeId);
                    machine.setActiveRecipe(null);
                }
            }
        }
        
        log.info("Reload complete.");
    }

    public void addMachine(Machine machine) {
        activeMachines.put(machine.getLocation(), machine);
    }

    public void removeMachine(Location location) {
        Machine machine = activeMachines.get(location);
        if (machine != null) {
            machine.destroy();
            activeMachines.remove(location);
        }
    }

    public void tick() {
        for (Machine machine : activeMachines.values()) {
            MachineHandler<MachineType> handler = handlers.get(machine.getType().id());
            if (handler != null) {
                handler.tick(machine);
                saveMachine(machine);
            }
        }
    }

    public RecipeRegistry getRecipeRegistry() {
        return recipeRegistry;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public Machine getMachine(Location loc) {
        return activeMachines.get(loc);
    }

    public void saveMachine(Machine machine) {
        PersistentDataContainer container = machine.getContainer();
        if (container == null) return;

        container.set(KEY_UUID, PersistentDataType.STRING, machine.getUuid().toString());
        container.set(KEY_TYPE, PersistentDataType.STRING, machine.getType().id());
        
        if (machine.getActiveRecipe() != null) {
            container.set(KEY_RECIPE, PersistentDataType.STRING, machine.getActiveRecipe().id());
        } else {
            container.remove(KEY_RECIPE);
        }
        
        container.set(KEY_TIME, PersistentDataType.INTEGER, machine.getRemainingTime());
        container.set(KEY_FUEL_TIMER, PersistentDataType.INTEGER, machine.getFuelTimer());
        container.set(KEY_MAX_FUEL_TIMER, PersistentDataType.INTEGER, machine.getMaxFuelTimer());

        ItemStack[] itemStacks = machine.getInventory().getContents();
        byte[] bytes = ItemStack.serializeItemsAsBytes(itemStacks);

        container.set(KEY_INVENTORY, PersistentDataType.BYTE_ARRAY, bytes);
    }

    public void loadMachineFromContainer(Entity entity, PersistentDataContainer container) {
        String uuidStr = container.get(KEY_UUID, PersistentDataType.STRING);
        String typeId = container.get(KEY_TYPE, PersistentDataType.STRING);
        String recipeId = container.get(KEY_RECIPE, PersistentDataType.STRING);
        byte[] inventoryBytes = container.get(KEY_INVENTORY, PersistentDataType.BYTE_ARRAY);
        int remainingTime = container.getOrDefault(KEY_TIME, PersistentDataType.INTEGER, 0);
        int fuelTimer = container.getOrDefault(KEY_FUEL_TIMER, PersistentDataType.INTEGER, 0);
        int maxFuelTimer = container.getOrDefault(KEY_MAX_FUEL_TIMER, PersistentDataType.INTEGER, 0);

        if (uuidStr == null || typeId == null) {
            log.warn("Machine found with missing data: UUID={}, Type={}", uuidStr, typeId);
            return;
        }

        MachineType machineType = configManager.getMachineTypes().values().stream()
                .filter(t -> t.id().equalsIgnoreCase(typeId))
                .findFirst()
                .orElse(null);

        if (machineType == null) {
            log.warn("Machine type {} not found in config, skipping", typeId);
            return;
        }

        Machine machine = new Machine(entity.getLocation().getBlock().getLocation(), machineType);
        machine.setUuid(java.util.UUID.fromString(uuidStr));
        machine.setContainer(container);
        
        if (recipeId != null) {
            recipeRegistry.getAllRecipes().stream()
                    .filter(r -> r.id().equalsIgnoreCase(recipeId))
                    .findFirst()
                    .ifPresent(machine::setActiveRecipe);
        }

        machine.getInventory().setContents(ItemStack.deserializeItemsFromBytes(inventoryBytes));
        machine.setRemainingTime(remainingTime);
        machine.setFuelTimer(fuelTimer);
        machine.setMaxFuelTimer(maxFuelTimer);
        addMachine(machine);
        log.debug("Loaded machine {} of type {}", machine.getUuid(), typeId);
    }

    public void associateWithEntity(Machine machine, Entity entity) {
        machine.setContainer(entity.getPersistentDataContainer());
        saveMachine(machine);
        log.debug("Associated machine {} with entity {}", machine.getUuid(), entity.getEntityId());
    }
}