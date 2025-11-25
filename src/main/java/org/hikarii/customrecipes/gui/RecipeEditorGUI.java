package org.hikarii.customrecipes.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.enchantments.Enchantment;
import org.hikarii.customrecipes.CustomRecipes;
import org.hikarii.customrecipes.recipe.CustomRecipe;
import org.hikarii.customrecipes.recipe.RecipeType;
import org.hikarii.customrecipes.recipe.RecipeWorldManager;
import org.hikarii.customrecipes.recipe.data.FurnaceRecipeData;
import org.hikarii.customrecipes.recipe.data.RecipeIngredient;
import org.hikarii.customrecipes.recipe.data.RandomResult;
import org.hikarii.customrecipes.recipe.data.RandomResultPool;
import org.hikarii.customrecipes.util.ItemStackSerializer;
import org.hikarii.customrecipes.util.MessageUtil;
import org.hikarii.customrecipes.language.LanguageManager;
import java.io.File;
import java.util.*;
import java.util.function.Consumer;

public class RecipeEditorGUI implements Listener {
    private final CustomRecipes plugin;
    private final Player player;
    private final CustomRecipe recipe;
    private final LanguageManager lang;
    private final Inventory inventory;
    private boolean deleteConfirmation = false;
    private static final int EDIT_RESULT_SLOT = 25;

    private static final int BTN_EDIT_PATTERN = 45;
    private static final int BTN_EDIT_RESULT = 46;
    private static final int BTN_CONDITIONS = 47;
    private static final int BTN_WORLD_SETTINGS = 48;
    private static final int BTN_VISIBILITY = 49;
    private static final int BTN_TOGGLE = 50;
    private static final int BTN_RANDOM_RESULTS = 51;
    private static final int BTN_GET_ITEM = 52;
    private static final int BTN_BACK = 53;

    private static final int BTN_INFO_BOOK = 44;  
    private static final int BTN_DELETE = 43;     

    private static final int BTN_SMELTING_CONDITIONS = 36;  
    private static final int BTN_FURNACE_CONDITIONS = 37;   

    private static final int BTN_FURNACE_WORLD_SETTINGS = 47;
    private static final int BTN_FURNACE_VISIBILITY = 48;
    private static final int BTN_FURNACE_TOGGLE = 49;
    private boolean editMode = false;
    private ItemStack[] editGridItems = new ItemStack[9];
    private ItemStack editResultItem = null;
    private RandomResultPool randomResults;

    private static final int[] GRID_SLOTS = {
            10, 11, 12,
            19, 20, 21,
            28, 29, 30
    };

    public RecipeEditorGUI(CustomRecipes plugin, Player player, CustomRecipe recipe) {
        this.plugin = plugin;
        this.player = player;
        this.recipe = recipe;
        this.lang = plugin.getLanguageManager();
        this.randomResults = recipe.getRandomResults();

        plugin.getLogger().info("[DEBUG RandomResults] RecipeEditorGUI opened for: " + recipe.getKey());
        plugin.getLogger().info("[DEBUG RandomResults] recipe.getRandomResults() is null: " + (recipe.getRandomResults() == null));
        if (recipe.getRandomResults() != null) {
            plugin.getLogger().info("[DEBUG RandomResults] hasRandomResults: " + recipe.getRandomResults().hasRandomResults());
            plugin.getLogger().info("[DEBUG RandomResults] count: " + recipe.getRandomResults().getResults().size());
        }

        this.inventory = Bukkit.createInventory(
                null,
                54,
                MessageUtil.createGradientMenuTitle(lang.getMessage("gui.title.recipe_editor"))
        );
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        updateInventory();
    }

    public void open() {
        player.openInventory(inventory);
    }

    private void updateInventory() {
        inventory.clear();
        fillBorders();

        if (editMode) {
            addEditableGrid();
            addEqualsSign();
            addEditableResultItem();
            addSaveEditButton();
            addCancelEditButton();
        } else {
            addCraftingGrid();
            addEqualsSign();
            addResultItem();
            addInfoBook();
            addHiddenToggleButton();
            addToggleButton();
            addWorldSettingsButton();
            addEditRecipeButton();
            addEditItemButton();
            addConditionsButton();
            if (isFurnaceRecipe()) {
                addSmeltingConditionsButton();
            }
            addRandomResultsButton();
            addDeleteButton();
            addBackButton();
            addGiveItemButton();
        }
    }

    private void addEditableResultItem() {
        if (editResultItem != null && editResultItem.getType() != Material.AIR) {
            ItemStack display = editResultItem.clone();
            ItemMeta meta = display.getItemMeta();
            if (meta != null) {
                List<Component> lore = meta.hasLore() && meta.lore() != null ?
                        new ArrayList<>(meta.lore()) : new ArrayList<>();
                lore.add(Component.empty());
                lore.add(Component.text(lang.getMessage("lore.amount", Map.of("amount", String.valueOf(display.getAmount()))), NamedTextColor.YELLOW)
                        .decoration(TextDecoration.ITALIC, false));
                lore.add(Component.empty());
                lore.add(Component.text(lang.getMessage("lore.left_click_add"), NamedTextColor.GREEN)
                        .decoration(TextDecoration.ITALIC, true));
                lore.add(Component.text(lang.getMessage("lore.right_click_remove"), NamedTextColor.RED)
                        .decoration(TextDecoration.ITALIC, true));
                meta.lore(lore);
                display.setItemMeta(meta);
            }
            inventory.setItem(EDIT_RESULT_SLOT, display);
        } else {
            inventory.setItem(EDIT_RESULT_SLOT, null);
        }
    }

    private void addEditableGrid() {
        if (isFurnaceRecipe()) {
            
            addEditableFurnaceSlot(0, 12, lang.getMessage("furnace_creator.input_hint"));
            addEditableFurnaceSlot(1, 30, lang.getMessage("furnace_creator.fuel_hint"));

            ItemStack plus = new ItemStack(Material.ORANGE_STAINED_GLASS_PANE);
            ItemMeta plusMeta = plus.getItemMeta();
            plusMeta.displayName(Component.text("+", NamedTextColor.GOLD)
                    .decoration(TextDecoration.BOLD, true)
                    .decoration(TextDecoration.ITALIC, false));
            plus.setItemMeta(plusMeta);
            inventory.setItem(21, plus);
        } else {
            for (int i = 0; i < 9; i++) {
                if (editGridItems[i] != null && editGridItems[i].getType() != Material.AIR) {
                    ItemStack display = editGridItems[i].clone();
                    ItemMeta meta = display.getItemMeta();
                    if (meta != null) {
                        List<Component> originalLore = meta.hasLore() && meta.lore() != null ?
                                new ArrayList<>(meta.lore()) : new ArrayList<>();
                        List<Component> displayLore = new ArrayList<>(originalLore);
                        displayLore.add(Component.empty());
                        displayLore.add(Component.text(lang.getMessage("lore.amount", Map.of("amount", String.valueOf(display.getAmount()))), NamedTextColor.YELLOW)
                                .decoration(TextDecoration.ITALIC, false));

                        if (meta instanceof org.bukkit.inventory.meta.Damageable) {
                            org.bukkit.inventory.meta.Damageable damageable = (org.bukkit.inventory.meta.Damageable) meta;
                            if (damageable.hasDamage()) {
                                int maxDurability = display.getType().getMaxDurability();
                                int currentDurability = maxDurability - damageable.getDamage();
                                displayLore.add(Component.text(lang.getMessage("lore.durability",
                                                Map.of("current", String.valueOf(currentDurability), "max", String.valueOf(maxDurability))),
                                                NamedTextColor.AQUA)
                                        .decoration(TextDecoration.ITALIC, false));
                            }
                        }
                        displayLore.add(Component.empty());
                        displayLore.add(Component.text(lang.getMessage("lore.drag_to_replace"), NamedTextColor.AQUA)
                                .decoration(TextDecoration.ITALIC, true));
                        displayLore.add(Component.text(lang.getMessage("lore.left_click_add"), NamedTextColor.GREEN)
                                .decoration(TextDecoration.ITALIC, true));
                        displayLore.add(Component.text(lang.getMessage("lore.right_click_remove"), NamedTextColor.RED)
                                .decoration(TextDecoration.ITALIC, true));
                        displayLore.add(Component.text(lang.getMessage("lore.middle_click_delete"), NamedTextColor.DARK_RED)
                                .decoration(TextDecoration.ITALIC, true));
                        meta.lore(displayLore);
                        display.setItemMeta(meta);
                    }
                    inventory.setItem(GRID_SLOTS[i], display);
                } else {
                    inventory.setItem(GRID_SLOTS[i], null);
                }
            }
        }
    }

