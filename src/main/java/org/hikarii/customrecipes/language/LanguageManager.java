package org.hikarii.customrecipes.language;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.configuration.file.YamlConfiguration;
import org.hikarii.customrecipes.CustomRecipes;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.*;

public class LanguageManager {
    private final CustomRecipes plugin;
    private final File langFolder;
    private final Map<String, YamlConfiguration> loadedLanguages;
    private final MiniMessage miniMessage;
    private String currentLanguage;

    public static final String ENGLISH = "en_US";
    public static final String RUSSIAN = "ru_RU";
    public static final String GERMAN = "de_DE";
    public static final String UKRAINIAN = "uk_UA";

    public LanguageManager(CustomRecipes plugin) {
        this.plugin = plugin;
        this.langFolder = new File(plugin.getDataFolder(), "lang");
        this.loadedLanguages = new HashMap<>();
        this.miniMessage = MiniMessage.miniMessage();

        if (!langFolder.exists()) {
            langFolder.mkdirs();
        }

        this.currentLanguage = plugin.getConfig().getString("language", ENGLISH);

        initializeLanguageFiles();

        loadAllLanguages();
    }

    private void initializeLanguageFiles() {
        String[] languages = {ENGLISH, RUSSIAN, GERMAN, UKRAINIAN};

        for (String lang : languages) {
            File langFile = new File(langFolder, lang + ".yml");

            try (InputStream in = plugin.getResource("lang/" + lang + ".yml")) {
                if (in != null) {
                    
                    if (langFile.exists()) {
                        langFile.delete();
                    }
                    Files.copy(in, langFile.toPath());
                    plugin.getLogger().info("Initialized language file: " + lang + ".yml");
                }
            } catch (IOException e) {
                plugin.getLogger().warning("Failed to initialize language file " + lang + ".yml: " + e.getMessage());
            }
        }
    }

    private void loadAllLanguages() {
        File[] files = langFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) return;

        for (File file : files) {
            String langCode = file.getName().replace(".yml", "");
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            loadedLanguages.put(langCode, config);
            plugin.debug("Loaded language: " + langCode);
        }

