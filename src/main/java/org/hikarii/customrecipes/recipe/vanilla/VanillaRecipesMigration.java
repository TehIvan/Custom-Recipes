package org.hikarii.customrecipes.recipe.vanilla;

import org.bukkit.configuration.file.YamlConfiguration;
import org.hikarii.customrecipes.CustomRecipes;
import org.hikarii.customrecipes.config.MigrationVersions;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class VanillaRecipesMigration {
    private final CustomRecipes plugin;

    public VanillaRecipesMigration(CustomRecipes plugin) {
        this.plugin = plugin;
    }

    public boolean checkAndMigrate() {
        boolean craftingSuccess = migrateCraftingRecipes();
        boolean furnaceSuccess = migrateFurnaceRecipes();
        return craftingSuccess && furnaceSuccess;
    }

    private boolean migrateCraftingRecipes() {
        File vanillaRecipesFile = new File(plugin.getDataFolder(), "vanilla-recipes.yml");
        if (!vanillaRecipesFile.exists()) {
            plugin.saveResource("vanilla-recipes.yml", false);
            YamlConfiguration config = YamlConfiguration.loadConfiguration(vanillaRecipesFile);
            config.set(MigrationVersions.VANILLA_RECIPES_VERSION_KEY, MigrationVersions.VANILLA_RECIPES_VERSION);
            try {
                config.save(vanillaRecipesFile);
            } catch (IOException e) {
                plugin.getLogger().warning("Failed to set version in vanilla-recipes.yml");
            }
            return true;
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(vanillaRecipesFile);
        int version = config.getInt(MigrationVersions.VANILLA_RECIPES_VERSION_KEY, 0);
        if (version == MigrationVersions.VANILLA_RECIPES_VERSION) {
            plugin.debug("Vanilla recipes are up to date (v" + version + ")");
            return true;
        }

        if (version < MigrationVersions.VANILLA_RECIPES_VERSION) {
            plugin.getLogger().info("Migrating vanilla recipes from v" + version + " to v" + MigrationVersions.VANILLA_RECIPES_VERSION);
            createBackup(vanillaRecipesFile);
            plugin.saveResource("vanilla-recipes.yml", true);
            config = YamlConfiguration.loadConfiguration(vanillaRecipesFile);
            config.set(MigrationVersions.VANILLA_RECIPES_VERSION_KEY, MigrationVersions.VANILLA_RECIPES_VERSION);
            try {
                config.save(vanillaRecipesFile);
                plugin.getLogger().info("✓ Vanilla recipes updated to v" + MigrationVersions.VANILLA_RECIPES_VERSION);
                return true;
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to update vanilla recipes: " + e.getMessage());
                return false;
            }
        }
        return true;
    }

    private boolean migrateFurnaceRecipes() {
        File furnaceRecipesFile = new File(plugin.getDataFolder(), "vanilla-furnace-recipes.yml");
        if (!furnaceRecipesFile.exists()) {
            plugin.saveResource("vanilla-furnace-recipes.yml", false);
            YamlConfiguration config = YamlConfiguration.loadConfiguration(furnaceRecipesFile);
            config.set(MigrationVersions.VANILLA_FURNACE_RECIPES_VERSION_KEY, MigrationVersions.VANILLA_FURNACE_RECIPES_VERSION);
            try {
                config.save(furnaceRecipesFile);
            } catch (IOException e) {
                plugin.getLogger().warning("Failed to set version in vanilla-furnace-recipes.yml");
            }
            return true;
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(furnaceRecipesFile);
        int version = config.getInt(MigrationVersions.VANILLA_FURNACE_RECIPES_VERSION_KEY, 0);
        if (version == MigrationVersions.VANILLA_FURNACE_RECIPES_VERSION) {
            plugin.debug("Vanilla furnace recipes are up to date (v" + version + ")");
            return true;
        }

        if (version < MigrationVersions.VANILLA_FURNACE_RECIPES_VERSION) {
            plugin.getLogger().info("Migrating vanilla furnace recipes from v" + version + " to v" + MigrationVersions.VANILLA_FURNACE_RECIPES_VERSION);
            createBackup(furnaceRecipesFile, "vanilla-furnace-recipes-backup-");
            plugin.saveResource("vanilla-furnace-recipes.yml", true);
            config = YamlConfiguration.loadConfiguration(furnaceRecipesFile);
            config.set(MigrationVersions.VANILLA_FURNACE_RECIPES_VERSION_KEY, MigrationVersions.VANILLA_FURNACE_RECIPES_VERSION);
            try {
                config.save(furnaceRecipesFile);
                plugin.getLogger().info("✓ Vanilla furnace recipes updated to v" + MigrationVersions.VANILLA_FURNACE_RECIPES_VERSION);
                return true;
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to update vanilla furnace recipes: " + e.getMessage());
                return false;
            }
        }
        return true;
    }

    private void createBackup(File file) {
        createBackup(file, "vanilla-recipes-backup-");
    }

    private void createBackup(File file, String prefix) {
        try {
            File backupFolder = new File(plugin.getDataFolder(), "backups");
            if (!backupFolder.exists()) {
                backupFolder.mkdirs();
            }
            String timestamp = String.valueOf(System.currentTimeMillis());
            File backupFile = new File(backupFolder, prefix + timestamp + ".yml");
            Files.copy(file.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            plugin.getLogger().info("Created backup: " + backupFile.getName());
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to create backup: " + e.getMessage());
        }
    }
}