package com.hishacorp.dataMachines.config;

import com.hishacorp.dataMachines.api.MachineType;
import com.hishacorp.dataMachines.api.Recipe;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class ConfigManager {
    private final File recipesFile;
    private final File machinesFile;
    private final Map<String, Recipe> recipes = new HashMap<>();
    private final Map<String, MachineType> machineTypes = new HashMap<>();

    public ConfigManager(File recipesFile, File machinesFile) {
        this.recipesFile = recipesFile;
        this.machinesFile = machinesFile;
    }

    public void load() {
        recipes.clear();
        machineTypes.clear();
        loadRecipes();
        loadMachines();
    }

    private void loadRecipes() {
        FileConfiguration config = YamlConfiguration.loadConfiguration(recipesFile);
        ConfigurationSection section = config.getConfigurationSection("recipes");
        if (section == null) return;

        for (String id : section.getKeys(false)) {
            ConfigurationSection recipeSection = section.getConfigurationSection(id);
            if (recipeSection == null) continue;

            String type = recipeSection.getString("type");
            int processingTime = recipeSection.getInt("processing_time");

            List<?> inputsRaw = recipeSection.getList("inputs");
            List<Recipe.ItemStackData> inputs = new ArrayList<>();
            if (inputsRaw != null) {
                for (Object obj : inputsRaw) {
                    if (obj instanceof Map<?, ?> map) {
                        String item = (String) map.get("item");
                        int amount = 0;
                        if (map.get("amount") instanceof Integer i) amount = i;
                        else if (map.get("amount") instanceof Double d) amount = d.intValue();
                        
                        double chance = 1.0;
                        if (map.get("chance") instanceof Double d) chance = d;
                        else if (map.get("chance") instanceof Integer i) chance = i.doubleValue();
                        
                        inputs.add(new Recipe.ItemStackData(item, amount, chance));
                    }
                }
            }

            List<?> outputsRaw = recipeSection.getList("outputs");
            List<Recipe.ItemStackData> outputs = new ArrayList<>();
            if (outputsRaw != null) {
                for (Object obj : outputsRaw) {
                    if (obj instanceof Map<?, ?> map) {
                        String item = (String) map.get("item");
                        int amount = 0;
                        if (map.get("amount") instanceof Integer i) amount = i;
                        else if (map.get("amount") instanceof Double d) amount = d.intValue();
                        
                        double chance = 1.0;
                        if (map.get("chance") instanceof Integer i) chance = i.doubleValue();
                        else if (map.get("chance") instanceof Double d) chance = d;
                        
                        outputs.add(new Recipe.ItemStackData(item, amount, chance));
                    }
                }
            }

            recipes.put(id, new Recipe(id, type, inputs, outputs, processingTime));
        }
    }

    private void loadMachines() {
        FileConfiguration config = YamlConfiguration.loadConfiguration(machinesFile);
        ConfigurationSection section = config.getConfigurationSection("machines");
        if (section == null) return;

        for (String id : section.getKeys(false)) {
            ConfigurationSection machineSection = section.getConfigurationSection(id);
            if (machineSection == null) continue;

            List<String> supportedRecipes = machineSection.getStringList("supported_recipe_types");
            List<Integer> inputSlots = machineSection.getIntegerList("input_slots");
            List<Integer> outputSlots = machineSection.getIntegerList("output_slots");
            List<Integer> fuelSlots = machineSection.getIntegerList("fuel_slots");
            
            // Handle modifiers: can be a single number or a map
            Object modifierObj = machineSection.get("modifiers");
            double globalModifier = 1.0;
            Map<String, Double> perTypeModifiers = new HashMap<>();

            if (modifierObj instanceof Double d) {
                globalModifier = d;
            } else if (modifierObj instanceof Integer i) {
                globalModifier = i.doubleValue();
            } else if (modifierObj instanceof Map<?, ?> map) {
                ConfigurationSection modSection = machineSection.getConfigurationSection("modifiers");
                if (modSection != null) {
                    ConfigurationSection perTypeSection = modSection.getConfigurationSection("per_type");
                    if (perTypeSection != null) {
                        for (String key : perTypeSection.getKeys(false)) {
                            perTypeModifiers.put(key, perTypeSection.getDouble(key));
                        }
                    }
                }
            }

            Map<Integer, String> progressTextures = new HashMap<>();
            ConfigurationSection progressSection = machineSection.getConfigurationSection("progress_textures");
            if (progressSection != null) {
                for (String key : progressSection.getKeys(false)) {
                    try {
                        progressTextures.put(Integer.parseInt(key), progressSection.getString(key));
                    } catch (NumberFormatException e) {
                        // Ignore invalid progress keys
                    }
                }
            }

            ConfigurationSection propsSection = machineSection.getConfigurationSection("properties");
            if (propsSection == null) continue;

            MachineType.MachineProperties properties = new MachineType.MachineProperties(
                propsSection.getBoolean("always_drop", false),
                propsSection.getBoolean("stop_on_full_buffer", true),
                propsSection.getBoolean("allow_pushing", true),
                propsSection.getString("redstone_required", "disabled"),
                propsSection.getBoolean("fuel_required", false),
                propsSection.getStringList("fuel_items"),
                propsSection.getString("requires_permission", null)
            );

            machineTypes.put(id, new MachineType(id, supportedRecipes, inputSlots, outputSlots, fuelSlots, globalModifier, perTypeModifiers, progressTextures, properties));
        }
    }

    public Map<String, Recipe> getRecipes() {
        return Collections.unmodifiableMap(recipes);
    }

    public Map<String, MachineType> getMachineTypes() {
        return Collections.unmodifiableMap(machineTypes);
    }
}