        plugin.getLogger().info("Loaded " + loadedLanguages.size() + " language(s)");
    }

    public String getMessage(String key) {
        return getMessage(key, new HashMap<>());
    }

    public String getMessage(String key, Map<String, String> placeholders) {
        YamlConfiguration config = loadedLanguages.get(currentLanguage);

        if (config == null) {
            config = loadedLanguages.get(ENGLISH);
        }

        if (config == null || !config.contains(key)) {
            plugin.getLogger().warning("Missing translation key: " + key + " for language: " + currentLanguage);
            return key;
        }

        String message = config.getString(key, key);

        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            message = message.replace("{" + entry.getKey() + "}", entry.getValue());
        }

        return message;
    }

    public Component getComponent(String key) {
        return Component.text(getMessage(key));
    }

    public Component getMiniMessage(String key) {
        return miniMessage.deserialize(getMessage(key));
    }

    public Component getComponent(String key, Map<String, String> placeholders) {
        return Component.text(getMessage(key, placeholders));
    }

    public List<String> getMessageList(String key) {
        YamlConfiguration config = loadedLanguages.get(currentLanguage);

        if (config == null) {
            config = loadedLanguages.get(ENGLISH);
        }

        if (config == null || !config.contains(key)) {
            return Collections.emptyList();
        }

        return config.getStringList(key);
    }

    public String getMaterialName(String materialName) {
        
        String key = "minecraft." + materialName.toLowerCase().replace(" ", "_");

        YamlConfiguration config = loadedLanguages.get(currentLanguage);
        if (config == null) {
            config = loadedLanguages.get(ENGLISH);
        }

        if (config != null && config.contains(key)) {
            return config.getString(key, materialName);
        }

        return formatEnumName(materialName);
    }

    public String getCategoryName(String categoryName) {
        String key = "minecraft.category." + categoryName.toLowerCase();

        YamlConfiguration config = loadedLanguages.get(currentLanguage);
        if (config == null) {
            config = loadedLanguages.get(ENGLISH);
        }

        if (config != null && config.contains(key)) {
            return config.getString(key, categoryName);
        }

        return formatEnumName(categoryName);
    }

    public String getStationName(String stationName) {
        String key = "minecraft.station." + stationName.toLowerCase();

        YamlConfiguration config = loadedLanguages.get(currentLanguage);
        if (config == null) {
            config = loadedLanguages.get(ENGLISH);
        }

        if (config != null && config.contains(key)) {
            return config.getString(key, stationName);
        }

        return formatEnumName(stationName);
    }

    public String getRecipeTypeName(String typeName) {
        String key = "type." + typeName.toLowerCase();

        YamlConfiguration config = loadedLanguages.get(currentLanguage);
        if (config == null) {
            config = loadedLanguages.get(ENGLISH);
        }

        if (config != null && config.contains(key)) {
            return config.getString(key, typeName);
        }

        return formatEnumName(typeName);
    }

    public String getEnchantmentName(String enchantmentKey) {
        String key = "minecraft.enchantment." + enchantmentKey.toLowerCase();

        YamlConfiguration config = loadedLanguages.get(currentLanguage);
        if (config == null) {
            config = loadedLanguages.get(ENGLISH);
        }

        if (config != null && config.contains(key)) {
            return config.getString(key, enchantmentKey);
        }

        return formatEnumName(enchantmentKey);
    }

    public String getVanillaRecipeName(String recipeKey) {
        String recipeKeyLower = recipeKey.toLowerCase();

        boolean isFurnaceRecipe = recipeKeyLower.startsWith("furnace_");
        String lookupKey = isFurnaceRecipe ? recipeKeyLower.substring(8) : recipeKeyLower;

        YamlConfiguration config = loadedLanguages.get(currentLanguage);
        if (config == null) {
            config = loadedLanguages.get(ENGLISH);
        }

        String vanillaKey = "minecraft.vanilla_recipe." + lookupKey;
        if (config != null && config.contains(vanillaKey)) {
            String value = config.getString(vanillaKey, recipeKey);
            
            if (!value.equals(recipeKey) && !value.matches("^[A-Z][a-z].*")) {
                return value;
            }
        }

        String materialKey = "minecraft." + lookupKey;
        if (config != null && config.contains(materialKey)) {
            return config.getString(materialKey, recipeKey);
        }

        return formatEnumName(lookupKey);
    }

    private String formatEnumName(String enumName) {
        String[] words = enumName.toLowerCase().split("_");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (result.length() > 0) result.append(" ");
            result.append(word.substring(0, 1).toUpperCase()).append(word.substring(1));
        }
        return result.toString();
    }

    public String getCurrentLanguage() {
        return currentLanguage;
    }

    public void setLanguage(String language) {
        if (!loadedLanguages.containsKey(language)) {
            plugin.getLogger().warning("Language not found: " + language + ", keeping current: " + currentLanguage);
            return;
        }

        this.currentLanguage = language;
        plugin.getConfig().set("language", language);
        plugin.saveConfig();

        plugin.getLogger().info("Language changed to: " + language);
    }

    public Map<String, String> getAvailableLanguages() {
        Map<String, String> languages = new LinkedHashMap<>();

        for (String langCode : loadedLanguages.keySet()) {
            YamlConfiguration config = loadedLanguages.get(langCode);
            String displayName = config.getString("language.name", langCode);
            languages.put(langCode, displayName);
        }

        return languages;
    }

    public void reload() {
        loadedLanguages.clear();
        loadAllLanguages();
        this.currentLanguage = plugin.getConfig().getString("language", ENGLISH);
        plugin.getLogger().info("Language files reloaded");
    }
}
