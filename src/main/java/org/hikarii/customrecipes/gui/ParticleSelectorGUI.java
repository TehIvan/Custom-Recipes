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
import org.hikarii.customrecipes.util.MaterialVersionAdapter;
import org.hikarii.customrecipes.util.MessageUtil;
import org.hikarii.customrecipes.util.ParticleVersionAdapter;

import java.util.*;

public class ParticleSelectorGUI implements Listener, GUIBase {
    private final CustomRecipes plugin;
    private final Player player;
    private final Inventory inventory;
    private final LanguageManager lang;
    private final CraftEventPreset preset;
    private final CraftEventPresetEditorGUI parentGUI;

    private int currentPage = 0;
    private static final int PARTICLES_PER_PAGE = 28;

    private static final int BACK_SLOT = 45;
    private static final int PREV_PAGE_SLOT = 48;
    private static final int PAGE_INFO_SLOT = 49;
    private static final int NEXT_PAGE_SLOT = 50;
    private static final int CONFIGURED_PARTICLES_SLOT = 53;

    public ParticleSelectorGUI(CustomRecipes plugin, Player player, CraftEventPreset preset, CraftEventPresetEditorGUI parentGUI) {
        this.plugin = plugin;
        this.player = player;
        this.preset = preset;
        this.parentGUI = parentGUI;
        this.lang = plugin.getLanguageManager();
        this.inventory = Bukkit.createInventory(
                null,
                54,
                MessageUtil.createGradientMenuTitle(lang.getMessage("craft_events.particles_title"))
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
        addParticleButtons();
        addNavigationButtons();
        addConfiguredParticlesButton();
        addBackButton();
    }

    private void fillBorders() {
        ItemStack borderPane = new ItemStack(Material.MAGENTA_STAINED_GLASS_PANE);
        ItemMeta meta = borderPane.getItemMeta();
        meta.displayName(Component.empty());
        borderPane.setItemMeta(meta);

        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, borderPane);
        }
        
        for (int i = 45; i < 54; i++) {
            inventory.setItem(i, borderPane);
        }
        
