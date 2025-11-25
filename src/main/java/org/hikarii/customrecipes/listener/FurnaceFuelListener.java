package org.hikarii.customrecipes.listener;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Furnace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.FurnaceBurnEvent;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.bukkit.event.inventory.FurnaceStartSmeltEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.FurnaceInventory;
import org.bukkit.inventory.ItemStack;
import org.hikarii.customrecipes.CustomRecipes;
import org.hikarii.customrecipes.integration.VaultIntegration;
import org.hikarii.customrecipes.language.LanguageManager;
import org.hikarii.customrecipes.recipe.CraftEvents;
import org.hikarii.customrecipes.recipe.CraftTracker;
import org.hikarii.customrecipes.recipe.CustomRecipe;
import org.hikarii.customrecipes.recipe.RecipeConditions;
import org.hikarii.customrecipes.recipe.RecipeType;
import org.hikarii.customrecipes.recipe.data.FurnaceRecipeData;
import org.hikarii.customrecipes.recipe.data.IngredientMatcher;
import org.hikarii.customrecipes.recipe.vanilla.IngredientChoice;
import org.hikarii.customrecipes.recipe.vanilla.VanillaRecipeInfo;
import org.hikarii.customrecipes.recipe.vanilla.VanillaRecipeManager;
import org.hikarii.customrecipes.util.MessageUtil;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class FurnaceFuelListener implements Listener {
    private final CustomRecipes plugin;

    private final Map<Location, UUID> furnaceUsers = new ConcurrentHashMap<>();

    private final Map<Location, String> lastErrorSent = new ConcurrentHashMap<>();

    private final Map<Location, String> lastDebugSent = new ConcurrentHashMap<>();

    private final Map<Location, String> validatedCustomFuel = new ConcurrentHashMap<>();

    public FurnaceFuelListener(CustomRecipes plugin) {
        this.plugin = plugin;
    }

    private boolean shouldSendError(Location location, String errorKey, Material inputMaterial) {
        String newKey = errorKey + ":" + (inputMaterial != null ? inputMaterial.name() : "null");
        
        final boolean[] shouldSend = {false};
        lastErrorSent.compute(location, (loc, oldKey) -> {
            if (oldKey == null || !oldKey.equals(newKey)) {
                shouldSend[0] = true;
                return newKey;
            }
            return oldKey; 
        });
        return shouldSend[0];
    }

    private boolean shouldDebug(Location location, String debugKey) {
        final boolean[] shouldSend = {false};
        lastDebugSent.compute(location, (loc, oldKey) -> {
            if (oldKey == null || !oldKey.equals(debugKey)) {
                shouldSend[0] = true;
                return debugKey;
            }
            return oldKey;
        });
        return shouldSend[0];
    }

    private void clearErrorCache(Location location) {
        lastErrorSent.remove(location);
        lastDebugSent.remove(location);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (event.isCancelled()) return;
        if (!(event.getPlayer() instanceof Player player)) return;

        InventoryType type = event.getInventory().getType();
        if (type != InventoryType.FURNACE && type != InventoryType.BLAST_FURNACE && type != InventoryType.SMOKER) {
            return;
        }

        if (event.getInventory().getLocation() != null) {
            Location loc = event.getInventory().getLocation();
            furnaceUsers.put(loc, player.getUniqueId());
            
            clearErrorCache(loc);
            plugin.debug("[FurnaceFuel] Tracking player " + player.getName() + " for furnace at " + loc);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onFurnaceBurn(FurnaceBurnEvent event) {
        if (event.isCancelled()) return;

        ItemStack fuel = event.getFuel();
        if (fuel == null || fuel.getType() == Material.AIR) return;

        RecipeType recipeType = switch (event.getBlock().getType()) {
            case FURNACE -> RecipeType.FURNACE;
            case BLAST_FURNACE -> RecipeType.BLAST_FURNACE;
            case SMOKER -> RecipeType.SMOKER;
            default -> null;
        };
        if (recipeType == null) return;

        Furnace furnace = (Furnace) event.getBlock().getState();
        FurnaceInventory inventory = furnace.getInventory();
        ItemStack input = inventory.getSmelting();

        if (input == null || input.getType() == Material.AIR) {
            
            if (event.getBurnTime() <= 0) {
                FurnaceRecipeData.CustomFuel customFuel = findCustomFuel(fuel, recipeType);
                if (customFuel != null) {
                    event.setBurnTime(customFuel.getBurnTime());
                    event.setCancelled(false);
                    plugin.debug("[FurnaceFuel] Custom fuel accepted (no input): " + fuel.getType());
                }
            }
            return;
        }

        if (isVanillaFurnaceRecipeDisabled(input.getType(), recipeType)) {
            event.setCancelled(true);
            plugin.debug("[FurnaceFuel] Blocked burn - vanilla recipe variant disabled for input: " + input.getType());
            return;
        }

        CustomRecipe recipe = findMatchingFurnaceRecipe(input, recipeType);

        boolean isCustomFuel = event.getBurnTime() <= 0;
        FurnaceRecipeData.CustomFuel customFuel = null;

        if (isCustomFuel) {
            customFuel = findCustomFuel(fuel, recipeType);
            if (customFuel == null) {
                
                return;
            }
        }

        Location furnaceLocation = event.getBlock().getLocation();

        if (recipe != null) {
            FurnaceRecipeData furnaceData = recipe.getFurnaceData();

            if (furnaceData != null && furnaceData.hasCustomFuels()) {
                boolean fuelMatches = false;
                for (FurnaceRecipeData.CustomFuel cf : furnaceData.getCustomFuels()) {
                    if (matchesCustomFuel(fuel, cf)) {
                        fuelMatches = true;
                        break;
                    }
                }

                if (!fuelMatches) {
                    
                    if (shouldDebug(furnaceLocation, "wrong_fuel:" + fuel.getType())) {
                        plugin.debug("[FurnaceFuel] Custom recipe requires specific fuel, wrong fuel used - CANCELLING");
                    }
                    event.setCancelled(true);
                    return;
                }
            }

            UUID playerUUID = furnaceUsers.get(furnaceLocation);
            Player player = playerUUID != null ? Bukkit.getPlayer(playerUUID) : null;

            if (player != null && !plugin.getRecipeWorldManager().isRecipeAllowedForPlayer(recipe.getKey(), player)) {
                event.setCancelled(true);
                return;
            }

            RecipeConditions conditions = recipe.getConditions();
            if (conditions != null && conditions.hasAnyCondition() && player != null) {
                LanguageManager lang = plugin.getLanguageManager();
                CraftTracker tracker = plugin.getCraftTracker();
                String recipeKey = recipe.getKey();
                Material inputMaterial = input.getType();

                if (conditions.hasPermission() && !player.hasPermission(conditions.getPermission())) {
                    event.setCancelled(true);
                    if (shouldSendError(furnaceLocation, "no_permission", inputMaterial)) {
                        MessageUtil.sendError(player, lang.getMessage("conditions.no_permission",
                                Map.of("permission", conditions.getPermission())));
                        plugin.debug("[FurnaceFuel] Burn cancelled - player lacks permission");
                    }
                    return;
                }

                if (conditions.hasXpLevelRequirement() && player.getLevel() < conditions.getRequiredXpLevel()) {
                    event.setCancelled(true);
                    if (shouldSendError(furnaceLocation, "xp_level", inputMaterial)) {
                        MessageUtil.sendError(player, lang.getMessage("conditions.not_enough_xp_level",
                                Map.of("required", String.valueOf(conditions.getRequiredXpLevel()),
                                       "current", String.valueOf(player.getLevel()))));
                        plugin.debug("[FurnaceFuel] Burn cancelled - not enough XP level");
                    }
                    return;
                }

                if (conditions.hasCooldown()) {
                    int remaining = tracker.getRemainingCooldown(player.getUniqueId(), recipeKey, conditions.getCooldownSeconds());
                    if (remaining > 0) {
                        event.setCancelled(true);
                        if (shouldSendError(furnaceLocation, "cooldown", inputMaterial)) {
                            MessageUtil.sendError(player, lang.getMessage("conditions.on_cooldown",
                                    Map.of("time", CraftTracker.formatTime(remaining))));
                            plugin.debug("[FurnaceFuel] Burn cancelled - on cooldown");
                        }
                        return;
                    }
                }

                if (conditions.hasDailyLimit()) {
                    int crafted = tracker.getDailyCrafts(player.getUniqueId(), recipeKey);
                    if (crafted >= conditions.getCraftLimitDaily()) {
                        event.setCancelled(true);
                        if (shouldSendError(furnaceLocation, "daily_limit", inputMaterial)) {
                            MessageUtil.sendError(player, lang.getMessage("conditions.daily_limit_reached",
                                    Map.of("limit", String.valueOf(conditions.getCraftLimitDaily()))));
                            plugin.debug("[FurnaceFuel] Burn cancelled - daily limit reached");
                        }
                        return;
                    }
                }

                if (conditions.hasWeeklyLimit()) {
                    int crafted = tracker.getWeeklyCrafts(player.getUniqueId(), recipeKey);
                    if (crafted >= conditions.getCraftLimitWeekly()) {
                        event.setCancelled(true);
                        if (shouldSendError(furnaceLocation, "weekly_limit", inputMaterial)) {
                            MessageUtil.sendError(player, lang.getMessage("conditions.weekly_limit_reached",
                                    Map.of("limit", String.valueOf(conditions.getCraftLimitWeekly()))));
                            plugin.debug("[FurnaceFuel] Burn cancelled - weekly limit reached");
                        }
                        return;
                    }
                }

                if (conditions.hasTotalLimit()) {
                    int crafted = tracker.getTotalCrafts(player.getUniqueId(), recipeKey);
                    if (crafted >= conditions.getCraftLimitTotal()) {
                        event.setCancelled(true);
                        if (shouldSendError(furnaceLocation, "total_limit", inputMaterial)) {
                            MessageUtil.sendError(player, lang.getMessage("conditions.total_limit_reached",
                                    Map.of("limit", String.valueOf(conditions.getCraftLimitTotal()))));
                            plugin.debug("[FurnaceFuel] Burn cancelled - total limit reached");
                        }
                        return;
                    }
                }

                if (conditions.hasMoneyCost()) {
                    VaultIntegration vault = plugin.getVaultIntegration();
                    if (vault.isEnabled() && !vault.has(player, conditions.getMoneyCost())) {
                        event.setCancelled(true);
                        if (shouldSendError(furnaceLocation, "money", inputMaterial)) {
                            MessageUtil.sendError(player, lang.getMessage("conditions.not_enough_money",
                                    Map.of("cost", vault.format(conditions.getMoneyCost()))));
                            plugin.debug("[FurnaceFuel] Burn cancelled - not enough money");
                        }
                        return;
                    }
                }

                clearErrorCache(furnaceLocation);
                
                plugin.debug("[FurnaceFuel] All conditions passed for recipe: " + recipe.getKey());

                if (conditions.hasCooldown()) {
                    tracker.setCooldown(player.getUniqueId(), recipeKey);
                    plugin.debug("[FurnaceFuel] Cooldown set for: " + recipeKey);
                }

                if (conditions.hasAnyLimit()) {
                    tracker.incrementCraftCount(player.getUniqueId(), recipeKey);
                    plugin.debug("[FurnaceFuel] Craft count incremented for: " + recipeKey);
                }

                if (conditions.hasMoneyCost()) {
                    VaultIntegration vault = plugin.getVaultIntegration();
                    if (vault.isEnabled() && vault.withdraw(player, conditions.getMoneyCost())) {
                        MessageUtil.sendWarning(player, lang.getMessage("conditions.money_spent",
                                Map.of("amount", vault.format(conditions.getMoneyCost()))));
                        plugin.debug("[FurnaceFuel] Money withdrawn: " + conditions.getMoneyCost());
                    }
                }
            }
        }

        if (isCustomFuel && customFuel != null) {
            event.setBurnTime(customFuel.getBurnTime());
            event.setCancelled(false);
            
            if (recipe != null) {
                validatedCustomFuel.put(furnaceLocation, recipe.getKey());
                plugin.debug("[FurnaceFuel] Tracking validated custom fuel for recipe: " + recipe.getKey());
            }
            plugin.debug("[FurnaceFuel] Custom fuel accepted in " + recipeType + ": " + fuel.getType() +
                    ", burn time: " + customFuel.getBurnTime());
        }

        if (!isCustomFuel && recipe != null) {
            FurnaceRecipeData furnaceData = recipe.getFurnaceData();
            if (furnaceData != null && furnaceData.hasCustomFuels()) {
                
                for (FurnaceRecipeData.CustomFuel cf : furnaceData.getCustomFuels()) {
                    if (matchesCustomFuel(fuel, cf)) {
                        validatedCustomFuel.put(furnaceLocation, recipe.getKey());
                        plugin.debug("[FurnaceFuel] Tracking validated vanilla-compatible custom fuel for recipe: " + recipe.getKey());
                        break;
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onFurnaceStartSmelt(FurnaceStartSmeltEvent event) {
        try {
            ItemStack source = event.getSource();
            if (source == null || source.getType() == Material.AIR) return;

            RecipeType recipeType = switch (event.getBlock().getType()) {
                case FURNACE -> RecipeType.FURNACE;
                case BLAST_FURNACE -> RecipeType.BLAST_FURNACE;
                case SMOKER -> RecipeType.SMOKER;
                default -> null;
            };
            if (recipeType == null) return;

            if (isVanillaFurnaceRecipeDisabled(source.getType(), recipeType)) {
                
                event.setTotalCookTime(Integer.MAX_VALUE);
                plugin.debug("[FurnaceFuel] Blocked start smelt - vanilla recipe variant disabled for input: " + source.getType());
            }
        } catch (Exception e) {
            
            plugin.debug("[FurnaceFuel] FurnaceStartSmeltEvent handling failed (version compatibility): " + e.getMessage());
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onFurnaceSmelt(FurnaceSmeltEvent event) {
        if (event.isCancelled()) return;

        RecipeType recipeType = switch (event.getBlock().getType()) {
            case FURNACE -> RecipeType.FURNACE;
            case BLAST_FURNACE -> RecipeType.BLAST_FURNACE;
            case SMOKER -> RecipeType.SMOKER;
            default -> null;
        };
        if (recipeType == null) return;

        Furnace furnace = (Furnace) event.getBlock().getState();
        FurnaceInventory inventory = furnace.getInventory();
        ItemStack input = inventory.getSmelting();
        ItemStack fuel = inventory.getFuel();

        if (input == null || input.getType() == Material.AIR) return;

        plugin.debug("[FurnaceFuel] FurnaceSmeltEvent fired for input: " + input.getType());

        CustomRecipe recipe = findMatchingFurnaceRecipe(input, recipeType);
        if (recipe == null) {
            plugin.debug("[FurnaceFuel] No custom recipe found for input: " + input.getType());
            return;
        }

        plugin.debug("[FurnaceFuel] Found recipe: " + recipe.getKey());

        FurnaceRecipeData furnaceData = recipe.getFurnaceData();
        if (furnaceData == null) return;

        Location furnaceLocation = event.getBlock().getLocation();

        if (furnaceData.hasCustomFuels()) {
            String validatedRecipeKey = validatedCustomFuel.get(furnaceLocation);

            if (validatedRecipeKey != null && validatedRecipeKey.equals(recipe.getKey())) {
                
                plugin.debug("[FurnaceFuel] Custom fuel was validated for recipe: " + recipe.getKey());
                
                validatedCustomFuel.remove(furnaceLocation);
            } else {
                
                boolean fuelMatches = false;
                for (FurnaceRecipeData.CustomFuel customFuel : furnaceData.getCustomFuels()) {
                    if (matchesCustomFuel(fuel, customFuel)) {
                        fuelMatches = true;
                        break;
                    }
                }

                if (!fuelMatches) {
                    
                    plugin.debug("[FurnaceFuel] Custom recipe requires specific fuel, wrong fuel used - CANCELLING smelt");
                    event.setCancelled(true);
                    return;
                }
            }
        }

        UUID playerUUID = furnaceUsers.get(furnaceLocation);
        Player player = playerUUID != null ? Bukkit.getPlayer(playerUUID) : null;

        RecipeConditions conditions = recipe.getConditions();
        if (conditions != null && conditions.hasXpReward() && player != null) {
            LanguageManager lang = plugin.getLanguageManager();
            int xpReward = conditions.getXpReward();

            if (xpReward > 0) {
                player.giveExp(xpReward);
                MessageUtil.sendSuccess(player, lang.getMessage("conditions.xp_gained",
                        Map.of("amount", String.valueOf(xpReward))));
                plugin.debug("[FurnaceFuel] XP reward given: " + xpReward);
            } else if (xpReward < 0) {
                int toTake = Math.abs(xpReward);
                int currentTotal = getTotalExperience(player);
                if (currentTotal >= toTake) {
                    setTotalExperience(player, currentTotal - toTake);
                    MessageUtil.sendWarning(player, lang.getMessage("conditions.xp_spent",
                            Map.of("amount", String.valueOf(toTake))));
                    plugin.debug("[FurnaceFuel] XP cost applied: " + toTake);
                }
            }
        }

        plugin.getLogger().info("[DEBUG Furnace] Recipe " + recipe.getKey() + " hasCraftEvents: " + recipe.hasCraftEvents() + ", player: " + (player != null ? player.getName() : "null"));
        if (recipe.hasCraftEvents() && player != null) {
            CraftEvents events = recipe.getCraftEvents();
            final Player finalPlayer = player;
            plugin.getLogger().info("[DEBUG Furnace] Executing craft events for: " + recipe.getKey());
            plugin.getServer().getScheduler().runTask(plugin, () -> events.execute(finalPlayer));
        }

        ItemStack result = recipe.getResultItem();

        if (recipe.hasRandomResults()) {
            ItemStack randomResult = recipe.getRandomResults().selectRandomResult();
            if (randomResult != null) {
                result = randomResult.clone();
                plugin.debug("[FurnaceFuel] Applied random result: " + result.getType() + " x" + result.getAmount());
            }

            if (recipe.getRandomResults().hasFailureChance() && recipe.getRandomResults().rollFailure()) {
                
                event.setResult(new ItemStack(Material.AIR));
                if (player != null) {
                    MessageUtil.sendError(player, plugin.getLanguageManager().getMessage("craft.smelting_failure_message"));
                }
                plugin.debug("[FurnaceFuel] Smelting failed due to failure chance");
                return;
            }
        }

        if (result != null && result.getType() != Material.AIR) {
            event.setResult(result);
            plugin.debug("[FurnaceFuel] Set smelt result: " + result.getType() + " x" + result.getAmount());
        }
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

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.isCancelled()) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;

        InventoryType invType = event.getInventory().getType();
        RecipeType recipeType;
        if (invType == InventoryType.FURNACE) {
            recipeType = RecipeType.FURNACE;
        } else if (invType == InventoryType.BLAST_FURNACE) {
            recipeType = RecipeType.BLAST_FURNACE;
        } else if (invType == InventoryType.SMOKER) {
            recipeType = RecipeType.SMOKER;
        } else {
            return;
        }

        if (event.getInventory().getLocation() != null) {
            Location loc = event.getInventory().getLocation();
            furnaceUsers.put(loc, player.getUniqueId());

            if (event.getRawSlot() == 0 || event.getRawSlot() == 1) {
                clearErrorCache(loc);
            }

            if (event.getRawSlot() == 0) {
                validatedCustomFuel.remove(loc);
            }
        }

        if (event.getRawSlot() != 1) return;

        ItemStack cursor = event.getCursor();
        if (cursor == null || cursor.getType() == Material.AIR) return;

        if (isVanillaFuel(cursor.getType())) return;

        FurnaceRecipeData.CustomFuel customFuel = findCustomFuel(cursor, recipeType);
        if (customFuel != null) {
            
            event.setCancelled(true);

            ItemStack currentFuel = event.getInventory().getItem(1);

            if (event.getClick() == ClickType.LEFT) {
                
                if (currentFuel == null || currentFuel.getType() == Material.AIR) {
                    
                    event.getInventory().setItem(1, cursor.clone());
                    event.getView().setCursor(null);
                } else if (currentFuel.isSimilar(cursor)) {
                    
                    int maxStack = currentFuel.getMaxStackSize();
                    int total = currentFuel.getAmount() + cursor.getAmount();
                    if (total <= maxStack) {
                        currentFuel.setAmount(total);
                        event.getView().setCursor(null);
                    } else {
                        currentFuel.setAmount(maxStack);
                        cursor.setAmount(total - maxStack);
                    }
                } else {
                    
                    event.getInventory().setItem(1, cursor.clone());
                    event.getView().setCursor(currentFuel);
                }
            } else if (event.getClick() == ClickType.RIGHT) {
                
                if (currentFuel == null || currentFuel.getType() == Material.AIR) {
                    ItemStack oneItem = cursor.clone();
                    oneItem.setAmount(1);
                    event.getInventory().setItem(1, oneItem);
                    if (cursor.getAmount() > 1) {
                        cursor.setAmount(cursor.getAmount() - 1);
                    } else {
                        event.getView().setCursor(null);
                    }
                } else if (currentFuel.isSimilar(cursor)) {
                    int maxStack = currentFuel.getMaxStackSize();
                    if (currentFuel.getAmount() < maxStack) {
                        currentFuel.setAmount(currentFuel.getAmount() + 1);
                        if (cursor.getAmount() > 1) {
                            cursor.setAmount(cursor.getAmount() - 1);
                        } else {
                            event.getView().setCursor(null);
                        }
                    }
                }
            }

            player.updateInventory();
            plugin.debug("[FurnaceFuel] Custom fuel placed: " + cursor.getType());
        }
    }

    private FurnaceRecipeData.CustomFuel findCustomFuel(ItemStack item, RecipeType targetType) {
        if (item == null || item.getType() == Material.AIR) return null;

        for (CustomRecipe recipe : plugin.getRecipeManager().getAllRecipes()) {
            
            if (recipe.getType() != targetType) {
                continue;
            }

            if (!plugin.getRecipeManager().isRecipeEnabled(recipe.getKey())) {
                continue;
            }

            FurnaceRecipeData furnaceData = recipe.getFurnaceData();
            if (furnaceData == null || !furnaceData.hasCustomFuels()) continue;

            for (FurnaceRecipeData.CustomFuel customFuel : furnaceData.getCustomFuels()) {
                if (matchesCustomFuel(item, customFuel)) {
                    return customFuel;
                }
            }
        }
        return null;
    }

    private FurnaceRecipeData.CustomFuel findCustomFuel(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return null;

        for (CustomRecipe recipe : plugin.getRecipeManager().getAllRecipes()) {
            if (recipe.getType() != RecipeType.FURNACE &&
                recipe.getType() != RecipeType.BLAST_FURNACE &&
                recipe.getType() != RecipeType.SMOKER) {
                continue;
            }

            FurnaceRecipeData furnaceData = recipe.getFurnaceData();
            if (furnaceData == null || !furnaceData.hasCustomFuels()) continue;

            for (FurnaceRecipeData.CustomFuel customFuel : furnaceData.getCustomFuels()) {
                if (matchesCustomFuel(item, customFuel)) {
                    return customFuel;
                }
            }
        }
        return null;
    }

    private boolean matchesCustomFuel(ItemStack item, FurnaceRecipeData.CustomFuel customFuel) {
        if (item == null || customFuel == null) return false;

        if (customFuel.getExactItem() != null) {
            return IngredientMatcher.matches(customFuel.getExactItem(), item, false);
        }

        return customFuel.getIngredient() != null &&
               customFuel.getIngredient().material() == item.getType();
    }

    private CustomRecipe findMatchingFurnaceRecipe(ItemStack input, RecipeType targetType) {
        if (input == null || input.getType() == Material.AIR) return null;

        for (CustomRecipe recipe : plugin.getRecipeManager().getAllRecipes()) {
            
            if (recipe.getType() != targetType) {
                continue;
            }

            if (!plugin.getRecipeManager().isRecipeEnabled(recipe.getKey())) {
                continue;
            }

            FurnaceRecipeData furnaceData = recipe.getFurnaceData();
            if (furnaceData == null) continue;

            if (furnaceData.getExactInput() != null) {
                if (IngredientMatcher.matches(furnaceData.getExactInput(), input, false)) {
                    return recipe;
                }
            } else if (furnaceData.getInput() != null) {
                if (furnaceData.getInput().material() == input.getType()) {
                    return recipe;
                }
            }
        }
        return null;
    }

    private boolean isVanillaFuel(Material material) {
        
        return material.isFuel();
    }

    private boolean isVanillaFurnaceRecipeDisabled(Material inputMaterial, RecipeType recipeType) {
        VanillaRecipeManager manager = plugin.getVanillaRecipeManager();
        if (manager == null) return false;

        for (VanillaRecipeInfo info : manager.getAllVanillaRecipes().values()) {
            
            if (info.getType() != recipeType) {
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
                return true;
            }

            if (matchingVariantIndex >= 0 && manager.isVariantDisabled(recipeKey, matchingVariantIndex)) {
                return true;
            }
        }

        return false;
    }
}
