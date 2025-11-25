package org.hikarii.customrecipes.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.hikarii.customrecipes.CustomRecipes;
import org.hikarii.customrecipes.language.LanguageManager;
import org.hikarii.customrecipes.recipe.CustomRecipe;
import org.hikarii.customrecipes.recipe.RecipeType;
import org.hikarii.customrecipes.util.MessageUtil;
import java.util.Map;
import java.util.function.Consumer;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class RecipeListGUI implements Listener {
    private final CustomRecipes plugin;
    private final Player player;
    private final Inventory inventory;
    private final LanguageManager lang;
    private List<CustomRecipe> recipes;
    private int page = 0;
    private static final int RECIPES_PER_PAGE = 45;
    private RecipeFilter currentFilter = RecipeFilter.ALL;

    public enum RecipeFilter {
        ALL("filter.all", Material.CHEST, NamedTextColor.WHITE),
        CRAFTING_TABLE("filter.crafting_table", Material.CRAFTING_TABLE, NamedTextColor.GREEN),
        FURNACE("filter.furnace", Material.FURNACE, NamedTextColor.GOLD),
        BLAST_FURNACE("filter.blast_furnace", Material.BLAST_FURNACE, NamedTextColor.DARK_RED),
        SMOKER("filter.smoker", Material.SMOKER, NamedTextColor.GRAY),
        ANVIL("filter.anvil", Material.ANVIL, NamedTextColor.DARK_GRAY),
        SMITHING_TABLE("filter.smithing_table", Material.SMITHING_TABLE, NamedTextColor.BLUE),
        BREWING_STAND("filter.brewing_stand", Material.BREWING_STAND, NamedTextColor.LIGHT_PURPLE),
        STONECUTTER("filter.stonecutter", Material.STONECUTTER, NamedTextColor.GRAY);

        private final String translationKey;
        private final Material icon;
        private final NamedTextColor color;

        RecipeFilter(String translationKey, Material icon, NamedTextColor color) {
            this.translationKey = translationKey;
            this.icon = icon;
            this.color = color;
        }

        public String getTranslationKey() {
            return translationKey;
        }

        public Material getIcon() {
            return icon;
        }

        public NamedTextColor getColor() {
            return color;
        }

        public RecipeFilter next() {
            RecipeFilter[] values = values();
            return values[(this.ordinal() + 1) % values.length];
        }
    }

    public RecipeListGUI(CustomRecipes plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.lang = plugin.getLanguageManager();
        this.recipes = getFilteredRecipes();
        this.inventory = Bukkit.createInventory(
                null,
                54,
                MessageUtil.createGradientMenuTitle(lang.getMessage("gui.title.recipe_list"))
        );
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        updateInventory();
    }

    public void open() {
        player.openInventory(inventory);
    }

    private List<CustomRecipe> getFilteredRecipes() {
        List<CustomRecipe> allRecipes = new ArrayList<>(plugin.getRecipeManager().getAllRecipes());
        if (currentFilter == RecipeFilter.ALL) {
            return allRecipes;
        }
        return allRecipes.stream()
                .filter(recipe -> {
                    switch (currentFilter) {
                        case CRAFTING_TABLE:
                            return recipe.getType() == RecipeType.SHAPED ||
                                   recipe.getType() == RecipeType.SHAPELESS;
                        case FURNACE:
                            return recipe.getType() == RecipeType.FURNACE;
                        case BLAST_FURNACE:
                            return recipe.getType() == RecipeType.BLAST_FURNACE;
                        case SMOKER:
                            return recipe.getType() == RecipeType.SMOKER;
                        case ANVIL:
                        case SMITHING_TABLE:
                        case BREWING_STAND:
                        case STONECUTTER:
                            return false; 
                        default:
                            return true;
                    }
                })
                .collect(Collectors.toList());
    }

    private void cycleFilter() {
        currentFilter = currentFilter.next();
        recipes = getFilteredRecipes();
        page = 0; 
        updateInventory();
    }

    private void updateInventory() {
        inventory.clear();
        ItemStack emptySlot = new ItemStack(Material.LIGHT_BLUE_STAINED_GLASS_PANE);
        ItemMeta emptyMeta = emptySlot.getItemMeta();
        emptyMeta.displayName(Component.empty());
        emptySlot.setItemMeta(emptyMeta);
        for (int i = 0; i < RECIPES_PER_PAGE; i++) {
            inventory.setItem(i, emptySlot);
        }

        if (!recipes.isEmpty()) {
            int startIndex = page * RECIPES_PER_PAGE;
            int endIndex = Math.min(startIndex + RECIPES_PER_PAGE, recipes.size());
            int slot = 0;
            for (int i = startIndex; i < endIndex; i++) {
                CustomRecipe recipe = recipes.get(i);
                ItemStack item = createRecipeItem(recipe);
                inventory.setItem(slot++, item);
            }
        }
        addNavigationButtons();
        addInfoItem();
    }

    private ItemStack createRecipeItem(CustomRecipe recipe) {
        ItemStack item = new ItemStack(recipe.getResultMaterial());
        ItemMeta meta = item.getItemMeta();
        boolean enabled = plugin.getRecipeManager().isRecipeEnabled(recipe.getKey());
        Component displayName;
        if (recipe.getName() != null && !recipe.getName().isEmpty()) {
            displayName = MessageUtil.colorize(recipe.getName())
                    .decoration(TextDecoration.BOLD, true)
                    .decoration(TextDecoration.ITALIC, false);
        } else {
            displayName = Component.text(
                            lang.getMaterialName(recipe.getResultMaterial().name()),
                            NamedTextColor.AQUA)
                    .decoration(TextDecoration.BOLD, true)
                    .decoration(TextDecoration.ITALIC, false);
        }
        meta.displayName(displayName);
        List<Component> lore = new ArrayList<>();
        if (recipe.getDescription() != null && !recipe.getDescription().isEmpty()) {
            for (String line : recipe.getDescription()) {
                lore.add(MessageUtil.colorize(line)
                        .decoration(TextDecoration.ITALIC, false));
            }
            lore.add(Component.empty());
        }
        Component statusLine = Component.text(enabled ? "✓ " : "✗ ",
                        enabled ? NamedTextColor.GREEN : NamedTextColor.RED)
                .decoration(TextDecoration.BOLD, true)
                .append(Component.text(lang.getMessage("info.status") + " ", NamedTextColor.GRAY)
                        .decoration(TextDecoration.BOLD, false))
                .append(Component.text(enabled ? lang.getMessage("lore.status_enabled") : lang.getMessage("lore.status_disabled"),
                                enabled ? NamedTextColor.GREEN : NamedTextColor.RED)
                        .decoration(TextDecoration.BOLD, false))
                .decoration(TextDecoration.ITALIC, false);
        lore.add(statusLine);
        lore.add(Component.text(lang.getMessage("info.type") + " ", NamedTextColor.GRAY)
                .append(Component.text(recipe.getType().toString(), NamedTextColor.WHITE))
                .decoration(TextDecoration.ITALIC, false));

        ItemMeta resultMeta = recipe.getResultItem().getItemMeta();
        if (resultMeta != null) {
            boolean hasSpecialProps = false;
            if (resultMeta.hasCustomModelData()) {
                if (!hasSpecialProps) {
                    lore.add(Component.empty());
                    hasSpecialProps = true;
                }
                lore.add(Component.text(lang.getMessage("info.custom_model_data") + " ", NamedTextColor.LIGHT_PURPLE)
                        .append(Component.text(resultMeta.getCustomModelData(), NamedTextColor.WHITE))
                        .decoration(TextDecoration.ITALIC, false));
            }
            if (resultMeta.hasEnchants()) {
                if (!hasSpecialProps) {
                    lore.add(Component.empty());
                    hasSpecialProps = true;
                }
                lore.add(Component.text(lang.getMessage("info.enchantments") + " ", NamedTextColor.LIGHT_PURPLE)
                        .append(Component.text(resultMeta.getEnchants().size() + "x", NamedTextColor.WHITE))
                        .decoration(TextDecoration.ITALIC, false));
                int shown = 0;
                for (Map.Entry<Enchantment, Integer> entry : resultMeta.getEnchants().entrySet()) {
                    if (shown++ >= 2) break;
                    String enchName = entry.getKey().getKey().getKey();
                    lore.add(Component.text("  • " + enchName + " " + entry.getValue(), NamedTextColor.AQUA)
                            .decoration(TextDecoration.ITALIC, false));
                }
                if (resultMeta.getEnchants().size() > 2) {
                    Map<String, String> placeholders = Map.of("count", String.valueOf(resultMeta.getEnchants().size() - 2));
                    lore.add(Component.text("  " + lang.getMessage("info.and_more", placeholders),
                                    NamedTextColor.DARK_GRAY)
                            .decoration(TextDecoration.ITALIC, false));
                }
            }
            PersistentDataContainer container = resultMeta.getPersistentDataContainer();
            if (!container.isEmpty()) {
                if (!hasSpecialProps) {
                    lore.add(Component.empty());
                }
                Map<String, String> placeholders = Map.of("count", String.valueOf(container.getKeys().size()));
                lore.add(Component.text(lang.getMessage("info.nbt_data") + " ", NamedTextColor.DARK_AQUA)
                        .append(Component.text(lang.getMessage("info.tags", placeholders), NamedTextColor.WHITE))
                        .decoration(TextDecoration.ITALIC, false));
                int shown = 0;
                for (NamespacedKey key : container.getKeys()) {
                    if (shown++ >= 2) break;
                    if (key.getNamespace().equals(plugin.getName().toLowerCase())) {
                        
                        String value = null;
                        try {
                            value = container.get(key, PersistentDataType.STRING);
                        } catch (IllegalArgumentException e) {
                            
                            try {
                                Byte byteVal = container.get(key, PersistentDataType.BYTE);
                                value = byteVal != null ? byteVal.toString() : null;
                            } catch (IllegalArgumentException e2) {
                                try {
                                    Integer intVal = container.get(key, PersistentDataType.INTEGER);
                                    value = intVal != null ? intVal.toString() : null;
                                } catch (IllegalArgumentException e3) {
                                    value = "<complex>";
                                }
                            }
                        }
                        if (value != null) {
                            lore.add(Component.text("  • " + key.getKey() + ": " + value, NamedTextColor.AQUA)
                                    .decoration(TextDecoration.ITALIC, false));
                        }
                    }
                }
                if (container.getKeys().size() > 2) {
                    Map<String, String> placeholders2 = Map.of("count", String.valueOf(container.getKeys().size() - 2));
                    lore.add(Component.text("  " + lang.getMessage("info.and_more", placeholders2),
                                    NamedTextColor.DARK_GRAY)
                            .decoration(TextDecoration.ITALIC, false));
                }
            }
        }
        lore.add(Component.empty());
        lore.add(Component.text("⚙ ", NamedTextColor.GOLD)
                .append(Component.text(lang.getMessage("info.left_click_manage"), NamedTextColor.GRAY))
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("✎ ", NamedTextColor.LIGHT_PURPLE)
                .append(Component.text(lang.getMessage("info.right_click_edit"), NamedTextColor.GRAY))
                .decoration(TextDecoration.ITALIC, false));
        List<String> disabledWorlds = plugin.getRecipeWorldManager().getDisabledWorlds(recipe.getKey());
        if (!disabledWorlds.isEmpty()) {
            lore.add(Component.empty());
            lore.add(Component.text(lang.getMessage("info.world_restrictions"), NamedTextColor.GOLD)
                    .decoration(TextDecoration.ITALIC, false));
            Map<String, String> worldPlaceholders = Map.of("worlds", String.join(", ", disabledWorlds));
            lore.add(Component.text(lang.getMessage("info.disabled_in", worldPlaceholders),
                            NamedTextColor.YELLOW)
                    .decoration(TextDecoration.ITALIC, false));
        }
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private void addNavigationButtons() {
        int maxPages = (int) Math.ceil((double) recipes.size() / RECIPES_PER_PAGE);
        ItemStack prevButton = new ItemStack(Material.ARROW);
        ItemMeta prevMeta = prevButton.getItemMeta();
        if (page > 0) {
            prevMeta.displayName(Component.text(lang.getMessage("nav.previous_page"), NamedTextColor.YELLOW)
                    .decoration(TextDecoration.ITALIC, false));
            Map<String, String> placeholders = Map.of("current", String.valueOf(page), "total", String.valueOf(maxPages));
            prevMeta.lore(List.of(
                    Component.text(lang.getMessage("nav.page", placeholders), NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false)
            ));
        } else {
            prevMeta.displayName(Component.text(lang.getMessage("nav.previous_page"), NamedTextColor.DARK_GRAY)
                    .decoration(TextDecoration.ITALIC, false));
            prevMeta.lore(List.of(
                    Component.text(lang.getMessage("nav.already_first_page"), NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false)
            ));
        }
        prevButton.setItemMeta(prevMeta);
        inventory.setItem(48, prevButton);
        ItemStack nextButton = new ItemStack(Material.ARROW);
        ItemMeta nextMeta = nextButton.getItemMeta();
        if (page < maxPages - 1) {
            nextMeta.displayName(Component.text(lang.getMessage("nav.next_page"), NamedTextColor.YELLOW)
                    .decoration(TextDecoration.ITALIC, false));
            Map<String, String> placeholders = Map.of("current", String.valueOf(page + 2), "total", String.valueOf(maxPages));
            nextMeta.lore(List.of(
                    Component.text(lang.getMessage("nav.page", placeholders), NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false)
            ));
        } else {
            nextMeta.displayName(Component.text(lang.getMessage("nav.next_page"), NamedTextColor.DARK_GRAY)
                    .decoration(TextDecoration.ITALIC, false));
            nextMeta.lore(List.of(
                    Component.text(lang.getMessage("nav.already_last_page"), NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false)
            ));
        }
        nextButton.setItemMeta(nextMeta);
        inventory.setItem(50, nextButton);

        ItemStack createButton = new ItemStack(Material.CRAFTING_TABLE);
        ItemMeta createMeta = createButton.getItemMeta();
        createMeta.displayName(Component.text(lang.getMessage("button.create_new_recipe"), NamedTextColor.GREEN)
                .decoration(TextDecoration.ITALIC, false));
        createMeta.lore(List.of(
                Component.empty(),
                Component.text(lang.getMessage("button.click_to_create"), NamedTextColor.YELLOW)
                        .decoration(TextDecoration.ITALIC, true)
        ));
        createButton.setItemMeta(createMeta);
        inventory.setItem(45, createButton);

        ItemStack vanillaButton = new ItemStack(Material.CRAFTING_TABLE);
        ItemMeta vanillaMeta = vanillaButton.getItemMeta();
        vanillaMeta.displayName(Component.text(lang.getMessage("button.vanilla_recipes_browse"), NamedTextColor.LIGHT_PURPLE)
                .decoration(TextDecoration.ITALIC, false));
        vanillaMeta.lore(List.of(
                Component.empty(),
                Component.text(lang.getMessage("button.click_to_browse"), NamedTextColor.YELLOW)
                        .decoration(TextDecoration.ITALIC, true)
        ));
        vanillaButton.setItemMeta(vanillaMeta);
        inventory.setItem(52, vanillaButton);

        ItemStack filterButton = new ItemStack(currentFilter.getIcon());
        ItemMeta filterMeta = filterButton.getItemMeta();
        Map<String, String> filterPlaceholders = Map.of("filter", lang.getMessage(currentFilter.getTranslationKey()));
        filterMeta.displayName(Component.text(lang.getMessage("info.filter_info", filterPlaceholders), NamedTextColor.AQUA)
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false));

        List<Component> filterLore = new ArrayList<>();
        filterLore.add(Component.empty());
        filterLore.add(Component.text(lang.getMessage("info.click_to_cycle"), NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        filterLore.add(Component.empty());
        for (RecipeFilter filter : RecipeFilter.values()) {
            NamedTextColor color = filter == currentFilter ? NamedTextColor.GREEN : NamedTextColor.YELLOW;
            filterLore.add(Component.text("  • " + lang.getMessage(filter.getTranslationKey()), color)
                    .decoration(TextDecoration.ITALIC, false));
        }
        filterMeta.lore(filterLore);
        filterButton.setItemMeta(filterMeta);
        inventory.setItem(47, filterButton);

        ItemStack settingsButton = new ItemStack(Material.COMPARATOR);
        ItemMeta settingsMeta = settingsButton.getItemMeta();
        settingsMeta.displayName(Component.text(lang.getMessage("button.settings"), NamedTextColor.AQUA)
                .decoration(TextDecoration.ITALIC, false));
        settingsMeta.lore(List.of(
                Component.empty(),
                Component.text(lang.getMessage("button.click_to_open_settings"), NamedTextColor.YELLOW)
                        .decoration(TextDecoration.ITALIC, true)
        ));
        settingsButton.setItemMeta(settingsMeta);
        inventory.setItem(53, settingsButton);
    }

    private void addInfoItem() {
        ItemStack info = new ItemStack(Material.BOOK);
        ItemMeta meta = info.getItemMeta();
        meta.displayName(Component.text(lang.getMessage("info.recipe_information"), NamedTextColor.GOLD)
                .decoration(TextDecoration.ITALIC, false));
        List<Component> infoLore = new ArrayList<>();
        infoLore.add(Component.empty());

        int totalRecipes = plugin.getRecipeManager().getAllRecipes().size();
        int filteredRecipes = recipes.size();
        if (currentFilter == RecipeFilter.ALL) {
            Map<String, String> placeholders = Map.of("count", String.valueOf(totalRecipes));
            infoLore.add(Component.text(lang.getMessage("info.total_recipes_label", placeholders), NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false));
        } else {
            Map<String, String> placeholders = Map.of(
                "filtered", String.valueOf(filteredRecipes),
                "total", String.valueOf(totalRecipes)
            );
            infoLore.add(Component.text(lang.getMessage("info.showing_recipes", placeholders), NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false));
            Map<String, String> stationPlaceholders = Map.of("station", lang.getMessage(currentFilter.getTranslationKey()));
            infoLore.add(Component.text(lang.getMessage("info.station", stationPlaceholders), currentFilter.getColor())
                    .decoration(TextDecoration.ITALIC, false));
        }
        infoLore.add(Component.empty());
        infoLore.add(Component.text(lang.getMessage("info.left_click_recipe_manage"), NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        infoLore.add(Component.text(lang.getMessage("info.right_click_recipe_edit"), NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(infoLore);
        info.setItemMeta(meta);
        inventory.setItem(49, info);
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
        ClickType clickType = event.getClick();
        if (slot == 48 && page > 0) {
            page--;
            updateInventory();
            return;
        }

        int maxPages = (int) Math.ceil((double) recipes.size() / RECIPES_PER_PAGE);
        if (slot == 50 && page < maxPages - 1) {
            page++;
            updateInventory();
            return;
        }

        if (slot == 45) {
            if (!player.hasPermission("customrecipes.manage")) {
                MessageUtil.sendError(player, lang.getMessage("general.no_permission_create"));
                return;
            }
            new StationSelectorGUI(plugin, player).open();
            return;
        }

        if (slot == 47) {

            cycleFilter();
            Map<String, String> placeholders = Map.of("station", lang.getMessage(currentFilter.getTranslationKey()));
            MessageUtil.sendAdminInfo(player, lang.getMessage("info.station_filter", placeholders));
            return;
        }

        if (slot == 52) {
            new VanillaRecipesGUI(plugin, player).open();
            return;
        }

        if (slot == 53) {
            new SettingsGUI(plugin, player).open();
            return;
        }

        if (slot < 45) {
            int recipeIndex = (page * RECIPES_PER_PAGE) + slot;
            if (recipeIndex < recipes.size()) {
                CustomRecipe recipe = recipes.get(recipeIndex);
                if (clickType == ClickType.RIGHT) {
                    if (!player.hasPermission("customrecipes.manage")) {
                        MessageUtil.sendError(player, lang.getMessage("general.no_permission_edit"));
                        return;
                    }
                    ItemStack resultItem = recipe.getResultItem();
                    new ItemEditorGUI(plugin, player, resultItem, (editedItem) -> {
                        if (editedItem != null) {
                            ItemEditorGUI editor = ItemEditorGUI.getLastEditor(player.getUniqueId());
                            String newName = editor != null ? editor.getCustomName() : null;
                            List<String> newDesc = editor != null ? editor.getCustomLore() : null;
                            plugin.getRecipeManager().updateRecipeResult(recipe.getKey(), editedItem,
                                    newName, newDesc);
                            Map<String, String> placeholders = Map.of("recipe", recipe.getKey());
                            MessageUtil.sendAdminSuccess(player, lang.getMessage("info.updated_result_item", placeholders));
                        }
                        new RecipeListGUI(plugin, player).open();
                    }).open();
                } else {
                    new RecipeEditorGUI(plugin, player, recipe).open();
                }
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