        for (int row = 1; row < 5; row++) {
            inventory.setItem(row * 9, borderPane);
            inventory.setItem(row * 9 + 8, borderPane);
        }
    }

    private void addParticleButtons() {
        List<Particle> particles = getAvailableParticles();
        int start = currentPage * PARTICLES_PER_PAGE;
        int end = Math.min(start + PARTICLES_PER_PAGE, particles.size());

        int[] slots = getParticleSlots();
        int slotIndex = 0;

        for (int i = start; i < end && slotIndex < slots.length; i++, slotIndex++) {
            Particle particle = particles.get(i);
            addParticleButton(slots[slotIndex], particle);
        }
    }

    private int[] getParticleSlots() {
        return new int[]{
                10, 11, 12, 13, 14, 15, 16,
                19, 20, 21, 22, 23, 24, 25,
                28, 29, 30, 31, 32, 33, 34,
                37, 38, 39, 40, 41, 42, 43
        };
    }

    private void addParticleButton(int slot, Particle particle) {
        boolean isAdded = preset.getParticles().stream()
                .anyMatch(p -> p.getParticle() == particle);

        Material material = getParticleMaterial(particle);
        ItemStack button = new ItemStack(isAdded ? Material.LIME_DYE : material);
        ItemMeta meta = button.getItemMeta();

        NamedTextColor nameColor = isAdded ? NamedTextColor.GREEN : NamedTextColor.LIGHT_PURPLE;
        meta.displayName(Component.text(formatParticleName(particle), nameColor)
                .decoration(TextDecoration.BOLD, isAdded)
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text(particle.name(), NamedTextColor.DARK_GRAY)
                .decoration(TextDecoration.ITALIC, false));

        if (isAdded) {
            CraftEventPreset.ParticleEvent particleEvent = preset.getParticles().stream()
                    .filter(p -> p.getParticle() == particle)
                    .findFirst().orElse(null);

            if (particleEvent != null) {
                lore.add(Component.empty());
                lore.add(Component.text(lang.getMessage("craft_events.count") + ": ", NamedTextColor.GRAY)
                        .append(Component.text(particleEvent.getCount(), NamedTextColor.YELLOW))
                        .decoration(TextDecoration.ITALIC, false));
                lore.add(Component.text(lang.getMessage("craft_events.speed") + ": ", NamedTextColor.GRAY)
                        .append(Component.text(String.format("%.2f", particleEvent.getSpeed()), NamedTextColor.YELLOW))
                        .decoration(TextDecoration.ITALIC, false));
            }

            lore.add(Component.empty());
            lore.add(Component.text("» " + lang.getMessage("craft_events.left_click_preview"), NamedTextColor.GREEN)
                    .decoration(TextDecoration.ITALIC, true));
            lore.add(Component.text("» " + lang.getMessage("craft_events.right_click_adjust"), NamedTextColor.YELLOW)
                    .decoration(TextDecoration.ITALIC, true));
            lore.add(Component.text("» " + lang.getMessage("craft_events.shift_click_remove"), NamedTextColor.RED)
                    .decoration(TextDecoration.ITALIC, true));
        } else {
            lore.add(Component.empty());
            lore.add(Component.text("» " + lang.getMessage("craft_events.left_click_preview"), NamedTextColor.GREEN)
                    .decoration(TextDecoration.ITALIC, true));
            lore.add(Component.text("» " + lang.getMessage("craft_events.right_click_add"), NamedTextColor.YELLOW)
                    .decoration(TextDecoration.ITALIC, true));
        }

        meta.lore(lore);
        button.setItemMeta(meta);
        inventory.setItem(slot, button);
    }

    private Material getParticleMaterial(Particle particle) {
        String name = particle.name();
        if (name.contains("FLAME") || name.contains("FIRE")) return Material.BLAZE_POWDER;
        if (name.contains("SMOKE")) return Material.GRAY_DYE;
        if (name.contains("HEART")) return Material.RED_DYE;
        if (name.contains("WATER") || name.contains("DRIP")) return Material.WATER_BUCKET;
        if (name.contains("LAVA")) return Material.LAVA_BUCKET;
        if (name.contains("SOUL")) return Material.SOUL_SAND;
        if (name.contains("END")) return Material.END_ROD;
        if (name.contains("ENCHANT")) return Material.ENCHANTING_TABLE;
        if (name.contains("CRIT")) return Material.DIAMOND_SWORD;
        if (name.contains("EXPLOSION")) return Material.TNT;
        if (name.contains("FIREWORK")) return Material.FIREWORK_ROCKET;
        if (name.contains("HAPPY")) return Material.EMERALD;
        if (name.contains("ANGRY")) return Material.REDSTONE;
        if (name.contains("PORTAL")) return Material.OBSIDIAN;
        if (name.contains("WITCH")) return Material.BREWING_STAND;
        if (name.contains("SNOW")) return Material.SNOWBALL;
        if (name.contains("TOTEM")) return Material.TOTEM_OF_UNDYING;
        if (name.contains("CHERRY")) {
            
            Material cherryLeaves = MaterialVersionAdapter.adaptMaterial("CHERRY_LEAVES");
            return cherryLeaves != null ? cherryLeaves : Material.OAK_LEAVES;
        }
        if (name.contains("GLOW")) return Material.GLOW_INK_SAC;
        return Material.BLAZE_POWDER;
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
        String formatted = result.toString();
        if (formatted.length() > 25) {
            formatted = formatted.substring(0, 22) + "...";
        }
        return formatted;
    }

    private List<Particle> getAvailableParticles() {
        
        return ParticleVersionAdapter.getSortedParticles();
    }

    private void addNavigationButtons() {
        List<Particle> particles = getAvailableParticles();
        int totalPages = (int) Math.ceil((double) particles.size() / PARTICLES_PER_PAGE);

        if (currentPage > 0) {
            ItemStack prevButton = new ItemStack(Material.ARROW);
            ItemMeta meta = prevButton.getItemMeta();
            meta.displayName(Component.text(lang.getMessage("lore.previous_page"), NamedTextColor.YELLOW)
                    .decoration(TextDecoration.ITALIC, false));
            prevButton.setItemMeta(meta);
            inventory.setItem(PREV_PAGE_SLOT, prevButton);
        }

        ItemStack pageInfo = new ItemStack(Material.PAPER);
        ItemMeta meta = pageInfo.getItemMeta();
        meta.displayName(Component.text(lang.getMessage("lore.page_info")
                        .replace("{current}", String.valueOf(currentPage + 1))
                        .replace("{total}", String.valueOf(Math.max(1, totalPages))), NamedTextColor.WHITE)
                .decoration(TextDecoration.ITALIC, false));
        pageInfo.setItemMeta(meta);
        inventory.setItem(PAGE_INFO_SLOT, pageInfo);

        if (currentPage < totalPages - 1) {
            ItemStack nextButton = new ItemStack(Material.ARROW);
            ItemMeta nextMeta = nextButton.getItemMeta();
            nextMeta.displayName(Component.text(lang.getMessage("lore.next_page"), NamedTextColor.YELLOW)
                    .decoration(TextDecoration.ITALIC, false));
            nextButton.setItemMeta(nextMeta);
            inventory.setItem(NEXT_PAGE_SLOT, nextButton);
        }
    }

    private void addConfiguredParticlesButton() {
        ItemStack button = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = button.getItemMeta();
        meta.displayName(Component.text(lang.getMessage("craft_events.configured_particles"), NamedTextColor.GOLD)
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text(lang.getMessage("craft_events.configured_count")
                        .replace("{count}", String.valueOf(preset.getParticles().size())), NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));

        if (!preset.getParticles().isEmpty()) {
            lore.add(Component.empty());
            int shown = 0;
            for (CraftEventPreset.ParticleEvent particle : preset.getParticles()) {
                if (shown >= 5) {
                    lore.add(Component.text("  ..." + (preset.getParticles().size() - shown) + " more", NamedTextColor.DARK_GRAY)
                            .decoration(TextDecoration.ITALIC, false));
                    break;
                }
                lore.add(Component.text("  • " + formatParticleName(particle.getParticle()) + " x" + particle.getCount(), NamedTextColor.GREEN)
                        .decoration(TextDecoration.ITALIC, false));
                shown++;
            }
        }

        meta.lore(lore);
        button.setItemMeta(meta);
        inventory.setItem(CONFIGURED_PARTICLES_SLOT, button);
    }

    private void addBackButton() {
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta meta = back.getItemMeta();
        meta.displayName(Component.text(lang.getMessage("button.back"), NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));
        back.setItemMeta(meta);
        inventory.setItem(BACK_SLOT, back);
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

        if (slot == BACK_SLOT) {
            parentGUI.refreshAndOpen();
            return;
        }

        if (slot == PREV_PAGE_SLOT && currentPage > 0) {
            currentPage--;
            updateInventory();
            return;
        }

        if (slot == NEXT_PAGE_SLOT) {
            List<Particle> particles = getAvailableParticles();
            int totalPages = (int) Math.ceil((double) particles.size() / PARTICLES_PER_PAGE);
            if (currentPage < totalPages - 1) {
                currentPage++;
                updateInventory();
            }
            return;
        }

        int[] particleSlots = getParticleSlots();
        for (int i = 0; i < particleSlots.length; i++) {
            if (slot == particleSlots[i]) {
                List<Particle> particles = getAvailableParticles();
                int particleIndex = currentPage * PARTICLES_PER_PAGE + i;

                if (particleIndex < particles.size()) {
                    Particle particle = particles.get(particleIndex);
                    handleParticleClick(particle, clickType);
                }
                return;
            }
        }
    }

    private void handleParticleClick(Particle particle, ClickType clickType) {
        CraftEventPreset.ParticleEvent existing = preset.getParticles().stream()
                .filter(p -> p.getParticle() == particle)
                .findFirst().orElse(null);

        if (clickType.isLeftClick() && !clickType.isShiftClick()) {
            
            int count = existing != null ? existing.getCount() : 10;
            double speed = existing != null ? existing.getSpeed() : 0.1;
            player.getWorld().spawnParticle(particle, player.getLocation().add(0, 1, 0), count, 0.5, 0.5, 0.5, speed);
            return;
        }

        if (clickType.isRightClick()) {
            if (existing != null) {
                
                ParticleAdjustGUI adjustGUI = new ParticleAdjustGUI(plugin, player, preset, existing, this);
                adjustGUI.open();
            } else {
                
                CraftEventPreset.ParticleEvent newParticle = new CraftEventPreset.ParticleEvent(particle, 10);
                preset.addParticle(newParticle);
                MessageUtil.sendInfo(player, lang.getMessage("craft_events.particle_added")
                        .replace("{particle}", formatParticleName(particle)));
                updateInventory();
            }
            return;
        }

        if (clickType.isShiftClick() && existing != null) {
            
            preset.removeParticle(particle);
            MessageUtil.sendInfo(player, lang.getMessage("craft_events.particle_removed")
                    .replace("{particle}", formatParticleName(particle)));
            updateInventory();
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
        updateInventory();
        player.openInventory(inventory);
    }
}
