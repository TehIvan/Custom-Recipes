package org.hikarii.customrecipes.listener;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.hikarii.customrecipes.CustomRecipes;
import org.hikarii.customrecipes.integration.VaultIntegration;
import org.hikarii.customrecipes.language.LanguageManager;
import org.hikarii.customrecipes.recipe.CraftEvents;
import org.hikarii.customrecipes.recipe.CraftTracker;
import org.hikarii.customrecipes.recipe.CustomRecipe;
import org.hikarii.customrecipes.recipe.RecipeConditions;
import org.hikarii.customrecipes.recipe.RecipeType;
import org.hikarii.customrecipes.util.MessageUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.hikarii.customrecipes.recipe.data.RecipeIngredient;

public class RecipeCraftListener implements Listener {
    private final CustomRecipes plugin;

    public RecipeCraftListener(CustomRecipes plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCraftItem(CraftItemEvent event) {
        if (event.isCancelled()) {
            return;
        }

        Recipe recipe = event.getRecipe();
        if (recipe == null) {
            return;
        }

        NamespacedKey key = null;
        if (recipe instanceof ShapedRecipe shapedRecipe) {
            key = shapedRecipe.getKey();
        } else if (recipe instanceof org.bukkit.inventory.ShapelessRecipe shapelessRecipe) {
            key = shapelessRecipe.getKey();
        }

        if (key == null || !key.getNamespace().equals(plugin.getName().toLowerCase())) {
            return;
        }

        String recipeKey = key.getKey();
        CustomRecipe customRecipe = plugin.getRecipeManager().getRecipe(recipeKey);
        if (customRecipe == null) {
            return;
        }

        if (customRecipe.getType() == RecipeType.SHAPELESS) {
            handleShapelessCraft(event, customRecipe);
            return;
        }

        if (customRecipe.getType() != RecipeType.SHAPED) {
            return;
        }

        if (plugin.isDebugMode()) {
            plugin.getLogger().info("[CRAFT] Recipe selected by Bukkit: " + recipeKey);
        }

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        CraftingInventory inventory = event.getInventory();
        ItemStack[] matrix = inventory.getMatrix();
        CustomRecipe exactMatchRecipe = findExactMatchRecipe(matrix, player);
        if (exactMatchRecipe != null && !exactMatchRecipe.getKey().equals(recipeKey)) {
            if (plugin.isDebugMode()) {
                plugin.getLogger().info("[CRAFT] Found more specific recipe with exact-ingredients: " +
                    exactMatchRecipe.getKey());
            }
            customRecipe = exactMatchRecipe;
            boolean useCraftedNames = plugin.isUseCraftedCustomNames();
            boolean keepSpawnEggNames = plugin.isKeepSpawnEggNames();
            ItemStack correctResult = exactMatchRecipe.createResult(useCraftedNames, keepSpawnEggNames);
            inventory.setResult(correctResult);
            if (plugin.isDebugMode()) {
                plugin.getLogger().info("[CRAFT] Replaced result with: " + correctResult.getType());
            }
        }
        List<RecipeIngredient> ingredients = customRecipe.getRecipeData().ingredients();
        for (int i = 0; i < Math.min(matrix.length, ingredients.size()); i++) {
            RecipeIngredient required = ingredients.get(i);
            ItemStack actual = matrix[i];
            if (required.material() == Material.AIR) {
                continue;
            }

            if (actual == null || actual.getType() != required.material()) {
                continue; 
            }

            if (plugin.isDebugMode()) {
                plugin.getLogger().info("[CRAFT] Slot " + i + ": hasExactItem=" + required.hasExactItem());
            }

            if (required.hasExactItem()) {
                ItemStack exactItem = required.getExactItem();
                if (plugin.isDebugMode()) {
                    plugin.getLogger().info("[CRAFT] Slot " + i + " exact: " + exactItem.getType() +
                        " actual: " + actual.getType());
                }

                if (!org.hikarii.customrecipes.recipe.data.IngredientMatcher.matches(exactItem, actual, false)) {
                    if (plugin.isDebugMode()) {
                        plugin.getLogger().info("[CRAFT] CANCELLED: ingredient properties don't match at slot " + i);
                    }
                    event.setCancelled(true);
                    return;
                }
            }
        }

        if (plugin.isDebugMode()) {
            plugin.getLogger().info("[CRAFT] All checks passed!");
        }

        if (!plugin.getRecipeWorldManager().isRecipeAllowedForPlayer(recipeKey, player)) {
            event.setCancelled(true);
            return;
        }

        RecipeConditions conditions = customRecipe.getConditions();
        if (conditions != null && conditions.hasAnyCondition()) {
            LanguageManager lang = plugin.getLanguageManager();
            CraftTracker tracker = plugin.getCraftTracker();

            if (conditions.hasPermission() && !player.hasPermission(conditions.getPermission())) {
                event.setCancelled(true);
                MessageUtil.sendError(player, lang.getMessage("conditions.no_permission",
                        Map.of("permission", conditions.getPermission())));
                return;
            }

            if (conditions.hasXpLevelRequirement() && player.getLevel() < conditions.getRequiredXpLevel()) {
                event.setCancelled(true);
                MessageUtil.sendError(player, lang.getMessage("conditions.not_enough_xp_level",
                        Map.of("required", String.valueOf(conditions.getRequiredXpLevel()),
                               "current", String.valueOf(player.getLevel()))));
                return;
            }

            if (conditions.hasCooldown()) {
                int remaining = tracker.getRemainingCooldown(player.getUniqueId(), recipeKey, conditions.getCooldownSeconds());
                if (remaining > 0) {
                    event.setCancelled(true);
                    MessageUtil.sendError(player, lang.getMessage("conditions.on_cooldown",
                            Map.of("time", CraftTracker.formatTime(remaining))));
                    return;
                }
            }

            if (conditions.hasDailyLimit()) {
                int crafted = tracker.getDailyCrafts(player.getUniqueId(), recipeKey);
                if (crafted >= conditions.getCraftLimitDaily()) {
                    event.setCancelled(true);
                    MessageUtil.sendError(player, lang.getMessage("conditions.daily_limit_reached",
                            Map.of("limit", String.valueOf(conditions.getCraftLimitDaily()))));
                    return;
                }
            }

            if (conditions.hasWeeklyLimit()) {
                int crafted = tracker.getWeeklyCrafts(player.getUniqueId(), recipeKey);
                if (crafted >= conditions.getCraftLimitWeekly()) {
                    event.setCancelled(true);
                    MessageUtil.sendError(player, lang.getMessage("conditions.weekly_limit_reached",
                            Map.of("limit", String.valueOf(conditions.getCraftLimitWeekly()))));
                    return;
                }
            }

            if (conditions.hasTotalLimit()) {
                int crafted = tracker.getTotalCrafts(player.getUniqueId(), recipeKey);
                if (crafted >= conditions.getCraftLimitTotal()) {
                    event.setCancelled(true);
                    MessageUtil.sendError(player, lang.getMessage("conditions.total_limit_reached",
                            Map.of("limit", String.valueOf(conditions.getCraftLimitTotal()))));
                    return;
                }
            }

            if (conditions.hasMoneyCost()) {
                VaultIntegration vault = plugin.getVaultIntegration();
                if (vault.isEnabled()) {
                    if (!vault.has(player, conditions.getMoneyCost())) {
                        event.setCancelled(true);
                        MessageUtil.sendError(player, lang.getMessage("conditions.not_enough_money",
                                Map.of("cost", vault.format(conditions.getMoneyCost()))));
                        return;
                    }
                }
            }

            final String finalRecipeKey = customRecipe.getKey();
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                
                if (conditions.hasCooldown()) {
                    tracker.setCooldown(player.getUniqueId(), finalRecipeKey);
                }

                if (conditions.hasAnyLimit()) {
                    tracker.incrementCraftCount(player.getUniqueId(), finalRecipeKey);
                }

                if (conditions.hasMoneyCost()) {
                    VaultIntegration vault = plugin.getVaultIntegration();
                    if (vault.isEnabled() && vault.withdraw(player, conditions.getMoneyCost())) {
                        MessageUtil.sendWarning(player, lang.getMessage("conditions.money_spent",
                                Map.of("amount", vault.format(conditions.getMoneyCost()))));
                    }
                }

                if (conditions.hasXpReward()) {
                    int xpReward = conditions.getXpReward();
                    if (xpReward > 0) {
                        player.giveExp(xpReward);
                        MessageUtil.sendSuccess(player, lang.getMessage("conditions.xp_gained",
                                Map.of("amount", String.valueOf(xpReward))));
                    } else if (xpReward < 0) {
                        int toTake = Math.abs(xpReward);
                        int currentTotal = getTotalExperience(player);
                        if (currentTotal >= toTake) {
                            setTotalExperience(player, currentTotal - toTake);
                            MessageUtil.sendWarning(player, lang.getMessage("conditions.xp_spent",
                                    Map.of("amount", String.valueOf(toTake))));
                        }
                    }
                }
            });
        }

