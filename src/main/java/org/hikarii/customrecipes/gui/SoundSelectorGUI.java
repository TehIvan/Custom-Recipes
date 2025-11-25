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

import java.util.*;

public class SoundSelectorGUI implements Listener, GUIBase {
    private final CustomRecipes plugin;
    private final Player player;
    private final Inventory inventory;
    private final LanguageManager lang;
    private final CraftEventPreset preset;
    private final CraftEventPresetEditorGUI parentGUI;

    private SoundCategory currentCategory = SoundCategory.POPULAR;
    private int currentPage = 0;
    private static final int SOUNDS_PER_PAGE = 28;

    private static final int[] CATEGORY_SLOTS = {1, 2, 3, 4, 5, 6, 7};
    private static final int BACK_SLOT = 45;
    private static final int PREV_PAGE_SLOT = 48;
    private static final int PAGE_INFO_SLOT = 49;
    private static final int NEXT_PAGE_SLOT = 50;
    private static final int CONFIGURED_SOUNDS_SLOT = 53;

    private static final Sound[] POPULAR_SOUNDS = {
            Sound.ENTITY_EXPERIENCE_ORB_PICKUP,
            Sound.ENTITY_PLAYER_LEVELUP,
            Sound.BLOCK_ANVIL_USE,
            Sound.BLOCK_ENCHANTMENT_TABLE_USE,
            Sound.ENTITY_VILLAGER_YES,
            Sound.ENTITY_VILLAGER_NO,
            Sound.BLOCK_NOTE_BLOCK_PLING,
            Sound.BLOCK_NOTE_BLOCK_CHIME,
            Sound.ENTITY_ARROW_HIT_PLAYER,
            Sound.ENTITY_FIREWORK_ROCKET_BLAST,
            Sound.ENTITY_FIREWORK_ROCKET_TWINKLE,
            Sound.ITEM_TOTEM_USE,
            Sound.UI_TOAST_CHALLENGE_COMPLETE,
            Sound.ENTITY_ENDER_DRAGON_GROWL,
            Sound.ENTITY_WITHER_SPAWN,
            Sound.BLOCK_BEACON_ACTIVATE,
            Sound.BLOCK_BEACON_POWER_SELECT,
            Sound.ENTITY_PLAYER_BURP,
            Sound.ENTITY_ITEM_PICKUP,
            Sound.BLOCK_CHEST_OPEN,
            Sound.BLOCK_CHEST_CLOSE,
            Sound.BLOCK_SMITHING_TABLE_USE
    };

    public enum SoundCategory {
        POPULAR("popular", Material.NETHER_STAR),
        ALL("all", Material.JUKEBOX),
        AMBIENT("ambient", Material.CAVE_AIR),
        BLOCK("block", Material.STONE),
        ENTITY("entity", Material.ZOMBIE_HEAD),
        MUSIC("music", Material.MUSIC_DISC_CAT),
        UI("ui", Material.PAINTING);

        private final String key;
        private final Material icon;

        SoundCategory(String key, Material icon) {
            this.key = key;
            this.icon = icon;
        }

        public String getKey() {
            return key;
        }

        public Material getIcon() {
            return icon;
        }
    }

