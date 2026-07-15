package com.hishacorp.dataMachines.core;

import com.hishacorp.dataMachines.api.Recipe;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class RecipeRegistry {
    private final Map<String, Recipe> recipes = new HashMap<>();

    public void register(Recipe recipe) {
        recipes.put(recipe.id(), recipe);
    }

    public void clear() {
        recipes.clear();
    }

    public Collection<Recipe> getAllRecipes() {
        return Collections.unmodifiableCollection(recipes.values());
    }
}