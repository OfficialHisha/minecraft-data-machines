package com.hishacorp.dataMachines;

import com.hishacorp.dataMachines.config.ConfigManager;
import com.hishacorp.dataMachines.core.MachineManager;
import com.hishacorp.dataMachines.core.RecipeRegistry;
import com.hishacorp.dataMachines.handlers.BasicMachineHandler;
import com.hishacorp.dataMachines.handlers.BasicRecipeHandler;
import com.hishacorp.dataMachines.listeners.ChunkListener;
import com.hishacorp.dataMachines.listeners.InventoryListener;
import com.hishacorp.dataMachines.listeners.NexoFurnitureListener;
import com.hishacorp.dataMachines.tasks.MachineHopperTask;
import com.nexomc.nexo.api.NexoItems;
import com.nexomc.nexo.items.ItemBuilder;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public final class DataMachines extends JavaPlugin {
    private static final Logger log = LoggerFactory.getLogger(DataMachines.class);
    private static DataMachines plugin;
    private MachineManager machineManager;

    public static final TextColor COLOR_RED = TextColor.fromHexString("FF5555");

    @Override
    public void onEnable() {
        plugin = this;

        // Setup Configs
        File recipesFile = new File(getDataFolder(), "recipes.yml");
        File machinesFile = new File(getDataFolder(), "machines.yml");
        
        // Ensure config files exist
        saveResource("recipes.yml", false);
        saveResource("machines.yml", false);

        ConfigManager configManager = new ConfigManager(recipesFile, machinesFile);
        configManager.load();

        // Setup Registry
        RecipeRegistry recipeRegistry = new RecipeRegistry();
        configManager.getRecipes().values().forEach(recipeRegistry::register);

        // Setup Manager
        this.machineManager = new MachineManager(configManager, recipeRegistry);

        // Register Handlers
        configManager.getMachineTypes().values().forEach(machineType -> this.machineManager.registerHandler(machineType.id(), new BasicMachineHandler(this.machineManager, new BasicRecipeHandler())));

        // Register Listeners
        this.getServer().getPluginManager().registerEvents(new NexoFurnitureListener(this.machineManager), this);
        this.getServer().getPluginManager().registerEvents(new ChunkListener(this.machineManager), this);
        this.getServer().getPluginManager().registerEvents(new InventoryListener(), this);

        // Start Tick Loop
        new MachineHopperTask(this.machineManager).runTaskTimer(this, 20L, 20L);

        this.getServer().getScheduler().scheduleSyncRepeatingTask(this, () -> this.machineManager.tick(), 1L, 1L);

        getLogger().info("DataMachines enabled successfully!");
        
        // Register testing command
        getCommand("dm").setExecutor((sender, command, label, args) -> {
            if (args.length == 3 && "give".equalsIgnoreCase(args[0])) {
                if (!sender.hasPermission("datamachines.admin.give")) {
                    sender.sendMessage("You do not have permission to give machines.");
                    return true;
                }

                String machineId = args[1];
                Player player = Bukkit.getPlayer(args[2]);

                if (player != null) {
                    ItemStack itemStack = getNexoItem(machineId);

                    if (itemStack == null) {
                        sender.sendMessage("Machine id " + machineId + " not found!");
                        return true;
                    }

                    player.getInventory().addItem(itemStack);
                    player.sendMessage("Given " + machineId);

                    return true;
                }

                sender.sendMessage("Player not found.");
                return true;
            }

            if (args.length == 1 && "reload".equalsIgnoreCase(args[0])) {
                if (!sender.hasPermission("datamachines.admin.reload")) {
                    sender.sendMessage("You do not have permission to reload configurations.");
                    return true;
                }
                this.machineManager.reload();
                sender.sendMessage("Configurations reloaded successfully!");
                return true;
            }
            sender.sendMessage("Usage: /dm give <machine_id> <player> OR /dm reload");
            return true;
        });
    }

    @Override
    public void onDisable() {
        getLogger().info("DataMachines disabled!");
    }

    public static DataMachines getPlugin() {
        return plugin;
    }

    public static ItemStack getNexoItem(String nexoItemId) {
        ItemBuilder builder = NexoItems.itemFromId(nexoItemId);

        if (builder == null) {
            log.error("Tried to load nexo item {}, but no such item has been defined!", nexoItemId);
            return null;
        }

        return builder.build();
    }

    public static boolean isItemMatch(ItemStack item, String itemID) {
        if (itemID.startsWith("nexo:")) {
            String nexoId = itemID.substring(5);

            ItemStack nexoStack = getNexoItem(nexoId);

            if (nexoStack == null) return false;

            return nexoStack.isSimilar(item);
        }
        return item.getType().name().equalsIgnoreCase(itemID);
    }
}
