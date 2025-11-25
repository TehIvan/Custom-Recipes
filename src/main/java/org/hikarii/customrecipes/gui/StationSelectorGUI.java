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
import java.util.Map;

public class StationSelectorGUI implements Listener {
    private final CustomRecipes plugin;
    private final LanguageManager lang;
    private final Player player;
    private final Inventory inventory;

    public enum RecipeStation {
        CRAFTING_TABLE("Crafting Table", Material.CRAFTING_TABLE, true, 9,
                "Create shaped or shapeless recipes", "Standard 3x3 crafting grid"),

        FURNACE("Furnace", Material.FURNACE, true, 2,
                "Smelt items with fuel", "Standard furnace smelting"),

        BLAST_FURNACE("Blast Furnace", Material.BLAST_FURNACE, false, 2,
                "Faster ore smelting", "Coming soon!"),

        SMOKER("Smoker", Material.SMOKER, false, 2,
                "Faster food cooking", "Coming soon!"),

        CAMPFIRE("Campfire", Material.CAMPFIRE, false, 1,
                "Cook food without fuel", "Coming soon!"),

        STONECUTTER("Stonecutter", Material.STONECUTTER, false, 1,
                "Cut stone blocks", "Coming soon!"),

        BREWING_STAND("Brewing Stand", Material.BREWING_STAND, false, 4,
                "Brew potions", "Coming soon!"),

        SMITHING_TABLE("Smithing Table", Material.SMITHING_TABLE, false, 3,
                "Upgrade equipment", "Coming soon!"),

        ANVIL("Anvil", Material.ANVIL, false, 2,
                "Combine and rename items", "Coming soon!"),

        LOOM("Loom", Material.LOOM, false, 3,
                "Create banners", "Coming soon!");

        private final String displayName;
        private final Material icon;
        private final boolean enabled;
        private final int gridSize;
        private final String description;
        private final String statusMessage;

        RecipeStation(String displayName, Material icon, boolean enabled, int gridSize,
                      String description, String statusMessage) {
            this.displayName = displayName;
            this.icon = icon;
            this.enabled = enabled;
            this.gridSize = gridSize;
            this.description = description;
            this.statusMessage = statusMessage;
        }

        public String getDisplayName() {
            return displayName;
        }

        public Material getIcon() {
            return icon;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public int getGridSize() {
            return gridSize;
        }

        public String getDescription() {
            return description;
        }

        public String getStatusMessage() {
            return statusMessage;
        }
    }

