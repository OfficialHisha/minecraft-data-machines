package com.hishacorp.dataMachines.handlers;

import com.hishacorp.dataMachines.api.Recipe;
import org.bukkit.inventory.Inventory;
import java.util.List;

public interface RecipeHandler {
    /**
     * Processes a recipe and returns the resulting items.
     * 
     * @param recipe The recipe to process.
     * @param inventory The machine's internal inventory.
     * @return A list of items to be produced.
     */
    List<Recipe.ItemStackData> process(Recipe recipe, Inventory inventory);
}
