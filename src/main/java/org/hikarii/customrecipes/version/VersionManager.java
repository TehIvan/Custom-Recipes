package org.hikarii.customrecipes.version;

import org.bukkit.Bukkit;

public class VersionManager {
    private final MinecraftVersion version;

    public VersionManager() {
        this.version = detectVersion();
    }

    private MinecraftVersion detectVersion() {
        String versionString = Bukkit.getVersion();

        if (versionString.contains("1.18")) {
            return MinecraftVersion.v1_18;
        } else if (versionString.contains("1.19")) {
            return MinecraftVersion.v1_19;
        } else if (versionString.contains("1.20")) {
            return MinecraftVersion.v1_20;
        } else if (versionString.contains("1.21")) {
            return MinecraftVersion.v1_21;
        } else {
            
            return MinecraftVersion.v1_21;
        }
    }

    public MinecraftVersion getVersion() {
        return version;
    }

    public boolean isAtLeast(MinecraftVersion targetVersion) {
        return version.ordinal() >= targetVersion.ordinal();
    }

    public boolean isBelow(MinecraftVersion targetVersion) {
        return version.ordinal() < targetVersion.ordinal();
    }

    public String getVersionString() {
        return version.name().replace("v", "").replace("_", ".");
    }

    public enum MinecraftVersion {
        v1_18,
        v1_19,
        v1_20,
        v1_21
    }
}
