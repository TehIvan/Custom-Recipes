package org.hikarii.customrecipes.recipe.data;

import org.bukkit.inventory.ItemStack;

public class RandomResult {
    private final ItemStack item;
    private final int weight;
    private final String name;

    public RandomResult(ItemStack item, int weight, String name) {
        this.item = item;
        this.weight = Math.max(1, weight);
        this.name = name;
    }

    public RandomResult(ItemStack item, int weight) {
        this(item, weight, null);
    }

    public ItemStack getItem() {
        return item.clone();
    }

    public int getWeight() {
        return weight;
    }

    public String getName() {
        return name;
    }

    public boolean hasCustomName() {
        return name != null && !name.isEmpty();
    }
}
