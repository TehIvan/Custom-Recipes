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
import org.hikarii.customrecipes.recipe.CustomRecipe;
import org.hikarii.customrecipes.recipe.RecipeType;
import org.hikarii.customrecipes.recipe.data.RandomResultPool;
import org.hikarii.customrecipes.recipe.data.RecipeIngredient;
import org.hikarii.customrecipes.util.MessageUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RecipeViewerGUI implements Listener {
    private final CustomRecipes plugin;
    private final LanguageManager lang;
    private final Player player;
    private final CustomRecipe recipe;
    private final PlayerRecipeListGUI parentGUI;
    private final Inventory inventory;

    private static final int RESULT_SLOT = 25;
    private static final int[] GRID_SLOTS = {10, 11, 12, 19, 20, 21, 28, 29, 30};
    private static final int BACK_BUTTON_SLOT = 53;
    private static final int INFO_SLOT = 44;
    private static final int RANDOM_RESULTS_SLOT = 52;

    private static final int FURNACE_INPUT_SLOT = 12;
    private static final int FURNACE_FUEL_SLOT = 30;
    private static final int FURNACE_ICON_SLOT = 21;

    public RecipeViewerGUI(CustomRecipes plugin, Player player, CustomRecipe recipe, PlayerRecipeListGUI parentGUI) {
        this.plugin = plugin;
        this.lang = plugin.getLanguageManager();
        this.player = player;
        this.recipe = recipe;
        this.parentGUI = parentGUI;

        this.inventory = Bukkit.createInventory(
                null,
                54,
                MessageUtil.createGradientMenuTitle(lang.getMessage("recipe_viewer.title"))
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
        addCraftingGrid();
        addEqualsSign();
        addResultItem();
        addInfoBook();
        addRandomResultsButton();
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

        if (recipe.getType() == RecipeType.SHAPED || recipe.getType() == RecipeType.SHAPELESS) {
            for (int slot : GRID_SLOTS) {
                inventory.setItem(slot, null);
            }
        } else if (recipe.getType().isFurnaceType()) {
            
            inventory.setItem(FURNACE_INPUT_SLOT, null);
            inventory.setItem(FURNACE_FUEL_SLOT, null);
            inventory.setItem(FURNACE_ICON_SLOT, null);
        }
        inventory.setItem(RESULT_SLOT, null);
    }

    private void addCraftingGrid() {
        if (recipe.getType() == RecipeType.SHAPED) {
            List<RecipeIngredient> ingredients = recipe.getRecipeData().ingredients();
            for (int i = 0; i < Math.min(GRID_SLOTS.length, ingredients.size()); i++) {
                RecipeIngredient ingredient = ingredients.get(i);
                if (ingredient.material() != Material.AIR) {
                    ItemStack displayItem;
                    if (ingredient.hasExactItem()) {
                        displayItem = ingredient.getExactItem().clone();
                    } else {
                        displayItem = new ItemStack(ingredient.material(), ingredient.amount());
                    }
                    ItemMeta meta = displayItem.getItemMeta();
                    if (meta != null) {
                        List<Component> lore = meta.hasLore() ? new ArrayList<>(meta.lore()) : new ArrayList<>();
                        lore.add(Component.empty());
                        lore.add(Component.text(lang.getMessage("recipe_viewer.amount_label", Map.of("amount", String.valueOf(ingredient.amount()))), NamedTextColor.GRAY)
                                .decoration(TextDecoration.ITALIC, false));
                        if (ingredient.hasExactItem()) {
                            lore.add(Component.text(lang.getMessage("recipe_viewer.exact_item_required"), NamedTextColor.YELLOW)
                                    .decoration(TextDecoration.ITALIC, false));
                        }
                        meta.lore(lore);
                        displayItem.setItemMeta(meta);
                    }
                    inventory.setItem(GRID_SLOTS[i], displayItem);
                }
            }
        } else if (recipe.getType() == RecipeType.SHAPELESS) {
            var shapelessData = recipe.getShapelessData();
            if (shapelessData != null) {
                List<Material> expandedMaterials = shapelessData.getExpandedMaterials();
                List<ItemStack> exactIngredients = shapelessData.exactIngredients();

                for (int i = 0; i < Math.min(GRID_SLOTS.length, expandedMaterials.size()); i++) {
                    Material material = expandedMaterials.get(i);
                    ItemStack displayItem;

                    if (exactIngredients != null && i < exactIngredients.size() && exactIngredients.get(i) != null) {
                        displayItem = exactIngredients.get(i).clone();
                    } else {
                        displayItem = new ItemStack(material, 1);
                    }

                    ItemMeta meta = displayItem.getItemMeta();
                    if (meta != null) {
                        List<Component> lore = meta.hasLore() ? new ArrayList<>(meta.lore()) : new ArrayList<>();
                        lore.add(Component.empty());
                        lore.add(Component.text(lang.getMessage("recipe_viewer.amount_label", Map.of("amount", "1")), NamedTextColor.GRAY)
                                .decoration(TextDecoration.ITALIC, false));
                        if (exactIngredients != null && i < exactIngredients.size() && exactIngredients.get(i) != null) {
                            lore.add(Component.text(lang.getMessage("recipe_viewer.exact_item_required"), NamedTextColor.YELLOW)
                                    .decoration(TextDecoration.ITALIC, false));
                        }
                        meta.lore(lore);
                        displayItem.setItemMeta(meta);
                    }
                    inventory.setItem(GRID_SLOTS[i], displayItem);
                }
            }
        } else if (recipe.getType().isFurnaceType()) {
            addFurnaceDisplay();
        }
    }

    private void addFurnaceDisplay() {
        
        if (recipe.getFurnaceData() != null) {
            RecipeIngredient input = recipe.getFurnaceData().getInput();
            if (input != null && input.material() != Material.AIR) {
                ItemStack inputItem;
                if (input.hasExactItem()) {
                    inputItem = input.getExactItem().clone();
                } else {
                    inputItem = new ItemStack(input.material(), input.amount());
                }
                ItemMeta meta = inputItem.getItemMeta();
                if (meta != null) {
                    List<Component> lore = meta.hasLore() ? new ArrayList<>(meta.lore()) : new ArrayList<>();
                    lore.add(Component.empty());
                    lore.add(Component.text(lang.getMessage("recipe_viewer.input_item"), NamedTextColor.AQUA)
                            .decoration(TextDecoration.ITALIC, false));
                    if (input.hasExactItem()) {
                        lore.add(Component.text(lang.getMessage("recipe_viewer.exact_item_required"), NamedTextColor.YELLOW)
                                .decoration(TextDecoration.ITALIC, false));
                    }
                    meta.lore(lore);
                    inputItem.setItemMeta(meta);
                }
                inventory.setItem(FURNACE_INPUT_SLOT, inputItem);
            }
        }

        ItemStack fuelItem = new ItemStack(Material.COAL);
        ItemMeta fuelMeta = fuelItem.getItemMeta();
        fuelMeta.displayName(Component.text(lang.getMessage("recipe_viewer.fuel"), NamedTextColor.GOLD)
                .decoration(TextDecoration.ITALIC, false));
        List<Component> fuelLore = new ArrayList<>();
        fuelLore.add(Component.text(lang.getMessage("recipe_viewer.any_fuel"), NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        fuelMeta.lore(fuelLore);
        fuelItem.setItemMeta(fuelMeta);
        inventory.setItem(FURNACE_FUEL_SLOT, fuelItem);

        Material furnaceIcon = getFurnaceIconMaterial();
        ItemStack furnaceItem = new ItemStack(furnaceIcon);
        ItemMeta furnaceMeta = furnaceItem.getItemMeta();
        furnaceMeta.displayName(Component.text(lang.getMessage("type." + recipe.getType().name().toLowerCase()), NamedTextColor.WHITE)
                .decoration(TextDecoration.ITALIC, false));
        List<Component> furnaceLore = new ArrayList<>();
        if (recipe.getFurnaceData() != null) {
            furnaceLore.add(Component.text(lang.getMessage("recipe_viewer.cooking_time",
                    Map.of("time", String.valueOf(recipe.getFurnaceData().getCookingTime() / 20))), NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false));
            furnaceLore.add(Component.text(lang.getMessage("recipe_viewer.experience",
                    Map.of("exp", String.valueOf(recipe.getFurnaceData().getExperience()))), NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false));
        }
        furnaceMeta.lore(furnaceLore);
        furnaceItem.setItemMeta(furnaceMeta);
        inventory.setItem(FURNACE_ICON_SLOT, furnaceItem);
    }

    private Material getFurnaceIconMaterial() {
        return switch (recipe.getType()) {
            case BLAST_FURNACE -> Material.BLAST_FURNACE;
            case SMOKER -> Material.SMOKER;
            case CAMPFIRE -> Material.CAMPFIRE;
            default -> Material.FURNACE;
        };
    }

    private void addEqualsSign() {
        ItemStack equals = new ItemStack(Material.ORANGE_STAINED_GLASS_PANE);
        ItemMeta meta = equals.getItemMeta();
        meta.displayName(Component.text(lang.getMessage("recipe_viewer.equals_sign"), NamedTextColor.GOLD)
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false)
        );
        equals.setItemMeta(meta);
        inventory.setItem(23, equals);
    }

    private void addResultItem() {
        ItemStack resultDisplay = recipe.getResultItem().clone();
        ItemMeta resultMeta = resultDisplay.getItemMeta();
        if (resultMeta != null) {
            List<Component> lore = resultMeta.hasLore() ? new ArrayList<>(resultMeta.lore()) : new ArrayList<>();
            lore.add(Component.empty());
            lore.add(Component.text(lang.getMessage("recipe_viewer.recipe_result"), NamedTextColor.GREEN)
                    .decoration(TextDecoration.BOLD, true)
                    .decoration(TextDecoration.ITALIC, false));
            resultMeta.lore(lore);
            resultDisplay.setItemMeta(resultMeta);
        }
        inventory.setItem(RESULT_SLOT, resultDisplay);
    }

    private void addInfoBook() {
        ItemStack info = new ItemStack(Material.BOOK);
        ItemMeta meta = info.getItemMeta();
        meta.displayName(Component.text(lang.getMessage("recipe_viewer.recipe_information"), NamedTextColor.GOLD)
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        if (recipe.getName() != null && !recipe.getName().isEmpty()) {
            lore.add(Component.text(lang.getMessage("recipe_viewer.name_label"), NamedTextColor.GRAY)
                    .append(MessageUtil.colorize(recipe.getName()))
                    .decoration(TextDecoration.ITALIC, false));
        }

        lore.add(Component.text(lang.getMessage("recipe_viewer.type_label"), NamedTextColor.GRAY)
                .append(Component.text(lang.getMessage("type." + recipe.getType().name().toLowerCase()), NamedTextColor.WHITE))
                .decoration(TextDecoration.ITALIC, false));
        if (recipe.getDescription() != null && !recipe.getDescription().isEmpty()) {
            lore.add(Component.empty());
            lore.add(Component.text(lang.getMessage("recipe_viewer.description_label"), NamedTextColor.YELLOW)
                    .decoration(TextDecoration.ITALIC, false));
            for (String line : recipe.getDescription()) {
                lore.add(Component.text(lang.getMessage("recipe_viewer.description_line", Map.of("line", line)), NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false));
            }
        }
        meta.lore(lore);
        info.setItemMeta(meta);
        inventory.setItem(INFO_SLOT, info);
    }

    private void addBackButton() {
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta meta = back.getItemMeta();
        meta.displayName(Component.text(lang.getMessage("recipe_viewer.back_to_recipe_list"), NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));
        back.setItemMeta(meta);
        inventory.setItem(BACK_BUTTON_SLOT, back);
    }

    private void addRandomResultsButton() {
        RandomResultPool randomResults = recipe.getRandomResults();
        if (randomResults == null || !randomResults.hasRandomResults()) {
            return; 
        }

        ItemStack button = new ItemStack(Material.HOPPER);
        ItemMeta meta = button.getItemMeta();
        meta.displayName(Component.text(lang.getMessage("random_results.view_variants"), NamedTextColor.LIGHT_PURPLE)
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text(lang.getMessage("random_results.view_variants_desc1"), NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text(lang.getMessage("random_results.view_variants_desc2"), NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(Component.text(lang.getMessage("random_results.total_results",
                        Map.of("count", String.valueOf(randomResults.getResults().size()))), NamedTextColor.AQUA)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(Component.text(lang.getMessage("random_results.click_to_view"), NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, true));

        meta.lore(lore);
        button.setItemMeta(meta);
        inventory.setItem(RANDOM_RESULTS_SLOT, button);
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

        int slot = event.getSlot();
        if (slot == BACK_BUTTON_SLOT) {
            InventoryClickEvent.getHandlerList().unregister(this);
            InventoryCloseEvent.getHandlerList().unregister(this);
            parentGUI.reopen();
            return;
        }

        if (slot == RANDOM_RESULTS_SLOT) {
            RandomResultPool randomResults = recipe.getRandomResults();
            if (randomResults != null && randomResults.hasRandomResults()) {
                InventoryClickEvent.getHandlerList().unregister(this);
                InventoryCloseEvent.getHandlerList().unregister(this);
                
                boolean showOnlyAvailable = !player.hasPermission("customrecipes.list.all");
                new RandomResultsViewerGUI(plugin, player, randomResults, showOnlyAvailable, () -> {
                    
                    new RecipeViewerGUI(plugin, player, recipe, parentGUI).open();
                }).open();
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
