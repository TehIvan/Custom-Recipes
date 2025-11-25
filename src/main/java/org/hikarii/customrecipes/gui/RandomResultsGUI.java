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
import java.util.function.Consumer;

public class RandomResultsGUI implements Listener, GUIBase {
    private final CustomRecipes plugin;
    private final Player player;
    private final Inventory inventory;
    private final LanguageManager lang;
    private final Consumer<RandomResultPool> onSave;
    private final RandomResultPool originalPool;
    private final ItemStack defaultResult;

    private final List<RandomResult> results;
    private boolean showChances;
    private int failureChance; 
    private int currentPage = 0;
    private boolean isSaving = false;
    private boolean isCancelling = false;

    private static final int RESULTS_PER_PAGE = 21;
    private static final int SAVE_SLOT = 45;
    private static final int CANCEL_SLOT = 46;
    private static final int CLEAR_ALL_SLOT = 47;
    private static final int PREV_PAGE_SLOT = 48;
    private static final int INFO_SLOT = 49;
    private static final int NEXT_PAGE_SLOT = 50;
    private static final int FAILURE_CHANCE_SLOT = 51;
    private static final int BACK_SLOT = 53;

    public RandomResultsGUI(CustomRecipes plugin, Player player, RandomResultPool currentPool,
                            ItemStack defaultResult, Consumer<RandomResultPool> onSave) {
        this.plugin = plugin;
        this.player = player;
        this.onSave = onSave;
        this.lang = plugin.getLanguageManager();
        this.originalPool = currentPool;
        this.defaultResult = defaultResult;

        if (currentPool != null && currentPool.hasRandomResults()) {
            this.results = new ArrayList<>(currentPool.getResults());
            this.showChances = currentPool.isShowChances();
            this.failureChance = currentPool.getFailureChance();
        } else {
            this.results = new ArrayList<>();
            this.showChances = true;
            this.failureChance = 0;
            
            if (defaultResult != null && defaultResult.getType() != Material.AIR) {
                this.results.add(new RandomResult(defaultResult.clone(), 100,
                        lang.getMessage("random_results.default_result")));
            }
        }

        this.inventory = Bukkit.createInventory(
                null,
                54,
                MessageUtil.createGradientMenuTitle(lang.getMessage("random_results.title"))
        );
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        updateInventory();
    }

    public RandomResultsGUI(CustomRecipes plugin, Player player, RandomResultPool currentPool, Consumer<RandomResultPool> onSave) {
        this(plugin, player, currentPool, null, onSave);
    }

    public void open() {
        player.openInventory(inventory);
    }

    private void updateInventory() {
        inventory.clear();
        fillBorders();
        addResultButtons();
        addControlButtons();
        addNavigationButtons();
        addFailureChanceButton();
        addBackButton();
    }

