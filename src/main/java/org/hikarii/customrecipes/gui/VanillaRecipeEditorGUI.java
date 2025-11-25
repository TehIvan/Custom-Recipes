package org.hikarii.customrecipes.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.hikarii.customrecipes.CustomRecipes;
import org.hikarii.customrecipes.language.LanguageManager;
import org.hikarii.customrecipes.recipe.RecipeConditions;
import org.hikarii.customrecipes.recipe.RecipeType;
import org.hikarii.customrecipes.recipe.vanilla.IngredientChoice;
import org.hikarii.customrecipes.recipe.vanilla.VanillaRecipeInfo;
import org.hikarii.customrecipes.recipe.vanilla.VanillaRecipeManager;
import org.hikarii.customrecipes.util.MessageUtil;
import java.util.*;

public class VanillaRecipeEditorGUI implements Listener {
    private static final Map<UUID, VanillaRecipeEditorGUI> waitingForSearch = new HashMap<>();
    private static final int[] GRID_SLOTS = {10, 11, 12, 19, 20, 21, 28, 29, 30};
    private static final int[] FURNACE_INPUT_SLOT = {12}; 
    private static final int RESULT_SLOT = 25;
    private static final int INGREDIENT_CYCLE_BUTTON = 52;
    private int currentVariantIndex = 0;
    private int maxVariants = 1;
    private long lastVariantSwitch = 0;
    private static final long SWITCH_COOLDOWN = 200;
    private boolean awaitingResetConfirmation = false;
    private boolean awaitingSubGUI = false;

    private final CustomRecipes plugin;
    private final Player player;
    private final LanguageManager lang;
    private final VanillaRecipeInfo originalRecipe;
    private final VanillaRecipesGUI parentGUI;
    private final VanillaRecipeSearchResultsGUI searchResultsGUI;
    private final Inventory inventory;
    private final ItemStack[] gridItems = new ItemStack[9];
    private RecipeType currentType;

    public VanillaRecipeEditorGUI(CustomRecipes plugin, Player player, VanillaRecipeInfo recipe, VanillaRecipesGUI parentGUI) {
        this(plugin, player, recipe, parentGUI, null);
    }

    public VanillaRecipeEditorGUI(CustomRecipes plugin, Player player, VanillaRecipeInfo recipe, VanillaRecipesGUI parentGUI, VanillaRecipeSearchResultsGUI searchResultsGUI) {
        this.plugin = plugin;
        this.player = player;
        this.lang = plugin.getLanguageManager();
        this.originalRecipe = recipe;
        this.parentGUI = parentGUI;
        this.searchResultsGUI = searchResultsGUI;
        this.currentType = recipe.getType();
        this.inventory = Bukkit.createInventory(
                null,
                54,
                MessageUtil.createMenuTitle(lang.getMessage("vanilla.editor_title_simple"), NamedTextColor.DARK_AQUA)
        );
        String recipeKey = recipe.getKey().replace("minecraft:", "");
        this.maxVariants = plugin.getVanillaRecipeManager().getMaxVariantsForRecipe(recipeKey);
        VanillaRecipeManager.VanillaRecipeState state = plugin.getVanillaRecipeManager().getRecipeState(recipeKey);

        if (state != null) {
            this.currentVariantIndex = state.getCurrentVariantIndex();
            loadVariantPattern(currentVariantIndex);
        } else {
            this.currentVariantIndex = 0;
            loadOriginalPattern();
        }
        plugin.getVanillaRecipeManager().setCurrentVariant(recipeKey, currentVariantIndex);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        updateInventory();
    }

    private void loadOriginalPattern() {
        List<String> pattern = originalRecipe.getPattern();
        for (int i = 0; i < Math.min(pattern.size(), 3); i++) {
            String[] row = pattern.get(i).split(" ");
            for (int j = 0; j < Math.min(row.length, 3); j++) {
                String materialName = row[j];
                if (!materialName.equals("AIR") && !materialName.isEmpty()) {
                    Material material = Material.getMaterial(materialName);
                    if (material != null) {
                        gridItems[i * 3 + j] = new ItemStack(material);
                    }
                }
            }
        }
    }

    private void loadChangedPattern(List<String> pattern) {
        for (int i = 0; i < Math.min(pattern.size(), 3); i++) {
            String[] row = pattern.get(i).split(" ");
            for (int j = 0; j < Math.min(row.length, 3); j++) {
                String materialName = row[j];
                if (!materialName.equals("AIR") && !materialName.isEmpty()) {
                    Material material = Material.getMaterial(materialName);
                    if (material != null) {
                        gridItems[i * 3 + j] = new ItemStack(material);
                    }
                }
            }
        }
    }

    private void loadVariantPattern(int variantIndex) {
        Arrays.fill(gridItems, null);
        String recipeKey = originalRecipe.getKey().replace("minecraft:", "");
        VanillaRecipeManager.VanillaRecipeState state = plugin.getVanillaRecipeManager().getRecipeState(recipeKey);
        if (state != null && state.getPatternForVariant(variantIndex) != null) {
            List<String> pattern = state.getPatternForVariant(variantIndex);
            
            if (!isFurnaceRecipe()) {
                this.currentType = state.getTypeForVariant(variantIndex);
            }

            List<ItemStack> exactItems = state.getExactItemsForVariant(variantIndex);

            for (int i = 0; i < Math.min(pattern.size(), 3); i++) {
                String[] row = pattern.get(i).split(" ");
                for (int j = 0; j < Math.min(row.length, 3); j++) {
                    int gridIndex = i * 3 + j;
                    String materialName = row[j];
                    if (!materialName.equals("AIR") && !materialName.isEmpty()) {
                        
                        if (exactItems != null && gridIndex < exactItems.size() && exactItems.get(gridIndex) != null) {
                            gridItems[gridIndex] = exactItems.get(gridIndex).clone();
                        } else {
                            
                            Material material = Material.getMaterial(materialName);
                            if (material != null) {
                                gridItems[gridIndex] = new ItemStack(material);
                            } else {
                                gridItems[gridIndex] = null;
                            }
                        }
                    } else {
                        gridItems[gridIndex] = null;
                    }
                }
            }
        } else {
            loadOriginalPattern();
        }
    }