    public SoundSelectorGUI(CustomRecipes plugin, Player player, CraftEventPreset preset, CraftEventPresetEditorGUI parentGUI) {
        this.plugin = plugin;
        this.player = player;
        this.preset = preset;
        this.parentGUI = parentGUI;
        this.lang = plugin.getLanguageManager();
        this.inventory = Bukkit.createInventory(
                null,
                54,
                MessageUtil.createGradientMenuTitle(lang.getMessage("craft_events.sounds_title"))
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
        addCategoryButtons();
        addSoundButtons();
        addNavigationButtons();
        addConfiguredSoundsButton();
        addBackButton();
    }

    private void fillBorders() {
        ItemStack borderPane = new ItemStack(Material.CYAN_STAINED_GLASS_PANE);
        ItemMeta meta = borderPane.getItemMeta();
        meta.displayName(Component.empty());
        borderPane.setItemMeta(meta);

        inventory.setItem(0, borderPane);
        inventory.setItem(8, borderPane);

        for (int i = 45; i < 54; i++) {
            inventory.setItem(i, borderPane);
        }

        for (int row = 1; row < 5; row++) {
            inventory.setItem(row * 9, borderPane);
            inventory.setItem(row * 9 + 8, borderPane);
        }
    }

    private void addCategoryButtons() {
        SoundCategory[] categories = SoundCategory.values();
        for (int i = 0; i < categories.length && i < CATEGORY_SLOTS.length; i++) {
            SoundCategory category = categories[i];
            ItemStack button = new ItemStack(category.getIcon());
            ItemMeta meta = button.getItemMeta();

            NamedTextColor color = category == currentCategory ? NamedTextColor.GREEN : NamedTextColor.GRAY;
            meta.displayName(Component.text(lang.getMessage("craft_events.category_" + category.getKey()), color)
                    .decoration(TextDecoration.BOLD, category == currentCategory)
                    .decoration(TextDecoration.ITALIC, false));

            List<Component> lore = new ArrayList<>();
            if (category == currentCategory) {
                lore.add(Component.text(lang.getMessage("info.selected"), NamedTextColor.GREEN)
                        .decoration(TextDecoration.ITALIC, false));
            } else {
                lore.add(Component.text("» " + lang.getMessage("lore.click_to_select"), NamedTextColor.YELLOW)
                        .decoration(TextDecoration.ITALIC, true));
            }

            meta.lore(lore);
            button.setItemMeta(meta);
            inventory.setItem(CATEGORY_SLOTS[i], button);
        }
    }

    private void addSoundButtons() {
        List<Sound> sounds = getSoundsForCategory(currentCategory);
        int start = currentPage * SOUNDS_PER_PAGE;
        int end = Math.min(start + SOUNDS_PER_PAGE, sounds.size());

        int[] slots = getSoundSlots();
        int slotIndex = 0;

        for (int i = start; i < end && slotIndex < slots.length; i++, slotIndex++) {
            Sound sound = sounds.get(i);
            addSoundButton(slots[slotIndex], sound);
        }
    }

    private int[] getSoundSlots() {
        return new int[]{
                10, 11, 12, 13, 14, 15, 16,
                19, 20, 21, 22, 23, 24, 25,
                28, 29, 30, 31, 32, 33, 34,
                37, 38, 39, 40, 41, 42, 43
        };
    }

    private void addSoundButton(int slot, Sound sound) {
        boolean isAdded = preset.getSounds().stream()
                .anyMatch(s -> s.getSound() == sound);

        ItemStack button = new ItemStack(isAdded ? Material.LIME_DYE : Material.NOTE_BLOCK);
        ItemMeta meta = button.getItemMeta();

        NamedTextColor nameColor = isAdded ? NamedTextColor.GREEN : NamedTextColor.AQUA;
        meta.displayName(Component.text(formatSoundName(sound), nameColor)
                .decoration(TextDecoration.BOLD, isAdded)
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text(sound.name(), NamedTextColor.DARK_GRAY)
                .decoration(TextDecoration.ITALIC, false));

        if (isAdded) {
            
            CraftEventPreset.SoundEvent soundEvent = preset.getSounds().stream()
                    .filter(s -> s.getSound() == sound)
                    .findFirst().orElse(null);

            if (soundEvent != null) {
                lore.add(Component.empty());
                lore.add(Component.text(lang.getMessage("craft_events.volume") + ": ", NamedTextColor.GRAY)
                        .append(Component.text(String.format("%.1f", soundEvent.getVolume()), NamedTextColor.YELLOW))
                        .decoration(TextDecoration.ITALIC, false));
                lore.add(Component.text(lang.getMessage("craft_events.pitch") + ": ", NamedTextColor.GRAY)
                        .append(Component.text(String.format("%.1f", soundEvent.getPitch()), NamedTextColor.YELLOW))
                        .decoration(TextDecoration.ITALIC, false));
            }

            lore.add(Component.empty());
            lore.add(Component.text("» " + lang.getMessage("craft_events.left_click_preview"), NamedTextColor.GREEN)
                    .decoration(TextDecoration.ITALIC, true));
            lore.add(Component.text("» " + lang.getMessage("craft_events.right_click_adjust"), NamedTextColor.YELLOW)
                    .decoration(TextDecoration.ITALIC, true));
            lore.add(Component.text("» " + lang.getMessage("craft_events.shift_click_remove"), NamedTextColor.RED)
                    .decoration(TextDecoration.ITALIC, true));
        } else {
            lore.add(Component.empty());
            lore.add(Component.text("» " + lang.getMessage("craft_events.left_click_preview"), NamedTextColor.GREEN)
                    .decoration(TextDecoration.ITALIC, true));
            lore.add(Component.text("» " + lang.getMessage("craft_events.right_click_add"), NamedTextColor.YELLOW)
                    .decoration(TextDecoration.ITALIC, true));
        }

        meta.lore(lore);
        button.setItemMeta(meta);
        inventory.setItem(slot, button);
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

    private List<Sound> getSoundsForCategory(SoundCategory category) {
        List<Sound> sounds = new ArrayList<>();

        switch (category) {
            case POPULAR -> {
                Collections.addAll(sounds, POPULAR_SOUNDS);
            }
            case ALL -> {
                Collections.addAll(sounds, Sound.values());
            }
            case AMBIENT -> {
                for (Sound sound : Sound.values()) {
                    if (sound.name().startsWith("AMBIENT_")) {
                        sounds.add(sound);
                    }
                }
            }
            case BLOCK -> {
                for (Sound sound : Sound.values()) {
                    if (sound.name().startsWith("BLOCK_")) {
                        sounds.add(sound);
                    }
                }
            }
            case ENTITY -> {
                for (Sound sound : Sound.values()) {
                    if (sound.name().startsWith("ENTITY_")) {
                        sounds.add(sound);
                    }
                }
            }
            case MUSIC -> {
                for (Sound sound : Sound.values()) {
                    if (sound.name().startsWith("MUSIC_")) {
                        sounds.add(sound);
                    }
                }
            }
            case UI -> {
                for (Sound sound : Sound.values()) {
                    if (sound.name().startsWith("UI_") || sound.name().startsWith("ITEM_")) {
                        sounds.add(sound);
                    }
                }
            }
        }

        return sounds;
    }

    private void addNavigationButtons() {
        List<Sound> sounds = getSoundsForCategory(currentCategory);
        int totalPages = (int) Math.ceil((double) sounds.size() / SOUNDS_PER_PAGE);

        if (currentPage > 0) {
            ItemStack prevButton = new ItemStack(Material.ARROW);
            ItemMeta meta = prevButton.getItemMeta();
            meta.displayName(Component.text(lang.getMessage("lore.previous_page"), NamedTextColor.YELLOW)
                    .decoration(TextDecoration.ITALIC, false));
            prevButton.setItemMeta(meta);
            inventory.setItem(PREV_PAGE_SLOT, prevButton);
        }

        ItemStack pageInfo = new ItemStack(Material.PAPER);
        ItemMeta meta = pageInfo.getItemMeta();
        meta.displayName(Component.text(lang.getMessage("lore.page_info")
                        .replace("{current}", String.valueOf(currentPage + 1))
                        .replace("{total}", String.valueOf(Math.max(1, totalPages))), NamedTextColor.WHITE)
                .decoration(TextDecoration.ITALIC, false));
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

    private void addConfiguredSoundsButton() {
        ItemStack button = new ItemStack(Material.BELL);
        ItemMeta meta = button.getItemMeta();
        meta.displayName(Component.text(lang.getMessage("craft_events.configured_sounds"), NamedTextColor.GOLD)
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text(lang.getMessage("craft_events.configured_count")
                        .replace("{count}", String.valueOf(preset.getSounds().size())), NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));

        if (!preset.getSounds().isEmpty()) {
            lore.add(Component.empty());
            int shown = 0;
            for (CraftEventPreset.SoundEvent sound : preset.getSounds()) {
                if (shown >= 5) {
                    lore.add(Component.text("  ..." + (preset.getSounds().size() - shown) + " more", NamedTextColor.DARK_GRAY)
                            .decoration(TextDecoration.ITALIC, false));
                    break;
                }
                lore.add(Component.text("  • " + formatSoundName(sound.getSound()), NamedTextColor.GREEN)
                        .decoration(TextDecoration.ITALIC, false));
                shown++;
            }
        }

        meta.lore(lore);
        button.setItemMeta(meta);
        inventory.setItem(CONFIGURED_SOUNDS_SLOT, button);
    }

    private void addBackButton() {
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta meta = back.getItemMeta();
        meta.displayName(Component.text(lang.getMessage("button.back"), NamedTextColor.YELLOW)
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
        ClickType clickType = event.getClick();

        if (slot == BACK_SLOT) {
            parentGUI.refreshAndOpen();
            return;
        }

        if (slot == PREV_PAGE_SLOT && currentPage > 0) {
            currentPage--;
            updateInventory();
            return;
        }

        if (slot == NEXT_PAGE_SLOT) {
            List<Sound> sounds = getSoundsForCategory(currentCategory);
            int totalPages = (int) Math.ceil((double) sounds.size() / SOUNDS_PER_PAGE);
            if (currentPage < totalPages - 1) {
                currentPage++;
                updateInventory();
            }
            return;
        }

        for (int i = 0; i < CATEGORY_SLOTS.length; i++) {
            if (slot == CATEGORY_SLOTS[i] && i < SoundCategory.values().length) {
                SoundCategory newCategory = SoundCategory.values()[i];
                if (newCategory != currentCategory) {
                    currentCategory = newCategory;
                    currentPage = 0;
                    updateInventory();
                }
                return;
            }
        }

        int[] soundSlots = getSoundSlots();
        for (int i = 0; i < soundSlots.length; i++) {
            if (slot == soundSlots[i]) {
                List<Sound> sounds = getSoundsForCategory(currentCategory);
                int soundIndex = currentPage * SOUNDS_PER_PAGE + i;

                if (soundIndex < sounds.size()) {
                    Sound sound = sounds.get(soundIndex);
                    handleSoundClick(sound, clickType);
                }
                return;
            }
        }
    }

    private void handleSoundClick(Sound sound, ClickType clickType) {
        CraftEventPreset.SoundEvent existing = preset.getSounds().stream()
                .filter(s -> s.getSound() == sound)
                .findFirst().orElse(null);

        if (clickType.isLeftClick() && !clickType.isShiftClick()) {
            
            float volume = existing != null ? existing.getVolume() : 1.0f;
            float pitch = existing != null ? existing.getPitch() : 1.0f;
            player.playSound(player.getLocation(), sound, volume, pitch);
            return;
        }

        if (clickType.isRightClick()) {
            if (existing != null) {
                
                SoundAdjustGUI adjustGUI = new SoundAdjustGUI(plugin, player, preset, existing, this);
                adjustGUI.open();
            } else {
                
                CraftEventPreset.SoundEvent newSound = new CraftEventPreset.SoundEvent(sound, 1.0f, 1.0f);
                preset.addSound(newSound);
                MessageUtil.sendInfo(player, lang.getMessage("craft_events.sound_added")
                        .replace("{sound}", formatSoundName(sound)));
                updateInventory();
            }
            return;
        }

        if (clickType.isShiftClick() && existing != null) {
            
            preset.removeSound(sound);
            MessageUtil.sendInfo(player, lang.getMessage("craft_events.sound_removed")
                    .replace("{sound}", formatSoundName(sound)));
            updateInventory();
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
