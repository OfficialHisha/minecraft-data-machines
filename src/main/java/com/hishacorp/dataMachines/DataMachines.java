package com.hishacorp.dataMachines;

import com.hishacorp.dataMachines.config.ConfigManager;
import com.hishacorp.dataMachines.core.MachineManager;
import com.hishacorp.dataMachines.core.RecipeRegistry;
import com.hishacorp.dataMachines.handlers.BasicMachineHandler;
import com.hishacorp.dataMachines.handlers.BasicRecipeHandler;
import com.hishacorp.dataMachines.listeners.ChunkListener;
import com.hishacorp.dataMachines.listeners.NexoFurnitureListener;
import com.nexomc.nexo.api.NexoItems;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class DataMachines extends JavaPlugin {
    private MachineManager machineManager;

    @Override
    public void onEnable() {
        // 1. Setup Configs
        File recipesFile = new File(getDataFolder(), "recipes.yml");
        File machinesFile = new File(getDataFolder(), "machines.yml");
        
        // Ensure files exist
        saveResource("recipes.yml", false);
        saveResource("machines.yml", false);

        ConfigManager configManager = new ConfigManager(recipesFile, machinesFile);
        configManager.load();

        // 2. Setup Registry
        RecipeRegistry recipeRegistry = new RecipeRegistry();
        configManager.getRecipes().values().forEach(recipeRegistry::register);

        // 3. Setup Manager
        this.machineManager = new MachineManager(configManager, recipeRegistry);

        // 4. Register Handlers
        configManager.getMachineTypes().values().forEach(machineType -> this.machineManager.registerHandler(machineType.id(), new BasicMachineHandler(this.machineManager, new BasicRecipeHandler())));

        // 5. Register Listeners
        this.getServer().getPluginManager().registerEvents(new NexoFurnitureListener(this.machineManager), this);
        this.getServer().getPluginManager().registerEvents(new ChunkListener(this, this.machineManager), this);

        // 6. Start Tick Loop
        this.getServer().getScheduler().scheduleSyncRepeatingTask(this, () -> this.machineManager.tick(), 10L, 10L);

        getLogger().info("DataMachines enabled successfully!");
        
        // Register testing command
        getCommand("dm").setExecutor((sender, command, label, args) -> {
            if (args.length == 3 && "give".equalsIgnoreCase(args[0])) {
                String machineId = args[1];
                Player player = Bukkit.getPlayer(args[2]);

                if (player != null) {
                    try {
                        player.getInventory().addItem(NexoItems.itemFromId(machineId).build());
                        player.sendMessage("Given " + machineId);
                    } catch (NullPointerException e) {
                        sender.sendMessage("Machine id " + machineId + " not found!");
                    }
                    return true;
                }

                sender.sendMessage("Player not found.");
                return true;
            }

            sender.sendMessage("Usage: /dm give <machine_id> <player>");
            return true;
        });
    }

    @Override
    public void onDisable() {
        getLogger().info("DataMachines disabled!");
    }
}