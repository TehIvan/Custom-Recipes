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

public class PermissionsSettingsGUI implements Listener {
    private final CustomRecipes plugin;
    private final Player player;
    private final Inventory inventory;
    private final LanguageManager lang;
    private final SettingsGUI parent;

    public PermissionsSettingsGUI(CustomRecipes plugin, Player player, SettingsGUI parent) {
        this.plugin = plugin;
        this.player = player;
        this.parent = parent;
        this.lang = plugin.getLanguageManager();
        this.inventory = Bukkit.createInventory(
                null,
                36,
                MessageUtil.createGradientMenuTitle(lang.getMessage("gui.title.permissions"))
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

        addPermissionItem(10, Material.CRAFTING_TABLE, "customrecipes.gui",
                lang.getMessage("permissions.gui_title"),
                lang.getMessage("permissions.gui_desc"),
                "op");

        addPermissionItem(11, Material.BOOK, "customrecipes.list",
                lang.getMessage("permissions.list_title"),
                lang.getMessage("permissions.list_desc"),
                "true");

        addPermissionItem(12, Material.KNOWLEDGE_BOOK, "customrecipes.list.all",
                lang.getMessage("permissions.list_all_title"),
                lang.getMessage("permissions.list_all_desc"),
                "op");

        addPermissionItem(13, Material.PAPER, "customrecipes.help",
                lang.getMessage("permissions.help_title"),
                lang.getMessage("permissions.help_desc"),
                "true");

        addPermissionItem(14, Material.COMPARATOR, "customrecipes.manage",
                lang.getMessage("permissions.manage_title"),
                lang.getMessage("permissions.manage_desc"),
                "op");

        addPermissionItem(15, Material.COMMAND_BLOCK, "customrecipes.reload",
                lang.getMessage("permissions.reload_title"),
                lang.getMessage("permissions.reload_desc"),
                "op");

        addPermissionItem(16, Material.BELL, "customrecipes.update.notify",
                lang.getMessage("permissions.update_notify_title"),
                lang.getMessage("permissions.update_notify_desc"),
                "op");

        addInfoItem();

        addBackButton();
    }

    private void fillBorders() {
        ItemStack borderPane = new ItemStack(Material.LIGHT_BLUE_STAINED_GLASS_PANE);
        ItemMeta meta = borderPane.getItemMeta();
        meta.displayName(Component.empty());
        borderPane.setItemMeta(meta);
        for (int i = 0; i < 36; i++) {
            inventory.setItem(i, borderPane);
        }
    }

    private void addPermissionItem(int slot, Material material, String permission,
                                   String title, String description, String defaultValue) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        boolean hasPermission = player.hasPermission(permission);

        meta.displayName(Component.text(title, hasPermission ? NamedTextColor.GREEN : NamedTextColor.RED)
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text(lang.getMessage("permissions.node") + " ", NamedTextColor.GRAY)
                .append(Component.text(permission, NamedTextColor.YELLOW))
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(Component.text(description, NamedTextColor.WHITE)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(Component.text(lang.getMessage("permissions.default") + " ", NamedTextColor.GRAY)
                .append(Component.text(defaultValue, NamedTextColor.AQUA))
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(Component.text(lang.getMessage("permissions.your_status") + " ", NamedTextColor.GRAY)
                .append(Component.text(
                        hasPermission ? lang.getMessage("permissions.has_permission") : lang.getMessage("permissions.no_permission"),
                        hasPermission ? NamedTextColor.GREEN : NamedTextColor.RED))
                .decoration(TextDecoration.ITALIC, false));

        meta.lore(lore);
        item.setItemMeta(meta);
        inventory.setItem(slot, item);
    }

    private void addInfoItem() {
        ItemStack info = new ItemStack(Material.OAK_SIGN);
        ItemMeta meta = info.getItemMeta();
        meta.displayName(Component.text(lang.getMessage("permissions.info_title"), NamedTextColor.GOLD)
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text(lang.getMessage("permissions.info_line1"), NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text(lang.getMessage("permissions.info_line2"), NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(Component.text(lang.getMessage("permissions.info_line3"), NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));

        meta.lore(lore);
        info.setItemMeta(meta);
        inventory.setItem(31, info);  
    }

    private void addBackButton() {
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta meta = back.getItemMeta();
        meta.displayName(Component.text(lang.getMessage("settings.back_to_settings"), NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));
        back.setItemMeta(meta);
        inventory.setItem(35, back);  
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

        int slot = event.getSlot();

        if (slot == 35) {
            parent.refreshAndOpen();
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
}
