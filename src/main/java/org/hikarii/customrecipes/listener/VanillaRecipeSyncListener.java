package org.hikarii.customrecipes.listener;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.hikarii.customrecipes.CustomRecipes;

public class VanillaRecipeSyncListener implements Listener {
    private final CustomRecipes plugin;

    public VanillaRecipeSyncListener(CustomRecipes plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                plugin.getVanillaRecipeManager().updateRecipesForPlayer(player);
                plugin.debug("Updated recipes for player: " + player.getName());
            }
        }, 40L); 
    }
}
