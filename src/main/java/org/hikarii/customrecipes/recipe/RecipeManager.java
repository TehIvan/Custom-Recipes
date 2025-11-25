package org.hikarii.customrecipes.recipe;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.BlastingRecipe;
import org.bukkit.inventory.CampfireRecipe;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.SmokingRecipe;
import org.bukkit.Material;
import org.bukkit.inventory.meta.ItemMeta;
import org.hikarii.customrecipes.CustomRecipes;
import org.hikarii.customrecipes.config.DefaultRecipesManager;
import org.hikarii.customrecipes.config.JsonRecipeFileManager;
import org.hikarii.customrecipes.recipe.data.FurnaceRecipeData;
import org.hikarii.customrecipes.recipe.data.RecipeIngredient;
import org.hikarii.customrecipes.util.ItemStackSerializer;
import java.util.HashMap;
import java.util.Map;
import java.util.*;

public class RecipeManager {
    private final CustomRecipes plugin;
    private final Map<String, CustomRecipe> recipes;
    private final Set<NamespacedKey> registeredKeys;

    public RecipeManager(CustomRecipes plugin) {
        this.plugin = plugin;
        this.recipes = new LinkedHashMap<>();
        this.registeredKeys = new HashSet<>();
    }

    public void addRecipe(CustomRecipe recipe) {
        recipes.put(recipe.getKey(), recipe);
        plugin.debug("Added recipe: " + recipe.getKey());
    }

    public CustomRecipe removeRecipe(String key) {
        CustomRecipe removed = recipes.remove(key.toLowerCase());
        if (removed != null) {
            plugin.debug("Removed recipe: " + key);
        }
        return removed;
    }

    public CustomRecipe getRecipe(String key) {
        return recipes.get(key.toLowerCase());
    }

    public Collection<CustomRecipe> getAllRecipes() {
        return Collections.unmodifiableCollection(recipes.values());
    }

    public int getRecipeCount() {
        return recipes.size();
    }

    public void clearRecipes() {
        recipes.clear();
        plugin.debug("Cleared all recipes from manager");
    }

    public void registerAllRecipes() {
        unregisterAll();
        int registered = 0;
        int skipped = 0;
        for (CustomRecipe recipe : recipes.values()) {
            boolean isDisabled = plugin.getRecipeStateTracker().isRecipeDisabled(recipe.getKey());

            if (isDisabled) {
                skipped++;
                continue;
            }

            if (registerRecipe(recipe)) {
                registered++;
            } else {
                plugin.getLogger().warning("Failed to register recipe: " + recipe.getKey());
            }
        }
        plugin.getLogger().info("Registered " + registered + " custom recipes" +
                (skipped > 0 ? " (" + skipped + " disabled)" : ""));
    }

    public boolean registerSingleRecipe(CustomRecipe recipe) {
        recipes.put(recipe.getKey(), recipe);
        boolean success = registerRecipe(recipe);
        if (success) {
            plugin.getLogger().info("Registered new recipe: " + recipe.getKey());
        }
        return success;
    }