    private void loadPatternToGrid(List<String> pattern) {
        for (int i = 0; i < Math.min(pattern.size(), 3); i++) {
            String[] row = pattern.get(i).split(" ");
            for (int j = 0; j < Math.min(row.length, 3); j++) {
                String materialName = row[j];
                if (!materialName.equals("AIR") && !materialName.isEmpty()) {
                    Material material = Material.getMaterial(materialName);
                    if (material != null) {
                        gridItems[i * 3 + j] = new ItemStack(material);
                    }
                } else {
                    gridItems[i * 3 + j] = null;
                }
            }
        }
    }

    public void open() {
        player.openInventory(inventory);
    }

    private void updateInventory() {
        inventory.clear();
        fillBorders();
        addGridItems();
        addResultItem();
        addEqualsSign();
        if (isFurnaceRecipe()) {
            addFurnacePlusSymbol();
            addFurnaceFuelSlot();
            addFurnaceButtons();
        } else {
            addTypeToggle();
            addButtons();
        }
    }

    private void fillBorders() {
        ItemStack borderPane = new ItemStack(Material.CYAN_STAINED_GLASS_PANE);
        ItemMeta meta = borderPane.getItemMeta();
        meta.displayName(Component.empty());
        borderPane.setItemMeta(meta);
        for (int i = 0; i < 54; i++) {
            inventory.setItem(i, borderPane);
        }

        if (isFurnaceRecipe()) {
            inventory.setItem(12, null); 
        } else {
            for (int slot : GRID_SLOTS) {
                inventory.setItem(slot, null);
            }
        }
        inventory.setItem(RESULT_SLOT, null);
    }

    private boolean isFurnaceRecipe() {
        return originalRecipe.getType() == RecipeType.FURNACE ||
               originalRecipe.getType() == RecipeType.BLAST_FURNACE ||
               originalRecipe.getType() == RecipeType.SMOKER;
    }

    private void addGridItems() {
        if (isFurnaceRecipe()) {
            
            if (gridItems[0] != null) {
                ItemStack display = gridItems[0].clone();
                ItemMeta meta = display.getItemMeta();
                List<Component> displayLore = new ArrayList<>();
                displayLore.add(Component.empty());
                displayLore.add(Component.text(lang.getMessage("furnace_creator.input_hint"), NamedTextColor.YELLOW)
                        .decoration(TextDecoration.ITALIC, false));
                meta.lore(displayLore);
                display.setItemMeta(meta);
                inventory.setItem(12, display);
            }
        } else {
            
            for (int i = 0; i < 9; i++) {
                if (gridItems[i] != null) {
                    ItemStack display = gridItems[i].clone();
                    ItemMeta meta = display.getItemMeta();

                    List<Component> originalLore = gridItems[i].hasItemMeta() &&
                                                  gridItems[i].getItemMeta().hasLore() &&
                                                  gridItems[i].getItemMeta().lore() != null ?
                            new ArrayList<>(gridItems[i].getItemMeta().lore()) : new ArrayList<>();

                    List<Component> displayLore = new ArrayList<>(originalLore);
                    displayLore.add(Component.empty());
                    displayLore.add(Component.text(lang.getMessage("vanilla.amount_display", Map.of("amount", String.valueOf(display.getAmount()))), NamedTextColor.YELLOW)
                            .decoration(TextDecoration.ITALIC, false));
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
                    inventory.setItem(GRID_SLOTS[i], display);
                }
            }
        }
    }

    private void addResultItem() {
        
        String recipeKey = originalRecipe.getKey().replace("minecraft:", "");
        VanillaRecipeManager.VanillaRecipeState state = plugin.getVanillaRecipeManager().getRecipeState(recipeKey);
        int resultAmount = originalRecipe.getResultAmount();

        if (state != null) {
            Integer variantAmount = state.getResultAmountForVariant(currentVariantIndex);
            if (variantAmount != null) {
                resultAmount = variantAmount;
            } else if (state.getCustomResultAmount() != null) {
                resultAmount = state.getCustomResultAmount();
            }
        }

        ItemStack result = originalRecipe.hasVariantResults() ?
                originalRecipe.getResultForVariant(currentVariantIndex) :
                new ItemStack(originalRecipe.getResultMaterial(), resultAmount);

        if (state != null) {
            Integer variantAmount = state.getResultAmountForVariant(currentVariantIndex);
            if (variantAmount != null) {
                result.setAmount(variantAmount);
            } else if (state.getCustomResultAmount() != null) {
                result.setAmount(state.getCustomResultAmount());
            }
        }

        ItemMeta meta = result.getItemMeta();
        if (meta != null) {
            List<Component> lore = meta.hasLore() && meta.lore() != null ?
                    new ArrayList<>(meta.lore()) : new ArrayList<>();
            lore.add(Component.empty());
            lore.add(Component.text(lang.getMessage("vanilla.left_click_add_one"), NamedTextColor.GREEN)
                    .decoration(TextDecoration.ITALIC, true));
            lore.add(Component.text(lang.getMessage("vanilla.right_click_remove_one"), NamedTextColor.RED)
                    .decoration(TextDecoration.ITALIC, true));
            meta.lore(lore);
            result.setItemMeta(meta);
        }
        inventory.setItem(RESULT_SLOT, result);
    }

