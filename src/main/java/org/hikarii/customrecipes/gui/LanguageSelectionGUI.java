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
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.hikarii.customrecipes.CustomRecipes;
import org.hikarii.customrecipes.language.LanguageManager;
import org.hikarii.customrecipes.util.MessageUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class LanguageSelectionGUI implements Listener {
    private final CustomRecipes plugin;
    private final Player player;
    private final Inventory inventory;
    private final GUIBase parentGUI;

    public LanguageSelectionGUI(CustomRecipes plugin, Player player, GUIBase parentGUI) {
        this.plugin = plugin;
        this.player = player;
        this.parentGUI = parentGUI;

        LanguageManager lang = plugin.getLanguageManager();
        String title = lang.getMessage("gui.title.language_selector");

        this.inventory = Bukkit.createInventory(null, 27, Component.text(title));
        setupGUI();
    }

    private void setupGUI() {
        
        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glass.getItemMeta();
        if (glassMeta != null) {
            glassMeta.displayName(Component.text(" "));
            glass.setItemMeta(glassMeta);
        }

        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, glass);
        }

        Map<String, String> languages = plugin.getLanguageManager().getAvailableLanguages();
        int slot = 11;

        for (Map.Entry<String, String> entry : languages.entrySet()) {
            String langCode = entry.getKey();
            String langName = entry.getValue();

            ItemStack langItem = createLanguageButton(langCode, langName);
            inventory.setItem(slot, langItem);
            slot += 2;
        }

        ItemStack backButton = new ItemStack(Material.BARRIER);
        ItemMeta backMeta = backButton.getItemMeta();
        if (backMeta != null) {
            LanguageManager lang = plugin.getLanguageManager();
            backMeta.displayName(Component.text(lang.getMessage("button.back"), NamedTextColor.RED)
                    .decoration(TextDecoration.ITALIC, false));
            backButton.setItemMeta(backMeta);
        }
        inventory.setItem(22, backButton);
    }

    private ItemStack createLanguageButton(String langCode, String langName) {
        Material flagMaterial = getFlagMaterial(langCode);
        ItemStack item = new ItemStack(flagMaterial);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            
            meta.displayName(Component.text(langName, NamedTextColor.GOLD)
                    .decoration(TextDecoration.ITALIC, false));

            List<Component> lore = new ArrayList<>();
            LanguageManager lang = plugin.getLanguageManager();

            lore.add(Component.empty());
            if (langCode.equals(lang.getCurrentLanguage())) {
                lore.add(Component.text("✓ " + lang.getMessage("lore.language_current"), NamedTextColor.GREEN)
                        .decoration(TextDecoration.ITALIC, false));
            } else {
                lore.add(Component.text("» " + lang.getMessage("lore.language_select"), NamedTextColor.YELLOW)
                        .decoration(TextDecoration.ITALIC, false));
            }
            lore.add(Component.empty());

            meta.lore(lore);
            item.setItemMeta(meta);
        }

        return item;
    }

    private Material getFlagMaterial(String langCode) {
        return switch (langCode) {
            case "en_US" -> Material.WHITE_BANNER; 
            case "ru_RU" -> Material.BLUE_BANNER; 
            case "de_DE" -> Material.BLACK_BANNER; 
            case "uk_UA" -> Material.YELLOW_BANNER; 
            default -> Material.WHITE_BANNER;
        };
    }

    public void open() {
        player.openInventory(inventory);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getInventory().equals(inventory)) return;
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player clicker)) return;
        if (!clicker.equals(player)) return;

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        int slot = event.getSlot();

        if (slot == 22) {
            InventoryClickEvent.getHandlerList().unregister(this);
            
            SettingsGUI settingsGUI = new SettingsGUI(plugin, player);
            settingsGUI.open();
            return;
        }

        Map<String, String> languages = plugin.getLanguageManager().getAvailableLanguages();
        int index = (slot - 11) / 2;

        if (index >= 0 && index < languages.size()) {
            String selectedLang = (String) languages.keySet().toArray()[index];
            LanguageManager langManager = plugin.getLanguageManager();

            langManager.setLanguage(selectedLang);

            langManager.reload();

            String langName = languages.get(selectedLang);
            Map<String, String> placeholders = Map.of("language", langName);
            String message = langManager.getMessage("settings.language_changed", placeholders);
            MessageUtil.send(player, message, NamedTextColor.GREEN);

            InventoryClickEvent.getHandlerList().unregister(this);
            player.closeInventory();
        }
    }
}
