package org.hikarii.customrecipes.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.components.CustomModelDataComponent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.hikarii.customrecipes.CustomRecipes;
import org.hikarii.customrecipes.language.LanguageManager;
import org.hikarii.customrecipes.util.MessageUtil;

import java.util.*;
import java.util.function.Consumer;

public class ItemEditorGUI implements Listener {
    private static final Map<UUID, ItemEditorGUI> waitingForInput = new HashMap<>();
    private static final Map<UUID, ItemEditorGUI> lastEditors = new HashMap<>();
    private enum EditMode {
        NONE,
        NAME,
        LORE,
        CUSTOM_MODEL_DATA,
        NBT_KEY,
        NBT_VALUE
    }

    private final CustomRecipes plugin;
    private final LanguageManager lang;
    private final Player player;
    private final Inventory inventory;
    private ItemStack item;
    private final Consumer<ItemStack> onComplete;
    private Consumer<List<ItemStack>> onCompleteVariants;
    private String customName;
    private List<String> customLore;
    private String customModelData;
    private Map<String, String> customNBT;
    public Map<Enchantment, Integer> customEnchantments;
    public Set<ItemFlag> customFlags;
    public boolean noPlace = false;
    private EditMode currentMode = EditMode.NONE;
    private String tempNBTKey;
    public boolean hideEnchantments = false;
    public String craftEventPresetName = null;

    private List<ItemStack> variants;
    private int currentVariantIndex = 0;

    public static ItemEditorGUI getLastEditor(UUID playerUuid) {
        return lastEditors.get(playerUuid);
    }

    public ItemEditorGUI(CustomRecipes plugin, Player player, ItemStack item, Consumer<ItemStack> onComplete) {
        this(plugin, player, item, onComplete, null, null, 0);
    }

    public ItemEditorGUI(CustomRecipes plugin, Player player, List<ItemStack> variants, int variantIndex,
                         Consumer<List<ItemStack>> onCompleteVariants) {
        this(plugin, player, variants.get(variantIndex), null, variants, onCompleteVariants, variantIndex);
    }

