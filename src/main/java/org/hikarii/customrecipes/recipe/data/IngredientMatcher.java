package org.hikarii.customrecipes.recipe.data;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;

import java.util.Map;
import java.util.Objects;

public class IngredientMatcher {
    public static boolean matches(ItemStack required, ItemStack actual, boolean ignoreMetadata) {
        if (actual == null || actual.getType() == Material.AIR) {
            return required == null || required.getType() == Material.AIR;
        }

        if (required == null || required.getType() == Material.AIR) {
            return false;
        }

        if (required.getType() != actual.getType()) {
            return false;
        }

        if (ignoreMetadata) {
            return true;
        }

        ItemMeta requiredMeta = required.getItemMeta();
        ItemMeta actualMeta = actual.getItemMeta();

        if (requiredMeta == null) {
            return true; 
        }

        if (actualMeta == null) {
            
            return !hasAnyRequirement(requiredMeta);
        }

        if (requiredMeta.hasDisplayName()) {
            if (!actualMeta.hasDisplayName()) {
                return false;
            }
            if (!Objects.equals(requiredMeta.displayName(), actualMeta.displayName())) {
                return false;
            }
        }

        if (requiredMeta.hasLore()) {
            if (!actualMeta.hasLore()) {
                return false;
            }
            if (!Objects.equals(requiredMeta.lore(), actualMeta.lore())) {
                return false;
            }
        }

        if (requiredMeta.hasEnchants()) {
            Map<Enchantment, Integer> requiredEnchants = requiredMeta.getEnchants();
            Map<Enchantment, Integer> actualEnchants = actualMeta.getEnchants();

            for (Map.Entry<Enchantment, Integer> entry : requiredEnchants.entrySet()) {
                if (!actualEnchants.containsKey(entry.getKey()) ||
                        actualEnchants.get(entry.getKey()) < entry.getValue()) {
                    return false;
                }
            }
        }

        if (requiredMeta instanceof Damageable requiredDamageable && requiredDamageable.hasDamage()) {
            if (!(actualMeta instanceof Damageable actualDamageable)) {
                return false;
            }
            
            if (actualDamageable.getDamage() < requiredDamageable.getDamage()) {
                return false;
            }
        }

        if (requiredMeta.hasCustomModelData()) {
            if (!actualMeta.hasCustomModelData()) {
                return false;
            }
            if (requiredMeta.getCustomModelData() != actualMeta.getCustomModelData()) {
                return false;
            }
        }

        if (requiredMeta.isUnbreakable()) {
            if (!actualMeta.isUnbreakable()) {
                return false;
            }
        }

        if (!requiredMeta.getItemFlags().isEmpty()) {
            for (ItemFlag flag : requiredMeta.getItemFlags()) {
                if (!actualMeta.hasItemFlag(flag)) {
                    return false;
                }
            }
        }

        if (!requiredMeta.getPersistentDataContainer().getKeys().isEmpty()) {
            for (NamespacedKey key : requiredMeta.getPersistentDataContainer().getKeys()) {
                String requiredValue = requiredMeta.getPersistentDataContainer().get(key, PersistentDataType.STRING);
                String actualValue = actualMeta.getPersistentDataContainer().get(key, PersistentDataType.STRING);

                if (!Objects.equals(requiredValue, actualValue)) {
                    return false;
                }
            }
        }

        return true;
    }

    private static boolean hasAnyRequirement(ItemMeta meta) {
        if (meta.hasDisplayName()) return true;
        if (meta.hasLore()) return true;
        if (meta.hasEnchants()) return true;
        if (meta.hasCustomModelData()) return true;
        if (meta.isUnbreakable()) return true;
        if (!meta.getItemFlags().isEmpty()) return true;
        if (!meta.getPersistentDataContainer().getKeys().isEmpty()) return true;
        if (meta instanceof Damageable damageable && damageable.hasDamage()) return true;
        return false;
    }
}