    private boolean registerRecipe(CustomRecipe recipe) {
        try {
            NamespacedKey key = new NamespacedKey(plugin, recipe.getKey());
            if (recipe.getType() == RecipeType.SHAPED) {
                boolean useCraftedNames = plugin.isUseCraftedCustomNames();
                boolean keepSpawnEggNames = plugin.isKeepSpawnEggNames();
                ShapedRecipe shapedRecipe = new ShapedRecipe(key, recipe.createResult(useCraftedNames, keepSpawnEggNames));
                String[] shape = recipe.getRecipeData().toShapeArray();
                if (shape.length == 1) {
                    shapedRecipe.shape(shape[0]);
                } else if (shape.length == 2) {
                    shapedRecipe.shape(shape[0], shape[1]);
                } else {
                    shapedRecipe.shape(shape[0], shape[1], shape[2]);
                }

                Map<Character, Material> ingredients = recipe.getRecipeData().getIngredientMap();
                List<RecipeIngredient> allIngredients = recipe.getRecipeData().ingredients();
                for (Map.Entry<Character, Material> entry : ingredients.entrySet()) {
                    RecipeIngredient ingredientWithAmount = allIngredients.stream()
                            .filter(ing -> ing.material() == entry.getValue())
                            .findFirst()
                            .orElse(new RecipeIngredient(entry.getValue(), 1));
                    if (ingredientWithAmount.hasExactItem()) {
                        ItemStack exactItem = ingredientWithAmount.getExactItem().clone();
                        if (exactItem.getType() == Material.ENCHANTED_BOOK) {
                            shapedRecipe.setIngredient(entry.getKey(), Material.ENCHANTED_BOOK);
                        } else {
                            shapedRecipe.setIngredient(entry.getKey(), new org.bukkit.inventory.RecipeChoice.ExactChoice(exactItem));
                        }
                        plugin.debug("Registered ingredient with exactItem: " + exactItem.getType() + " with enchants: " + exactItem.hasItemMeta());
                    } else if (ingredientWithAmount.amount() > 1) {
                        ItemStack itemWithAmount = new ItemStack(entry.getValue(), ingredientWithAmount.amount());
                        shapedRecipe.setIngredient(entry.getKey(), new org.bukkit.inventory.RecipeChoice.ExactChoice(itemWithAmount));
                    } else {
                        shapedRecipe.setIngredient(entry.getKey(), entry.getValue());
                    }
                }
                Bukkit.addRecipe(shapedRecipe);
                registeredKeys.add(key);
                plugin.debug("Registered shaped recipe: " + recipe.getKey());
                return true;
            } else if (recipe.getType() == RecipeType.SHAPELESS) {
                boolean useCraftedNames = plugin.isUseCraftedCustomNames();
                boolean keepSpawnEggNames = plugin.isKeepSpawnEggNames();
                org.bukkit.inventory.ShapelessRecipe shapelessRecipe =
                        new org.bukkit.inventory.ShapelessRecipe(key, recipe.createResult(useCraftedNames, keepSpawnEggNames));

                var shapelessData = recipe.getShapelessData();
                Map<Material, Integer> ingredients = shapelessData.ingredients();

                if (shapelessData.hasExactIngredients()) {
                    List<ItemStack> exactIngredients = shapelessData.exactIngredients();
                    int exactIndex = 0;
                    for (Map.Entry<Material, Integer> entry : ingredients.entrySet()) {
                        for (int i = 0; i < entry.getValue(); i++) {
                            ItemStack exactItem = null;
                            if (exactIngredients != null && exactIndex < exactIngredients.size()) {
                                exactItem = exactIngredients.get(exactIndex);
                            }

                            if (exactItem != null) {
                                
                                ItemStack itemWithAmount = exactItem.clone();
                                itemWithAmount.setAmount(1);
                                shapelessRecipe.addIngredient(new RecipeChoice.ExactChoice(itemWithAmount));
                                plugin.debug("  Shapeless exact ingredient: " + exactItem.getType());
                            } else {
                                
                                shapelessRecipe.addIngredient(new RecipeChoice.MaterialChoice(entry.getKey()));
                            }
                            exactIndex++;
                        }
                    }
                } else {
                    
                    for (Map.Entry<Material, Integer> entry : ingredients.entrySet()) {
                        shapelessRecipe.addIngredient(entry.getValue(), entry.getKey());
                    }
                }

                Bukkit.addRecipe(shapelessRecipe);
                registeredKeys.add(key);
                plugin.debug("Registered shapeless recipe: " + recipe.getKey() +
                        (shapelessData.hasExactIngredients() ? " (with exact ingredients)" : ""));
                return true;
            } else if (recipe.getType().isFurnaceType()) {
                return registerFurnaceRecipe(key, recipe);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to register recipe '" + recipe.getKey() + "': " + e.getMessage());
            if (plugin.isDebugMode()) {
                e.printStackTrace();
            }
        }
        return false;
    }

    private boolean registerFurnaceRecipe(NamespacedKey key, CustomRecipe recipe) {
        FurnaceRecipeData furnaceData = recipe.getFurnaceData();
        if (furnaceData == null) {
            plugin.getLogger().warning("Furnace recipe '" + recipe.getKey() + "' has no furnace data");
            return false;
        }

        boolean useCraftedNames = plugin.isUseCraftedCustomNames();
        boolean keepSpawnEggNames = plugin.isKeepSpawnEggNames();
        ItemStack result = recipe.createResult(useCraftedNames, keepSpawnEggNames);

        RecipeChoice inputChoice;
        if (furnaceData.hasExactInput()) {
            inputChoice = new RecipeChoice.ExactChoice(furnaceData.getExactInput());
        } else {
            inputChoice = new RecipeChoice.MaterialChoice(furnaceData.getInput().material());
        }

        int cookingTime = furnaceData.getCookingTime();
        float experience = furnaceData.getExperience();
        String group = furnaceData.getGroup();

        try {
            switch (recipe.getType()) {
                case FURNACE -> {
                    FurnaceRecipe furnaceRecipe = new FurnaceRecipe(key, result, inputChoice, experience, cookingTime);
                    if (!group.isEmpty()) {
                        furnaceRecipe.setGroup(group);
                    }
                    Bukkit.addRecipe(furnaceRecipe);
                    registeredKeys.add(key);
                    plugin.debug("Registered furnace recipe: " + recipe.getKey());
                    return true;
                }
                case BLAST_FURNACE -> {
                    BlastingRecipe blastingRecipe = new BlastingRecipe(key, result, inputChoice, experience, cookingTime);
                    if (!group.isEmpty()) {
                        blastingRecipe.setGroup(group);
                    }
                    Bukkit.addRecipe(blastingRecipe);
                    registeredKeys.add(key);
                    plugin.debug("Registered blast furnace recipe: " + recipe.getKey());
                    return true;
                }
                case SMOKER -> {
                    SmokingRecipe smokingRecipe = new SmokingRecipe(key, result, inputChoice, experience, cookingTime);
                    if (!group.isEmpty()) {
                        smokingRecipe.setGroup(group);
                    }
                    Bukkit.addRecipe(smokingRecipe);
                    registeredKeys.add(key);
                    plugin.debug("Registered smoker recipe: " + recipe.getKey());
                    return true;
                }
                case CAMPFIRE -> {
                    CampfireRecipe campfireRecipe = new CampfireRecipe(key, result, inputChoice, experience, cookingTime);
                    if (!group.isEmpty()) {
                        campfireRecipe.setGroup(group);
                    }
                    Bukkit.addRecipe(campfireRecipe);
                    registeredKeys.add(key);
                    plugin.debug("Registered campfire recipe: " + recipe.getKey());
                    return true;
                }
                default -> {
                    plugin.getLogger().warning("Unknown furnace type: " + recipe.getType());
                    return false;
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to register furnace recipe '" + recipe.getKey() + "': " + e.getMessage());
            if (plugin.isDebugMode()) {
                e.printStackTrace();
            }
            return false;
        }
    }

    public void unregisterAll() {
        int unregistered = 0;
        for (NamespacedKey key : registeredKeys) {
            if (Bukkit.removeRecipe(key)) {
                unregistered++;
                plugin.debug("Unregistered recipe: " + key.getKey());
            }
        }

        registeredKeys.clear();
        if (unregistered > 0) {
            plugin.getLogger().info("Unregistered " + unregistered + " custom recipes");
        }
    }

    public boolean hasRecipe(String key) {
        return recipes.containsKey(key.toLowerCase());
    }

    public boolean enableRecipe(String key) {
        CustomRecipe recipe = getRecipe(key);
        if (recipe == null) {
            return false;
        }

        NamespacedKey namespacedKey = new NamespacedKey(plugin, recipe.getKey());
        if (registeredKeys.contains(namespacedKey)) {
            return false;
        }
        return registerRecipe(recipe);
    }

    public boolean disableRecipe(String key) {
        CustomRecipe recipe = getRecipe(key);
        if (recipe == null) {
            return false;
        }

        NamespacedKey namespacedKey = new NamespacedKey(plugin, recipe.getKey());
        if (!registeredKeys.contains(namespacedKey)) {
            return false;
        }
        if (Bukkit.removeRecipe(namespacedKey)) {
            registeredKeys.remove(namespacedKey);
            plugin.debug("Disabled recipe: " + key);
            return true;
        }
        return false;
    }

    public boolean deleteRecipe(String key) {
        CustomRecipe recipe = removeRecipe(key);
        if (recipe == null) {
            return false;
        }

        NamespacedKey namespacedKey = new NamespacedKey(plugin, recipe.getKey());
        if (registeredKeys.contains(namespacedKey)) {
            Bukkit.removeRecipe(namespacedKey);
            registeredKeys.remove(namespacedKey);
        }
        plugin.debug("Deleted recipe: " + key);
        return true;
    }

    public boolean deleteRecipePermanently(String key) {
        CustomRecipe recipe = getRecipe(key);
        if (recipe == null) {
            return false;
        }

        removeRecipe(key);
        NamespacedKey namespacedKey = new NamespacedKey(plugin, recipe.getKey());
        if (registeredKeys.contains(namespacedKey)) {
            Bukkit.removeRecipe(namespacedKey);
            registeredKeys.remove(namespacedKey);
        }

        plugin.getConfigManager().removeEnabledRecipe(key);
        boolean isDefault = DefaultRecipesManager.isDefaultRecipe(key);
        if (isDefault) {
            plugin.getRecipeStateTracker().markDefaultRecipeDeleted(key);
        }

        plugin.getConfigManager().getRecipeFileManager().deleteRecipe(key);
        JsonRecipeFileManager jsonManager = new JsonRecipeFileManager(plugin);
        jsonManager.deleteRecipe(key);
        return true;
    }

    public boolean updateRecipeResult(String recipeKey, ItemStack newResult, String newName, List<String> newDescription) {
        CustomRecipe recipe = getRecipe(recipeKey);
        if (recipe == null) {
            return false;
        }

        String finalName = newName;
        List<String> finalDescription = newDescription;
        if (finalName == null || finalName.isEmpty()) {
            ItemMeta meta = newResult.getItemMeta();
            if (meta != null && meta.hasDisplayName()) {
                finalName = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                        .serialize(meta.displayName());
            }
        }

        if (finalDescription == null || finalDescription.isEmpty()) {
            ItemMeta meta = newResult.getItemMeta();
            if (meta != null && meta.hasLore() && meta.lore() != null) {
                finalDescription = new ArrayList<>();
                for (Component line : meta.lore()) {
                    String loreText = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                            .serialize(line);
                    if (loreText != null && !loreText.isEmpty()) {
                        finalDescription.add(loreText);
                    }
                }
            }
        }

        CraftEvents craftEvents = recipe.getCraftEvents();
        if (newResult.hasItemMeta()) {
            ItemMeta resultMeta = newResult.getItemMeta();
            NamespacedKey presetKey = new NamespacedKey(plugin, "craft_event_preset");
            String presetName = resultMeta.getPersistentDataContainer().get(presetKey, org.bukkit.persistence.PersistentDataType.STRING);
            if (presetName != null && !presetName.isEmpty()) {
                CraftEventPreset preset = plugin.getCraftEventPresetManager().getPreset(presetName);
                if (preset != null) {
                    craftEvents = preset.toCraftEvents();
                    plugin.getLogger().info("[DEBUG] Loaded craft events from preset for existing recipe: " + presetName);
                } else {
                    plugin.getLogger().warning("[DEBUG] Preset not found: " + presetName);
                }
            } else {
                
                craftEvents = null;
                plugin.getLogger().info("[DEBUG] No preset on result item, craft events cleared");
            }
        }

        CustomRecipe updatedRecipe = new CustomRecipe(
                recipe.getKey(),
                finalName,
                finalDescription,
                recipe.getType(),
                recipe.getRecipeData(),
                recipe.getShapelessData(),
                recipe.getFurnaceData(),
                newResult,
                recipe.isHidden(),
                recipe.getPotionEffects(),
                recipe.getConditions(),
                recipe.getRandomResults(),
                craftEvents
        );
        recipes.put(recipeKey.toLowerCase(), updatedRecipe);
        NamespacedKey namespacedKey = new NamespacedKey(plugin, recipeKey);
        if (registeredKeys.contains(namespacedKey)) {
            Bukkit.removeRecipe(namespacedKey);
            registeredKeys.remove(namespacedKey);
            registerRecipe(updatedRecipe);
        }
        saveRecipeToFile(updatedRecipe);
        plugin.debug("Updated result item for recipe: " + recipeKey);
        return true;
    }

    private void saveRecipeToFile(CustomRecipe recipe) {
        try {
            Map<String, Object> recipeData = new HashMap<>();
            if (recipe.getName() != null && !recipe.getName().isEmpty()) {
                recipeData.put("name", recipe.getName());
            }
            if (recipe.getDescription() != null && !recipe.getDescription().isEmpty()) {
                recipeData.put("description", recipe.getDescription());
            }

            recipeData.put("type", recipe.getType().name());
            recipeData.put("hidden", recipe.isHidden());
            if (recipe.getType() == RecipeType.SHAPED) {
                String[] pattern = recipe.getRecipeData().toShapeArray();
                List<String> patternList = new ArrayList<>();
                for (String row : pattern) {
                    StringBuilder rowBuilder = new StringBuilder();
                    Map<Character, Material> ingredients = recipe.getRecipeData().getIngredientMap();
                    for (char c : row.toCharArray()) {
                        if (c == ' ') {
                            rowBuilder.append("AIR ");
                        } else {
                            Material mat = ingredients.get(c);
                            rowBuilder.append(mat.name()).append(" ");
                        }
                    }
                    patternList.add(rowBuilder.toString().trim());
                }
                recipeData.put("recipe", patternList);
            } else if (recipe.getType() == RecipeType.SHAPELESS) {
                recipeData.put("ingredients", recipe.getShapelessData().toConfigList());
            } else if (recipe.getType().isFurnaceType()) {
                FurnaceRecipeData furnaceData = recipe.getFurnaceData();
                if (furnaceData != null) {
                    recipeData.put("input", furnaceData.getInput().material().name());
                    recipeData.put("cooking-time", furnaceData.getCookingTime());
                    recipeData.put("experience", furnaceData.getExperience());
                    if (!furnaceData.getGroup().isEmpty()) {
                        recipeData.put("group", furnaceData.getGroup());
                    }
                    if (furnaceData.hasExactInput()) {
                        recipeData.put("input-full", ItemStackSerializer.toMap(furnaceData.getExactInput()));
                    }
                    if (furnaceData.hasCustomFuels()) {
                        Map<String, Object> fuelsMap = new HashMap<>();
                        int fuelIndex = 0;
                        for (FurnaceRecipeData.CustomFuel fuel : furnaceData.getCustomFuels()) {
                            Map<String, Object> fuelData = new HashMap<>();
                            fuelData.put("material", fuel.getIngredient().material().name());
                            fuelData.put("burn-time", fuel.getBurnTime());
                            if (fuel.hasExactItem()) {
                                fuelData.put("item-full", ItemStackSerializer.toMap(fuel.getExactItem()));
                            }
                            fuelsMap.put("fuel_" + fuelIndex++, fuelData);
                        }
                        recipeData.put("custom-fuels", fuelsMap);
                    }
                }
            }
            Map<String, Object> resultData = ItemStackSerializer.toMap(recipe.getResultItem());
            recipeData.put("result", resultData);
            recipeData.put("material", recipe.getResultMaterial().name());
            recipeData.put("amount", recipe.getResultAmount());
            plugin.getConfigManager().getRecipeFileManager().saveRecipe(recipe.getKey(), recipeData, recipe.getType());
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to save recipe: " + e.getMessage());
        }
    }

    public boolean isRecipeEnabled(String key) {
        CustomRecipe recipe = getRecipe(key);
        if (recipe == null) {
            return false;
        }
        NamespacedKey namespacedKey = new NamespacedKey(plugin, recipe.getKey());
        return registeredKeys.contains(namespacedKey);
    }
}