    public StationSelectorGUI(CustomRecipes plugin, Player player) {
        this.plugin = plugin;
        this.lang = plugin.getLanguageManager();
        this.player = player;
        this.inventory = Bukkit.createInventory(
                null,
                54,
                MessageUtil.createGradientMenuTitle(lang.getMessage("station_selector.title"))
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
        addStationButtons();
        addInfoItem();
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
    private void addStationButtons() {
        int[] slots = {10, 12, 14, 16, 19, 21, 23, 25, 28, 30};
        RecipeStation[] stations = RecipeStation.values();
        for (int i = 0; i < Math.min(stations.length, slots.length); i++) {
            RecipeStation station = stations[i];
            ItemStack button = createStationButton(station);
            inventory.setItem(slots[i], button);
        }
    }

    private ItemStack createStationButton(RecipeStation station) {
        ItemStack item = new ItemStack(station.getIcon());
        ItemMeta meta = item.getItemMeta();

        NamedTextColor nameColor = station.isEnabled() ? NamedTextColor.AQUA : NamedTextColor.DARK_GRAY;
        meta.displayName(Component.text(getStationName(station), nameColor)
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text(getStationDescription(station), NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(Component.text(lang.getMessage("station_selector.grid_size") + lang.getMessage("station_selector.slots", Map.of("count", String.valueOf(station.getGridSize()), "plural", station.getGridSize() > 1 ? "s" : "")), NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        if (station.isEnabled()) {
            lore.add(Component.text(lang.getMessage("station_selector.available"), NamedTextColor.GREEN)
                    .decoration(TextDecoration.BOLD, true)
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.empty());
            lore.add(Component.text(lang.getMessage("station_selector.click_to_create_recipe"), NamedTextColor.YELLOW)
                    .decoration(TextDecoration.ITALIC, true));
        } else {
            lore.add(Component.text(getStationStatusMessage(station), NamedTextColor.RED)
                    .decoration(TextDecoration.BOLD, true)
                    .decoration(TextDecoration.ITALIC, false));
        }
        meta.lore(lore);
        if (station.isEnabled()) {
            org.bukkit.enchantments.Enchantment unbreaking = org.bukkit.enchantments.Enchantment.getByKey(org.bukkit.NamespacedKey.minecraft("unbreaking"));
            if (unbreaking != null) {
                meta.addEnchant(unbreaking, 1, true);
                meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
            }
        }
        item.setItemMeta(meta);
        return item;
    }

    private void addInfoItem() {
        ItemStack info = new ItemStack(Material.BOOK);
        ItemMeta meta = info.getItemMeta();
        meta.displayName(Component.text(lang.getMessage("station_selector.crafting_stations"), NamedTextColor.GOLD)
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text(lang.getMessage("station_selector.select_a_station"), NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text(lang.getMessage("station_selector.to_create_recipe"), NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        int enabledCount = 0;
        int totalCount = RecipeStation.values().length;
        for (RecipeStation station : RecipeStation.values()) {
            if (station.isEnabled()) enabledCount++;
        }
        lore.add(Component.text(lang.getMessage("station_selector.available_count", Map.of("enabled", String.valueOf(enabledCount), "total", String.valueOf(totalCount))), NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(Component.text(lang.getMessage("station_selector.more_coming_soon"), NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);
        info.setItemMeta(meta);
        inventory.setItem(49, info);
    }

    private void addBackButton() {
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta meta = back.getItemMeta();
        meta.displayName(Component.text(lang.getMessage("station_selector.back_to_recipe_list"), NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
                Component.empty(),
                Component.text(lang.getMessage("station_selector.return_to_main_menu"), NamedTextColor.GRAY)
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

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) {
            return;
        }

        int slot = event.getSlot();
        if (!player.hasPermission("customrecipes.manage")) {
            MessageUtil.sendError(player, lang.getMessage("station_selector.no_permission_create"));
            return;
        }

        if (slot == 53) {
            new RecipeListGUI(plugin, player).open();
            return;
        }

        int[] stationSlots = {10, 12, 14, 16, 19, 21, 23, 25, 28, 30};
        for (int i = 0; i < stationSlots.length; i++) {
            if (slot == stationSlots[i] && i < RecipeStation.values().length) {
                RecipeStation station = RecipeStation.values()[i];
                handleStationClick(station);
                return;
            }
        }
    }

    private void handleStationClick(RecipeStation station) {
        if (!station.isEnabled()) {
            MessageUtil.sendAdminWarning(player, lang.getMessage("station_selector.not_yet_available", Map.of("station", getStationName(station))));
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 0.5f, 1.0f);
            return;
        }
        switch (station) {
            case CRAFTING_TABLE -> {
                new RecipeCreatorGUI(plugin, player).open();
                MessageUtil.sendAdminSuccess(player, lang.getMessage("station_selector.opening_creator"));
            }
            case FURNACE -> {
                new FurnaceRecipeCreatorGUI(plugin, player).open();
                MessageUtil.sendAdminSuccess(player, lang.getMessage("station_selector.opening_furnace_creator"));
            }
            case BLAST_FURNACE, SMOKER, CAMPFIRE, STONECUTTER, BREWING_STAND, SMITHING_TABLE, ANVIL, LOOM -> {
                MessageUtil.sendAdminWarning(player, lang.getMessage("station_selector.coming_soon_message", Map.of("station", getStationName(station))));
            }
            default -> {
                MessageUtil.sendError(player, lang.getMessage("station_selector.unknown_station"));
            }
        }
    }

    private String getStationName(RecipeStation station) {
        return switch (station) {
            case CRAFTING_TABLE -> lang.getMessage("station_selector.crafting_table_name");
            case FURNACE -> lang.getMessage("station_selector.furnace_name");
            case BLAST_FURNACE -> lang.getMessage("station_selector.blast_furnace_name");
            case SMOKER -> lang.getMessage("station_selector.smoker_name");
            case CAMPFIRE -> lang.getMessage("station_selector.campfire_name");
            case STONECUTTER -> lang.getMessage("station_selector.stonecutter_name");
            case BREWING_STAND -> lang.getMessage("station_selector.brewing_stand_name");
            case SMITHING_TABLE -> lang.getMessage("station_selector.smithing_table_name");
            case ANVIL -> lang.getMessage("station_selector.anvil_name");
            case LOOM -> lang.getMessage("station_selector.loom_name");
        };
    }

    private String getStationDescription(RecipeStation station) {
        return switch (station) {
            case CRAFTING_TABLE -> lang.getMessage("station_selector.crafting_table_desc");
            case FURNACE -> lang.getMessage("station_selector.furnace_desc");
            case BLAST_FURNACE -> lang.getMessage("station_selector.blast_furnace_desc");
            case SMOKER -> lang.getMessage("station_selector.smoker_desc");
            case CAMPFIRE -> lang.getMessage("station_selector.campfire_desc");
            case STONECUTTER -> lang.getMessage("station_selector.stonecutter_desc");
            case BREWING_STAND -> lang.getMessage("station_selector.brewing_stand_desc");
            case SMITHING_TABLE -> lang.getMessage("station_selector.smithing_table_desc");
            case ANVIL -> lang.getMessage("station_selector.anvil_desc");
            case LOOM -> lang.getMessage("station_selector.loom_desc");
        };
    }

    private String getStationStatusMessage(RecipeStation station) {
        return switch (station) {
            case CRAFTING_TABLE -> lang.getMessage("station_selector.crafting_table_status");
            case FURNACE -> lang.getMessage("station_selector.furnace_status");
            case BLAST_FURNACE -> lang.getMessage("station_selector.blast_furnace_status");
            case SMOKER -> lang.getMessage("station_selector.smoker_status");
            case CAMPFIRE -> lang.getMessage("station_selector.campfire_status");
            case STONECUTTER -> lang.getMessage("station_selector.stonecutter_status");
            case BREWING_STAND -> lang.getMessage("station_selector.brewing_stand_status");
            case SMITHING_TABLE -> lang.getMessage("station_selector.smithing_table_status");
            case ANVIL -> lang.getMessage("station_selector.anvil_status");
            case LOOM -> lang.getMessage("station_selector.loom_status");
        };
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().equals(inventory) && event.getPlayer().equals(player)) {
            InventoryClickEvent.getHandlerList().unregister(this);
            InventoryCloseEvent.getHandlerList().unregister(this);
        }
    }
}