package org.hikarii.customrecipes.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
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

public class RecipeManagementGUI implements Listener, GUIBase {
    private final CustomRecipes plugin;
    private final Player player;
    private final Inventory inventory;
    private final LanguageManager lang;
    private final SettingsGUI parentGUI;

    private static final int ENABLE_ALL_CUSTOM_SLOT = 10;
    private static final int DISABLE_ALL_CUSTOM_SLOT = 11;
    private static final int ENABLE_ALL_VANILLA_SLOT = 15;
    private static final int DISABLE_ALL_VANILLA_SLOT = 16;
    private static final int WORLD_RESTRICTION_SLOT = 22;
    private static final int WORLD_OVERWORLD_SLOT = 29;
    private static final int WORLD_NETHER_SLOT = 31;
    private static final int WORLD_END_SLOT = 33;
    private static final int BACK_SLOT = 53;

    public RecipeManagementGUI(CustomRecipes plugin, Player player, SettingsGUI parentGUI) {
        this.plugin = plugin;
        this.player = player;
        this.parentGUI = parentGUI;
        this.lang = plugin.getLanguageManager();
        this.inventory = Bukkit.createInventory(
                null,
                54,
                MessageUtil.createGradientMenuTitle(lang.getMessage("settings.recipe_management_title"))
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
        addEnableAllCustomButton();
        addDisableAllCustomButton();
        addEnableAllVanillaButton();
        addDisableAllVanillaButton();
        addWorldRestrictionSetting();
        addWorldSettingsButtons();
        addBackButton();
    }

    private void fillBorders() {
        ItemStack borderPane = new ItemStack(Material.CYAN_STAINED_GLASS_PANE);
        ItemMeta meta = borderPane.getItemMeta();
        meta.displayName(Component.empty());
        borderPane.setItemMeta(meta);
        for (int i = 0; i < 54; i++) {
            inventory.setItem(i, borderPane);
        }
    }

    private void addEnableAllCustomButton() {
        int enabledCount = (int) plugin.getRecipeManager().getAllRecipes().stream()
                .filter(recipe -> plugin.getRecipeManager().isRecipeEnabled(recipe.getKey()))
                .count();
        int totalCount = plugin.getRecipeManager().getRecipeCount();

        ItemStack button = new ItemStack(Material.LIME_DYE);
        ItemMeta meta = button.getItemMeta();
        meta.displayName(Component.text(lang.getMessage("settings.enable_all_custom_title"), NamedTextColor.GREEN)
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text(lang.getMessage("settings.total_recipes").replace("{count}", String.valueOf(totalCount)), NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text(lang.getMessage("settings.currently_enabled").replace("{count}", String.valueOf(enabledCount)), NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(Component.text(lang.getMessage("settings.enable_all_custom_desc_line1"), NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(Component.text(lang.getMessage("settings.click_to_enable_all"), NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, true));
        meta.lore(lore);
        button.setItemMeta(meta);
        inventory.setItem(ENABLE_ALL_CUSTOM_SLOT, button);
    }

    private void addDisableAllCustomButton() {
        int disabledCount = (int) plugin.getRecipeManager().getAllRecipes().stream()
                .filter(recipe -> !plugin.getRecipeManager().isRecipeEnabled(recipe.getKey()))
                .count();
        int totalCount = plugin.getRecipeManager().getRecipeCount();
        boolean allDisabled = disabledCount == totalCount && totalCount > 0;

        ItemStack button = new ItemStack(allDisabled ? Material.RED_DYE : Material.BARRIER);
        ItemMeta meta = button.getItemMeta();
        meta.displayName(Component.text(lang.getMessage("settings.disable_all_custom_title"), NamedTextColor.RED)
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text(lang.getMessage("settings.total_recipes").replace("{count}", String.valueOf(totalCount)), NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text(lang.getMessage("settings.currently_disabled").replace("{count}", String.valueOf(disabledCount)), NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(Component.text(lang.getMessage("settings.disable_all_custom_desc_line1"), NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(Component.text(lang.getMessage("settings.click_to_disable_all"), NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, true));
        meta.lore(lore);
        button.setItemMeta(meta);
        inventory.setItem(DISABLE_ALL_CUSTOM_SLOT, button);
    }

    private void addEnableAllVanillaButton() {
        int totalVanilla = plugin.getVanillaRecipeManager().getAllVanillaRecipes().size();
        int disabledVanilla = (int) plugin.getVanillaRecipeManager().getAllVanillaRecipes().keySet().stream()
                .filter(key -> plugin.getVanillaRecipeManager().isRecipeDisabled(key))
                .count();
        int enabledVanilla = totalVanilla - disabledVanilla;

        ItemStack button = new ItemStack(Material.LIME_DYE);
        ItemMeta meta = button.getItemMeta();
        meta.displayName(Component.text(lang.getMessage("settings.enable_all_vanilla_title"), NamedTextColor.DARK_GREEN)
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text(lang.getMessage("settings.total_recipes").replace("{count}", String.valueOf(totalVanilla)), NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text(lang.getMessage("settings.currently_enabled").replace("{count}", String.valueOf(enabledVanilla)), NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(Component.text(lang.getMessage("settings.enable_all_vanilla_desc_line1"), NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(Component.text(lang.getMessage("settings.click_to_enable_all"), NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, true));
        meta.lore(lore);
        button.setItemMeta(meta);
        inventory.setItem(ENABLE_ALL_VANILLA_SLOT, button);
    }

    private void addDisableAllVanillaButton() {
        int totalVanilla = plugin.getVanillaRecipeManager().getAllVanillaRecipes().size();
        int disabledVanilla = (int) plugin.getVanillaRecipeManager().getAllVanillaRecipes().keySet().stream()
                .filter(key -> plugin.getVanillaRecipeManager().isRecipeDisabled(key))
                .count();
        boolean allDisabled = disabledVanilla == totalVanilla && totalVanilla > 0;

        ItemStack button = new ItemStack(allDisabled ? Material.RED_DYE : Material.BARRIER);
        ItemMeta meta = button.getItemMeta();
        meta.displayName(Component.text(lang.getMessage("settings.disable_all_vanilla_title"), NamedTextColor.DARK_RED)
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text(lang.getMessage("settings.total_recipes").replace("{count}", String.valueOf(totalVanilla)), NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text(lang.getMessage("settings.currently_disabled").replace("{count}", String.valueOf(disabledVanilla)), NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(Component.text(lang.getMessage("settings.disable_all_vanilla_desc_line1"), NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(Component.text(lang.getMessage("settings.disable_all_vanilla_warning_line1"), NamedTextColor.GOLD)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(Component.text(lang.getMessage("settings.click_to_disable_all"), NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, true));
        meta.lore(lore);
        button.setItemMeta(meta);
        inventory.setItem(DISABLE_ALL_VANILLA_SLOT, button);
    }

    private void addWorldRestrictionSetting() {
        List<String> disabledWorlds = plugin.getRecipeWorldManager().getGlobalDisabledWorlds();
        boolean hasRestrictions = !disabledWorlds.isEmpty();
        Material material = hasRestrictions ? Material.LIME_DYE : Material.GRAY_DYE;
        String status = hasRestrictions ? lang.getMessage("lore.status_enabled") : lang.getMessage("lore.status_disabled");
        NamedTextColor color = hasRestrictions ? NamedTextColor.GREEN : NamedTextColor.RED;

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(lang.getMessage("settings.world_restrictions"), NamedTextColor.AQUA)
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text(lang.getMessage("info.status") + " ", NamedTextColor.GRAY)
                .append(Component.text(status, color))
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(Component.text(lang.getMessage("settings.world_restrictions_desc_line1"), NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text(lang.getMessage("settings.world_restrictions_desc_line2"), NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        if (hasRestrictions) {
            lore.add(Component.empty());
            lore.add(Component.text(lang.getMessage("settings.world_restrictions_globally_disabled"), NamedTextColor.GOLD)
                    .decoration(TextDecoration.ITALIC, false));
            for (String world : disabledWorlds) {
                lore.add(Component.text("  • " + world, NamedTextColor.YELLOW)
                        .decoration(TextDecoration.ITALIC, false));
            }
        }
        lore.add(Component.empty());
        lore.add(Component.text("» " + lang.getMessage("lore.click_to_toggle"), NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, true));
        meta.lore(lore);
        item.setItemMeta(meta);
        inventory.setItem(WORLD_RESTRICTION_SLOT, item);
    }

    private void addWorldSettingsButtons() {
        addWorldButton(WORLD_OVERWORLD_SLOT, World.Environment.NORMAL, lang.getMessage("settings.world_overworld"), Material.GRASS_BLOCK);
        addWorldButton(WORLD_NETHER_SLOT, World.Environment.NETHER, lang.getMessage("settings.world_nether"), Material.NETHERRACK);
        addWorldButton(WORLD_END_SLOT, World.Environment.THE_END, lang.getMessage("settings.world_the_end"), Material.END_STONE);
    }

    private void addWorldButton(int slot, World.Environment environment, String displayName, Material icon) {
        ItemStack item = new ItemStack(icon);
        ItemMeta meta = item.getItemMeta();

        List<String> globallyDisabled = plugin.getRecipeWorldManager().getGlobalDisabledWorlds();
        boolean isDisabled = false;
        List<String> worldsOfType = new ArrayList<>();
        for (World world : Bukkit.getWorlds()) {
            if (world.getEnvironment() == environment) {
                worldsOfType.add(world.getName());
                if (globallyDisabled.contains(world.getName())) {
                    isDisabled = true;
                }
            }
        }

        NamedTextColor titleColor = isDisabled ? NamedTextColor.RED : NamedTextColor.GREEN;
        meta.displayName(Component.text(displayName + lang.getMessage("settings.world_settings_suffix"), titleColor)
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text(lang.getMessage("info.status") + " ", NamedTextColor.GRAY)
                .append(Component.text(isDisabled ? lang.getMessage("settings.world_recipes_disabled") : lang.getMessage("settings.world_recipes_enabled"),
                        isDisabled ? NamedTextColor.RED : NamedTextColor.GREEN))
                .decoration(TextDecoration.ITALIC, false));
        if (!worldsOfType.isEmpty()) {
            lore.add(Component.empty());
            lore.add(Component.text(lang.getMessage("settings.worlds_label"), NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false));
            for (String worldName : worldsOfType) {
                boolean worldDisabled = globallyDisabled.contains(worldName);
                lore.add(Component.text("  • " + worldName,
                                worldDisabled ? NamedTextColor.RED : NamedTextColor.GREEN)
                        .decoration(TextDecoration.ITALIC, false));
            }
        }
        lore.add(Component.empty());
        lore.add(Component.text("» " + lang.getMessage("lore.click_to_toggle"), NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, true));
        meta.lore(lore);
        item.setItemMeta(meta);
        inventory.setItem(slot, item);
    }

    private void addBackButton() {
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta meta = back.getItemMeta();
        meta.displayName(Component.text("« " + lang.getMessage("button.back_to_settings"), NamedTextColor.YELLOW)
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

        if (slot == BACK_SLOT) {
            parentGUI.refreshAndOpen();
            return;
        }

        if (!player.hasPermission("customrecipes.manage")) {
            MessageUtil.sendError(player, lang.getMessage("settings.no_permission_change"));
            return;
        }

        if (slot == ENABLE_ALL_CUSTOM_SLOT) {
            for (org.hikarii.customrecipes.recipe.CustomRecipe recipe : plugin.getRecipeManager().getAllRecipes()) {
                String key = recipe.getKey();
                if (!plugin.getRecipeManager().isRecipeEnabled(key)) {
                    plugin.getRecipeManager().registerSingleRecipe(recipe);
                    plugin.getConfigManager().addEnabledRecipe(key);
                }
            }
            MessageUtil.sendAdminSuccess(player, lang.getMessage("settings.enabled_all_custom"));
            updateInventory();
            return;
        }

        if (slot == DISABLE_ALL_CUSTOM_SLOT) {
            for (org.hikarii.customrecipes.recipe.CustomRecipe recipe : plugin.getRecipeManager().getAllRecipes()) {
                if (plugin.getRecipeManager().isRecipeEnabled(recipe.getKey())) {
                    plugin.getRecipeManager().disableRecipe(recipe.getKey());
                    plugin.getConfigManager().removeEnabledRecipe(recipe.getKey());
                }
            }
            MessageUtil.sendAdminWarning(player, lang.getMessage("settings.disabled_all_custom"));
            updateInventory();
            return;
        }

        if (slot == ENABLE_ALL_VANILLA_SLOT) {
            int enabled = 0;
            for (String recipeKey : plugin.getVanillaRecipeManager().getAllVanillaRecipes().keySet()) {
                if (plugin.getVanillaRecipeManager().isRecipeDisabled(recipeKey)) {
                    plugin.getVanillaRecipeManager().toggleRecipe(recipeKey);
                    enabled++;
                }
            }
            MessageUtil.sendAdminSuccess(player, lang.getMessage("settings.enabled_all_vanilla_count").replace("{count}", String.valueOf(enabled)));
            updateInventory();
            return;
        }

        if (slot == DISABLE_ALL_VANILLA_SLOT) {
            int disabled = 0;
            for (String recipeKey : plugin.getVanillaRecipeManager().getAllVanillaRecipes().keySet()) {
                if (!plugin.getVanillaRecipeManager().isRecipeDisabled(recipeKey)) {
                    plugin.getVanillaRecipeManager().toggleRecipe(recipeKey);
                    disabled++;
                }
            }
            MessageUtil.sendAdminWarning(player, lang.getMessage("settings.disabled_all_vanilla_count").replace("{count}", String.valueOf(disabled)));
            updateInventory();
            return;
        }

        if (slot == WORLD_RESTRICTION_SLOT) {
            boolean newValue = !plugin.getRecipeWorldManager().isGlobalWorldRestrictionEnabled();
            List<String> currentDisabled = plugin.getRecipeWorldManager().getGlobalDisabledWorlds();
            plugin.getRecipeWorldManager().setGlobalWorldRestrictions(newValue, currentDisabled);
            String status = newValue ? lang.getMessage("lore.status_enabled") : lang.getMessage("lore.status_disabled");
            MessageUtil.sendAdminSuccess(player, lang.getMessage("settings.world_restrictions_toggled").replace("{status}", status));
            updateInventory();
            return;
        }

        if (slot == WORLD_OVERWORLD_SLOT || slot == WORLD_NETHER_SLOT || slot == WORLD_END_SLOT) {
            World.Environment env = switch (slot) {
                case WORLD_OVERWORLD_SLOT -> World.Environment.NORMAL;
                case WORLD_NETHER_SLOT -> World.Environment.NETHER;
                case WORLD_END_SLOT -> World.Environment.THE_END;
                default -> null;
            };
            if (env != null) {
                toggleWorldEnvironment(env);
            }
        }
    }

    private void toggleWorldEnvironment(World.Environment environment) {
        List<String> globallyDisabled = new ArrayList<>(plugin.getRecipeWorldManager().getGlobalDisabledWorlds());
        List<String> worldsOfType = new ArrayList<>();
        for (World world : Bukkit.getWorlds()) {
            if (world.getEnvironment() == environment) {
                worldsOfType.add(world.getName());
            }
        }

        if (worldsOfType.isEmpty()) {
            MessageUtil.sendAdminWarning(player, lang.getMessage("settings.no_worlds_found"));
            return;
        }

        String worldName = switch (environment) {
            case NORMAL -> lang.getMessage("settings.world_overworld");
            case NETHER -> lang.getMessage("settings.world_nether");
            case THE_END -> lang.getMessage("settings.world_the_end");
            default -> environment.name().toLowerCase().replace("_", " ");
        };

        boolean anyDisabled = worldsOfType.stream().anyMatch(globallyDisabled::contains);
        if (anyDisabled) {
            globallyDisabled.removeAll(worldsOfType);
            MessageUtil.sendAdminSuccess(player, lang.getMessage("settings.enabled_recipes_in_worlds").replace("{world}", worldName.toLowerCase()));
        } else {
            globallyDisabled.addAll(worldsOfType);
            MessageUtil.sendAdminWarning(player, lang.getMessage("settings.disabled_recipes_in_worlds").replace("{world}", worldName.toLowerCase()));
        }
        plugin.getRecipeWorldManager().setGlobalWorldRestrictions(true, globallyDisabled);
        updateInventory();
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
