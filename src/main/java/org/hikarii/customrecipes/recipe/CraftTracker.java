package org.hikarii.customrecipes.recipe;

import org.bukkit.configuration.file.YamlConfiguration;
import org.hikarii.customrecipes.CustomRecipes;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CraftTracker {
    private final CustomRecipes plugin;
    private final File dataFile;

    private final Map<UUID, Map<String, Long>> cooldowns = new ConcurrentHashMap<>();

    private final Map<UUID, Map<String, CraftCounts>> craftCounts = new ConcurrentHashMap<>();

    public CraftTracker(CustomRecipes plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "craft-data.yml");
        loadData();
    }

    public int getRemainingCooldown(UUID playerUUID, String recipeKey, int cooldownSeconds) {
        if (cooldownSeconds <= 0) return 0;

        Map<String, Long> playerCooldowns = cooldowns.get(playerUUID);
        if (playerCooldowns == null) return 0;

        Long lastCraft = playerCooldowns.get(recipeKey.toLowerCase());
        if (lastCraft == null) return 0;

        long elapsed = (System.currentTimeMillis() - lastCraft) / 1000;
        int remaining = (int) (cooldownSeconds - elapsed);
        return Math.max(0, remaining);
    }

    public void setCooldown(UUID playerUUID, String recipeKey) {
        cooldowns.computeIfAbsent(playerUUID, k -> new ConcurrentHashMap<>())
                .put(recipeKey.toLowerCase(), System.currentTimeMillis());
    }

    public int getDailyCrafts(UUID playerUUID, String recipeKey) {
        CraftCounts counts = getCraftCounts(playerUUID, recipeKey);
        if (counts == null) return 0;

        LocalDate today = LocalDate.now();
        if (!today.equals(counts.lastDailyReset)) {
            counts.dailyCount = 0;
            counts.lastDailyReset = today;
        }
        return counts.dailyCount;
    }

    public int getWeeklyCrafts(UUID playerUUID, String recipeKey) {
        CraftCounts counts = getCraftCounts(playerUUID, recipeKey);
        if (counts == null) return 0;

        int currentWeek = LocalDate.now().get(WeekFields.ISO.weekOfYear());
        int currentYear = LocalDate.now().getYear();

        if (currentWeek != counts.lastWeekNumber || currentYear != counts.lastWeekYear) {
            counts.weeklyCount = 0;
            counts.lastWeekNumber = currentWeek;
            counts.lastWeekYear = currentYear;
        }
        return counts.weeklyCount;
    }

    public int getTotalCrafts(UUID playerUUID, String recipeKey) {
        CraftCounts counts = getCraftCounts(playerUUID, recipeKey);
        return counts == null ? 0 : counts.totalCount;
    }

    public void incrementCraftCount(UUID playerUUID, String recipeKey) {
        CraftCounts counts = craftCounts
                .computeIfAbsent(playerUUID, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(recipeKey.toLowerCase(), k -> new CraftCounts());

        LocalDate today = LocalDate.now();
        int currentWeek = today.get(WeekFields.ISO.weekOfYear());
        int currentYear = today.getYear();

        if (!today.equals(counts.lastDailyReset)) {
            counts.dailyCount = 0;
            counts.lastDailyReset = today;
        }

        if (currentWeek != counts.lastWeekNumber || currentYear != counts.lastWeekYear) {
            counts.weeklyCount = 0;
            counts.lastWeekNumber = currentWeek;
            counts.lastWeekYear = currentYear;
        }

        counts.dailyCount++;
        counts.weeklyCount++;
        counts.totalCount++;

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, this::saveData);
    }

    private CraftCounts getCraftCounts(UUID playerUUID, String recipeKey) {
        Map<String, CraftCounts> playerCounts = craftCounts.get(playerUUID);
        if (playerCounts == null) return null;
        return playerCounts.get(recipeKey.toLowerCase());
    }

    public void loadData() {
        if (!dataFile.exists()) return;

        try {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(dataFile);

            for (String uuidStr : config.getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(uuidStr);
                    var section = config.getConfigurationSection(uuidStr);
                    if (section == null) continue;

                    Map<String, CraftCounts> playerCounts = new ConcurrentHashMap<>();

                    for (String recipeKey : section.getKeys(false)) {
                        var recipeSection = section.getConfigurationSection(recipeKey);
                        if (recipeSection == null) continue;

                        CraftCounts counts = new CraftCounts();
                        counts.dailyCount = recipeSection.getInt("daily", 0);
                        counts.weeklyCount = recipeSection.getInt("weekly", 0);
                        counts.totalCount = recipeSection.getInt("total", 0);

                        String dailyResetStr = recipeSection.getString("daily-reset");
                        if (dailyResetStr != null) {
                            try {
                                counts.lastDailyReset = LocalDate.parse(dailyResetStr);
                            } catch (Exception ignored) {}
                        }

                        counts.lastWeekNumber = recipeSection.getInt("week-number", 0);
                        counts.lastWeekYear = recipeSection.getInt("week-year", 0);

                        playerCounts.put(recipeKey, counts);
                    }

                    if (!playerCounts.isEmpty()) {
                        craftCounts.put(uuid, playerCounts);
                    }
                } catch (IllegalArgumentException ignored) {}
            }

            plugin.debug("Loaded craft tracking data for " + craftCounts.size() + " players");
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to load craft tracking data: " + e.getMessage());
        }
    }

    public void saveData() {
        try {
            YamlConfiguration config = new YamlConfiguration();

            for (Map.Entry<UUID, Map<String, CraftCounts>> playerEntry : craftCounts.entrySet()) {
                String uuidStr = playerEntry.getKey().toString();

                for (Map.Entry<String, CraftCounts> recipeEntry : playerEntry.getValue().entrySet()) {
                    String path = uuidStr + "." + recipeEntry.getKey();
                    CraftCounts counts = recipeEntry.getValue();

                    config.set(path + ".daily", counts.dailyCount);
                    config.set(path + ".weekly", counts.weeklyCount);
                    config.set(path + ".total", counts.totalCount);

                    if (counts.lastDailyReset != null) {
                        config.set(path + ".daily-reset", counts.lastDailyReset.toString());
                    }
                    config.set(path + ".week-number", counts.lastWeekNumber);
                    config.set(path + ".week-year", counts.lastWeekYear);
                }
            }

            config.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save craft tracking data: " + e.getMessage());
        }
    }

    public static String formatTime(int seconds) {
        if (seconds < 60) {
            return seconds + "s";
        } else if (seconds < 3600) {
            int mins = seconds / 60;
            int secs = seconds % 60;
            return mins + "m " + secs + "s";
        } else {
            int hours = seconds / 3600;
            int mins = (seconds % 3600) / 60;
            return hours + "h " + mins + "m";
        }
    }

    private static class CraftCounts {
        int dailyCount = 0;
        int weeklyCount = 0;
        int totalCount = 0;
        LocalDate lastDailyReset = LocalDate.now();
        int lastWeekNumber = LocalDate.now().get(WeekFields.ISO.weekOfYear());
        int lastWeekYear = LocalDate.now().getYear();
    }
}
