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
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.hikarii.customrecipes.CustomRecipes;
import org.hikarii.customrecipes.language.LanguageManager;
import org.hikarii.customrecipes.recipe.vanilla.VanillaRecipeInfo;
import org.hikarii.customrecipes.util.MessageUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class VanillaRecipeSearchResultsGUI implements Listener {
    private final CustomRecipes plugin;
    private final Player player;
    private final LanguageManager lang;
    private final Inventory inventory;
    private final List<VanillaRecipeInfo> results;
    private final String query;
    private final VanillaRecipesGUI parentGUI;
    private int page = 0;
    private static final int RESULTS_PER_PAGE = 45;

    public VanillaRecipeSearchResultsGUI(CustomRecipes plugin, Player player, List<VanillaRecipeInfo> results, String query, VanillaRecipesGUI parentGUI) {
        this.plugin = plugin;
        this.player = player;
        this.lang = plugin.getLanguageManager();
        this.results = results;
        this.query = query;
        this.parentGUI = parentGUI;
        this.inventory = Bukkit.createInventory(
                null,
                54,
                MessageUtil.createMenuTitle(lang.getMessage("vanilla.search_results_title", Map.of("query", query)), NamedTextColor.LIGHT_PURPLE)
        );
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        updateInventory();
    }

    public void open() {
        player.openInventory(inventory);
    }

    private void updateInventory() {
        inventory.clear();
        ItemStack emptySlot = new ItemStack(Material.PURPLE_STAINED_GLASS_PANE);
        ItemMeta emptyMeta = emptySlot.getItemMeta();
        emptyMeta.displayName(Component.empty());
        emptySlot.setItemMeta(emptyMeta);
        for (int i = 0; i < 45; i++) {
            inventory.setItem(i, emptySlot);
        }

        int startIndex = page * RESULTS_PER_PAGE;
        int endIndex = Math.min(startIndex + RESULTS_PER_PAGE, results.size());
        for (int i = startIndex; i < endIndex; i++) {
            VanillaRecipeInfo recipe = results.get(i);
            ItemStack item = createResultItem(recipe);
            inventory.setItem(i - startIndex, item);
        }
        addNavigationButtons();
        addInfoItem();
        addBackButton();
    }

    private ItemStack createResultItem(VanillaRecipeInfo recipe) {
        ItemStack item = new ItemStack(recipe.getResultMaterial());
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(lang.getVanillaRecipeName(recipe.getKey().replace("minecraft:", "")), NamedTextColor.AQUA)
                .decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text(lang.getMessage("vanilla.amount_label", Map.of("amount", recipe.getResultAmount() + "x")), NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text(lang.getMessage("vanilla.category_display", Map.of("category", lang.getCategoryName(recipe.getCategory().name()))), NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(Component.text(lang.getMessage("vanilla.left_click_edit"), NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, true));

        String recipeKey = recipe.getKey().replace("minecraft:", "");
        boolean disabled = plugin.getVanillaRecipeManager().isRecipeDisabled(recipeKey);
        String action = disabled ? lang.getMessage("vanilla.action_enable") : lang.getMessage("vanilla.action_disable");
        lore.add(Component.text(lang.getMessage("vanilla.right_click_action", Map.of("action", action)),
                        disabled ? NamedTextColor.GREEN : NamedTextColor.RED)
                .decoration(TextDecoration.ITALIC, true));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private void addNavigationButtons() {
        int maxPages = (int) Math.ceil((double) results.size() / RESULTS_PER_PAGE);

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

    private void addInfoItem() {
        ItemStack info = new ItemStack(Material.BOOK);
        ItemMeta meta = info.getItemMeta();
        meta.displayName(Component.text(lang.getMessage("vanilla.search_results", Map.of("count", String.valueOf(results.size()))), NamedTextColor.GOLD)
                .decoration(TextDecoration.ITALIC, false));

        int maxPages = (int) Math.ceil((double) results.size() / RESULTS_PER_PAGE);
        meta.lore(List.of(
                Component.empty(),
                Component.text(lang.getMessage("vanilla.query", Map.of("query", query)), NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false),
                Component.text(lang.getMessage("vanilla.found_recipes", Map.of("count", String.valueOf(results.size()))), NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false),
                Component.text(lang.getMessage("vanilla.page_display", Map.of("page", (page + 1) + "/" + maxPages)), NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false)
        ));
        info.setItemMeta(meta);
        inventory.setItem(49, info);
    }

    private void addBackButton() {
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta meta = back.getItemMeta();
        meta.displayName(Component.text(lang.getMessage("nav.back"), NamedTextColor.YELLOW)
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

        int maxPages = (int) Math.ceil((double) results.size() / RESULTS_PER_PAGE);
        if (slot == 50 && page < maxPages - 1) {
            page++;
            updateInventory();
            return;
        }

        if (slot == 53) {
            new VanillaRecipesGUI(plugin, player).open();
            return;
        }

        if (slot >= 0 && slot < 45) {
            int recipeIndex = (page * RESULTS_PER_PAGE) + slot;
            if (recipeIndex < results.size()) {
                VanillaRecipeInfo recipe = results.get(recipeIndex);
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
                    new VanillaRecipeEditorGUI(plugin, player, recipe, parentGUI, this).open();
                }
            }
        }
    }

    public void reopen() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        updateInventory();
        player.openInventory(inventory);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().equals(inventory) && event.getPlayer().equals(player)) {
            InventoryClickEvent.getHandlerList().unregister(this);
            InventoryCloseEvent.getHandlerList().unregister(this);
        }
    }
}