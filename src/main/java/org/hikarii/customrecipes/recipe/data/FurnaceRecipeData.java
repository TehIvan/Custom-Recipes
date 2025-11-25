package org.hikarii.customrecipes.recipe.data;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.hikarii.customrecipes.recipe.RecipeType;

import java.util.ArrayList;
import java.util.List;

public class FurnaceRecipeData {
    private final RecipeIngredient input;
    private final ItemStack exactInput;
    private final int cookingTime;
    private final float experience;
    private final String group;
    private final List<CustomFuel> customFuels;

    public static final int DEFAULT_FURNACE_COOKING_TIME = 200;
    public static final int DEFAULT_BLAST_FURNACE_COOKING_TIME = 100;
    public static final int DEFAULT_SMOKER_COOKING_TIME = 100;
    public static final int DEFAULT_CAMPFIRE_COOKING_TIME = 600;
    public static final float DEFAULT_EXPERIENCE = 0.0f;

    public FurnaceRecipeData(RecipeIngredient input, ItemStack exactInput, int cookingTime, float experience, String group, List<CustomFuel> customFuels) {
        this.input = input;
        this.exactInput = exactInput;
        this.cookingTime = cookingTime;
        this.experience = experience;
        this.group = group != null ? group : "";
        this.customFuels = customFuels != null ? customFuels : new ArrayList<>();
    }

    public RecipeIngredient getInput() {
        return input;
    }

    public ItemStack getExactInput() {
        return exactInput;
    }

    public boolean hasExactInput() {
        return exactInput != null && exactInput.getType() != Material.AIR;
    }

    public int getCookingTime() {
        return cookingTime;
    }

    public float getExperience() {
        return experience;
    }

    public String getGroup() {
        return group;
    }

    public List<CustomFuel> getCustomFuels() {
        return customFuels;
    }

    public boolean hasCustomFuels() {
        return customFuels != null && !customFuels.isEmpty();
    }

    public static int getDefaultCookingTime(RecipeType type) {
        return switch (type) {
            case FURNACE -> DEFAULT_FURNACE_COOKING_TIME;
            case BLAST_FURNACE -> DEFAULT_BLAST_FURNACE_COOKING_TIME;
            case SMOKER -> DEFAULT_SMOKER_COOKING_TIME;
            case CAMPFIRE -> DEFAULT_CAMPFIRE_COOKING_TIME;
            default -> DEFAULT_FURNACE_COOKING_TIME;
        };
    }

    public static class CustomFuel {
        private final RecipeIngredient ingredient;
        private final ItemStack exactItem;
        private final int burnTime;

        public CustomFuel(RecipeIngredient ingredient, ItemStack exactItem, int burnTime) {
            this.ingredient = ingredient;
            this.exactItem = exactItem;
            this.burnTime = burnTime;
        }

        public RecipeIngredient getIngredient() {
            return ingredient;
        }

        public ItemStack getExactItem() {
            return exactItem;
        }

        public boolean hasExactItem() {
            return exactItem != null && exactItem.getType() != Material.AIR;
        }

        public int getBurnTime() {
            return burnTime;
        }

        public boolean matches(ItemStack item) {
            if (item == null || item.getType() == Material.AIR) {
                return false;
            }

            if (hasExactItem()) {
                
                return IngredientMatcher.matches(exactItem, item, false);
            }

            return ingredient != null && ingredient.material() == item.getType();
        }
    }

    public static class Builder {
        private RecipeIngredient input;
        private ItemStack exactInput;
        private int cookingTime = DEFAULT_FURNACE_COOKING_TIME;
        private float experience = DEFAULT_EXPERIENCE;
        private String group = "";
        private List<CustomFuel> customFuels = new ArrayList<>();

        public Builder input(RecipeIngredient input) {
            this.input = input;
            return this;
        }

        public Builder input(Material material) {
            this.input = new RecipeIngredient(material);
            return this;
        }

        public Builder exactInput(ItemStack exactInput) {
            this.exactInput = exactInput;
            if (exactInput != null && exactInput.getType() != Material.AIR) {
                this.input = new RecipeIngredient(exactInput.getType());
            }
            return this;
        }

        public Builder cookingTime(int cookingTime) {
            this.cookingTime = cookingTime;
            return this;
        }

        public Builder experience(float experience) {
            this.experience = experience;
            return this;
        }

        public Builder group(String group) {
            this.group = group;
            return this;
        }

        public Builder addCustomFuel(CustomFuel fuel) {
            this.customFuels.add(fuel);
            return this;
        }

        public Builder customFuels(List<CustomFuel> fuels) {
            this.customFuels = fuels != null ? fuels : new ArrayList<>();
            return this;
        }

        public FurnaceRecipeData build() {
            if (input == null) {
                throw new IllegalStateException("Input ingredient is required for furnace recipe");
            }
            return new FurnaceRecipeData(input, exactInput, cookingTime, experience, group, customFuels);
        }
    }

    public static Builder builder() {
        return new Builder();
    }
}
