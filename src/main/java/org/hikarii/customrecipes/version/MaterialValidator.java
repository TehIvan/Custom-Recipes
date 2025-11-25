package org.hikarii.customrecipes.version;

import org.bukkit.Material;
import org.hikarii.customrecipes.CustomRecipes;
import org.hikarii.customrecipes.version.VersionManager.MinecraftVersion;

import java.util.*;

public class MaterialValidator {
    private final CustomRecipes plugin;
    private final VersionManager versionManager;
    private static final Map<String, MinecraftVersion> MATERIAL_VERSION_MAP = new HashMap<>();

    static {
        
        MATERIAL_VERSION_MAP.put("MANGROVE_LOG", MinecraftVersion.v1_19);
        MATERIAL_VERSION_MAP.put("MANGROVE_WOOD", MinecraftVersion.v1_19);
        MATERIAL_VERSION_MAP.put("MANGROVE_PLANKS", MinecraftVersion.v1_19);
        MATERIAL_VERSION_MAP.put("MANGROVE_LEAVES", MinecraftVersion.v1_19);
        MATERIAL_VERSION_MAP.put("MANGROVE_PROPAGULE", MinecraftVersion.v1_19);
        MATERIAL_VERSION_MAP.put("MANGROVE_ROOTS", MinecraftVersion.v1_19);
        MATERIAL_VERSION_MAP.put("MUDDY_MANGROVE_ROOTS", MinecraftVersion.v1_19);
        MATERIAL_VERSION_MAP.put("MUD", MinecraftVersion.v1_19);
        MATERIAL_VERSION_MAP.put("MUD_BRICKS", MinecraftVersion.v1_19);
        MATERIAL_VERSION_MAP.put("PACKED_MUD", MinecraftVersion.v1_19);
        MATERIAL_VERSION_MAP.put("FROG_SPAWN", MinecraftVersion.v1_19);
        MATERIAL_VERSION_MAP.put("OCHRE_FROGLIGHT", MinecraftVersion.v1_19);
        MATERIAL_VERSION_MAP.put("PEARLESCENT_FROGLIGHT", MinecraftVersion.v1_19);
        MATERIAL_VERSION_MAP.put("VERDANT_FROGLIGHT", MinecraftVersion.v1_19);
        MATERIAL_VERSION_MAP.put("SCULK", MinecraftVersion.v1_19);
        MATERIAL_VERSION_MAP.put("SCULK_VEIN", MinecraftVersion.v1_19);
        MATERIAL_VERSION_MAP.put("SCULK_CATALYST", MinecraftVersion.v1_19);
        MATERIAL_VERSION_MAP.put("SCULK_SHRIEKER", MinecraftVersion.v1_19);
        MATERIAL_VERSION_MAP.put("SCULK_SENSOR", MinecraftVersion.v1_19);
        MATERIAL_VERSION_MAP.put("REINFORCED_DEEPSLATE", MinecraftVersion.v1_19);
        MATERIAL_VERSION_MAP.put("RECOVERY_COMPASS", MinecraftVersion.v1_19);
        MATERIAL_VERSION_MAP.put("ECHO_SHARD", MinecraftVersion.v1_19);

        MATERIAL_VERSION_MAP.put("CHERRY_LOG", MinecraftVersion.v1_20);
        MATERIAL_VERSION_MAP.put("CHERRY_WOOD", MinecraftVersion.v1_20);
        MATERIAL_VERSION_MAP.put("CHERRY_PLANKS", MinecraftVersion.v1_20);
        MATERIAL_VERSION_MAP.put("CHERRY_LEAVES", MinecraftVersion.v1_20);
        MATERIAL_VERSION_MAP.put("CHERRY_SAPLING", MinecraftVersion.v1_20);
        MATERIAL_VERSION_MAP.put("BAMBOO_MOSAIC", MinecraftVersion.v1_20);
        MATERIAL_VERSION_MAP.put("BAMBOO_PLANKS", MinecraftVersion.v1_20);
        MATERIAL_VERSION_MAP.put("BAMBOO_BLOCK", MinecraftVersion.v1_20);
        MATERIAL_VERSION_MAP.put("STRIPPED_BAMBOO_BLOCK", MinecraftVersion.v1_20);
        MATERIAL_VERSION_MAP.put("CHISELED_BOOKSHELF", MinecraftVersion.v1_20);
        MATERIAL_VERSION_MAP.put("DECORATED_POT", MinecraftVersion.v1_20);
        MATERIAL_VERSION_MAP.put("SUSPICIOUS_SAND", MinecraftVersion.v1_20);
        MATERIAL_VERSION_MAP.put("SUSPICIOUS_GRAVEL", MinecraftVersion.v1_20);
        MATERIAL_VERSION_MAP.put("TORCHFLOWER", MinecraftVersion.v1_20);
        MATERIAL_VERSION_MAP.put("TORCHFLOWER_SEEDS", MinecraftVersion.v1_20);
        MATERIAL_VERSION_MAP.put("PITCHER_PLANT", MinecraftVersion.v1_20);
        MATERIAL_VERSION_MAP.put("PITCHER_POD", MinecraftVersion.v1_20);
        MATERIAL_VERSION_MAP.put("SNIFFER_EGG", MinecraftVersion.v1_20);
        MATERIAL_VERSION_MAP.put("BRUSH", MinecraftVersion.v1_20);
        MATERIAL_VERSION_MAP.put("NETHERITE_UPGRADE_SMITHING_TEMPLATE", MinecraftVersion.v1_20);
        MATERIAL_VERSION_MAP.put("CALIBRATED_SCULK_SENSOR", MinecraftVersion.v1_20);
        MATERIAL_VERSION_MAP.put("PINK_PETALS", MinecraftVersion.v1_20);

        MATERIAL_VERSION_MAP.put("CRAFTER", MinecraftVersion.v1_21);
        MATERIAL_VERSION_MAP.put("TRIAL_SPAWNER", MinecraftVersion.v1_21);
        MATERIAL_VERSION_MAP.put("TRIAL_KEY", MinecraftVersion.v1_21);
        MATERIAL_VERSION_MAP.put("OMINOUS_TRIAL_KEY", MinecraftVersion.v1_21);
        MATERIAL_VERSION_MAP.put("VAULT", MinecraftVersion.v1_21);
        MATERIAL_VERSION_MAP.put("OMINOUS_BOTTLE", MinecraftVersion.v1_21);
        MATERIAL_VERSION_MAP.put("BREEZE_ROD", MinecraftVersion.v1_21);
        MATERIAL_VERSION_MAP.put("HEAVY_CORE", MinecraftVersion.v1_21);
        MATERIAL_VERSION_MAP.put("MACE", MinecraftVersion.v1_21);
        MATERIAL_VERSION_MAP.put("COPPER_GRATE", MinecraftVersion.v1_21);
        MATERIAL_VERSION_MAP.put("EXPOSED_COPPER_GRATE", MinecraftVersion.v1_21);
        MATERIAL_VERSION_MAP.put("WEATHERED_COPPER_GRATE", MinecraftVersion.v1_21);
        MATERIAL_VERSION_MAP.put("OXIDIZED_COPPER_GRATE", MinecraftVersion.v1_21);
        MATERIAL_VERSION_MAP.put("WAXED_COPPER_GRATE", MinecraftVersion.v1_21);
        MATERIAL_VERSION_MAP.put("WAXED_EXPOSED_COPPER_GRATE", MinecraftVersion.v1_21);
        MATERIAL_VERSION_MAP.put("WAXED_WEATHERED_COPPER_GRATE", MinecraftVersion.v1_21);
        MATERIAL_VERSION_MAP.put("WAXED_OXIDIZED_COPPER_GRATE", MinecraftVersion.v1_21);
        MATERIAL_VERSION_MAP.put("COPPER_BULB", MinecraftVersion.v1_21);
        MATERIAL_VERSION_MAP.put("EXPOSED_COPPER_BULB", MinecraftVersion.v1_21);
        MATERIAL_VERSION_MAP.put("WEATHERED_COPPER_BULB", MinecraftVersion.v1_21);
        MATERIAL_VERSION_MAP.put("OXIDIZED_COPPER_BULB", MinecraftVersion.v1_21);
        MATERIAL_VERSION_MAP.put("WAXED_COPPER_BULB", MinecraftVersion.v1_21);
        MATERIAL_VERSION_MAP.put("WAXED_EXPOSED_COPPER_BULB", MinecraftVersion.v1_21);
        MATERIAL_VERSION_MAP.put("WAXED_WEATHERED_COPPER_BULB", MinecraftVersion.v1_21);
        MATERIAL_VERSION_MAP.put("WAXED_OXIDIZED_COPPER_BULB", MinecraftVersion.v1_21);
        MATERIAL_VERSION_MAP.put("COPPER_DOOR", MinecraftVersion.v1_21);
        MATERIAL_VERSION_MAP.put("EXPOSED_COPPER_DOOR", MinecraftVersion.v1_21);
        MATERIAL_VERSION_MAP.put("WEATHERED_COPPER_DOOR", MinecraftVersion.v1_21);
        MATERIAL_VERSION_MAP.put("OXIDIZED_COPPER_DOOR", MinecraftVersion.v1_21);
        MATERIAL_VERSION_MAP.put("WAXED_COPPER_DOOR", MinecraftVersion.v1_21);
        MATERIAL_VERSION_MAP.put("WAXED_EXPOSED_COPPER_DOOR", MinecraftVersion.v1_21);
        MATERIAL_VERSION_MAP.put("WAXED_WEATHERED_COPPER_DOOR", MinecraftVersion.v1_21);
        MATERIAL_VERSION_MAP.put("WAXED_OXIDIZED_COPPER_DOOR", MinecraftVersion.v1_21);
        MATERIAL_VERSION_MAP.put("COPPER_TRAPDOOR", MinecraftVersion.v1_21);
        MATERIAL_VERSION_MAP.put("EXPOSED_COPPER_TRAPDOOR", MinecraftVersion.v1_21);
        MATERIAL_VERSION_MAP.put("WEATHERED_COPPER_TRAPDOOR", MinecraftVersion.v1_21);
        MATERIAL_VERSION_MAP.put("OXIDIZED_COPPER_TRAPDOOR", MinecraftVersion.v1_21);
        MATERIAL_VERSION_MAP.put("WAXED_COPPER_TRAPDOOR", MinecraftVersion.v1_21);
        MATERIAL_VERSION_MAP.put("WAXED_EXPOSED_COPPER_TRAPDOOR", MinecraftVersion.v1_21);
        MATERIAL_VERSION_MAP.put("WAXED_WEATHERED_COPPER_TRAPDOOR", MinecraftVersion.v1_21);
        MATERIAL_VERSION_MAP.put("WAXED_OXIDIZED_COPPER_TRAPDOOR", MinecraftVersion.v1_21);
    }