    private void addEditableFurnaceSlot(int gridIndex, int displaySlot, String hint) {
        if (editGridItems[gridIndex] != null && editGridItems[gridIndex].getType() != Material.AIR) {
            ItemStack display = editGridItems[gridIndex].clone();
            ItemMeta meta = display.getItemMeta();
            if (meta != null) {
                List<Component> displayLore = new ArrayList<>();
                displayLore.add(Component.empty());
                displayLore.add(Component.text(hint, NamedTextColor.YELLOW)
                        .decoration(TextDecoration.ITALIC, false));
                displayLore.add(Component.empty());
                displayLore.add(Component.text(lang.getMessage("lore.drag_to_replace"), NamedTextColor.AQUA)
                        .decoration(TextDecoration.ITALIC, true));
                displayLore.add(Component.text(lang.getMessage("lore.click_to_remove"), NamedTextColor.RED)
                        .decoration(TextDecoration.ITALIC, true));
                meta.lore(displayLore);
                display.setItemMeta(meta);
            }
            inventory.setItem(displaySlot, display);
        } else {
            inventory.setItem(displaySlot, null);
        }
    }

    private void addEditRecipeButton() {
        ItemStack button = new ItemStack(Material.CRAFTING_TABLE);
        ItemMeta meta = button.getItemMeta();
        meta.displayName(Component.text(lang.getMessage("editor.button.edit_pattern_title"), NamedTextColor.AQUA)
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text(lang.getMessage("editor.button.edit_pattern_desc1"), NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text(lang.getMessage("editor.button.edit_pattern_desc2"), NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(Component.text(lang.getMessage("editor.button.edit_pattern_action"), NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, true));
        meta.lore(lore);
        button.setItemMeta(meta);
        inventory.setItem(BTN_EDIT_PATTERN, button);
    }

    private void addSaveEditButton() {
        ItemStack button = new ItemStack(Material.LIME_WOOL);
        ItemMeta meta = button.getItemMeta();
        meta.displayName(Component.text(lang.getMessage("editor.button.save_changes_title"), NamedTextColor.GREEN)
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text(lang.getMessage("editor.button.save_changes_desc1"), NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(Component.text(lang.getMessage("editor.button.save_changes_action"), NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, true));
        meta.lore(lore);
        button.setItemMeta(meta);
        inventory.setItem(45, button);
    }

    private void addCancelEditButton() {
        ItemStack button = new ItemStack(Material.RED_WOOL);
        ItemMeta meta = button.getItemMeta();
        meta.displayName(Component.text(lang.getMessage("editor.button.cancel_editing_title"), NamedTextColor.RED)
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text(lang.getMessage("editor.button.cancel_editing_desc1"), NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(Component.text(lang.getMessage("editor.button.cancel_editing_action"), NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, true));
        meta.lore(lore);
        button.setItemMeta(meta);
        inventory.setItem(46, button);
    }

    private void initializeEditGrid() {
        Arrays.fill(editGridItems, null);
        editResultItem = recipe.getResultItem().clone();
        if (recipe.getType() == RecipeType.SHAPED) {
            List<RecipeIngredient> ingredients = recipe.getRecipeData().ingredients();
            for (int i = 0; i < Math.min(ingredients.size(), 9); i++) {
                RecipeIngredient ingredient = ingredients.get(i);
                if (ingredient.material() != Material.AIR) {
                    if (ingredient.hasExactItem()) {
                        editGridItems[i] = ingredient.getExactItem().clone();
                    } else {
                        editGridItems[i] = new ItemStack(ingredient.material(), ingredient.amount());
                    }
                }
            }
        } else if (recipe.getType() == RecipeType.SHAPELESS) {
            var shapelessData = recipe.getShapelessData();
            Map<Material, Integer> ingredients = shapelessData.ingredients();
            List<ItemStack> exactIngredients = shapelessData.exactIngredients();
            int index = 0;
            int exactIndex = 0;
            for (Map.Entry<Material, Integer> entry : ingredients.entrySet()) {
                for (int i = 0; i < entry.getValue(); i++) {
                    if (index >= 9) break;
                    
                    ItemStack exactItem = null;
                    if (exactIngredients != null && exactIndex < exactIngredients.size()) {
                        exactItem = exactIngredients.get(exactIndex);
                    }
                    if (exactItem != null) {
                        editGridItems[index] = exactItem.clone();
                    } else {
                        editGridItems[index] = new ItemStack(entry.getKey(), 1);
                    }
                    index++;
                    exactIndex++;
                }
            }
        } else if (isFurnaceRecipe()) {
            FurnaceRecipeData furnaceData = recipe.getFurnaceData();
            if (furnaceData != null) {
                
                RecipeIngredient input = furnaceData.getInput();
                if (input != null && input.material() != Material.AIR) {
                    if (furnaceData.hasExactInput()) {
                        editGridItems[0] = furnaceData.getExactInput().clone();
                    } else {
                        editGridItems[0] = new ItemStack(input.material(), 1);
                    }
                }
                
                if (furnaceData.hasCustomFuels()) {
                    FurnaceRecipeData.CustomFuel fuel = furnaceData.getCustomFuels().get(0);
                    if (fuel.hasExactItem()) {
                        editGridItems[1] = fuel.getExactItem().clone();
                    } else {
                        editGridItems[1] = new ItemStack(fuel.getIngredient().material(), 1);
                    }
                }
            }
        }
    }

    private boolean isFurnaceRecipe() {
        return recipe.getType() == RecipeType.FURNACE ||
               recipe.getType() == RecipeType.BLAST_FURNACE ||
               recipe.getType() == RecipeType.SMOKER;
    }

    private boolean shouldSaveExactItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        
        if (item.getType() == Material.ENCHANTED_BOOK &&
            meta instanceof org.bukkit.inventory.meta.EnchantmentStorageMeta bookMeta) {
            if (bookMeta.hasStoredEnchants()) {
                return true;
            }
        }
        
        if (meta.hasEnchants()) {
            return true;
        }
        
        if (!meta.getPersistentDataContainer().getKeys().isEmpty()) {
            return true;
        }
        
        if (meta instanceof org.bukkit.inventory.meta.Damageable damageable) {
            if (damageable.hasDamage() && damageable.getDamage() > 0) {
                return true;
            }
        }
        
        if (meta.hasDisplayName()) {
            return true;
        }
        
        if (meta.hasLore()) {
            return true;
        }
        
        if (meta.hasCustomModelData()) {
            return true;
        }
        
        if (!meta.getItemFlags().isEmpty()) {
            return true;
        }
        
        if (meta.isUnbreakable()) {
            return true;
        }
        return false;
    }

    private void saveEditedRecipe() {
        if (editResultItem == null || editResultItem.getType() == Material.AIR) {
            MessageUtil.sendError(player, lang.getMessage("editor.error.no_result"));
            return;
        }

        try {
            Map<String, Object> recipeData = new HashMap<>();
            recipeData.put("name", recipe.getName());
            recipeData.put("description", recipe.getDescription());
            recipeData.put("type", recipe.getType().name());
            recipeData.put("hidden", recipe.isHidden());
            if (recipe.getType() == RecipeType.SHAPED) {
                List<String> pattern = new ArrayList<>();
                List<ItemStack> exactItems = new ArrayList<>();
                for (int row = 0; row < 3; row++) {
                    StringBuilder rowBuilder = new StringBuilder();
                    for (int col = 0; col < 3; col++) {
                        int index = row * 3 + col;
                        ItemStack item = editGridItems[index];
                        if (item == null || item.getType() == Material.AIR) {
                            rowBuilder.append("AIR ");
                            exactItems.add(null);
                        } else {
                            rowBuilder.append(item.getType().name());
                            if (item.getAmount() > 1) {
                                rowBuilder.append(":").append(item.getAmount());
                            }
                            rowBuilder.append(" ");
                            boolean shouldSaveExact = false;
                            if (item.hasItemMeta()) {
                                ItemMeta meta = item.getItemMeta();
                                if (item.getType() == Material.ENCHANTED_BOOK) {
                                    if (meta instanceof org.bukkit.inventory.meta.EnchantmentStorageMeta) {
                                        org.bukkit.inventory.meta.EnchantmentStorageMeta bookMeta =
                                                (org.bukkit.inventory.meta.EnchantmentStorageMeta) meta;
                                        if (bookMeta.hasStoredEnchants()) {
                                            shouldSaveExact = true;
                                        }
                                    }
                                }
                                if (meta.hasEnchants()) {
                                    shouldSaveExact = true;
                                }
                                if (!meta.getPersistentDataContainer().getKeys().isEmpty()) {
                                    shouldSaveExact = true;
                                }
                                if (meta instanceof org.bukkit.inventory.meta.Damageable) {
                                    org.bukkit.inventory.meta.Damageable damageable = (org.bukkit.inventory.meta.Damageable) meta;
                                    if (damageable.hasDamage() && damageable.getDamage() > 0) {
                                        shouldSaveExact = true;
                                    }
                                }
                            }
                            if (shouldSaveExact) {
                                exactItems.add(item.clone());
                            } else {
                                exactItems.add(null);
                            }
                        }
                    }
                    pattern.add(rowBuilder.toString().trim());
                }
                recipeData.put("recipe", pattern);
                List<String> exactItemsData = new ArrayList<>();
                for (ItemStack exactItem : exactItems) {
                    if (exactItem != null) {
                        exactItemsData.add(ItemStackSerializer.toBase64(exactItem));
                    } else {
                        exactItemsData.add(null);
                    }
                }
                if (exactItemsData.stream().anyMatch(Objects::nonNull)) {
                    recipeData.put("exact-ingredients", exactItemsData);
                }
            } else if (recipe.getType() == RecipeType.SHAPELESS) {
                List<String> ingredients = new ArrayList<>();
                Map<Material, Integer> counts = new LinkedHashMap<>();
                List<String> exactItemsData = new ArrayList<>();

                for (ItemStack item : editGridItems) {
                    if (item != null && item.getType() != Material.AIR) {
                        counts.merge(item.getType(), 1, Integer::sum);

                        if (shouldSaveExactItem(item)) {
                            exactItemsData.add(ItemStackSerializer.toBase64(item));
                        } else {
                            exactItemsData.add(null);
                        }
                    }
                }
                for (Map.Entry<Material, Integer> entry : counts.entrySet()) {
                    ingredients.add(entry.getKey().name() + ":" + entry.getValue());
                }
                recipeData.put("ingredients", ingredients);

                if (exactItemsData.stream().anyMatch(Objects::nonNull)) {
                    recipeData.put("exact-ingredients", exactItemsData);
                }
            } else if (isFurnaceRecipe()) {
                
                ItemStack inputItem = editGridItems[0]; 
                ItemStack fuelItem = editGridItems[1];  

                if (inputItem != null && inputItem.getType() != Material.AIR) {
                    recipeData.put("input", inputItem.getType().name());
                    
                    if (shouldSaveExactItem(inputItem)) {
                        recipeData.put("input-full", ItemStackSerializer.toMap(inputItem));
                    }
                } else {
                    MessageUtil.sendError(player, lang.getMessage("editor.error.no_input"));
                    return;
                }

                if (fuelItem != null && fuelItem.getType() != Material.AIR) {
                    Map<String, Object> fuelData = new HashMap<>();
                    fuelData.put("material", fuelItem.getType().name());
                    
                    FurnaceRecipeData existingData = recipe.getFurnaceData();
                    int burnTime = 200; 
                    if (existingData != null && existingData.hasCustomFuels()) {
                        burnTime = existingData.getCustomFuels().get(0).getBurnTime();
                    }
                    fuelData.put("burn-time", burnTime);
                    
                    if (shouldSaveExactItem(fuelItem)) {
                        fuelData.put("item-full", ItemStackSerializer.toMap(fuelItem));
                    }
                    recipeData.put("custom-fuel", List.of(fuelData));
                }

                FurnaceRecipeData existingData = recipe.getFurnaceData();
                if (existingData != null) {
                    recipeData.put("cooking-time", existingData.getCookingTime());
                    recipeData.put("experience", existingData.getExperience());
                }
            }

            Map<String, Object> resultData = ItemStackSerializer.toMap(editResultItem);
            recipeData.put("result", resultData);
            recipeData.put("material", editResultItem.getType().name());
            recipeData.put("amount", editResultItem.getAmount());
            if (editResultItem.hasItemMeta()) {
                ItemMeta resultMeta = editResultItem.getItemMeta();
                if (resultMeta.hasDisplayName() || resultMeta.hasLore() || resultMeta.hasEnchants() ||
                        !resultMeta.getPersistentDataContainer().getKeys().isEmpty()) {
                    recipeData.put("result-full", ItemStackSerializer.toMap(editResultItem));
                }
                
                NamespacedKey presetKey = new NamespacedKey(plugin, "craft_event_preset");
                String presetName = resultMeta.getPersistentDataContainer().get(presetKey, PersistentDataType.STRING);
                if (presetName != null && !presetName.isEmpty()) {
                    recipeData.put("craft_events_preset", presetName);
                }
            }

            org.hikarii.customrecipes.recipe.RecipeConditions conditions = recipe.getConditions();
            if (conditions != null && conditions.hasAnyCondition()) {
                Map<String, Object> conditionsData = new HashMap<>();
                if (conditions.hasPermission()) {
                    conditionsData.put("permission", conditions.getPermission());
                }
                if (conditions.hasXpLevelRequirement()) {
                    conditionsData.put("xp-level", conditions.getRequiredXpLevel());
                }
                if (conditions.hasXpReward()) {
                    conditionsData.put("xp-reward", conditions.getXpReward());
                }
                if (conditions.hasCooldown()) {
                    conditionsData.put("cooldown", conditions.getCooldownSeconds());
                }
                if (conditions.hasDailyLimit()) {
                    conditionsData.put("limit-daily", conditions.getCraftLimitDaily());
                }
                if (conditions.hasWeeklyLimit()) {
                    conditionsData.put("limit-weekly", conditions.getCraftLimitWeekly());
                }
                if (conditions.hasTotalLimit()) {
                    conditionsData.put("limit-total", conditions.getCraftLimitTotal());
                }
                if (conditions.hasMoneyCost()) {
                    conditionsData.put("money-cost", conditions.getMoneyCost());
                }
                recipeData.put("conditions", conditionsData);
            }

            if (randomResults != null && randomResults.hasRandomResults()) {
                Map<String, Object> randomResultsData = new HashMap<>();
                randomResultsData.put("show-chances", randomResults.isShowChances());

                List<Map<String, Object>> items = new ArrayList<>();
                for (org.hikarii.customrecipes.recipe.data.RandomResult result : randomResults.getResults()) {
                    Map<String, Object> itemData = ItemStackSerializer.toMap(result.getItem());
                    itemData.put("chance", result.getWeight());
                    if (result.hasCustomName()) {
                        itemData.put("name", result.getName());
                    }
                    items.add(itemData);
                }
                randomResultsData.put("items", items);
                recipeData.put("random-results", randomResultsData);
            }

            if (!recipeData.containsKey("craft_events_preset")) {
                
                try {
                    File recipeFile = plugin.getConfigManager().getRecipeFileManager().findRecipeFile(recipe.getKey());
                    if (recipeFile != null && recipeFile.exists()) {
                        YamlConfiguration config = YamlConfiguration.loadConfiguration(recipeFile);
                        if (config.contains("craft_events_preset")) {
                            recipeData.put("craft_events_preset", config.getString("craft_events_preset"));
                        }
                    }
                } catch (Exception ignored) {}
            }

            plugin.getConfigManager().getRecipeFileManager().saveRecipe(recipe.getKey(), recipeData, recipe.getType());
            plugin.loadConfiguration();
            MessageUtil.sendAdminSuccess(player, lang.getMessage("editor.success.pattern_updated"));
            editMode = false;
            editResultItem = null;
            CustomRecipe updatedRecipe = plugin.getRecipeManager().getRecipe(recipe.getKey());
            if (updatedRecipe != null) {
                new RecipeEditorGUI(plugin, player, updatedRecipe).open();
            }
        } catch (Exception e) {
            MessageUtil.sendError(player, lang.getMessage("editor.error.save_failed", Map.of("error", e.getMessage())));
            plugin.getLogger().severe("Failed to save edited recipe: " + e.getMessage());
            if (plugin.isDebugMode()) {
                e.printStackTrace();
            }
        }
    }

    private void fillBorders() {
        ItemStack borderPane = new ItemStack(Material.LIGHT_BLUE_STAINED_GLASS_PANE);
        ItemMeta meta = borderPane.getItemMeta();
        meta.displayName(Component.empty());
        borderPane.setItemMeta(meta);
        for (int i = 0; i < 54; i++) {
            inventory.setItem(i, borderPane);
        }

        if (recipe.getType() == RecipeType.FURNACE ||
            recipe.getType() == RecipeType.BLAST_FURNACE ||
            recipe.getType() == RecipeType.SMOKER) {
            
            inventory.setItem(12, null); 
            inventory.setItem(30, null); 
        } else {
            
            int[] gridSlots = {10, 11, 12, 19, 20, 21, 28, 29, 30};
            for (int slot : gridSlots) {
                inventory.setItem(slot, null);
            }
        }
        inventory.setItem(25, null); 
    }

    private void addEqualsSign() {
        ItemStack equals = new ItemStack(Material.ORANGE_STAINED_GLASS_PANE);
        ItemMeta meta = equals.getItemMeta();
        meta.displayName(Component.text("=", NamedTextColor.GOLD)
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
                Component.empty(),
                Component.text(lang.getMessage("creator.equals.line1"), NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false),
                Component.text(lang.getMessage("creator.equals.line2"), NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false),
                Component.empty(),
                Component.text(lang.getMessage("creator.equals.line3"), NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false),
                Component.text(lang.getMessage("creator.equals.line4"), NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false)
        ));
        equals.setItemMeta(meta);
        inventory.setItem(23, equals);
    }

    private void addCraftingGrid() {
        if (recipe.getType() == RecipeType.SHAPED) {
            addShapedCraftingGrid();
        } else if (recipe.getType() == RecipeType.SHAPELESS) {
            addShapelessCraftingGrid();
        } else if (recipe.getType() == RecipeType.FURNACE ||
                   recipe.getType() == RecipeType.BLAST_FURNACE ||
                   recipe.getType() == RecipeType.SMOKER) {
            addFurnaceGrid();
        }
    }

    private void addFurnaceGrid() {
        FurnaceRecipeData furnaceData = recipe.getFurnaceData();
        if (furnaceData == null) return;

        int inputSlot = 12;
        RecipeIngredient input = furnaceData.getInput();
        if (input != null && input.material() != Material.AIR) {
            ItemStack inputItem;
            if (furnaceData.hasExactInput()) {
                inputItem = furnaceData.getExactInput().clone();
            } else {
                inputItem = new ItemStack(input.material(), 1);
            }
            ItemMeta meta = inputItem.getItemMeta();
            if (meta != null) {
                List<Component> lore = meta.hasLore() && meta.lore() != null ?
                        new ArrayList<>(meta.lore()) : new ArrayList<>();
                lore.add(Component.empty());
                lore.add(Component.text(lang.getMessage("furnace_creator.input_hint"), NamedTextColor.YELLOW)
                        .decoration(TextDecoration.ITALIC, false));
                meta.lore(lore);
                inputItem.setItemMeta(meta);
            }
            inventory.setItem(inputSlot, inputItem);
        }

        ItemStack plus = new ItemStack(Material.ORANGE_STAINED_GLASS_PANE);
        ItemMeta plusMeta = plus.getItemMeta();
        plusMeta.displayName(Component.text("+", NamedTextColor.GOLD)
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false));
        plus.setItemMeta(plusMeta);
        inventory.setItem(21, plus);

        int fuelSlot = 30;
        if (furnaceData.hasCustomFuels()) {
            
            FurnaceRecipeData.CustomFuel fuel = furnaceData.getCustomFuels().get(0);
            ItemStack fuelItem;
            if (fuel.hasExactItem()) {
                fuelItem = fuel.getExactItem().clone();
            } else {
                fuelItem = new ItemStack(fuel.getIngredient().material(), 1);
            }
            ItemMeta fuelMeta = fuelItem.getItemMeta();
            if (fuelMeta != null) {
                List<Component> lore = fuelMeta.hasLore() && fuelMeta.lore() != null ?
                        new ArrayList<>(fuelMeta.lore()) : new ArrayList<>();
                lore.add(Component.empty());
                lore.add(Component.text(lang.getMessage("furnace_creator.fuel_hint"), NamedTextColor.YELLOW)
                        .decoration(TextDecoration.ITALIC, false));
                lore.add(Component.text(lang.getMessage("furnace_creator.burn_time",
                        Map.of("ticks", String.valueOf(fuel.getBurnTime()))), NamedTextColor.GOLD)
                        .decoration(TextDecoration.ITALIC, false));
                fuelMeta.lore(lore);
                fuelItem.setItemMeta(fuelMeta);
            }
            inventory.setItem(fuelSlot, fuelItem);
        }
    }

    private void addShapedCraftingGrid() {
        List<RecipeIngredient> ingredients = recipe.getRecipeData().ingredients();
        int[] gridSlots = {10, 11, 12, 19, 20, 21, 28, 29, 30};
        for (int i = 0; i < ingredients.size() && i < gridSlots.length; i++) {
            RecipeIngredient ingredient = ingredients.get(i);
            if (ingredient.material() == Material.AIR) {
                continue;
            }

            ItemStack item;
            if (ingredient.hasExactItem()) {
                item = ingredient.getExactItem().clone();
            } else {
                item = new ItemStack(ingredient.material(), ingredient.amount());
            }

            ItemMeta meta = item.getItemMeta();
            if (meta != null && meta instanceof org.bukkit.inventory.meta.Damageable) {
                org.bukkit.inventory.meta.Damageable damageable = (org.bukkit.inventory.meta.Damageable) meta;
                if (damageable.hasDamage()) {
                    int maxDurability = item.getType().getMaxDurability();
                    int currentDurability = maxDurability - damageable.getDamage();
                    List<Component> lore = meta.hasLore() && meta.lore() != null ?
                            new ArrayList<>(meta.lore()) : new ArrayList<>();
                    lore.add(Component.empty());
                    lore.add(Component.text("Durability: " + currentDurability + "/" + maxDurability,
                                    NamedTextColor.AQUA)
                            .decoration(TextDecoration.ITALIC, false));
                    meta.lore(lore);
                    item.setItemMeta(meta);
                }
            }
            inventory.setItem(gridSlots[i], item);
        }
    }

    private void addShapelessCraftingGrid() {
        var shapelessData = recipe.getShapelessData();
        Map<Material, Integer> ingredients = shapelessData.ingredients();
        List<ItemStack> exactIngredients = shapelessData.exactIngredients();
        int[] gridSlots = {10, 11, 12, 19, 20, 21, 28, 29, 30};
        int slotIndex = 0;
        int exactIndex = 0;

        for (Map.Entry<Material, Integer> entry : ingredients.entrySet()) {
            Material mat = entry.getKey();
            int count = entry.getValue();
            for (int i = 0; i < count && slotIndex < gridSlots.length; i++) {
                ItemStack item;

                ItemStack exactItem = null;
                if (exactIngredients != null && exactIndex < exactIngredients.size()) {
                    exactItem = exactIngredients.get(exactIndex);
                }

                if (exactItem != null) {
                    
                    item = exactItem.clone();
                    ItemMeta meta = item.getItemMeta();
                    if (meta != null) {
                        
                        if (meta instanceof org.bukkit.inventory.meta.Damageable damageable) {
                            if (damageable.hasDamage()) {
                                int maxDurability = item.getType().getMaxDurability();
                                int currentDurability = maxDurability - damageable.getDamage();
                                List<Component> lore = meta.hasLore() && meta.lore() != null ?
                                        new ArrayList<>(meta.lore()) : new ArrayList<>();
                                lore.add(Component.empty());
                                lore.add(Component.text("Durability: " + currentDurability + "/" + maxDurability,
                                                NamedTextColor.AQUA)
                                        .decoration(TextDecoration.ITALIC, false));
                                meta.lore(lore);
                                item.setItemMeta(meta);
                            }
                        }
                    }
                } else {
                    
                    item = new ItemStack(mat);
                    ItemMeta meta = item.getItemMeta();
                    if (meta != null) {
                        List<Component> lore = new ArrayList<>();
                        lore.add(Component.text(lang.getMessage("editor.shapeless.ingredient"), NamedTextColor.GRAY)
                                .decoration(TextDecoration.ITALIC, false));
                        lore.add(Component.text(lang.getMessage("editor.shapeless.position_info"), NamedTextColor.DARK_GRAY)
                                .decoration(TextDecoration.ITALIC, false));
                        meta.lore(lore);
                        item.setItemMeta(meta);
                    }
                }

                inventory.setItem(gridSlots[slotIndex], item);
                slotIndex++;
                exactIndex++;
            }
        }
    }

    private void addResultItem() {
        ItemStack result = recipe.getResultItem().clone();
        inventory.setItem(25, result);
    }

    private void addInfoBook() {
        ItemStack info = new ItemStack(Material.BOOK);
        ItemMeta meta = info.getItemMeta();
        meta.displayName(Component.text(lang.getMessage("editor.info.title"), NamedTextColor.GOLD)
                .decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text(lang.getMessage("editor.info.key"), NamedTextColor.GRAY)
                .append(Component.text(recipe.getKey(), NamedTextColor.WHITE))
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text(lang.getMessage("editor.info.type"), NamedTextColor.GRAY)
                .append(Component.text(lang.getRecipeTypeName(recipe.getType().name()), NamedTextColor.WHITE))
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text(lang.getMessage("editor.info.result"), NamedTextColor.GRAY)
                .append(Component.text(
                        lang.getMaterialName(recipe.getResultMaterial().name()),
                        NamedTextColor.WHITE))
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text(lang.getMessage("editor.info.amount"), NamedTextColor.GRAY)
                .append(Component.text(recipe.getResultAmount() + "x", NamedTextColor.WHITE))
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());

        if (recipe.getName() != null && !recipe.getName().isEmpty()) {
            lore.add(Component.text(lang.getMessage("editor.info.name"), NamedTextColor.YELLOW)
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(MessageUtil.colorize("  " + recipe.getName())
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.empty());
        }

        if (recipe.getDescription() != null && !recipe.getDescription().isEmpty()) {
            lore.add(Component.text(lang.getMessage("editor.info.description"), NamedTextColor.AQUA)
                    .decoration(TextDecoration.ITALIC, false));
            for (String line : recipe.getDescription()) {
                lore.add(MessageUtil.colorize("  " + line)
                        .decoration(TextDecoration.ITALIC, false));
            }
            lore.add(Component.empty());
        }

        ItemMeta resultMeta = recipe.getResultItem().getItemMeta();
        if (resultMeta != null && resultMeta.hasCustomModelData()) {
            lore.add(Component.text(lang.getMessage("editor.info.custom_model_data"), NamedTextColor.LIGHT_PURPLE)
                    .append(Component.text(resultMeta.getCustomModelData(), NamedTextColor.WHITE))
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.empty());
        }

        if (resultMeta != null) {
            PersistentDataContainer container = resultMeta.getPersistentDataContainer();
            if (!container.getKeys().isEmpty()) {
                lore.add(Component.text(lang.getMessage("editor.info.nbt_data"), NamedTextColor.DARK_AQUA)
                        .decoration(TextDecoration.ITALIC, false));
                for (NamespacedKey key : container.getKeys()) {
                    if (key.getNamespace().equals(plugin.getName().toLowerCase())) {
                        
                        String value = null;
                        try {
                            value = container.get(key, PersistentDataType.STRING);
                        } catch (IllegalArgumentException e) {
                            try {
                                Byte byteVal = container.get(key, PersistentDataType.BYTE);
                                value = byteVal != null ? byteVal.toString() : null;
                            } catch (IllegalArgumentException e2) {
                                try {
                                    Integer intVal = container.get(key, PersistentDataType.INTEGER);
                                    value = intVal != null ? intVal.toString() : null;
                                } catch (IllegalArgumentException e3) {
                                    value = "<complex>";
                                }
                            }
                        }
                        if (value != null) {
                            lore.add(Component.text("  • " + key.getKey() + ": " + value, NamedTextColor.AQUA)
                                    .decoration(TextDecoration.ITALIC, false));
                        }
                    }
                }
                lore.add(Component.empty());
            }
        }
        if (resultMeta != null && resultMeta.hasEnchants()) {
            lore.add(Component.text(lang.getMessage("editor.info.enchantments"), NamedTextColor.LIGHT_PURPLE)
                    .decoration(TextDecoration.ITALIC, false));
            for (Map.Entry<Enchantment, Integer> entry : resultMeta.getEnchants().entrySet()) {
                String enchName = entry.getKey().getKey().getKey();
                lore.add(Component.text("  • " + enchName + " " + entry.getValue(), NamedTextColor.AQUA)
                        .decoration(TextDecoration.ITALIC, false));
            }
            lore.add(Component.empty());
        }
        List<String> disabledWorlds = plugin.getRecipeWorldManager().getDisabledWorlds(recipe.getKey());
        if (!disabledWorlds.isEmpty()) {
            lore.add(Component.text(lang.getMessage("editor.info.world_restrictions"), NamedTextColor.RED)
                    .decoration(TextDecoration.ITALIC, false));
            for (String world : disabledWorlds) {
                lore.add(Component.text("  • " + world, NamedTextColor.YELLOW)
                        .decoration(TextDecoration.ITALIC, false));
            }
        }
        meta.lore(lore);
        info.setItemMeta(meta);
        inventory.setItem(44, info);
    }

    private void addToggleButton() {
        boolean enabled = plugin.getRecipeManager().isRecipeEnabled(recipe.getKey());
        Material material = enabled ? Material.LIME_DYE : Material.GRAY_DYE;
        ItemStack toggleButton = new ItemStack(material);
        ItemMeta meta = toggleButton.getItemMeta();
        meta.displayName(Component.text(
                lang.getMessage(enabled ? "editor.button.toggle_enabled_title_on" : "editor.button.toggle_enabled_title_off"),
                enabled ? NamedTextColor.GREEN : NamedTextColor.RED)
                .decoration(TextDecoration.BOLD, true).decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text(lang.getMessage("editor.button.toggle_enabled_status"), NamedTextColor.GRAY)
                .append(Component.text(
                        lang.getMessage(enabled ? "editor.button.toggle_enabled_active" : "editor.button.toggle_enabled_inactive"),
                        enabled ? NamedTextColor.GREEN : NamedTextColor.RED))
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(Component.text(
                lang.getMessage(enabled ? "editor.button.toggle_enabled_desc_on" : "editor.button.toggle_enabled_desc_off"),
                NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(Component.text(lang.getMessage(enabled ? "editor.button.toggle_enabled_action_on" : "editor.button.toggle_enabled_action_off"), NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, true));
        meta.lore(lore);
        toggleButton.setItemMeta(meta);
        inventory.setItem(isFurnaceRecipe() ? BTN_FURNACE_TOGGLE : BTN_TOGGLE, toggleButton);
    }

    private void addWorldSettingsButton() {
        ItemStack button = new ItemStack(Material.FILLED_MAP);
        ItemMeta meta = button.getItemMeta();
        meta.displayName(Component.text(lang.getMessage("editor.button.world_settings_title"), NamedTextColor.AQUA)
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        List<String> disabledWorlds = plugin.getRecipeWorldManager().getDisabledWorlds(recipe.getKey());
        if (disabledWorlds.isEmpty()) {
            lore.add(Component.text(lang.getMessage("editor.button.world_settings_all_enabled"), NamedTextColor.GREEN)
                    .decoration(TextDecoration.ITALIC, false));
        } else {
            lore.add(Component.text(lang.getMessage("editor.button.world_settings_disabled_in"), NamedTextColor.RED)
                    .decoration(TextDecoration.ITALIC, false));
            for (String world : disabledWorlds) {
                lore.add(Component.text("  • " + world, NamedTextColor.YELLOW)
                        .decoration(TextDecoration.ITALIC, false));
            }
        }
        lore.add(Component.empty());
        lore.add(Component.text(lang.getMessage("editor.button.world_settings_desc1"), NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text(lang.getMessage("editor.button.world_settings_desc2"), NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(Component.text(lang.getMessage("editor.button.world_settings_action"), NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, true));
        meta.lore(lore);
        button.setItemMeta(meta);
        inventory.setItem(isFurnaceRecipe() ? BTN_FURNACE_WORLD_SETTINGS : BTN_WORLD_SETTINGS, button);
    }

    private void addEditItemButton() {
        ItemStack button = new ItemStack(Material.ANVIL);
        ItemMeta meta = button.getItemMeta();
        meta.displayName(Component.text(lang.getMessage("editor.button.edit_result_title"), NamedTextColor.LIGHT_PURPLE)
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text(lang.getMessage("editor.button.edit_result_desc1"), NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text(lang.getMessage("editor.button.edit_result_desc2"), NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text(lang.getMessage("editor.button.edit_result_desc3"), NamedTextColor.AQUA)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text(lang.getMessage("editor.button.edit_result_desc4"), NamedTextColor.AQUA)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text(lang.getMessage("editor.button.edit_result_desc5"), NamedTextColor.AQUA)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text(lang.getMessage("editor.button.edit_result_desc6"), NamedTextColor.AQUA)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(Component.text(lang.getMessage("editor.button.edit_result_action"), NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, true));
        meta.lore(lore);
        button.setItemMeta(meta);
        inventory.setItem(BTN_EDIT_RESULT, button);
    }

    private void addConditionsButton() {
        ItemStack button = new ItemStack(Material.COMPARATOR);
        ItemMeta meta = button.getItemMeta();
        meta.displayName(Component.text(lang.getMessage("editor.button.conditions_title"), NamedTextColor.GOLD)
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text(lang.getMessage("editor.button.conditions_desc1"), NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text(lang.getMessage("editor.button.conditions_desc2"), NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());

        org.hikarii.customrecipes.recipe.RecipeConditions conditions = recipe.getConditions();
        lore.add(Component.text(lang.getMessage("editor.button.conditions_current"), NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));

        if (conditions != null && conditions.hasAnyCondition()) {
            if (conditions.hasPermission()) {
                lore.add(Component.text(lang.getMessage("editor.button.conditions_permission",
                        java.util.Map.of("value", conditions.getPermission())), NamedTextColor.AQUA)
                        .decoration(TextDecoration.ITALIC, false));
            }
            if (conditions.hasXpLevelRequirement()) {
                lore.add(Component.text(lang.getMessage("editor.button.conditions_xp_level",
                        java.util.Map.of("value", String.valueOf(conditions.getRequiredXpLevel()))), NamedTextColor.AQUA)
                        .decoration(TextDecoration.ITALIC, false));
            }
            if (conditions.hasXpReward()) {
                String reward = (conditions.getXpReward() > 0 ? "+" : "") + conditions.getXpReward();
                lore.add(Component.text(lang.getMessage("editor.button.conditions_xp_reward",
                        java.util.Map.of("value", reward)), NamedTextColor.AQUA)
                        .decoration(TextDecoration.ITALIC, false));
            }
            if (conditions.hasCooldown()) {
                int seconds = conditions.getCooldownSeconds();
                String timeStr = org.hikarii.customrecipes.recipe.CraftTracker.formatTime(seconds);
                lore.add(Component.text(lang.getMessage("editor.button.conditions_cooldown",
                        java.util.Map.of("value", timeStr)), NamedTextColor.AQUA)
                        .decoration(TextDecoration.ITALIC, false));
            }
            if (conditions.hasDailyLimit()) {
                lore.add(Component.text(lang.getMessage("editor.button.conditions_daily_limit",
                        java.util.Map.of("value", String.valueOf(conditions.getCraftLimitDaily()))), NamedTextColor.AQUA)
                        .decoration(TextDecoration.ITALIC, false));
            }
            if (conditions.hasWeeklyLimit()) {
                lore.add(Component.text(lang.getMessage("editor.button.conditions_weekly_limit",
                        java.util.Map.of("value", String.valueOf(conditions.getCraftLimitWeekly()))), NamedTextColor.AQUA)
                        .decoration(TextDecoration.ITALIC, false));
            }
            if (conditions.hasTotalLimit()) {
                lore.add(Component.text(lang.getMessage("editor.button.conditions_total_limit",
                        java.util.Map.of("value", String.valueOf(conditions.getCraftLimitTotal()))), NamedTextColor.AQUA)
                        .decoration(TextDecoration.ITALIC, false));
            }
            if (conditions.hasMoneyCost()) {
                lore.add(Component.text(lang.getMessage("editor.button.conditions_money",
                        java.util.Map.of("value", String.valueOf(conditions.getMoneyCost()))), NamedTextColor.AQUA)
                        .decoration(TextDecoration.ITALIC, false));
            }
        } else {
            lore.add(Component.text(lang.getMessage("editor.button.conditions_none"), NamedTextColor.DARK_GRAY)
                    .decoration(TextDecoration.ITALIC, false));
        }

        lore.add(Component.empty());
        lore.add(Component.text(lang.getMessage("editor.button.conditions_action"), NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, true));
        meta.lore(lore);
        button.setItemMeta(meta);
        inventory.setItem(isFurnaceRecipe() ? BTN_FURNACE_CONDITIONS : BTN_CONDITIONS, button);
    }

    private void addSmeltingConditionsButton() {
        ItemStack button = new ItemStack(Material.BLAZE_POWDER);
        ItemMeta meta = button.getItemMeta();
        meta.displayName(Component.text(lang.getMessage("editor.button.smelting_conditions_title"), NamedTextColor.GOLD)
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text(lang.getMessage("editor.button.smelting_conditions_desc1"), NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text(lang.getMessage("editor.button.smelting_conditions_desc2"), NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());

        FurnaceRecipeData furnaceData = recipe.getFurnaceData();
        lore.add(Component.text(lang.getMessage("editor.button.smelting_conditions_current"), NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));

        if (furnaceData != null) {
            lore.add(Component.text(lang.getMessage("editor.button.smelting_conditions_cook_time",
                    java.util.Map.of("value", String.valueOf(furnaceData.getCookingTime()))), NamedTextColor.AQUA)
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text(lang.getMessage("editor.button.smelting_conditions_experience",
                    java.util.Map.of("value", String.format("%.1f", furnaceData.getExperience()))), NamedTextColor.AQUA)
                    .decoration(TextDecoration.ITALIC, false));
            if (furnaceData.hasCustomFuels()) {
                
                int burnTime = furnaceData.getCustomFuels().get(0).getBurnTime();
                lore.add(Component.text(lang.getMessage("editor.button.smelting_conditions_burn_time",
                        java.util.Map.of("value", String.valueOf(burnTime))), NamedTextColor.AQUA)
                        .decoration(TextDecoration.ITALIC, false));
            }
        } else {
            lore.add(Component.text(lang.getMessage("editor.button.smelting_conditions_default"), NamedTextColor.DARK_GRAY)
                    .decoration(TextDecoration.ITALIC, false));
        }

        lore.add(Component.empty());
        lore.add(Component.text(lang.getMessage("editor.button.smelting_conditions_action"), NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, true));
        meta.lore(lore);
        button.setItemMeta(meta);
        inventory.setItem(BTN_SMELTING_CONDITIONS, button);
    }

    private void addDeleteButton() {
        ItemStack deleteButton;
        ItemMeta meta;
        if (deleteConfirmation) {
            deleteButton = new ItemStack(Material.LIME_WOOL);
            meta = deleteButton.getItemMeta();
            meta.displayName(Component.text(lang.getMessage("editor.button.delete_confirm_title"), NamedTextColor.RED)
                    .decoration(TextDecoration.BOLD, true)
                    .decoration(TextDecoration.ITALIC, false));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.empty());
            lore.add(Component.text(lang.getMessage("editor.button.delete_confirm_warning"), NamedTextColor.GOLD)
                    .decoration(TextDecoration.BOLD, true)
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.empty());
            lore.add(Component.text(lang.getMessage("editor.button.delete_confirm_desc1"), NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text(lang.getMessage("editor.button.delete_confirm_desc2"), NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text(lang.getMessage("editor.button.delete_confirm_desc3"), NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text(lang.getMessage("editor.button.delete_confirm_desc4"), NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.empty());
            lore.add(Component.text(lang.getMessage("editor.button.delete_confirm_action"), NamedTextColor.RED)
                    .decoration(TextDecoration.BOLD, true)
                    .decoration(TextDecoration.ITALIC, true));
            lore.add(Component.text(lang.getMessage("editor.button.delete_confirm_cancel"), NamedTextColor.YELLOW)
                    .decoration(TextDecoration.ITALIC, true));
            meta.lore(lore);
            deleteButton.setItemMeta(meta);
        } else {
            deleteButton = new ItemStack(Material.BARRIER);
            meta = deleteButton.getItemMeta();
            meta.displayName(Component.text(lang.getMessage("editor.button.delete_title"), NamedTextColor.RED)
                    .decoration(TextDecoration.BOLD, true)
                    .decoration(TextDecoration.ITALIC, false));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.empty());
            lore.add(Component.text("⚠ " + lang.getMessage("general.warning"), NamedTextColor.GOLD)
                    .decoration(TextDecoration.BOLD, true)
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text(lang.getMessage("editor.button.delete_desc1"), NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text(lang.getMessage("editor.button.delete_desc2"), NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.empty());
            lore.add(Component.text(lang.getMessage("editor.button.delete_action"), NamedTextColor.YELLOW)
                    .decoration(TextDecoration.ITALIC, true));
            meta.lore(lore);
            deleteButton.setItemMeta(meta);
        }
        inventory.setItem(BTN_DELETE, deleteButton);
    }

    private void addHiddenToggleButton() {
        boolean hidden = recipe.isHidden();
        Material material = hidden ? Material.ENDER_EYE : Material.ENDER_PEARL;
        ItemStack button = new ItemStack(material);
        ItemMeta meta = button.getItemMeta();
        meta.displayName(Component.text(lang.getMessage(hidden ? "editor.button.toggle_hidden_title_on" : "editor.button.toggle_hidden_title_off"),
                        hidden ? NamedTextColor.DARK_PURPLE : NamedTextColor.AQUA)
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text(lang.getMessage("editor.button.toggle_hidden_status"), NamedTextColor.GRAY)
                .append(Component.text(lang.getMessage(hidden ? "editor.button.toggle_hidden_hidden" : "editor.button.toggle_hidden_visible"),
                        hidden ? NamedTextColor.DARK_PURPLE : NamedTextColor.GREEN))
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(Component.text(lang.getMessage("editor.button.toggle_hidden_desc1"), NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text(lang.getMessage("editor.button.toggle_hidden_desc2"), NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(Component.text(lang.getMessage("editor.button.toggle_hidden_action"), NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, true));
        meta.lore(lore);
        button.setItemMeta(meta);
        inventory.setItem(isFurnaceRecipe() ? BTN_FURNACE_VISIBILITY : BTN_VISIBILITY, button);
    }

    private void addBackButton() {
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta meta = back.getItemMeta();
        meta.displayName(Component.text(lang.getMessage("editor.button.back_title"), NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));
        back.setItemMeta(meta);
        inventory.setItem(BTN_BACK, back);
    }

    private void addRandomResultsButton() {
        ItemStack button = new ItemStack(Material.HOPPER);
        ItemMeta meta = button.getItemMeta();
        meta.displayName(Component.text(lang.getMessage("random_results.title"), NamedTextColor.LIGHT_PURPLE)
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text(lang.getMessage("random_results.button_desc1"), NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text(lang.getMessage("random_results.button_desc2"), NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());

        if (randomResults != null && randomResults.hasRandomResults()) {
            lore.add(Component.text(lang.getMessage("info.status") + ":", NamedTextColor.YELLOW)
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text(lang.getMessage("random_results.total_results",
                    Map.of("count", String.valueOf(randomResults.getResults().size()))), NamedTextColor.AQUA)
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text(lang.getMessage("random_results.show_chances") + ": " +
                    (randomResults.isShowChances() ? lang.getMessage("info.enabled") : lang.getMessage("info.disabled")),
                    randomResults.isShowChances() ? NamedTextColor.GREEN : NamedTextColor.RED)
                    .decoration(TextDecoration.ITALIC, false));
        } else {
            lore.add(Component.text(lang.getMessage("random_results.not_configured"), NamedTextColor.DARK_GRAY)
                    .decoration(TextDecoration.ITALIC, false));
        }

        lore.add(Component.empty());
        lore.add(Component.text(lang.getMessage("random_results.click_to_configure"), NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, true));
        meta.lore(lore);
        button.setItemMeta(meta);
        inventory.setItem(BTN_RANDOM_RESULTS, button);
    }

    private void addGiveItemButton() {
        ItemStack button = new ItemStack(Material.CHEST);
        ItemMeta meta = button.getItemMeta();
        meta.displayName(Component.text(lang.getMessage("editor.button.get_item_title"), NamedTextColor.GREEN)
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text(lang.getMessage("editor.button.get_item_desc1"), NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text(lang.getMessage("editor.button.get_item_desc2"), NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text(lang.getMessage("editor.button.get_item_desc3"), NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(Component.text(lang.getMessage("editor.button.get_item_action"), NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, true));
        meta.lore(lore);
        button.setItemMeta(meta);
        inventory.setItem(BTN_GET_ITEM, button);
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!event.getInventory().equals(inventory)) {
            return;
        }

        if (!(event.getWhoClicked() instanceof Player dragPlayer) || !dragPlayer.equals(player)) {
            return;
        }

        for (int rawSlot : event.getRawSlots()) {
            if (rawSlot < inventory.getSize()) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getInventory().equals(inventory)) {
            return;
        }

        if (!(event.getWhoClicked() instanceof Player clicker)) {
            return;
        }

        if (!clicker.equals(player)) {
            return;
        }

        int slot = event.getSlot();
        ClickType clickType = event.getClick();
        if (editMode) {
            if (slot == EDIT_RESULT_SLOT) {
                if (event.getClickedInventory() != inventory) {
                    return;
                }
                event.setCancelled(true);
                if (clickType.isLeftClick()) {
                    ItemStack cursor = event.getCursor();
                    if (cursor != null && cursor.getType() != Material.AIR) {
                        if (editResultItem == null || editResultItem.getType() == Material.AIR) {
                            editResultItem = cursor.clone();
                            editResultItem.setAmount(1);
                        } else if (editResultItem.getType() == cursor.getType()) {
                            int newAmount = Math.min(editResultItem.getAmount() + 1, cursor.getType().getMaxStackSize());
                            editResultItem.setAmount(newAmount);
                        } else {
                            editResultItem = cursor.clone();
                            editResultItem.setAmount(1);
                        }
                    } else if (editResultItem != null && editResultItem.getType() != Material.AIR) {
                        int newAmount = Math.min(editResultItem.getAmount() + 1, editResultItem.getType().getMaxStackSize());
                        editResultItem.setAmount(newAmount);
                    }
                    updateInventory();
                } else if (clickType.isRightClick()) {
                    if (editResultItem != null && editResultItem.getType() != Material.AIR) {
                        if (editResultItem.getAmount() > 1) {
                            editResultItem.setAmount(editResultItem.getAmount() - 1);
                        } else {
                            editResultItem = null;
                        }
                        updateInventory();
                    }
                }
                return;
            }
            boolean isGridSlot = false;
            int gridIndex = -1;

            if (isFurnaceRecipe()) {
                if (slot == 12) {
                    isGridSlot = true;
                    gridIndex = 0; 
                } else if (slot == 30) {
                    isGridSlot = true;
                    gridIndex = 1; 
                }
            } else {
                for (int i = 0; i < GRID_SLOTS.length; i++) {
                    if (slot == GRID_SLOTS[i]) {
                        isGridSlot = true;
                        gridIndex = i;
                        break;
                    }
                }
            }
            if (isGridSlot) {
                if (event.getClickedInventory() != inventory) {
                    return;
                }
                event.setCancelled(true);
                if (clickType.isLeftClick()) {
                    ItemStack cursor = event.getCursor();
                    if (cursor != null && cursor.getType() != Material.AIR) {
                        ItemStack existing = editGridItems[gridIndex];
                        int maxStack = cursor.getType().getMaxStackSize();

                        if (existing == null || existing.getType() == Material.AIR) {
                            editGridItems[gridIndex] = cursor.clone();
                            editGridItems[gridIndex].setAmount(1);
                        } else if (existing.isSimilar(cursor)) {
                            existing.setAmount(Math.min(existing.getAmount() + 1, maxStack));
                        } else {
                            editGridItems[gridIndex] = cursor.clone();
                            editGridItems[gridIndex].setAmount(1);
                        }
                    } else {
                        ItemStack existing = editGridItems[gridIndex];
                        if (existing != null && existing.getType() != Material.AIR) {
                            int maxStack = existing.getType().getMaxStackSize();
                            existing.setAmount(Math.min(existing.getAmount() + 1, maxStack));
                        }
                    }
                    updateInventory();
                } else if (clickType.isRightClick()) {
                    ItemStack existing = editGridItems[gridIndex];
                    if (existing != null && existing.getType() != Material.AIR) {
                        if (existing.getAmount() > 1) {
                            existing.setAmount(existing.getAmount() - 1);
                        } else {
                            editGridItems[gridIndex] = null;
                        }
                        updateInventory();
                    }
                } else if (clickType == ClickType.MIDDLE) {
                    editGridItems[gridIndex] = null;
                    updateInventory();
                }
                return;
            }
            if (event.getClickedInventory() == inventory) {
                event.setCancelled(true);
                if (slot == 45) {
                    saveEditedRecipe();
                    return;
                }
                if (slot == 46) {
                    editMode = false;
                    editResultItem = null;
                    Arrays.fill(editGridItems, null);
                    updateInventory();
                    return;
                }
            }
            return;
        }

        if (event.getClickedInventory() == inventory) {
            event.setCancelled(true);
        }

        if (event.getClickedInventory() != inventory) {
            return;
        }

        ItemStack clicked = event.getCurrentItem();
        if (slot != BTN_DELETE && deleteConfirmation) {
            deleteConfirmation = false;
            updateInventory();
        }

        if (!player.hasPermission("customrecipes.manage")) {
            MessageUtil.sendError(player, lang.getMessage("editor.error.no_permission"));
            return;
        }

        if (clicked == null || clicked.getType() == Material.AIR) {
            return;
        }

        if (slot == BTN_EDIT_PATTERN) {
            editMode = true;
            initializeEditGrid();
            updateInventory();
            return;
        }

        if (isFurnaceRecipe()) {
            if (slot == BTN_FURNACE_TOGGLE) {
                boolean currentlyEnabled = plugin.getRecipeManager().isRecipeEnabled(recipe.getKey());
                if (currentlyEnabled) {
                    plugin.getRecipeManager().disableRecipe(recipe.getKey());
                    plugin.getRecipeStateTracker().markRecipeDisabled(recipe.getKey());
                    MessageUtil.sendAdminWarning(player, lang.getMessage("editor.success.recipe_disabled", Map.of("recipe", recipe.getKey())));
                } else {
                    plugin.getRecipeManager().enableRecipe(recipe.getKey());
                    plugin.getRecipeStateTracker().markRecipeEnabled(recipe.getKey());
                    MessageUtil.sendAdminSuccess(player, lang.getMessage("editor.success.recipe_enabled", Map.of("recipe", recipe.getKey())));
                }
                updateInventory();
                return;
            }
        } else {
            if (slot == BTN_TOGGLE) {
                boolean currentlyEnabled = plugin.getRecipeManager().isRecipeEnabled(recipe.getKey());
                if (currentlyEnabled) {
                    plugin.getRecipeManager().disableRecipe(recipe.getKey());
                    plugin.getRecipeStateTracker().markRecipeDisabled(recipe.getKey());
                    MessageUtil.sendAdminWarning(player, lang.getMessage("editor.success.recipe_disabled", Map.of("recipe", recipe.getKey())));
                } else {
                    plugin.getRecipeManager().enableRecipe(recipe.getKey());
                    plugin.getRecipeStateTracker().markRecipeEnabled(recipe.getKey());
                    MessageUtil.sendAdminSuccess(player, lang.getMessage("editor.success.recipe_enabled", Map.of("recipe", recipe.getKey())));
                }
                updateInventory();
                return;
            }
        }

        if (isFurnaceRecipe()) {
            if (slot == BTN_FURNACE_WORLD_SETTINGS) {
                new WorldSettingsGUI(plugin, player, recipe).open();
                return;
            }
        } else {
            if (slot == BTN_WORLD_SETTINGS) {
                new WorldSettingsGUI(plugin, player, recipe).open();
                return;
            }
        }

        if (slot == BTN_EDIT_RESULT) {
            ItemStack resultItem = recipe.getResultItem();

            if (recipe.hasRandomResults() && recipe.getRandomResults().hasRandomResults()) {
                
                List<ItemStack> variants = new ArrayList<>();
                for (org.hikarii.customrecipes.recipe.data.RandomResult rr : recipe.getRandomResults().getResults()) {
                    variants.add(rr.getItem().clone());
                }

                new ItemEditorGUI(plugin, player, variants, 0, (editedVariants) -> {
                    if (editedVariants != null && !editedVariants.isEmpty()) {
                        
                        ItemEditorGUI editor = ItemEditorGUI.getLastEditor(player.getUniqueId());
                        String newName = editor != null ? editor.getCustomName() : null;
                        List<String> newDesc = editor != null ? editor.getCustomLore() : null;
                        plugin.getRecipeManager().updateRecipeResult(recipe.getKey(), editedVariants.get(0),
                                newName, newDesc);

                        List<org.hikarii.customrecipes.recipe.data.RandomResult> originalResults =
                                recipe.getRandomResults().getResults();
                        List<org.hikarii.customrecipes.recipe.data.RandomResult> newResults = new ArrayList<>();

                        for (int i = 0; i < editedVariants.size() && i < originalResults.size(); i++) {
                            org.hikarii.customrecipes.recipe.data.RandomResult original = originalResults.get(i);
                            newResults.add(new org.hikarii.customrecipes.recipe.data.RandomResult(
                                    editedVariants.get(i), original.getWeight(), original.getName()));
                        }

                        org.hikarii.customrecipes.recipe.data.RandomResultPool newPool =
                                new org.hikarii.customrecipes.recipe.data.RandomResultPool(
                                        newResults, recipe.getRandomResults().isShowChances());
                        saveRandomResultsToFile(recipe.getKey(), newPool);

                        MessageUtil.sendAdminSuccess(player, lang.getMessage("editor.success.result_updated", Map.of("recipe", recipe.getKey())));
                    }
                    
                    try {
                        plugin.getRecipeManager().unregisterAll();
                        plugin.getConfigManager().loadRecipes();
                        plugin.getRecipeManager().registerAllRecipes();
                    } catch (Exception e) {
                        plugin.getLogger().severe("Failed to reload recipes: " + e.getMessage());
                    }
                    CustomRecipe updatedRecipe = plugin.getRecipeManager().getRecipe(recipe.getKey());
                    if (updatedRecipe != null) {
                        new RecipeEditorGUI(plugin, player, updatedRecipe).open();
                    }
                }).open();
            } else {
                
                new ItemEditorGUI(plugin, player, resultItem, (editedItem) -> {
                    if (editedItem != null) {
                        ItemEditorGUI editor = ItemEditorGUI.getLastEditor(player.getUniqueId());
                        String newName = editor != null ? editor.getCustomName() : null;
                        List<String> newDesc = editor != null ? editor.getCustomLore() : null;
                        plugin.getRecipeManager().updateRecipeResult(recipe.getKey(), editedItem,
                                newName, newDesc);
                        MessageUtil.sendAdminSuccess(player, lang.getMessage("editor.success.result_updated", Map.of("recipe", recipe.getKey())));
                    }
                    CustomRecipe updatedRecipe = plugin.getRecipeManager().getRecipe(recipe.getKey());
                    if (updatedRecipe != null) {
                        new RecipeEditorGUI(plugin, player, updatedRecipe).open();
                    }
                }).open();
            }
            return;
        }

        if (slot == BTN_CONDITIONS || (isFurnaceRecipe() && slot == BTN_FURNACE_CONDITIONS)) {
            new ConditionsSelectorGUI(plugin, player, recipe, () -> {
                CustomRecipe updatedRecipe = plugin.getRecipeManager().getRecipe(recipe.getKey());
                if (updatedRecipe != null) {
                    new RecipeEditorGUI(plugin, player, updatedRecipe).open();
                }
            }).open();
            return;
        }

        if (slot == BTN_SMELTING_CONDITIONS && isFurnaceRecipe()) {
            new SmeltingConditionsGUI(plugin, player, recipe, () -> {
                CustomRecipe updatedRecipe = plugin.getRecipeManager().getRecipe(recipe.getKey());
                if (updatedRecipe != null) {
                    new RecipeEditorGUI(plugin, player, updatedRecipe).open();
                }
            }).open();
            return;
        }

        if (slot == BTN_DELETE) {
            if (!deleteConfirmation) {
                deleteConfirmation = true;
                updateInventory();
                MessageUtil.sendAdminWarning(player, lang.getMessage("editor.warning.confirm_deletion"));
                return;
            }
            String recipeKey = recipe.getKey();
            if (plugin.getRecipeManager().deleteRecipePermanently(recipeKey)) {
                plugin.getConfigManager().removeEnabledRecipe(recipeKey);
                MessageUtil.sendAdminSuccess(player, lang.getMessage("editor.success.recipe_deleted", Map.of("recipe", recipeKey)));
                player.closeInventory();
                new RecipeListGUI(plugin, player).open();
            } else {
                MessageUtil.sendError(player, lang.getMessage("editor.error.delete_failed"));
                deleteConfirmation = false;
                updateInventory();
            }
            return;
        }

        boolean isVisibilityClick = isFurnaceRecipe() ? (slot == BTN_FURNACE_VISIBILITY) : (slot == BTN_VISIBILITY);
        if (isVisibilityClick) {
            boolean newValue = !recipe.isHidden();
            String recipeKey = recipe.getKey();
            try {
                File recipeFile = plugin.getConfigManager().getRecipeFileManager().findRecipeFile(recipeKey);
                if (recipeFile != null && recipeFile.exists()) {
                    YamlConfiguration config = YamlConfiguration.loadConfiguration(recipeFile);
                    config.set("hidden", newValue);
                    config.save(recipeFile);

                    plugin.getRecipeManager().unregisterAll();
                    plugin.getConfigManager().loadRecipes();
                    plugin.getRecipeManager().registerAllRecipes();

                    MessageUtil.sendAdminSuccess(player,
                            lang.getMessage(newValue ? "editor.success.recipe_now_hidden" : "editor.success.recipe_now_visible"));
                    CustomRecipe updatedRecipe = plugin.getRecipeManager().getRecipe(recipeKey);
                    if (updatedRecipe != null) {
                        new RecipeEditorGUI(plugin, player, updatedRecipe).open();
                    } else {
                        new RecipeListGUI(plugin, player).open();
                    }
                } else {
                    MessageUtil.sendError(player, lang.getMessage("editor.error.file_not_found"));
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to toggle hidden state: " + e.getMessage());
                MessageUtil.sendError(player, lang.getMessage("editor.error.update_failed"));
            }
            return;
        }

        if (slot == BTN_RANDOM_RESULTS) {
            
            String recipeKey = recipe.getKey();
            ItemStack defaultResult = recipe.getResultItem();
            RandomResultsGUI randomResultsGUI = new RandomResultsGUI(plugin, player, randomResults,
                    defaultResult, pool -> {
                
                if (pool != null && pool.isCancelled()) {
                    new RecipeEditorGUI(plugin, player, recipe).open();
                    return;
                }

                RandomResultPool poolToSave = (pool != null && pool.isEmptySaved()) ? null : pool;

                saveRandomResultsToFile(recipeKey, poolToSave);
                
                try {
                    plugin.getRecipeManager().unregisterAll();
                    plugin.getConfigManager().loadRecipes();
                    plugin.getRecipeManager().registerAllRecipes();
                    CustomRecipe currentRecipe = plugin.getRecipeManager().getRecipe(recipeKey);
                    if (currentRecipe != null) {
                        new RecipeEditorGUI(plugin, player, currentRecipe).open();
                    }
                } catch (Exception e) {
                    plugin.getLogger().severe("Failed to reload recipes: " + e.getMessage());
                    MessageUtil.sendError(player, lang.getMessage("editor.error.update_failed"));
                }
            });
            randomResultsGUI.open();
            return;
        }

        if (slot == BTN_BACK) {
            new RecipeListGUI(plugin, player).open();
            return;
        }

        if (slot == BTN_GET_ITEM) {
            ItemStack resultItem = recipe.getResultItem().clone();
            if (player.getInventory().firstEmpty() != -1) {
                player.getInventory().addItem(resultItem);
                MessageUtil.sendAdminSuccess(player, lang.getMessage("editor.success.item_received", Map.of("recipe", recipe.getKey())));
                try {
                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ITEM_PICKUP, 0.5f, 1.0f);
                } catch (Exception ignored) {}
            } else {
                MessageUtil.sendError(player, lang.getMessage("editor.error.inventory_full"));
            }
            return;
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().equals(inventory) && event.getPlayer().equals(player)) {
            InventoryClickEvent.getHandlerList().unregister(this);
            InventoryCloseEvent.getHandlerList().unregister(this);
        }
    }

    private void saveRandomResultsToFile(String recipeKey, RandomResultPool pool) {
        try {
            File recipeFile = plugin.getConfigManager().getRecipeFileManager().findRecipeFile(recipeKey);
            if (recipeFile == null || !recipeFile.exists()) {
                plugin.getLogger().warning("Recipe file not found for: " + recipeKey);
                return;
            }

            YamlConfiguration config = YamlConfiguration.loadConfiguration(recipeFile);

            if (pool == null || !pool.hasRandomResults()) {
                
                config.set("random-results", null);
            } else {
                
                Map<String, Object> randomResultsData = new HashMap<>();
                randomResultsData.put("show-chances", pool.isShowChances());
                if (pool.getFailureChance() > 0) {
                    randomResultsData.put("failure-chance", pool.getFailureChance());
                }

                List<Map<String, Object>> items = new ArrayList<>();
                for (RandomResult result : pool.getResults()) {
                    Map<String, Object> itemData = ItemStackSerializer.toMap(result.getItem());
                    itemData.put("chance", result.getWeight());
                    if (result.hasCustomName()) {
                        itemData.put("name", result.getName());
                    }
                    items.add(itemData);
                }
                randomResultsData.put("items", items);
                config.set("random-results", randomResultsData);
            }

            config.save(recipeFile);
            MessageUtil.sendAdminSuccess(player, lang.getMessage("random_results.saved"));
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to save random results: " + e.getMessage());
            MessageUtil.sendError(player, lang.getMessage("editor.error.save_failed"));
        }
    }
}