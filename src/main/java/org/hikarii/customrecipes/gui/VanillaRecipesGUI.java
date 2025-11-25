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
import org.hikarii.customrecipes.language.LanguageManager;
import org.hikarii.customrecipes.recipe.vanilla.VanillaRecipeInfo;
import org.hikarii.customrecipes.recipe.vanilla.VanillaRecipeManager;
import org.hikarii.customrecipes.util.MessageUtil;
import java.util.*;
import java.util.stream.Collectors;

public class VanillaRecipesGUI implements Listener {
    private static final Map<UUID, VanillaRecipesGUI> waitingForSearch = new HashMap<>();
    private final CustomRecipes plugin;
    private final Player player;
    private final LanguageManager lang;
    private final Inventory inventory;
    private int page = 0;
    private static final int RECIPES_PER_PAGE = 45;

    private VanillaRecipeInfo.RecipeCategory currentCategory = null;
    private VanillaRecipeInfo.RecipeStation currentStation = VanillaRecipeInfo.RecipeStation.CRAFTING_TABLE;
    private List<VanillaRecipeInfo> displayedRecipes;
    private static final Map<UUID, VanillaRecipeInfo.RecipeCategory> savedCategories = new HashMap<>();
    private static final Map<UUID, VanillaRecipeInfo.RecipeStation> savedStations = new HashMap<>();
    private static final Map<UUID, Integer> savedPages = new HashMap<>();

    public VanillaRecipesGUI(CustomRecipes plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.lang = plugin.getLanguageManager();
        VanillaRecipeInfo.RecipeCategory savedCat = savedCategories.get(player.getUniqueId());
        VanillaRecipeInfo.RecipeStation savedStat = savedStations.get(player.getUniqueId());
        Integer savedPage = savedPages.get(player.getUniqueId());

        if (savedCat != null) {
            this.currentCategory = savedCat;
        }
        if (savedStat != null) {
            this.currentStation = savedStat;
        }
        if (savedPage != null) {
            this.page = savedPage;
        }
        this.inventory = Bukkit.createInventory(
                null,
                54,
                MessageUtil.createMenuTitle(lang.getMessage("gui.title.vanilla_recipes"), NamedTextColor.DARK_GREEN)
        );
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        updateDisplayedRecipes();
        
        int maxPages = (int) Math.ceil((double) displayedRecipes.size() / RECIPES_PER_PAGE);
        if (page >= maxPages && maxPages > 0) {
            page = maxPages - 1;
        }
        updateInventory();
    }

    public VanillaRecipesGUI(CustomRecipes plugin, Player player, VanillaRecipeInfo.RecipeCategory category, VanillaRecipeInfo.RecipeStation station) {
        this(plugin, player, category, station, 0);
    }

    public VanillaRecipesGUI(CustomRecipes plugin, Player player, VanillaRecipeInfo.RecipeCategory category, VanillaRecipeInfo.RecipeStation station, int page) {
        this.plugin = plugin;
        this.player = player;
        this.lang = plugin.getLanguageManager();
        this.currentCategory = category;
        this.currentStation = station;
        this.page = page;
        this.inventory = Bukkit.createInventory(
                null,
                54,
                MessageUtil.createMenuTitle(lang.getMessage("gui.title.vanilla_recipes"), NamedTextColor.DARK_GREEN)
        );
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        updateDisplayedRecipes();
        
        int maxPages = (int) Math.ceil((double) displayedRecipes.size() / RECIPES_PER_PAGE);
        if (this.page >= maxPages && maxPages > 0) {
            this.page = maxPages - 1;
        }
        updateInventory();
    }

    public void open() {
        savedCategories.put(player.getUniqueId(), currentCategory);
        savedStations.put(player.getUniqueId(), currentStation);
        savedPages.put(player.getUniqueId(), page);
        player.openInventory(inventory);
    }

    private void updateDisplayedRecipes() {
        List<VanillaRecipeInfo> allRecipes = new ArrayList<>(
                plugin.getVanillaRecipeManager().getAllVanillaRecipes().values()
        );

        if (currentStation != null) {
            allRecipes = allRecipes.stream()
                    .filter(r -> r.getStation() == currentStation)
                    .collect(Collectors.toList());
        }

        if (currentCategory != null) {
            allRecipes = allRecipes.stream()
                    .filter(r -> r.getCategory() == currentCategory)
                    .collect(Collectors.toList());
        }
        this.displayedRecipes = allRecipes;
    }

