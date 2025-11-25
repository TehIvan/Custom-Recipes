package org.hikarii.customrecipes.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.hikarii.customrecipes.CustomRecipes;
import org.hikarii.customrecipes.language.LanguageManager;
import org.hikarii.customrecipes.recipe.CraftEventPreset;
import org.hikarii.customrecipes.util.MessageUtil;

import java.util.ArrayList;
import java.util.List;

public class ParticleAdjustGUI implements Listener, GUIBase {
    private final CustomRecipes plugin;
    private final Player player;
    private final Inventory inventory;
    private final LanguageManager lang;
    private final CraftEventPreset preset;
    private final CraftEventPreset.ParticleEvent particleEvent;
    private final ParticleSelectorGUI parentGUI;

    private int count;
    private double offsetX;
    private double offsetY;
    private double offsetZ;
    private double speed;

    private static final int PARTICLE_INFO_SLOT = 4;

    private static final int COUNT_DECREASE_SLOT = 10;
    private static final int COUNT_DISPLAY_SLOT = 11;
    private static final int COUNT_INCREASE_SLOT = 12;

    private static final int SPEED_DECREASE_SLOT = 14;
    private static final int SPEED_DISPLAY_SLOT = 15;
    private static final int SPEED_INCREASE_SLOT = 16;

    private static final int OFFSET_X_DEC_SLOT = 19;
    private static final int OFFSET_X_DISP_SLOT = 20;
    private static final int OFFSET_X_INC_SLOT = 21;

    private static final int OFFSET_Y_DEC_SLOT = 23;
    private static final int OFFSET_Y_DISP_SLOT = 24;
    private static final int OFFSET_Y_INC_SLOT = 25;

    private static final int OFFSET_Z_DEC_SLOT = 28;
    private static final int OFFSET_Z_DISP_SLOT = 29;
    private static final int OFFSET_Z_INC_SLOT = 30;

    private static final int PREVIEW_SLOT = 32;
    private static final int SAVE_SLOT = 40;
    private static final int BACK_SLOT = 36;

    public ParticleAdjustGUI(CustomRecipes plugin, Player player, CraftEventPreset preset,
                             CraftEventPreset.ParticleEvent particleEvent, ParticleSelectorGUI parentGUI) {
        this.plugin = plugin;
        this.player = player;
        this.preset = preset;
        this.particleEvent = particleEvent;
        this.parentGUI = parentGUI;
        this.lang = plugin.getLanguageManager();
        this.count = particleEvent.getCount();
        this.offsetX = particleEvent.getOffsetX();
        this.offsetY = particleEvent.getOffsetY();
        this.offsetZ = particleEvent.getOffsetZ();
        this.speed = particleEvent.getSpeed();
        this.inventory = Bukkit.createInventory(
                null,
                45,
                MessageUtil.createGradientMenuTitle(lang.getMessage("craft_events.adjust_particle_title"))
        );
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        updateInventory();
    }

    public void open() {
        player.openInventory(inventory);
    }

    private void updateInventory() {
        inventory.clear();
        fillBorders();
        addParticleInfo();
        addCountControls();
        addSpeedControls();
        addOffsetXControls();
        addOffsetYControls();
        addOffsetZControls();
        addPreviewButton();
        addSaveButton();
        addBackButton();
    }

    private void fillBorders() {
        ItemStack borderPane = new ItemStack(Material.PINK_STAINED_GLASS_PANE);
        ItemMeta meta = borderPane.getItemMeta();
        meta.displayName(Component.empty());
        borderPane.setItemMeta(meta);

        for (int i = 0; i < 45; i++) {
            inventory.setItem(i, borderPane);
        }
    }

    private void addParticleInfo() {
        ItemStack item = new ItemStack(Material.BLAZE_POWDER);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(formatParticleName(particleEvent.getParticle()), NamedTextColor.GOLD)
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text(particleEvent.getParticle().name(), NamedTextColor.DARK_GRAY)
                .decoration(TextDecoration.ITALIC, false));