    public MaterialValidator(CustomRecipes plugin, VersionManager versionManager) {
        this.plugin = plugin;
        this.versionManager = versionManager;
    }

    public ValidationResult validateMaterial(Material material) {
        if (material == null) {
            return new ValidationResult(false, "Material is null");
        }

        String materialName = material.name();
        MinecraftVersion requiredVersion = MATERIAL_VERSION_MAP.get(materialName);

        if (requiredVersion == null) {
            
            return new ValidationResult(true, null);
        }

        MinecraftVersion currentVersion = versionManager.getVersion();
        if (currentVersion.ordinal() >= requiredVersion.ordinal()) {
            return new ValidationResult(true, null);
        }

        String message = String.format(
            "Material '%s' requires Minecraft %s but server is running %s",
            materialName,
            requiredVersion.name().replace("v", "").replace("_", "."),
            currentVersion.name().replace("v", "").replace("_", ".")
        );

        return new ValidationResult(false, message);
    }

    public ValidationResult validateRecipe(String recipeKey, List<Material> materials) {
        List<String> warnings = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        for (Material material : materials) {
            if (material == null || material == Material.AIR) {
                continue;
            }

            ValidationResult result = validateMaterial(material);
            if (!result.isValid()) {
                errors.add(result.getMessage());
            }
        }

        if (!errors.isEmpty()) {
            return new ValidationResult(false,
                String.format("Recipe '%s' contains incompatible materials: %s",
                    recipeKey, String.join(", ", errors)));
        }

        return new ValidationResult(true, null);
    }

    public void logMaterialWarning(String recipeKey, Material material) {
        ValidationResult result = validateMaterial(material);
        if (!result.isValid()) {
            plugin.getLogger().warning(String.format(
                "[Recipe: %s] %s - Recipe may not work on this server version!",
                recipeKey,
                result.getMessage()
            ));
        }
    }

    public static class ValidationResult {
        private final boolean valid;
        private final String message;

        public ValidationResult(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }

        public boolean isValid() {
            return valid;
        }

        public String getMessage() {
            return message;
        }
    }
}