    private ItemEditorGUI(CustomRecipes plugin, Player player, ItemStack item, Consumer<ItemStack> onComplete,
                          List<ItemStack> variants, Consumer<List<ItemStack>> onCompleteVariants, int variantIndex) {
        this.plugin = plugin;
        this.lang = plugin.getLanguageManager();
        lastEditors.put(player.getUniqueId(), this);
        this.player = player;
        this.item = item.clone();
        this.onComplete = onComplete;
        this.onCompleteVariants = onCompleteVariants;
        this.variants = variants != null ? new ArrayList<>(variants) : null;
        this.currentVariantIndex = variantIndex;
        this.customName = null;
        this.customLore = new ArrayList<>();
        this.customModelData = null;
        this.customNBT = new HashMap<>();
        this.customEnchantments = new HashMap<>();
        this.customFlags = new HashSet<>();
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (meta.hasDisplayName()) {
                customName = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                        .serialize(meta.displayName());
            }
            if (meta.hasLore() && meta.lore() != null) {
                for (Component line : meta.lore()) {
                    String loreText = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                            .serialize(line);
                    customLore.add(loreText);
                }
            }
            if (meta.hasCustomModelData()) {
                customModelData = meta.getCustomModelDataComponent().getStrings().get(0);
            }
            if (meta.hasEnchants()) {
                customEnchantments.putAll(meta.getEnchants());
            }
            
            if (item.getType() == Material.ENCHANTED_BOOK &&
                    meta instanceof org.bukkit.inventory.meta.EnchantmentStorageMeta bookMeta) {
                if (bookMeta.hasStoredEnchants()) {
                    customEnchantments.putAll(bookMeta.getStoredEnchants());
                }
            }
            if (meta.hasItemFlag(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS)) {
                hideEnchantments = true;
            }
            
            customFlags.addAll(meta.getItemFlags());
            
            PersistentDataContainer container = meta.getPersistentDataContainer();
            NamespacedKey noPlaceKey = new NamespacedKey(plugin, "no_place");
            if (container.has(noPlaceKey, PersistentDataType.BYTE)) {
                noPlace = container.get(noPlaceKey, PersistentDataType.BYTE) == 1;
            }
            
            NamespacedKey presetKey = new NamespacedKey(plugin, "craft_event_preset");
            if (container.has(presetKey, PersistentDataType.STRING)) {
                craftEventPresetName = container.get(presetKey, PersistentDataType.STRING);
            }
            for (NamespacedKey key : container.getKeys()) {
                if (key.getNamespace().equals(plugin.getName().toLowerCase())) {
                    
                    if (key.getKey().equals("no_place") || key.getKey().equals("craft_event_preset")) {
                        continue;
                    }
                    
                    try {
                        if (container.has(key, PersistentDataType.STRING)) {
                            String value = container.get(key, PersistentDataType.STRING);
                            if (value != null) {
                                customNBT.put(key.getKey(), value);
                            }
                        }
                    } catch (IllegalArgumentException ignored) {
                        
                    }
                }
            }
        }
        this.inventory = Bukkit.createInventory(
                null,
                54,
                MessageUtil.createGradientMenuTitle(lang.getMessage("gui.title.item_editor"))
        );
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        updateInventory();
    }

    public void open() {
        player.openInventory(inventory);
    }

    public void updateInventory() {
        inventory.clear();
        fillBorders();
        addPreviewItem();
        addVariantSwitcherButton();
        addNameButton();
        addLoreButton();
        addCustomModelDataButton();
        addNBTButton();
        addEnchantmentButton();
        addFlagsButton();
        addCraftEventsButton();
        addClearButton();
        addSaveButton();
        addCancelButton();
    }

    private void fillBorders() {
        ItemStack borderPane = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
        ItemMeta meta = borderPane.getItemMeta();
        meta.displayName(Component.empty());
        borderPane.setItemMeta(meta);
        for (int i = 0; i < 54; i++) {
            inventory.setItem(i, borderPane);
        }
    }

    private void addPreviewItem() {
        ItemStack preview = item.clone();
        ItemMeta meta = preview.getItemMeta();
        if (meta != null) {
            if (customName != null && !customName.isEmpty()) {
                meta.displayName(MessageUtil.colorize(customName)
                        .decoration(TextDecoration.ITALIC, false));
            }

            if (!customLore.isEmpty()) {
                List<Component> loreComponents = customLore.stream()
                        .map(line -> MessageUtil.colorize(line)
                                .decoration(TextDecoration.ITALIC, false))
                        .toList();
                meta.lore(loreComponents);
            }

            if (customModelData != null) {
                CustomModelDataComponent component = meta.getCustomModelDataComponent();

                component.setStrings(List.of(customModelData));

                meta.setCustomModelDataComponent(
                        component
                );
            }

            if (item.getType() == Material.ENCHANTED_BOOK &&
                    meta instanceof org.bukkit.inventory.meta.EnchantmentStorageMeta bookMeta) {
                for (Map.Entry<Enchantment, Integer> entry : customEnchantments.entrySet()) {
                    bookMeta.addStoredEnchant(entry.getKey(), entry.getValue(), true);
                }
            } else {
                for (Map.Entry<Enchantment, Integer> entry : customEnchantments.entrySet()) {
                    meta.addEnchant(entry.getKey(), entry.getValue(), true);
                }
            }

            if (hideEnchantments) {
                meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
            }

            for (ItemFlag flag : customFlags) {
                meta.addItemFlags(flag);
            }

            if (!customNBT.isEmpty()) {
                PersistentDataContainer container = meta.getPersistentDataContainer();
                for (Map.Entry<String, String> entry : customNBT.entrySet()) {
                    NamespacedKey key = new NamespacedKey(plugin, entry.getKey());
                    container.set(key, PersistentDataType.STRING, entry.getValue());
                }
            }

            if (noPlace) {
                PersistentDataContainer container = meta.getPersistentDataContainer();
                NamespacedKey noPlaceKey = new NamespacedKey(plugin, "no_place");
                container.set(noPlaceKey, PersistentDataType.BYTE, (byte) 1);
            }

            preview.setItemMeta(meta);
        }
        inventory.setItem(22, preview);
    }

    private void addVariantSwitcherButton() {
        
        if (variants == null || variants.size() <= 1) {
            return;
        }

        ItemStack button = new ItemStack(Material.CHEST);
        ItemMeta meta = button.getItemMeta();

        String variantName = currentVariantIndex == 0 ?
                lang.getMessage("item_editor.variant_default") :
                lang.getMessage("item_editor.variant_random", Map.of("index", String.valueOf(currentVariantIndex)));

        meta.displayName(Component.text(lang.getMessage("item_editor.variant_switcher"), NamedTextColor.AQUA)
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text(lang.getMessage("item_editor.current_variant") + " ", NamedTextColor.GRAY)
                .append(Component.text(variantName, NamedTextColor.YELLOW))
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("(" + (currentVariantIndex + 1) + "/" + variants.size() + ")", NamedTextColor.DARK_GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(Component.text("§a> §7" + lang.getMessage("item_editor.lmb_next"))
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("§c< §7" + lang.getMessage("item_editor.rmb_prev"))
                .decoration(TextDecoration.ITALIC, false));

        meta.lore(lore);
        button.setItemMeta(meta);
        inventory.setItem(13, button); 
    }

    private void addNameButton() {
        ItemStack button = new ItemStack(Material.NAME_TAG);
        ItemMeta meta = button.getItemMeta();
        meta.displayName(Component.text(lang.getMessage("item_editor_advanced.edit_name"), NamedTextColor.GOLD)
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        if (customName != null && !customName.isEmpty()) {
            lore.add(Component.text(lang.getMessage("item_editor_advanced.current"), NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(MessageUtil.colorize(customName)
                    .decoration(TextDecoration.ITALIC, false));
        } else {
            lore.add(Component.text(lang.getMessage("item_editor_advanced.no_name_set"), NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false));
        }
        lore.add(Component.empty());
        lore.add(Component.text(lang.getMessage("item_editor_advanced.click_to_edit"), NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, true));
        meta.lore(lore);
        button.setItemMeta(meta);
        inventory.setItem(11, button);
    }

    private void addLoreButton() {
        ItemStack button = new ItemStack(Material.WRITABLE_BOOK);
        ItemMeta meta = button.getItemMeta();
        meta.displayName(Component.text(lang.getMessage("item_editor_advanced.edit_description"), NamedTextColor.AQUA)
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        if (!customLore.isEmpty()) {
            lore.add(Component.text(lang.getMessage("item_editor_advanced.current_lines", Map.of("count", String.valueOf(customLore.size()))), NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false));
            for (int i = 0; i < Math.min(3, customLore.size()); i++) {
                lore.add(MessageUtil.colorize(customLore.get(i))
                        .decoration(TextDecoration.ITALIC, false));
            }
            if (customLore.size() > 3) {
                lore.add(Component.text(lang.getMessage("item_editor_advanced.and_more", Map.of("count", String.valueOf(customLore.size() - 3))), NamedTextColor.DARK_GRAY)
                        .decoration(TextDecoration.ITALIC, false));
            }
        } else {
            lore.add(Component.text(lang.getMessage("item_editor_advanced.no_description_set"), NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false));
        }
        lore.add(Component.empty());
        lore.add(Component.text(lang.getMessage("item_editor_advanced.left_click_add_line"), NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, true));
        lore.add(Component.text(lang.getMessage("item_editor_advanced.right_click_clear"), NamedTextColor.RED)
                .decoration(TextDecoration.ITALIC, true));
        meta.lore(lore);
        button.setItemMeta(meta);
        inventory.setItem(20, button);
    }

    private void addCustomModelDataButton() {
        ItemStack button = new ItemStack(Material.PAINTING);
        ItemMeta meta = button.getItemMeta();
        meta.displayName(Component.text(lang.getMessage("item_editor_advanced.custom_model_data_title"), NamedTextColor.LIGHT_PURPLE)
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        if (customModelData != null) {
            lore.add(Component.text(lang.getMessage("item_editor_advanced.current_value", Map.of("value", String.valueOf(customModelData))), NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false));
        } else {
            lore.add(Component.text(lang.getMessage("item_editor_advanced.no_custom_model_data"), NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false));
        }
        lore.add(Component.empty());
        lore.add(Component.text(lang.getMessage("item_editor_advanced.used_for_texture_packs"), NamedTextColor.DARK_GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(Component.text(lang.getMessage("item_editor_advanced.click_to_set"), NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, true));
        meta.lore(lore);
        button.setItemMeta(meta);
        inventory.setItem(15, button);
    }

    private void addNBTButton() {
        ItemStack button = new ItemStack(Material.COMMAND_BLOCK);
        ItemMeta meta = button.getItemMeta();
        meta.displayName(Component.text(lang.getMessage("item_editor_advanced.nbt_data"), NamedTextColor.DARK_AQUA)
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        if (!customNBT.isEmpty()) {
            lore.add(Component.text(lang.getMessage("item_editor_advanced.current_nbt_tags", Map.of("count", String.valueOf(customNBT.size()))), NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false));
            int shown = 0;
            for (Map.Entry<String, String> entry : customNBT.entrySet()) {
                if (shown++ >= 3) {
                    lore.add(Component.text(lang.getMessage("item_editor_advanced.and_more", Map.of("count", String.valueOf(customNBT.size() - 3))), NamedTextColor.DARK_GRAY)
                            .decoration(TextDecoration.ITALIC, false));
                    break;
                }
                lore.add(Component.text(lang.getMessage("item_editor_advanced.nbt_key_value", Map.of("key", entry.getKey(), "value", entry.getValue())), NamedTextColor.AQUA)
                        .decoration(TextDecoration.ITALIC, false));
            }
        } else {
            lore.add(Component.text(lang.getMessage("item_editor_advanced.no_nbt_data_set"), NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false));
        }
        lore.add(Component.empty());
        lore.add(Component.text(lang.getMessage("item_editor_advanced.click_to_add_nbt"), NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, true));
        meta.lore(lore);
        button.setItemMeta(meta);
        inventory.setItem(24, button);
    }

    private void addEnchantmentButton() {
        ItemStack button = new ItemStack(Material.ENCHANTING_TABLE);
        ItemMeta meta = button.getItemMeta();
        meta.displayName(Component.text(lang.getMessage("item_editor_advanced.enchantments_title"), NamedTextColor.LIGHT_PURPLE)
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        if (!customEnchantments.isEmpty()) {
            lore.add(Component.text(lang.getMessage("item_editor_advanced.current_enchantments", Map.of("count", String.valueOf(customEnchantments.size()))), NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false));
            int shown = 0;
            for (Map.Entry<Enchantment, Integer> entry : customEnchantments.entrySet()) {
                if (shown++ >= 3) {
                    lore.add(Component.text(lang.getMessage("item_editor_advanced.and_more", Map.of("count", String.valueOf(customEnchantments.size() - 3))),
                                    NamedTextColor.DARK_GRAY)
                            .decoration(TextDecoration.ITALIC, false));
                    break;
                }
                String enchName = entry.getKey().getKey().getKey();
                lore.add(Component.text(lang.getMessage("item_editor_advanced.enchantment_level", Map.of("name", enchName, "level", String.valueOf(entry.getValue()))), NamedTextColor.LIGHT_PURPLE)
                        .decoration(TextDecoration.ITALIC, false));
            }
        } else {
            lore.add(Component.text(lang.getMessage("item_editor_advanced.no_enchantments"), NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false));
        }
        lore.add(Component.empty());
        lore.add(Component.text(lang.getMessage("item_editor_advanced.click_to_open_selector"), NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, true));
        meta.lore(lore);
        button.setItemMeta(meta);
        inventory.setItem(29, button);
    }

    private void addFlagsButton() {
        ItemStack button = new ItemStack(Material.WHITE_BANNER);
        ItemMeta meta = button.getItemMeta();
        meta.displayName(Component.text(lang.getMessage("item_editor_advanced.flags_title"), NamedTextColor.DARK_GREEN)
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        int totalFlags = customFlags.size() + (noPlace ? 1 : 0);
        if (totalFlags > 0) {
            lore.add(Component.text(lang.getMessage("item_editor_advanced.current_flags", Map.of("count", String.valueOf(totalFlags))), NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false));
            int shown = 0;
            for (ItemFlag flag : customFlags) {
                if (shown++ >= 3) {
                    lore.add(Component.text(lang.getMessage("item_editor_advanced.and_more", Map.of("count", String.valueOf(totalFlags - 3))),
                                    NamedTextColor.DARK_GRAY)
                            .decoration(TextDecoration.ITALIC, false));
                    break;
                }
                lore.add(Component.text("• " + flag.name(), NamedTextColor.GREEN)
                        .decoration(TextDecoration.ITALIC, false));
            }
            if (noPlace && shown < 3) {
                lore.add(Component.text("• NO_PLACE", NamedTextColor.GREEN)
                        .decoration(TextDecoration.ITALIC, false));
            }
        } else {
            lore.add(Component.text(lang.getMessage("item_editor_advanced.no_flags"), NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false));
        }
        lore.add(Component.empty());
        lore.add(Component.text(lang.getMessage("item_editor_advanced.click_to_open_flags"), NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, true));
        meta.lore(lore);
        button.setItemMeta(meta);
        inventory.setItem(33, button);
    }

    private void addCraftEventsButton() {
        ItemStack button = new ItemStack(Material.BELL);
        ItemMeta meta = button.getItemMeta();
        meta.displayName(Component.text(lang.getMessage("item_editor_advanced.craft_events_title"), NamedTextColor.GOLD)
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text(lang.getMessage("item_editor_advanced.craft_events_desc1"), NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text(lang.getMessage("item_editor_advanced.craft_events_desc2"), NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());

        if (craftEventPresetName != null && !craftEventPresetName.isEmpty()) {
            lore.add(Component.text(lang.getMessage("info.current") + ": ", NamedTextColor.GRAY)
                    .append(Component.text(craftEventPresetName, NamedTextColor.GREEN))
                    .decoration(TextDecoration.ITALIC, false));
        } else {
            lore.add(Component.text(lang.getMessage("item_editor_advanced.no_preset_set"), NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false));
        }

        lore.add(Component.empty());
        lore.add(Component.text(lang.getMessage("item_editor_advanced.click_to_select_preset"), NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, true));
        meta.lore(lore);
        button.setItemMeta(meta);
        inventory.setItem(38, button);
    }

    private void addClearButton() {
        ItemStack button = new ItemStack(Material.BARRIER);
        ItemMeta meta = button.getItemMeta();
        meta.displayName(Component.text(lang.getMessage("item_editor_advanced.clear_all"), NamedTextColor.RED)
                .decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text(lang.getMessage("item_editor_advanced.clear_all_customization"), NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(Component.text(lang.getMessage("item_editor_advanced.click_to_clear"), NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, true));
        meta.lore(lore);
        button.setItemMeta(meta);
        inventory.setItem(53, button);
    }

    private void addSaveButton() {
        ItemStack button = new ItemStack(Material.LIME_WOOL);
        ItemMeta meta = button.getItemMeta();
        meta.displayName(Component.text(lang.getMessage("item_editor_advanced.save_and_return"), NamedTextColor.GREEN)
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text(lang.getMessage("item_editor_advanced.apply_changes_and"), NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text(lang.getMessage("item_editor_advanced.return_to_creator"), NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(Component.text(lang.getMessage("item_editor_advanced.click_to_save"), NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, true));
        meta.lore(lore);
        button.setItemMeta(meta);
        inventory.setItem(48, button);
    }

    private void addCancelButton() {
        ItemStack button = new ItemStack(Material.RED_WOOL);
        ItemMeta meta = button.getItemMeta();
        meta.displayName(Component.text(lang.getMessage("item_editor_advanced.cancel_button"), NamedTextColor.RED)
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text(lang.getMessage("item_editor_advanced.discard_changes"), NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(Component.text(lang.getMessage("item_editor_advanced.click_to_cancel"), NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, true));
        meta.lore(lore);
        button.setItemMeta(meta);
        inventory.setItem(50, button);
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

        if (slot == 13 && variants != null && variants.size() > 1) {
            
            saveCurrentVariant();

            if (event.getClick().isLeftClick()) {
                currentVariantIndex = (currentVariantIndex + 1) % variants.size();
            } else if (event.getClick().isRightClick()) {
                currentVariantIndex = (currentVariantIndex - 1 + variants.size()) % variants.size();
            }

            loadVariant(currentVariantIndex);
            updateInventory();
            return;
        }

        if (slot == 11) {
            currentMode = EditMode.NAME;
            waitingForInput.put(player.getUniqueId(), this);
            player.closeInventory();
            MessageUtil.sendAdminInfo(player, lang.getMessage("item_editor_advanced.type_item_name"));
            MessageUtil.sendAdminInfo(player, lang.getMessage("item_editor_advanced.type_cancel_to_cancel"));
            return;
        }

        if (slot == 20) {
            if (event.getClick().isRightClick()) {
                customLore.clear();
                MessageUtil.sendAdminSuccess(player, lang.getMessage("item_editor_advanced.cleared_description"));
                updateInventory();
                return;
            }

            currentMode = EditMode.LORE;
            waitingForInput.put(player.getUniqueId(), this);
            player.closeInventory();
            MessageUtil.sendAdminInfo(player, lang.getMessage("item_editor_advanced.type_description_line"));
            MessageUtil.sendAdminInfo(player, lang.getMessage("item_editor_advanced.type_done_or_cancel"));
            return;
        }

        if (slot == 15) {
            currentMode = EditMode.CUSTOM_MODEL_DATA;
            waitingForInput.put(player.getUniqueId(), this);
            player.closeInventory();
            MessageUtil.sendAdminInfo(player, lang.getMessage("item_editor_advanced.type_custom_model_data"));
            MessageUtil.sendAdminInfo(player, lang.getMessage("item_editor_advanced.type_cancel_or_clear"));
            return;
        }

        if (slot == 24) {
            currentMode = EditMode.NBT_KEY;
            waitingForInput.put(player.getUniqueId(), this);
            player.closeInventory();
            MessageUtil.sendAdminInfo(player, lang.getMessage("item_editor_advanced.type_nbt_key"));
            MessageUtil.sendAdminInfo(player, lang.getMessage("item_editor_advanced.type_cancel_to_cancel"));
            return;
        }

        if (slot == 29) {
            player.closeInventory();
            String savedName = customName;
            List<String> savedLore = new ArrayList<>(customLore);
            String savedModelData = customModelData;
            Map<String, String> savedNBT = new HashMap<>(customNBT);
            Map<Enchantment, Integer> savedEnchants = new HashMap<>(customEnchantments);
            boolean savedHide = hideEnchantments;
            
            List<ItemStack> savedVariants = variants != null ? new ArrayList<>(variants) : null;
            int savedVariantIndex = currentVariantIndex;
            Consumer<List<ItemStack>> savedOnCompleteVariants = onCompleteVariants;
            Bukkit.getScheduler().runTask(plugin, () -> {
                new EnchantmentSelectorGUI(plugin, player, item,
                        savedEnchants,
                        savedHide,
                        (v) -> {
                            ItemEditorGUI editor = ItemEditorGUI.getLastEditor(player.getUniqueId());
                            ItemEditorGUI newEditor = new ItemEditorGUI(plugin, player, item, onComplete);
                            newEditor.customName = savedName;
                            newEditor.customLore = savedLore;
                            newEditor.customModelData = savedModelData;
                            newEditor.customNBT = savedNBT;
                            
                            newEditor.variants = savedVariants;
                            newEditor.currentVariantIndex = savedVariantIndex;
                            newEditor.onCompleteVariants = savedOnCompleteVariants;

                            if (editor != null) {
                                newEditor.customEnchantments.clear();
                                newEditor.customEnchantments.putAll(editor.customEnchantments);
                                newEditor.hideEnchantments = editor.hideEnchantments;
                            }
                            newEditor.updateInventory();
                            newEditor.open();
                        }).open();
            });
            return;
        }

        if (slot == 33) {
            player.closeInventory();
            String savedName = customName;
            List<String> savedLore = new ArrayList<>(customLore);
            String savedModelData = customModelData;
            Map<String, String> savedNBT = new HashMap<>(customNBT);
            Map<Enchantment, Integer> savedEnchants = new HashMap<>(customEnchantments);
            Set<ItemFlag> savedFlags = new HashSet<>(customFlags);
            boolean savedHide = hideEnchantments;
            boolean savedNoPlace = noPlace;
            
            List<ItemStack> savedVariants = variants != null ? new ArrayList<>(variants) : null;
            int savedVariantIndex = currentVariantIndex;
            Consumer<List<ItemStack>> savedOnCompleteVariants = onCompleteVariants;
            Bukkit.getScheduler().runTask(plugin, () -> {
                new FlagsSelectorGUI(plugin, player, item,
                        savedFlags,
                        savedNoPlace,
                        (v) -> {
                            ItemEditorGUI editor = ItemEditorGUI.getLastEditor(player.getUniqueId());
                            ItemEditorGUI newEditor = new ItemEditorGUI(plugin, player, item, onComplete);
                            newEditor.customName = savedName;
                            newEditor.customLore = savedLore;
                            newEditor.customModelData = savedModelData;
                            newEditor.customNBT = savedNBT;
                            newEditor.customEnchantments.clear();
                            newEditor.customEnchantments.putAll(savedEnchants);
                            newEditor.hideEnchantments = savedHide;
                            
                            newEditor.variants = savedVariants;
                            newEditor.currentVariantIndex = savedVariantIndex;
                            newEditor.onCompleteVariants = savedOnCompleteVariants;

                            if (editor != null) {
                                newEditor.customFlags.clear();
                                newEditor.customFlags.addAll(editor.customFlags);
                                newEditor.noPlace = editor.noPlace;
                            }
                            newEditor.updateInventory();
                            newEditor.open();
                        }).open();
            });
            return;
        }

        if (slot == 38) {
            player.closeInventory();
            String savedName = customName;
            List<String> savedLore = new ArrayList<>(customLore);
            String savedModelData = customModelData;
            Map<String, String> savedNBT = new HashMap<>(customNBT);
            Map<Enchantment, Integer> savedEnchants = new HashMap<>(customEnchantments);
            Set<ItemFlag> savedFlags = new HashSet<>(customFlags);
            boolean savedHide = hideEnchantments;
            boolean savedNoPlace = noPlace;
            String savedPreset = craftEventPresetName;
            
            List<ItemStack> savedVariants = variants != null ? new ArrayList<>(variants) : null;
            int savedVariantIndex = currentVariantIndex;
            Consumer<List<ItemStack>> savedOnCompleteVariants = onCompleteVariants;
            Bukkit.getScheduler().runTask(plugin, () -> {
                new CraftEventPresetSelectorGUI(plugin, player, savedPreset, true, (selectedPreset) -> {
                    ItemEditorGUI newEditor = new ItemEditorGUI(plugin, player, item, onComplete);
                    newEditor.customName = savedName;
                    newEditor.customLore = savedLore;
                    newEditor.customModelData = savedModelData;
                    newEditor.customNBT = savedNBT;
                    newEditor.customEnchantments.clear();
                    newEditor.customEnchantments.putAll(savedEnchants);
                    newEditor.customFlags.clear();
                    newEditor.customFlags.addAll(savedFlags);
                    newEditor.hideEnchantments = savedHide;
                    newEditor.noPlace = savedNoPlace;
                    
                    newEditor.variants = savedVariants;
                    newEditor.currentVariantIndex = savedVariantIndex;
                    newEditor.onCompleteVariants = savedOnCompleteVariants;

                    if (selectedPreset == null) {
                        
                        newEditor.craftEventPresetName = savedPreset;
                    } else if (selectedPreset.isEmpty()) {
                        
                        newEditor.craftEventPresetName = null;
                    } else {
                        
                        newEditor.craftEventPresetName = selectedPreset;
                    }

                    newEditor.updateInventory();
                    newEditor.open();
                }).open();
            });
            return;
        }

        if (slot == 53) {
            customName = null;
            customLore.clear();
            customModelData = null;
            customNBT.clear();
            customEnchantments.clear();
            customFlags.clear();
            noPlace = false;
            hideEnchantments = false;
            craftEventPresetName = null;
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.displayName(null);
                meta.lore(null);
                meta.setCustomModelData(null);
                for (Enchantment ench : meta.getEnchants().keySet()) {
                    meta.removeEnchant(ench);
                }
                PersistentDataContainer container = meta.getPersistentDataContainer();
                for (NamespacedKey key : new HashSet<>(container.getKeys())) {
                    if (key.getNamespace().equals(plugin.getName().toLowerCase())) {
                        container.remove(key);
                    }
                }
                item.setItemMeta(meta);
            }
            MessageUtil.sendAdminSuccess(player, lang.getMessage("item_editor_advanced.cleared_all_customization"));
            updateInventory();
            return;
        }
        if (slot == 48) {
            applyToItem();
            MessageUtil.sendAdminSuccess(player, lang.getMessage("item_editor_advanced.saved_item_customization"));
            player.closeInventory();

            if (variants != null && onCompleteVariants != null) {
                saveCurrentVariant();
                Bukkit.getScheduler().runTask(plugin, () -> onCompleteVariants.accept(new ArrayList<>(variants)));
            } else if (onComplete != null) {
                Bukkit.getScheduler().runTask(plugin, () -> onComplete.accept(getEditedItem()));
            }
            return;
        }
        if (slot == 50) {
            player.closeInventory();
            
            if (onCompleteVariants != null) {
                Bukkit.getScheduler().runTask(plugin, () -> onCompleteVariants.accept(null));
            } else if (onComplete != null) {
                Bukkit.getScheduler().runTask(plugin, () -> onComplete.accept(null));
            }
            return;
        }
    }

    private void saveCurrentVariant() {
        if (variants == null || currentVariantIndex >= variants.size()) {
            return;
        }
        applyToItem();
        variants.set(currentVariantIndex, getEditedItem());
    }

    private void loadVariant(int index) {
        if (variants == null || index >= variants.size()) {
            return;
        }

        ItemStack newItem = variants.get(index);
        this.item = newItem.clone();

        this.customName = null;
        this.customLore = new ArrayList<>();
        this.customModelData = null;
        this.customNBT = new HashMap<>();
        this.customEnchantments = new HashMap<>();
        this.customFlags = new HashSet<>();
        this.noPlace = false;
        this.hideEnchantments = false;
        this.craftEventPresetName = null;

        ItemMeta meta = newItem.getItemMeta();
        if (meta != null) {
            if (meta.hasDisplayName()) {
                customName = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                        .serialize(meta.displayName());
            }
            if (meta.hasLore() && meta.lore() != null) {
                for (Component line : meta.lore()) {
                    String loreText = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                            .serialize(line);
                    customLore.add(loreText);
                }
            }
            if (meta.hasCustomModelData()) {
                customModelData = meta.getCustomModelDataComponent().getStrings().get(0);
            }
            if (meta.hasEnchants()) {
                customEnchantments.putAll(meta.getEnchants());
            }
            
            if (newItem.getType() == Material.ENCHANTED_BOOK &&
                    meta instanceof org.bukkit.inventory.meta.EnchantmentStorageMeta bookMeta) {
                if (bookMeta.hasStoredEnchants()) {
                    customEnchantments.putAll(bookMeta.getStoredEnchants());
                }
            }
            customFlags.addAll(meta.getItemFlags());

            PersistentDataContainer container = meta.getPersistentDataContainer();
            for (NamespacedKey key : container.getKeys()) {
                if (key.getNamespace().equals(plugin.getName().toLowerCase())) {
                    if (key.getKey().equals("no_place")) {
                        noPlace = container.has(key, PersistentDataType.BYTE);
                    } else if (key.getKey().equals("craft_event_preset")) {
                        try {
                            if (container.has(key, PersistentDataType.STRING)) {
                                craftEventPresetName = container.get(key, PersistentDataType.STRING);
                            }
                        } catch (Exception ignored) {}
                    } else {
                        try {
                            if (container.has(key, PersistentDataType.STRING)) {
                                String value = container.get(key, PersistentDataType.STRING);
                                if (value != null) {
                                    customNBT.put(key.getKey(), value);
                                }
                            }
                        } catch (Exception ignored) {}
                    }
                }
            }
        }
    }

    private void applyToItem() {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (customName != null && !customName.isEmpty()) {
                meta.displayName(MessageUtil.colorize(customName)
                        .decoration(TextDecoration.ITALIC, false));
            }

            if (!customLore.isEmpty()) {
                List<Component> loreComponents = customLore.stream()
                        .map(line -> MessageUtil.colorize(line)
                                .decoration(TextDecoration.ITALIC, false))
                        .toList();
                meta.lore(loreComponents);
            }

            if (customModelData != null) {
                CustomModelDataComponent component = meta.getCustomModelDataComponent();

                component.setStrings(List.of(customModelData));

                meta.setCustomModelDataComponent(
                        component
                );
            }

            if (item.getType() == Material.ENCHANTED_BOOK &&
                    meta instanceof org.bukkit.inventory.meta.EnchantmentStorageMeta bookMeta) {
                for (Map.Entry<Enchantment, Integer> entry : customEnchantments.entrySet()) {
                    bookMeta.addStoredEnchant(entry.getKey(), entry.getValue(), true);
                }
            } else {
                for (Map.Entry<Enchantment, Integer> entry : customEnchantments.entrySet()) {
                    meta.addEnchant(entry.getKey(), entry.getValue(), true);
                }
            }

            if (hideEnchantments) {
                meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
            } else {
                meta.removeItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
            }

            for (ItemFlag flag : customFlags) {
                meta.addItemFlags(flag);
            }

            PersistentDataContainer container = meta.getPersistentDataContainer();

            if (!customNBT.isEmpty()) {
                for (Map.Entry<String, String> entry : customNBT.entrySet()) {
                    NamespacedKey key = new NamespacedKey(plugin, entry.getKey());
                    container.set(key, PersistentDataType.STRING, entry.getValue());
                }
            }

            NamespacedKey noPlaceKey = new NamespacedKey(plugin, "no_place");
            if (noPlace) {
                container.set(noPlaceKey, PersistentDataType.BYTE, (byte) 1);
            } else {
                container.remove(noPlaceKey);
            }

            NamespacedKey presetKey = new NamespacedKey(plugin, "craft_event_preset");
            plugin.getLogger().info("[DEBUG ItemEditor] Saving item, craftEventPresetName = '" + craftEventPresetName + "'");
            if (craftEventPresetName != null && !craftEventPresetName.isEmpty()) {
                container.set(presetKey, PersistentDataType.STRING, craftEventPresetName);
                plugin.getLogger().info("[DEBUG ItemEditor] Set craft_event_preset PDC key to: " + craftEventPresetName);
            } else {
                container.remove(presetKey);
                plugin.getLogger().info("[DEBUG ItemEditor] Removed craft_event_preset PDC key");
            }

            item.setItemMeta(meta);
            plugin.getLogger().info("[DEBUG ItemEditor] After save, PDC keys: " +
                item.getItemMeta().getPersistentDataContainer().getKeys());
        }
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player chatPlayer = event.getPlayer();
        ItemEditorGUI editor = waitingForInput.get(chatPlayer.getUniqueId());
        if (editor == null || !chatPlayer.equals(player)) {
            return;
        }

        event.setCancelled(true);
        String message = event.getMessage();
        if (message.equalsIgnoreCase("cancel")) {
            waitingForInput.remove(chatPlayer.getUniqueId());
            currentMode = EditMode.NONE;
            Bukkit.getScheduler().runTask(plugin, () -> {
                MessageUtil.sendAdminWarning(chatPlayer, lang.getMessage("item_editor_advanced.cancelled"));
                editor.open();
            });
            return;
        }
        switch (currentMode) {
            case NAME -> {
                editor.customName = message;
                waitingForInput.remove(chatPlayer.getUniqueId());
                currentMode = EditMode.NONE;
                Bukkit.getScheduler().runTask(plugin, () -> {
                    MessageUtil.sendAdminSuccess(chatPlayer, lang.getMessage("item_editor_advanced.set_item_name"));
                    editor.updateInventory();
                    editor.open();
                });
            }
            case LORE -> {
                if (message.equalsIgnoreCase("done")) {
                    waitingForInput.remove(chatPlayer.getUniqueId());
                    currentMode = EditMode.NONE;
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        MessageUtil.sendAdminSuccess(chatPlayer, lang.getMessage("item_editor_advanced.finished_editing_description"));
                        editor.updateInventory();
                        editor.open();
                    });
                } else {
                    editor.customLore.add(message);
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        MessageUtil.sendAdminSuccess(chatPlayer, lang.getMessage("item_editor_advanced.added_description_line"));
                        MessageUtil.sendAdminInfo(chatPlayer, lang.getMessage("item_editor_advanced.type_another_line"));
                    });
                }
            }
            case CUSTOM_MODEL_DATA -> {
                if (message.equalsIgnoreCase("clear")) {
                    editor.customModelData = null;
                    waitingForInput.remove(chatPlayer.getUniqueId());
                    currentMode = EditMode.NONE;
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        MessageUtil.sendAdminSuccess(chatPlayer, lang.getMessage("item_editor_advanced.cleared_custom_model_data"));
                        editor.updateInventory();
                        editor.open();
                    });
                } else {
                        editor.customModelData = message;
                        waitingForInput.remove(chatPlayer.getUniqueId());
                        currentMode = EditMode.NONE;
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            MessageUtil.sendAdminSuccess(chatPlayer, lang.getMessage("item_editor_advanced.set_custom_model_data", Map.of("value", String.valueOf(customModelData))));
                            editor.updateInventory();
                            editor.open();
                        });
                }
            }
            case NBT_KEY -> {
                editor.tempNBTKey = message;
                currentMode = EditMode.NBT_VALUE;
                Bukkit.getScheduler().runTask(plugin, () -> {
                    MessageUtil.sendAdminInfo(chatPlayer, lang.getMessage("item_editor_advanced.type_nbt_value", Map.of("key", message)));
                });
            }
            case NBT_VALUE -> {
                editor.customNBT.put(editor.tempNBTKey, message);
                waitingForInput.remove(chatPlayer.getUniqueId());
                currentMode = EditMode.NONE;
                Bukkit.getScheduler().runTask(plugin, () -> {
                    MessageUtil.sendAdminSuccess(chatPlayer, lang.getMessage("item_editor_advanced.added_nbt", Map.of("key", editor.tempNBTKey, "value", message)));
                    editor.updateInventory();
                    editor.open();
                });
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().equals(inventory) && event.getPlayer().equals(player)) {
            if (!waitingForInput.containsKey(player.getUniqueId())) {
                InventoryClickEvent.getHandlerList().unregister(this);
                InventoryCloseEvent.getHandlerList().unregister(this);
                AsyncPlayerChatEvent.getHandlerList().unregister(this);
            }
        }
    }

    public String getCustomName() {
        return customName;
    }

    public List<String> getCustomLore() {
        return customLore;
    }

    public String getCustomModelData() {
        return customModelData;
    }

    public Map<String, String> getCustomNBT() {
        return customNBT;
    }

    public ItemStack getEditedItem() {
        return item.clone();
    }

    public void forceUpdatePreview() {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            
            for (Enchantment ench : new HashSet<>(meta.getEnchants().keySet())) {
                meta.removeEnchant(ench);
            }
            
            if (item.getType() == Material.ENCHANTED_BOOK &&
                    meta instanceof org.bukkit.inventory.meta.EnchantmentStorageMeta bookMeta) {
                for (Enchantment ench : new HashSet<>(bookMeta.getStoredEnchants().keySet())) {
                    bookMeta.removeStoredEnchant(ench);
                }
                
                for (Map.Entry<Enchantment, Integer> entry : customEnchantments.entrySet()) {
                    bookMeta.addStoredEnchant(entry.getKey(), entry.getValue(), true);
                }
            } else {
                for (Map.Entry<Enchantment, Integer> entry : customEnchantments.entrySet()) {
                    meta.addEnchant(entry.getKey(), entry.getValue(), true);
                }
            }

            if (hideEnchantments) {
                meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
            } else {
                meta.removeItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
            }
            item.setItemMeta(meta);
        }
        updateInventory();
    }
}