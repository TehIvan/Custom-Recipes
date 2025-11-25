package org.hikarii.customrecipes.recipe.vanilla;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.hikarii.customrecipes.CustomRecipes;
import org.hikarii.customrecipes.recipe.RecipeConditions;
import org.hikarii.customrecipes.recipe.RecipeType;
import org.hikarii.customrecipes.util.ItemStackSerializer;
import org.hikarii.customrecipes.util.MaterialVersionAdapter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

public class VanillaRecipeManager {
    private final CustomRecipes plugin;
    private final File vanillaRecipesFolder;
    private final File vanillaRecipesDataFile;
    private final File vanillaFurnaceRecipesDataFile;
    private final Map<String, VanillaRecipeInfo> allVanillaRecipes;
    private final Map<String, VanillaRecipeState> modifiedRecipes;
    private int cleanupTaskId = -1;

    public VanillaRecipeManager(CustomRecipes plugin) {
        this.plugin = plugin;
        this.vanillaRecipesFolder = new File(plugin.getDataFolder(), "vanillarecipes");
        this.vanillaRecipesDataFile = new File(plugin.getDataFolder(), "vanilla-recipes.yml");
        this.vanillaFurnaceRecipesDataFile = new File(plugin.getDataFolder(), "vanilla-furnace-recipes.yml");
        this.allVanillaRecipes = new LinkedHashMap<>();
        this.modifiedRecipes = new HashMap<>();
        if (!vanillaRecipesFolder.exists()) {
            vanillaRecipesFolder.mkdirs();
        }

        VanillaRecipesMigration migration = new VanillaRecipesMigration(plugin);
        if (!migration.checkAndMigrate()) {
            plugin.getLogger().warning("Vanilla recipes migration failed, but continuing...");
        }
        plugin.getLogger().info("Detected Minecraft version 1." + MaterialVersionAdapter.getMinorVersion() + " - materials will be adapted accordingly");
        loadVanillaRecipesData();
        loadVanillaFurnaceRecipesData();
        loadModifiedRecipes();
        applyModifications();
        startRecipeCleanupTask();
    }

