package com.hishacorp.dataMachines.handlers;

import com.hishacorp.dataMachines.api.Recipe;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class BasicRecipeHandler implements RecipeHandler {
    private final Random random = new Random();

    @Override
    public List<Recipe.ItemStackData> process(Recipe recipe, org.bukkit.inventory.Inventory inventory) {
        List<Recipe.ItemStackData> results = new ArrayList<>();
        
        for (Recipe.ItemStackData output : recipe.outputs()) {
            if (random.nextDouble() <= output.chance()) {
                results.add(output);
            }
        }
        
        return results;
    }
}