        if (customRecipe.hasRandomResults()) {
            ItemStack storedResult = RecipePreviewListener.getStoredRandomResult(player.getUniqueId());
            if (storedResult != null) {
                
                inventory.setResult(storedResult.clone());
                
                RecipePreviewListener.clearStoredRandomResult(player.getUniqueId());
                if (plugin.isDebugMode()) {
                    plugin.getLogger().info("[CRAFT] Using stored preview random result for " + player.getName() + ": " +
                            storedResult.getType() + " x" + storedResult.getAmount());
                }
            } else {
                
                ItemStack randomResult = customRecipe.getRandomOrDefaultResult(
                        plugin.isUseCraftedCustomNames(),
                        plugin.isKeepSpawnEggNames()
                );
                if (randomResult != null) {
                    inventory.setResult(randomResult);
                    if (plugin.isDebugMode()) {
                        plugin.getLogger().info("[CRAFT] Applied new random result (no preview stored): " + randomResult.getType() +
                                " x" + randomResult.getAmount());
                    }
                }
            }

            if (customRecipe.getRandomResults().hasFailureChance() && customRecipe.getRandomResults().rollFailure()) {
                
                inventory.setResult(new ItemStack(Material.AIR));
                MessageUtil.sendError(player, plugin.getLanguageManager().getMessage("craft.failure_message"));
                if (plugin.isDebugMode()) {
                    plugin.getLogger().info("[CRAFT] Craft failed due to failure chance for " + player.getName());
                }
                return;
            }
        }

