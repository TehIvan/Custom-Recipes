package org.hikarii.customrecipes.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
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
import org.hikarii.customrecipes.recipe.CraftEventPreset;
import org.hikarii.customrecipes.util.MessageUtil;

import java.util.ArrayList;
import java.util.List;

public class SoundAdjustGUI implements Listener, GUIBase {
    private final CustomRecipes plugin;
    private final Player player;
    private final Inventory inventory;
    private final LanguageManager lang;
    private final CraftEventPreset preset;
    private final CraftEventPreset.SoundEvent soundEvent;
    private final SoundSelectorGUI parentGUI;

    private float volume;
    private float pitch;

    private static final int SOUND_INFO_SLOT = 4;
    private static final int VOLUME_DECREASE_SLOT = 19;
    private static final int VOLUME_DISPLAY_SLOT = 20;
    private static final int VOLUME_INCREASE_SLOT = 21;
    private static final int PITCH_DECREASE_SLOT = 23;
    private static final int PITCH_DISPLAY_SLOT = 24;
    private static final int PITCH_INCREASE_SLOT = 25;
    private static final int PREVIEW_SLOT = 31;
    private static final int SAVE_SLOT = 40;
    private static final int BACK_SLOT = 36;

    public SoundAdjustGUI(CustomRecipes plugin, Player player, CraftEventPreset preset,
                          CraftEventPreset.SoundEvent soundEvent, SoundSelectorGUI parentGUI) {
        this.plugin = plugin;
        this.player = player;
        this.preset = preset;
        this.soundEvent = soundEvent;
        this.parentGUI = parentGUI;
        this.lang = plugin.getLanguageManager();
        this.volume = soundEvent.getVolume();
        this.pitch = soundEvent.getPitch();
        this.inventory = Bukkit.createInventory(
                null,
                45,
                MessageUtil.createGradientMenuTitle(lang.getMessage("craft_events.adjust_sound_title"))
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
        addSoundInfo();
        addVolumeControls();
        addPitchControls();
        addPreviewButton();
        addSaveButton();
        addBackButton();
    }

    private void fillBorders() {
        ItemStack borderPane = new ItemStack(Material.ORANGE_STAINED_GLASS_PANE);
        ItemMeta meta = borderPane.getItemMeta();
        meta.displayName(Component.empty());
        borderPane.setItemMeta(meta);

        for (int i = 0; i < 45; i++) {
            inventory.setItem(i, borderPane);
        }
    }

    private void addSoundInfo() {
        ItemStack item = new ItemStack(Material.NOTE_BLOCK);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(formatSoundName(soundEvent.getSound()), NamedTextColor.GOLD)
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text(soundEvent.getSound().name(), NamedTextColor.DARK_GRAY)
                .decoration(TextDecoration.ITALIC, false));

        meta.lore(lore);
        item.setItemMeta(meta);
        inventory.setItem(SOUND_INFO_SLOT, item);
    }

