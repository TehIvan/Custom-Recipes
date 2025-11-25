package org.hikarii.customrecipes.recipe.data;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.hikarii.customrecipes.config.ValidationException;
import java.util.*;

public record ShapelessRecipeData(
        Map<Material, Integer> ingredients,
        List<ItemStack> exactIngredients
) {
    public ShapelessRecipeData {
        if (ingredients == null || ingredients.isEmpty()) {
            throw new IllegalArgumentException("Ingredients cannot be null or empty");
        }

        int totalItems = ingredients.values().stream().mapToInt(Integer::intValue).sum();
        if (totalItems > 9) {
            throw new IllegalArgumentException("Too many ingredients (max 9 items)");
        }
        ingredients = Collections.unmodifiableMap(new HashMap<>(ingredients));
        
        exactIngredients = exactIngredients != null ? Collections.unmodifiableList(new ArrayList<>(exactIngredients)) : null;
    }

    public ShapelessRecipeData(Map<Material, Integer> ingredients) {
        this(ingredients, null);
    }

    public boolean hasExactIngredients() {
        return exactIngredients != null && exactIngredients.stream().anyMatch(Objects::nonNull);
    }

    public ItemStack getExactIngredient(int index) {
        if (exactIngredients == null || index < 0 || index >= exactIngredients.size()) {
            return null;
        }
        return exactIngredients.get(index);
    }

    public static ShapelessRecipeData fromConfigList(List<String> ingredientList) throws ValidationException {
        return fromConfigList(ingredientList, null);
    }

    public static ShapelessRecipeData fromConfigList(List<String> ingredientList, List<ItemStack> exactItems) throws ValidationException {
        Map<Material, Integer> ingredients = new LinkedHashMap<>(); 
        for (String entry : ingredientList) {
            String[] parts = entry.split(":");
            if (parts.length != 2) {
                throw new ValidationException("Invalid ingredient format: " + entry + " (expected MATERIAL:COUNT)");
            }

            Material material = Material.getMaterial(parts[0].toUpperCase());
            if (material == null) {
                throw new ValidationException("Invalid material: " + parts[0]);
            }

            int count;
            try {
                count = Integer.parseInt(parts[1]);
            } catch (NumberFormatException e) {
                throw new ValidationException("Invalid count: " + parts[1]);
            }

            if (count < 1 || count > 9) {
                throw new ValidationException("Count must be 1-9: " + count);
            }
            ingredients.put(material, count);
        }
        return new ShapelessRecipeData(ingredients, exactItems);
    }

    public static ShapelessRecipeData fromGridItems(ItemStack[] gridItems) {
        return fromGridItems(gridItems, false);
    }

    public static ShapelessRecipeData fromGridItems(ItemStack[] gridItems, boolean preserveExactItems) {
        Map<Material, Integer> counts = new LinkedHashMap<>();
        List<ItemStack> exactItems = preserveExactItems ? new ArrayList<>() : null;

        for (ItemStack item : gridItems) {
            if (item != null && item.getType() != Material.AIR) {
                Material mat = item.getType();
                counts.put(mat, counts.getOrDefault(mat, 0) + 1);

                if (preserveExactItems) {
                    
                    if (hasCustomProperties(item)) {
                        exactItems.add(item.clone());
                    } else {
                        exactItems.add(null);
                    }
                }
            }
        }
        return new ShapelessRecipeData(counts, exactItems);
    }

    private static boolean hasCustomProperties(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        var meta = item.getItemMeta();

        if (meta.hasEnchants()) {
            return true;
        }

        if (item.getType() == Material.ENCHANTED_BOOK &&
            meta instanceof org.bukkit.inventory.meta.EnchantmentStorageMeta bookMeta) {
            if (bookMeta.hasStoredEnchants()) {
                return true;
            }
        }

        if (!meta.getPersistentDataContainer().getKeys().isEmpty()) {
            return true;
        }

        if (meta instanceof org.bukkit.inventory.meta.Damageable damageable) {
            if (damageable.hasDamage() && damageable.getDamage() > 0) {
                return true;
            }
        }

        if (meta.hasDisplayName()) {
            return true;
        }

        if (meta.hasLore()) {
            return true;
        }

        if (meta.hasCustomModelData()) {
            return true;
        }

        if (!meta.getItemFlags().isEmpty()) {
            return true;
        }

        if (meta.isUnbreakable()) {
            return true;
        }

        return false;
    }

    public List<String> toConfigList() {
        List<String> list = new ArrayList<>();
        for (Map.Entry<Material, Integer> entry : ingredients.entrySet()) {
            list.add(entry.getKey().name() + ":" + entry.getValue());
        }
        return list;
    }

    public int getTotalItemCount() {
        return ingredients.values().stream().mapToInt(Integer::intValue).sum();
    }

    public List<Material> getExpandedMaterials() {
        List<Material> expanded = new ArrayList<>();
        for (Map.Entry<Material, Integer> entry : ingredients.entrySet()) {
            for (int i = 0; i < entry.getValue(); i++) {
                expanded.add(entry.getKey());
            }
        }
        return expanded;
    }
}
