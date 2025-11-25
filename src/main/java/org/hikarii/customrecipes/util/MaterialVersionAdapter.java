package org.hikarii.customrecipes.util;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MaterialVersionAdapter {
    private static final int minorVersion;
    private static final Map<String, String> materialMappings = new HashMap<>();

    static {
        
        String version = Bukkit.getVersion();
        Pattern pattern = Pattern.compile("MC: (\\d+)\\.(\\d+)");
        Matcher matcher = pattern.matcher(version);

        int detectedMinor = 18; 
        if (matcher.find()) {
            detectedMinor = Integer.parseInt(matcher.group(2));
        }
        minorVersion = detectedMinor;

        initializeMaterialMappings();
    }

    private static void initializeMaterialMappings() {

        if (minorVersion >= 21) {
            
            materialMappings.put("SCUTE", "TURTLE_SCUTE");
        } else {
            
            materialMappings.put("TURTLE_SCUTE", "SCUTE");
        }

    }

    public static String adaptMaterialName(String materialName) {
        if (materialName == null || materialName.isEmpty()) {
            return materialName;
        }

        String mapped = materialMappings.get(materialName);
        if (mapped != null) {
            return mapped;
        }

        return materialName;
    }

    public static Material adaptMaterial(String materialName) {
        String adaptedName = adaptMaterialName(materialName);
        Material material = Material.getMaterial(adaptedName);

        if (material == null) {
            material = Material.getMaterial(materialName);
        }

        return material;
    }

    public static String adaptIngredientString(String ingredientString) {
        if (ingredientString == null || ingredientString.isEmpty() || ingredientString.equals("AIR")) {
            return ingredientString;
        }

        if (ingredientString.contains("|")) {
            String[] options = ingredientString.split("\\|");
            StringBuilder validOptions = new StringBuilder();

            for (String option : options) {
                String adapted = adaptMaterialName(option);
                Material mat = Material.getMaterial(adapted);

                if (mat != null && mat != Material.AIR) {
                    if (validOptions.length() > 0) {
                        validOptions.append("|");
                    }
                    validOptions.append(adapted);
                }
            }

            return validOptions.length() > 0 ? validOptions.toString() : ingredientString;
        }

        return adaptMaterialName(ingredientString);
    }

    public static int getMinorVersion() {
        return minorVersion;
    }

    public static boolean materialExists(String materialName) {
        return Material.getMaterial(materialName) != null;
    }
}
