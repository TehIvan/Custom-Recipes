package org.hikarii.customrecipes;

import org.bukkit.plugin.java.JavaPlugin;
import org.hikarii.customrecipes.command.CustomRecipesCommand;
import org.hikarii.customrecipes.config.ConfigManager;
import org.hikarii.customrecipes.config.ConfigMigration;
import org.hikarii.customrecipes.config.DefaultRecipesManager;
import org.hikarii.customrecipes.config.RecipeStateTracker;
import org.hikarii.customrecipes.language.LanguageManager;
import org.hikarii.customrecipes.listener.FurnaceFuelListener;
import org.hikarii.customrecipes.listener.RecipeAmountListener;
import org.hikarii.customrecipes.listener.RecipeCraftListener;
import org.hikarii.customrecipes.listener.RecipeDiscoverListener;
import org.hikarii.customrecipes.listener.RecipeHidingListener;
import org.hikarii.customrecipes.listener.RecipePreviewListener;
import org.hikarii.customrecipes.recipe.CraftEventPresetManager;
import org.hikarii.customrecipes.recipe.CraftTracker;
import org.hikarii.customrecipes.recipe.RecipeDataManager;
import org.hikarii.customrecipes.recipe.RecipeManager;
import org.hikarii.customrecipes.recipe.RecipeWorldManager;
import org.hikarii.customrecipes.integration.VaultIntegration;
import org.hikarii.customrecipes.recipe.vanilla.VanillaRecipeManager;
import org.hikarii.customrecipes.update.UpdateChecker;
import org.hikarii.customrecipes.update.UpdateNotifier;
import org.hikarii.customrecipes.update.UpdateSource;
import org.hikarii.customrecipes.util.MessageUtil;
import org.hikarii.customrecipes.version.VersionManager;
import org.hikarii.customrecipes.version.MaterialValidator;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.bstats.charts.SingleLineChart;

public final class CustomRecipes extends JavaPlugin {
    private static CustomRecipes instance;
    private ConfigManager configManager;
    private ConfigMigration configMigration;
    private RecipeManager recipeManager;
    private boolean debugMode = false;
    private boolean keepSpawnEggNames = false;
    private boolean useCraftedCustomNames = true;
    private UpdateChecker updateChecker;
    private RecipeDataManager recipeDataManager;
    private RecipeWorldManager recipeWorldManager;
    private VanillaRecipeManager vanillaRecipeManager;
    private RecipeStateTracker recipeStateTracker;
    private VersionManager versionManager;
    private MaterialValidator materialValidator;
    private LanguageManager languageManager;
    private CraftTracker craftTracker;
    private VaultIntegration vaultIntegration;
    private CraftEventPresetManager craftEventPresetManager;
    private org.hikarii.customrecipes.data.PlayerFavoritesManager playerFavoritesManager;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        MessageUtil.setPlugin(this);

        this.versionManager = new VersionManager();
        this.materialValidator = new MaterialValidator(this, versionManager);
        getLogger().info("Running on Minecraft " + versionManager.getVersionString());

        this.recipeManager = new RecipeManager(this);
        this.recipeDataManager = new RecipeDataManager(this);
        this.recipeWorldManager = new RecipeWorldManager(this);
        this.configManager = new ConfigManager(this);
        this.recipeStateTracker = new RecipeStateTracker(this);
        this.configMigration = new ConfigMigration(this);
        if (!configMigration.checkAndMigrate()) {
            getLogger().severe("Config migration failed! Some recipes may not work correctly.");
            getLogger().severe("Please check the console for errors and report them to the developer.");
        }

        this.languageManager = new LanguageManager(this);

        this.craftTracker = new CraftTracker(this);
        this.vaultIntegration = new VaultIntegration(this);
        this.craftEventPresetManager = new CraftEventPresetManager(this);
        this.playerFavoritesManager = new org.hikarii.customrecipes.data.PlayerFavoritesManager(this);