    private void fillBorders() {
        ItemStack borderPane = new ItemStack(Material.ORANGE_STAINED_GLASS_PANE);
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

    private void addResultButtons() {
        int start = currentPage * RESULTS_PER_PAGE;
        int end = Math.min(start + RESULTS_PER_PAGE, results.size());

        int[] slots = getResultSlots();
        int slotIndex = 0;

        int totalWeight = results.stream().mapToInt(RandomResult::getWeight).sum();

        for (int i = start; i < end && slotIndex < slots.length; i++, slotIndex++) {
            RandomResult result = results.get(i);
            addResultButton(slots[slotIndex], result, i, totalWeight);
        }
    }

    private int[] getResultSlots() {
        return new int[]{
                10, 11, 12, 13, 14, 15, 16,
                19, 20, 21, 22, 23, 24, 25,
                28, 29, 30, 31, 32, 33, 34
        };
    }

    private void addResultButton(int slot, RandomResult result, int index, int totalWeight) {
        ItemStack displayItem = result.getItem().clone();
        ItemMeta meta = displayItem.getItemMeta();

        List<Component> lore = meta.hasLore() ? new ArrayList<>(meta.lore()) : new ArrayList<>();
        lore.add(Component.empty());

        if (totalWeight > 0) {
            double chance = (result.getWeight() * 100.0) / totalWeight;
            lore.add(Component.text(lang.getMessage("random_results.chance_label") + ": ", NamedTextColor.GRAY)
                    .append(Component.text(String.format("%.1f%%", chance), NamedTextColor.GOLD))
                    .decoration(TextDecoration.ITALIC, false));
        }

        lore.add(Component.empty());
        lore.add(Component.text("§e▲ §7" + lang.getMessage("random_results.left_click") + " §e+1%")
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("§c▼ §7" + lang.getMessage("random_results.right_click") + " §c-1%")
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("§e▲ §7Shift + " + lang.getMessage("random_results.left_click") + " §e+10%")
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("§c▼ §7Shift + " + lang.getMessage("random_results.right_click") + " §c-10%")
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(Component.text(lang.getMessage("random_results.middle_click_to_remove"), NamedTextColor.DARK_RED)
                .decoration(TextDecoration.ITALIC, false));

        meta.lore(lore);
        displayItem.setItemMeta(meta);
        inventory.setItem(slot, displayItem);
    }

    private void addControlButtons() {
        
        ItemStack saveButton = new ItemStack(Material.LIME_CONCRETE);
        ItemMeta saveMeta = saveButton.getItemMeta();
        saveMeta.displayName(Component.text(lang.getMessage("random_results.save"), NamedTextColor.GREEN)
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false));
        List<Component> saveLore = new ArrayList<>();
        saveLore.add(Component.empty());
        saveLore.add(Component.text(lang.getMessage("random_results.save_desc"), NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        saveMeta.lore(saveLore);
        saveButton.setItemMeta(saveMeta);
        inventory.setItem(SAVE_SLOT, saveButton);

        ItemStack cancelButton = new ItemStack(Material.RED_CONCRETE);
        ItemMeta cancelMeta = cancelButton.getItemMeta();
        cancelMeta.displayName(Component.text(lang.getMessage("random_results.cancel"), NamedTextColor.RED)
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false));
        List<Component> cancelLore = new ArrayList<>();
        cancelLore.add(Component.empty());
        cancelLore.add(Component.text(lang.getMessage("random_results.cancel_desc"), NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        cancelMeta.lore(cancelLore);
        cancelButton.setItemMeta(cancelMeta);
        inventory.setItem(CANCEL_SLOT, cancelButton);

        ItemStack clearButton = new ItemStack(Material.BARRIER);
        ItemMeta clearMeta = clearButton.getItemMeta();
        clearMeta.displayName(Component.text(lang.getMessage("random_results.clear_all"), NamedTextColor.RED)
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false));
        List<Component> clearLore = new ArrayList<>();
        clearLore.add(Component.empty());
        clearLore.add(Component.text(lang.getMessage("random_results.clear_all_desc"), NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        clearMeta.lore(clearLore);
        clearButton.setItemMeta(clearMeta);
        inventory.setItem(CLEAR_ALL_SLOT, clearButton);
    }

    private void addNavigationButtons() {
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
        lore.add(Component.text(lang.getMessage("random_results.how_to_add"), NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text(lang.getMessage("random_results.click_item_in_inventory"), NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(Component.text(lang.getMessage("random_results.show_chances") + ":", NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text(showChances ? lang.getMessage("info.enabled") : lang.getMessage("info.disabled"),
                        showChances ? NamedTextColor.GREEN : NamedTextColor.RED)
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
        meta.displayName(Component.text(lang.getMessage("random_results.back_to_editor"), NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));
        back.setItemMeta(meta);
        inventory.setItem(BACK_SLOT, back);
    }

    private void addFailureChanceButton() {
        ItemStack button = new ItemStack(Material.SKELETON_SKULL);
        ItemMeta meta = button.getItemMeta();
        meta.displayName(Component.text(lang.getMessage("random_results.failure_chance"), NamedTextColor.RED)
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text(lang.getMessage("random_results.failure_chance_desc"), NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(Component.text(lang.getMessage("random_results.current_value") + ": ", NamedTextColor.YELLOW)
                .append(Component.text(failureChance + "%", failureChance > 0 ? NamedTextColor.RED : NamedTextColor.GREEN))
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(Component.text(lang.getMessage("random_results.lmb_increase"), NamedTextColor.GREEN)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text(lang.getMessage("random_results.rmb_decrease"), NamedTextColor.RED)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text(lang.getMessage("random_results.shift_multiply"), NamedTextColor.AQUA)
                .decoration(TextDecoration.ITALIC, false));

        meta.lore(lore);
        button.setItemMeta(meta);
        inventory.setItem(FAILURE_CHANCE_SLOT, button);
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
        int slot = event.getSlot();

        if (event.getClickedInventory() != null && event.getClickedInventory().equals(player.getInventory())) {
            if (clicked != null && clicked.getType() != Material.AIR) {
                
                int defaultWeight = results.isEmpty() ? 100 :
                    (int) (results.stream().mapToInt(RandomResult::getWeight).sum() / (double) results.size());
                results.add(new RandomResult(clicked.clone(), Math.max(1, defaultWeight)));
                updateInventory();
            }
            return;
        }

        if (clicked == null || clicked.getType() == Material.AIR) {
            return;
        }

        if (slot == SAVE_SLOT) {
            saveAndClose();
            return;
        }

        if (slot == CANCEL_SLOT) {
            cancelAndClose();
            return;
        }

        if (slot == BACK_SLOT) {
            cancelAndClose();
            return;
        }

        if (slot == INFO_SLOT) {
            showChances = !showChances;
            updateInventory();
            return;
        }

        if (slot == CLEAR_ALL_SLOT) {
            results.clear();
            updateInventory();
            player.sendMessage(MessageUtil.colorize(lang.getMessage("random_results.cleared")));
            return;
        }

        if (slot == FAILURE_CHANCE_SLOT) {
            int change = event.isShiftClick() ? 10 : 1;
            if (event.getClick().isLeftClick()) {
                failureChance = Math.min(100, failureChance + change);
            } else if (event.getClick().isRightClick()) {
                failureChance = Math.max(0, failureChance - change);
            }
            updateInventory();
            return;
        }

        if (slot == PREV_PAGE_SLOT && currentPage > 0) {
            currentPage--;
            updateInventory();
            return;
        }

        if (slot == NEXT_PAGE_SLOT) {
            int totalPages = (int) Math.ceil((double) results.size() / RESULTS_PER_PAGE);
            if (currentPage < totalPages - 1) {
                currentPage++;
                updateInventory();
            }
            return;
        }

        int[] resultSlots = getResultSlots();
        for (int i = 0; i < resultSlots.length; i++) {
            if (slot == resultSlots[i]) {
                int resultIndex = currentPage * RESULTS_PER_PAGE + i;

                if (resultIndex < results.size()) {
                    handleResultClick(event, resultIndex);
                }
                return;
            }
        }
    }

    private void handleResultClick(InventoryClickEvent event, int resultIndex) {
        ClickType click = event.getClick();

        if (click == ClickType.MIDDLE) {
            results.remove(resultIndex);
            updateInventory();
            return;
        }

        int totalWeight = results.stream().mapToInt(RandomResult::getWeight).sum();
        if (totalWeight == 0) return;

        RandomResult current = results.get(resultIndex);
        double currentChance = (current.getWeight() * 100.0) / totalWeight;

        double changePercent = 0;
        if (click == ClickType.LEFT) {
            changePercent = 1;
        } else if (click == ClickType.SHIFT_LEFT) {
            changePercent = 10;
        } else if (click == ClickType.RIGHT) {
            changePercent = -1;
        } else if (click == ClickType.SHIFT_RIGHT) {
            changePercent = -10;
        } else {
            return; 
        }

        double newChance = Math.max(1, Math.min(99, currentChance + changePercent));
        int newWeight = (int) Math.round(newChance * totalWeight / 100.0);
        newWeight = Math.max(1, newWeight);

        results.set(resultIndex, new RandomResult(current.getItem(), newWeight, current.getName()));
        updateInventory();
    }

    private void saveAndClose() {
        isSaving = true;
        unregister();
        player.closeInventory();

        plugin.getLogger().info("[DEBUG RandomResults] saveAndClose called, results count: " + results.size() + ", failureChance: " + failureChance);
        for (RandomResult r : results) {
            plugin.getLogger().info("[DEBUG RandomResults] - Item: " + r.getItem().getType() + " weight: " + r.getWeight());
        }

        Bukkit.getScheduler().runTask(plugin, () -> {
            if (onSave != null) {
                RandomResultPool pool = results.isEmpty() ? null : new RandomResultPool(results, showChances, failureChance);
                
                pool = pool != null ? pool : RandomResultPool.EMPTY_SAVED;
                plugin.getLogger().info("[DEBUG RandomResults] Calling onSave with pool: " + (pool == RandomResultPool.EMPTY_SAVED ? "EMPTY_SAVED" : (pool != null ? "pool with " + pool.size() + " items, failureChance=" + pool.getFailureChance() : "null")));
                onSave.accept(pool);
            }
        });
    }

    private void cancelAndClose() {
        isCancelling = true;
        unregister();
        player.closeInventory();

        plugin.getLogger().info("[DEBUG RandomResults] cancelAndClose called");

        Bukkit.getScheduler().runTask(plugin, () -> {
            if (onSave != null) {
                onSave.accept(RandomResultPool.CANCELLED);
            }
        });
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().equals(inventory) && event.getPlayer().equals(player)) {
            if (!isSaving && !isCancelling) {
                
                unregister();
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (onSave != null) {
                        onSave.accept(RandomResultPool.CANCELLED);
                    }
                });
            }
        }
    }

    private void unregister() {
        InventoryClickEvent.getHandlerList().unregister(this);
        InventoryCloseEvent.getHandlerList().unregister(this);
    }
}
