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
import org.hikarii.customrecipes.recipe.CraftEventPreset;
import org.hikarii.customrecipes.util.MessageUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

public class CraftEventPresetSelectorGUI implements Listener, GUIBase {
    private final CustomRecipes plugin;
    private final Player player;
    private final Inventory inventory;
    private final LanguageManager lang;
    private final String currentPresetName;
    private final Consumer<String> onSelect;
    private final boolean filterForCustomRecipes;

    private int currentPage = 0;
    private static final int PRESETS_PER_PAGE = 28;

    private static final int CLEAR_SLOT = 45;
    private static final int PREV_PAGE_SLOT = 48;
    private static final int PAGE_INFO_SLOT = 49;
    private static final int NEXT_PAGE_SLOT = 50;
    private static final int BACK_SLOT = 53;

    public CraftEventPresetSelectorGUI(CustomRecipes plugin, Player player, String currentPresetName,
                                        boolean filterForCustomRecipes, Consumer<String> onSelect) {
        this.plugin = plugin;
        this.player = player;
        this.currentPresetName = currentPresetName;
        this.filterForCustomRecipes = filterForCustomRecipes;
        this.onSelect = onSelect;
        this.lang = plugin.getLanguageManager();
        this.inventory = Bukkit.createInventory(
                null,
                54,
                MessageUtil.createGradientMenuTitle(lang.getMessage("craft_events.select_preset_title"))
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
        addPresetButtons();
        addNavigationButtons();
        addClearButton();
        addBackButton();
    }

    private void fillBorders() {
        ItemStack borderPane = new ItemStack(Material.PURPLE_STAINED_GLASS_PANE);
        ItemMeta meta = borderPane.getItemMeta();
        meta.displayName(Component.empty());
        borderPane.setItemMeta(meta);

        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, borderPane);
        }
        
        for (int i = 45; i < 54; i++) {
            inventory.setItem(i, borderPane);
        }
        
