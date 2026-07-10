package com.hishacorp.dataMachines.core;

import com.hishacorp.dataMachines.api.Recipe;
import java.util.*;
import java.util.stream.Collectors;

public class RecipeRegistry {
    private final Map<String, Recipe> recipes = new HashMap<>();

    public void register(Recipe recipe) {
        recipes.put(recipe.id(), recipe);
    }

    public Collection<Recipe> getAllRecipes() {
        return Collections.unmodifiableCollection(recipes.values());
    }

    public List<String> getRecipeTypes() {
        return recipes.values().stream().map(Recipe::type).collect(Collectors.toList());
    }

    public List<Recipe> getRecipesByType(String type) {
        return recipes.values().stream()
            .filter(r -> r.type().equalsIgnoreCase(type))
            .collect(Collectors.toList());
    }
}