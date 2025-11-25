package org.hikarii.customrecipes.data;

import org.bukkit.configuration.file.YamlConfiguration;
import org.hikarii.customrecipes.CustomRecipes;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class PlayerFavoritesManager {
    private final CustomRecipes plugin;
    private final File dataFile;
    private final Map<UUID, Set<String>> favorites = new HashMap<>();

    public PlayerFavoritesManager(CustomRecipes plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "player-favorites.yml");
        load();
    }

    public void load() {
        favorites.clear();
        if (!dataFile.exists()) {
            return;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(dataFile);
        for (String uuidStr : config.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(uuidStr);
                List<String> recipeList = config.getStringList(uuidStr);
                favorites.put(uuid, new HashSet<>(recipeList));
            } catch (IllegalArgumentException ignored) {
                
            }
        }
    }

    public void save() {
        YamlConfiguration config = new YamlConfiguration();
        for (Map.Entry<UUID, Set<String>> entry : favorites.entrySet()) {
            config.set(entry.getKey().toString(), new ArrayList<>(entry.getValue()));
        }

        try {
            config.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save player favorites: " + e.getMessage());
        }
    }

    public Set<String> getFavorites(UUID playerUuid) {
        return favorites.getOrDefault(playerUuid, new HashSet<>());
    }

    public boolean isFavorite(UUID playerUuid, String recipeKey) {
        return getFavorites(playerUuid).contains(recipeKey.toLowerCase());
    }

    public void addFavorite(UUID playerUuid, String recipeKey) {
        favorites.computeIfAbsent(playerUuid, k -> new HashSet<>()).add(recipeKey.toLowerCase());
        save();
    }

    public void removeFavorite(UUID playerUuid, String recipeKey) {
        Set<String> playerFavorites = favorites.get(playerUuid);
        if (playerFavorites != null) {
            playerFavorites.remove(recipeKey.toLowerCase());
            save();
        }
    }

    public void toggleFavorite(UUID playerUuid, String recipeKey) {
        if (isFavorite(playerUuid, recipeKey)) {
            removeFavorite(playerUuid, recipeKey);
        } else {
            addFavorite(playerUuid, recipeKey);
        }
    }
}