    private void addEqualsSign() {
        ItemStack equals = new ItemStack(Material.ORANGE_STAINED_GLASS_PANE);
        ItemMeta meta = equals.getItemMeta();
        meta.displayName(Component.text(lang.getMessage("vanilla.equals_symbol"), NamedTextColor.GOLD)
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
                Component.empty(),
                Component.text(lang.getMessage("vanilla.place_ingredients_line1"), NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false),
                Component.text(lang.getMessage("vanilla.place_ingredients_line2"), NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false),
                Component.empty(),
                Component.text(lang.getMessage("vanilla.place_result_line1"), NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false),
                Component.text(lang.getMessage("vanilla.place_result_line2"), NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false)
        ));
        equals.setItemMeta(meta);
        inventory.setItem(23, equals);
    }

    private void addTypeToggle() {
        
        if (isFurnaceRecipe()) {
            addSmeltingConditionsButton(51);
            return;
        }

        boolean isShaped = currentType == RecipeType.SHAPED;
        ItemStack toggle = new ItemStack(isShaped ? Material.CRAFTING_TABLE : Material.CHEST);
        ItemMeta meta = toggle.getItemMeta();
        String recipeName = isShaped ? lang.getMessage("vanilla.shaped_recipe") : lang.getMessage("vanilla.shapeless_recipe");
        meta.displayName(Component.text(
                recipeName,
                isShaped ? NamedTextColor.AQUA : NamedTextColor.LIGHT_PURPLE
        ).decoration(TextDecoration.BOLD, true).decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        if (isShaped) {
            lore.add(Component.text(lang.getMessage("vanilla.position_matters"), NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false));
        } else {
            lore.add(Component.text(lang.getMessage("vanilla.position_no_matter"), NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false));
        }
        lore.add(Component.empty());
        lore.add(Component.text(lang.getMessage("lore.click_to_toggle"), NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, true));
        meta.lore(lore);
        toggle.setItemMeta(meta);
        inventory.setItem(49, toggle); 
    }

    private void addSmeltingConditionsButton(int slot) {
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

        int cookTime = 200;
        double experience = 0.1;
        String recipeKey = originalRecipe.getKey().replace("minecraft:", "");

        try {
            java.io.File configFile = new java.io.File(plugin.getDataFolder(), "vanilla-furnace-recipes.yml");
            if (configFile.exists()) {
                org.bukkit.configuration.file.YamlConfiguration config =
                    org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(configFile);
                cookTime = config.getInt("recipes." + recipeKey + ".cooking_time", 200);
                experience = config.getDouble("recipes." + recipeKey + ".experience", 0.1);
            }
        } catch (Exception e) {
            plugin.debug("Could not load smelting conditions for " + recipeKey);
        }

        lore.add(Component.text(lang.getMessage("editor.button.smelting_conditions_current"), NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text(lang.getMessage("editor.button.smelting_conditions_cook_time",
                Map.of("value", String.valueOf(cookTime))), NamedTextColor.AQUA)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text(lang.getMessage("editor.button.smelting_conditions_experience",
                Map.of("value", String.format("%.1f", experience))), NamedTextColor.AQUA)
                .decoration(TextDecoration.ITALIC, false));

        lore.add(Component.empty());
        lore.add(Component.text(lang.getMessage("editor.button.smelting_conditions_action"), NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, true));

        meta.lore(lore);
        button.setItemMeta(meta);
        inventory.setItem(slot, button);
    }

    private void addFurnacePlusSymbol() {
        ItemStack plus = new ItemStack(Material.ORANGE_STAINED_GLASS_PANE);
        ItemMeta meta = plus.getItemMeta();
        meta.displayName(Component.text("+", NamedTextColor.GOLD)
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
                Component.empty(),
                Component.text(lang.getMessage("furnace_creator.plus_desc"), NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false),
                Component.text(lang.getMessage("furnace_creator.fuel_optional"), NamedTextColor.DARK_GRAY)
                        .decoration(TextDecoration.ITALIC, false)
        ));
        plus.setItemMeta(meta);
        inventory.setItem(21, plus); 
    }

    private void addFurnaceFuelSlot() {
        ItemStack fuel = new ItemStack(Material.COAL);
        ItemMeta meta = fuel.getItemMeta();
        meta.displayName(Component.text(lang.getMessage("furnace_creator.fuel_slot"), NamedTextColor.GOLD)
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
                Component.empty(),
                Component.text(lang.getMessage("furnace_creator.fuel_hint"), NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false)
        ));
        fuel.setItemMeta(meta);
        inventory.setItem(30, fuel); 
    }

    private void addFurnaceButtons() {
        if (maxVariants > 1) {
            addVariantSwitcher();
        }

        addRecipeInfoButton(44);

        ItemStack search = new ItemStack(Material.COMPASS);
        ItemMeta searchMeta = search.getItemMeta();
        searchMeta.displayName(Component.text(lang.getMessage("vanilla.new_search"), NamedTextColor.LIGHT_PURPLE)
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false));
        searchMeta.lore(List.of(
                Component.empty(),
                Component.text(lang.getMessage("vanilla.search_another_recipe"), NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false)
        ));
        search.setItemMeta(searchMeta);
        inventory.setItem(45, search);

        addSmeltingConditionsButton(46);

        addConditionsButton(47);

        addFurnaceCraftEventsButton(48);

        ItemStack save = new ItemStack(Material.LIME_WOOL);
        ItemMeta saveMeta = save.getItemMeta();
        saveMeta.displayName(Component.text(lang.getMessage("button.save"), NamedTextColor.GREEN)
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false));
        saveMeta.lore(List.of(
                Component.empty(),
                Component.text(lang.getMessage("vanilla.save_changes_stay"), NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false)
        ));
        save.setItemMeta(saveMeta);
        inventory.setItem(49, save);

        ItemStack close = new ItemStack(Material.RED_WOOL);
        ItemMeta closeMeta = close.getItemMeta();
        closeMeta.displayName(Component.text(lang.getMessage("button.close"), NamedTextColor.RED)
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false));
        closeMeta.lore(List.of(
                Component.empty(),
                Component.text(lang.getMessage("vanilla.return_to_list"), NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false)
        ));
        close.setItemMeta(closeMeta);
        inventory.setItem(50, close);

        addToggleButton(52);

        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta backMeta = back.getItemMeta();
        backMeta.displayName(Component.text(lang.getMessage("vanilla.back_to_vanilla_recipes"), NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));
        backMeta.lore(List.of(
                Component.empty(),
                Component.text(lang.getMessage("vanilla.return_to_list"), NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false)
        ));
        back.setItemMeta(backMeta);
        inventory.setItem(53, back);
    }

    private void addFurnaceCraftEventsButton(int slot) {
        String recipeKey = originalRecipe.getKey().replace("minecraft:", "");
        String currentPreset = plugin.getVanillaRecipeManager().getCraftEventPreset(recipeKey);

        ItemStack button = new ItemStack(Material.BELL);
        ItemMeta meta = button.getItemMeta();
        meta.displayName(Component.text(lang.getMessage("vanilla.craft_events_title"), NamedTextColor.GOLD)
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text(lang.getMessage("vanilla.craft_events_desc1"), NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text(lang.getMessage("vanilla.craft_events_desc2"), NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());

        if (currentPreset != null && !currentPreset.isEmpty()) {
            lore.add(Component.text(lang.getMessage("info.current") + ": ", NamedTextColor.GRAY)
                    .append(Component.text(currentPreset, NamedTextColor.GREEN))
                    .decoration(TextDecoration.ITALIC, false));
        } else {
            lore.add(Component.text(lang.getMessage("vanilla.no_preset_set"), NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false));
        }

        lore.add(Component.empty());
        lore.add(Component.text(lang.getMessage("vanilla.click_to_select_preset"), NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, true));

        meta.lore(lore);
        button.setItemMeta(meta);
        inventory.setItem(slot, button);
    }

    private void addRecipeInfoButton(int slot) {
        ItemStack info = new ItemStack(Material.BOOK);
        ItemMeta meta = info.getItemMeta();
        meta.displayName(Component.text(lang.getMessage("editor.recipe_information"), NamedTextColor.AQUA)
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text(lang.getMessage("vanilla.recipe_key") + ": ", NamedTextColor.GRAY)
                .append(Component.text(originalRecipe.getKey(), NamedTextColor.WHITE))
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text(lang.getMessage("info.type") + ": ", NamedTextColor.GRAY)
                .append(Component.text(originalRecipe.getType().name(), NamedTextColor.WHITE))
                .decoration(TextDecoration.ITALIC, false));
        if (maxVariants > 1) {
            lore.add(Component.text(lang.getMessage("vanilla.variants_count") + ": ", NamedTextColor.GRAY)
                    .append(Component.text(String.valueOf(maxVariants), NamedTextColor.WHITE))
                    .decoration(TextDecoration.ITALIC, false));
        }

        meta.lore(lore);
        info.setItemMeta(meta);
        inventory.setItem(slot, info);
    }

    private void addConditionsButton(int slot) {
        String recipeKey = originalRecipe.getKey().replace("minecraft:", "");
        RecipeConditions conditions = plugin.getVanillaRecipeManager().getRecipeConditions(recipeKey);
        boolean hasConditions = conditions != null && conditions.hasAnyCondition();

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

        if (hasConditions) {
            lore.add(Component.text("✓ " + lang.getMessage("editor.button.conditions_enabled"), NamedTextColor.GREEN)
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.empty());
        }

        lore.add(Component.text(lang.getMessage("editor.button.conditions_action"), NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, true));

        meta.lore(lore);
        button.setItemMeta(meta);
        inventory.setItem(slot, button);
    }

    private void addResetButton(int slot) {
        ItemStack reset = new ItemStack(awaitingResetConfirmation ? Material.TNT : Material.BARRIER);
        ItemMeta resetMeta = reset.getItemMeta();

        if (awaitingResetConfirmation) {
            resetMeta.displayName(Component.text(lang.getMessage("vanilla.confirm_reset"), NamedTextColor.RED)
                    .decoration(TextDecoration.BOLD, true)
                    .decoration(TextDecoration.ITALIC, false));
            resetMeta.lore(List.of(
                    Component.empty(),
                    Component.text(lang.getMessage("vanilla.click_to_confirm"), NamedTextColor.GOLD)
                            .decoration(TextDecoration.ITALIC, false),
                    Component.text(lang.getMessage("vanilla.click_other_to_cancel"), NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false)
            ));
        } else {
            resetMeta.displayName(Component.text(lang.getMessage("vanilla.reset"), NamedTextColor.RED)
                    .decoration(TextDecoration.BOLD, true)
                    .decoration(TextDecoration.ITALIC, false));
            resetMeta.lore(List.of(
                    Component.empty(),
                    Component.text(lang.getMessage("vanilla.restore_original"), NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false),
                    Component.empty(),
                    Component.text(lang.getMessage("vanilla.cannot_be_undone"), NamedTextColor.GOLD)
                            .decoration(TextDecoration.ITALIC, false)
            ));
        }

        reset.setItemMeta(resetMeta);
        inventory.setItem(slot, reset);
    }

    private void addButtons() {
        if (maxVariants > 1) {
            addVariantSwitcher();
        }

        ItemStack search = new ItemStack(Material.COMPASS);
        ItemMeta searchMeta = search.getItemMeta();
        searchMeta.displayName(Component.text(lang.getMessage("vanilla.new_search"), NamedTextColor.LIGHT_PURPLE)
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false));
        searchMeta.lore(List.of(
                Component.empty(),
                Component.text(lang.getMessage("vanilla.search_another_recipe"), NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false)
        ));
        search.setItemMeta(searchMeta);
        inventory.setItem(45, search);

        addCraftEventsButton(46);

        ItemStack save = new ItemStack(Material.LIME_WOOL);
        ItemMeta saveMeta = save.getItemMeta();
        saveMeta.displayName(Component.text(lang.getMessage("button.save"), NamedTextColor.GREEN)
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false));
        saveMeta.lore(List.of(
                Component.empty(),
                Component.text(lang.getMessage("vanilla.save_changes_stay"), NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false)
        ));
        save.setItemMeta(saveMeta);
        inventory.setItem(48, save);

        ItemStack close = new ItemStack(Material.RED_WOOL);
        ItemMeta closeMeta = close.getItemMeta();
        closeMeta.displayName(Component.text(lang.getMessage("button.close"), NamedTextColor.RED)
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false));
        closeMeta.lore(List.of(
                Component.empty(),
                Component.text(lang.getMessage("vanilla.return_to_list"), NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false)
        ));
        close.setItemMeta(closeMeta);
        inventory.setItem(50, close);

        addToggleButton(52);

        ItemStack reset = new ItemStack(Material.BARRIER);
        ItemMeta resetMeta = reset.getItemMeta();
        resetMeta.displayName(Component.text(lang.getMessage("vanilla.reset"), NamedTextColor.RED)
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false));
        resetMeta.lore(List.of(
                Component.empty(),
                Component.text(lang.getMessage("vanilla.restore_original"), NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false),
                Component.empty(),
                Component.text(lang.getMessage("vanilla.cannot_be_undone"), NamedTextColor.GOLD)
                        .decoration(TextDecoration.ITALIC, false)
        ));
        reset.setItemMeta(resetMeta);
        inventory.setItem(53, reset);
    }

    private void addCraftEventsButton(int slot) {
        String recipeKey = originalRecipe.getKey().replace("minecraft:", "");
        String currentPreset = plugin.getVanillaRecipeManager().getCraftEventPreset(recipeKey);

        ItemStack button = new ItemStack(Material.BELL);
        ItemMeta meta = button.getItemMeta();
        meta.displayName(Component.text(lang.getMessage("vanilla.craft_events_title"), NamedTextColor.GOLD)
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text(lang.getMessage("vanilla.craft_events_desc1"), NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text(lang.getMessage("vanilla.craft_events_desc2"), NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());

        if (currentPreset != null && !currentPreset.isEmpty()) {
            lore.add(Component.text(lang.getMessage("info.current") + ": ", NamedTextColor.GRAY)
                    .append(Component.text(currentPreset, NamedTextColor.GREEN))
                    .decoration(TextDecoration.ITALIC, false));
        } else {
            lore.add(Component.text(lang.getMessage("vanilla.no_preset_set"), NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false));
        }

        lore.add(Component.empty());
        lore.add(Component.text(lang.getMessage("vanilla.click_to_select_preset"), NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, true));

        meta.lore(lore);
        button.setItemMeta(meta);
        inventory.setItem(slot, button);
    }

    private void addVariantSwitcher() {
        String variantName = getVariantName(currentVariantIndex);
        ItemStack switcher = new ItemStack(Material.ARROW);
        ItemMeta meta = switcher.getItemMeta();
        meta.displayName(Component.text(lang.getMessage("vanilla.ingredient_variant"), NamedTextColor.GOLD)
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text(lang.getMessage("vanilla.current"), NamedTextColor.GRAY)
                .append(Component.text(variantName, NamedTextColor.GREEN)
                        .decoration(TextDecoration.BOLD, true))
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(Component.text(lang.getMessage("vanilla.available_variants"), NamedTextColor.AQUA)
                .decoration(TextDecoration.ITALIC, false));

        for (int i = 0; i < maxVariants; i++) {
            String name = getVariantName(i);
            boolean isCurrent = i == currentVariantIndex;
            lore.add(Component.text(isCurrent ? "➤ " : "  ",
                            isCurrent ? NamedTextColor.GREEN : NamedTextColor.GRAY)
                    .append(Component.text(name, isCurrent ? NamedTextColor.GREEN : NamedTextColor.GRAY))
                    .decoration(TextDecoration.ITALIC, false));
        }
        lore.add(Component.empty());
        lore.add(Component.text(lang.getMessage("vanilla.each_variant_line1"), NamedTextColor.DARK_GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text(lang.getMessage("vanilla.each_variant_line2"), NamedTextColor.DARK_GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(Component.text(lang.getMessage("vanilla.click_switch_variant"), NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, true));
        meta.lore(lore);
        switcher.setItemMeta(meta);
        inventory.setItem(8, switcher);
    }

    private void addToggleButton(int slot) {
        String recipeKey = originalRecipe.getKey().replace("minecraft:", "");
        boolean isDisabled = plugin.getVanillaRecipeManager().isVariantDisabled(recipeKey, currentVariantIndex);

        ItemStack toggle = new ItemStack(isDisabled ? Material.RED_DYE : Material.LIME_DYE);
        ItemMeta meta = toggle.getItemMeta();

        String statusText = isDisabled ? lang.getMessage("vanilla.variant_disabled") : lang.getMessage("vanilla.variant_enabled");
        NamedTextColor statusColor = isDisabled ? NamedTextColor.RED : NamedTextColor.GREEN;

        meta.displayName(Component.text(statusText, statusColor)
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        if (isDisabled) {
            lore.add(Component.text(lang.getMessage("vanilla.variant_disabled_desc"), NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false));
        } else {
            lore.add(Component.text(lang.getMessage("vanilla.variant_enabled_desc"), NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false));
        }
        lore.add(Component.empty());
        lore.add(Component.text(lang.getMessage("vanilla.click_to_toggle_variant"), NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, true));

        meta.lore(lore);
        toggle.setItemMeta(meta);
        inventory.setItem(slot, toggle);
    }

    private String getVariantName(int index) {
        IngredientChoice firstChoice = null;
        for (List<IngredientChoice> row : originalRecipe.getIngredientGrid()) {
            for (IngredientChoice choice : row) {
                if (choice.hasMultipleOptions()) {
                    firstChoice = choice;
                    break;
                }
            }
            if (firstChoice != null) break;
        }
        if (firstChoice != null && index < firstChoice.getOptions().size()) {
            Material mat = firstChoice.getOptions().get(index);
            return MessageUtil.formatMaterialName(mat.name());
        }
        return lang.getMessage("vanilla.variant_number", Map.of("number", String.valueOf(index + 1)));
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!event.getInventory().equals(inventory)) {
            return;
        }

        if (!(event.getWhoClicked() instanceof Player dragPlayer) || !dragPlayer.equals(player)) {
            return;
        }

        for (int slot : event.getRawSlots()) {
            if (slot < inventory.getSize()) {
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

        if (!(event.getWhoClicked() instanceof Player clicker) || !clicker.equals(player)) {
            return;
        }

        int slot = event.getSlot();
        ClickType clickType = event.getClick();
        if (slot == 8 && maxVariants > 1) {
            event.setCancelled(true);
            long now = System.currentTimeMillis();
            if (now - lastVariantSwitch < SWITCH_COOLDOWN) {
                return;
            }
            lastVariantSwitch = now;
            saveCurrentVariant();
            currentVariantIndex = (currentVariantIndex + 1) % maxVariants;
            String recipeKey = originalRecipe.getKey().replace("minecraft:", "");
            plugin.getVanillaRecipeManager().setCurrentVariant(recipeKey, currentVariantIndex);
            loadVariantPattern(currentVariantIndex);
            updateInventoryFast();
            return;
        }

        if (slot < 0 || slot >= 54) {
            return;
        }

        if (event.getClickedInventory() != inventory) {
            return;
        }

        if (slot == RESULT_SLOT) {
            event.setCancelled(true);
            ItemStack result = inventory.getItem(RESULT_SLOT);

            if (clickType.isLeftClick()) {
                if (result != null && result.getType() != Material.AIR) {
                    int newAmount = Math.min(result.getAmount() + 1, result.getType().getMaxStackSize());
                    result.setAmount(newAmount);
                    inventory.setItem(RESULT_SLOT, result);
                }
            } else if (clickType.isRightClick()) {
                if (result != null && result.getType() != Material.AIR && result.getAmount() > 1) {
                    result.setAmount(result.getAmount() - 1);
                    inventory.setItem(RESULT_SLOT, result);
                }
            }
            return;
        }

        for (int i = 0; i < GRID_SLOTS.length; i++) {
            if (slot == GRID_SLOTS[i]) {
                event.setCancelled(true);
                ItemStack cursor = event.getCursor();

                if (cursor != null && cursor.getType() != Material.AIR) {
                    
                    gridItems[i] = cursor.clone();
                    gridItems[i].setAmount(1); 
                    updateInventory();
                } else {
                    
                    if (clickType.isLeftClick()) {
                        if (gridItems[i] != null && gridItems[i].getType() != Material.AIR) {
                            
                            int maxStack = gridItems[i].getType().getMaxStackSize();
                            gridItems[i].setAmount(Math.min(gridItems[i].getAmount() + 1, maxStack));
                            updateInventory();
                        }
                    } else if (clickType.isRightClick()) {
                        if (gridItems[i] != null && gridItems[i].getType() != Material.AIR) {
                            if (gridItems[i].getAmount() > 1) {
                                
                                gridItems[i].setAmount(gridItems[i].getAmount() - 1);
                            } else {
                                
                                gridItems[i] = null;
                            }
                            updateInventory();
                        }
                    } else if (clickType == ClickType.MIDDLE) {
                        
                        gridItems[i] = null;
                        updateInventory();
                    }
                }
                return;
            }
        }

        event.setCancelled(true);

        if (awaitingResetConfirmation && slot != 44 && slot != 53) {
            awaitingResetConfirmation = false;
            updateInventory();
        }

        if (isFurnaceRecipe()) {
            
            if (slot == 45) {
                waitingForSearch.put(player.getUniqueId(), this);
                player.closeInventory();
                MessageUtil.sendAdminInfo(player, lang.getMessage("vanilla.type_recipe_search"));
                MessageUtil.sendAdminInfo(player, lang.getMessage("vanilla.type_cancel"));
                return;
            }

            if (slot == 46) {
                openSmeltingConditions();
                return;
            }

            if (slot == 47) {
                openConditions();
                return;
            }

            if (slot == 48) {
                String recipeKey = originalRecipe.getKey().replace("minecraft:", "");
                String currentPreset = plugin.getVanillaRecipeManager().getCraftEventPreset(recipeKey);
                awaitingSubGUI = true;
                CraftEventPresetSelectorGUI selectorGUI = new CraftEventPresetSelectorGUI(
                        plugin, player, currentPreset, false,
                        selectedPreset -> {
                            awaitingSubGUI = false;
                            if (selectedPreset == null) {
                                updateInventory();
                                open();
                            } else if (selectedPreset.isEmpty()) {
                                plugin.getVanillaRecipeManager().setCraftEventPreset(recipeKey, null);
                                MessageUtil.sendAdminSuccess(player, lang.getMessage("craft_events.preset_cleared"));
                                updateInventory();
                                open();
                            } else {
                                plugin.getVanillaRecipeManager().setCraftEventPreset(recipeKey, selectedPreset);
                                MessageUtil.sendAdminSuccess(player, lang.getMessage("craft_events.preset_assigned")
                                        .replace("{preset}", selectedPreset));
                                updateInventory();
                                open();
                            }
                        }
                );
                selectorGUI.open();
                return;
            }

            if (slot == 49) {
                saveRecipe();
                MessageUtil.sendAdminSuccess(player, lang.getMessage("vanilla.saved_recipe_changes"));
                return;
            }

            if (slot == 50) {
                if (searchResultsGUI != null) {
                    searchResultsGUI.reopen();
                } else {
                    VanillaRecipesGUI gui = new VanillaRecipesGUI(plugin, player,
                            parentGUI.getCurrentCategory(),
                            parentGUI.getCurrentStation(),
                            parentGUI.getCurrentPage());
                    gui.open();
                }
                return;
            }

            if (slot == 52) {
                String recipeKey = originalRecipe.getKey().replace("minecraft:", "");
                plugin.getVanillaRecipeManager().toggleVariant(recipeKey, currentVariantIndex);
                boolean isDisabled = plugin.getVanillaRecipeManager().isVariantDisabled(recipeKey, currentVariantIndex);
                if (isDisabled) {
                    MessageUtil.sendAdminWarning(player, lang.getMessage("vanilla.variant_toggled_off", Map.of("variant", getVariantName(currentVariantIndex))));
                } else {
                    MessageUtil.sendAdminSuccess(player, lang.getMessage("vanilla.variant_toggled_on", Map.of("variant", getVariantName(currentVariantIndex))));
                }
                updateInventory();
                return;
            }

            if (slot == 53) {
                if (searchResultsGUI != null) {
                    searchResultsGUI.reopen();
                } else {
                    VanillaRecipesGUI gui = new VanillaRecipesGUI(plugin, player,
                            parentGUI.getCurrentCategory(),
                            parentGUI.getCurrentStation(),
                            parentGUI.getCurrentPage());
                    gui.open();
                }
                return;
            }
        } else {

            if (slot == 45) {
                waitingForSearch.put(player.getUniqueId(), this);
                player.closeInventory();
                MessageUtil.sendAdminInfo(player, lang.getMessage("vanilla.type_recipe_search"));
                MessageUtil.sendAdminInfo(player, lang.getMessage("vanilla.type_cancel"));
                return;
            }

            if (slot == 46) {
                String recipeKey = originalRecipe.getKey().replace("minecraft:", "");
                String currentPreset = plugin.getVanillaRecipeManager().getCraftEventPreset(recipeKey);
                awaitingSubGUI = true;
                CraftEventPresetSelectorGUI selectorGUI = new CraftEventPresetSelectorGUI(
                        plugin, player, currentPreset, false,
                        selectedPreset -> {
                            awaitingSubGUI = false;
                            if (selectedPreset == null) {
                                updateInventory();
                                open();
                            } else if (selectedPreset.isEmpty()) {
                                plugin.getVanillaRecipeManager().setCraftEventPreset(recipeKey, null);
                                MessageUtil.sendAdminSuccess(player, lang.getMessage("craft_events.preset_cleared"));
                                updateInventory();
                                open();
                            } else {
                                plugin.getVanillaRecipeManager().setCraftEventPreset(recipeKey, selectedPreset);
                                MessageUtil.sendAdminSuccess(player, lang.getMessage("craft_events.preset_assigned")
                                        .replace("{preset}", selectedPreset));
                                updateInventory();
                                open();
                            }
                        }
                );
                selectorGUI.open();
                return;
            }

            if (slot == 49) {
                currentType = currentType == RecipeType.SHAPED ? RecipeType.SHAPELESS : RecipeType.SHAPED;
                updateInventory();
                return;
            }

            if (slot == 52) {
                String recipeKey = originalRecipe.getKey().replace("minecraft:", "");
                plugin.getVanillaRecipeManager().toggleVariant(recipeKey, currentVariantIndex);
                boolean isDisabled = plugin.getVanillaRecipeManager().isVariantDisabled(recipeKey, currentVariantIndex);
                if (isDisabled) {
                    MessageUtil.sendAdminWarning(player, lang.getMessage("vanilla.variant_toggled_off", Map.of("variant", getVariantName(currentVariantIndex))));
                } else {
                    MessageUtil.sendAdminSuccess(player, lang.getMessage("vanilla.variant_toggled_on", Map.of("variant", getVariantName(currentVariantIndex))));
                }
                updateInventory();
                return;
            }

            if (slot == 53) {
                String recipeKey = originalRecipe.getKey().replace("minecraft:", "");
                plugin.getVanillaRecipeManager().resetRecipe(recipeKey);
                MessageUtil.sendAdminSuccess(player, lang.getMessage("vanilla.reset_to_original"));
                loadOriginalPattern();
                currentType = originalRecipe.getType();
                updateInventory();
                return;
            }
        }

        if (isFurnaceRecipe() && slot == 52) {
            String recipeKey = originalRecipe.getKey().replace("minecraft:", "");
            plugin.getVanillaRecipeManager().toggleVariant(recipeKey, currentVariantIndex);
            boolean isDisabled = plugin.getVanillaRecipeManager().isVariantDisabled(recipeKey, currentVariantIndex);
            if (isDisabled) {
                MessageUtil.sendAdminWarning(player, lang.getMessage("vanilla.variant_toggled_off", Map.of("variant", getVariantName(currentVariantIndex))));
            } else {
                MessageUtil.sendAdminSuccess(player, lang.getMessage("vanilla.variant_toggled_on", Map.of("variant", getVariantName(currentVariantIndex))));
            }
            updateInventory();
            return;
        }

        if (isFurnaceRecipe() && slot == 48) {
            String recipeKey = originalRecipe.getKey().replace("minecraft:", "");
            String currentPreset = plugin.getVanillaRecipeManager().getCraftEventPreset(recipeKey);
            awaitingSubGUI = true;
            CraftEventPresetSelectorGUI selectorGUI = new CraftEventPresetSelectorGUI(
                    plugin, player, currentPreset, false, 
                    selectedPreset -> {
                        awaitingSubGUI = false;
                        if (selectedPreset == null) {
                            
                            updateInventory();
                            open();
                        } else if (selectedPreset.isEmpty()) {
                            
                            plugin.getVanillaRecipeManager().setCraftEventPreset(recipeKey, null);
                            MessageUtil.sendAdminSuccess(player, lang.getMessage("craft_events.preset_cleared"));
                            updateInventory();
                            open();
                        } else {
                            
                            plugin.getVanillaRecipeManager().setCraftEventPreset(recipeKey, selectedPreset);
                            MessageUtil.sendAdminSuccess(player, lang.getMessage("craft_events.preset_assigned")
                                    .replace("{preset}", selectedPreset));
                            updateInventory();
                            open();
                        }
                    }
            );
            selectorGUI.open();
            return;
        }

        if (slot == 48) {
            saveRecipe();
            MessageUtil.sendAdminSuccess(player, lang.getMessage("vanilla.saved_recipe_changes"));
            return;
        }

        if (slot == 50) {
            if (searchResultsGUI != null) {
                searchResultsGUI.reopen();
            } else {
                VanillaRecipesGUI gui = new VanillaRecipesGUI(plugin, player,
                        parentGUI.getCurrentCategory(),
                        parentGUI.getCurrentStation(),
                        parentGUI.getCurrentPage());
                gui.open();
            }
            return;
        }
    }

    private void openSmeltingConditions() {
        String recipeKey = originalRecipe.getKey().replace("minecraft:", "");

        int cookTime = 200;
        float experience = 0.1f;
        int burnTime = 0;

        try {
            java.io.File configFile = new java.io.File(plugin.getDataFolder(), "vanilla-furnace-recipes.yml");
            if (configFile.exists()) {
                org.bukkit.configuration.file.YamlConfiguration config =
                    org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(configFile);
                cookTime = config.getInt("recipes." + recipeKey + ".cooking_time", 200);
                experience = (float) config.getDouble("recipes." + recipeKey + ".experience", 0.1);
            }
        } catch (Exception e) {
            plugin.debug("Could not load smelting conditions for " + recipeKey);
        }

        awaitingSubGUI = true;
        final String finalRecipeKey = recipeKey;
        final int origCookTime = cookTime;
        final float origExperience = experience;

        new FurnaceConditionsGUI(plugin, player, cookTime, experience, burnTime,
            (newCookTime, newExperience, newBurnTime) -> {
                awaitingSubGUI = false;
                
                if (newCookTime != origCookTime || Math.abs(newExperience - origExperience) > 0.001f) {
                    try {
                        java.io.File configFile = new java.io.File(plugin.getDataFolder(), "vanilla-furnace-recipes.yml");
                        org.bukkit.configuration.file.YamlConfiguration config;
                        if (configFile.exists()) {
                            config = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(configFile);
                        } else {
                            config = new org.bukkit.configuration.file.YamlConfiguration();
                        }
                        config.set("recipes." + finalRecipeKey + ".cooking_time", newCookTime);
                        config.set("recipes." + finalRecipeKey + ".experience", (double) newExperience);
                        config.save(configFile);
                        MessageUtil.sendAdminSuccess(player, lang.getMessage("smelting_conditions.saved"));
                    } catch (Exception e) {
                        MessageUtil.sendError(player, "Failed to save smelting conditions: " + e.getMessage());
                    }
                }
                updateInventory();
                open();
            }).open();
    }

    private void openConditions() {
        String recipeKey = originalRecipe.getKey().replace("minecraft:", "");

        RecipeConditions currentConditions = plugin.getVanillaRecipeManager().getRecipeConditions(recipeKey);
        if (currentConditions == null) {
            currentConditions = new RecipeConditions();
        }

        awaitingSubGUI = true;
        new ConditionsEditorGUI(plugin, player, currentConditions, (newConditions) -> {
            awaitingSubGUI = false;
            
            plugin.getVanillaRecipeManager().setRecipeConditions(recipeKey, newConditions);
            MessageUtil.sendAdminSuccess(player, lang.getMessage("conditions_gui.saved"));
            updateInventory();
            open();
        }).open();
    }

    private void saveRecipe() {
        saveCurrentVariant();
        ItemStack resultItem = inventory.getItem(RESULT_SLOT);
        if (resultItem != null) {
            String recipeKey = originalRecipe.getKey().replace("minecraft:", "");
            
            plugin.getVanillaRecipeManager().updateVariantResultAmount(recipeKey, currentVariantIndex, resultItem.getAmount());
            plugin.debug("Saved custom result amount: " + resultItem.getAmount() + " for recipe: " + recipeKey + " variant: " + currentVariantIndex);
        }
        MessageUtil.sendAdminSuccess(player, lang.getMessage("vanilla.saved_variant", Map.of("variant", getVariantName(currentVariantIndex))));
    }

    private void saveCurrentVariant() {
        List<String> pattern = new ArrayList<>();
        List<ItemStack> exactItems = new ArrayList<>();
        boolean hasExactItems = false;

        for (int row = 0; row < 3; row++) {
            StringBuilder rowPattern = new StringBuilder();
            for (int col = 0; col < 3; col++) {
                ItemStack item = gridItems[row * 3 + col];
                if (item == null) {
                    rowPattern.append("AIR ");
                    exactItems.add(null);
                } else {
                    rowPattern.append(item.getType().name()).append(" ");

                    boolean shouldSaveExact = false;
                    if (item.hasItemMeta()) {
                        ItemMeta meta = item.getItemMeta();

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

                        if (item.getType() == Material.ENCHANTED_BOOK &&
                                meta instanceof org.bukkit.inventory.meta.EnchantmentStorageMeta) {
                            org.bukkit.inventory.meta.EnchantmentStorageMeta bookMeta =
                                    (org.bukkit.inventory.meta.EnchantmentStorageMeta) meta;
                            if (bookMeta.hasStoredEnchants()) {
                                shouldSaveExact = true;
                            }
                        }

                        if (meta.hasDisplayName() || meta.hasLore()) {
                            shouldSaveExact = true;
                        }
                    }

                    if (shouldSaveExact) {
                        exactItems.add(item.clone());
                        hasExactItems = true;
                    } else {
                        exactItems.add(null);
                    }
                }
            }
            pattern.add(rowPattern.toString().trim());
        }

        String recipeKey = originalRecipe.getKey().replace("minecraft:", "");

        if (hasExactItems) {
            plugin.getVanillaRecipeManager().updateRecipeVariant(
                    recipeKey, currentVariantIndex, pattern, currentType, exactItems);
        } else {
            plugin.getVanillaRecipeManager().updateRecipeVariant(
                    recipeKey, currentVariantIndex, pattern, currentType);
        }
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player chatPlayer = event.getPlayer();
        VanillaRecipeEditorGUI editor = waitingForSearch.get(chatPlayer.getUniqueId());
        if (editor == null || !chatPlayer.equals(player)) {
            return;
        }

        event.setCancelled(true);
        String message = event.getMessage();
        if (message.equalsIgnoreCase("cancel")) {
            waitingForSearch.remove(chatPlayer.getUniqueId());
            Bukkit.getScheduler().runTask(plugin, () -> {
                MessageUtil.sendAdminWarning(chatPlayer, lang.getMessage("vanilla.search_cancelled"));
                editor.open();
            });
            return;
        }

        List<VanillaRecipeInfo> results = plugin.getVanillaRecipeManager().searchRecipes(message);
        waitingForSearch.remove(chatPlayer.getUniqueId());
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (results.isEmpty()) {
                MessageUtil.sendError(chatPlayer, lang.getMessage("vanilla.no_recipes_matching", Map.of("query", message)));
                editor.open();
            } else {
                new VanillaRecipeSearchResultsGUI(plugin, chatPlayer, results, message, parentGUI).open();
            }
        });
    }

    private void updateInventoryFast() {
        
        if (isFurnaceRecipe()) {
            inventory.setItem(12, null); 
        } else {
            for (int slot : GRID_SLOTS) {
                inventory.setItem(slot, null);
            }
        }
        if (maxVariants > 1) {
            addVariantSwitcher();
        }
        
        addToggleButton(52);
        addResultItem();
        addGridItems();
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().equals(inventory) && event.getPlayer().equals(player)) {
            if (!waitingForSearch.containsKey(player.getUniqueId()) && !awaitingSubGUI) {
                InventoryClickEvent.getHandlerList().unregister(this);
                InventoryCloseEvent.getHandlerList().unregister(this);
                AsyncPlayerChatEvent.getHandlerList().unregister(this);
            }
        }
    }
}