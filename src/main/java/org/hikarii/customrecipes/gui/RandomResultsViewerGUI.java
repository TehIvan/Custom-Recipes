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
import org.hikarii.customrecipes.recipe.data.RandomResult;
import org.hikarii.customrecipes.recipe.data.RandomResultPool;
import org.hikarii.customrecipes.util.MessageUtil;

import java.util.ArrayList;
import java.util.List;

public class RandomResultsViewerGUI implements Listener {
    private final CustomRecipes plugin;
    private final Player player;
    private final Inventory inventory;
    private final LanguageManager lang;
    private final Runnable onBack;
    private final RandomResultPool pool;
    private final boolean showOnlyAvailable;

    private int currentPage = 0;
    private static final int RESULTS_PER_PAGE = 21;
    private static final int PREV_PAGE_SLOT = 48;
    private static final int INFO_SLOT = 49;
    private static final int NEXT_PAGE_SLOT = 50;
    private static final int BACK_SLOT = 53;

    public RandomResultsViewerGUI(CustomRecipes plugin, Player player, RandomResultPool pool,
                                   boolean showOnlyAvailable, Runnable onBack) {
        this.plugin = plugin;
        this.player = player;
        this.pool = pool;
        this.showOnlyAvailable = showOnlyAvailable;
        this.onBack = onBack;
        this.lang = plugin.getLanguageManager();

        this.inventory = Bukkit.createInventory(
                null,
                54,
                MessageUtil.createGradientMenuTitle(lang.getMessage("random_results.viewer_title"))
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
        addResultItems();
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

        for (int i = 36; i < 54; i++) {
            inventory.setItem(i, borderPane);
        }

        for (int row = 1; row < 4; row++) {
            inventory.setItem(row * 9, borderPane);
            inventory.setItem(row * 9 + 8, borderPane);
        }
    }

    private void addResultItems() {
        if (pool == null || !pool.hasRandomResults()) {
            return;
        }

        List<RandomResult> results = pool.getResults();
        int start = currentPage * RESULTS_PER_PAGE;
        int end = Math.min(start + RESULTS_PER_PAGE, results.size());

        int[] slots = getResultSlots();
        int slotIndex = 0;

        int totalWeight = results.stream().mapToInt(RandomResult::getWeight).sum();

        for (int i = start; i < end && slotIndex < slots.length; i++, slotIndex++) {
            RandomResult result = results.get(i);
            addResultItem(slots[slotIndex], result, totalWeight);
        }
    }

    private int[] getResultSlots() {
        return new int[]{
                10, 11, 12, 13, 14, 15, 16,
                19, 20, 21, 22, 23, 24, 25,
                28, 29, 30, 31, 32, 33, 34
        };
    }

    private void addResultItem(int slot, RandomResult result, int totalWeight) {
        ItemStack displayItem = result.getItem().clone();
        ItemMeta meta = displayItem.getItemMeta();

        List<Component> lore = meta.hasLore() ? new ArrayList<>(meta.lore()) : new ArrayList<>();
        lore.add(Component.empty());

        if (pool.isShowChances() && totalWeight > 0) {
            double chance = (result.getWeight() * 100.0) / totalWeight;
            lore.add(Component.text(lang.getMessage("random_results.chance_label") + ": ", NamedTextColor.GRAY)
                    .append(Component.text(String.format("%.1f%%", chance), NamedTextColor.GOLD))
                    .decoration(TextDecoration.ITALIC, false));
        }

        if (result.hasCustomName()) {
            lore.add(Component.empty());
            lore.add(Component.text(result.getName(), NamedTextColor.YELLOW)
                    .decoration(TextDecoration.ITALIC, false));
        }

        meta.lore(lore);
        displayItem.setItemMeta(meta);
        inventory.setItem(slot, displayItem);
    }

    private void addNavigationButtons() {
        if (pool == null || !pool.hasRandomResults()) {
            return;
        }

        List<RandomResult> results = pool.getResults();
        int totalPages = (int) Math.ceil((double) results.size() / RESULTS_PER_PAGE);
        if (totalPages == 0) totalPages = 1;

        if (currentPage > 0) {
            ItemStack prevButton = new ItemStack(Material.ARROW);
            ItemMeta meta = prevButton.getItemMeta();
            meta.displayName(Component.text(lang.getMessage("lore.previous_page"), NamedTextColor.YELLOW)
                    .decoration(TextDecoration.ITALIC, false));
            prevButton.setItemMeta(meta);
            inventory.setItem(PREV_PAGE_SLOT, prevButton);
        }

        ItemStack infoBook = new ItemStack(Material.BOOK);
        ItemMeta meta = infoBook.getItemMeta();
        meta.displayName(Component.text(lang.getMessage("random_results.information"), NamedTextColor.GOLD)
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text(lang.getMessage("lore.page_info")
                        .replace("{current}", String.valueOf(currentPage + 1))
                        .replace("{total}", String.valueOf(totalPages)), NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text(lang.getMessage("random_results.total_results")
                        .replace("{count}", String.valueOf(results.size())), NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(Component.text(lang.getMessage("random_results.viewer_info"), NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));

        meta.lore(lore);
        infoBook.setItemMeta(meta);
        inventory.setItem(INFO_SLOT, infoBook);

        if (currentPage < totalPages - 1) {
            ItemStack nextButton = new ItemStack(Material.ARROW);
            ItemMeta nextMeta = nextButton.getItemMeta();
            nextMeta.displayName(Component.text(lang.getMessage("lore.next_page"), NamedTextColor.YELLOW)
                    .decoration(TextDecoration.ITALIC, false));
            nextButton.setItemMeta(nextMeta);
            inventory.setItem(NEXT_PAGE_SLOT, nextButton);
        }
    }

    private void addBackButton() {
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta meta = back.getItemMeta();
        meta.displayName(Component.text(lang.getMessage("random_results.back_to_recipe"), NamedTextColor.YELLOW)
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

        int slot = event.getSlot();

        if (slot == BACK_SLOT) {
            unregister();
            if (onBack != null) {
                Bukkit.getScheduler().runTask(plugin, onBack);
            }
            return;
        }

        if (slot == PREV_PAGE_SLOT && currentPage > 0) {
            currentPage--;
            updateInventory();
            return;
        }

        if (slot == NEXT_PAGE_SLOT && pool != null && pool.hasRandomResults()) {
            int totalPages = (int) Math.ceil((double) pool.getResults().size() / RESULTS_PER_PAGE);
            if (currentPage < totalPages - 1) {
                currentPage++;
                updateInventory();
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().equals(inventory) && event.getPlayer().equals(player)) {
            unregister();
        }
    }

    private void unregister() {
        InventoryClickEvent.getHandlerList().unregister(this);
        InventoryCloseEvent.getHandlerList().unregister(this);
    }
}