        meta.lore(lore);
        item.setItemMeta(meta);
        inventory.setItem(PARTICLE_INFO_SLOT, item);
    }

    private void addCountControls() {
        
        ItemStack decrease = new ItemStack(Material.RED_CONCRETE);
        ItemMeta decMeta = decrease.getItemMeta();
        decMeta.displayName(Component.text("-1 " + lang.getMessage("craft_events.count"), NamedTextColor.RED)
                .decoration(TextDecoration.ITALIC, false));
        List<Component> decLore = new ArrayList<>();
        decLore.add(Component.text(lang.getMessage("craft_events.shift_for_more").replace("±0.5", "±10"), NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        decMeta.lore(decLore);
        decrease.setItemMeta(decMeta);
        inventory.setItem(COUNT_DECREASE_SLOT, decrease);

        int displayAmount = Math.max(1, Math.min(64, count));
        ItemStack display = new ItemStack(Material.NETHER_STAR, displayAmount);
        ItemMeta dispMeta = display.getItemMeta();
        dispMeta.displayName(Component.text(lang.getMessage("craft_events.count"), NamedTextColor.AQUA)
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false));
        List<Component> dispLore = new ArrayList<>();
        dispLore.add(Component.empty());
        dispLore.add(Component.text(lang.getMessage("info.current") + ": ", NamedTextColor.GRAY)
                .append(Component.text(count, NamedTextColor.YELLOW))
                .decoration(TextDecoration.ITALIC, false));
        dispMeta.lore(dispLore);
        display.setItemMeta(dispMeta);
        inventory.setItem(COUNT_DISPLAY_SLOT, display);

        ItemStack increase = new ItemStack(Material.LIME_CONCRETE);
        ItemMeta incMeta = increase.getItemMeta();
        incMeta.displayName(Component.text("+1 " + lang.getMessage("craft_events.count"), NamedTextColor.GREEN)
                .decoration(TextDecoration.ITALIC, false));
        List<Component> incLore = new ArrayList<>();
        incLore.add(Component.text(lang.getMessage("craft_events.shift_for_more").replace("±0.5", "±10"), NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        incMeta.lore(incLore);
        increase.setItemMeta(incMeta);
        inventory.setItem(COUNT_INCREASE_SLOT, increase);
    }

    private void addSpeedControls() {
        
        ItemStack decrease = new ItemStack(Material.RED_CONCRETE);
        ItemMeta decMeta = decrease.getItemMeta();
        decMeta.displayName(Component.text("-0.05 " + lang.getMessage("craft_events.speed"), NamedTextColor.RED)
                .decoration(TextDecoration.ITALIC, false));
        decrease.setItemMeta(decMeta);
        inventory.setItem(SPEED_DECREASE_SLOT, decrease);

        ItemStack display = new ItemStack(Material.FEATHER);
        ItemMeta dispMeta = display.getItemMeta();
        dispMeta.displayName(Component.text(lang.getMessage("craft_events.speed"), NamedTextColor.LIGHT_PURPLE)
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false));
        List<Component> dispLore = new ArrayList<>();
        dispLore.add(Component.empty());
        dispLore.add(Component.text(lang.getMessage("info.current") + ": ", NamedTextColor.GRAY)
                .append(Component.text(String.format("%.2f", speed), NamedTextColor.YELLOW))
                .decoration(TextDecoration.ITALIC, false));
        dispMeta.lore(dispLore);
        display.setItemMeta(dispMeta);
        inventory.setItem(SPEED_DISPLAY_SLOT, display);

        ItemStack increase = new ItemStack(Material.LIME_CONCRETE);
        ItemMeta incMeta = increase.getItemMeta();
        incMeta.displayName(Component.text("+0.05 " + lang.getMessage("craft_events.speed"), NamedTextColor.GREEN)
                .decoration(TextDecoration.ITALIC, false));
        increase.setItemMeta(incMeta);
        inventory.setItem(SPEED_INCREASE_SLOT, increase);
    }

    private void addOffsetXControls() {
        addOffsetControl("X", offsetX, OFFSET_X_DEC_SLOT, OFFSET_X_DISP_SLOT, OFFSET_X_INC_SLOT, Material.RED_WOOL);
    }

    private void addOffsetYControls() {
        addOffsetControl("Y", offsetY, OFFSET_Y_DEC_SLOT, OFFSET_Y_DISP_SLOT, OFFSET_Y_INC_SLOT, Material.GREEN_WOOL);
    }

    private void addOffsetZControls() {
        addOffsetControl("Z", offsetZ, OFFSET_Z_DEC_SLOT, OFFSET_Z_DISP_SLOT, OFFSET_Z_INC_SLOT, Material.BLUE_WOOL);
    }

    private void addOffsetControl(String axis, double value, int decSlot, int dispSlot, int incSlot, Material displayMat) {
        
        ItemStack decrease = new ItemStack(Material.RED_CONCRETE);
        ItemMeta decMeta = decrease.getItemMeta();
        decMeta.displayName(Component.text("-0.1 " + lang.getMessage("craft_events.offset_" + axis.toLowerCase()), NamedTextColor.RED)
                .decoration(TextDecoration.ITALIC, false));
        decrease.setItemMeta(decMeta);
        inventory.setItem(decSlot, decrease);

        ItemStack display = new ItemStack(displayMat);
        ItemMeta dispMeta = display.getItemMeta();
        dispMeta.displayName(Component.text(lang.getMessage("craft_events.offset_" + axis.toLowerCase()), NamedTextColor.GOLD)
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false));
        List<Component> dispLore = new ArrayList<>();
        dispLore.add(Component.empty());
        dispLore.add(Component.text(lang.getMessage("info.current") + ": ", NamedTextColor.GRAY)
                .append(Component.text(String.format("%.1f", value), NamedTextColor.YELLOW))
                .decoration(TextDecoration.ITALIC, false));
        dispMeta.lore(dispLore);
        display.setItemMeta(dispMeta);
        inventory.setItem(dispSlot, display);

        ItemStack increase = new ItemStack(Material.LIME_CONCRETE);
        ItemMeta incMeta = increase.getItemMeta();
        incMeta.displayName(Component.text("+0.1 " + lang.getMessage("craft_events.offset_" + axis.toLowerCase()), NamedTextColor.GREEN)
                .decoration(TextDecoration.ITALIC, false));
        increase.setItemMeta(incMeta);
        inventory.setItem(incSlot, increase);
    }

    private void addPreviewButton() {
        ItemStack button = new ItemStack(Material.ENDER_EYE);
        ItemMeta meta = button.getItemMeta();
        meta.displayName(Component.text(lang.getMessage("craft_events.preview_particle"), NamedTextColor.GREEN)
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text("» " + lang.getMessage("lore.click_to_preview"), NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, true));

        meta.lore(lore);
        button.setItemMeta(meta);
        inventory.setItem(PREVIEW_SLOT, button);
    }

    private void addSaveButton() {
        ItemStack button = new ItemStack(Material.LIME_WOOL);
        ItemMeta meta = button.getItemMeta();
        meta.displayName(Component.text(lang.getMessage("button.save_and_return"), NamedTextColor.GREEN)
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false));
        button.setItemMeta(meta);
        inventory.setItem(SAVE_SLOT, button);
    }

    private void addBackButton() {
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta meta = back.getItemMeta();
        meta.displayName(Component.text(lang.getMessage("button.back"), NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));
        back.setItemMeta(meta);
        inventory.setItem(BACK_SLOT, back);
    }

    private String formatParticleName(Particle particle) {
        String name = particle.name().toLowerCase().replace("_", " ");
        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = true;
        for (char c : name.toCharArray()) {
            if (c == ' ') {
                result.append(c);
                capitalizeNext = true;
            } else if (capitalizeNext) {
                result.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getInventory().equals(inventory)) {
            return;
        }

        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player clicker) || !clicker.equals(player)) {
            return;
        }

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) {
            return;
        }

        int slot = event.getSlot();
        ClickType clickType = event.getClick();
        int countStep = clickType.isShiftClick() ? 10 : 1;
        double offsetStep = clickType.isShiftClick() ? 0.5 : 0.1;

        switch (slot) {
            case BACK_SLOT -> parentGUI.refreshAndOpen();
            case SAVE_SLOT -> {
                
                preset.removeParticle(particleEvent.getParticle());
                CraftEventPreset.ParticleEvent newEvent = new CraftEventPreset.ParticleEvent(
                        particleEvent.getParticle(), count, offsetX, offsetY, offsetZ, speed
                );
                preset.addParticle(newEvent);
                MessageUtil.sendInfo(player, lang.getMessage("craft_events.particle_adjusted"));
                parentGUI.refreshAndOpen();
            }
            case PREVIEW_SLOT -> player.getWorld().spawnParticle(
                    particleEvent.getParticle(),
                    player.getLocation().add(0, 1, 0),
                    count, offsetX, offsetY, offsetZ, speed
            );
            case COUNT_DECREASE_SLOT -> {
                count = Math.max(1, count - countStep);
                updateInventory();
            }
            case COUNT_INCREASE_SLOT -> {
                count = Math.min(500, count + countStep);
                updateInventory();
            }
            case SPEED_DECREASE_SLOT -> {
                speed = Math.max(0, speed - 0.05);
                updateInventory();
            }
            case SPEED_INCREASE_SLOT -> {
                speed = Math.min(5.0, speed + 0.05);
                updateInventory();
            }
            case OFFSET_X_DEC_SLOT -> {
                offsetX = Math.max(0, offsetX - offsetStep);
                updateInventory();
            }
            case OFFSET_X_INC_SLOT -> {
                offsetX = Math.min(5.0, offsetX + offsetStep);
                updateInventory();
            }
            case OFFSET_Y_DEC_SLOT -> {
                offsetY = Math.max(0, offsetY - offsetStep);
                updateInventory();
            }
            case OFFSET_Y_INC_SLOT -> {
                offsetY = Math.min(5.0, offsetY + offsetStep);
                updateInventory();
            }
            case OFFSET_Z_DEC_SLOT -> {
                offsetZ = Math.max(0, offsetZ - offsetStep);
                updateInventory();
            }
            case OFFSET_Z_INC_SLOT -> {
                offsetZ = Math.min(5.0, offsetZ + offsetStep);
                updateInventory();
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().equals(inventory) && event.getPlayer().equals(player)) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!inventory.equals(player.getOpenInventory().getTopInventory())) {
                    InventoryClickEvent.getHandlerList().unregister(this);
                    InventoryCloseEvent.getHandlerList().unregister(this);
                }
            }, 1L);
        }
    }

    public void refreshAndOpen() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        player.openInventory(inventory);
    }
}