        plugin.getLogger().info("[DEBUG Craft] Recipe " + customRecipe.getKey() + " hasCraftEvents: " + customRecipe.hasCraftEvents());
        if (customRecipe.hasCraftEvents()) {
            CraftEvents events = customRecipe.getCraftEvents();
            final String craftKey = customRecipe.getKey();
            plugin.getLogger().info("[DEBUG Craft] CraftEvents object: " + events +
                " hasAnyEvent: " + events.hasAnyEvent());
            
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                plugin.getLogger().info("[DEBUG Craft] Executing craft events for: " + craftKey);
                events.execute(player);
            });
        }

        for (int i = 0; i < Math.min(matrix.length, ingredients.size()); i++) {
            RecipeIngredient required = ingredients.get(i);
            ItemStack actual = matrix[i];
            if (required.material() == Material.AIR) {
                continue;
            }

            if (actual != null && actual.getType() == required.material()) {
                if (required.amount() > 1) {
                    actual.setAmount(actual.getAmount() - (required.amount() - 1));
                }
            }
        }
    }

    private CustomRecipe findExactMatchRecipe(ItemStack[] matrix, Player player) {
        if (matrix == null || matrix.length != 9) {
            return null;
        }

        for (CustomRecipe recipe : plugin.getRecipeManager().getAllRecipes()) {
            if (recipe.getType() != RecipeType.SHAPED) {
                continue;
            }

            if (player != null && !plugin.getRecipeWorldManager().isRecipeAllowedForPlayer(recipe.getKey(), player)) {
                continue;
            }

            List<RecipeIngredient> ingredients = recipe.getRecipeData().ingredients();
            if (ingredients.size() != 9) {
                continue;
            }

            boolean hasExactIngredient = false;
            for (RecipeIngredient ingredient : ingredients) {
                if (ingredient.hasExactItem()) {
                    hasExactIngredient = true;
                    break;
                }
            }

            if (!hasExactIngredient) {
                continue;
            }

            boolean allIngredientsMatch = true;
            for (int i = 0; i < 9; i++) {
                RecipeIngredient required = ingredients.get(i);
                ItemStack actual = matrix[i];

                if (required.material() == Material.AIR) {
                    if (actual != null && actual.getType() != Material.AIR) {
                        allIngredientsMatch = false;
                        break;
                    }
                    continue;
                }

                if (actual == null || actual.getType() != required.material()) {
                    allIngredientsMatch = false;
                    break;
                }

                if (required.hasExactItem()) {
                    ItemStack exactItem = required.getExactItem();
                    
                    if (!org.hikarii.customrecipes.recipe.data.IngredientMatcher.matches(exactItem, actual, false)) {
                        allIngredientsMatch = false;
                        break;
                    }
                }
            }

            if (allIngredientsMatch) {
                if (plugin.isDebugMode()) {
                    plugin.getLogger().info("[CRAFT] Found recipe with exact-ingredients: " + recipe.getKey());
                }
                return recipe;
            }
        }

        return null;
    }

    private int getTotalExperience(Player player) {
        int level = player.getLevel();
        int totalXP = 0;

        if (level <= 16) {
            totalXP = (int) (level * level + 6 * level);
        } else if (level <= 31) {
            totalXP = (int) (2.5 * level * level - 40.5 * level + 360);
        } else {
            totalXP = (int) (4.5 * level * level - 162.5 * level + 2220);
        }

        totalXP += Math.round(player.getExpToLevel() * player.getExp());
        return totalXP;
    }

    private void setTotalExperience(Player player, int totalXP) {
        player.setExp(0);
        player.setLevel(0);
        player.setTotalExperience(0);

        if (totalXP > 0) {
            player.giveExp(totalXP);
        }
    }

    private void handleShapelessCraft(CraftItemEvent event, CustomRecipe customRecipe) {
        if (plugin.isDebugMode()) {
            plugin.getLogger().info("[CRAFT] Shapeless recipe: " + customRecipe.getKey());
        }

        CraftingInventory inventory = event.getInventory();
        ItemStack[] matrix = inventory.getMatrix();

        var shapelessData = customRecipe.getShapelessData();
        if (shapelessData.hasExactIngredients()) {
            if (!validateShapelessExactIngredients(matrix, shapelessData)) {
                if (plugin.isDebugMode()) {
                    plugin.getLogger().info("[CRAFT] CANCELLED: shapeless exact ingredients don't match");
                }
                event.setCancelled(true);
                return;
            }
        }

        if (plugin.isDebugMode()) {
            plugin.getLogger().info("[CRAFT] Shapeless checks passed!");
        }

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (!plugin.getRecipeWorldManager().isRecipeAllowedForPlayer(customRecipe.getKey(), player)) {
            event.setCancelled(true);
            return;
        }

        RecipeConditions conditions = customRecipe.getConditions();
        if (conditions != null && conditions.hasAnyCondition()) {
            LanguageManager lang = plugin.getLanguageManager();
            CraftTracker tracker = plugin.getCraftTracker();
            String recipeKey = customRecipe.getKey();

            if (conditions.hasPermission() && !player.hasPermission(conditions.getPermission())) {
                event.setCancelled(true);
                MessageUtil.sendError(player, lang.getMessage("conditions.no_permission",
                        Map.of("permission", conditions.getPermission())));
                return;
            }

            if (conditions.hasXpLevelRequirement() && player.getLevel() < conditions.getRequiredXpLevel()) {
                event.setCancelled(true);
                MessageUtil.sendError(player, lang.getMessage("conditions.not_enough_xp_level",
                        Map.of("required", String.valueOf(conditions.getRequiredXpLevel()),
                               "current", String.valueOf(player.getLevel()))));
                return;
            }

            if (conditions.hasCooldown()) {
                int remaining = tracker.getRemainingCooldown(player.getUniqueId(), recipeKey, conditions.getCooldownSeconds());
                if (remaining > 0) {
                    event.setCancelled(true);
                    MessageUtil.sendError(player, lang.getMessage("conditions.on_cooldown",
                            Map.of("time", CraftTracker.formatTime(remaining))));
                    return;
                }
            }

            if (conditions.hasDailyLimit()) {
                int crafted = tracker.getDailyCrafts(player.getUniqueId(), recipeKey);
                if (crafted >= conditions.getCraftLimitDaily()) {
                    event.setCancelled(true);
                    MessageUtil.sendError(player, lang.getMessage("conditions.daily_limit_reached",
                            Map.of("limit", String.valueOf(conditions.getCraftLimitDaily()))));
                    return;
                }
            }

            if (conditions.hasWeeklyLimit()) {
                int crafted = tracker.getWeeklyCrafts(player.getUniqueId(), recipeKey);
                if (crafted >= conditions.getCraftLimitWeekly()) {
                    event.setCancelled(true);
                    MessageUtil.sendError(player, lang.getMessage("conditions.weekly_limit_reached",
                            Map.of("limit", String.valueOf(conditions.getCraftLimitWeekly()))));
                    return;
                }
            }

            if (conditions.hasTotalLimit()) {
                int crafted = tracker.getTotalCrafts(player.getUniqueId(), recipeKey);
                if (crafted >= conditions.getCraftLimitTotal()) {
                    event.setCancelled(true);
                    MessageUtil.sendError(player, lang.getMessage("conditions.total_limit_reached",
                            Map.of("limit", String.valueOf(conditions.getCraftLimitTotal()))));
                    return;
                }
            }

            if (conditions.hasMoneyCost()) {
                VaultIntegration vault = plugin.getVaultIntegration();
                if (vault.isEnabled()) {
                    if (!vault.has(player, conditions.getMoneyCost())) {
                        event.setCancelled(true);
                        MessageUtil.sendError(player, lang.getMessage("conditions.not_enough_money",
                                Map.of("cost", vault.format(conditions.getMoneyCost()))));
                        return;
                    }
                }
            }

            final String finalRecipeKey = customRecipe.getKey();
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                
                if (conditions.hasCooldown()) {
                    tracker.setCooldown(player.getUniqueId(), finalRecipeKey);
                }

                if (conditions.hasAnyLimit()) {
                    tracker.incrementCraftCount(player.getUniqueId(), finalRecipeKey);
                }

                if (conditions.hasMoneyCost()) {
                    VaultIntegration vault = plugin.getVaultIntegration();
                    if (vault.isEnabled() && vault.withdraw(player, conditions.getMoneyCost())) {
                        MessageUtil.sendWarning(player, lang.getMessage("conditions.money_spent",
                                Map.of("amount", vault.format(conditions.getMoneyCost()))));
                    }
                }

                if (conditions.hasXpReward()) {
                    int xpReward = conditions.getXpReward();
                    if (xpReward > 0) {
                        player.giveExp(xpReward);
                        MessageUtil.sendSuccess(player, lang.getMessage("conditions.xp_gained",
                                Map.of("amount", String.valueOf(xpReward))));
                    } else if (xpReward < 0) {
                        int toTake = Math.abs(xpReward);
                        int currentTotal = getTotalExperience(player);
                        if (currentTotal >= toTake) {
                            setTotalExperience(player, currentTotal - toTake);
                            MessageUtil.sendWarning(player, lang.getMessage("conditions.xp_spent",
                                    Map.of("amount", String.valueOf(toTake))));
                        }
                    }
                }
            });
        }

        if (customRecipe.hasRandomResults()) {
            ItemStack storedResult = RecipePreviewListener.getStoredRandomResult(player.getUniqueId());
            if (storedResult != null) {
                
                inventory.setResult(storedResult.clone());
                
                RecipePreviewListener.clearStoredRandomResult(player.getUniqueId());
                if (plugin.isDebugMode()) {
                    plugin.getLogger().info("[CRAFT] Using stored preview random result (shapeless) for " + player.getName() + ": " +
                            storedResult.getType() + " x" + storedResult.getAmount());
                }
            } else {
                
                ItemStack randomResult = customRecipe.getRandomOrDefaultResult(
                        plugin.isUseCraftedCustomNames(),
                        plugin.isKeepSpawnEggNames()
                );
                if (randomResult != null) {
                    inventory.setResult(randomResult);
                    if (plugin.isDebugMode()) {
                        plugin.getLogger().info("[CRAFT] Applied new random result (shapeless, no preview stored): " + randomResult.getType() +
                                " x" + randomResult.getAmount());
                    }
                }
            }

            if (customRecipe.getRandomResults().hasFailureChance() && customRecipe.getRandomResults().rollFailure()) {
                
                inventory.setResult(new ItemStack(Material.AIR));
                MessageUtil.sendError(player, plugin.getLanguageManager().getMessage("craft.failure_message"));
                if (plugin.isDebugMode()) {
                    plugin.getLogger().info("[CRAFT] Shapeless craft failed due to failure chance for " + player.getName());
                }
                return;
            }
        }

        plugin.getLogger().info("[DEBUG Shapeless Craft] Recipe " + customRecipe.getKey() + " hasCraftEvents: " + customRecipe.hasCraftEvents());
        if (customRecipe.hasCraftEvents() && event.getWhoClicked() instanceof Player craftPlayer) {
            CraftEvents events = customRecipe.getCraftEvents();
            plugin.getLogger().info("[DEBUG Shapeless Craft] CraftEvents object: " + events +
                " hasAnyEvent: " + events.hasAnyEvent());
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                plugin.getLogger().info("[DEBUG Shapeless Craft] Executing craft events for: " + customRecipe.getKey());
                events.execute(craftPlayer);
            });
        }
    }

    private boolean validateShapelessExactIngredients(ItemStack[] matrix,
            org.hikarii.customrecipes.recipe.data.ShapelessRecipeData shapelessData) {
        List<ItemStack> exactIngredients = shapelessData.exactIngredients();
        if (exactIngredients == null || exactIngredients.isEmpty()) {
            return true;
        }

        List<ItemStack> matrixItems = new ArrayList<>();
        for (ItemStack item : matrix) {
            if (item != null && item.getType() != Material.AIR) {
                matrixItems.add(item);
            }
        }

        List<Boolean> usedMatrixSlots = new ArrayList<>();
        for (int i = 0; i < matrixItems.size(); i++) {
            usedMatrixSlots.add(false);
        }

        for (int i = 0; i < exactIngredients.size(); i++) {
            ItemStack exactItem = exactIngredients.get(i);
            if (exactItem == null) {
                continue; 
            }

            boolean found = false;
            for (int j = 0; j < matrixItems.size(); j++) {
                if (usedMatrixSlots.get(j)) {
                    continue; 
                }

                ItemStack matrixItem = matrixItems.get(j);
                if (matrixItem.getType() != exactItem.getType()) {
                    continue;
                }

                if (org.hikarii.customrecipes.recipe.data.IngredientMatcher.matches(exactItem, matrixItem, false)) {
                    usedMatrixSlots.set(j, true);
                    found = true;
                    if (plugin.isDebugMode()) {
                        plugin.getLogger().info("[CRAFT] Shapeless exact ingredient matched at index " + j);
                    }
                    break;
                }
            }

            if (!found) {
                if (plugin.isDebugMode()) {
                    plugin.getLogger().info("[CRAFT] Shapeless exact ingredient NOT found: " + exactItem.getType());
                }
                return false;
            }
        }

        return true;
    }
}