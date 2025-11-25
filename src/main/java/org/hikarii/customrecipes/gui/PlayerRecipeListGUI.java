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
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.hikarii.customrecipes.CustomRecipes;
import org.hikarii.customrecipes.data.PlayerFavoritesManager;
import org.hikarii.customrecipes.language.LanguageManager;
import org.hikarii.customrecipes.recipe.CustomRecipe;
import org.hikarii.customrecipes.recipe.RecipeConditions;
import org.hikarii.customrecipes.recipe.RecipeType;
import org.hikarii.customrecipes.util.MessageUtil;

import java.util.*;
import java.util.stream.Collectors;

public class PlayerRecipeListGUI implements Listener {
    private final CustomRecipes plugin;
    private final LanguageManager lang;
    private final Player player;
    private final PlayerFavoritesManager favoritesManager;
    private final Inventory inventory;
    private List<CustomRecipe> recipes;
    private int page = 0;
    private static final int RECIPES_PER_PAGE = 45;
    private RecipeCategory currentCategory = RecipeCategory.ALL;
    private StationFilter currentFilter = StationFilter.ALL;
    private String searchQuery = null;
    private boolean waitingForSearch = false;

    public enum RecipeCategory {
        ALL("category.all", Material.CHEST, NamedTextColor.WHITE),
        FAVORITES("category.favorites", Material.NETHER_STAR, NamedTextColor.GOLD),
        WEAPONS("category.weapons", Material.DIAMOND_SWORD, NamedTextColor.RED),
        TOOLS("category.tools", Material.DIAMOND_PICKAXE, NamedTextColor.AQUA),
        ARMOR("category.armor", Material.DIAMOND_CHESTPLATE, NamedTextColor.BLUE),
        FOOD("category.food", Material.COOKED_BEEF, NamedTextColor.YELLOW),
        BLOCKS("category.blocks", Material.STONE, NamedTextColor.GRAY),
        MISC("category.misc", Material.STICK, NamedTextColor.LIGHT_PURPLE);

        private final String translationKey;
        private final Material icon;
        private final NamedTextColor color;

        RecipeCategory(String translationKey, Material icon, NamedTextColor color) {
            this.translationKey = translationKey;
            this.icon = icon;
            this.color = color;
        }

        public String getTranslationKey() { return translationKey; }
        public Material getIcon() { return icon; }
        public NamedTextColor getColor() { return color; }

        public RecipeCategory next() {
            RecipeCategory[] values = values();
            return values[(this.ordinal() + 1) % values.length];
        }
    }

    public enum StationFilter {
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

        StationFilter(String translationKey, Material icon, NamedTextColor color) {
            this.translationKey = translationKey;
            this.icon = icon;
            this.color = color;
        }

        public String getTranslationKey() { return translationKey; }
        public Material getIcon() { return icon; }
        public NamedTextColor getColor() { return color; }

        public StationFilter next() {
            StationFilter[] values = values();
            return values[(this.ordinal() + 1) % values.length];
        }
    }

    public PlayerRecipeListGUI(CustomRecipes plugin, Player player) {
        this.plugin = plugin;
        this.lang = plugin.getLanguageManager();
        this.player = player;
        this.favoritesManager = plugin.getPlayerFavoritesManager();
        this.recipes = getFilteredRecipes();
        this.inventory = Bukkit.createInventory(
                null,
                54,
                MessageUtil.createGradientMenuTitle(lang.getMessage("player_recipe_list.title"))
        );
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        updateInventory();
    }

    public void open() {
        player.openInventory(inventory);
    }

