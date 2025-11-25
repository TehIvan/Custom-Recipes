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
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.hikarii.customrecipes.CustomRecipes;
import org.hikarii.customrecipes.language.LanguageManager;
import org.hikarii.customrecipes.util.MessageUtil;
import org.hikarii.customrecipes.version.VersionManager;

import java.util.*;
import java.util.function.Consumer;

public class FlagsSelectorGUI implements Listener {
    private final CustomRecipes plugin;
    private final LanguageManager lang;
    private final VersionManager versionManager;
    private final Player player;
    private final Inventory inventory;
    private final ItemStack targetItem;
    private final Set<ItemFlag> selectedFlags;
    private final Consumer<Void> onReturn;
    private boolean noPlace = false;

    private static final List<FlagInfo> ALL_FLAGS = new ArrayList<>();

    static {
        
        ALL_FLAGS.add(new FlagInfo(ItemFlag.HIDE_ENCHANTS, "hide_enchants", null));
        ALL_FLAGS.add(new FlagInfo(ItemFlag.HIDE_ATTRIBUTES, "hide_attributes", null));
        ALL_FLAGS.add(new FlagInfo(ItemFlag.HIDE_UNBREAKABLE, "hide_unbreakable", null));
        ALL_FLAGS.add(new FlagInfo(ItemFlag.HIDE_DESTROYS, "hide_destroys", null));
        ALL_FLAGS.add(new FlagInfo(ItemFlag.HIDE_PLACED_ON, "hide_placed_on", null));

        ALL_FLAGS.add(new FlagInfo(ItemFlag.HIDE_POTION_EFFECTS, "hide_potion_effects", null));

        try {
            ItemFlag hideDye = ItemFlag.valueOf("HIDE_DYE");
            ALL_FLAGS.add(new FlagInfo(hideDye, "hide_dye", null));
        } catch (IllegalArgumentException ignored) {
            
        }

        try {
            ItemFlag hideArmorTrim = ItemFlag.valueOf("HIDE_ARMOR_TRIM");
            ALL_FLAGS.add(new FlagInfo(hideArmorTrim, "hide_armor_trim", VersionManager.MinecraftVersion.v1_20));
        } catch (IllegalArgumentException ignored) {
            
        }

        try {
            ItemFlag hideAdditional = ItemFlag.valueOf("HIDE_ADDITIONAL_TOOLTIP");
            ALL_FLAGS.add(new FlagInfo(hideAdditional, "hide_additional_tooltip", VersionManager.MinecraftVersion.v1_20));
        } catch (IllegalArgumentException ignored) {
            
        }
    }

