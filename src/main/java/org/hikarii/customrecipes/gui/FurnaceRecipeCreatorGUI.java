package org.hikarii.customrecipes.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
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
import org.bukkit.persistence.PersistentDataType;
import org.hikarii.customrecipes.CustomRecipes;
import org.hikarii.customrecipes.config.JsonRecipeFileManager;
import org.hikarii.customrecipes.language.LanguageManager;
import org.hikarii.customrecipes.recipe.CraftEventPreset;
import org.hikarii.customrecipes.recipe.CraftEvents;
import org.hikarii.customrecipes.recipe.CustomRecipe;
import org.hikarii.customrecipes.recipe.RecipeConditions;
import org.hikarii.customrecipes.recipe.RecipeType;
import org.hikarii.customrecipes.recipe.data.FurnaceRecipeData;
import org.hikarii.customrecipes.recipe.data.RandomResult;
import org.hikarii.customrecipes.recipe.data.RandomResultPool;
import org.hikarii.customrecipes.util.ItemStackSerializer;
import org.hikarii.customrecipes.util.MessageUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FurnaceRecipeCreatorGUI implements Listener {
    private final CustomRecipes plugin;
    private final Player player;
    private final LanguageManager lang;
    private final Inventory inventory;

    private ItemStack inputItem = null;
    private ItemStack fuelItem = null;
    private ItemStack resultItem = null;
    private int cookingTime = FurnaceRecipeData.DEFAULT_FURNACE_COOKING_TIME;
    private float experience = 0.0f;
    private int burnTime = 200; 
    private String group = "";
    private RecipeConditions conditions = new RecipeConditions();
    private RandomResultPool randomResults = null;

    private static final int INPUT_SLOT = 12;      
    private static final int PLUS_SLOT = 21;       
    private static final int FUEL_SLOT = 30;       
    private static final int EQUALS_SLOT = 23;     
    private static final int RESULT_SLOT = 25;     

    private static final int SMELTING_CONDITIONS_BUTTON = 45;  
    private static final int CONDITIONS_BUTTON = 46;           
    private static final int SAVE_BUTTON = 48;                 
    private static final int CANCEL_BUTTON = 50;               
    private static final int RANDOM_RESULTS_BUTTON = 52;       
    private static final int EDIT_RESULT_BUTTON = 53;          

    public FurnaceRecipeCreatorGUI(CustomRecipes plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.lang = plugin.getLanguageManager();
        this.inventory = Bukkit.createInventory(
                null,
                54,
                MessageUtil.createGradientMenuTitle(lang.getMessage("furnace_creator.title"))
        );
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        updateInventory();
    }

    public void open() {
        player.openInventory(inventory);
    }

    private void updateInventory() {
        inventory.clear();
        fillBackground();
        addInputSlot();
        addPlusSymbol();
        addFuelSlot();
        addEqualsSymbol();
        addResultSlot();
        addSmeltingConditionsButton();
        addConditionsButton();
        addSaveButton();
        addCancelButton();
        addRandomResultsButton();
        addEditResultButton();
    }

    private void fillBackground() {
        ItemStack borderPane = new ItemStack(Material.LIGHT_BLUE_STAINED_GLASS_PANE);
        ItemMeta meta = borderPane.getItemMeta();
        meta.displayName(Component.empty());
        borderPane.setItemMeta(meta);

        for (int i = 0; i < 54; i++) {
            inventory.setItem(i, borderPane);
        }

        inventory.setItem(INPUT_SLOT, null);
        inventory.setItem(FUEL_SLOT, null);
        inventory.setItem(RESULT_SLOT, null);
    }

    private void addInputSlot() {
        if (inputItem != null && inputItem.getType() != Material.AIR) {
            ItemStack display = inputItem.clone();
            ItemMeta meta = display.getItemMeta();
            if (meta != null) {
                List<Component> lore = meta.hasLore() && meta.lore() != null ?
                        new ArrayList<>(meta.lore()) : new ArrayList<>();
                lore.add(Component.empty());
                lore.add(Component.text(lang.getMessage("furnace_creator.input_hint"), NamedTextColor.YELLOW)
                        .decoration(TextDecoration.ITALIC, false));
                lore.add(Component.empty());
                lore.add(Component.text(lang.getMessage("lore.click_to_remove"), NamedTextColor.RED)
                        .decoration(TextDecoration.ITALIC, true));
                meta.lore(lore);
                display.setItemMeta(meta);
            }
            inventory.setItem(INPUT_SLOT, display);
        }
        
    }

    private void addPlusSymbol() {
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
        inventory.setItem(PLUS_SLOT, plus);
    }

    private void addFuelSlot() {
        if (fuelItem != null && fuelItem.getType() != Material.AIR) {
            ItemStack display = fuelItem.clone();
            ItemMeta meta = display.getItemMeta();
            if (meta != null) {
                List<Component> lore = meta.hasLore() && meta.lore() != null ?
                        new ArrayList<>(meta.lore()) : new ArrayList<>();
                lore.add(Component.empty());
                lore.add(Component.text(lang.getMessage("furnace_creator.fuel_hint"), NamedTextColor.YELLOW)
                        .decoration(TextDecoration.ITALIC, false));
                lore.add(Component.empty());
                lore.add(Component.text(lang.getMessage("lore.click_to_remove"), NamedTextColor.RED)
                        .decoration(TextDecoration.ITALIC, true));
                meta.lore(lore);
                display.setItemMeta(meta);
            }
            inventory.setItem(FUEL_SLOT, display);
        }
        
    }

    private void addEqualsSymbol() {
        ItemStack equals = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
        ItemMeta meta = equals.getItemMeta();
        meta.displayName(Component.text("=", NamedTextColor.GREEN)
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
                Component.empty(),
                Component.text(lang.getMessage("furnace_creator.equals_desc"), NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false)
        ));
        equals.setItemMeta(meta);
        inventory.setItem(EQUALS_SLOT, equals);
    }

    private void addResultSlot() {
        if (resultItem != null && resultItem.getType() != Material.AIR) {
            ItemStack display = resultItem.clone();
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
            inventory.setItem(RESULT_SLOT, display);
        }
        
    }

    private void addCancelButton() {
        ItemStack cancel = new ItemStack(Material.RED_WOOL);
        ItemMeta meta = cancel.getItemMeta();
        meta.displayName(Component.text(lang.getMessage("button.cancel"), NamedTextColor.RED)
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
                Component.empty(),
                Component.text(lang.getMessage("creator.button.cancel_desc"), NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false),
                Component.empty(),
                Component.text(lang.getMessage("creator.button.cancel_action"), NamedTextColor.YELLOW)
                        .decoration(TextDecoration.ITALIC, true)
        ));
        cancel.setItemMeta(meta);
        inventory.setItem(CANCEL_BUTTON, cancel);
    }

    private void addEditResultButton() {
        ItemStack button = new ItemStack(Material.ANVIL);
        ItemMeta meta = button.getItemMeta();
        meta.displayName(Component.text(lang.getMessage("creator.button.edit_result_title"), NamedTextColor.LIGHT_PURPLE)
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text(lang.getMessage("creator.button.edit_result_desc1"), NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text(lang.getMessage("creator.button.edit_result_desc2"), NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(Component.text(lang.getMessage("creator.button.edit_result_action"), NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, true));
        meta.lore(lore);
        button.setItemMeta(meta);
        inventory.setItem(EDIT_RESULT_BUTTON, button);
    }

    private void addRandomResultsButton() {
        ItemStack button = new ItemStack(Material.HOPPER);
        ItemMeta meta = button.getItemMeta();
        meta.displayName(Component.text(lang.getMessage("random_results.title"), NamedTextColor.DARK_PURPLE)
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
            lore.add(Component.text(lang.getMessage("info.status"), NamedTextColor.YELLOW)
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("  " + lang.getMessage("random_results.total_results",
                    Map.of("count", String.valueOf(randomResults.getResults().size()))), NamedTextColor.AQUA)
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
        inventory.setItem(RANDOM_RESULTS_BUTTON, button);
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

        lore.add(Component.text(lang.getMessage("editor.button.smelting_conditions_current"), NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));

        lore.add(Component.text(lang.getMessage("editor.button.smelting_conditions_cook_time",
                Map.of("value", String.valueOf(cookingTime))), NamedTextColor.AQUA)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text(lang.getMessage("editor.button.smelting_conditions_experience",
                Map.of("value", String.format("%.1f", experience))), NamedTextColor.AQUA)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text(lang.getMessage("editor.button.smelting_conditions_burn_time",
                Map.of("value", String.valueOf(burnTime))), NamedTextColor.AQUA)
                .decoration(TextDecoration.ITALIC, false));

        lore.add(Component.empty());
        lore.add(Component.text(lang.getMessage("editor.button.smelting_conditions_action"), NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, true));

        meta.lore(lore);
        button.setItemMeta(meta);
        inventory.setItem(SMELTING_CONDITIONS_BUTTON, button);
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

        lore.add(Component.text(lang.getMessage("editor.button.conditions_current"), NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));

        if (conditions != null && conditions.hasAnyCondition()) {
            if (conditions.hasPermission()) {
                lore.add(Component.text(lang.getMessage("editor.button.conditions_permission",
                        Map.of("value", conditions.getPermission())), NamedTextColor.AQUA)
                        .decoration(TextDecoration.ITALIC, false));
            }
            if (conditions.hasXpLevelRequirement()) {
                lore.add(Component.text(lang.getMessage("editor.button.conditions_xp_level",
                        Map.of("value", String.valueOf(conditions.getRequiredXpLevel()))), NamedTextColor.AQUA)
                        .decoration(TextDecoration.ITALIC, false));
            }
            if (conditions.hasXpReward()) {
                String reward = (conditions.getXpReward() > 0 ? "+" : "") + conditions.getXpReward();
                lore.add(Component.text(lang.getMessage("editor.button.conditions_xp_reward",
                        Map.of("value", reward)), NamedTextColor.AQUA)
                        .decoration(TextDecoration.ITALIC, false));
            }
            if (conditions.hasCooldown()) {
                lore.add(Component.text(lang.getMessage("editor.button.conditions_cooldown",
                        Map.of("value", org.hikarii.customrecipes.recipe.CraftTracker.formatTime(conditions.getCooldownSeconds()))), NamedTextColor.AQUA)
                        .decoration(TextDecoration.ITALIC, false));
            }
            if (conditions.hasDailyLimit()) {
                lore.add(Component.text(lang.getMessage("editor.button.conditions_daily_limit",
                        Map.of("value", String.valueOf(conditions.getCraftLimitDaily()))), NamedTextColor.AQUA)
                        .decoration(TextDecoration.ITALIC, false));
            }
            if (conditions.hasWeeklyLimit()) {
                lore.add(Component.text(lang.getMessage("editor.button.conditions_weekly_limit",
                        Map.of("value", String.valueOf(conditions.getCraftLimitWeekly()))), NamedTextColor.AQUA)
                        .decoration(TextDecoration.ITALIC, false));
            }
            if (conditions.hasTotalLimit()) {
                lore.add(Component.text(lang.getMessage("editor.button.conditions_total_limit",
                        Map.of("value", String.valueOf(conditions.getCraftLimitTotal()))), NamedTextColor.AQUA)
                        .decoration(TextDecoration.ITALIC, false));
            }
            if (conditions.hasMoneyCost()) {
                lore.add(Component.text(lang.getMessage("editor.button.conditions_money",
                        Map.of("value", String.valueOf(conditions.getMoneyCost()))), NamedTextColor.AQUA)
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
        inventory.setItem(CONDITIONS_BUTTON, button);
    }

    private void addSaveButton() {
        ItemStack save = new ItemStack(Material.LIME_WOOL);
        ItemMeta meta = save.getItemMeta();
        meta.displayName(Component.text(lang.getMessage("creator.button.create_title"), NamedTextColor.GREEN)
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
                Component.empty(),
                Component.text(lang.getMessage("furnace_creator.create_desc"), NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false),
                Component.empty(),
                Component.text(lang.getMessage("creator.button.create_action"), NamedTextColor.YELLOW)
                        .decoration(TextDecoration.ITALIC, true)
        ));
        save.setItemMeta(meta);
        inventory.setItem(SAVE_BUTTON, save);
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

        if (slot == INPUT_SLOT) {
            if (event.getClickedInventory() != inventory) return;
            event.setCancelled(true);

            if (inputItem != null && inputItem.getType() != Material.AIR) {
                inputItem = null;
            } else {
                
                ItemStack cursor = event.getCursor();
                if (cursor != null && cursor.getType() != Material.AIR) {
                    inputItem = cursor.clone();
                    inputItem.setAmount(1);
                }
            }
            updateInventory();
            return;
        }

        if (slot == FUEL_SLOT) {
            if (event.getClickedInventory() != inventory) return;
            event.setCancelled(true);

            if (fuelItem != null && fuelItem.getType() != Material.AIR) {
                fuelItem = null;
            } else {
                
                ItemStack cursor = event.getCursor();
                if (cursor != null && cursor.getType() != Material.AIR) {
                    fuelItem = cursor.clone();
                    fuelItem.setAmount(1);
                }
            }
            updateInventory();
            return;
        }

        if (slot == RESULT_SLOT) {
            if (event.getClickedInventory() != inventory) return;
            event.setCancelled(true);

            if (clickType.isLeftClick()) {
                ItemStack cursor = event.getCursor();
                if (cursor != null && cursor.getType() != Material.AIR) {
                    if (resultItem == null || resultItem.getType() == Material.AIR) {
                        resultItem = cursor.clone();
                        resultItem.setAmount(1);
                    } else if (resultItem.isSimilar(cursor)) {
                        int newAmount = Math.min(resultItem.getAmount() + 1, cursor.getType().getMaxStackSize());
                        resultItem.setAmount(newAmount);
                    } else {
                        resultItem = cursor.clone();
                        resultItem.setAmount(1);
                    }
                } else if (resultItem != null && resultItem.getType() != Material.AIR) {
                    int newAmount = Math.min(resultItem.getAmount() + 1, resultItem.getType().getMaxStackSize());
                    resultItem.setAmount(newAmount);
                }
            } else if (clickType.isRightClick()) {
                if (resultItem != null && resultItem.getType() != Material.AIR) {
                    if (resultItem.getAmount() > 1) {
                        resultItem.setAmount(resultItem.getAmount() - 1);
                    } else {
                        resultItem = null;
                    }
                }
            }
            updateInventory();
            return;
        }

        if (event.getClickedInventory() != inventory) return;
        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        if (slot == CANCEL_BUTTON) {
            new StationSelectorGUI(plugin, player).open();
            return;
        }

        if (slot == SMELTING_CONDITIONS_BUTTON) {
            openSmeltingConditionsEditor();
            return;
        }

        if (slot == CONDITIONS_BUTTON) {
            openConditionsEditor();
            return;
        }

        if (slot == SAVE_BUTTON) {
            handleCreateRecipe();
            return;
        }

        if (slot == RANDOM_RESULTS_BUTTON) {
            if (resultItem == null || resultItem.getType() == Material.AIR) {
                MessageUtil.sendError(player, lang.getMessage("creator.error.no_result_first"));
                return;
            }

            ItemStack tempInput = inputItem != null ? inputItem.clone() : null;
            ItemStack tempFuel = fuelItem != null ? fuelItem.clone() : null;
            ItemStack tempResult = resultItem != null ? resultItem.clone() : null;
            int tempCookingTime = cookingTime;
            float tempExperience = experience;
            int tempBurnTime = burnTime;
            RecipeConditions tempConditions = conditions;

            RandomResultPool tempRandomResults = randomResults;
            RandomResultsGUI randomResultsGUI = new RandomResultsGUI(plugin, player, randomResults, resultItem, pool -> {
                FurnaceRecipeCreatorGUI newGUI = new FurnaceRecipeCreatorGUI(plugin, player);
                newGUI.inputItem = tempInput;
                newGUI.fuelItem = tempFuel;
                newGUI.resultItem = tempResult;
                newGUI.cookingTime = tempCookingTime;
                newGUI.experience = tempExperience;
                newGUI.burnTime = tempBurnTime;
                newGUI.conditions = tempConditions;

                if (pool != null && pool.isCancelled()) {
                    newGUI.randomResults = tempRandomResults;
                } else if (pool != null && pool.isEmptySaved()) {
                    newGUI.randomResults = null;
                } else {
                    newGUI.randomResults = pool;
                }

                newGUI.updateInventory();
                newGUI.open();
            });
            randomResultsGUI.open();
            return;
        }

        if (slot == EDIT_RESULT_BUTTON) {
            if (resultItem == null || resultItem.getType() == Material.AIR) {
                MessageUtil.sendError(player, lang.getMessage("creator.error.no_result_first"));
                return;
            }
            openResultEditor();
            return;
        }
    }

    private void openResultEditor() {
        ItemStack tempInput = inputItem != null ? inputItem.clone() : null;
        ItemStack tempFuel = fuelItem != null ? fuelItem.clone() : null;
        ItemStack tempResult = resultItem.clone();
        int tempCookingTime = cookingTime;
        float tempExperience = experience;
        int tempBurnTime = burnTime;
        RecipeConditions tempConditions = conditions;
        RandomResultPool tempRandomResults = randomResults;

        if (randomResults != null && randomResults.hasRandomResults() && randomResults.size() > 1) {
            List<ItemStack> variants = new ArrayList<>();
            for (org.hikarii.customrecipes.recipe.data.RandomResult rr : randomResults.getResults()) {
                variants.add(rr.getItem().clone());
            }

            new ItemEditorGUI(plugin, player, variants, 0, (editedVariants) -> {
                FurnaceRecipeCreatorGUI newGUI = new FurnaceRecipeCreatorGUI(plugin, player);
                newGUI.inputItem = tempInput;
                newGUI.fuelItem = tempFuel;
                newGUI.cookingTime = tempCookingTime;
                newGUI.experience = tempExperience;
                newGUI.burnTime = tempBurnTime;
                newGUI.conditions = tempConditions;

                if (editedVariants != null && !editedVariants.isEmpty()) {
                    
                    newGUI.resultItem = editedVariants.get(0).clone();

                    List<org.hikarii.customrecipes.recipe.data.RandomResult> originalResults =
                            tempRandomResults.getResults();
                    List<org.hikarii.customrecipes.recipe.data.RandomResult> newResults = new ArrayList<>();

                    for (int i = 0; i < editedVariants.size() && i < originalResults.size(); i++) {
                        org.hikarii.customrecipes.recipe.data.RandomResult original = originalResults.get(i);
                        newResults.add(new org.hikarii.customrecipes.recipe.data.RandomResult(
                                editedVariants.get(i), original.getWeight(), original.getName()));
                    }
                    newGUI.randomResults = new RandomResultPool(newResults, tempRandomResults.isShowChances(),
                            tempRandomResults.getFailureChance());
                } else {
                    newGUI.resultItem = tempResult.clone();
                    newGUI.randomResults = tempRandomResults;
                }
                newGUI.updateInventory();
                newGUI.open();
            }).open();
        } else {
            
            new ItemEditorGUI(plugin, player, tempResult, (editedItem) -> {
                FurnaceRecipeCreatorGUI newGUI = new FurnaceRecipeCreatorGUI(plugin, player);
                newGUI.inputItem = tempInput;
                newGUI.fuelItem = tempFuel;
                newGUI.cookingTime = tempCookingTime;
                newGUI.experience = tempExperience;
                newGUI.burnTime = tempBurnTime;
                newGUI.conditions = tempConditions;
                newGUI.randomResults = tempRandomResults;

                if (editedItem != null) {
                    newGUI.resultItem = editedItem.clone();
                } else {
                    newGUI.resultItem = tempResult.clone();
                }
                newGUI.updateInventory();
                newGUI.open();
            }).open();
        }
    }

    private void openSmeltingConditionsEditor() {
        
        ItemStack tempInput = inputItem != null ? inputItem.clone() : null;
        ItemStack tempFuel = fuelItem != null ? fuelItem.clone() : null;
        ItemStack tempResult = resultItem != null ? resultItem.clone() : null;
        int tempCookingTime = cookingTime;
        float tempExperience = experience;
        int tempBurnTime = burnTime;
        RecipeConditions tempConditions = conditions;
        RandomResultPool tempRandomResults = randomResults;

        new FurnaceConditionsGUI(plugin, player, cookingTime, experience, burnTime, (newCookingTime, newExperience, newBurnTime) -> {
            FurnaceRecipeCreatorGUI newGUI = new FurnaceRecipeCreatorGUI(plugin, player);
            newGUI.inputItem = tempInput;
            newGUI.fuelItem = tempFuel;
            newGUI.resultItem = tempResult;
            newGUI.cookingTime = newCookingTime;
            newGUI.experience = newExperience;
            newGUI.burnTime = newBurnTime;
            newGUI.conditions = tempConditions;
            newGUI.randomResults = tempRandomResults;
            newGUI.updateInventory();
            newGUI.open();
        }).open();
    }

    private void openConditionsEditor() {
        
        ItemStack tempInput = inputItem != null ? inputItem.clone() : null;
        ItemStack tempFuel = fuelItem != null ? fuelItem.clone() : null;
        ItemStack tempResult = resultItem != null ? resultItem.clone() : null;
        int tempCookingTime = cookingTime;
        float tempExperience = experience;
        int tempBurnTime = burnTime;
        RandomResultPool tempRandomResults = randomResults;

        new ConditionsEditorGUI(plugin, player, conditions, (newConditions) -> {
            FurnaceRecipeCreatorGUI newGUI = new FurnaceRecipeCreatorGUI(plugin, player);
            newGUI.inputItem = tempInput;
            newGUI.fuelItem = tempFuel;
            newGUI.resultItem = tempResult;
            newGUI.cookingTime = tempCookingTime;
            newGUI.experience = tempExperience;
            newGUI.burnTime = tempBurnTime;
            newGUI.conditions = newConditions;
            newGUI.randomResults = tempRandomResults;
            newGUI.updateInventory();
            newGUI.open();
        }).open();
    }

    private void handleCreateRecipe() {
        if (inputItem == null || inputItem.getType() == Material.AIR) {
            MessageUtil.sendError(player, lang.getMessage("furnace_creator.error.no_input"));
            return;
        }

        if (resultItem == null || resultItem.getType() == Material.AIR) {
            MessageUtil.sendError(player, lang.getMessage("creator.error.no_result"));
            return;
        }

        String recipeKey = generateRecipeKey(resultItem.getType());

        try {
            FurnaceRecipeData.Builder builder = FurnaceRecipeData.builder()
                    .input(inputItem.getType())
                    .exactInput(inputItem.hasItemMeta() ? inputItem : null)
                    .cookingTime(cookingTime)
                    .experience(experience)
                    .group(group);

            if (fuelItem != null && fuelItem.getType() != Material.AIR) {
                FurnaceRecipeData.CustomFuel customFuel = new FurnaceRecipeData.CustomFuel(
                        new org.hikarii.customrecipes.recipe.data.RecipeIngredient(fuelItem.getType()),
                        fuelItem.hasItemMeta() ? fuelItem : null,
                        200 
                );
                builder.addCustomFuel(customFuel);
            }

            FurnaceRecipeData furnaceData = builder.build();

            org.hikarii.customrecipes.recipe.CraftEvents craftEvents = null;
            if (resultItem.hasItemMeta()) {
                ItemMeta resultMeta = resultItem.getItemMeta();
                NamespacedKey presetKey = new NamespacedKey(plugin, "craft_event_preset");
                String presetName = resultMeta.getPersistentDataContainer().get(presetKey, PersistentDataType.STRING);
                if (presetName != null && !presetName.isEmpty()) {
                    org.hikarii.customrecipes.recipe.CraftEventPreset preset =
                        plugin.getCraftEventPresetManager().getPreset(presetName);
                    if (preset != null) {
                        craftEvents = preset.toCraftEvents();
                        plugin.getLogger().info("[DEBUG] Loaded craft events from preset for furnace: " + presetName);
                    }
                }
            }

            CustomRecipe newRecipe = new CustomRecipe(
                    recipeKey,
                    null,
                    new ArrayList<>(),
                    RecipeType.FURNACE,
                    null,
                    null,
                    furnaceData,
                    resultItem.clone(),
                    false,
                    new ArrayList<>(),
                    conditions,
                    randomResults,
                    craftEvents
            );

            saveRecipeToConfig(recipeKey, furnaceData, resultItem);

            List<String> enabledRecipes = plugin.getConfig().getStringList("enabled-recipes");
            if (!enabledRecipes.contains(recipeKey)) {
                enabledRecipes.add(recipeKey);
                plugin.getConfig().set("enabled-recipes", enabledRecipes);
                plugin.saveConfig();
                plugin.reloadConfig();
            }

            plugin.getRecipeManager().registerSingleRecipe(newRecipe);
            MessageUtil.sendAdminSuccess(player, lang.getMessage("creator.success.created", Map.of("recipe", recipeKey)));
            player.closeInventory();
            new RecipeListGUI(plugin, player).open();
        } catch (Exception e) {
            MessageUtil.sendError(player, lang.getMessage("creator.error.create_failed", Map.of("error", e.getMessage())));
            plugin.getLogger().warning("Furnace recipe creation failed: " + e.getMessage());
            if (plugin.isDebugMode()) {
                e.printStackTrace();
            }
        }
    }

    private void saveRecipeToConfig(String recipeKey, FurnaceRecipeData furnaceData, ItemStack result) {
        try {
            Map<String, Object> recipeData = new HashMap<>();
            recipeData.put("type", RecipeType.FURNACE.name());
            recipeData.put("input", furnaceData.getInput().material().name());
            recipeData.put("cooking-time", furnaceData.getCookingTime());
            recipeData.put("experience", furnaceData.getExperience());

            if (!furnaceData.getGroup().isEmpty()) {
                recipeData.put("group", furnaceData.getGroup());
            }

            if (inputItem != null && inputItem.hasItemMeta()) {
                ItemMeta meta = inputItem.getItemMeta();
                
                boolean hasCustomProperties = meta.hasEnchants() || meta.hasDisplayName() || meta.hasLore() ||
                        !meta.getPersistentDataContainer().getKeys().isEmpty() ||
                        meta.hasCustomModelData() || meta.isUnbreakable() ||
                        !meta.getItemFlags().isEmpty() || meta.hasAttributeModifiers();

                if (meta instanceof org.bukkit.inventory.meta.Damageable damageable) {
                    if (damageable.hasDamage()) {
                        hasCustomProperties = true;
                    }
                }

                if (hasCustomProperties) {
                    recipeData.put("input-full", ItemStackSerializer.toMap(inputItem));
                }
            }

            if (fuelItem != null && fuelItem.getType() != Material.AIR) {
                Map<String, Object> fuelsMap = new HashMap<>();
                Map<String, Object> fuelData = new HashMap<>();
                fuelData.put("material", fuelItem.getType().name());
                fuelData.put("burn-time", 200);
                if (fuelItem.hasItemMeta()) {
                    fuelData.put("item-full", ItemStackSerializer.toMap(fuelItem));
                }
                fuelsMap.put("custom_fuel_1", fuelData);
                recipeData.put("custom-fuels", fuelsMap);
            }

            Map<String, Object> resultData = ItemStackSerializer.toMap(result);
            recipeData.put("result", resultData);
            recipeData.put("material", result.getType().name());
            recipeData.put("amount", result.getAmount());

            if (result.hasItemMeta()) {
                ItemMeta resultMeta = result.getItemMeta();
                if (resultMeta.hasDisplayName() || resultMeta.hasLore() || resultMeta.hasEnchants() ||
                        !resultMeta.getPersistentDataContainer().getKeys().isEmpty()) {
                    recipeData.put("result-full", ItemStackSerializer.toMap(result));
                }

                NamespacedKey presetKey = new NamespacedKey(plugin, "craft_event_preset");
                String presetName = resultMeta.getPersistentDataContainer().get(presetKey, PersistentDataType.STRING);
                if (presetName != null && !presetName.isEmpty()) {
                    recipeData.put("craft_events_preset", presetName);
                }

                if (resultMeta.hasDisplayName()) {
                    String name = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                            .serialize(resultMeta.displayName());
                    if (name != null && !name.isEmpty()) {
                        recipeData.put("name", name);
                    }
                }
                if (resultMeta.hasLore() && resultMeta.lore() != null) {
                    List<String> loreStrings = new ArrayList<>();
                    for (Component line : resultMeta.lore()) {
                        String loreText = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                                .serialize(line);
                        if (loreText != null && !loreText.isEmpty()) {
                            loreStrings.add(loreText);
                        }
                    }
                    if (!loreStrings.isEmpty()) {
                        recipeData.put("description", loreStrings);
                    }
                }
            }

            recipeData.put("hidden", false);

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
                for (RandomResult randomResult : randomResults.getResults()) {
                    Map<String, Object> itemData = ItemStackSerializer.toMap(randomResult.getItem());
                    itemData.put("chance", randomResult.getWeight());
                    if (randomResult.hasCustomName()) {
                        itemData.put("name", randomResult.getName());
                    }
                    items.add(itemData);
                }
                randomResultsData.put("items", items);
                recipeData.put("random-results", randomResultsData);
            }

            plugin.getConfigManager().getRecipeFileManager().saveRecipe(recipeKey, recipeData, RecipeType.FURNACE);

            JsonRecipeFileManager jsonManager = new JsonRecipeFileManager(plugin);
            jsonManager.saveRecipeJson(recipeKey, recipeData, RecipeType.FURNACE);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save furnace recipe: " + e.getMessage());
            MessageUtil.sendError(player, lang.getMessage("creator.error.save_failed"));
        }
    }

    private String generateRecipeKey(Material material) {
        String baseName = formatMaterialName(material.name());
        String key = baseName;
        int counter = 1;
        while (plugin.getRecipeManager().getRecipe(key) != null) {
            key = baseName + counter;
            counter++;
        }
        return key;
    }

    private String formatMaterialName(String materialName) {
        String[] parts = materialName.toLowerCase().split("_");
        StringBuilder result = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) {
                result.append(Character.toUpperCase(part.charAt(0)))
                        .append(part.substring(1));
            }
        }
        return result.toString();
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().equals(inventory) && event.getPlayer().equals(player)) {
            InventoryClickEvent.getHandlerList().unregister(this);
            InventoryCloseEvent.getHandlerList().unregister(this);
            InventoryDragEvent.getHandlerList().unregister(this);
        }
    }
}
