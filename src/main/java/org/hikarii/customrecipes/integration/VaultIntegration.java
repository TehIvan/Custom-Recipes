package org.hikarii.customrecipes.integration;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.hikarii.customrecipes.CustomRecipes;

public class VaultIntegration {
    private final CustomRecipes plugin;
    private Object economy = null;
    private boolean enabled = false;

    public VaultIntegration(CustomRecipes plugin) {
        this.plugin = plugin;
        setupEconomy();
    }

    private void setupEconomy() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            plugin.debug("Vault not found, economy features disabled");
            return;
        }

        try {
            Class<?> economyClass = Class.forName("net.milkbowl.vault.economy.Economy");
            RegisteredServiceProvider<?> rsp = Bukkit.getServicesManager().getRegistration(economyClass);
            if (rsp != null) {
                economy = rsp.getProvider();
                enabled = true;
                plugin.getLogger().info("Vault economy integration enabled");
            } else {
                plugin.debug("No economy provider found");
            }
        } catch (ClassNotFoundException e) {
            plugin.debug("Vault Economy class not found");
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to setup Vault: " + e.getMessage());
        }
    }

    public boolean isEnabled() {
        return enabled && economy != null;
    }

    public double getBalance(Player player) {
        if (!isEnabled()) return 0;
        try {
            return (double) economy.getClass()
                    .getMethod("getBalance", org.bukkit.OfflinePlayer.class)
                    .invoke(economy, player);
        } catch (Exception e) {
            plugin.debug("Failed to get balance: " + e.getMessage());
            return 0;
        }
    }

    public boolean has(Player player, double amount) {
        if (!isEnabled()) return true;
        try {
            return (boolean) economy.getClass()
                    .getMethod("has", org.bukkit.OfflinePlayer.class, double.class)
                    .invoke(economy, player, amount);
        } catch (Exception e) {
            plugin.debug("Failed to check balance: " + e.getMessage());
            return true;
        }
    }

    public boolean withdraw(Player player, double amount) {
        if (!isEnabled()) return false;
        try {
            Object response = economy.getClass()
                    .getMethod("withdrawPlayer", org.bukkit.OfflinePlayer.class, double.class)
                    .invoke(economy, player, amount);
            return (boolean) response.getClass().getMethod("transactionSuccess").invoke(response);
        } catch (Exception e) {
            plugin.debug("Failed to withdraw: " + e.getMessage());
            return false;
        }
    }

    public String format(double amount) {
        if (!isEnabled()) return String.format("%.2f", amount);
        try {
            return (String) economy.getClass()
                    .getMethod("format", double.class)
                    .invoke(economy, amount);
        } catch (Exception e) {
            return String.format("%.2f", amount);
        }
    }

    public String getCurrencyName() {
        if (!isEnabled()) return "money";
        try {
            return (String) economy.getClass()
                    .getMethod("currencyNamePlural")
                    .invoke(economy);
        } catch (Exception e) {
            return "money";
        }
    }
}
