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

public class PluginSettingsGUI implements Listener, GUIBase {
    private final CustomRecipes plugin;
    private final Player player;
    private final Inventory inventory;
    private final LanguageManager lang;
    private final SettingsGUI parentGUI;

    private static final int SPAWN_EGG_SLOT = 11;
    private static final int CRAFTED_NAMES_SLOT = 13;
    private static final int IGNORE_DATA_SLOT = 15;
    private static final int ADMIN_NOTIFICATIONS_SLOT = 29;
    private static final int LANGUAGE_SLOT = 31;
    private static final int BACK_SLOT = 53;

    public PluginSettingsGUI(CustomRecipes plugin, Player player, SettingsGUI parentGUI) {
        this.plugin = plugin;
        this.player = player;
        this.parentGUI = parentGUI;
        this.lang = plugin.getLanguageManager();
        this.inventory = Bukkit.createInventory(
                null,
                54,
                MessageUtil.createGradientMenuTitle(lang.getMessage("settings.plugin_settings_title"))
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
        addSpawnEggNameSetting();
        addCraftedNamesSetting();
        addIgnoreDataSetting();
        addAdminNotificationsSetting();
        addLanguageButton();
        addBackButton();
    }

    private void fillBorders() {
        ItemStack borderPane = new ItemStack(Material.LIGHT_BLUE_STAINED_GLASS_PANE);
        ItemMeta meta = borderPane.getItemMeta();
        meta.displayName(Component.empty());
        borderPane.setItemMeta(meta);
        for (int i = 0; i < 54; i++) {
            inventory.setItem(i, borderPane);
        }
    }

    private void addSpawnEggNameSetting() {
        boolean enabled = plugin.isKeepSpawnEggNames();
        Material material = enabled ? Material.LIME_DYE : Material.GRAY_DYE;
        String status = enabled ? lang.getMessage("lore.status_enabled") : lang.getMessage("lore.status_disabled");
        NamedTextColor color = enabled ? NamedTextColor.GREEN : NamedTextColor.RED;

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(lang.getMessage("settings.spawn_egg_name"), NamedTextColor.GOLD)
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text(lang.getMessage("info.status") + " ", NamedTextColor.GRAY)
                .append(Component.text(status, color))
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(Component.text(lang.getMessage("settings.spawn_egg_desc_line1"), NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text(lang.getMessage("settings.spawn_egg_desc_line2"), NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text(lang.getMessage("settings.spawn_egg_desc_line3"), NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(Component.text("» " + lang.getMessage("lore.click_to_toggle"), NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, true));
        meta.lore(lore);
        item.setItemMeta(meta);
        inventory.setItem(SPAWN_EGG_SLOT, item);
    }

    private void addCraftedNamesSetting() {
        boolean enabled = plugin.isUseCraftedCustomNames();
        Material material = enabled ? Material.LIME_DYE : Material.GRAY_DYE;
        String status = enabled ? lang.getMessage("lore.status_enabled") : lang.getMessage("lore.status_disabled");
        NamedTextColor color = enabled ? NamedTextColor.GREEN : NamedTextColor.RED;

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(lang.getMessage("settings.crafted_names"), NamedTextColor.GOLD)
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text(lang.getMessage("info.status") + " ", NamedTextColor.GRAY)
                .append(Component.text(status, color))
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(Component.text(lang.getMessage("settings.crafted_names_desc_line1"), NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text(lang.getMessage("settings.crafted_names_desc_line2"), NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text(lang.getMessage("settings.crafted_names_desc_line3"), NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(Component.text("» " + lang.getMessage("lore.click_to_toggle"), NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, true));
        meta.lore(lore);
        item.setItemMeta(meta);
        inventory.setItem(CRAFTED_NAMES_SLOT, item);
    }

    private void addIgnoreDataSetting() {
        boolean enabled = plugin.getConfig().getBoolean("ignore-metadata", false);
        Material material = enabled ? Material.LIME_DYE : Material.GRAY_DYE;
        String status = enabled ? lang.getMessage("lore.status_enabled") : lang.getMessage("lore.status_disabled");
        NamedTextColor color = enabled ? NamedTextColor.GREEN : NamedTextColor.RED;

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(lang.getMessage("settings.ignore_data"), NamedTextColor.LIGHT_PURPLE)
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text(lang.getMessage("info.status") + " ", NamedTextColor.GRAY)
                .append(Component.text(status, color))
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(Component.text(lang.getMessage("settings.ignore_data_desc_line1"), NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text(lang.getMessage("settings.ignore_data_desc_line2"), NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text(lang.getMessage("settings.ignore_data_desc_line3"), NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(Component.text("» " + lang.getMessage("lore.click_to_toggle"), NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, true));
        meta.lore(lore);
        item.setItemMeta(meta);
        inventory.setItem(IGNORE_DATA_SLOT, item);
    }

    private void addAdminNotificationsSetting() {
        boolean enabled = plugin.getConfig().getBoolean("admin-notifications", true);
        Material material = enabled ? Material.LIME_DYE : Material.GRAY_DYE;
        String status = enabled ? lang.getMessage("lore.status_enabled") : lang.getMessage("lore.status_disabled");
        NamedTextColor color = enabled ? NamedTextColor.GREEN : NamedTextColor.RED;

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(lang.getMessage("settings.admin_notifications"), NamedTextColor.YELLOW)
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text(lang.getMessage("info.status") + " ", NamedTextColor.GRAY)
                .append(Component.text(status, color))
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(Component.text(lang.getMessage("settings.admin_notifications_desc_line1"), NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text(lang.getMessage("settings.admin_notifications_desc_line2"), NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text(lang.getMessage("settings.admin_notifications_desc_line3"), NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(Component.text("» " + lang.getMessage("lore.click_to_toggle"), NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, true));
        meta.lore(lore);
        item.setItemMeta(meta);
        inventory.setItem(ADMIN_NOTIFICATIONS_SLOT, item);
    }

    private void addLanguageButton() {
        String currentLanguage = plugin.getConfig().getString("language", "en_US");
        String languageName = lang.getAvailableLanguages().getOrDefault(currentLanguage, "English");

        ItemStack item = new ItemStack(Material.KNOWLEDGE_BOOK);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(lang.getMessage("button.language"), NamedTextColor.LIGHT_PURPLE)
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text(lang.getMessage("lore.language_current") + ": ", NamedTextColor.GRAY)
                .append(Component.text(languageName, NamedTextColor.GOLD))
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(Component.text(lang.getMessage("lore.language_change"), NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(Component.text("» " + lang.getMessage("lore.click_to_open"), NamedTextColor.AQUA)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);
        item.setItemMeta(meta);
        inventory.setItem(LANGUAGE_SLOT, item);
    }

    private void addBackButton() {
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta meta = back.getItemMeta();
        meta.displayName(Component.text("« " + lang.getMessage("button.back_to_settings"), NamedTextColor.YELLOW)
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
        if (!(event.getWhoClicked() instanceof Player clicker) || !clicker.equals(player)) {
            return;
        }

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) {
            return;
        }

        int slot = event.getSlot();

        if (slot == BACK_SLOT) {
            parentGUI.refreshAndOpen();
            return;
        }

        if (!player.hasPermission("customrecipes.manage")) {
            MessageUtil.sendError(player, lang.getMessage("settings.no_permission_change"));
            return;
        }

        if (slot == SPAWN_EGG_SLOT) {
            boolean newValue = !plugin.isKeepSpawnEggNames();
            plugin.getConfig().set("spawn-egg-keep-custom-name", newValue);
            plugin.saveConfig();
            plugin.loadConfiguration();
            String status = newValue ? lang.getMessage("lore.status_enabled") : lang.getMessage("lore.status_disabled");
            MessageUtil.sendAdminSuccess(player, lang.getMessage("settings.spawn_egg_toggled").replace("{status}", status));
            updateInventory();
            return;
        }

        if (slot == CRAFTED_NAMES_SLOT) {
            boolean newValue = !plugin.isUseCraftedCustomNames();
            plugin.getConfig().set("use-crafted-custom-names", newValue);
            plugin.saveConfig();
            plugin.loadConfiguration();
            String status = newValue ? lang.getMessage("lore.status_enabled") : lang.getMessage("lore.status_disabled");
            MessageUtil.sendAdminSuccess(player, lang.getMessage("settings.crafted_names_toggled").replace("{status}", status));
            updateInventory();
            return;
        }

        if (slot == IGNORE_DATA_SLOT) {
            boolean newValue = !plugin.getConfig().getBoolean("ignore-metadata", false);
            plugin.getConfig().set("ignore-metadata", newValue);
            plugin.saveConfig();
            String status = newValue ? lang.getMessage("lore.status_enabled") : lang.getMessage("lore.status_disabled");
            MessageUtil.sendAdminSuccess(player, lang.getMessage("settings.ignore_metadata_toggled").replace("{status}", status));
            updateInventory();
            return;
        }

        if (slot == ADMIN_NOTIFICATIONS_SLOT) {
            boolean newValue = !plugin.getConfig().getBoolean("admin-notifications", true);
            plugin.getConfig().set("admin-notifications", newValue);
            plugin.saveConfig();
            String status = newValue ? lang.getMessage("lore.status_enabled") : lang.getMessage("lore.status_disabled");
            MessageUtil.sendAdminSuccess(player, lang.getMessage("settings.admin_notifications_toggled").replace("{status}", status));
            updateInventory();
            return;
        }

        if (slot == LANGUAGE_SLOT) {
            LanguageSelectionGUI languageGUI = new LanguageSelectionGUI(plugin, player, this);
            languageGUI.open();
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
