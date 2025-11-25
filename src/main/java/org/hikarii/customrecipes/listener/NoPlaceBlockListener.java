package org.hikarii.customrecipes.listener;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.hikarii.customrecipes.CustomRecipes;
import org.hikarii.customrecipes.util.MessageUtil;

public class NoPlaceBlockListener implements Listener {
    private final CustomRecipes plugin;
    private final NamespacedKey noPlaceKey;

    public NoPlaceBlockListener(CustomRecipes plugin) {
        this.plugin = plugin;
        this.noPlaceKey = new NamespacedKey(plugin, "no_place");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();

        if (item == null || !item.hasItemMeta()) {
            return;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }

        PersistentDataContainer container = meta.getPersistentDataContainer();

        if (container.has(noPlaceKey, PersistentDataType.BYTE)) {
            Byte value = container.get(noPlaceKey, PersistentDataType.BYTE);
            if (value != null && value == 1) {
                event.setCancelled(true);

                Player player = event.getPlayer();
                String message = plugin.getLanguageManager().getMessage("flags_selector.flag.no_place_blocked");
                if (!message.equals("flags_selector.flag.no_place_blocked")) {
                    MessageUtil.sendError(player, message);
                } else {
                    MessageUtil.sendError(player, "This block cannot be placed!");
                }

                plugin.debug("Blocked placement of no_place item by " + player.getName());
            }
        }
    }
}
