package org.hikarii.customrecipes.recipe;

import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.hikarii.customrecipes.CustomRecipes;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CraftEventPresetManager {
    private final CustomRecipes plugin;
    private final File presetsFile;
    private final Map<String, CraftEventPreset> presets = new ConcurrentHashMap<>();

    public CraftEventPresetManager(CustomRecipes plugin) {
        this.plugin = plugin;
        this.presetsFile = new File(plugin.getDataFolder(), "craft-event-presets.yml");
        loadPresets();
    }

    public void loadPresets() {
        presets.clear();

        if (!presetsFile.exists()) {
            plugin.debug("No craft event presets file found, starting fresh");
            return;
        }

        try {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(presetsFile);

            for (String presetName : config.getKeys(false)) {
                ConfigurationSection section = config.getConfigurationSection(presetName);
                if (section == null) continue;

                try {
                    CraftEventPreset preset = loadPreset(presetName, section);
                    if (preset != null) {
                        presets.put(presetName.toLowerCase(), preset);
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to load preset '" + presetName + "': " + e.getMessage());
                }
            }

            plugin.debug("Loaded " + presets.size() + " craft event presets");
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to load craft event presets: " + e.getMessage());
        }
    }

    private CraftEventPreset loadPreset(String name, ConfigurationSection section) {
        
        String scopeStr = section.getString("scope", "ALL");
        CraftEventPreset.RecipeScope scope;
        try {
            scope = CraftEventPreset.RecipeScope.valueOf(scopeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            scope = CraftEventPreset.RecipeScope.ALL;
        }

        List<CraftEventPreset.SoundEvent> sounds = new ArrayList<>();
        if (section.contains("sounds")) {
            List<Map<?, ?>> soundsList = section.getMapList("sounds");
            for (Map<?, ?> soundMap : soundsList) {
                try {
                    String soundName = (String) soundMap.get("sound");
                    if (soundName == null) continue;

                    Sound sound = Sound.valueOf(soundName.toUpperCase());
                    float volume = soundMap.containsKey("volume") ?
                            ((Number) soundMap.get("volume")).floatValue() : 1.0f;
                    float pitch = soundMap.containsKey("pitch") ?
                            ((Number) soundMap.get("pitch")).floatValue() : 1.0f;

                    sounds.add(new CraftEventPreset.SoundEvent(sound, volume, pitch));
                } catch (Exception e) {
                    plugin.getLogger().warning("Invalid sound in preset '" + name + "': " + e.getMessage());
                }
            }
        }

        List<CraftEventPreset.ParticleEvent> particles = new ArrayList<>();
        if (section.contains("particles")) {
            List<Map<?, ?>> particlesList = section.getMapList("particles");
            for (Map<?, ?> particleMap : particlesList) {
                try {
                    String particleName = (String) particleMap.get("particle");
                    if (particleName == null) continue;

                    Particle particle = Particle.valueOf(particleName.toUpperCase());
                    int count = particleMap.containsKey("count") ?
                            ((Number) particleMap.get("count")).intValue() : 10;
                    double offsetX = particleMap.containsKey("offset_x") ?
                            ((Number) particleMap.get("offset_x")).doubleValue() : 0.5;
                    double offsetY = particleMap.containsKey("offset_y") ?
                            ((Number) particleMap.get("offset_y")).doubleValue() : 0.5;
                    double offsetZ = particleMap.containsKey("offset_z") ?
                            ((Number) particleMap.get("offset_z")).doubleValue() : 0.5;
                    double speed = particleMap.containsKey("speed") ?
                            ((Number) particleMap.get("speed")).doubleValue() : 0.1;

                    particles.add(new CraftEventPreset.ParticleEvent(particle, count, offsetX, offsetY, offsetZ, speed));
                } catch (Exception e) {
                    plugin.getLogger().warning("Invalid particle in preset '" + name + "': " + e.getMessage());
                }
            }
        }

        List<CraftEventPreset.CommandEvent> commands = new ArrayList<>();
        if (section.contains("commands")) {
            List<Map<?, ?>> commandsList = section.getMapList("commands");
            for (Map<?, ?> commandMap : commandsList) {
                try {
                    String command = (String) commandMap.get("command");
                    if (command == null || command.isEmpty()) continue;

                    Object typeObj = commandMap.get("type");
                    String typeStr = typeObj != null ? typeObj.toString() : "CONSOLE";
                    CraftEventPreset.CommandEvent.CommandType type;
                    try {
                        type = CraftEventPreset.CommandEvent.CommandType.valueOf(typeStr.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        type = CraftEventPreset.CommandEvent.CommandType.CONSOLE;
                    }

                    commands.add(new CraftEventPreset.CommandEvent(command, type));
                } catch (Exception e) {
                    plugin.getLogger().warning("Invalid command in preset '" + name + "': " + e.getMessage());
                }
            }
        }

        return new CraftEventPreset(name, sounds, particles, commands, scope);
    }

    public void savePresets() {
        try {
            YamlConfiguration config = new YamlConfiguration();

            for (CraftEventPreset preset : presets.values()) {
                savePreset(config, preset);
            }

            config.save(presetsFile);
            plugin.debug("Saved " + presets.size() + " craft event presets");
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save craft event presets: " + e.getMessage());
        }
    }

    private void savePreset(YamlConfiguration config, CraftEventPreset preset) {
        String path = preset.getName();

        config.set(path + ".scope", preset.getScope().name());

        if (!preset.getSounds().isEmpty()) {
            List<Map<String, Object>> soundsList = new ArrayList<>();
            for (CraftEventPreset.SoundEvent sound : preset.getSounds()) {
                Map<String, Object> soundMap = new LinkedHashMap<>();
                soundMap.put("sound", sound.getSound().name());
                soundMap.put("volume", sound.getVolume());
                soundMap.put("pitch", sound.getPitch());
                soundsList.add(soundMap);
            }
            config.set(path + ".sounds", soundsList);
        }

        if (!preset.getParticles().isEmpty()) {
            List<Map<String, Object>> particlesList = new ArrayList<>();
            for (CraftEventPreset.ParticleEvent particle : preset.getParticles()) {
                Map<String, Object> particleMap = new LinkedHashMap<>();
                particleMap.put("particle", particle.getParticle().name());
                particleMap.put("count", particle.getCount());
                particleMap.put("offset_x", particle.getOffsetX());
                particleMap.put("offset_y", particle.getOffsetY());
                particleMap.put("offset_z", particle.getOffsetZ());
                particleMap.put("speed", particle.getSpeed());
                particlesList.add(particleMap);
            }
            config.set(path + ".particles", particlesList);
        }

        if (!preset.getCommands().isEmpty()) {
            List<Map<String, Object>> commandsList = new ArrayList<>();
            for (CraftEventPreset.CommandEvent command : preset.getCommands()) {
                Map<String, Object> commandMap = new LinkedHashMap<>();
                commandMap.put("command", command.getCommand());
                commandMap.put("type", command.getType().name());
                commandsList.add(commandMap);
            }
            config.set(path + ".commands", commandsList);
        }
    }

    public CraftEventPreset getPreset(String name) {
        return presets.get(name.toLowerCase());
    }

    public Collection<CraftEventPreset> getAllPresets() {
        return Collections.unmodifiableCollection(presets.values());
    }

    public List<CraftEventPreset> getPresetsForScope(boolean isCustomRecipe) {
        List<CraftEventPreset> result = new ArrayList<>();
        for (CraftEventPreset preset : presets.values()) {
            if (preset.canApplyTo(isCustomRecipe)) {
                result.add(preset);
            }
        }
        return result;
    }

    public void addPreset(CraftEventPreset preset) {
        presets.put(preset.getName().toLowerCase(), preset);
        savePresets();
    }

    public void removePreset(String name) {
        presets.remove(name.toLowerCase());
        savePresets();
    }

    public boolean hasPreset(String name) {
        return presets.containsKey(name.toLowerCase());
    }

    public int getPresetCount() {
        return presets.size();
    }
}
