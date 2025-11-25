package org.hikarii.customrecipes.config;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.hikarii.customrecipes.util.ItemStackSerializer;
import org.hikarii.customrecipes.CustomRecipes;
import org.hikarii.customrecipes.recipe.CraftEventPreset;
import org.hikarii.customrecipes.recipe.CraftEvents;
import org.hikarii.customrecipes.recipe.CustomRecipe;
import org.hikarii.customrecipes.recipe.RecipeConditions;
import org.hikarii.customrecipes.recipe.RecipeType;
import org.hikarii.customrecipes.recipe.data.FurnaceRecipeData;
import org.hikarii.customrecipes.recipe.data.RandomResult;
import org.hikarii.customrecipes.recipe.data.RandomResultPool;
import org.hikarii.customrecipes.recipe.data.RecipeIngredient;
import org.hikarii.customrecipes.recipe.data.ShapedRecipeData;
import org.hikarii.customrecipes.recipe.data.ShapelessRecipeData;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RecipeConfigLoader {
    private final CustomRecipes plugin;
    public RecipeConfigLoader(CustomRecipes plugin) {
        this.plugin = plugin;
    }

    public CustomRecipe loadRecipe(String key, ConfigurationSection section) throws ValidationException {
        if (section == null) {
            throw new ValidationException("Recipe '" + key + "' has no configuration section");
        }
        try {
            String name = section.getString("crafted-name");
            List<String> description = section.getStringList("crafted-description");
            if (name == null || name.isEmpty()) {
                name = section.getString("crafted-name");
                if (name == null || name.isEmpty()) {
                    name = section.getString("gui-name");
                }
            }

            if (description == null || description.isEmpty()) {
                description = section.getStringList("crafted-description");
                if (description == null || description.isEmpty()) {
                    description = section.getStringList("gui-description");
                }
            }

            String typeStr = section.getString("type", "SHAPED");
            RecipeType type = RecipeType.fromString(typeStr);
            ItemStack resultItem;
            if (section.contains("result-full")) {
                try {
                    Object resultObj = section.get("result-full");
                    Map<String, Object> resultMap = null;
                    if (resultObj instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> map = (Map<String, Object>) resultObj;
                        resultMap = map;
                    } else if (resultObj instanceof ConfigurationSection) {
                        ConfigurationSection resultSection = (ConfigurationSection) resultObj;
                        resultMap = configSectionToMap(resultSection);
                    }

                    if (resultMap != null) {
                        resultItem = ItemStackSerializer.fromMap(resultMap);
                    } else {
                        throw new ValidationException("Invalid result-full format");
                    }
                } catch (Exception e) {
                    throw new ValidationException("Failed to load result-full item: " + e.getMessage(), e);
                }
            }
            else if (section.contains("result")) {
                try {
                    Object resultObj = section.get("result");
                    Map<String, Object> resultMap = null;
                    if (resultObj instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> map = (Map<String, Object>) resultObj;
                        resultMap = map;
                    } else if (resultObj instanceof ConfigurationSection) {
                        ConfigurationSection resultSection = (ConfigurationSection) resultObj;
                        resultMap = configSectionToMap(resultSection);
                    }

                    if (resultMap != null) {
                        resultItem = ItemStackSerializer.fromMap(resultMap);
                    } else {
                        throw new ValidationException("Invalid result format - not a Map or ConfigurationSection");
                    }
                } catch (Exception e) {
                    throw new ValidationException("Failed to load result item: " + e.getMessage(), e);
                }
            }
            else {
                String materialStr = section.getString("material");
                if (materialStr == null || materialStr.isEmpty()) {
                    throw new ValidationException("Recipe '" + key + "' is missing 'material' field");
                }

                Material resultMaterial = Material.getMaterial(materialStr.toUpperCase());
                if (resultMaterial == null) {
                    throw new ValidationException("Recipe '" + key + "' has invalid material: " + materialStr);
                }

                int resultAmount = section.getInt("amount", 1);
                if (resultAmount < 1 || resultAmount > 64) {
                    throw new ValidationException("Recipe '" + key + "' amount must be between 1 and 64");
                }
                resultItem = new ItemStack(resultMaterial, resultAmount);
            }
            if ((name == null || name.isEmpty()) && resultItem != null) {
                ItemMeta meta = resultItem.getItemMeta();
                if (meta != null && meta.hasDisplayName()) {
                    name = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                            .serialize(meta.displayName());
                }
            }
            if ((description == null || description.isEmpty()) && resultItem != null) {
                ItemMeta meta = resultItem.getItemMeta();
                if (meta != null && meta.hasLore() && meta.lore() != null) {
                    description = new ArrayList<>();
                    for (Component line : meta.lore()) {
                        String loreText = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                                .serialize(line);
                        if (loreText != null && !loreText.isEmpty()) {
                            description.add(loreText);
                        }
                    }
                }
            }
            boolean hidden = section.getBoolean("hidden", false);
            ShapedRecipeData recipeData = null;
            ShapelessRecipeData shapelessData = null;
            FurnaceRecipeData furnaceData = null;

            if (type == RecipeType.SHAPED) {
                recipeData = loadShapedRecipeData(key, section);
            } else if (type == RecipeType.SHAPELESS) {
                shapelessData = loadShapelessRecipeData(key, section);
            } else if (type.isFurnaceType()) {
                furnaceData = loadFurnaceRecipeData(key, section, type);
            } else {
                throw new ValidationException("Recipe type '" + type + "' is not yet supported");
            }
            List<PotionEffect> potionEffects = new ArrayList<>();
            if (section.contains("potion-effects")) {
                List<Map<?, ?>> effectsList = section.getMapList("potion-effects");
                for (Map<?, ?> effectMap : effectsList) {
                    try {
                        String effectName = (String) effectMap.get("type");
                        int duration = effectMap.containsKey("duration") ?
                                ((Number) effectMap.get("duration")).intValue() : 160;
                        int amplifier = effectMap.containsKey("amplifier") ?
                                ((Number) effectMap.get("amplifier")).intValue() : 0;
                        PotionEffectType effectType = PotionEffectType.getByName(effectName);
                        if (effectType != null) {
                            potionEffects.add(new PotionEffect(effectType, duration, amplifier));
                        } else {
                            plugin.getLogger().warning("Unknown potion effect: " + effectName);
                        }
                    } catch (Exception e) {
                        plugin.getLogger().warning("Failed to parse potion effect: " + e.getMessage());
                    }
                }
            }

            RecipeConditions conditions = loadConditions(section);

            RandomResultPool randomResults = loadRandomResults(section);

            CraftEvents craftEvents = loadCraftEvents(section);

            return new CustomRecipe(key, name, description, type, recipeData, shapelessData, furnaceData, resultItem, hidden, potionEffects, conditions, randomResults, craftEvents);
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Error loading recipe '" + key + "': " + e.getMessage(), e);
        }
    }

    private ShapedRecipeData loadShapedRecipeData(String key, ConfigurationSection section)
            throws ValidationException {
        List<String> recipePattern = section.getStringList("recipe");
        if (recipePattern.isEmpty() || recipePattern.size() > 3) {
            throw new ValidationException("Recipe '" + key + "' has invalid recipe data: Recipe must have 1-3 rows");
        }
        while (recipePattern.size() < 3) {
            recipePattern.add("AIR AIR AIR");
        }
        for (int i = 0; i < recipePattern.size(); i++) {
            String row = recipePattern.get(i);
            String[] items = row.split(" ");
            if (items.length < 3) {
                StringBuilder paddedRow = new StringBuilder(row);
                for (int j = items.length; j < 3; j++) {
                    if (paddedRow.length() > 0) paddedRow.append(" ");
                    paddedRow.append("AIR");
                }
                recipePattern.set(i, paddedRow.toString());
            }
        }
        List<ItemStack> exactItems = null;
        if (section.contains("exact-ingredients")) {
            List<?> exactItemsList = section.getList("exact-ingredients");
            exactItems = new ArrayList<>();
            for (Object obj : exactItemsList) {
                if (obj == null) {
                    exactItems.add(null);
                } else if (obj instanceof String) {
                    String base64 = (String) obj;
                    try {
                        ItemStack item = ItemStackSerializer.fromBase64(base64);
                        exactItems.add(item);
                    } catch (Exception e) {
                        plugin.getLogger().warning("Failed to deserialize exact item from Base64: " + e.getMessage());
                        exactItems.add(null);
                    }
                } else {
                    exactItems.add(null);
                }
            }
        }
        else if (section.contains("exact-items")) {
            List<?> exactItemsList = section.getList("exact-items");
            exactItems = new ArrayList<>();
            for (Object obj : exactItemsList) {
                if (obj == null) {
                    exactItems.add(null);
                } else if (obj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> itemMap = (Map<String, Object>) obj;
                    try {
                        ItemStack item = ItemStackSerializer.fromMap(itemMap);
                        exactItems.add(item);
                    } catch (Exception e) {
                        plugin.getLogger().warning("Failed to deserialize exact item: " + e.getMessage());
                        exactItems.add(null);
                    }
                } else {
                    exactItems.add(null);
                }
            }
        }
        try {
            return ShapedRecipeData.fromConfigList(recipePattern, exactItems);
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Recipe '" + key + "' has invalid recipe data: " + e.getMessage(), e);
        }
    }

    private ShapelessRecipeData loadShapelessRecipeData(String key, ConfigurationSection section)
            throws ValidationException {
        List<String> ingredientList = section.getStringList("ingredients");
        if (ingredientList.isEmpty()) {
            throw new ValidationException("Recipe '" + key + "' has no ingredients");
        }

        List<ItemStack> exactItems = null;
        if (section.contains("exact-ingredients")) {
            List<?> exactItemsList = section.getList("exact-ingredients");
            exactItems = new ArrayList<>();
            for (Object obj : exactItemsList) {
                if (obj == null) {
                    exactItems.add(null);
                } else if (obj instanceof String) {
                    
                    String base64 = (String) obj;
                    try {
                        ItemStack item = ItemStackSerializer.fromBase64(base64);
                        exactItems.add(item);
                    } catch (Exception e) {
                        plugin.getLogger().warning("Failed to deserialize exact shapeless ingredient from Base64: " + e.getMessage());
                        exactItems.add(null);
                    }
                } else if (obj instanceof Map) {
                    
                    @SuppressWarnings("unchecked")
                    Map<String, Object> itemMap = (Map<String, Object>) obj;
                    try {
                        ItemStack item = ItemStackSerializer.fromMap(itemMap);
                        exactItems.add(item);
                    } catch (Exception e) {
                        plugin.getLogger().warning("Failed to deserialize exact shapeless ingredient: " + e.getMessage());
                        exactItems.add(null);
                    }
                } else {
                    exactItems.add(null);
                }
            }
        }

        return ShapelessRecipeData.fromConfigList(ingredientList, exactItems);
    }

    private FurnaceRecipeData loadFurnaceRecipeData(String key, ConfigurationSection section, RecipeType type)
            throws ValidationException {
        FurnaceRecipeData.Builder builder = FurnaceRecipeData.builder();

        String inputStr = section.getString("input");
        if (inputStr == null || inputStr.isEmpty()) {
            throw new ValidationException("Furnace recipe '" + key + "' is missing 'input' field");
        }

        Material inputMaterial = Material.getMaterial(inputStr.toUpperCase());
        if (inputMaterial == null) {
            throw new ValidationException("Furnace recipe '" + key + "' has invalid input material: " + inputStr);
        }
        builder.input(inputMaterial);

        if (section.contains("input-full")) {
            try {
                Object inputObj = section.get("input-full");
                Map<String, Object> inputMap = null;
                if (inputObj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> map = (Map<String, Object>) inputObj;
                    inputMap = map;
                } else if (inputObj instanceof ConfigurationSection) {
                    inputMap = configSectionToMap((ConfigurationSection) inputObj);
                }

                if (inputMap != null) {
                    ItemStack exactInput = ItemStackSerializer.fromMap(inputMap);
                    builder.exactInput(exactInput);
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load exact input for '" + key + "': " + e.getMessage());
            }
        }

        int defaultCookingTime = FurnaceRecipeData.getDefaultCookingTime(type);
        int cookingTime = section.getInt("cooking-time", defaultCookingTime);
        builder.cookingTime(cookingTime);

        float experience = (float) section.getDouble("experience", 0.0);
        builder.experience(experience);

        String group = section.getString("group", "");
        builder.group(group);

        if (section.contains("custom-fuels")) {
            List<FurnaceRecipeData.CustomFuel> customFuels = loadCustomFuels(section.getConfigurationSection("custom-fuels"));
            builder.customFuels(customFuels);
        }

        return builder.build();
    }

    private List<FurnaceRecipeData.CustomFuel> loadCustomFuels(ConfigurationSection section) {
        List<FurnaceRecipeData.CustomFuel> fuels = new ArrayList<>();
        if (section == null) {
            return fuels;
        }

        for (String fuelKey : section.getKeys(false)) {
            ConfigurationSection fuelSection = section.getConfigurationSection(fuelKey);
            if (fuelSection == null) continue;

            try {
                String materialStr = fuelSection.getString("material");
                if (materialStr == null || materialStr.isEmpty()) continue;

                Material material = Material.getMaterial(materialStr.toUpperCase());
                if (material == null) {
                    plugin.getLogger().warning("Invalid custom fuel material: " + materialStr);
                    continue;
                }

                int burnTime = fuelSection.getInt("burn-time", 200);
                RecipeIngredient ingredient = new RecipeIngredient(material);
                ItemStack exactItem = null;

                if (fuelSection.contains("item-full")) {
                    try {
                        Object itemObj = fuelSection.get("item-full");
                        Map<String, Object> itemMap = null;
                        if (itemObj instanceof Map) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> map = (Map<String, Object>) itemObj;
                            itemMap = map;
                        } else if (itemObj instanceof ConfigurationSection) {
                            itemMap = configSectionToMap((ConfigurationSection) itemObj);
                        }

                        if (itemMap != null) {
                            exactItem = ItemStackSerializer.fromMap(itemMap);
                        }
                    } catch (Exception e) {
                        plugin.getLogger().warning("Failed to load exact fuel item '" + fuelKey + "': " + e.getMessage());
                    }
                }

                fuels.add(new FurnaceRecipeData.CustomFuel(ingredient, exactItem, burnTime));
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load custom fuel '" + fuelKey + "': " + e.getMessage());
            }
        }

        return fuels;
    }

    private Map<String, Object> configSectionToMap(ConfigurationSection section) {
        Map<String, Object> map = new HashMap<>();
        for (String key : section.getKeys(false)) {
            Object value = section.get(key);
            if (value instanceof ConfigurationSection) {
                map.put(key, configSectionToMap((ConfigurationSection) value));
            } else if (value instanceof List) {
                map.put(key, value);
            } else {
                map.put(key, value);
            }
        }
        return map;
    }

    public void validateKey(String key) throws ValidationException {
        if (key == null || key.isEmpty()) {
            throw new ValidationException("Recipe key cannot be empty");
        }
        if (!key.matches("[a-zA-Z0-9_-]+")) {
            throw new ValidationException(
                    "Recipe key '" + key + "' contains invalid characters. Only letters, numbers, underscores, and hyphens are allowed."
            );
        }
    }

    private RecipeConditions loadConditions(ConfigurationSection section) {
        if (!section.contains("conditions")) {
            return new RecipeConditions();
        }

        ConfigurationSection conditionsSection = section.getConfigurationSection("conditions");
        if (conditionsSection == null) {
            return new RecipeConditions();
        }

        String permission = conditionsSection.getString("permission", null);
        int requiredXpLevel = conditionsSection.getInt("xp-level", 0);
        int xpReward = conditionsSection.getInt("xp-reward", 0);
        int cooldownSeconds = conditionsSection.getInt("cooldown", 0);
        int craftLimitDaily = conditionsSection.getInt("limit-daily", 0);
        int craftLimitWeekly = conditionsSection.getInt("limit-weekly", 0);
        int craftLimitTotal = conditionsSection.getInt("limit-total", 0);
        double moneyCost = conditionsSection.getDouble("money-cost", 0);

        return new RecipeConditions(permission, requiredXpLevel, xpReward,
                cooldownSeconds, craftLimitDaily, craftLimitWeekly, craftLimitTotal, moneyCost);
    }

    private RandomResultPool loadRandomResults(ConfigurationSection section) {
        
        String key = section.contains("random-results") ? "random-results" : "random_results";
        if (!section.contains(key)) {
            plugin.getLogger().info("[DEBUG RandomResults] No random results key found in section");
            return null;
        }

        plugin.getLogger().info("[DEBUG RandomResults] Loading random results from key: " + key);

        ConfigurationSection randomSection = section.getConfigurationSection(key);
        if (randomSection == null) {
            plugin.getLogger().info("[DEBUG RandomResults] randomSection is null");
            return null;
        }

        boolean showChances = randomSection.getBoolean("show-chances", randomSection.getBoolean("show_chances", true));
        
        int failureChance = randomSection.getInt("failure-chance", randomSection.getInt("failure_chance", 0));
        List<?> itemsList = randomSection.getList("items");
        plugin.getLogger().info("[DEBUG RandomResults] itemsList: " + (itemsList != null ? itemsList.size() + " items" : "null") + ", failureChance: " + failureChance);
        if (itemsList == null || itemsList.isEmpty()) {
            return null;
        }

        List<RandomResult> results = new ArrayList<>();
        for (Object obj : itemsList) {
            if (obj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> itemMap = (Map<String, Object>) obj;
                try {
                    RandomResult result = parseRandomResultItem(itemMap);
                    if (result != null) {
                        results.add(result);
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to parse random result item: " + e.getMessage());
                }
            } else if (obj instanceof ConfigurationSection) {
                try {
                    ConfigurationSection itemSection = (ConfigurationSection) obj;
                    RandomResult result = parseRandomResultSection(itemSection);
                    if (result != null) {
                        results.add(result);
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to parse random result item: " + e.getMessage());
                }
            }
        }

        if (results.isEmpty()) {
            return null;
        }

        return new RandomResultPool(results, showChances, failureChance);
    }

    private RandomResult parseRandomResultItem(Map<String, Object> itemMap) {
        
        ItemStack item = null;
        boolean loadedFromSerializer = false;
        try {
            item = ItemStackSerializer.fromMap(itemMap);
            loadedFromSerializer = (item != null);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to deserialize random result item: " + e.getMessage());
        }

        if (item == null) {
            String materialStr = (String) itemMap.get("material");
            if (materialStr == null || materialStr.isEmpty()) {
                
                materialStr = (String) itemMap.get("type");
            }
            if (materialStr == null || materialStr.isEmpty()) {
                return null;
            }

            Material material = Material.getMaterial(materialStr.toUpperCase());
            if (material == null) {
                plugin.getLogger().warning("Invalid material in random_results: " + materialStr);
                return null;
            }

            int amount = 1;
            if (itemMap.containsKey("amount")) {
                amount = ((Number) itemMap.get("amount")).intValue();
            }
            item = new ItemStack(material, amount);
        }

        int weight = 1;
        if (itemMap.containsKey("weight")) {
            weight = ((Number) itemMap.get("weight")).intValue();
        } else if (itemMap.containsKey("chance")) {
            
            weight = ((Number) itemMap.get("chance")).intValue();
        }

        String resultName = null;
        if (itemMap.containsKey("name")) {
            resultName = (String) itemMap.get("name");
        }

        if (!loadedFromSerializer && itemMap.containsKey("lore")) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                @SuppressWarnings("unchecked")
                List<String> loreList = (List<String>) itemMap.get("lore");
                if (loreList != null && !loreList.isEmpty()) {
                    List<Component> lore = new ArrayList<>();
                    for (String line : loreList) {
                        lore.add(org.hikarii.customrecipes.util.MessageUtil.colorize(line)
                                .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));
                    }
                    meta.lore(lore);
                    item.setItemMeta(meta);
                }
            }
        }

        return new RandomResult(item, weight, resultName);
    }

    private RandomResult parseRandomResultSection(ConfigurationSection section) {
        String materialStr = section.getString("material");
        if (materialStr == null || materialStr.isEmpty()) {
            
            materialStr = section.getString("type");
        }
        if (materialStr == null || materialStr.isEmpty()) {
            return null;
        }

        Material material = Material.getMaterial(materialStr.toUpperCase());
        if (material == null) {
            plugin.getLogger().warning("Invalid material in random_results: " + materialStr);
            return null;
        }

        int amount = section.getInt("amount", 1);
        int weight = section.getInt("weight", section.getInt("chance", 1));
        
        String resultName = section.getString("name", null);

        ItemStack item = new ItemStack(material, amount);

        List<String> loreList = section.getStringList("lore");
        if (!loreList.isEmpty()) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                List<Component> lore = new ArrayList<>();
                for (String line : loreList) {
                    lore.add(org.hikarii.customrecipes.util.MessageUtil.colorize(line)
                            .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));
                }
                meta.lore(lore);
                item.setItemMeta(meta);
            }
        }

        return new RandomResult(item, weight, resultName);
    }

    private CraftEvents loadCraftEvents(ConfigurationSection section) {
        
        plugin.getLogger().info("[DEBUG CraftEvents] Section keys: " + section.getKeys(false));

        if (section.contains("craft_events_preset")) {
            String presetName = section.getString("craft_events_preset");
            plugin.getLogger().info("[DEBUG CraftEvents] Found preset name in config: '" + presetName + "'");
            if (presetName != null && !presetName.isEmpty()) {
                CraftEventPreset preset = plugin.getCraftEventPresetManager().getPreset(presetName);
                plugin.getLogger().info("[DEBUG CraftEvents] Preset lookup result: " + (preset != null ? "FOUND" : "NOT FOUND"));
                if (preset != null) {
                    plugin.getLogger().info("[DEBUG CraftEvents] Loaded preset '" + presetName + "' with " +
                            preset.getSounds().size() + " sounds, " +
                            preset.getParticles().size() + " particles, " +
                            preset.getCommands().size() + " commands");
                    return preset.toCraftEvents();
                } else {
                    plugin.getLogger().warning("Craft event preset not found: " + presetName);
                    
                    plugin.getLogger().info("[DEBUG CraftEvents] Available presets: " +
                        plugin.getCraftEventPresetManager().getAllPresets().stream()
                            .map(CraftEventPreset::getName)
                            .toList());
                }
            }
        } else {
            plugin.getLogger().info("[DEBUG CraftEvents] No 'craft_events_preset' key found in section");
        }

        if (!section.contains("craft_events")) {
            return null;
        }

        ConfigurationSection eventsSection = section.getConfigurationSection("craft_events");
        if (eventsSection == null) {
            return null;
        }

        List<CraftEvents.SoundEvent> sounds = loadSoundEvents(eventsSection);
        List<CraftEvents.ParticleEvent> particles = loadParticleEvents(eventsSection);
        List<CraftEvents.CommandEvent> commands = loadCommandEvents(eventsSection);

        if (sounds.isEmpty() && particles.isEmpty() && commands.isEmpty()) {
            return null;
        }

        return new CraftEvents(sounds, particles, commands);
    }

    private List<CraftEvents.SoundEvent> loadSoundEvents(ConfigurationSection section) {
        List<CraftEvents.SoundEvent> sounds = new ArrayList<>();

        if (!section.contains("sounds")) {
            return sounds;
        }

        List<Map<?, ?>> soundsList = section.getMapList("sounds");
        for (Map<?, ?> soundMap : soundsList) {
            try {
                String soundName = (String) soundMap.get("sound");
                if (soundName == null) continue;

                Sound sound = Sound.valueOf(soundName.toUpperCase());
                float volume = soundMap.containsKey("volume") ?
                        ((Number) soundMap.get("volume")).floatValue() : 1.0f;
                float pitch = soundMap.containsKey("pitch") ?
                        ((Number) soundMap.get("pitch")).floatValue() : 1.0f;

                sounds.add(new CraftEvents.SoundEvent(sound, volume, pitch));
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid sound in craft_events: " + soundMap.get("sound"));
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to parse sound event: " + e.getMessage());
            }
        }

        return sounds;
    }

    private List<CraftEvents.ParticleEvent> loadParticleEvents(ConfigurationSection section) {
        List<CraftEvents.ParticleEvent> particles = new ArrayList<>();

        if (!section.contains("particles")) {
            return particles;
        }

        List<Map<?, ?>> particlesList = section.getMapList("particles");
        for (Map<?, ?> particleMap : particlesList) {
            try {
                String particleName = (String) particleMap.get("particle");
                if (particleName == null) continue;

                Particle particle = Particle.valueOf(particleName.toUpperCase());
                int count = particleMap.containsKey("count") ?
                        ((Number) particleMap.get("count")).intValue() : 10;
                double offsetX = particleMap.containsKey("offset_x") ?
                        ((Number) particleMap.get("offset_x")).doubleValue() : 0.5;
                double offsetY = particleMap.containsKey("offset_y") ?
                        ((Number) particleMap.get("offset_y")).doubleValue() : 0.5;
                double offsetZ = particleMap.containsKey("offset_z") ?
                        ((Number) particleMap.get("offset_z")).doubleValue() : 0.5;
                double speed = particleMap.containsKey("speed") ?
                        ((Number) particleMap.get("speed")).doubleValue() : 0.1;

                particles.add(new CraftEvents.ParticleEvent(particle, count, offsetX, offsetY, offsetZ, speed));
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid particle in craft_events: " + particleMap.get("particle"));
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to parse particle event: " + e.getMessage());
            }
        }

        return particles;
    }

    private List<CraftEvents.CommandEvent> loadCommandEvents(ConfigurationSection section) {
        List<CraftEvents.CommandEvent> commands = new ArrayList<>();

        if (!section.contains("commands")) {
            return commands;
        }

        List<Map<?, ?>> commandsList = section.getMapList("commands");
        for (Map<?, ?> commandMap : commandsList) {
            try {
                String command = (String) commandMap.get("command");
                if (command == null || command.isEmpty()) continue;

                Object typeObj = commandMap.get("type");
                String typeStr = typeObj != null ? typeObj.toString() : "CONSOLE";
                CraftEvents.CommandEvent.CommandType type;
                try {
                    type = CraftEvents.CommandEvent.CommandType.valueOf(typeStr.toUpperCase());
                } catch (IllegalArgumentException e) {
                    type = CraftEvents.CommandEvent.CommandType.CONSOLE;
                }

                commands.add(new CraftEvents.CommandEvent(command, type));
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to parse command event: " + e.getMessage());
            }
        }

        return commands;
    }
}