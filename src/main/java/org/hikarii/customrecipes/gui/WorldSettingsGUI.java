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
import org.hikarii.customrecipes.recipe.CustomRecipe;
import org.hikarii.customrecipes.recipe.RecipeWorldManager;
import org.hikarii.customrecipes.util.MessageUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class WorldSettingsGUI implements Listener {
    private final CustomRecipes plugin;
    private final LanguageManager lang;
    private final Player player;
    private final CustomRecipe recipe;
    private final Inventory inventory;
    private final List<World> worlds;

    public WorldSettingsGUI(CustomRecipes plugin, Player player, CustomRecipe recipe) {
        this.plugin = plugin;
        this.lang = plugin.getLanguageManager();
        this.player = player;
        this.recipe = recipe;
        this.worlds = new ArrayList<>(Bukkit.getWorlds());
        this.inventory = Bukkit.createInventory(
                null,
                54,
                MessageUtil.createMenuTitle(lang.getMessage("world_settings.title_prefix") + recipe.getKey(), NamedTextColor.DARK_AQUA)
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
        addInfoItem();
        addWorldButtons();
        addEnableAllButton();
        addDisableAllButton();
        addBackButton();
    }

    private void fillBorders() {
        ItemStack borderPane = new ItemStack(Material.CYAN_STAINED_GLASS_PANE);
        ItemMeta meta = borderPane.getItemMeta();
        meta.displayName(Component.empty());
        borderPane.setItemMeta(meta);
        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, borderPane);
            inventory.setItem(45 + i, borderPane);
        }

        for (int i = 1; i < 5; i++) {
            inventory.setItem(i * 9, borderPane);
            inventory.setItem((i * 9) + 8, borderPane);
        }
    }

    private void addInfoItem() {
        ItemStack info = new ItemStack(Material.BOOK);
        ItemMeta meta = info.getItemMeta();
        meta.displayName(Component.text(lang.getMessage("world_settings.world_restrictions"), NamedTextColor.GOLD)
                .decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text(lang.getMessage("world_settings.configure_which_worlds"), NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text(lang.getMessage("world_settings.this_recipe_works_in"), NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(Component.text(lang.getMessage("world_settings.recipe_enabled"), NamedTextColor.GREEN)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text(lang.getMessage("world_settings.recipe_disabled"), NamedTextColor.RED)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(Component.text(lang.getMessage("world_settings.click_world_to_toggle"), NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);
        info.setItemMeta(meta);
        inventory.setItem(4, info);
    }

    private void addWorldButtons() {
        List<String> disabledWorlds = plugin.getRecipeWorldManager().getDisabledWorlds(recipe.getKey());
        int slot = 10;
        for (World world : worlds) {
            if (slot == 17 || slot == 26 || slot == 35) {
                slot += 2;
            }

            if (slot > 43) {
                break;
            }
            boolean isEnabled = !disabledWorlds.contains(world.getName());
            addWorldButton(slot, world, isEnabled);
            slot++;
        }
    }

    private void addWorldButton(int slot, World world, boolean enabled) {
        Material icon = switch (world.getEnvironment()) {
            case NORMAL -> Material.GRASS_BLOCK;
            case NETHER -> Material.NETHERRACK;
            case THE_END -> Material.END_STONE;
            default -> Material.COMMAND_BLOCK;
        };
        ItemStack item = new ItemStack(icon);
        ItemMeta meta = item.getItemMeta();
        NamedTextColor nameColor = enabled ? NamedTextColor.GREEN : NamedTextColor.RED;
        meta.displayName(Component.text(world.getName(), nameColor)
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());

        String worldType = switch (world.getEnvironment()) {
            case NORMAL -> lang.getMessage("world_settings.overworld");
            case NETHER -> lang.getMessage("world_settings.nether");
            case THE_END -> lang.getMessage("world_settings.the_end");
            default -> lang.getMessage("world_settings.custom");
        };
        lore.add(Component.text(lang.getMessage("world_settings.type_label"), NamedTextColor.GRAY)
                .append(Component.text(worldType, NamedTextColor.WHITE))
                .decoration(TextDecoration.ITALIC, false));
        int playerCount = world.getPlayers().size();
        lore.add(Component.text(lang.getMessage("world_settings.players_label"), NamedTextColor.GRAY)
                .append(Component.text(lang.getMessage("world_settings.players_online", Map.of("count", String.valueOf(playerCount))), NamedTextColor.WHITE))
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(Component.text(lang.getMessage("world_settings.recipe_status"), NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text(enabled ? lang.getMessage("world_settings.enabled") : lang.getMessage("world_settings.disabled"),
                        enabled ? NamedTextColor.GREEN : NamedTextColor.RED)
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(Component.text(enabled ? lang.getMessage("world_settings.click_to_disable") : lang.getMessage("world_settings.click_to_enable"), NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, true));
        if (enabled) {
            org.bukkit.enchantments.Enchantment unbreaking = org.bukkit.enchantments.Enchantment.getByKey(org.bukkit.NamespacedKey.minecraft("unbreaking"));
            if (unbreaking != null) {
                meta.addEnchant(unbreaking, 1, true);
                meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
            }
        }
        meta.lore(lore);
        item.setItemMeta(meta);
        inventory.setItem(slot, item);
    }

    private void addEnableAllButton() {
        ItemStack button = new ItemStack(Material.LIME_WOOL);
        ItemMeta meta = button.getItemMeta();
        meta.displayName(Component.text(lang.getMessage("world_settings.enable_in_all_worlds"), NamedTextColor.GREEN)
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text(lang.getMessage("world_settings.allow_recipe_in"), NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text(lang.getMessage("world_settings.work_in_all_worlds"), NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(Component.text(lang.getMessage("world_settings.click_to_enable_all"), NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, true));
        meta.lore(lore);
        button.setItemMeta(meta);
        inventory.setItem(47, button);
    }

    private void addDisableAllButton() {
        ItemStack button = new ItemStack(Material.RED_WOOL);
        ItemMeta meta = button.getItemMeta();
        meta.displayName(Component.text(lang.getMessage("world_settings.disable_in_all_worlds"), NamedTextColor.RED)
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text(lang.getMessage("world_settings.prevent_recipe_from"), NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text(lang.getMessage("world_settings.working_in_all_worlds"), NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(Component.text(lang.getMessage("world_settings.click_to_disable_all"), NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, true));
        meta.lore(lore);
        button.setItemMeta(meta);
        inventory.setItem(51, button);
    }

    private void addBackButton() {
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta meta = back.getItemMeta();
        meta.displayName(Component.text(lang.getMessage("world_settings.back_to_recipe_editor"), NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));
        back.setItemMeta(meta);
        inventory.setItem(49, back);
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
        if (slot == 49) {
            new RecipeEditorGUI(plugin, player, recipe).open();
            return;
        }

        if (!player.hasPermission("customrecipes.manage")) {
            MessageUtil.sendError(player, lang.getMessage("world_settings.no_permission_manage"));
            return;
        }

        if (slot == 47) {
            plugin.getRecipeWorldManager().setRecipeWorldRestrictions(recipe.getKey(), new ArrayList<>());
            MessageUtil.sendAdminSuccess(player, lang.getMessage("world_settings.enabled_recipe_in_all"));
            updateInventory();
            return;
        }

        if (slot == 51) {
            List<String> allWorlds = worlds.stream().map(World::getName).toList();
            plugin.getRecipeWorldManager().setRecipeWorldRestrictions(recipe.getKey(), allWorlds);
            MessageUtil.sendAdminWarning(player, lang.getMessage("world_settings.disabled_recipe_in_all"));
            updateInventory();
            return;
        }

        if (slot >= 10 && slot <= 43 &&
                slot % 9 != 0 && slot % 9 != 8) {
            int index = calculateWorldIndex(slot);
            if (index < 0 || index >= worlds.size()) {
                return;
            }

            World world = worlds.get(index);
            plugin.getRecipeWorldManager().toggleWorldRestriction(recipe.getKey(), world.getName());
            List<String> disabledWorlds = plugin.getRecipeWorldManager().getDisabledWorlds(recipe.getKey());
            boolean nowEnabled = !disabledWorlds.contains(world.getName());

            if (nowEnabled) {
                MessageUtil.sendAdminSuccess(player, lang.getMessage("world_settings.enabled_recipe_in_world", Map.of("world", world.getName())));
            } else {
                MessageUtil.sendAdminWarning(player, lang.getMessage("world_settings.disabled_recipe_in_world", Map.of("world", world.getName())));
            }
            updateInventory();
        }
    }

    private int calculateWorldIndex(int slot) {
        int row = slot / 9;
        int col = slot % 9;
        if (row < 1 || row > 4 || col < 1 || col > 7) {
            return -1;
        }
        int baseIndex = (row - 1) * 7;
        int index = baseIndex + (col - 1);
        return index;
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().equals(inventory) && event.getPlayer().equals(player)) {
            InventoryClickEvent.getHandlerList().unregister(this);
            InventoryCloseEvent.getHandlerList().unregister(this);
        }
    }
}