    private void startRecipeCleanupTask() {
        
        this.cleanupTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            removeDisabledRecipes();
        }, 100L, 100L);
        plugin.getLogger().info("Started vanilla recipe cleanup task (runs every 5 seconds)");
    }

    public void stopCleanupTask() {
        if (cleanupTaskId != -1) {
            Bukkit.getScheduler().cancelTask(cleanupTaskId);
            cleanupTaskId = -1;
        }
    }

    private void removeDisabledRecipes() {
        for (Map.Entry<String, VanillaRecipeState> entry : modifiedRecipes.entrySet()) {
            if (entry.getValue().isDisabled()) {
                String recipeKey = entry.getKey();
                NamespacedKey minecraftKey = NamespacedKey.minecraft(recipeKey);
                Bukkit.removeRecipe(minecraftKey);
            }
        }
    }

    private void loadVanillaRecipesData() {
        if (!vanillaRecipesDataFile.exists()) {
            plugin.saveResource("vanilla-recipes.yml", false);
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(vanillaRecipesDataFile);
        ConfigurationSection recipesSection = config.getConfigurationSection("recipes");
        if (recipesSection == null) {
            plugin.getLogger().warning("No recipes found in vanilla-recipes.yml");
            return;
        }

        int loaded = 0;
        for (String recipeKey : recipesSection.getKeys(false)) {
            try {
                ConfigurationSection recipeSection = recipesSection.getConfigurationSection(recipeKey);
                if (recipeSection == null) continue;
                String displayName = recipeSection.getString("name", recipeKey);
                String materialStr = recipeSection.getString("material");
                int amount = recipeSection.getInt("amount", 1);
                String typeStr = recipeSection.getString("type", "SHAPED");
                List<String> patternStrings = recipeSection.getStringList("pattern");
                List<List<IngredientChoice>> ingredientGrid = new ArrayList<>();
                boolean hasInvalidIngredient = false;
                for (String rowString : patternStrings) {
                    List<IngredientChoice> row = new ArrayList<>();
                    String[] items = rowString.split(" ");
                    for (String item : items) {
                        IngredientChoice choice = IngredientChoice.fromString(item);
                        
                        if (!item.equals("AIR") && !item.isEmpty() && !choice.isValid()) {
                            plugin.debug("Invalid ingredient for recipe " + recipeKey + ": " + item);
                            hasInvalidIngredient = true;
                        }
                        row.add(choice);
                    }
                    ingredientGrid.add(row);
                }
                if (hasInvalidIngredient) {
                    plugin.debug("Skipping recipe " + recipeKey + " due to invalid ingredients");
                    continue;
                }
                String categoryStr = recipeSection.getString("category", "MISC");
                String stationStr = recipeSection.getString("station", "CRAFTING_TABLE");
                
                Material material = MaterialVersionAdapter.adaptMaterial(materialStr);
                if (material == null) {
                    plugin.debug("Invalid material for recipe " + recipeKey + ": " + materialStr);
                    continue;
                }
                Map<Integer, ItemStack> variantResults = new HashMap<>();
                ConfigurationSection resultsSection = recipeSection.getConfigurationSection("variant-results");
                if (resultsSection != null) {
                    for (String variantKey : resultsSection.getKeys(false)) {
                        try {
                            int variantIndex = Integer.parseInt(variantKey);
                            ConfigurationSection variantResultSection = resultsSection.getConfigurationSection(variantKey);
                            if (variantResultSection != null) {
                                String variantMaterial = variantResultSection.getString("material", materialStr);
                                
                                Material mat = MaterialVersionAdapter.adaptMaterial(variantMaterial);
                                if (mat == null) {
                                    plugin.debug("Invalid variant result material for recipe " + recipeKey + " variant " + variantIndex + ": " + variantMaterial);
                                    continue;
                                }
                                int variantAmount = variantResultSection.getInt("amount", 1);
                                ItemStack variantItem = new ItemStack(mat, variantAmount);
                                if (variantResultSection.contains("potion-effects")) {
                                    ItemMeta meta = variantItem.getItemMeta();
                                    if (meta instanceof org.bukkit.inventory.meta.SuspiciousStewMeta stewMeta) {
                                        List<Map<?, ?>> effectsList = variantResultSection.getMapList("potion-effects");
                                        for (Map<?, ?> effectMap : effectsList) {
                                            try {
                                                String effectName = (String) effectMap.get("type");
                                                int duration = effectMap.containsKey("duration") ?
                                                        ((Number) effectMap.get("duration")).intValue() : 160;
                                                int amplifier = effectMap.containsKey("amplifier") ?
                                                        ((Number) effectMap.get("amplifier")).intValue() : 0;
                                                org.bukkit.potion.PotionEffectType effectType =
                                                        org.bukkit.potion.PotionEffectType.getByName(effectName);
                                                if (effectType != null) {
                                                    stewMeta.addCustomEffect(
                                                            new org.bukkit.potion.PotionEffect(effectType, duration, amplifier),
                                                            true
                                                    );
                                                }
                                            } catch (Exception e) {
                                                plugin.getLogger().warning("Failed to parse effect: " + e.getMessage());
                                            }
                                        }
                                        variantItem.setItemMeta(stewMeta);
                                    }
                                }
                                variantResults.put(variantIndex, variantItem);
                            }
                        } catch (NumberFormatException e) {
                            plugin.debug("Invalid variant index: " + variantKey);
                        }
                    }
                }
                RecipeType type = RecipeType.fromString(typeStr);
                VanillaRecipeInfo.RecipeCategory category = VanillaRecipeInfo.RecipeCategory.valueOf(categoryStr);
                VanillaRecipeInfo.RecipeStation station = VanillaRecipeInfo.RecipeStation.valueOf(stationStr);
                VanillaRecipeInfo info = new VanillaRecipeInfo(
                        "minecraft:" + recipeKey,
                        displayName,
                        material,
                        amount,
                        type,
                        ingredientGrid,
                        category,
                        station,
                        variantResults
                );
                allVanillaRecipes.put(recipeKey, info);
                loaded++;
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load vanilla recipe " + recipeKey + ": " + e.getMessage());
            }
        }
        plugin.getLogger().info("Loaded " + loaded + " vanilla recipes from vanilla-recipes.yml");
    }

    private void loadVanillaFurnaceRecipesData() {
        if (!vanillaFurnaceRecipesDataFile.exists()) {
            plugin.saveResource("vanilla-furnace-recipes.yml", false);
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(vanillaFurnaceRecipesDataFile);
        ConfigurationSection recipesSection = config.getConfigurationSection("recipes");
        if (recipesSection == null) {
            plugin.getLogger().warning("No recipes found in vanilla-furnace-recipes.yml");
            return;
        }

        int loaded = 0;
        for (String recipeKey : recipesSection.getKeys(false)) {
            try {
                ConfigurationSection recipeSection = recipesSection.getConfigurationSection(recipeKey);
                if (recipeSection == null) continue;

                String displayName = recipeSection.getString("name", recipeKey);
                String inputMaterialStr = recipeSection.getString("input");
                String resultMaterialStr = recipeSection.getString("result");
                int amount = recipeSection.getInt("amount", 1);
                String categoryStr = recipeSection.getString("category", "MISC");

                Material resultMaterial = MaterialVersionAdapter.adaptMaterial(resultMaterialStr);
                if (resultMaterial == null) {
                    plugin.debug("Invalid result material for furnace recipe " + recipeKey + ": " + resultMaterialStr);
                    continue;
                }

                List<List<IngredientChoice>> ingredientGrid = new ArrayList<>();
                List<IngredientChoice> row = new ArrayList<>();

                if (inputMaterialStr.contains("|")) {
                    
                    String[] inputVariants = inputMaterialStr.split("\\|");
                    List<Material> validMaterials = new ArrayList<>();
                    for (String variant : inputVariants) {
                        Material mat = MaterialVersionAdapter.adaptMaterial(variant.trim());
                        if (mat != null) {
                            validMaterials.add(mat);
                        }
                    }
                    if (validMaterials.isEmpty()) {
                        plugin.debug("Invalid input materials for furnace recipe " + recipeKey + ": " + inputMaterialStr);
                        continue;
                    }
                    row.add(new IngredientChoice(validMaterials));
                } else {
                    
                    Material inputMaterial = MaterialVersionAdapter.adaptMaterial(inputMaterialStr);
                    if (inputMaterial == null) {
                        plugin.debug("Invalid input material for furnace recipe " + recipeKey + ": " + inputMaterialStr);
                        continue;
                    }
                    row.add(new IngredientChoice(inputMaterial));
                }
                ingredientGrid.add(row);

                VanillaRecipeInfo.RecipeCategory category;
                try {
                    category = VanillaRecipeInfo.RecipeCategory.valueOf(categoryStr);
                } catch (IllegalArgumentException e) {
                    category = VanillaRecipeInfo.RecipeCategory.MISC;
                }

                VanillaRecipeInfo info = new VanillaRecipeInfo(
                        "minecraft:furnace_" + recipeKey,
                        displayName,
                        resultMaterial,
                        amount,
                        RecipeType.FURNACE,
                        ingredientGrid,
                        category,
                        VanillaRecipeInfo.RecipeStation.FURNACE,
                        new HashMap<>()
                );

                allVanillaRecipes.put("furnace_" + recipeKey, info);
                loaded++;
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load vanilla furnace recipe " + recipeKey + ": " + e.getMessage());
            }
        }
        plugin.getLogger().info("Loaded " + loaded + " vanilla furnace recipes from vanilla-furnace-recipes.yml");
    }

    private ItemStack createItemStackFromConfig(ConfigurationSection section) {
        String materialStr = section.getString("material");
        Material material = Material.getMaterial(materialStr);
        if (material == null) {
            throw new IllegalArgumentException("Invalid material: " + materialStr);
        }

        int amount = section.getInt("amount", 1);
        ItemStack item = new ItemStack(material, amount);
        if (section.contains("nbt")) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                ConfigurationSection nbtSection = section.getConfigurationSection("nbt");
                if (nbtSection != null) {
                    if (nbtSection.contains("suspicious_stew_effects")) {
                        List<Map<?, ?>> effects = nbtSection.getMapList("suspicious_stew_effects");
                        if (!effects.isEmpty()) {
                            Map<?, ?> firstEffect = effects.get(0);
                            String effectId = (String) firstEffect.get("id");
                            int duration = firstEffect.containsKey("duration") ?
                                    ((Number) firstEffect.get("duration")).intValue() : 160;
                            org.bukkit.persistence.PersistentDataContainer pdc = meta.getPersistentDataContainer();
                            pdc.set(
                                    new org.bukkit.NamespacedKey(plugin, "stew_effect"),
                                    org.bukkit.persistence.PersistentDataType.STRING,
                                    effectId
                            );
                            pdc.set(
                                    new org.bukkit.NamespacedKey(plugin, "stew_duration"),
                                    org.bukkit.persistence.PersistentDataType.INTEGER,
                                    duration
                            );
                        }
                    }
                }
                item.setItemMeta(meta);
            }
        }
        return item;
    }

    private void loadModifiedRecipes() {
        File[] files = vanillaRecipesFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null || files.length == 0) {
            return;
        }

        int loaded = 0;
        for (File file : files) {
            try {
                YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                String recipeKey = file.getName().replace(".yml", "");
                boolean disabled = config.getBoolean("disabled", false);
                String originalKey = config.getString("original-recipe-key");
                int currentVariant = config.getInt("current-variant-index", 0);
                VanillaRecipeState state = new VanillaRecipeState(disabled, originalKey);
                state.setCurrentVariantIndex(currentVariant);

                if (config.contains("custom-result-amount")) {
                    state.setResultAmount(config.getInt("custom-result-amount"));
                }

                ConfigurationSection variantsSection = config.getConfigurationSection("variants");
                if (variantsSection != null) {
                    for (String variantKey : variantsSection.getKeys(false)) {
                        try {
                            int variantIndex = Integer.parseInt(variantKey);
                            List<String> pattern = variantsSection.getStringList(variantKey + ".pattern");
                            String typeStr = variantsSection.getString(variantKey + ".type", "SHAPED");
                            RecipeType type = RecipeType.fromString(typeStr);
                            state.setPatternForVariant(variantIndex, pattern);
                            state.setTypeForVariant(variantIndex, type);

                            if (variantsSection.contains(variantKey + ".disabled")) {
                                state.setVariantDisabled(variantIndex, variantsSection.getBoolean(variantKey + ".disabled"));
                            }

                            if (variantsSection.contains(variantKey + ".result-amount")) {
                                state.setResultAmountForVariant(variantIndex, variantsSection.getInt(variantKey + ".result-amount"));
                            }

                            List<String> exactIngredients = variantsSection.getStringList(variantKey + ".exact-ingredients");
                            if (!exactIngredients.isEmpty()) {
                                List<ItemStack> exactItems = new ArrayList<>();
                                for (String serialized : exactIngredients) {
                                    if (serialized != null && !serialized.isEmpty()) {
                                        try {
                                            exactItems.add(ItemStackSerializer.fromBase64(serialized));
                                        } catch (Exception e) {
                                            exactItems.add(null);
                                            plugin.getLogger().warning("Failed to deserialize exact item: " + e.getMessage());
                                        }
                                    } else {
                                        exactItems.add(null);
                                    }
                                }
                                state.setExactItemsForVariant(variantIndex, exactItems);
                            }
                        } catch (NumberFormatException ignored) {}
                    }
                }

                if (config.contains("craft-event-preset")) {
                    state.setCraftEventPresetName(config.getString("craft-event-preset"));
                }

                if (config.contains("conditions")) {
                    ConfigurationSection condSection = config.getConfigurationSection("conditions");
                    if (condSection != null) {
                        String permission = condSection.getString("permission");
                        int xpLevel = condSection.getInt("xp-level", 0);
                        int xpReward = condSection.getInt("xp-reward", 0);
                        int cooldown = condSection.getInt("cooldown", 0);
                        int dailyLimit = condSection.getInt("limit-daily", 0);
                        int weeklyLimit = condSection.getInt("limit-weekly", 0);
                        int totalLimit = condSection.getInt("limit-total", 0);
                        double moneyCost = condSection.getDouble("money-cost", 0.0);
                        state.setConditions(new RecipeConditions(permission, xpLevel, xpReward, cooldown, dailyLimit, weeklyLimit, totalLimit, moneyCost));
                    }
                }

                modifiedRecipes.put(recipeKey, state);
                loaded++;
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load modified recipe " + file.getName() + ": " + e.getMessage());
            }
        }
        if (loaded > 0) {
            plugin.getLogger().info("Loaded " + loaded + " modified vanilla recipes");
        }
    }

    private void applyModifications() {
        int disabled = 0;
        int changed = 0;

        List<NamespacedKey> disabledKeys = new ArrayList<>();
        for (Map.Entry<String, VanillaRecipeState> entry : modifiedRecipes.entrySet()) {
            String recipeKey = entry.getKey();
            VanillaRecipeState state = entry.getValue();
            if (state.isDisabled()) {
                disabledKeys.add(NamespacedKey.minecraft(recipeKey));
            }
        }

        for (Map.Entry<String, VanillaRecipeState> entry : modifiedRecipes.entrySet()) {
            String recipeKey = entry.getKey();
            VanillaRecipeState state = entry.getValue();
            if (state.isDisabled()) {
                
                NamespacedKey key = NamespacedKey.minecraft(recipeKey);
                boolean removed = Bukkit.removeRecipe(key);

                VanillaRecipeInfo info = allVanillaRecipes.get(recipeKey);
                if (info != null) {
                    int maxVariants = getMaxVariantsForRecipe(recipeKey);
                    for (int i = 0; i < maxVariants; i++) {
                        NamespacedKey variantKey = new NamespacedKey(plugin, recipeKey + "_variant_" + i);
                        Bukkit.removeRecipe(variantKey);
                    }
                }

                if (removed) {
                    disabled++;
                    plugin.debug("Disabled vanilla recipe (including all variants): " + recipeKey);
                }
            } else if (state.hasChangedRecipe() || state.getCustomResultAmount() != null) {
                
                NamespacedKey originalKey = NamespacedKey.minecraft(recipeKey);
                Bukkit.removeRecipe(originalKey);

                if (state.hasChangedRecipe()) {
                    
                    registerAllVariants(recipeKey, state);
                } else {
                    
                    NamespacedKey customKey = new NamespacedKey(plugin, recipeKey + "_custom_amount");
                    Bukkit.removeRecipe(customKey); 
                    VanillaRecipeInfo info = allVanillaRecipes.get(recipeKey);
                    if (info != null) {
                        registerRecipe(customKey, info, info.getPattern(), 0, state);
                        plugin.debug("Registered custom amount recipe under key: " + customKey.getKey());
                    }
                }
                changed++;
                plugin.debug("Changed vanilla recipe: " + recipeKey +
                    (state.hasChangedRecipe() ? " (pattern)" : " (amount only)"));
            }
        }
        if (disabled > 0 || changed > 0) {
            plugin.getLogger().info("Applied vanilla recipe modifications: " +
                    disabled + " disabled, " + changed + " changed");
            
            final List<NamespacedKey> finalDisabledKeys = new ArrayList<>(disabledKeys);
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                
                for (org.bukkit.entity.Player player : Bukkit.getOnlinePlayers()) {
                    for (NamespacedKey key : finalDisabledKeys) {
                        player.undiscoverRecipe(key);
                        plugin.debug("Undiscovered disabled recipe for " + player.getName() + ": " + key.getKey());
                    }
                }
            }, 40L); 
        }
    }

    public void toggleRecipe(String recipeKey) {
        VanillaRecipeState state = modifiedRecipes.get(recipeKey);
        if (state == null) {
            state = new VanillaRecipeState(true, "minecraft:" + recipeKey);
            modifiedRecipes.put(recipeKey, state);
        } else {
            state.setDisabled(!state.isDisabled());
        }
        saveRecipeState(recipeKey, state);
        if (state.isDisabled()) {
            
            NamespacedKey originalKey = NamespacedKey.minecraft(recipeKey);
            for (org.bukkit.entity.Player player : Bukkit.getOnlinePlayers()) {
                player.undiscoverRecipe(originalKey);
            }

            Bukkit.removeRecipe(originalKey);

            NamespacedKey customAmountKey = new NamespacedKey(plugin, recipeKey + "_custom_amount");
            Bukkit.removeRecipe(customAmountKey);

            VanillaRecipeInfo info = allVanillaRecipes.get(recipeKey);
            if (info != null) {
                int maxVariants = getMaxVariantsForRecipe(recipeKey);
                for (int i = 0; i < maxVariants; i++) {
                    NamespacedKey variantKey = new NamespacedKey(plugin, recipeKey + "_variant_" + i);
                    Bukkit.removeRecipe(variantKey);
                }
            }
            plugin.debug("Disabled vanilla recipe: " + recipeKey);
        } else {
            VanillaRecipeInfo info = allVanillaRecipes.get(recipeKey);
            if (info != null) {
                
                NamespacedKey originalKey = NamespacedKey.minecraft(recipeKey);
                Bukkit.removeRecipe(originalKey);

                boolean hasModifications = state.hasChangedRecipe() || state.getCustomResultAmount() != null;

                if (hasModifications) {
                    if (state.hasChangedRecipe()) {
                        registerAllVariants(recipeKey, state);
                    } else {
                        
                        NamespacedKey customKey = new NamespacedKey(plugin, recipeKey + "_custom_amount");
                        registerRecipe(customKey, info, info.getPattern(), 0, state);
                        plugin.debug("Re-registered custom amount recipe under key: " + customKey.getKey());
                    }
                    plugin.getLogger().info("Re-enabled modified recipe: " + recipeKey);
                } else {
                    NamespacedKey key = NamespacedKey.minecraft(recipeKey);
                    registerRecipe(key, info, info.getPattern(), 0, state);
                    plugin.getLogger().info("Re-enabled vanilla recipe: " + recipeKey);
                }
            }
            
            updateRecipesForAllPlayers();
        }
    }

    public void updateRecipeVariant(String recipeKey, int variantIndex, List<String> newPattern, RecipeType newType) {
        VanillaRecipeState state = modifiedRecipes.get(recipeKey);
        if (state == null) {
            state = new VanillaRecipeState(false, "minecraft:" + recipeKey);
            modifiedRecipes.put(recipeKey, state);
            initializeAllVariants(recipeKey, state);
        }
        state.setPatternForVariant(variantIndex, newPattern);
        state.setTypeForVariant(variantIndex, newType);
        state.setDisabled(false);
        saveRecipeState(recipeKey, state);
        NamespacedKey originalKey = NamespacedKey.minecraft(recipeKey);
        Bukkit.removeRecipe(originalKey);
        VanillaRecipeInfo info = allVanillaRecipes.get(recipeKey);
        if (info != null) {
            int maxVariants = getMaxVariantsForRecipe(recipeKey);
            for (int i = 0; i < maxVariants; i++) {
                NamespacedKey variantKey = new NamespacedKey(plugin, recipeKey + "_variant_" + i);
                Bukkit.removeRecipe(variantKey);
            }
        }
        registerAllVariants(recipeKey, state);
        
        updateRecipesForAllPlayers();
    }

    public void updateRecipeVariant(String recipeKey, int variantIndex, List<String> newPattern,
                                     RecipeType newType, List<ItemStack> exactItems) {
        VanillaRecipeState state = modifiedRecipes.get(recipeKey);
        if (state == null) {
            state = new VanillaRecipeState(false, "minecraft:" + recipeKey);
            modifiedRecipes.put(recipeKey, state);
            initializeAllVariants(recipeKey, state);
        }
        state.setPatternForVariant(variantIndex, newPattern);
        state.setTypeForVariant(variantIndex, newType);
        state.setExactItemsForVariant(variantIndex, exactItems);
        state.setDisabled(false);
        saveRecipeState(recipeKey, state);
        NamespacedKey originalKey = NamespacedKey.minecraft(recipeKey);
        Bukkit.removeRecipe(originalKey);
        VanillaRecipeInfo info = allVanillaRecipes.get(recipeKey);
        if (info != null) {
            int maxVariants = getMaxVariantsForRecipe(recipeKey);
            for (int i = 0; i < maxVariants; i++) {
                NamespacedKey variantKey = new NamespacedKey(plugin, recipeKey + "_variant_" + i);
                Bukkit.removeRecipe(variantKey);
            }
        }
        registerAllVariants(recipeKey, state);
        
        updateRecipesForAllPlayers();
    }

    public void updateRecipeResultAmount(String recipeKey, int newAmount) {
        VanillaRecipeState state = modifiedRecipes.get(recipeKey);
        if (state == null) {
            state = new VanillaRecipeState(false, "minecraft:" + recipeKey);
            modifiedRecipes.put(recipeKey, state);
        }
        state.setResultAmount(newAmount);
        state.setDisabled(false);
        saveRecipeState(recipeKey, state);

        NamespacedKey originalKey = NamespacedKey.minecraft(recipeKey);
        Bukkit.removeRecipe(originalKey);

        NamespacedKey customAmountKey = new NamespacedKey(plugin, recipeKey + "_custom_amount");
        Bukkit.removeRecipe(customAmountKey);

        VanillaRecipeInfo info = allVanillaRecipes.get(recipeKey);
        if (info != null) {
            int maxVariants = getMaxVariantsForRecipe(recipeKey);
            for (int i = 0; i < maxVariants; i++) {
                NamespacedKey variantKey = new NamespacedKey(plugin, recipeKey + "_variant_" + i);
                Bukkit.removeRecipe(variantKey);
            }

            if (state.hasChangedRecipe()) {
                registerAllVariants(recipeKey, state);
            } else {
                
                registerRecipe(customAmountKey, info, info.getPattern(), 0, state);
                plugin.debug("Registered recipe with custom amount under key: " + customAmountKey.getKey());
            }
        }

        updateRecipesForAllPlayers();
        plugin.debug("Updated result amount for recipe " + recipeKey + " to " + newAmount);
    }

    public void updateVariantResultAmount(String recipeKey, int variantIndex, int newAmount) {
        VanillaRecipeState state = modifiedRecipes.get(recipeKey);
        if (state == null) {
            state = new VanillaRecipeState(false, "minecraft:" + recipeKey);
            modifiedRecipes.put(recipeKey, state);
            initializeAllVariants(recipeKey, state);
        }
        state.setResultAmountForVariant(variantIndex, newAmount);
        saveRecipeState(recipeKey, state);

        NamespacedKey originalKey = NamespacedKey.minecraft(recipeKey);
        Bukkit.removeRecipe(originalKey);

        VanillaRecipeInfo info = allVanillaRecipes.get(recipeKey);
        if (info != null) {
            int maxVariants = getMaxVariantsForRecipe(recipeKey);
            for (int i = 0; i < maxVariants; i++) {
                NamespacedKey variantKey = new NamespacedKey(plugin, recipeKey + "_variant_" + i);
                Bukkit.removeRecipe(variantKey);
            }
            registerAllVariants(recipeKey, state);
        }

        updateRecipesForAllPlayers();
        plugin.debug("Updated result amount for recipe " + recipeKey + " variant " + variantIndex + " to " + newAmount);
    }

    public void setCraftEventPreset(String recipeKey, String presetName) {
        VanillaRecipeState state = modifiedRecipes.get(recipeKey);
        if (state == null) {
            state = new VanillaRecipeState(false, "minecraft:" + recipeKey);
            modifiedRecipes.put(recipeKey, state);
        }
        state.setCraftEventPresetName(presetName);
        saveRecipeState(recipeKey, state);
    }

    public String getCraftEventPreset(String recipeKey) {
        VanillaRecipeState state = modifiedRecipes.get(recipeKey);
        if (state == null) return null;
        return state.getCraftEventPresetName();
    }

    public void setRecipeConditions(String recipeKey, RecipeConditions conditions) {
        VanillaRecipeState state = modifiedRecipes.get(recipeKey);
        if (state == null) {
            state = new VanillaRecipeState(false, "minecraft:" + recipeKey);
            modifiedRecipes.put(recipeKey, state);
        }
        state.setConditions(conditions);
        saveRecipeState(recipeKey, state);
    }

    public RecipeConditions getRecipeConditions(String recipeKey) {
        VanillaRecipeState state = modifiedRecipes.get(recipeKey);
        if (state == null) return null;
        return state.getConditions();
    }

    public void toggleVariant(String recipeKey, int variantIndex) {
        VanillaRecipeState state = modifiedRecipes.get(recipeKey);
        if (state == null) {
            state = new VanillaRecipeState(false, "minecraft:" + recipeKey);
            modifiedRecipes.put(recipeKey, state);
            initializeAllVariants(recipeKey, state);
        }

        boolean newDisabledState = !state.isVariantDisabled(variantIndex);
        state.setVariantDisabled(variantIndex, newDisabledState);
        saveRecipeState(recipeKey, state);

        NamespacedKey originalKey = NamespacedKey.minecraft(recipeKey);
        Bukkit.removeRecipe(originalKey);

        VanillaRecipeInfo info = allVanillaRecipes.get(recipeKey);
        if (info != null) {
            int maxVariants = getMaxVariantsForRecipe(recipeKey);
            for (int i = 0; i < maxVariants; i++) {
                NamespacedKey variantKey = new NamespacedKey(plugin, recipeKey + "_variant_" + i);
                Bukkit.removeRecipe(variantKey);
            }
            registerAllVariants(recipeKey, state);
        }

        updateRecipesForAllPlayers();
        plugin.debug("Toggled variant " + variantIndex + " for recipe " + recipeKey + " to " + (newDisabledState ? "disabled" : "enabled"));
    }

    public boolean isVariantDisabled(String recipeKey, int variantIndex) {
        VanillaRecipeState state = modifiedRecipes.get(recipeKey);
        if (state == null) return false;
        return state.isVariantDisabled(variantIndex);
    }

    public Integer getVariantResultAmount(String recipeKey, int variantIndex) {
        VanillaRecipeState state = modifiedRecipes.get(recipeKey);
        if (state == null) return null;
        return state.getResultAmountForVariant(variantIndex);
    }

    private void registerAllVariants(String recipeKey, VanillaRecipeState state) {
        VanillaRecipeInfo info = allVanillaRecipes.get(recipeKey);
        if (info == null) return;
        int maxVariants = getMaxVariantsForRecipe(recipeKey);
        NamespacedKey originalKey = NamespacedKey.minecraft(recipeKey);
        Bukkit.removeRecipe(originalKey);
        for (int i = 0; i < maxVariants; i++) {
            NamespacedKey key = new NamespacedKey(plugin, recipeKey + "_variant_" + i);
            Bukkit.removeRecipe(key);
        }

        for (int variantIndex = 0; variantIndex < maxVariants; variantIndex++) {
            List<String> pattern = state.getPatternForVariant(variantIndex);
            if (pattern == null || pattern.isEmpty()) {
                pattern = createPatternForVariant(info, variantIndex);
            }
            if (pattern != null && !pattern.isEmpty()) {
                NamespacedKey key = new NamespacedKey(plugin, recipeKey + "_variant_" + variantIndex);
                registerRecipe(key, info, pattern, variantIndex, state);
            }
        }
    }

    public void setCurrentVariant(String recipeKey, int variantIndex) {
        VanillaRecipeInfo info = allVanillaRecipes.get(recipeKey);
        if (info == null) return;
        for (int row = 0; row < info.getIngredientGrid().size(); row++) {
            List<IngredientChoice> rowList = info.getIngredientGrid().get(row);
            for (int col = 0; col < rowList.size(); col++) {
                IngredientChoice choice = rowList.get(col);
                if (choice.hasMultipleOptions() && variantIndex < choice.getOptions().size()) {
                    choice.setSelectedIndex(variantIndex);
                }
            }
        }
        VanillaRecipeState state = modifiedRecipes.get(recipeKey);
        if (state != null) {
            state.setCurrentVariantIndex(variantIndex);
            saveRecipeState(recipeKey, state);
        }
    }

    public int getMaxVariantsForRecipe(String recipeKey) {
        VanillaRecipeInfo info = allVanillaRecipes.get(recipeKey);
        if (info == null) return 1;
        int maxVariants = 1;
        for (List<IngredientChoice> row : info.getIngredientGrid()) {
            for (IngredientChoice choice : row) {
                if (choice.hasMultipleOptions()) {
                    maxVariants = Math.max(maxVariants, choice.getOptions().size());
                }
            }
        }
        return maxVariants;
    }

    private void initializeAllVariants(String recipeKey, VanillaRecipeState state) {
        VanillaRecipeInfo info = allVanillaRecipes.get(recipeKey);
        if (info == null) return;
        int maxVariants = getMaxVariantsForRecipe(recipeKey);
        for (int variantIndex = 0; variantIndex < maxVariants; variantIndex++) {
            if (state.getPatternForVariant(variantIndex) == null) {
                List<String> variantPattern = createPatternForVariant(info, variantIndex);
                state.setPatternForVariant(variantIndex, variantPattern);
                state.setTypeForVariant(variantIndex, info.getType());
            }
        }
    }

    private List<String> createAnyVariantPattern(VanillaRecipeInfo info) {
        List<String> pattern = new ArrayList<>();
        for (List<IngredientChoice> row : info.getIngredientGrid()) {
            StringBuilder rowPattern = new StringBuilder();
            for (IngredientChoice choice : row) {
                if (rowPattern.length() > 0) {
                    rowPattern.append(" ");
                }
                if (choice.hasMultipleOptions()) {
                    StringBuilder options = new StringBuilder();
                    for (Material mat : choice.getOptions()) {
                        if (options.length() > 0) options.append("|");
                        options.append(mat.name());
                    }
                    rowPattern.append(options);
                } else {
                    rowPattern.append(choice.getSelected().name());
                }
            }
            pattern.add(rowPattern.toString());
        }
        return pattern;
    }

    private List<String> createPatternForVariant(VanillaRecipeInfo info, int variantIndex) {
        List<String> pattern = new ArrayList<>();
        for (List<IngredientChoice> row : info.getIngredientGrid()) {
            StringBuilder rowPattern = new StringBuilder();
            for (IngredientChoice choice : row) {
                if (rowPattern.length() > 0) {
                    rowPattern.append(" ");
                }
                if (choice.hasMultipleOptions() && variantIndex < choice.getOptions().size()) {
                    rowPattern.append(choice.getOptions().get(variantIndex).name());
                } else if (choice.hasMultipleOptions() && variantIndex >= choice.getOptions().size()) {
                    rowPattern.append(choice.getOptions().get(0).name());
                } else {
                    rowPattern.append(choice.getSelected().name());
                }
            }
            pattern.add(rowPattern.toString());
        }
        return pattern;
    }

    public void resetRecipe(String recipeKey) {
        modifiedRecipes.remove(recipeKey);
        File file = new File(vanillaRecipesFolder, recipeKey + ".yml");
        if (file.exists()) {
            file.delete();
        }

        VanillaRecipeInfo info = allVanillaRecipes.get(recipeKey);
        if (info != null) {
            int maxVariants = getMaxVariantsForRecipe(recipeKey);
            for (int i = 0; i < maxVariants; i++) {
                NamespacedKey variantKey = new NamespacedKey(plugin, recipeKey + "_variant_" + i);
                Bukkit.removeRecipe(variantKey);
            }
        }
        NamespacedKey key = NamespacedKey.minecraft(recipeKey);
        Bukkit.removeRecipe(key);
        if (info != null) {
            registerOriginalRecipe(key, info);
            plugin.getLogger().info("Reset vanilla recipe: " + recipeKey);
        }
        
        updateRecipesForAllPlayers();
    }

    private boolean registerChangedRecipe(NamespacedKey key, VanillaRecipeInfo info, VanillaRecipeState state) {
        return registerRecipe(key, info, state.getChangedPattern(), state.getCurrentVariantIndex(), state);
    }

    private boolean registerOriginalRecipe(NamespacedKey key, VanillaRecipeInfo info) {
        return registerRecipe(key, info, info.getPattern(), 0, null);
    }

    private boolean registerRecipe(NamespacedKey key, VanillaRecipeInfo info, List<String> pattern, int variantIndex, VanillaRecipeState state) {
        try {
            
            if (state != null && state.isVariantDisabled(variantIndex)) {
                plugin.debug("Skipping disabled variant " + variantIndex + " for recipe " + key.getKey());
                return false;
            }

            int resultAmount = info.getResultAmount();
            if (state != null) {
                Integer variantAmount = state.getResultAmountForVariant(variantIndex);
                if (variantAmount != null) {
                    resultAmount = variantAmount;
                } else if (state.getCustomResultAmount() != null) {
                    
                    resultAmount = state.getCustomResultAmount();
                }
            }

            ItemStack resultItem = info.hasVariantResults() ?
                    info.getResultForVariant(variantIndex) :
                    new ItemStack(info.getResultMaterial(), resultAmount);

            if (state != null) {
                Integer variantAmount = state.getResultAmountForVariant(variantIndex);
                if (variantAmount != null) {
                    resultItem.setAmount(variantAmount);
                } else if (state.getCustomResultAmount() != null) {
                    resultItem.setAmount(state.getCustomResultAmount());
                }
            }
            if (info.getType() == RecipeType.SHAPED) {
                ShapedRecipe recipe = new ShapedRecipe(key, resultItem);
                Map<Material, Character> materialToChar = new HashMap<>();
                char currentChar = 'A';
                List<String> bukkitPattern = new ArrayList<>();
                for (String row : pattern) {
                    StringBuilder bukkitRow = new StringBuilder();
                    String[] items = row.split(" ");
                    for (String item : items) {
                        if (item.equals("AIR") || item.isEmpty()) {
                            bukkitRow.append(' ');
                            continue;
                        }
                        String firstMaterial = item.contains("|") ? item.split("\\|")[0] : item;
                        Material mat = Material.getMaterial(firstMaterial);
                        if (mat == null || mat == Material.AIR) {
                            bukkitRow.append(' ');
                        } else {
                            if (!materialToChar.containsKey(mat)) {
                                materialToChar.put(mat, currentChar);
                                currentChar++;
                            }
                            bukkitRow.append(materialToChar.get(mat));
                        }
                    }
                    bukkitPattern.add(bukkitRow.toString());
                }
                if (bukkitPattern.isEmpty()) {
                    return false;
                }

                if (bukkitPattern.size() == 1) {
                    recipe.shape(bukkitPattern.get(0));
                } else if (bukkitPattern.size() == 2) {
                    recipe.shape(bukkitPattern.get(0), bukkitPattern.get(1));
                } else if (bukkitPattern.size() == 3) {
                    recipe.shape(bukkitPattern.get(0), bukkitPattern.get(1), bukkitPattern.get(2));
                }

                List<ItemStack> exactItems = null;
                if (state != null) {
                    exactItems = state.getExactItemsForVariant(variantIndex);
                }

                Map<Character, Integer> charToGridIndex = new HashMap<>();
                int gridIndex = 0;
                for (String row : pattern) {
                    String[] items = row.split(" ");
                    for (String item : items) {
                        if (!item.equals("AIR") && !item.isEmpty()) {
                            String firstMaterial = item.contains("|") ? item.split("\\|")[0] : item;
                            Material mat = Material.getMaterial(firstMaterial);
                            if (mat != null && mat != Material.AIR) {
                                Character ch = materialToChar.get(mat);
                                if (ch != null && !charToGridIndex.containsKey(ch)) {
                                    charToGridIndex.put(ch, gridIndex);
                                }
                            }
                        }
                        gridIndex++;
                    }
                }

                for (Map.Entry<Material, Character> entry : materialToChar.entrySet()) {
                    Character ch = entry.getValue();
                    Integer gridIdx = charToGridIndex.get(ch);

                    if (exactItems != null && gridIdx != null && gridIdx < exactItems.size() && exactItems.get(gridIdx) != null) {
                        
                        ItemStack exactItem = exactItems.get(gridIdx);
                        recipe.setIngredient(ch, new RecipeChoice.ExactChoice(exactItem));
                        plugin.debug("Using exact item for char " + ch + ": " + exactItem);
                    } else {
                        
                        recipe.setIngredient(ch, entry.getKey());
                    }
                }
                Bukkit.addRecipe(recipe);
                return true;
            } else if (info.getType() == RecipeType.SHAPELESS) {
                org.bukkit.inventory.ShapelessRecipe recipe =
                        new org.bukkit.inventory.ShapelessRecipe(key, resultItem);

                List<ItemStack> exactItems = null;
                if (state != null) {
                    exactItems = state.getExactItemsForVariant(variantIndex);
                }

                int gridIndex = 0;
                for (String row : pattern) {
                    String[] items = row.split(" ");
                    for (String item : items) {
                        Material mat = Material.getMaterial(item);
                        if (mat != null && mat != Material.AIR) {
                            if (exactItems != null && gridIndex < exactItems.size() && exactItems.get(gridIndex) != null) {
                                
                                ItemStack exactItem = exactItems.get(gridIndex);
                                recipe.addIngredient(new RecipeChoice.ExactChoice(exactItem));
                                plugin.debug("Using exact item at index " + gridIndex + ": " + exactItem);
                            } else {
                                
                                recipe.addIngredient(mat);
                            }
                        }
                        gridIndex++;
                    }
                }
                Bukkit.addRecipe(recipe);
                return true;
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to register recipe " + key.getKey() + ": " + e.getMessage());
        }
        return false;
    }

    private void saveRecipeState(String recipeKey, VanillaRecipeState state) {
        try {
            File file = new File(vanillaRecipesFolder, recipeKey + ".yml");
            YamlConfiguration config = new YamlConfiguration();
            config.set("disabled", state.isDisabled());
            config.set("original-recipe-key", state.getOriginalRecipeKey());
            config.set("current-variant-index", state.getCurrentVariantIndex());

            if (state.getCustomResultAmount() != null) {
                config.set("custom-result-amount", state.getCustomResultAmount());
            }

            int maxVariants = getMaxVariantsForRecipe(recipeKey);

            for (int variantIndex = 0; variantIndex < maxVariants; variantIndex++) {
                
                List<String> pattern = state.getPatternForVariant(variantIndex);
                if (pattern != null && !pattern.isEmpty()) {
                    config.set("variants." + variantIndex + ".pattern", pattern);
                    config.set("variants." + variantIndex + ".type",
                            state.getTypeForVariant(variantIndex).name());
                }

                if (state.isVariantDisabled(variantIndex)) {
                    config.set("variants." + variantIndex + ".disabled", true);
                }

                Integer variantAmount = state.getResultAmountForVariant(variantIndex);
                if (variantAmount != null) {
                    config.set("variants." + variantIndex + ".result-amount", variantAmount);
                }

                List<ItemStack> exactItems = state.getExactItemsForVariant(variantIndex);
                if (exactItems != null && !exactItems.isEmpty()) {
                    List<String> serializedItems = new ArrayList<>();
                    for (ItemStack exactItem : exactItems) {
                        if (exactItem != null) {
                            serializedItems.add(ItemStackSerializer.toBase64(exactItem));
                        } else {
                            serializedItems.add(null);
                        }
                    }
                    config.set("variants." + variantIndex + ".exact-ingredients", serializedItems);
                }
            }

            if (state.getCraftEventPresetName() != null && !state.getCraftEventPresetName().isEmpty()) {
                config.set("craft-event-preset", state.getCraftEventPresetName());
            }

            RecipeConditions conditions = state.getConditions();
            if (conditions != null && conditions.hasAnyCondition()) {
                if (conditions.hasPermission()) {
                    config.set("conditions.permission", conditions.getPermission());
                }
                if (conditions.hasXpLevelRequirement()) {
                    config.set("conditions.xp-level", conditions.getRequiredXpLevel());
                }
                if (conditions.hasXpReward()) {
                    config.set("conditions.xp-reward", conditions.getXpReward());
                }
                if (conditions.hasCooldown()) {
                    config.set("conditions.cooldown", conditions.getCooldownSeconds());
                }
                if (conditions.hasDailyLimit()) {
                    config.set("conditions.limit-daily", conditions.getCraftLimitDaily());
                }
                if (conditions.hasWeeklyLimit()) {
                    config.set("conditions.limit-weekly", conditions.getCraftLimitWeekly());
                }
                if (conditions.hasTotalLimit()) {
                    config.set("conditions.limit-total", conditions.getCraftLimitTotal());
                }
                if (conditions.hasMoneyCost()) {
                    config.set("conditions.money-cost", conditions.getMoneyCost());
                }
            }

            config.save(file);
            plugin.debug("Saved vanilla recipe state: " + recipeKey);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save vanilla recipe state: " + e.getMessage());
        }
    }

    public Map<String, VanillaRecipeInfo> getAllVanillaRecipes() {
        return Collections.unmodifiableMap(allVanillaRecipes);
    }

    public VanillaRecipeInfo getRecipeInfo(String recipeKey) {
        return allVanillaRecipes.get(recipeKey);
    }

    public VanillaRecipeState getRecipeState(String recipeKey) {
        return modifiedRecipes.get(recipeKey);
    }

    public boolean isRecipeDisabled(String recipeKey) {
        VanillaRecipeState state = modifiedRecipes.get(recipeKey);
        return state != null && state.isDisabled();
    }

    public boolean isRecipeChanged(String recipeKey) {
        VanillaRecipeState state = modifiedRecipes.get(recipeKey);
        return state != null && state.hasChangedRecipe();
    }

    public java.util.Set<String> getAllDisabledRecipeKeys() {
        return modifiedRecipes.entrySet().stream()
                .filter(entry -> entry.getValue().isDisabled())
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    public List<VanillaRecipeInfo> searchRecipes(String query) {
        String lowerQuery = query.toLowerCase();
        return allVanillaRecipes.values().stream()
                .filter(recipe -> {
                    
                    if (recipe.getDisplayName().toLowerCase().contains(lowerQuery)) {
                        return true;
                    }
                    
                    String materialEn = recipe.getResultMaterial().name().toLowerCase().replace("_", " ");
                    if (materialEn.contains(lowerQuery)) {
                        return true;
                    }
                    
                    String materialLocalized = plugin.getLanguageManager()
                            .getMaterialName(recipe.getResultMaterial().name()).toLowerCase();
                    if (materialLocalized.contains(lowerQuery)) {
                        return true;
                    }
                    
                    String recipeNameLocalized = plugin.getLanguageManager()
                            .getVanillaRecipeName(recipe.getKey().replace("minecraft:", "")).toLowerCase();
                    return recipeNameLocalized.contains(lowerQuery);
                })
                .collect(Collectors.toList());
    }

    public List<VanillaRecipeInfo> getRecipesByCategory(VanillaRecipeInfo.RecipeCategory category) {
        return allVanillaRecipes.values().stream()
                .filter(recipe -> recipe.getCategory() == category)
                .collect(Collectors.toList());
    }

    public List<VanillaRecipeInfo> getRecipesByStation(VanillaRecipeInfo.RecipeStation station) {
        return allVanillaRecipes.values().stream()
                .filter(recipe -> recipe.getStation() == station)
                .collect(Collectors.toList());
    }

    public void updateRecipesForAllPlayers() {
        for (org.bukkit.entity.Player player : Bukkit.getOnlinePlayers()) {
            updateRecipesForPlayer(player);
        }
    }

    public void updateRecipesForPlayer(org.bukkit.entity.Player player) {
        try {
            
            for (Map.Entry<String, VanillaRecipeState> entry : modifiedRecipes.entrySet()) {
                if (entry.getValue().isDisabled()) {
                    NamespacedKey key = NamespacedKey.minecraft(entry.getKey());
                    player.undiscoverRecipe(key);
                    plugin.debug("Undiscovered recipe for player " + player.getName() + ": " + entry.getKey());
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to update recipes for player " + player.getName() + ": " + e.getMessage());
        }
    }

    public static class VanillaRecipeState {
        private boolean disabled; 
        private final String originalRecipeKey;
        private int currentVariantIndex;
        private Map<Integer, List<String>> variantPatterns;
        private Map<Integer, RecipeType> variantTypes;
        private Integer customResultAmount; 
        private Map<Integer, List<ItemStack>> variantExactItems; 
        private Map<Integer, Integer> variantResultAmounts; 
        private Map<Integer, Boolean> variantDisabled; 
        private String craftEventPresetName; 
        private RecipeConditions conditions; 

        public VanillaRecipeState(boolean disabled, String originalRecipeKey) {
            this.disabled = disabled;
            this.originalRecipeKey = originalRecipeKey;
            this.currentVariantIndex = 0;
            this.variantPatterns = new HashMap<>();
            this.variantTypes = new HashMap<>();
            this.customResultAmount = null;
            this.variantExactItems = new HashMap<>();
            this.variantResultAmounts = new HashMap<>();
            this.variantDisabled = new HashMap<>();
            this.craftEventPresetName = null;
            this.conditions = null;
        }

        public boolean isDisabled() {
            return disabled;
        }

        public void setDisabled(boolean disabled) {
            this.disabled = disabled;
        }

        public boolean isVariantDisabled(int variantIndex) {
            return variantDisabled.getOrDefault(variantIndex, false);
        }

        public void setVariantDisabled(int variantIndex, boolean disabled) {
            variantDisabled.put(variantIndex, disabled);
        }

        public boolean hasAnyDisabledVariant() {
            return variantDisabled.values().stream().anyMatch(d -> d);
        }

        public Map<Integer, Boolean> getAllVariantDisabled() {
            return variantDisabled;
        }

        public String getCraftEventPresetName() {
            return craftEventPresetName;
        }

        public void setCraftEventPresetName(String presetName) {
            this.craftEventPresetName = presetName;
        }

        public RecipeConditions getConditions() {
            return conditions;
        }

        public void setConditions(RecipeConditions conditions) {
            this.conditions = conditions;
        }

        public String getOriginalRecipeKey() {
            return originalRecipeKey;
        }

        public int getCurrentVariantIndex() {
            return currentVariantIndex;
        }

        public void setCurrentVariantIndex(int index) {
            this.currentVariantIndex = index;
        }

        public List<String> getChangedPattern() {
            return variantPatterns.get(currentVariantIndex);
        }

        public List<String> getPatternForVariant(int variantIndex) {
            return variantPatterns.get(variantIndex);
        }

        public void setPatternForVariant(int variantIndex, List<String> pattern) {
            variantPatterns.put(variantIndex, pattern);
        }

        public RecipeType getChangedType() {
            return variantTypes.getOrDefault(currentVariantIndex, RecipeType.SHAPED);
        }

        public RecipeType getTypeForVariant(int variantIndex) {
            return variantTypes.getOrDefault(variantIndex, RecipeType.SHAPED);
        }

        public void setTypeForVariant(int variantIndex, RecipeType type) {
            variantTypes.put(variantIndex, type);
        }

        public boolean hasChangedRecipe() {
            return !variantPatterns.isEmpty();
        }

        public Map<Integer, List<String>> getAllVariantPatterns() {
            return variantPatterns;
        }

        public Map<Integer, RecipeType> getAllVariantTypes() {
            return variantTypes;
        }

        @Deprecated
        public void setResultAmount(int amount) {
            this.customResultAmount = amount;
        }

        @Deprecated
        public Integer getCustomResultAmount() {
            return customResultAmount;
        }

        public void setResultAmountForVariant(int variantIndex, int amount) {
            variantResultAmounts.put(variantIndex, amount);
        }

        public Integer getResultAmountForVariant(int variantIndex) {
            return variantResultAmounts.get(variantIndex);
        }

        public Map<Integer, Integer> getAllVariantResultAmounts() {
            return variantResultAmounts;
        }

        public boolean hasAnyVariantResultAmount() {
            return !variantResultAmounts.isEmpty();
        }

        public List<ItemStack> getExactItemsForVariant(int variantIndex) {
            return variantExactItems.get(variantIndex);
        }

        public void setExactItemsForVariant(int variantIndex, List<ItemStack> exactItems) {
            variantExactItems.put(variantIndex, exactItems);
        }

        public Map<Integer, List<ItemStack>> getAllVariantExactItems() {
            return variantExactItems;
        }
    }
}