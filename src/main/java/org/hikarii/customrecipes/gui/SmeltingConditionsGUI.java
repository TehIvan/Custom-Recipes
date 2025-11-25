package org.hikarii.customrecipes.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.hikarii.customrecipes.CustomRecipes;
import org.hikarii.customrecipes.language.LanguageManager;
import org.hikarii.customrecipes.recipe.CustomRecipe;
import org.hikarii.customrecipes.recipe.data.FurnaceRecipeData;
import org.hikarii.customrecipes.util.MessageUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SmeltingConditionsGUI implements Listener {
    private final CustomRecipes plugin;
    private final LanguageManager lang;
    private final Player player;
    private final CustomRecipe recipe;
    private final Inventory inventory;
    private final Runnable onReturn;

    private int cookTime;
    private float experience;
    private int burnTime;

    private static final int COOK_TIME_SLOT = 19;
    private static final int EXPERIENCE_SLOT = 22;
    private static final int BURN_TIME_SLOT = 25;
    private static final int SAVE_SLOT = 45;
    private static final int BACK_SLOT = 53;

    public SmeltingConditionsGUI(CustomRecipes plugin, Player player, CustomRecipe recipe, Runnable onReturn) {
        this.plugin = plugin;
        this.lang = plugin.getLanguageManager();
        this.player = player;
        this.recipe = recipe;
        this.onReturn = onReturn;

        FurnaceRecipeData furnaceData = recipe.getFurnaceData();
        if (furnaceData != null) {
            this.cookTime = furnaceData.getCookingTime();
            this.experience = furnaceData.getExperience();
            if (furnaceData.hasCustomFuels() && !furnaceData.getCustomFuels().isEmpty()) {
                this.burnTime = furnaceData.getCustomFuels().get(0).getBurnTime();
            } else {
                this.burnTime = 200; 
            }
        } else {
            this.cookTime = 200;
            this.experience = 0.1f;
            this.burnTime = 200;
        }

        this.inventory = Bukkit.createInventory(
                null,
                54,
                MessageUtil.createGradientMenuTitle(lang.getMessage("gui.title.smelting_conditions"))
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
        addCookTimeButton();
        addExperienceButton();
        addBurnTimeButton();
        addSaveButton();
        addBackButton();
    }

    private void fillBorders() {
        ItemStack borderPane = new ItemStack(Material.ORANGE_STAINED_GLASS_PANE);
        ItemMeta meta = borderPane.getItemMeta();
        meta.displayName(Component.empty());
        borderPane.setItemMeta(meta);
        for (int i = 0; i < 54; i++) {
            inventory.setItem(i, borderPane);
        }
    }

    private void addCookTimeButton() {
        ItemStack button = new ItemStack(Material.CLOCK);
        ItemMeta meta = button.getItemMeta();
        meta.displayName(Component.text(lang.getMessage("smelting_conditions.cook_time_title"), NamedTextColor.GOLD)
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text(lang.getMessage("smelting_conditions.cook_time_desc"), NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(Component.text(lang.getMessage("smelting_conditions.current_value",
                Map.of("value", String.valueOf(cookTime))), NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text(lang.getMessage("smelting_conditions.seconds",
                Map.of("value", String.format("%.1f", cookTime / 20.0))), NamedTextColor.AQUA)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(Component.text(lang.getMessage("smelting_conditions.left_click_add"), NamedTextColor.GREEN)
                .decoration(TextDecoration.ITALIC, true));
        lore.add(Component.text(lang.getMessage("smelting_conditions.right_click_remove"), NamedTextColor.RED)
                .decoration(TextDecoration.ITALIC, true));
        lore.add(Component.text(lang.getMessage("smelting_conditions.shift_click_10"), NamedTextColor.AQUA)
                .decoration(TextDecoration.ITALIC, true));

        meta.lore(lore);
        button.setItemMeta(meta);
        inventory.setItem(COOK_TIME_SLOT, button);
    }

    private void addExperienceButton() {
        ItemStack button = new ItemStack(Material.EXPERIENCE_BOTTLE);
        ItemMeta meta = button.getItemMeta();
        meta.displayName(Component.text(lang.getMessage("smelting_conditions.experience_title"), NamedTextColor.LIGHT_PURPLE)
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text(lang.getMessage("smelting_conditions.experience_desc"), NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(Component.text(lang.getMessage("smelting_conditions.current_experience",
                Map.of("value", String.format("%.1f", experience))), NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(Component.text(lang.getMessage("smelting_conditions.left_click_add"), NamedTextColor.GREEN)
                .decoration(TextDecoration.ITALIC, true));
        lore.add(Component.text(lang.getMessage("smelting_conditions.right_click_remove"), NamedTextColor.RED)
                .decoration(TextDecoration.ITALIC, true));
        lore.add(Component.text(lang.getMessage("smelting_conditions.shift_click_1"), NamedTextColor.AQUA)
                .decoration(TextDecoration.ITALIC, true));

        meta.lore(lore);
        button.setItemMeta(meta);
        inventory.setItem(EXPERIENCE_SLOT, button);
    }

    private void addBurnTimeButton() {
        ItemStack button = new ItemStack(Material.BLAZE_POWDER);
        ItemMeta meta = button.getItemMeta();
        meta.displayName(Component.text(lang.getMessage("smelting_conditions.burn_time_title"), NamedTextColor.RED)
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text(lang.getMessage("smelting_conditions.burn_time_desc"), NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(Component.text(lang.getMessage("smelting_conditions.current_value",
                Map.of("value", String.valueOf(burnTime))), NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text(lang.getMessage("smelting_conditions.seconds",
                Map.of("value", String.format("%.1f", burnTime / 20.0))), NamedTextColor.AQUA)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(Component.text(lang.getMessage("smelting_conditions.left_click_add"), NamedTextColor.GREEN)
                .decoration(TextDecoration.ITALIC, true));
        lore.add(Component.text(lang.getMessage("smelting_conditions.right_click_remove"), NamedTextColor.RED)
                .decoration(TextDecoration.ITALIC, true));
        lore.add(Component.text(lang.getMessage("smelting_conditions.shift_click_10"), NamedTextColor.AQUA)
                .decoration(TextDecoration.ITALIC, true));

        meta.lore(lore);
        button.setItemMeta(meta);
        inventory.setItem(BURN_TIME_SLOT, button);
    }

    private void addSaveButton() {
        ItemStack button = new ItemStack(Material.LIME_WOOL);
        ItemMeta meta = button.getItemMeta();
        meta.displayName(Component.text(lang.getMessage("smelting_conditions.save_title"), NamedTextColor.GREEN)
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text(lang.getMessage("smelting_conditions.save_desc"), NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(Component.text(lang.getMessage("lore.click_to_save"), NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, true));

        meta.lore(lore);
        button.setItemMeta(meta);
        inventory.setItem(SAVE_SLOT, button);
    }

    private void addBackButton() {
        ItemStack button = new ItemStack(Material.ARROW);
        ItemMeta meta = button.getItemMeta();
        meta.displayName(Component.text(lang.getMessage("button.back"), NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));
        button.setItemMeta(meta);
        inventory.setItem(BACK_SLOT, button);
    }

    private void saveSettings() {
        try {
            
            File recipeFile = plugin.getConfigManager().getRecipeFileManager().findRecipeFile(recipe.getKey());

            if (recipeFile == null) {
                
                recipeFile = plugin.getConfigManager().getRecipeFileManager()
                        .getRecipeFile(recipe.getKey(), recipe.getType());
            }

            if (recipeFile != null && recipeFile.exists()) {
                YamlConfiguration config = YamlConfiguration.loadConfiguration(recipeFile);

                config.set("cooking-time", cookTime);
                config.set("experience", experience);

                if (config.contains("custom-fuels")) {
                    ConfigurationSection fuelsSection = config.getConfigurationSection("custom-fuels");
                    if (fuelsSection != null) {
                        for (String fuelKey : fuelsSection.getKeys(false)) {
                            fuelsSection.set(fuelKey + ".burn-time", burnTime);
                        }
                    }
                }

                config.save(recipeFile);
                plugin.loadConfiguration();
                MessageUtil.sendAdminSuccess(player, lang.getMessage("smelting_conditions.saved"));
            } else {
                MessageUtil.sendError(player, lang.getMessage("smelting_conditions.file_not_found"));
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to save smelting conditions: " + e.getMessage());
            MessageUtil.sendError(player, lang.getMessage("smelting_conditions.save_error"));
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

        event.setCancelled(true);

        if (event.getClickedInventory() != inventory) {
            return;
        }

        int slot = event.getSlot();
        ClickType clickType = event.getClick();
        int amount = clickType.isShiftClick() ? 10 : 1;

        if (slot == COOK_TIME_SLOT) {
            if (clickType.isLeftClick()) {
                cookTime = Math.min(cookTime + amount, 6000); 
            } else if (clickType.isRightClick()) {
                cookTime = Math.max(cookTime - amount, 1);
            }
            updateInventory();
            return;
        }

        if (slot == EXPERIENCE_SLOT) {
            float expAmount = clickType.isShiftClick() ? 1.0f : 0.1f;
            if (clickType.isLeftClick()) {
                experience = Math.min(experience + expAmount, 10.0f); 
            } else if (clickType.isRightClick()) {
                experience = Math.max(experience - expAmount, 0.0f);
            }
            
            experience = Math.round(experience * 10) / 10.0f;
            updateInventory();
            return;
        }

        if (slot == BURN_TIME_SLOT) {
            if (clickType.isLeftClick()) {
                burnTime = Math.min(burnTime + amount, 32000); 
            } else if (clickType.isRightClick()) {
                burnTime = Math.max(burnTime - amount, 1);
            }
            updateInventory();
            return;
        }

        if (slot == SAVE_SLOT) {
            saveSettings();
            player.closeInventory();
            if (onReturn != null) {
                onReturn.run();
            }
            return;
        }

        if (slot == BACK_SLOT) {
            player.closeInventory();
            if (onReturn != null) {
                onReturn.run();
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().equals(inventory) && event.getPlayer().equals(player)) {
            InventoryClickEvent.getHandlerList().unregister(this);
            InventoryCloseEvent.getHandlerList().unregister(this);
        }
    }
}
