package org.hikarii.customrecipes.config;

public final class MigrationVersions {
    public static final int CONFIG_VERSION = 3;
    public static final int VANILLA_RECIPES_VERSION = 4;
    public static final int VANILLA_FURNACE_RECIPES_VERSION = 1;

    public static final String CONFIG_VERSION_KEY = "config-version";
    public static final String VANILLA_RECIPES_VERSION_KEY = "vanilla-recipes-version";
    public static final String VANILLA_FURNACE_RECIPES_VERSION_KEY = "vanilla-furnace-recipes-version";

    private MigrationVersions() {
        throw new UnsupportedOperationException("Utility class");
    }
}