    private void updateInventory() {
        inventory.clear();
        ItemStack emptySlot = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
        ItemMeta emptyMeta = emptySlot.getItemMeta();
        emptyMeta.displayName(Component.empty());
        emptySlot.setItemMeta(emptyMeta);
        for (int i = 0; i < 45; i++) {
            inventory.setItem(i, emptySlot);
        }
        addRecipeItems();
        addNavigationButtons();
        addFilterButtons();
        addInfoItem();
        addSearchButton();
        addBackButton();
    }

    private void addRecipeItems() {
        int startIndex = page * RECIPES_PER_PAGE;
        int endIndex = Math.min(startIndex + RECIPES_PER_PAGE, displayedRecipes.size());
        for (int i = startIndex; i < endIndex; i++) {
            VanillaRecipeInfo recipe = displayedRecipes.get(i);
            ItemStack item = createRecipeItem(recipe);
            inventory.setItem(i - startIndex, item);
        }
    }

    private ItemStack createRecipeItem(VanillaRecipeInfo recipe) {
        String recipeKey = recipe.getKey().replace("minecraft:", "");
        ItemStack item = new ItemStack(recipe.getResultMaterial());
        ItemMeta meta = item.getItemMeta();
        boolean disabled = plugin.getVanillaRecipeManager().isRecipeDisabled(recipeKey);
        boolean changed = plugin.getVanillaRecipeManager().isRecipeChanged(recipeKey);

        VanillaRecipeManager.VanillaRecipeState state = plugin.getVanillaRecipeManager().getRecipeState(recipeKey);
        boolean amountModified = state != null && (state.getCustomResultAmount() != null || state.hasAnyVariantResultAmount());
        boolean anyModification = changed || amountModified;

        NamedTextColor nameColor;
        String statusText;
        if (disabled) {
            nameColor = NamedTextColor.RED;
            statusText = lang.getMessage("vanilla.status_disabled_symbol");
        } else if (anyModification) {
            nameColor = NamedTextColor.YELLOW;
            statusText = lang.getMessage("vanilla.status_modified");
        } else {
            nameColor = NamedTextColor.GREEN;
            statusText = lang.getMessage("vanilla.status_enabled_symbol");
        }
        Component displayName = Component.text(lang.getVanillaRecipeName(recipe.getKey().replace("minecraft:", "")), nameColor)
                .decoration(TextDecoration.ITALIC, false);
        meta.displayName(displayName);
        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text(lang.getMessage("info.status") + " ", NamedTextColor.GRAY)
                .append(Component.text(statusText, nameColor))
                .decoration(TextDecoration.ITALIC, false));