    private List<CustomRecipe> getFilteredRecipes() {
        List<CustomRecipe> allRecipes = new ArrayList<>(plugin.getRecipeManager().getAllRecipes());
        boolean canSeeAll = player.hasPermission("customrecipes.list.all");

        allRecipes = allRecipes.stream()
                .filter(recipe -> plugin.getRecipeManager().isRecipeEnabled(recipe.getKey()))
                .collect(Collectors.toList());

        if (!canSeeAll) {
            allRecipes = allRecipes.stream()
                    .filter(recipe -> !recipe.isHidden())
                    .collect(Collectors.toList());
        }

        allRecipes = allRecipes.stream()
                .filter(recipe -> {
                    RecipeConditions conditions = recipe.getConditions();
                    if (conditions == null || !conditions.hasPermission()) {
                        return true;
                    }
                    return player.hasPermission(conditions.getPermission());
                })
                .collect(Collectors.toList());

        if (currentFilter != StationFilter.ALL) {
            allRecipes = allRecipes.stream()
                    .filter(recipe -> {
                        RecipeType type = recipe.getType();
                        return switch (currentFilter) {
                            case CRAFTING_TABLE -> type == RecipeType.SHAPED || type == RecipeType.SHAPELESS;
                            case FURNACE -> type == RecipeType.FURNACE;
                            case BLAST_FURNACE -> type == RecipeType.BLAST_FURNACE;
                            case SMOKER -> type == RecipeType.SMOKER;
                            
                            default -> false;
                        };
                    })
                    .collect(Collectors.toList());
        }

        if (currentCategory == RecipeCategory.FAVORITES) {
            Set<String> favorites = favoritesManager.getFavorites(player.getUniqueId());
            allRecipes = allRecipes.stream()
                    .filter(recipe -> favorites.contains(recipe.getKey().toLowerCase()))
                    .collect(Collectors.toList());
        } else if (currentCategory != RecipeCategory.ALL) {
            allRecipes = allRecipes.stream()
                    .filter(recipe -> getCategoryForMaterial(recipe.getResultMaterial()) == currentCategory)
                    .collect(Collectors.toList());
        }

        if (searchQuery != null && !searchQuery.isEmpty()) {
            String query = searchQuery.toLowerCase();
            allRecipes = allRecipes.stream()
                    .filter(recipe -> {
                        
                        String name = recipe.getName() != null ? recipe.getName().toLowerCase() : "";
                        
                        String materialEn = recipe.getResultMaterial().name().toLowerCase().replace("_", " ");
                        
                        String materialLocalized = lang.getMaterialName(recipe.getResultMaterial().name()).toLowerCase();
                        return name.contains(query) || materialEn.contains(query) || materialLocalized.contains(query);
                    })
                    .collect(Collectors.toList());
        }

        return allRecipes;
    }

    private RecipeCategory getCategoryForMaterial(Material material) {
        String name = material.name();

        if (name.contains("SWORD") || name.contains("BOW") || name.contains("CROSSBOW") ||
            name.contains("TRIDENT") || name.contains("ARROW")) {
            return RecipeCategory.WEAPONS;
        }

        if (name.contains("PICKAXE") || name.contains("AXE") || name.contains("SHOVEL") ||
            name.contains("HOE") || name.contains("SHEARS") || name.contains("FISHING_ROD") ||
            name.contains("FLINT_AND_STEEL") || name.contains("COMPASS") || name.contains("CLOCK") ||
            name.contains("SPYGLASS") || name.contains("BRUSH")) {
            return RecipeCategory.TOOLS;
        }

        if (name.contains("HELMET") || name.contains("CHESTPLATE") || name.contains("LEGGINGS") ||
            name.contains("BOOTS") || name.contains("SHIELD") || name.contains("ELYTRA") ||
            name.contains("HORSE_ARMOR")) {
            return RecipeCategory.ARMOR;
        }

        if (material.isEdible() || name.contains("CAKE") || name.contains("COOKIE") ||
            name.contains("BREAD") || name.contains("STEW") || name.contains("SOUP")) {
            return RecipeCategory.FOOD;
        }

        if (material.isBlock()) {
            return RecipeCategory.BLOCKS;
        }

        return RecipeCategory.MISC;
    }