    public FlagsSelectorGUI(CustomRecipes plugin, Player player, ItemStack targetItem,
                            Set<ItemFlag> currentFlags,
                            boolean currentNoPlace,
                            Consumer<Void> onReturn) {
        this.plugin = plugin;
        this.lang = plugin.getLanguageManager();
        this.versionManager = plugin.getVersionManager();
        this.player = player;
        this.targetItem = targetItem;
        this.selectedFlags = new HashSet<>(currentFlags);
        this.noPlace = currentNoPlace;
        this.onReturn = onReturn;
        this.inventory = Bukkit.createInventory(
                null,
                54,
                MessageUtil.createMenuTitle(lang.getMessage("flags_selector.title"), NamedTextColor.DARK_GREEN)
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
        addFlagBanners();
        addNoPlaceButton();
        addBackButton();
    }

    private void fillBorders() {
        ItemStack borderPane = new ItemStack(Material.GREEN_STAINED_GLASS_PANE);
        ItemMeta meta = borderPane.getItemMeta();
        meta.displayName(Component.empty());
        borderPane.setItemMeta(meta);
        for (int i = 45; i < 54; i++) {
            inventory.setItem(i, borderPane);
        }
    }

    private List<FlagInfo> getAvailableFlags() {
        List<FlagInfo> available = new ArrayList<>();
        for (FlagInfo info : ALL_FLAGS) {
            
            if (info.minVersion != null && versionManager.isBelow(info.minVersion)) {
                continue;
            }
            
            if (info.key.equals("hide_additional_tooltip") &&
                available.stream().anyMatch(f -> f.key.equals("hide_potion_effects"))) {
                
                if (versionManager.isAtLeast(VersionManager.MinecraftVersion.v1_20)) {
                    available.removeIf(f -> f.key.equals("hide_potion_effects"));
                    available.add(info);
                }
                continue;
            }
            available.add(info);
        }
        return available;
    }

    private void addFlagBanners() {
        List<FlagInfo> flags = getAvailableFlags();
        int slot = 10; 

        for (FlagInfo flagInfo : flags) {
            if (slot == 17) slot = 19; 
            if (slot == 26) slot = 28;
            if (slot == 35) slot = 37;
            if (slot >= 44) break; 

            boolean isEnabled = selectedFlags.contains(flagInfo.flag);
            ItemStack banner = createFlagBanner(flagInfo, isEnabled);
            inventory.setItem(slot, banner);
            slot++;
        }
    }

    private ItemStack createFlagBanner(FlagInfo flagInfo, boolean isEnabled) {
        Material bannerType = isEnabled ? Material.LIME_BANNER : Material.RED_BANNER;
        ItemStack banner = new ItemStack(bannerType);
        ItemMeta meta = banner.getItemMeta();

        String flagName = lang.getMessage("flags_selector.flag." + flagInfo.key);
        NamedTextColor nameColor = isEnabled ? NamedTextColor.GREEN : NamedTextColor.RED;

        meta.displayName(Component.text(flagName, nameColor)
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());

        String statusKey = isEnabled ? "flags_selector.status_enabled" : "flags_selector.status_disabled";
        lore.add(Component.text(lang.getMessage(statusKey),
                        isEnabled ? NamedTextColor.GREEN : NamedTextColor.RED)
                .decoration(TextDecoration.ITALIC, false));

        lore.add(Component.empty());

        String description = lang.getMessage("flags_selector.flag." + flagInfo.key + "_desc");
        if (!description.equals("flags_selector.flag." + flagInfo.key + "_desc")) {
            lore.add(Component.text(description, NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.empty());
        }

        lore.add(Component.text(lang.getMessage("flags_selector.click_to_toggle"), NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, true));

        meta.lore(lore);
        banner.setItemMeta(meta);
        return banner;
    }

    private void addNoPlaceButton() {
        Material bannerType = noPlace ? Material.LIME_BANNER : Material.RED_BANNER;
        ItemStack banner = new ItemStack(bannerType);
        ItemMeta meta = banner.getItemMeta();

        String flagName = lang.getMessage("flags_selector.flag.no_place");
        NamedTextColor nameColor = noPlace ? NamedTextColor.GREEN : NamedTextColor.RED;

        meta.displayName(Component.text(flagName, nameColor)
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());

        String statusKey = noPlace ? "flags_selector.status_enabled" : "flags_selector.status_disabled";
        lore.add(Component.text(lang.getMessage(statusKey),
                        noPlace ? NamedTextColor.GREEN : NamedTextColor.RED)
                .decoration(TextDecoration.ITALIC, false));

        lore.add(Component.empty());
        lore.add(Component.text(lang.getMessage("flags_selector.flag.no_place_desc"), NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(Component.text(lang.getMessage("flags_selector.click_to_toggle"), NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, true));

        meta.lore(lore);
        banner.setItemMeta(meta);
        inventory.setItem(40, banner); 
    }

    private void addBackButton() {
        ItemStack back = new ItemStack(Material.BARRIER);
        ItemMeta meta = back.getItemMeta();
        meta.displayName(Component.text(lang.getMessage("flags_selector.back_to_item_editor"), NamedTextColor.RED)
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
                Component.empty(),
                Component.text(lang.getMessage("flags_selector.save_and_return"), NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false)
        ));
        back.setItemMeta(meta);
        inventory.setItem(53, back);
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

        int slot = event.getSlot();
        ItemStack clicked = event.getCurrentItem();

        if (clicked == null || clicked.getType() == Material.AIR) {
            return;
        }

        if (slot == 53) {
            ItemEditorGUI editor = ItemEditorGUI.getLastEditor(player.getUniqueId());
            if (editor != null) {
                editor.customFlags.clear();
                editor.customFlags.addAll(selectedFlags);
                editor.noPlace = noPlace;
                editor.forceUpdatePreview();
            }
            player.closeInventory();

            if (onReturn != null) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    onReturn.accept(null);
                });
            }
            return;
        }

        if (slot == 40) {
            noPlace = !noPlace;
            updateInventory();
            return;
        }

        if (clicked.getType() == Material.LIME_BANNER || clicked.getType() == Material.RED_BANNER) {
            List<FlagInfo> flags = getAvailableFlags();

            int flagIndex = slotToFlagIndex(slot);
            if (flagIndex >= 0 && flagIndex < flags.size()) {
                FlagInfo flagInfo = flags.get(flagIndex);

                if (selectedFlags.contains(flagInfo.flag)) {
                    selectedFlags.remove(flagInfo.flag);
                } else {
                    selectedFlags.add(flagInfo.flag);
                }
                updateInventory();
            }
        }
    }

    private int slotToFlagIndex(int slot) {

        if (slot >= 10 && slot <= 16) return slot - 10;
        if (slot >= 19 && slot <= 25) return slot - 19 + 7;
        if (slot >= 28 && slot <= 34) return slot - 28 + 14;
        if (slot >= 37 && slot <= 43) return slot - 37 + 21;
        return -1;
    }

    public Set<ItemFlag> getSelectedFlags() {
        return new HashSet<>(selectedFlags);
    }

    public boolean isNoPlace() {
        return noPlace;
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().equals(inventory) && event.getPlayer().equals(player)) {
            InventoryClickEvent.getHandlerList().unregister(this);
            InventoryCloseEvent.getHandlerList().unregister(this);
        }
    }

    private static class FlagInfo {
        final ItemFlag flag;
        final String key;
        final VersionManager.MinecraftVersion minVersion;

        FlagInfo(ItemFlag flag, String key, VersionManager.MinecraftVersion minVersion) {
            this.flag = flag;
            this.key = key;
            this.minVersion = minVersion;
        }
    }
}
