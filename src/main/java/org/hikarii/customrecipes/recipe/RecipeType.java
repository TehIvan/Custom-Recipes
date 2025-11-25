package org.hikarii.customrecipes.recipe;

public enum RecipeType {
    SHAPED,
    SHAPELESS,
    FURNACE,
    BLAST_FURNACE,
    SMOKER,
    CAMPFIRE;

    public static RecipeType fromString(String type) {
        try {
            return valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            return SHAPED;
        }
    }

    public boolean isFurnaceType() {
        return this == FURNACE || this == BLAST_FURNACE || this == SMOKER || this == CAMPFIRE;
    }

    public boolean isCraftingType() {
        return this == SHAPED || this == SHAPELESS;
    }
}