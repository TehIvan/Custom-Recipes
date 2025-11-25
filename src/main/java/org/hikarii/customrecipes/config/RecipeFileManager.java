package org.hikarii.customrecipes.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.hikarii.customrecipes.CustomRecipes;
import org.hikarii.customrecipes.recipe.CustomRecipe;
import org.hikarii.customrecipes.recipe.RecipeType;
import java.io.File;
import java.io.IOException;
import java.util.*;

public class RecipeFileManager {
    private final CustomRecipes plugin;
    private final File recipesFolder;
    public RecipeFileManager(CustomRecipes plugin) {
        this.plugin = plugin;
        this.recipesFolder = new File(plugin.getDataFolder(), "recipes");
        if (!recipesFolder.exists()) {
            recipesFolder.mkdirs();
        }
    }

    public Map<String, CustomRecipe> loadAllRecipes() throws ValidationException {
        Map<String, CustomRecipe> recipes = new HashMap<>();
        RecipeConfigLoader loader = new RecipeConfigLoader(plugin);

        List<File> allFiles = new ArrayList<>();

        File[] mainFiles = recipesFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (mainFiles != null) {
            allFiles.addAll(Arrays.asList(mainFiles));
        }

        File[] subfolders = recipesFolder.listFiles(File::isDirectory);
        if (subfolders != null) {
            for (File subfolder : subfolders) {
                File[] subFiles = subfolder.listFiles((dir, name) -> name.endsWith(".yml"));
                if (subFiles != null) {
                    allFiles.addAll(Arrays.asList(subFiles));
                }
            }
        }

        if (allFiles.isEmpty()) {
            plugin.getLogger().info("No recipe files found in recipes folder");
            return recipes;
        }

        int successCount = 0;
        int failCount = 0;
        for (File file : allFiles) {
            try {
                YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                String recipeKey = file.getName().replace(".yml", "");
                CustomRecipe recipe = loader.loadRecipe(recipeKey, config);
                recipes.put(recipeKey.toLowerCase(), recipe);
                successCount++;
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to load recipe from " + file.getName() + ": " + e.getMessage());
                failCount++;
                if (plugin.isDebugMode()) {
                    e.printStackTrace();
                }
            }
        }
        plugin.getLogger().info("Loaded " + successCount + " recipes from files" +
                (failCount > 0 ? " (" + failCount + " failed)" : ""));
        return recipes;
    }

    private File getFolderForType(RecipeType type) {
        if (type == null) return recipesFolder;

        return switch (type) {
            case SHAPED, SHAPELESS -> recipesFolder; 
            case FURNACE -> getOrCreateSubfolder("furnace");
            case BLAST_FURNACE -> getOrCreateSubfolder("blast_furnace");
            case SMOKER -> getOrCreateSubfolder("smoker");
            default -> recipesFolder;
        };
    }

    private File getOrCreateSubfolder(String name) {
        File subfolder = new File(recipesFolder, name);
        if (!subfolder.exists()) {
            subfolder.mkdirs();
        }
        return subfolder;
    }

    public void saveRecipe(String recipeKey, ConfigurationSection recipeData) throws IOException {
        saveRecipe(recipeKey, recipeData, null);
    }

    public void saveRecipe(String recipeKey, ConfigurationSection recipeData, RecipeType type) throws IOException {
        File folder = getFolderForType(type);
        File recipeFile = new File(folder, recipeKey + ".yml");
        YamlConfiguration config = new YamlConfiguration();
        for (String key : recipeData.getKeys(false)) {
            config.set(key, recipeData.get(key));
        }
        config.save(recipeFile);
        plugin.debug("Saved recipe to file: " + recipeFile.getPath());
    }

    public void saveRecipe(String recipeKey, Map<String, Object> data) throws IOException {
        saveRecipe(recipeKey, data, null);
    }

