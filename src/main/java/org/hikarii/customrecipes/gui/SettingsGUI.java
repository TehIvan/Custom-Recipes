package org.hikarii.customrecipes.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.hikarii.customrecipes.CustomRecipes;
import org.hikarii.customrecipes.language.LanguageManager;
import org.hikarii.customrecipes.util.MessageUtil;

import java.util.ArrayList;
import java.util.List;

public class SettingsGUI implements Listener, GUIBase {
    private final CustomRecipes plugin;
    private final Player player;
    private final Inventory inventory;
    private final LanguageManager lang;

    private static final int PLUGIN_SETTINGS_SLOT = 10;
    private static final int RECIPE_MANAGEMENT_SLOT = 12;
    private static final int CRAFT_EVENTS_SLOT = 14;
    private static final int PERMISSIONS_SLOT = 16;
    private static final int BACK_SLOT = 22;

    public SettingsGUI(CustomRecipes plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.lang = plugin.getLanguageManager();
        this.inventory = Bukkit.createInventory(
                null,
                27,
                MessageUtil.createGradientMenuTitle(lang.getMessage("gui.title.settings"))
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
        addPluginSettingsButton();
        addRecipeManagementButton();
        addCraftEventsButton();
        addPermissionsButton();
        addBackButton();
    }

    private void fillBorders() {
        ItemStack borderPane = new ItemStack(Material.LIGHT_BLUE_STAINED_GLASS_PANE);
        ItemMeta meta = borderPane.getItemMeta();
        meta.displayName(Component.empty());
        borderPane.setItemMeta(meta);
        for (int i = 0; i < 27; i++) {
            inventory.setItem(i, borderPane);
        }
    }

    private void addPluginSettingsButton() {
        ItemStack button = new ItemStack(Material.COMPARATOR);
        ItemMeta meta = button.getItemMeta();
        meta.displayName(Component.text(lang.getMessage("settings.plugin_settings"), NamedTextColor.GOLD)
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text(lang.getMessage("settings.plugin_settings_desc_line1"), NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text(lang.getMessage("settings.plugin_settings_desc_line2"), NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(Component.text("  • " + lang.getMessage("settings.spawn_egg_name"), NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("  • " + lang.getMessage("settings.crafted_names"), NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("  • " + lang.getMessage("settings.ignore_data"), NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("  • " + lang.getMessage("settings.admin_notifications"), NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("  • " + lang.getMessage("button.language"), NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(Component.text("» " + lang.getMessage("lore.click_to_open"), NamedTextColor.AQUA)
                .decoration(TextDecoration.ITALIC, false));

        meta.lore(lore);
        button.setItemMeta(meta);
        inventory.setItem(PLUGIN_SETTINGS_SLOT, button);
    }

    private void addRecipeManagementButton() {
        int customTotal = plugin.getRecipeManager().getRecipeCount();
        int customEnabled = (int) plugin.getRecipeManager().getAllRecipes().stream()
                .filter(recipe -> plugin.getRecipeManager().isRecipeEnabled(recipe.getKey()))
                .count();
        int vanillaTotal = plugin.getVanillaRecipeManager().getAllVanillaRecipes().size();
        int vanillaDisabled = (int) plugin.getVanillaRecipeManager().getAllVanillaRecipes().keySet().stream()
                .filter(key -> plugin.getVanillaRecipeManager().isRecipeDisabled(key))
                .count();
        int vanillaEnabled = vanillaTotal - vanillaDisabled;

        ItemStack button = new ItemStack(Material.CRAFTING_TABLE);
        ItemMeta meta = button.getItemMeta();
        meta.displayName(Component.text(lang.getMessage("settings.recipe_management"), NamedTextColor.AQUA)
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text(lang.getMessage("settings.recipe_management_desc_line1"), NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text(lang.getMessage("settings.recipe_management_desc_line2"), NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(Component.text(lang.getMessage("settings.custom_recipes_status")
                        .replace("{enabled}", String.valueOf(customEnabled))
                        .replace("{total}", String.valueOf(customTotal)), NamedTextColor.GREEN)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text(lang.getMessage("settings.vanilla_recipes_status")
                        .replace("{enabled}", String.valueOf(vanillaEnabled))
                        .replace("{total}", String.valueOf(vanillaTotal)), NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(Component.text("» " + lang.getMessage("lore.click_to_open"), NamedTextColor.AQUA)
                .decoration(TextDecoration.ITALIC, false));

        meta.lore(lore);
        button.setItemMeta(meta);
        inventory.setItem(RECIPE_MANAGEMENT_SLOT, button);
    }

    private void addCraftEventsButton() {
        int presetCount = plugin.getCraftEventPresetManager().getPresetCount();

        ItemStack button = new ItemStack(Material.NOTE_BLOCK);
        ItemMeta meta = button.getItemMeta();
        meta.displayName(Component.text(lang.getMessage("settings.craft_events"), NamedTextColor.LIGHT_PURPLE)
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text(lang.getMessage("settings.craft_events_desc_line1"), NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text(lang.getMessage("settings.craft_events_desc_line2"), NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(Component.text(lang.getMessage("settings.craft_events_preset_count")
                        .replace("{count}", String.valueOf(presetCount)), NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(Component.text("» " + lang.getMessage("lore.click_to_open"), NamedTextColor.AQUA)
                .decoration(TextDecoration.ITALIC, false));

        meta.lore(lore);
        button.setItemMeta(meta);
        inventory.setItem(CRAFT_EVENTS_SLOT, button);
    }

    private void addPermissionsButton() {
        ItemStack button = new ItemStack(Material.IRON_BARS);
        ItemMeta meta = button.getItemMeta();
        meta.displayName(Component.text(lang.getMessage("settings.permissions"), NamedTextColor.RED)
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text(lang.getMessage("settings.permissions_desc_line1"), NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text(lang.getMessage("settings.permissions_desc_line2"), NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(Component.text("  • " + lang.getMessage("settings.perm_gui"), NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("  • " + lang.getMessage("settings.perm_list"), NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("  • " + lang.getMessage("settings.perm_help"), NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("  • " + lang.getMessage("settings.perm_list_all"), NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(Component.text("» " + lang.getMessage("lore.click_to_open"), NamedTextColor.AQUA)
                .decoration(TextDecoration.ITALIC, false));

        meta.lore(lore);
        button.setItemMeta(meta);
        inventory.setItem(PERMISSIONS_SLOT, button);
    }

    private void addBackButton() {
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta meta = back.getItemMeta();
        meta.displayName(Component.text(lang.getMessage("settings.back_to_main"), NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));
        back.setItemMeta(meta);
        inventory.setItem(BACK_SLOT, back);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getInventory().equals(inventory)) {
            return;
        }

        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player clicker)) {
            return;
        }

        if (!clicker.equals(player)) {
            return;
        }

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) {
            return;
        }

        int slot = event.getSlot();

        if (slot == BACK_SLOT) {
            new RecipeListGUI(plugin, player).open();
            return;
        }

        if (slot == PLUGIN_SETTINGS_SLOT) {
            new PluginSettingsGUI(plugin, player, this).open();
            return;
        }

        if (slot == RECIPE_MANAGEMENT_SLOT) {
            new RecipeManagementGUI(plugin, player, this).open();
            return;
        }

        if (slot == CRAFT_EVENTS_SLOT) {
            new CraftEventsMenuGUI(plugin, player, this).open();
            return;
        }

        if (slot == PERMISSIONS_SLOT) {
            new PermissionsSettingsGUI(plugin, player, this).open();
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().equals(inventory) && event.getPlayer().equals(player)) {
            
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!inventory.equals(player.getOpenInventory().getTopInventory())) {
                    InventoryClickEvent.getHandlerList().unregister(this);
                    InventoryCloseEvent.getHandlerList().unregister(this);
                }
            }, 1L);
        }
    }

    public void refreshAndOpen() {
        
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        updateInventory();
        player.openInventory(inventory);
    }
}