        for (int row = 1; row < 5; row++) {
            inventory.setItem(row * 9, borderPane);
            inventory.setItem(row * 9 + 8, borderPane);
        }
    }

    private void addPresetButtons() {
        List<CraftEventPreset> presets = getFilteredPresets();
        int start = currentPage * PRESETS_PER_PAGE;
        int end = Math.min(start + PRESETS_PER_PAGE, presets.size());

        int[] slots = getPresetSlots();
        int slotIndex = 0;

        for (int i = start; i < end && slotIndex < slots.length; i++, slotIndex++) {
            CraftEventPreset preset = presets.get(i);
            addPresetButton(slots[slotIndex], preset);
        }
    }

    private int[] getPresetSlots() {
        return new int[]{
                10, 11, 12, 13, 14, 15, 16,
                19, 20, 21, 22, 23, 24, 25,
                28, 29, 30, 31, 32, 33, 34,
                37, 38, 39, 40, 41, 42, 43
        };
    }

    private List<CraftEventPreset> getFilteredPresets() {
        Collection<CraftEventPreset> allPresets = plugin.getCraftEventPresetManager().getAllPresets();
        List<CraftEventPreset> filtered = new ArrayList<>();

        for (CraftEventPreset preset : allPresets) {
            
            if (filterForCustomRecipes) {
                if (preset.canApplyTo(true)) {
                    filtered.add(preset);
                }
            } else {
                
                if (preset.canApplyTo(false)) {
                    filtered.add(preset);
                }
            }
        }

        return filtered;
    }

    private void addPresetButton(int slot, CraftEventPreset preset) {
        boolean isSelected = preset.getName().equals(currentPresetName);

        ItemStack button = new ItemStack(isSelected ? Material.LIME_CONCRETE : Material.WHITE_CONCRETE);
        ItemMeta meta = button.getItemMeta();

        NamedTextColor nameColor = isSelected ? NamedTextColor.GREEN : NamedTextColor.WHITE;
        meta.displayName(Component.text(preset.getName(), nameColor)
                .decoration(TextDecoration.BOLD, isSelected)
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());

        int soundCount = preset.getSounds().size();
        int particleCount = preset.getParticles().size();
        int commandCount = preset.getCommands().size();

        if (soundCount > 0) {
            lore.add(Component.text("♪ " + lang.getMessage("craft_events.sounds_label") + ": " + soundCount, NamedTextColor.AQUA)
                    .decoration(TextDecoration.ITALIC, false));
        }
        if (particleCount > 0) {
            lore.add(Component.text("✦ " + lang.getMessage("craft_events.particles_label") + ": " + particleCount, NamedTextColor.LIGHT_PURPLE)
                    .decoration(TextDecoration.ITALIC, false));
        }
        if (commandCount > 0) {
            lore.add(Component.text("⌘ " + lang.getMessage("craft_events.commands_label") + ": " + commandCount, NamedTextColor.GOLD)
                    .decoration(TextDecoration.ITALIC, false));
        }

        if (soundCount == 0 && particleCount == 0 && commandCount == 0) {
            lore.add(Component.text(lang.getMessage("craft_events.no_events_configured"), NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false));
        }

        lore.add(Component.empty());
        String scopeKey = "craft_events.scope_" + preset.getScope().name().toLowerCase();
        lore.add(Component.text(lang.getMessage("craft_events.scope_label") + ": ", NamedTextColor.GRAY)
                .append(Component.text(lang.getMessage(scopeKey), NamedTextColor.YELLOW))
                .decoration(TextDecoration.ITALIC, false));

        lore.add(Component.empty());
        if (isSelected) {
            lore.add(Component.text("✔ " + lang.getMessage("info.selected"), NamedTextColor.GREEN)
                    .decoration(TextDecoration.ITALIC, false));
        } else {
            lore.add(Component.text("» " + lang.getMessage("lore.click_to_select"), NamedTextColor.YELLOW)
                    .decoration(TextDecoration.ITALIC, true));
        }

        meta.lore(lore);
        button.setItemMeta(meta);
        inventory.setItem(slot, button);
    }

    private void addNavigationButtons() {
        List<CraftEventPreset> presets = getFilteredPresets();
        int totalPages = (int) Math.ceil((double) presets.size() / PRESETS_PER_PAGE);
        if (totalPages == 0) totalPages = 1;

        if (currentPage > 0) {
            ItemStack prevButton = new ItemStack(Material.ARROW);
            ItemMeta meta = prevButton.getItemMeta();
            meta.displayName(Component.text(lang.getMessage("lore.previous_page"), NamedTextColor.YELLOW)
                    .decoration(TextDecoration.ITALIC, false));
            prevButton.setItemMeta(meta);
            inventory.setItem(PREV_PAGE_SLOT, prevButton);
        }

        ItemStack pageInfo = new ItemStack(Material.BOOK);
        ItemMeta meta = pageInfo.getItemMeta();
        meta.displayName(Component.text(lang.getMessage("lore.page_info")
                        .replace("{current}", String.valueOf(currentPage + 1))
                        .replace("{total}", String.valueOf(totalPages)), NamedTextColor.WHITE)
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text(lang.getMessage("craft_events.available_presets")
                        .replace("{count}", String.valueOf(presets.size())), NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);
        pageInfo.setItemMeta(meta);
        inventory.setItem(PAGE_INFO_SLOT, pageInfo);

        if (currentPage < totalPages - 1) {
            ItemStack nextButton = new ItemStack(Material.ARROW);
            ItemMeta nextMeta = nextButton.getItemMeta();
            nextMeta.displayName(Component.text(lang.getMessage("lore.next_page"), NamedTextColor.YELLOW)
                    .decoration(TextDecoration.ITALIC, false));
            nextButton.setItemMeta(nextMeta);
            inventory.setItem(NEXT_PAGE_SLOT, nextButton);
        }
    }

    private void addClearButton() {
        ItemStack button = new ItemStack(Material.BARRIER);
        ItemMeta meta = button.getItemMeta();
        meta.displayName(Component.text(lang.getMessage("craft_events.clear_preset"), NamedTextColor.RED)
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text(lang.getMessage("craft_events.clear_preset_desc"), NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(Component.text("» " + lang.getMessage("lore.click_to_select"), NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, true));

        meta.lore(lore);
        button.setItemMeta(meta);
        inventory.setItem(CLEAR_SLOT, button);
    }

    private void addBackButton() {
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta meta = back.getItemMeta();
        meta.displayName(Component.text(lang.getMessage("craft_events_selector.back_title"), NamedTextColor.YELLOW)
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
            player.closeInventory();
            if (onSelect != null) {
                onSelect.accept(currentPresetName); 
            }
            return;
        }

        if (slot == CLEAR_SLOT) {
            player.closeInventory();
            if (onSelect != null) {
                onSelect.accept(""); 
            }
            return;
        }

        if (slot == PREV_PAGE_SLOT && currentPage > 0) {
            currentPage--;
            updateInventory();
            return;
        }

        if (slot == NEXT_PAGE_SLOT) {
            List<CraftEventPreset> presets = getFilteredPresets();
            int totalPages = (int) Math.ceil((double) presets.size() / PRESETS_PER_PAGE);
            if (currentPage < totalPages - 1) {
                currentPage++;
                updateInventory();
            }
            return;
        }

        int[] presetSlots = getPresetSlots();
        for (int i = 0; i < presetSlots.length; i++) {
            if (slot == presetSlots[i]) {
                List<CraftEventPreset> presets = getFilteredPresets();
                int presetIndex = currentPage * PRESETS_PER_PAGE + i;

                if (presetIndex < presets.size()) {
                    CraftEventPreset selected = presets.get(presetIndex);
                    player.closeInventory();
                    if (onSelect != null) {
                        onSelect.accept(selected.getName());
                    }
                }
                return;
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