    private void addVolumeControls() {
        
        ItemStack decrease = new ItemStack(Material.RED_CONCRETE);
        ItemMeta decMeta = decrease.getItemMeta();
        decMeta.displayName(Component.text("-0.1 " + lang.getMessage("craft_events.volume"), NamedTextColor.RED)
                .decoration(TextDecoration.ITALIC, false));
        List<Component> decLore = new ArrayList<>();
        decLore.add(Component.text(lang.getMessage("craft_events.shift_for_more"), NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        decMeta.lore(decLore);
        decrease.setItemMeta(decMeta);
        inventory.setItem(VOLUME_DECREASE_SLOT, decrease);

        int displayAmount = Math.max(1, Math.min(64, (int) (volume * 10)));
        ItemStack display = new ItemStack(Material.EXPERIENCE_BOTTLE, displayAmount);
        ItemMeta dispMeta = display.getItemMeta();
        dispMeta.displayName(Component.text(lang.getMessage("craft_events.volume"), NamedTextColor.AQUA)
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false));
        List<Component> dispLore = new ArrayList<>();
        dispLore.add(Component.empty());
        dispLore.add(Component.text(lang.getMessage("info.current") + ": ", NamedTextColor.GRAY)
                .append(Component.text(String.format("%.1f", volume), NamedTextColor.YELLOW))
                .decoration(TextDecoration.ITALIC, false));
        dispLore.add(Component.empty());
        dispLore.add(Component.text(lang.getMessage("craft_events.volume_range"), NamedTextColor.DARK_GRAY)
                .decoration(TextDecoration.ITALIC, false));
        dispMeta.lore(dispLore);
        display.setItemMeta(dispMeta);
        inventory.setItem(VOLUME_DISPLAY_SLOT, display);

        ItemStack increase = new ItemStack(Material.LIME_CONCRETE);
        ItemMeta incMeta = increase.getItemMeta();
        incMeta.displayName(Component.text("+0.1 " + lang.getMessage("craft_events.volume"), NamedTextColor.GREEN)
                .decoration(TextDecoration.ITALIC, false));
        List<Component> incLore = new ArrayList<>();
        incLore.add(Component.text(lang.getMessage("craft_events.shift_for_more"), NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        incMeta.lore(incLore);
        increase.setItemMeta(incMeta);
        inventory.setItem(VOLUME_INCREASE_SLOT, increase);
    }

    private void addPitchControls() {
        
        ItemStack decrease = new ItemStack(Material.RED_CONCRETE);
        ItemMeta decMeta = decrease.getItemMeta();
        decMeta.displayName(Component.text("-0.1 " + lang.getMessage("craft_events.pitch"), NamedTextColor.RED)
                .decoration(TextDecoration.ITALIC, false));
        List<Component> decLore = new ArrayList<>();
        decLore.add(Component.text(lang.getMessage("craft_events.shift_for_more"), NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        decMeta.lore(decLore);
        decrease.setItemMeta(decMeta);
        inventory.setItem(PITCH_DECREASE_SLOT, decrease);

        int displayAmount = Math.max(1, Math.min(64, (int) (pitch * 10)));
        ItemStack display = new ItemStack(Material.BLAZE_ROD, displayAmount);
        ItemMeta dispMeta = display.getItemMeta();
        dispMeta.displayName(Component.text(lang.getMessage("craft_events.pitch"), NamedTextColor.LIGHT_PURPLE)
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false));
        List<Component> dispLore = new ArrayList<>();
        dispLore.add(Component.empty());
        dispLore.add(Component.text(lang.getMessage("info.current") + ": ", NamedTextColor.GRAY)
                .append(Component.text(String.format("%.1f", pitch), NamedTextColor.YELLOW))
                .decoration(TextDecoration.ITALIC, false));
        dispLore.add(Component.empty());
        dispLore.add(Component.text(lang.getMessage("craft_events.pitch_range"), NamedTextColor.DARK_GRAY)
                .decoration(TextDecoration.ITALIC, false));
        dispMeta.lore(dispLore);
        display.setItemMeta(dispMeta);
        inventory.setItem(PITCH_DISPLAY_SLOT, display);

        ItemStack increase = new ItemStack(Material.LIME_CONCRETE);
        ItemMeta incMeta = increase.getItemMeta();
        incMeta.displayName(Component.text("+0.1 " + lang.getMessage("craft_events.pitch"), NamedTextColor.GREEN)
                .decoration(TextDecoration.ITALIC, false));
        List<Component> incLore = new ArrayList<>();
        incLore.add(Component.text(lang.getMessage("craft_events.shift_for_more"), NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        incMeta.lore(incLore);
        increase.setItemMeta(incMeta);
        inventory.setItem(PITCH_INCREASE_SLOT, increase);
    }

    private void addPreviewButton() {
        ItemStack button = new ItemStack(Material.BELL);
        ItemMeta meta = button.getItemMeta();
        meta.displayName(Component.text(lang.getMessage("craft_events.preview_sound"), NamedTextColor.GREEN)
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text("» " + lang.getMessage("lore.click_to_preview"), NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, true));

        meta.lore(lore);
        button.setItemMeta(meta);
        inventory.setItem(PREVIEW_SLOT, button);
    }

    private void addSaveButton() {
        ItemStack button = new ItemStack(Material.LIME_WOOL);
        ItemMeta meta = button.getItemMeta();
        meta.displayName(Component.text(lang.getMessage("button.save_and_return"), NamedTextColor.GREEN)
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false));
        button.setItemMeta(meta);
        inventory.setItem(SAVE_SLOT, button);
    }

    private void addBackButton() {
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta meta = back.getItemMeta();
        meta.displayName(Component.text(lang.getMessage("button.back"), NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));
        back.setItemMeta(meta);
        inventory.setItem(BACK_SLOT, back);
    }

    private String formatSoundName(Sound sound) {
        String name = sound.name().toLowerCase().replace("_", " ");
        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = true;
        for (char c : name.toCharArray()) {
            if (c == ' ') {
                result.append(c);
                capitalizeNext = true;
            } else if (capitalizeNext) {
                result.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                result.append(c);
            }
        }
        String formatted = result.toString();
        if (formatted.length() > 30) {
            formatted = formatted.substring(0, 27) + "...";
        }
        return formatted;
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
        float step = clickType.isShiftClick() ? 0.5f : 0.1f;

        switch (slot) {
            case BACK_SLOT -> parentGUI.refreshAndOpen();
            case SAVE_SLOT -> {
                soundEvent.setVolume(volume);
                soundEvent.setPitch(pitch);
                MessageUtil.sendInfo(player, lang.getMessage("craft_events.sound_adjusted"));
                parentGUI.refreshAndOpen();
            }
            case PREVIEW_SLOT -> player.playSound(player.getLocation(), soundEvent.getSound(), volume, pitch);
            case VOLUME_DECREASE_SLOT -> {
                volume = Math.max(0.1f, volume - step);
                updateInventory();
            }
            case VOLUME_INCREASE_SLOT -> {
                volume = Math.min(2.0f, volume + step);
                updateInventory();
            }
            case PITCH_DECREASE_SLOT -> {
                pitch = Math.max(0.5f, pitch - step);
                updateInventory();
            }
            case PITCH_INCREASE_SLOT -> {
                pitch = Math.min(2.0f, pitch + step);
                updateInventory();
            }
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
        player.openInventory(inventory);
    }
}
