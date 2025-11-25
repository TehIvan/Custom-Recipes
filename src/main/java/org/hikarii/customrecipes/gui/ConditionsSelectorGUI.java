package org.hikarii.customrecipes.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
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
import org.hikarii.customrecipes.recipe.CraftTracker;
import org.hikarii.customrecipes.recipe.CustomRecipe;
import org.hikarii.customrecipes.recipe.RecipeConditions;
import org.hikarii.customrecipes.util.MessageUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ConditionsSelectorGUI implements Listener {
    private final CustomRecipes plugin;
    private final LanguageManager lang;
    private final Player player;
    private final CustomRecipe recipe;
    private final Inventory inventory;
    private final Runnable onReturn;

    private String permission;
    private int requiredXpLevel;
    private int xpReward;
    private int cooldownSeconds;
    private int craftLimitDaily;
    private int craftLimitWeekly;
    private int craftLimitTotal;
    private double moneyCost;

    private boolean awaitingPermissionInput = false;

    private static final int PERMISSION_SLOT = 10;
    private static final int XP_LEVEL_SLOT = 12;
    private static final int XP_REWARD_SLOT = 14;
    private static final int COOLDOWN_SLOT = 16;
    
    private static final int DAILY_LIMIT_SLOT = 19;
    private static final int WEEKLY_LIMIT_SLOT = 21;
    private static final int TOTAL_LIMIT_SLOT = 23;
    private static final int MONEY_COST_SLOT = 25;
    
    private static final int SAVE_SLOT = 45;
    private static final int CLEAR_SLOT = 49;
    private static final int BACK_SLOT = 53;

    public ConditionsSelectorGUI(CustomRecipes plugin, Player player, CustomRecipe recipe, Runnable onReturn) {
        this.plugin = plugin;
        this.lang = plugin.getLanguageManager();
        this.player = player;
        this.recipe = recipe;
        this.onReturn = onReturn;

        RecipeConditions conditions = recipe.getConditions();
        if (conditions != null) {
            this.permission = conditions.getPermission();
            this.requiredXpLevel = conditions.getRequiredXpLevel();
            this.xpReward = conditions.getXpReward();
            this.cooldownSeconds = conditions.getCooldownSeconds();
            this.craftLimitDaily = conditions.getCraftLimitDaily();
            this.craftLimitWeekly = conditions.getCraftLimitWeekly();
            this.craftLimitTotal = conditions.getCraftLimitTotal();
            this.moneyCost = conditions.getMoneyCost();
        } else {
            this.permission = null;
            this.requiredXpLevel = 0;
            this.xpReward = 0;
            this.cooldownSeconds = 0;
            this.craftLimitDaily = 0;
            this.craftLimitWeekly = 0;
            this.craftLimitTotal = 0;
            this.moneyCost = 0;
        }

        this.inventory = Bukkit.createInventory(
                null,
                54,
                MessageUtil.createMenuTitle(lang.getMessage("conditions_gui.title"), NamedTextColor.GOLD)
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
        addPermissionButton();
        addXpLevelButton();
        addXpRewardButton();
        addCooldownButton();
        addDailyLimitButton();
        addWeeklyLimitButton();
        addTotalLimitButton();
        addMoneyCostButton();
        addSaveButton();
        addClearAllButton();
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
        for (int i = 45; i < 54; i++) {
            inventory.setItem(i, borderPane);
        }
    }

    private void addPermissionButton() {
        ItemStack button = new ItemStack(Material.NAME_TAG);
        ItemMeta meta = button.getItemMeta();
        meta.displayName(Component.text(lang.getMessage("conditions_gui.permission.title"), NamedTextColor.YELLOW)
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text(lang.getMessage("conditions_gui.permission.desc"), NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());

        if (permission != null && !permission.isEmpty()) {
            lore.add(Component.text(lang.getMessage("conditions_gui.permission.current",
                            Map.of("value", permission)), NamedTextColor.GREEN)
                    .decoration(TextDecoration.ITALIC, false));
        } else {
            lore.add(Component.text(lang.getMessage("conditions_gui.permission.current",
                            Map.of("value", lang.getMessage("conditions_gui.permission.none"))), NamedTextColor.DARK_GRAY)
                    .decoration(TextDecoration.ITALIC, false));
        }

        lore.add(Component.empty());
        lore.add(Component.text(lang.getMessage("conditions_gui.permission.click_to_set"), NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, true));
        if (permission != null && !permission.isEmpty()) {
            lore.add(Component.text(lang.getMessage("conditions_gui.permission.click_to_clear"), NamedTextColor.RED)
                    .decoration(TextDecoration.ITALIC, true));
        }

        meta.lore(lore);
        button.setItemMeta(meta);
        inventory.setItem(PERMISSION_SLOT, button);
    }

    private void addXpLevelButton() {
        ItemStack button = new ItemStack(Material.EXPERIENCE_BOTTLE);
        ItemMeta meta = button.getItemMeta();
        meta.displayName(Component.text(lang.getMessage("conditions_gui.xp_level.title"), NamedTextColor.AQUA)
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = createNumberLore("xp_level", requiredXpLevel, 0);
        meta.lore(lore);
        button.setItemMeta(meta);
        inventory.setItem(XP_LEVEL_SLOT, button);
    }

    private void addXpRewardButton() {
        Material material = xpReward > 0 ? Material.EMERALD : (xpReward < 0 ? Material.REDSTONE : Material.GOLD_NUGGET);
        ItemStack button = new ItemStack(material);
        ItemMeta meta = button.getItemMeta();
        meta.displayName(Component.text(lang.getMessage("conditions_gui.xp_reward.title"), NamedTextColor.LIGHT_PURPLE)
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text(lang.getMessage("conditions_gui.xp_reward.desc"), NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());

        if (xpReward != 0) {
            String rewardStr = (xpReward > 0 ? "+" : "") + xpReward;
            NamedTextColor color = xpReward > 0 ? NamedTextColor.GREEN : NamedTextColor.RED;
            lore.add(Component.text(lang.getMessage("conditions_gui.xp_reward.current",
                            Map.of("value", rewardStr)), color)
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text(lang.getMessage(xpReward > 0 ? "conditions_gui.xp_reward.positive" : "conditions_gui.xp_reward.negative"),
                            xpReward > 0 ? NamedTextColor.GREEN : NamedTextColor.RED)
                    .decoration(TextDecoration.ITALIC, false));
        } else {
            lore.add(Component.text(lang.getMessage("conditions_gui.xp_reward.current",
                            Map.of("value", lang.getMessage("conditions_gui.xp_reward.none"))), NamedTextColor.DARK_GRAY)
                    .decoration(TextDecoration.ITALIC, false));
        }

        lore.add(Component.empty());
        addClickInstructions(lore, "xp_reward");
        meta.lore(lore);
        button.setItemMeta(meta);
        inventory.setItem(XP_REWARD_SLOT, button);
    }

    private void addCooldownButton() {
        ItemStack button = new ItemStack(Material.CLOCK);
        ItemMeta meta = button.getItemMeta();
        meta.displayName(Component.text(lang.getMessage("conditions_gui.cooldown.title"), NamedTextColor.GOLD)
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text(lang.getMessage("conditions_gui.cooldown.desc"), NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());

        if (cooldownSeconds > 0) {
            lore.add(Component.text(lang.getMessage("conditions_gui.cooldown.current",
                            Map.of("value", CraftTracker.formatTime(cooldownSeconds))), NamedTextColor.GREEN)
                    .decoration(TextDecoration.ITALIC, false));
        } else {
            lore.add(Component.text(lang.getMessage("conditions_gui.cooldown.current",
                            Map.of("value", lang.getMessage("conditions_gui.cooldown.none"))), NamedTextColor.DARK_GRAY)
                    .decoration(TextDecoration.ITALIC, false));
        }

        lore.add(Component.empty());
        addClickInstructions(lore, "cooldown");
        meta.lore(lore);
        button.setItemMeta(meta);
        inventory.setItem(COOLDOWN_SLOT, button);
    }

    private void addDailyLimitButton() {
        ItemStack button = new ItemStack(Material.SUNFLOWER);
        ItemMeta meta = button.getItemMeta();
        meta.displayName(Component.text(lang.getMessage("conditions_gui.daily_limit.title"), NamedTextColor.YELLOW)
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = createNumberLore("daily_limit", craftLimitDaily, 0);
        meta.lore(lore);
        button.setItemMeta(meta);
        inventory.setItem(DAILY_LIMIT_SLOT, button);
    }

    private void addWeeklyLimitButton() {
        ItemStack button = new ItemStack(Material.PAPER);
        ItemMeta meta = button.getItemMeta();
        meta.displayName(Component.text(lang.getMessage("conditions_gui.weekly_limit.title"), NamedTextColor.AQUA)
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = createNumberLore("weekly_limit", craftLimitWeekly, 0);
        meta.lore(lore);
        button.setItemMeta(meta);
        inventory.setItem(WEEKLY_LIMIT_SLOT, button);
    }

    private void addTotalLimitButton() {
        ItemStack button = new ItemStack(Material.BOOK);
        ItemMeta meta = button.getItemMeta();
        meta.displayName(Component.text(lang.getMessage("conditions_gui.total_limit.title"), NamedTextColor.RED)
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = createNumberLore("total_limit", craftLimitTotal, 0);
        meta.lore(lore);
        button.setItemMeta(meta);
        inventory.setItem(TOTAL_LIMIT_SLOT, button);
    }

    private void addMoneyCostButton() {
        boolean vaultEnabled = plugin.getVaultIntegration().isEnabled();
        ItemStack button = new ItemStack(vaultEnabled ? Material.GOLD_INGOT : Material.BARRIER);
        ItemMeta meta = button.getItemMeta();
        meta.displayName(Component.text(lang.getMessage("conditions_gui.money_cost.title"),
                        vaultEnabled ? NamedTextColor.GOLD : NamedTextColor.DARK_GRAY)
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text(lang.getMessage("conditions_gui.money_cost.desc"), NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());

        if (!vaultEnabled) {
            lore.add(Component.text(lang.getMessage("conditions_gui.money_cost.vault_not_found"), NamedTextColor.RED)
                    .decoration(TextDecoration.ITALIC, false));
        } else if (moneyCost > 0) {
            lore.add(Component.text(lang.getMessage("conditions_gui.money_cost.current",
                            Map.of("value", plugin.getVaultIntegration().format(moneyCost))), NamedTextColor.GREEN)
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.empty());
            addClickInstructions(lore, "money_cost", true);
        } else {
            lore.add(Component.text(lang.getMessage("conditions_gui.money_cost.current",
                            Map.of("value", lang.getMessage("conditions_gui.money_cost.none"))), NamedTextColor.DARK_GRAY)
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.empty());
            addClickInstructions(lore, "money_cost", true);
        }

        meta.lore(lore);
        button.setItemMeta(meta);
        inventory.setItem(MONEY_COST_SLOT, button);
    }

    private List<Component> createNumberLore(String key, int value, int defaultValue) {
        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text(lang.getMessage("conditions_gui." + key + ".desc"), NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());

        if (value != defaultValue) {
            lore.add(Component.text(lang.getMessage("conditions_gui." + key + ".current",
                            Map.of("value", String.valueOf(value))), NamedTextColor.GREEN)
                    .decoration(TextDecoration.ITALIC, false));
        } else {
            lore.add(Component.text(lang.getMessage("conditions_gui." + key + ".current",
                            Map.of("value", lang.getMessage("conditions_gui." + key + ".none"))), NamedTextColor.DARK_GRAY)
                    .decoration(TextDecoration.ITALIC, false));
        }

        lore.add(Component.empty());
        addClickInstructions(lore, key, false);
        return lore;
    }

    private void addClickInstructions(List<Component> lore, String key) {
        addClickInstructions(lore, key, false);
    }

    private void addClickInstructions(List<Component> lore, String key, boolean hasMiddleClick) {
        lore.add(Component.text(lang.getMessage("conditions_gui." + key + ".click_to_increase"), NamedTextColor.GREEN)
                .decoration(TextDecoration.ITALIC, true));
        lore.add(Component.text(lang.getMessage("conditions_gui." + key + ".click_to_decrease"), NamedTextColor.RED)
                .decoration(TextDecoration.ITALIC, true));
        lore.add(Component.text(lang.getMessage("conditions_gui." + key + ".shift_click"), NamedTextColor.GOLD)
                .decoration(TextDecoration.ITALIC, true));
        if (hasMiddleClick) {
            String middleClick = lang.getMessage("conditions_gui." + key + ".middle_click");
            if (middleClick != null && !middleClick.startsWith("conditions_gui.")) {
                lore.add(Component.text(middleClick, NamedTextColor.LIGHT_PURPLE)
                        .decoration(TextDecoration.ITALIC, true));
            }
        }
    }

    private void addSaveButton() {
        ItemStack button = new ItemStack(Material.LIME_WOOL);
        ItemMeta meta = button.getItemMeta();
        meta.displayName(Component.text(lang.getMessage("conditions_gui.save_and_return"), NamedTextColor.GREEN)
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false));
        button.setItemMeta(meta);
        inventory.setItem(SAVE_SLOT, button);
    }

    private void addClearAllButton() {
        ItemStack button = new ItemStack(Material.BARRIER);
        ItemMeta meta = button.getItemMeta();
        meta.displayName(Component.text(lang.getMessage("conditions_gui.clear_all"), NamedTextColor.RED)
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false));
        button.setItemMeta(meta);
        inventory.setItem(CLEAR_SLOT, button);
    }

    private void addBackButton() {
        ItemStack button = new ItemStack(Material.ARROW);
        ItemMeta meta = button.getItemMeta();
        meta.displayName(Component.text(lang.getMessage("editor.button.back_title"), NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));
        button.setItemMeta(meta);
        inventory.setItem(BACK_SLOT, button);
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
        boolean isShift = event.isShiftClick();
        boolean isMiddle = clickType == ClickType.MIDDLE;

        if (slot == PERMISSION_SLOT) {
            if (clickType.isRightClick() && permission != null) {
                permission = null;
                updateInventory();
            } else if (clickType.isLeftClick()) {
                awaitingPermissionInput = true;
                player.closeInventory();
                MessageUtil.sendAdminInfo(player, lang.getMessage("conditions_gui.permission.enter_permission"));
            }
            return;
        }

        if (slot == XP_LEVEL_SLOT) {
            int delta = isShift ? 10 : 1;
            if (clickType.isLeftClick()) {
                requiredXpLevel = Math.min(requiredXpLevel + delta, 1000);
            } else if (clickType.isRightClick()) {
                requiredXpLevel = Math.max(requiredXpLevel - delta, 0);
            }
            updateInventory();
            return;
        }

        if (slot == XP_REWARD_SLOT) {
            int delta = isShift ? 10 : 1;
            if (clickType.isLeftClick()) {
                xpReward = Math.min(xpReward + delta, 10000);
            } else if (clickType.isRightClick()) {
                xpReward = Math.max(xpReward - delta, -10000);
            }
            updateInventory();
            return;
        }

        if (slot == COOLDOWN_SLOT) {
            
            int delta = isShift ? 10 : 1;
            if (clickType.isLeftClick()) {
                cooldownSeconds = Math.min(cooldownSeconds + delta, 86400); 
            } else if (clickType.isRightClick()) {
                cooldownSeconds = Math.max(cooldownSeconds - delta, 0);
            }
            updateInventory();
            return;
        }

        if (slot == DAILY_LIMIT_SLOT) {
            int delta = isShift ? 10 : 1;
            if (clickType.isLeftClick()) {
                craftLimitDaily = Math.min(craftLimitDaily + delta, 1000);
            } else if (clickType.isRightClick()) {
                craftLimitDaily = Math.max(craftLimitDaily - delta, 0);
            }
            updateInventory();
            return;
        }

        if (slot == WEEKLY_LIMIT_SLOT) {
            int delta = isShift ? 10 : 1;
            if (clickType.isLeftClick()) {
                craftLimitWeekly = Math.min(craftLimitWeekly + delta, 1000);
            } else if (clickType.isRightClick()) {
                craftLimitWeekly = Math.max(craftLimitWeekly - delta, 0);
            }
            updateInventory();
            return;
        }

        if (slot == TOTAL_LIMIT_SLOT) {
            int delta = isShift ? 10 : 1;
            if (clickType.isLeftClick()) {
                craftLimitTotal = Math.min(craftLimitTotal + delta, 10000);
            } else if (clickType.isRightClick()) {
                craftLimitTotal = Math.max(craftLimitTotal - delta, 0);
            }
            updateInventory();
            return;
        }

        if (slot == MONEY_COST_SLOT && plugin.getVaultIntegration().isEnabled()) {
            double delta = isMiddle ? 100 : (isShift ? 10 : 1);
            if (clickType.isLeftClick() || isMiddle) {
                moneyCost = Math.min(moneyCost + delta, 1000000);
            } else if (clickType.isRightClick()) {
                moneyCost = Math.max(moneyCost - delta, 0);
            }
            updateInventory();
            return;
        }

        if (slot == SAVE_SLOT) {
            saveConditions();
            player.closeInventory();
            if (onReturn != null) {
                Bukkit.getScheduler().runTask(plugin, onReturn);
            }
            return;
        }

        if (slot == CLEAR_SLOT) {
            permission = null;
            requiredXpLevel = 0;
            xpReward = 0;
            cooldownSeconds = 0;
            craftLimitDaily = 0;
            craftLimitWeekly = 0;
            craftLimitTotal = 0;
            moneyCost = 0;
            updateInventory();
            return;
        }

        if (slot == BACK_SLOT) {
            player.closeInventory();
            if (onReturn != null) {
                Bukkit.getScheduler().runTask(plugin, onReturn);
            }
        }
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        if (!event.getPlayer().equals(player) || !awaitingPermissionInput) {
            return;
        }

        event.setCancelled(true);
        awaitingPermissionInput = false;
        String message = event.getMessage().trim();

        if (message.equalsIgnoreCase("cancel")) {
            Bukkit.getScheduler().runTask(plugin, this::open);
            return;
        }

        permission = message;
        Bukkit.getScheduler().runTask(plugin, () -> {
            MessageUtil.sendAdminSuccess(player, "Permission set to: " + permission);
            updateInventory();
            open();
        });
    }

    private void saveConditions() {
        try {
            
            File recipeFile = plugin.getConfigManager().getRecipeFileManager().findRecipeFile(recipe.getKey());

            if (recipeFile == null || !recipeFile.exists()) {
                MessageUtil.sendError(player, lang.getMessage("editor.error.file_not_found"));
                return;
            }

            YamlConfiguration config = YamlConfiguration.loadConfiguration(recipeFile);

            boolean hasAnyCondition = (permission != null && !permission.isEmpty())
                    || requiredXpLevel > 0
                    || xpReward != 0
                    || cooldownSeconds > 0
                    || craftLimitDaily > 0
                    || craftLimitWeekly > 0
                    || craftLimitTotal > 0
                    || moneyCost > 0;

            if (!hasAnyCondition) {
                config.set("conditions", null);
            } else {
                setOrRemove(config, "conditions.permission", permission, s -> s != null && !s.isEmpty());
                setOrRemove(config, "conditions.xp-level", requiredXpLevel, v -> v > 0);
                setOrRemove(config, "conditions.xp-reward", xpReward, v -> v != 0);
                setOrRemove(config, "conditions.cooldown", cooldownSeconds, v -> v > 0);
                setOrRemove(config, "conditions.limit-daily", craftLimitDaily, v -> v > 0);
                setOrRemove(config, "conditions.limit-weekly", craftLimitWeekly, v -> v > 0);
                setOrRemove(config, "conditions.limit-total", craftLimitTotal, v -> v > 0);
                setOrRemove(config, "conditions.money-cost", moneyCost, v -> v > 0);
            }

            config.save(recipeFile);

            plugin.getRecipeManager().unregisterAll();
            plugin.getConfigManager().loadRecipes();
            plugin.getRecipeManager().registerAllRecipes();

            MessageUtil.sendAdminSuccess(player, lang.getMessage("conditions_gui.saved"));
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to save conditions: " + e.getMessage());
            MessageUtil.sendError(player, "Failed to save conditions: " + e.getMessage());
        }
    }

    private <T> void setOrRemove(YamlConfiguration config, String path, T value, java.util.function.Predicate<T> hasValue) {
        if (hasValue.test(value)) {
            config.set(path, value);
        } else {
            config.set(path, null);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().equals(inventory) && event.getPlayer().equals(player)) {
            if (!awaitingPermissionInput) {
                InventoryClickEvent.getHandlerList().unregister(this);
                InventoryCloseEvent.getHandlerList().unregister(this);
                AsyncPlayerChatEvent.getHandlerList().unregister(this);
            }
        }
    }
}
