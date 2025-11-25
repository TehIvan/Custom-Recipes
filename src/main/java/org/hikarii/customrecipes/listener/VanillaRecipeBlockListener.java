package org.hikarii.customrecipes.listener;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.hikarii.customrecipes.CustomRecipes;
import org.hikarii.customrecipes.recipe.RecipeType;
import org.hikarii.customrecipes.recipe.vanilla.VanillaRecipeManager;
import org.hikarii.customrecipes.recipe.vanilla.VanillaRecipeInfo;
import org.hikarii.customrecipes.recipe.vanilla.IngredientChoice;

import java.util.List;

public class VanillaRecipeBlockListener implements Listener {
    private final CustomRecipes plugin;

    public VanillaRecipeBlockListener(CustomRecipes plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onPrepareItemCraft(PrepareItemCraftEvent event) {
        Recipe recipe = event.getRecipe();
        if (recipe == null) {
            return;
        }

        ItemStack result = event.getInventory().getResult();
        if (result == null || result.getType() == Material.AIR) {
            return;
        }

        Material resultMaterial = result.getType();
        plugin.debug("PrepareItemCraft: Checking result " + resultMaterial.name());

        for (String recipeKey : plugin.getVanillaRecipeManager().getAllDisabledRecipeKeys()) {
            VanillaRecipeInfo info = plugin.getVanillaRecipeManager().getRecipeInfo(recipeKey);

            if (info != null && info.getResultMaterial() == resultMaterial) {

                if (matchesRecipePattern(event.getInventory().getMatrix(), info)) {
                    event.getInventory().setResult(null);
                    plugin.getLogger().info("Blocked disabled vanilla recipe by result: " + recipeKey +
                        " (original key might be different)");
                    return;
                }
            }
        }
    }

    private boolean matchesRecipePattern(ItemStack[] matrix, VanillaRecipeInfo info) {

        return true;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onCraftItem(CraftItemEvent event) {
        Recipe recipe = event.getRecipe();
        if (recipe == null) {
            return;
        }

        ItemStack result = event.getRecipe().getResult();
        if (result == null || result.getType() == Material.AIR) {
            return;
        }

        Material resultMaterial = result.getType();
        plugin.debug("CraftItem: Checking result " + resultMaterial.name());

        for (String recipeKey : plugin.getVanillaRecipeManager().getAllDisabledRecipeKeys()) {
            VanillaRecipeInfo info = plugin.getVanillaRecipeManager().getRecipeInfo(recipeKey);

            if (info != null && info.getResultMaterial() == resultMaterial) {
                
                if (matchesRecipePattern(event.getInventory().getMatrix(), info)) {
                    event.setCancelled(true);
                    plugin.getLogger().info("Cancelled crafting of disabled vanilla recipe: " + recipeKey +
                        " (original key might be different)");
                    return;
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onFurnaceSmelt(FurnaceSmeltEvent event) {
        ItemStack source = event.getSource();
        ItemStack result = event.getResult();

        if (source == null || source.getType() == Material.AIR) {
            return;
        }
        if (result == null || result.getType() == Material.AIR) {
            return;
        }

        Material inputMaterial = source.getType();
        Material resultMaterial = result.getType();

        plugin.debug("[VanillaBlock] FurnaceSmelt: input=" + inputMaterial + ", result=" + resultMaterial);

        VanillaRecipeManager manager = plugin.getVanillaRecipeManager();

        for (VanillaRecipeInfo info : manager.getAllVanillaRecipes().values()) {
            
            if (info.getType() != RecipeType.FURNACE &&
                info.getType() != RecipeType.BLAST_FURNACE &&
                info.getType() != RecipeType.SMOKER &&
                info.getType() != RecipeType.CAMPFIRE) {
                continue;
            }

            if (info.getResultMaterial() != resultMaterial) {
                continue;
            }

            String recipeKey = info.getKey().replace("minecraft:", "");
            List<List<IngredientChoice>> grid = info.getIngredientGrid();

            if (grid.isEmpty() || grid.get(0).isEmpty()) {
                continue;
            }

            IngredientChoice inputChoice = grid.get(0).get(0);
            boolean inputMatches = false;
            int matchingVariantIndex = -1;

            if (inputChoice.hasMultipleOptions()) {
                List<Material> options = inputChoice.getOptions();
                for (int i = 0; i < options.size(); i++) {
                    if (options.get(i) == inputMaterial) {
                        inputMatches = true;
                        matchingVariantIndex = i;
                        break;
                    }
                }
            } else if (inputChoice.getSelected() == inputMaterial) {
                inputMatches = true;
                matchingVariantIndex = 0;
            }

            if (!inputMatches) {
                continue;
            }

            if (manager.isRecipeDisabled(recipeKey)) {
                event.setCancelled(true);
                plugin.debug("[VanillaBlock] Blocked globally disabled furnace recipe: " + recipeKey);
                return;
            }

            if (matchingVariantIndex >= 0 && manager.isVariantDisabled(recipeKey, matchingVariantIndex)) {
                event.setCancelled(true);
                plugin.debug("[VanillaBlock] Blocked disabled variant " + matchingVariantIndex +
                        " of furnace recipe: " + recipeKey);
                return;
            }

            VanillaRecipeManager.VanillaRecipeState state = manager.getRecipeState(recipeKey);
            if (state != null) {
                Integer customAmount = state.getResultAmountForVariant(matchingVariantIndex);
                if (customAmount == null) {
                    customAmount = state.getCustomResultAmount(); 
                }
                if (customAmount != null && customAmount != result.getAmount()) {
                    ItemStack newResult = result.clone();
                    newResult.setAmount(customAmount);
                    event.setResult(newResult);
                    plugin.debug("[VanillaBlock] Applied custom result amount " + customAmount +
                            " for furnace recipe: " + recipeKey + " variant " + matchingVariantIndex);
                }
            }

            return;
        }
    }

}