    public void saveRecipe(String recipeKey, Map<String, Object> data, RecipeType type) throws IOException {
        File folder = getFolderForType(type);
        File recipeFile = new File(folder, recipeKey + ".yml");
        YamlConfiguration config = new YamlConfiguration();
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            config.set(entry.getKey(), entry.getValue());
        }
        config.save(recipeFile);
        plugin.debug("Saved recipe to file: " + recipeFile.getPath());
    }

    public boolean deleteRecipe(String recipeKey) {
        
        File recipeFile = new File(recipesFolder, recipeKey + ".yml");
        if (recipeFile.exists()) {
            return recipeFile.delete();
        }

        File[] subfolders = recipesFolder.listFiles(File::isDirectory);
        if (subfolders != null) {
            for (File subfolder : subfolders) {
                recipeFile = new File(subfolder, recipeKey + ".yml");
                if (recipeFile.exists()) {
                    return recipeFile.delete();
                }
            }
        }
        return false;
    }

    public boolean deleteRecipe(String recipeKey, RecipeType type) {
        File folder = getFolderForType(type);
        File recipeFile = new File(folder, recipeKey + ".yml");
        if (recipeFile.exists()) {
            return recipeFile.delete();
        }
        return false;
    }

    public boolean recipeFileExists(String recipeKey) {
        
        File recipeFile = new File(recipesFolder, recipeKey + ".yml");
        if (recipeFile.exists()) {
            return true;
        }

        File[] subfolders = recipesFolder.listFiles(File::isDirectory);
        if (subfolders != null) {
            for (File subfolder : subfolders) {
                recipeFile = new File(subfolder, recipeKey + ".yml");
                if (recipeFile.exists()) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean recipeFileExists(String recipeKey, RecipeType type) {
        File folder = getFolderForType(type);
        File recipeFile = new File(folder, recipeKey + ".yml");
        return recipeFile.exists();
    }

    public File getRecipeFile(String recipeKey, RecipeType type) {
        File folder = getFolderForType(type);
        File recipeFile = new File(folder, recipeKey + ".yml");
        return recipeFile.exists() ? recipeFile : null;
    }

    public File getRecipesFolder() {
        return recipesFolder;
    }

    public Set<String> getAllRecipeKeysFromFiles() {
        Set<String> keys = new HashSet<>();

        File[] mainFiles = recipesFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (mainFiles != null) {
            for (File file : mainFiles) {
                keys.add(file.getName().replace(".yml", ""));
            }
        }

        File[] subfolders = recipesFolder.listFiles(File::isDirectory);
        if (subfolders != null) {
            for (File subfolder : subfolders) {
                File[] subFiles = subfolder.listFiles((dir, name) -> name.endsWith(".yml"));
                if (subFiles != null) {
                    for (File file : subFiles) {
                        keys.add(file.getName().replace(".yml", ""));
                    }
                }
            }
        }

        return keys;
    }

    public File findRecipeFile(String recipeKey) {
        
        File recipeFile = new File(recipesFolder, recipeKey + ".yml");
        plugin.debug("Looking for recipe file: " + recipeFile.getAbsolutePath());
        if (recipeFile.exists()) {
            plugin.debug("Found recipe file in main folder: " + recipeFile.getName());
            return recipeFile;
        }

        File[] subfolders = recipesFolder.listFiles(File::isDirectory);
        if (subfolders != null) {
            for (File subfolder : subfolders) {
                recipeFile = new File(subfolder, recipeKey + ".yml");
                plugin.debug("Looking for recipe file: " + recipeFile.getAbsolutePath());
                if (recipeFile.exists()) {
                    plugin.debug("Found recipe file in subfolder: " + subfolder.getName() + "/" + recipeFile.getName());
                    return recipeFile;
                }
            }
        }

        plugin.debug("Recipe file not found for key: " + recipeKey);
        plugin.debug("Files in recipes folder:");
        listFilesRecursively(recipesFolder, "  ");

        return null;
    }

    private void listFilesRecursively(File folder, String indent) {
        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                plugin.debug(indent + (file.isDirectory() ? "[DIR] " : "") + file.getName());
                if (file.isDirectory()) {
                    listFilesRecursively(file, indent + "  ");
                }
            }
        }
    }
}