    private void cycleCategory() {
        currentCategory = currentCategory.next();
        recipes = getFilteredRecipes();
        page = 0;
        updateInventory();
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

        int startIndex = page * RECIPES_PER_PAGE;
        int endIndex = Math.min(startIndex + RECIPES_PER_PAGE, recipes.size());
        int slot = 0;

        for (int i = startIndex; i < endIndex; i++) {
            CustomRecipe recipe = recipes.get(i);
            ItemStack item = createRecipeItem(recipe);
            inventory.setItem(slot++, item);
        }

        addNavigationButtons();
    }

    private ItemStack createRecipeItem(CustomRecipe recipe) {
        ItemStack item = new ItemStack(recipe.getResultMaterial());
        ItemMeta meta = item.getItemMeta();

        boolean isFavorite = favoritesManager.isFavorite(player.getUniqueId(), recipe.getKey());

        Component displayName;
        if (recipe.getName() != null && !recipe.getName().isEmpty()) {
            displayName = MessageUtil.colorize(recipe.getName())
                    .decoration(TextDecoration.BOLD, true)
                    .decoration(TextDecoration.ITALIC, false);
        } else {
            displayName = Component.text(
                    lang.getMaterialName(recipe.getResultMaterial().name()),
                    NamedTextColor.AQUA
            ).decoration(TextDecoration.BOLD, true)
             .decoration(TextDecoration.ITALIC, false);
        }

        if (isFavorite) {
            displayName = Component.text("★ ", NamedTextColor.GOLD)
                    .append(displayName);
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

        lore.add(Component.text("👁 ", NamedTextColor.GREEN)
                .append(Component.text(lang.getMessage("player_recipe_list.left_click_view"), NamedTextColor.GRAY))
                .decoration(TextDecoration.ITALIC, false));

        if (isFavorite) {
            lore.add(Component.text("★ ", NamedTextColor.RED)
                    .append(Component.text(lang.getMessage("player_recipe_list.right_click_unfavorite"), NamedTextColor.GRAY))
                    .decoration(TextDecoration.ITALIC, false));
        } else {
            lore.add(Component.text("☆ ", NamedTextColor.YELLOW)
                    .append(Component.text(lang.getMessage("player_recipe_list.right_click_favorite"), NamedTextColor.GRAY))
                    .decoration(TextDecoration.ITALIC, false));
        }

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private void addNavigationButtons() {
        int maxPages = Math.max(1, (int) Math.ceil((double) recipes.size() / RECIPES_PER_PAGE));

        ItemStack prevButton = new ItemStack(Material.ARROW);
        ItemMeta prevMeta = prevButton.getItemMeta();
        if (page > 0) {
            prevMeta.displayName(Component.text(lang.getMessage("player_recipe_list.previous_page"), NamedTextColor.YELLOW)
                    .decoration(TextDecoration.ITALIC, false));
            Map<String, String> placeholders = Map.of("page", String.valueOf(page), "max", String.valueOf(maxPages));
            prevMeta.lore(List.of(
                    Component.text(lang.getMessage("player_recipe_list.page_info", placeholders), NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false)
            ));
        } else {
            prevMeta.displayName(Component.text(lang.getMessage("player_recipe_list.previous_page"), NamedTextColor.DARK_GRAY)
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
            nextMeta.displayName(Component.text(lang.getMessage("player_recipe_list.next_page"), NamedTextColor.YELLOW)
                    .decoration(TextDecoration.ITALIC, false));
            Map<String, String> placeholders = Map.of("page", String.valueOf(page + 2), "max", String.valueOf(maxPages));
            nextMeta.lore(List.of(
                    Component.text(lang.getMessage("player_recipe_list.page_info", placeholders), NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false)
            ));
        } else {
            nextMeta.displayName(Component.text(lang.getMessage("player_recipe_list.next_page"), NamedTextColor.DARK_GRAY)
                    .decoration(TextDecoration.ITALIC, false));
            nextMeta.lore(List.of(
                    Component.text(lang.getMessage("nav.already_last_page"), NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false)
            ));
        }
        nextButton.setItemMeta(nextMeta);
        inventory.setItem(50, nextButton);

        ItemStack categoryButton = new ItemStack(currentCategory.getIcon());
        ItemMeta categoryMeta = categoryButton.getItemMeta();
        String categoryName = lang.getMessage(currentCategory.getTranslationKey());
        categoryMeta.displayName(Component.text(
                        lang.getMessage("vanilla.category_label", Map.of("category", categoryName)),
                        NamedTextColor.GOLD)
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false));

        List<Component> categoryLore = new ArrayList<>();
        categoryLore.add(Component.empty());
        categoryLore.add(Component.text(lang.getMessage("vanilla.click_to_change_category"), NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        categoryLore.add(Component.empty());
        for (RecipeCategory cat : RecipeCategory.values()) {
            NamedTextColor color = cat == currentCategory ? NamedTextColor.GREEN : NamedTextColor.YELLOW;
            categoryLore.add(Component.text("  • " + lang.getMessage(cat.getTranslationKey()), color)
                    .decoration(TextDecoration.ITALIC, false));
        }
        categoryMeta.lore(categoryLore);
        categoryButton.setItemMeta(categoryMeta);
        inventory.setItem(46, categoryButton);

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
        for (StationFilter filter : StationFilter.values()) {
            NamedTextColor color = filter == currentFilter ? NamedTextColor.GREEN : NamedTextColor.YELLOW;
            filterLore.add(Component.text("  • " + lang.getMessage(filter.getTranslationKey()), color)
                    .decoration(TextDecoration.ITALIC, false));
        }
        filterMeta.lore(filterLore);
        filterButton.setItemMeta(filterMeta);
        inventory.setItem(47, filterButton);

        ItemStack searchButton = new ItemStack(Material.COMPASS);
        ItemMeta searchMeta = searchButton.getItemMeta();
        searchMeta.displayName(Component.text(lang.getMessage("player_recipe_list.search_title"), NamedTextColor.LIGHT_PURPLE)
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false));
        List<Component> searchLore = new ArrayList<>();
        searchLore.add(Component.empty());
        if (searchQuery != null && !searchQuery.isEmpty()) {
            searchLore.add(Component.text(lang.getMessage("player_recipe_list.current_search") + ": ", NamedTextColor.GRAY)
                    .append(Component.text(searchQuery, NamedTextColor.WHITE))
                    .decoration(TextDecoration.ITALIC, false));
            searchLore.add(Component.empty());
            searchLore.add(Component.text(lang.getMessage("player_recipe_list.left_click_new_search"), NamedTextColor.GREEN)
                    .decoration(TextDecoration.ITALIC, true));
            searchLore.add(Component.text(lang.getMessage("player_recipe_list.right_click_clear_search"), NamedTextColor.RED)
                    .decoration(TextDecoration.ITALIC, true));
        } else {
            searchLore.add(Component.text(lang.getMessage("player_recipe_list.click_to_search"), NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false));
        }
        searchMeta.lore(searchLore);
        searchButton.setItemMeta(searchMeta);
        inventory.setItem(45, searchButton);

        ItemStack infoButton = new ItemStack(Material.BOOK);
        ItemMeta infoMeta = infoButton.getItemMeta();
        infoMeta.displayName(Component.text(lang.getMessage("info.recipe_information"), NamedTextColor.GOLD)
                .decoration(TextDecoration.ITALIC, false));
        List<Component> infoLore = new ArrayList<>();
        infoLore.add(Component.empty());
        Map<String, String> countPlaceholders = Map.of("count", String.valueOf(recipes.size()));
        infoLore.add(Component.text(lang.getMessage("player_recipe_list.total_showing", countPlaceholders), NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        infoLore.add(Component.empty());
        infoLore.add(Component.text(lang.getMessage("player_recipe_list.left_click_view"), NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        infoLore.add(Component.text(lang.getMessage("player_recipe_list.right_click_favorite"), NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        infoMeta.lore(infoLore);
        infoButton.setItemMeta(infoMeta);
        inventory.setItem(49, infoButton);

        ItemStack closeButton = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = closeButton.getItemMeta();
        closeMeta.displayName(Component.text(lang.getMessage("player_recipe_list.close_menu"), NamedTextColor.RED)
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false));
        closeButton.setItemMeta(closeMeta);
        inventory.setItem(53, closeButton);
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

        if (slot < 45) {
            int recipeIndex = (page * RECIPES_PER_PAGE) + slot;
            if (recipeIndex < recipes.size()) {
                CustomRecipe recipe = recipes.get(recipeIndex);

                if (clickType.isLeftClick()) {
                    
                    InventoryClickEvent.getHandlerList().unregister(this);
                    InventoryCloseEvent.getHandlerList().unregister(this);
                    new RecipeViewerGUI(plugin, player, recipe, this).open();
                } else if (clickType.isRightClick()) {
                    
                    favoritesManager.toggleFavorite(player.getUniqueId(), recipe.getKey());
                    boolean nowFavorite = favoritesManager.isFavorite(player.getUniqueId(), recipe.getKey());

                    if (nowFavorite) {
                        MessageUtil.sendInfo(player, lang.getMessage("player_recipe_list.added_to_favorites"));
                    } else {
                        MessageUtil.sendInfo(player, lang.getMessage("player_recipe_list.removed_from_favorites"));
                    }

                    if (currentCategory == RecipeCategory.FAVORITES) {
                        recipes = getFilteredRecipes();
                        page = Math.min(page, Math.max(0, (int) Math.ceil((double) recipes.size() / RECIPES_PER_PAGE) - 1));
                    }
                    updateInventory();
                }
            }
            return;
        }

        if (slot == 45) {
            if (clickType.isRightClick() && searchQuery != null) {
                
                searchQuery = null;
                recipes = getFilteredRecipes();
                page = 0;
                updateInventory();
            } else if (clickType.isLeftClick()) {
                
                waitingForSearch = true;
                player.closeInventory();
                MessageUtil.sendInfo(player, lang.getMessage("player_recipe_list.enter_search_query"));
            }
            return;
        }

        if (slot == 46) {
            cycleCategory();
            return;
        }

        if (slot == 47) {
            cycleFilter();
            Map<String, String> placeholders = Map.of("station", lang.getMessage(currentFilter.getTranslationKey()));
            MessageUtil.sendInfo(player, lang.getMessage("info.station_filter", placeholders));
            return;
        }

        if (slot == 48 && page > 0) {
            page--;
            updateInventory();
            return;
        }

        int maxPages = Math.max(1, (int) Math.ceil((double) recipes.size() / RECIPES_PER_PAGE));
        if (slot == 50 && page < maxPages - 1) {
            page++;
            updateInventory();
            return;
        }

        if (slot == 53) {
            player.closeInventory();
        }
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        if (!event.getPlayer().equals(player) || !waitingForSearch) {
            return;
        }

        event.setCancelled(true);
        waitingForSearch = false;

        String message = event.getMessage().trim();

        if (message.equalsIgnoreCase("cancel")) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                MessageUtil.sendInfo(player, lang.getMessage("player_recipe_list.search_cancelled"));
                open();
            });
            return;
        }

        searchQuery = message;

        Bukkit.getScheduler().runTask(plugin, () -> {
            recipes = getFilteredRecipes();
            page = 0;
            Map<String, String> placeholders = Map.of("count", String.valueOf(recipes.size()), "query", searchQuery);
            MessageUtil.sendInfo(player, lang.getMessage("player_recipe_list.search_results", placeholders));
            updateInventory();
            open();
        });
    }

    public void reopen() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        recipes = getFilteredRecipes();
        updateInventory();
        player.openInventory(inventory);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().equals(inventory) && event.getPlayer().equals(player)) {
            if (!waitingForSearch) {
                InventoryClickEvent.getHandlerList().unregister(this);
                InventoryCloseEvent.getHandlerList().unregister(this);
                AsyncPlayerChatEvent.getHandlerList().unregister(this);
            }
        }
    }
}