        int displayAmount = recipe.getResultAmount();
        if (state != null) {
            Integer variantAmount = state.getResultAmountForVariant(0);
            if (variantAmount != null) {
                displayAmount = variantAmount;
            } else if (state.getCustomResultAmount() != null) {
                displayAmount = state.getCustomResultAmount();
            }
        }
        lore.add(Component.text(lang.getMessage("vanilla.amount_label", Map.of("amount", displayAmount + "x")), NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text(lang.getMessage("vanilla.category_display", Map.of("category", lang.getCategoryName(recipe.getCategory().name()))), NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(Component.text(lang.getMessage("vanilla.left_click_edit"), NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, true));
        String action = disabled ? lang.getMessage("vanilla.action_enable") : lang.getMessage("vanilla.action_disable");
        lore.add(Component.text(lang.getMessage("vanilla.right_click_action", Map.of("action", action)),
                        disabled ? NamedTextColor.GREEN : NamedTextColor.RED)
                .decoration(TextDecoration.ITALIC, true));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private void addNavigationButtons() {
        int maxPages = (int) Math.ceil((double) displayedRecipes.size() / RECIPES_PER_PAGE);
        ItemStack prevButton = new ItemStack(Material.ARROW);
        ItemMeta prevMeta = prevButton.getItemMeta();
        prevMeta.displayName(Component.text(lang.getMessage("nav.previous_page"),
                        page > 0 ? NamedTextColor.YELLOW : NamedTextColor.DARK_GRAY)
                .decoration(TextDecoration.ITALIC, false));
        if (page > 0) {
            prevMeta.lore(List.of(
                    Component.text(lang.getMessage("nav.page", Map.of("current", String.valueOf(page), "total", String.valueOf(maxPages))), NamedTextColor.GRAY)
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
                        page < maxPages - 1 ? NamedTextColor.YELLOW : NamedTextColor.DARK_GRAY)
                .decoration(TextDecoration.ITALIC, false));

        if (page < maxPages - 1) {
            nextMeta.lore(List.of(
                    Component.text(lang.getMessage("nav.page", Map.of("current", String.valueOf(page + 2), "total", String.valueOf(maxPages))), NamedTextColor.GRAY)
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
    }

    private void addFilterButtons() {
        ItemStack stationButton = new ItemStack(currentStation.getIcon());
        ItemMeta stationMeta = stationButton.getItemMeta();
        stationMeta.displayName(Component.text(lang.getMessage("vanilla.station_label", Map.of("station", lang.getStationName(currentStation.name()))), NamedTextColor.AQUA)
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false));
        List<Component> stationLore = new ArrayList<>();
        stationLore.add(Component.empty());
        stationLore.add(Component.text(lang.getMessage("info.click_to_change_station"), NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        stationLore.add(Component.empty());
        for (VanillaRecipeInfo.RecipeStation station : VanillaRecipeInfo.RecipeStation.values()) {
            NamedTextColor color = station == currentStation ? NamedTextColor.GREEN :
                    (station.isEnabled() ? NamedTextColor.YELLOW : NamedTextColor.DARK_GRAY);
            stationLore.add(Component.text("  • " + lang.getStationName(station.name()), color)
                    .decoration(TextDecoration.ITALIC, false));
        }
        stationMeta.lore(stationLore);
        stationButton.setItemMeta(stationMeta);
        inventory.setItem(46, stationButton);
        ItemStack categoryButton = new ItemStack( currentCategory != null ? currentCategory.getIcon() : Material.CHEST );
        ItemMeta categoryMeta = categoryButton.getItemMeta();
        String categoryName = currentCategory != null ? lang.getCategoryName(currentCategory.name()) : lang.getMessage("vanilla.category_all");
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
        categoryLore.add(Component.text(lang.getMessage("vanilla.all_categories"),
                        currentCategory == null ? NamedTextColor.GREEN : NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));
        for (VanillaRecipeInfo.RecipeCategory category : VanillaRecipeInfo.RecipeCategory.values()) {
            NamedTextColor color = category == currentCategory ? NamedTextColor.GREEN : NamedTextColor.YELLOW;
            categoryLore.add(Component.text("  • " + lang.getCategoryName(category.name()), color)
                    .decoration(TextDecoration.ITALIC, false));
        }
        categoryMeta.lore(categoryLore);
        categoryButton.setItemMeta(categoryMeta);
        inventory.setItem(47, categoryButton);
    }

    private void addSearchButton() {
        ItemStack search = new ItemStack(Material.COMPASS);
        ItemMeta meta = search.getItemMeta();
        meta.displayName(Component.text(lang.getMessage("vanilla.search_recipe"), NamedTextColor.LIGHT_PURPLE)
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
                Component.empty(),
                Component.text(lang.getMessage("vanilla.search_by_name"), NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false),
                Component.empty(),
                Component.text(lang.getMessage("vanilla.click_to_search"), NamedTextColor.YELLOW)
                        .decoration(TextDecoration.ITALIC, true)
        ));
        search.setItemMeta(meta);
        inventory.setItem(45, search);
    }

    private void addInfoItem() {
        ItemStack info = new ItemStack(Material.BOOK);
        ItemMeta meta = info.getItemMeta();
        meta.displayName(Component.text(lang.getMessage("gui.title.vanilla_recipes"), NamedTextColor.GOLD)
                .decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text(lang.getMessage("vanilla.total_recipes", Map.of("count", String.valueOf(displayedRecipes.size()))), NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(Component.text(lang.getMessage("vanilla.left_click_edit_recipe"), NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text(lang.getMessage("vanilla.right_click_toggle_recipe"), NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);
        info.setItemMeta(meta);
        inventory.setItem(49, info);
    }

    private void addBackButton() {
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta meta = back.getItemMeta();
        meta.displayName(Component.text(lang.getMessage("vanilla.back_to_main"), NamedTextColor.YELLOW)
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

        int slot = event.getSlot();
        ClickType clickType = event.getClick();
        if (slot == 48 && page > 0) {
            page--;
            updateInventory();
            return;
        }

        int maxPages = (int) Math.ceil((double) displayedRecipes.size() / RECIPES_PER_PAGE);
        if (slot == 50 && page < maxPages - 1) {
            page++;
            updateInventory();
            return;
        }

        if (slot == 45) {
            waitingForSearch.put(player.getUniqueId(), this);
            player.closeInventory();
            MessageUtil.sendAdminInfo(player, lang.getMessage("vanilla.type_recipe_name"));
            MessageUtil.sendAdminInfo(player, lang.getMessage("vanilla.type_cancel"));
            return;
        }

        if (slot == 46) {
            cycleStation();
            return;
        }

        if (slot == 47) {
            cycleCategory();
            return;
        }

        if (slot == 53) {
            new RecipeListGUI(plugin, player).open();
            return;
        }

        if (slot < 0 || slot >= 54) {
            return;
        }

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR ||
                clicked.getType() == Material.LIME_STAINED_GLASS_PANE) {
            return;
        }

        if (slot >= 0 && slot < 45) {
            int recipeIndex = (page * RECIPES_PER_PAGE) + slot;
            if (recipeIndex >= 0 && recipeIndex < displayedRecipes.size()) {
                VanillaRecipeInfo recipe = displayedRecipes.get(recipeIndex);
                String recipeKey = recipe.getKey().replace("minecraft:", "");

                if (clickType.isRightClick()) {
                    plugin.getVanillaRecipeManager().toggleRecipe(recipeKey);
                    boolean nowDisabled = plugin.getVanillaRecipeManager().isRecipeDisabled(recipeKey);
                    String status = nowDisabled ? lang.getMessage("vanilla.disabled") : lang.getMessage("vanilla.enabled");
                    MessageUtil.send(player,
                            lang.getMessage("vanilla.vanilla_recipe_toggled", Map.of("status", status, "name", lang.getVanillaRecipeName(recipe.getKey().replace("minecraft:", "")))),
                            nowDisabled ? NamedTextColor.RED : NamedTextColor.GREEN);
                    updateInventory();
                } else {
                    new VanillaRecipeEditorGUI(plugin, player, recipe, this).open();
                }
            }
        }
    }

    private void cycleStation() {
        VanillaRecipeInfo.RecipeStation[] stations = VanillaRecipeInfo.RecipeStation.values();
        int currentIndex = Arrays.asList(stations).indexOf(currentStation);
        int nextIndex = currentIndex;
        do {
            nextIndex = (nextIndex + 1) % stations.length;
            if (stations[nextIndex].isEnabled()) {
                currentStation = stations[nextIndex];
                page = 0;
                updateDisplayedRecipes();
                updateInventory();
                savedStations.put(player.getUniqueId(), currentStation);
                return;
            }
        } while (nextIndex != currentIndex);
        MessageUtil.sendAdminWarning(player, lang.getMessage("vanilla.other_stations_soon"));
    }

    private void cycleCategory() {
        VanillaRecipeInfo.RecipeCategory[] categories = VanillaRecipeInfo.RecipeCategory.values();
        if (currentCategory == null) {
            currentCategory = categories[0];
        } else {
            int currentIndex = Arrays.asList(categories).indexOf(currentCategory);
            if (currentIndex == categories.length - 1) {
                currentCategory = null;
            } else {
                currentCategory = categories[currentIndex + 1];
            }
        }
        page = 0;
        updateDisplayedRecipes();
        updateInventory();
        savedCategories.put(player.getUniqueId(), currentCategory);
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player chatPlayer = event.getPlayer();
        VanillaRecipesGUI gui = waitingForSearch.get(chatPlayer.getUniqueId());
        if (gui == null || !chatPlayer.equals(player)) {
            return;
        }

        event.setCancelled(true);
        String message = event.getMessage();
        if (message.equalsIgnoreCase("cancel")) {
            waitingForSearch.remove(chatPlayer.getUniqueId());
            Bukkit.getScheduler().runTask(plugin, () -> {
                MessageUtil.sendAdminWarning(chatPlayer, lang.getMessage("vanilla.search_cancelled"));
                gui.open();
            });
            return;
        }

        List<VanillaRecipeInfo> results = plugin.getVanillaRecipeManager().searchRecipes(message);
        waitingForSearch.remove(chatPlayer.getUniqueId());
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (results.isEmpty()) {
                MessageUtil.sendError(chatPlayer, lang.getMessage("vanilla.no_recipes_matching", Map.of("query", message)));
                gui.open();
            } else {
                new VanillaRecipeSearchResultsGUI(plugin, chatPlayer, results, message, gui).open();
            }
        });
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().equals(inventory) && event.getPlayer().equals(player)) {
            if (!waitingForSearch.containsKey(player.getUniqueId())) {
                savedCategories.remove(player.getUniqueId());
                savedStations.remove(player.getUniqueId());
                InventoryClickEvent.getHandlerList().unregister(this);
                InventoryCloseEvent.getHandlerList().unregister(this);
                AsyncPlayerChatEvent.getHandlerList().unregister(this);
            }
        }
    }

    public void reopen() {
        updateDisplayedRecipes();
        updateInventory();
        open();
    }
    public VanillaRecipeInfo.RecipeCategory getCurrentCategory() {
        return currentCategory;
    }

    public VanillaRecipeInfo.RecipeStation getCurrentStation() {
        return currentStation;
    }

    public int getCurrentPage() {
        return page;
    }
}