        DefaultRecipesManager defaultRecipes = new DefaultRecipesManager(this);
        defaultRecipes.initializeDefaultRecipes();
        this.vanillaRecipeManager = new VanillaRecipeManager(this);
        if (!loadConfiguration()) {
            getLogger().severe("Failed to load configuration! Plugin will be disabled.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        registerCommands();
        registerListeners();
        getLogger().info("CustomRecipes has been enabled!");
        getLogger().info("Loaded " + recipeManager.getRecipeCount() + " custom recipes");
        initializeMetrics();
        initializeUpdateChecker();
    }

    @Override
    public void onDisable() {
        if (recipeManager != null) {
            recipeManager.unregisterAll();
        }
        if (updateChecker != null) {
            updateChecker.stopPeriodicCheck();
        }
        if (vanillaRecipeManager != null) {
            vanillaRecipeManager.stopCleanupTask();
        }
        if (craftTracker != null) {
            craftTracker.saveData();
        }
        getLogger().info("CustomRecipes has been disabled!");
    }

    public boolean loadConfiguration() {
        try {
            reloadConfig();
            debugMode = getConfig().getBoolean("debug", false);
            useCraftedCustomNames = getConfig().getBoolean("use-crafted-custom-names", true);
            keepSpawnEggNames = getConfig().getBoolean("spawn-egg-keep-custom-name", false);
            
            configManager.syncEnabledRecipesWithFiles();
            configManager.loadRecipes();
            recipeStateTracker.syncEnabledRecipes();
            recipeManager.registerAllRecipes();
            if (recipeWorldManager != null) {
                recipeWorldManager.loadWorldRestrictions();
            }
            return true;
        } catch (Exception e) {
            getLogger().severe("Error loading configuration: " + e.getMessage());
            if (debugMode) {
                e.printStackTrace();
            }
            return false;
        }
    }

    private void registerCommands() {
        CustomRecipesCommand commandExecutor = new CustomRecipesCommand(this);
        getCommand("customrecipes").setExecutor(commandExecutor);
        getCommand("customrecipes").setTabCompleter(commandExecutor);
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(
                new RecipeDiscoverListener(this), this
        );
        getServer().getPluginManager().registerEvents(
                new RecipeHidingListener(this), this
        );
        getServer().getPluginManager().registerEvents(
                new RecipeAmountListener(this), this
        );
        getServer().getPluginManager().registerEvents(
                new RecipeCraftListener(this), this
        );
        getServer().getPluginManager().registerEvents(
                new FurnaceFuelListener(this), this
        );
        getServer().getPluginManager().registerEvents(
                new RecipePreviewListener(this), this
        );
        getServer().getPluginManager().registerEvents(
                new org.hikarii.customrecipes.listener.VanillaRecipeSyncListener(this), this
        );
        getServer().getPluginManager().registerEvents(
                new org.hikarii.customrecipes.listener.VanillaRecipeBlockListener(this), this
        );
        getServer().getPluginManager().registerEvents(
                new org.hikarii.customrecipes.listener.NoPlaceBlockListener(this), this
        );
    }

    public static CustomRecipes getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public RecipeManager getRecipeManager() {
        return recipeManager;
    }

    public VanillaRecipeManager getVanillaRecipeManager() {
        return vanillaRecipeManager;
    }

    public LanguageManager getLanguageManager() {
        return languageManager;
    }

    public RecipeStateTracker getRecipeStateTracker() {
        return recipeStateTracker;
    }

    public boolean isDebugMode() {
        return debugMode;
    }

    public boolean isKeepSpawnEggNames() {
        return keepSpawnEggNames;
    }

    public boolean isUseCraftedCustomNames() {
        return useCraftedCustomNames;
    }

    public RecipeDataManager getRecipeDataManager() {
        return recipeDataManager;
    }

    public void setDebugMode(boolean debugMode) {
        this.debugMode = debugMode;
        getConfig().set("debug", debugMode);
        saveConfig();
    }

    public void debug(String message) {
        if (debugMode) {
            getLogger().info("[DEBUG] " + message);
        }
    }

    private void initializeMetrics() {
        int pluginId = 27998;
        Metrics metrics = new Metrics(this, pluginId);
        metrics.addCustomChart(new SingleLineChart("total_recipes", () -> recipeManager.getRecipeCount()));
        metrics.addCustomChart(new SingleLineChart("enabled_recipes", () -> {
            return (int) recipeManager.getAllRecipes().stream()
                    .filter(recipe -> recipeManager.isRecipeEnabled(recipe.getKey()))
                    .count();
        }));
        metrics.addCustomChart(new SimplePie("using_custom_names", () ->
                useCraftedCustomNames ? "Yes" : "No"
        ));
        debug("bStats metrics initialized");
    }

    public RecipeWorldManager getRecipeWorldManager() {
        return recipeWorldManager;
    }

    public UpdateChecker getUpdateChecker() {
        return updateChecker;
    }

    private void initializeUpdateChecker() {
        updateChecker = new UpdateChecker(this, UpdateSource.GITHUB, "130198", "hikarii-dev/Custom-Recipes");
        updateChecker.startPeriodicCheck();
        getServer().getPluginManager().registerEvents(new UpdateNotifier(this, updateChecker), this);
        debug("Update checker initialized with periodic checks");
    }

    public VersionManager getVersionManager() {
        return versionManager;
    }

    public MaterialValidator getMaterialValidator() {
        return materialValidator;
    }

    public CraftTracker getCraftTracker() {
        return craftTracker;
    }

    public VaultIntegration getVaultIntegration() {
        return vaultIntegration;
    }

    public CraftEventPresetManager getCraftEventPresetManager() {
        return craftEventPresetManager;
    }

    public org.hikarii.customrecipes.data.PlayerFavoritesManager getPlayerFavoritesManager() {
        return playerFavoritesManager;
    }
}