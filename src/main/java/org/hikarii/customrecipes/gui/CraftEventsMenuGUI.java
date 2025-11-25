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
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import java.util.Map;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.hikarii.customrecipes.CustomRecipes;
import org.hikarii.customrecipes.language.LanguageManager;
import org.hikarii.customrecipes.recipe.CraftEventPreset;
import org.hikarii.customrecipes.recipe.CraftEventPresetManager;
import org.hikarii.customrecipes.util.MessageUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class CraftEventsMenuGUI implements Listener, GUIBase {
    private final CustomRecipes plugin;
    private final Player player;
    private final Inventory inventory;
    private final LanguageManager lang;
    private final GUIBase parentGUI;
    private final CraftEventPresetManager presetManager;

    private int currentPage = 0;
    private static final int PRESETS_PER_PAGE = 28;
    private boolean awaitingPresetName = false;

    public CraftEventsMenuGUI(CustomRecipes plugin, Player player, GUIBase parentGUI) {
        this.plugin = plugin;
        this.player = player;
        this.parentGUI = parentGUI;
        this.lang = plugin.getLanguageManager();
        this.presetManager = plugin.getCraftEventPresetManager();
        this.inventory = Bukkit.createInventory(
                null,
                54,
                MessageUtil.createGradientMenuTitle(lang.getMessage("craft_events.title"))
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
        addCreatePresetButton();
        addNavigationButtons();
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
        Collection<CraftEventPreset> allPresets = presetManager.getAllPresets();
        List<CraftEventPreset> presetList = new ArrayList<>(allPresets);

        int start = currentPage * PRESETS_PER_PAGE;
        int end = Math.min(start + PRESETS_PER_PAGE, presetList.size());

        int[] slots = getPresetSlots();
        int slotIndex = 0;

        for (int i = start; i < end && slotIndex < slots.length; i++, slotIndex++) {
            CraftEventPreset preset = presetList.get(i);
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

    private void addPresetButton(int slot, CraftEventPreset preset) {
        ItemStack button = new ItemStack(Material.BELL);
        ItemMeta meta = button.getItemMeta();

        meta.displayName(Component.text(preset.getName(), NamedTextColor.GOLD)
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());

        String scopeText = switch (preset.getScope()) {
            case ALL -> lang.getMessage("craft_events.scope_all");
            case CUSTOM_ONLY -> lang.getMessage("craft_events.scope_custom");
            case VANILLA_ONLY -> lang.getMessage("craft_events.scope_vanilla");
        };
        lore.add(Component.text(lang.getMessage("craft_events.scope_label") + ": ", NamedTextColor.GRAY)
                .append(Component.text(scopeText, NamedTextColor.AQUA))
                .decoration(TextDecoration.ITALIC, false));

        lore.add(Component.text(lang.getMessage("craft_events.sounds_label") + ": ", NamedTextColor.GRAY)
                .append(Component.text(String.valueOf(preset.getSounds().size()), NamedTextColor.GREEN))
                .decoration(TextDecoration.ITALIC, false));

        lore.add(Component.text(lang.getMessage("craft_events.particles_label") + ": ", NamedTextColor.GRAY)
                .append(Component.text(String.valueOf(preset.getParticles().size()), NamedTextColor.GREEN))
                .decoration(TextDecoration.ITALIC, false));

        lore.add(Component.text(lang.getMessage("craft_events.commands_label") + ": ", NamedTextColor.GRAY)
                .append(Component.text(String.valueOf(preset.getCommands().size()), NamedTextColor.GREEN))
                .decoration(TextDecoration.ITALIC, false));

        lore.add(Component.empty());
        lore.add(Component.text("» " + lang.getMessage("craft_events.click_to_edit"), NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, true));
        lore.add(Component.text("» " + lang.getMessage("craft_events.shift_click_to_delete"), NamedTextColor.RED)
                .decoration(TextDecoration.ITALIC, true));

        meta.lore(lore);
        button.setItemMeta(meta);
        inventory.setItem(slot, button);
    }

    private void addCreatePresetButton() {
        ItemStack button = new ItemStack(Material.EMERALD);
        ItemMeta meta = button.getItemMeta();
        meta.displayName(Component.text(lang.getMessage("craft_events.create_preset"), NamedTextColor.GREEN)
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text(lang.getMessage("craft_events.create_preset_desc"), NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(Component.text("» " + lang.getMessage("lore.click_to_create"), NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, true));

        meta.lore(lore);
        button.setItemMeta(meta);
        inventory.setItem(4, button);
    }

    private void addNavigationButtons() {
        int totalPages = Math.max(1, (int) Math.ceil((double) presetManager.getPresetCount() / PRESETS_PER_PAGE));

        ItemStack prevButton = new ItemStack(Material.ARROW);
        ItemMeta prevMeta = prevButton.getItemMeta();
        prevMeta.displayName(Component.text(lang.getMessage("nav.previous_page"),
                        currentPage > 0 ? NamedTextColor.YELLOW : NamedTextColor.DARK_GRAY)
                .decoration(TextDecoration.ITALIC, false));
        if (currentPage > 0) {
            prevMeta.lore(List.of(
                    Component.text(lang.getMessage("nav.page", Map.of("current", String.valueOf(currentPage), "total", String.valueOf(totalPages))), NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false)
            ));
        } else {
            prevMeta.lore(List.of(
                    Component.text(lang.getMessage("nav.already_first_page"), NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false)
            ));
        }
        prevButton.setItemMeta(prevMeta);
        inventory.setItem(48, prevButton);

        ItemStack nextButton = new ItemStack(Material.ARROW);
        ItemMeta nextMeta = nextButton.getItemMeta();
        nextMeta.displayName(Component.text(lang.getMessage("nav.next_page"),
                        currentPage < totalPages - 1 ? NamedTextColor.YELLOW : NamedTextColor.DARK_GRAY)
                .decoration(TextDecoration.ITALIC, false));
        if (currentPage < totalPages - 1) {
            nextMeta.lore(List.of(
                    Component.text(lang.getMessage("nav.page", Map.of("current", String.valueOf(currentPage + 2), "total", String.valueOf(totalPages))), NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false)
            ));
        } else {
            nextMeta.lore(List.of(
                    Component.text(lang.getMessage("nav.already_last_page"), NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false)
            ));
        }
        nextButton.setItemMeta(nextMeta);
        inventory.setItem(50, nextButton);

        ItemStack pageInfo = new ItemStack(Material.BOOK);
        ItemMeta meta = pageInfo.getItemMeta();
        meta.displayName(Component.text(lang.getMessage("lore.page_info")
                        .replace("{current}", String.valueOf(currentPage + 1))
                        .replace("{total}", String.valueOf(totalPages)), NamedTextColor.WHITE)
                .decoration(TextDecoration.ITALIC, false));
        pageInfo.setItemMeta(meta);
        inventory.setItem(49, pageInfo);
    }

    private void addBackButton() {
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta meta = back.getItemMeta();
        meta.displayName(Component.text("« " + lang.getMessage("button.back_to_settings"), NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));
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

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) {
            return;
        }

        int slot = event.getSlot();
        ClickType clickType = event.getClick();

        if (slot == 53) {
            if (parentGUI != null) {
                if (parentGUI instanceof SettingsGUI settingsGUI) {
                    settingsGUI.refreshAndOpen();
                } else {
                    player.closeInventory();
                }
            } else {
                player.closeInventory();
            }
            return;
        }

        if (slot == 4) {
            awaitingPresetName = true;
            player.closeInventory();
            MessageUtil.sendInfo(player, lang.getMessage("craft_events.enter_preset_name"));
            return;
        }

        if (slot == 48 && currentPage > 0) {
            currentPage--;
            updateInventory();
            return;
        }

        if (slot == 50) {
            int totalPages = Math.max(1, (int) Math.ceil((double) presetManager.getPresetCount() / PRESETS_PER_PAGE));
            if (currentPage < totalPages - 1) {
                currentPage++;
                updateInventory();
            }
            return;
        }

        int[] slots = getPresetSlots();
        for (int i = 0; i < slots.length; i++) {
            if (slot == slots[i]) {
                List<CraftEventPreset> presetList = new ArrayList<>(presetManager.getAllPresets());
                int presetIndex = currentPage * PRESETS_PER_PAGE + i;

                if (presetIndex < presetList.size()) {
                    CraftEventPreset preset = presetList.get(presetIndex);

                    if (clickType.isShiftClick()) {
                        
                        presetManager.removePreset(preset.getName());
                        MessageUtil.sendAdminWarning(player, lang.getMessage("craft_events.preset_deleted")
                                .replace("{name}", preset.getName()));
                        updateInventory();
                    } else {
                        
                        CraftEventPresetEditorGUI editor = new CraftEventPresetEditorGUI(plugin, player, preset, this);
                        editor.open();
                    }
                }
                return;
            }
        }
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        if (!event.getPlayer().equals(player) || !awaitingPresetName) {
            return;
        }

        event.setCancelled(true);
        awaitingPresetName = false;

        String presetName = event.getMessage().trim();

        if (presetName.equalsIgnoreCase("cancel")) {
            Bukkit.getScheduler().runTask(plugin, this::open);
            return;
        }

        if (presetName.isEmpty() || presetName.length() > 32) {
            MessageUtil.sendError(player, lang.getMessage("craft_events.invalid_preset_name"));
            Bukkit.getScheduler().runTask(plugin, this::open);
            return;
        }

        if (presetManager.hasPreset(presetName)) {
            MessageUtil.sendError(player, lang.getMessage("craft_events.preset_exists")
                    .replace("{name}", presetName));
            Bukkit.getScheduler().runTask(plugin, this::open);
            return;
        }

        CraftEventPreset newPreset = new CraftEventPreset(presetName);
        presetManager.addPreset(newPreset);

        MessageUtil.sendAdminSuccess(player, lang.getMessage("craft_events.preset_created")
                .replace("{name}", presetName));

        Bukkit.getScheduler().runTask(plugin, () -> {
            CraftEventPresetEditorGUI editor = new CraftEventPresetEditorGUI(plugin, player, newPreset, this);
            editor.open();
        });
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().equals(inventory) && event.getPlayer().equals(player)) {
            if (!awaitingPresetName) {
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (!inventory.equals(player.getOpenInventory().getTopInventory())) {
                        InventoryClickEvent.getHandlerList().unregister(this);
                        InventoryCloseEvent.getHandlerList().unregister(this);
                        AsyncPlayerChatEvent.getHandlerList().unregister(this);
                    }
                }, 1L);
            }
        }
    }

    public void refreshAndOpen() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        updateInventory();
        player.openInventory(inventory);
    }
}
