package com.hishacorp.dataMachines.handlers;

import com.hishacorp.dataMachines.api.MachineType;
import com.hishacorp.dataMachines.api.Recipe;
import com.hishacorp.dataMachines.core.Machine;
import com.hishacorp.dataMachines.core.MachineManager;
import com.nexomc.nexo.api.NexoItems;
import org.bukkit.Location;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class BasicMachineHandler implements MachineHandler<MachineType> {
    private static final Logger log = LoggerFactory.getLogger(BasicMachineHandler.class);

    protected final MachineManager machineManager;
    protected final RecipeHandler recipeHandler;

    public BasicMachineHandler(MachineManager machineManager, RecipeHandler recipeHandler) {
        this.machineManager = machineManager;
        this.recipeHandler = recipeHandler;
    }

    @Override
    public void tick(Machine machine) {
        Location location = machine.getLocation();
        Inventory inventory = machine.getInventory();
        MachineType type = machine.getType();

        if (!canProcess(location, inventory, type)) {
            return;
        }

        if (machine.getActiveRecipe() != null) {
            processActiveRecipe(machine);
        } else {
            tryStartNewRecipe(machine);
        }
    }

    private boolean canProcess(Location location, Inventory inventory, MachineType type) {
        // Redstone check
        String redstoneReq = type.properties().redstoneRequired();
        boolean isPowered = location.getBlock().isBlockPowered();
        if ("on".equalsIgnoreCase(redstoneReq) && !isPowered) {
            return false;
        } else if ("off".equalsIgnoreCase(redstoneReq) && isPowered) {
            return false;
        }

        // Fuel check
        if (type.properties().fuelRequired()) {
            boolean hasFuel = false;
            List<String> fuelItems = type.properties().fuelItems();
            for (int slot : type.fuelSlots()) {
                ItemStack item = inventory.getItem(slot);
                if (item != null && isFuelItem(item, fuelItems)) {
                    hasFuel = true;
                    break;
                }
            }
            return hasFuel;
        }

        return true;
    }

    private boolean isFuelItem(ItemStack item, List<String> fuelItems) {
        if (fuelItems == null || fuelItems.isEmpty()) {
            return true; // If no specific fuel items are defined, any item counts as fuel
        }
        for (String fuelId : fuelItems) {
            if (isItemMatch(item, fuelId)) {
                return true;
            }
        }
        return false;
    }

    private void tryStartNewRecipe(Machine machine) {
        MachineType type = machine.getType();
        Inventory inventory = machine.getInventory();

        for (Recipe recipe : machineManager.getRecipeRegistry().getAllRecipes()) {
            if (!type.supportedRecipeTypes().contains(recipe.type())) {
                continue;
            }

            if (hasInputs(inventory, type, recipe)) {
                consumeInputs(inventory, type, recipe);
                if (type.properties().fuelRequired()) {
                    consumeFuel(inventory, type);
                }
                machine.setActiveRecipe(recipe);
                machine.setRemainingTime(recipe.processingTime());
                return;
            }
        }
    }

    private void consumeFuel(Inventory inventory, MachineType type) {
        List<String> fuelItems = type.properties().fuelItems();
        for (int slot : type.fuelSlots()) {
            ItemStack item = inventory.getItem(slot);
            if (item != null && isFuelItem(item, fuelItems)) {
                if (item.getAmount() > 1) {
                    item.setAmount(item.getAmount() - 1);
                } else {
                    inventory.setItem(slot, null);
                }
                return;
            }
        }
    }

    private boolean hasInputs(Inventory inventory, MachineType type, Recipe recipe) {
        for (Recipe.ItemStackData input : recipe.inputs()) {
            boolean found = false;
            for (int slot : type.inputSlots()) {
                ItemStack item = inventory.getItem(slot);
                if (item != null && isItemMatch(item, input.item())) {
                    if (item.getAmount() >= input.amount()) {
                        found = true;
                        break;
                    }
                }
            }
            return found;
        }
        return true;
    }

    private void consumeInputs(Inventory inventory, MachineType type, Recipe recipe) {
        for (Recipe.ItemStackData input : recipe.inputs()) {
            for (int slot : type.inputSlots()) {
                ItemStack item = inventory.getItem(slot);
                if (item != null && isItemMatch(item, input.item())) {
                    if (item.getAmount() >= input.amount()) {
                        item.setAmount(item.getAmount() - input.amount());
                        break;
                    }
                }
            }
        }
    }

    private boolean isItemMatch(ItemStack item, String itemID) {
        // Check if it's a Nexo item first
        if (itemID.startsWith("nexo:")) {
            String nexoId = itemID.substring(5);
            return NexoItems.itemFromId(nexoId).build().isSimilar(item);
        }
        // Fallback to vanilla material
        return item.getType().name().equalsIgnoreCase(itemID);
    }

    private void processActiveRecipe(Machine machine) {
        Recipe recipe = machine.getActiveRecipe();
        int remainingTime = machine.getRemainingTime();
        MachineType type = machine.getType();

        // Apply modifiers
        double modifier = type.globalModifier();
        if (type.perTypeModifiers().containsKey(recipe.type())) {
            modifier = type.perTypeModifiers().get(recipe.type());
        }

        // We'll assume a tick is 10 units of time.
        // If modifier is 2.0, it processes 20 units per tick.
        int processedThisTick = Math.toIntExact(10 * Math.round(modifier));
        int newTime = remainingTime - processedThisTick;

        if (newTime <= 0) {
            completeRecipe(machine);
        } else {
            machine.setRemainingTime(newTime);
        }
    }

    protected void completeRecipe(Machine machine) {
        Recipe recipe = machine.getActiveRecipe();
        Inventory inventory = machine.getInventory();
        MachineType type = machine.getType();
        Location location = machine.getLocation();

        // Get the results from the recipe handler
        List<Recipe.ItemStackData> results = recipeHandler.process(recipe, inventory);

        // Output the results
        for (Recipe.ItemStackData result : results) {
            ItemStack itemStack = createItemStack(result);

            if (type.properties().alwaysDrop()) {
                location.getWorld().dropItemNaturally(location, itemStack);
            } else {
                boolean placed = false;
                for (int slot : type.outputSlots()) {
                    ItemStack current = inventory.getItem(slot);

                    if (current == null || (current.getType() == itemStack.getType() && current.getAmount() + itemStack.getAmount() <= 64)) {
                        if (current == null) {
                            inventory.setItem(slot, itemStack);
                        } else {
                            current.setAmount(current.getAmount() + itemStack.getAmount());
                        }
                        placed = true;
                        break;
                    }
                }

                if (!placed) {
                    location.getWorld().dropItemNaturally(location, itemStack);
                }
            }
        }

        // Clear the active recipe and time
        machine.setActiveRecipe(null);
        machine.setRemainingTime(0);
    }

    private ItemStack createItemStack(Recipe.ItemStackData data) {
        String itemID = data.item();

        ItemStack itemStack;

        if (itemID.startsWith("nexo:")) {
            itemStack = NexoItems.itemFromId(itemID.substring(5)).build();
        } else {
            try {
                itemStack = new ItemStack(org.bukkit.Material.valueOf(itemID.toUpperCase()));
            } catch (Exception e) {
                log.error("Could not create item stack for " + data.item(), e);
                itemStack = new ItemStack(org.bukkit.Material.STONE);
            }
        }

        itemStack.setAmount(data.amount());

        return itemStack;
    }
}