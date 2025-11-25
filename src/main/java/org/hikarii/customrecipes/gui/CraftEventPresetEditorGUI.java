package org.hikarii.customrecipes.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
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

public class CraftEventPresetEditorGUI implements Listener, GUIBase {
    private final CustomRecipes plugin;
    private final Player player;
    private final Inventory inventory;
    private final LanguageManager lang;
    private final CraftEventPreset preset;
    private final CraftEventsMenuGUI parentGUI;

    private static final int SOUNDS_SLOT = 20;
    private static final int PARTICLES_SLOT = 22;
    private static final int COMMANDS_SLOT = 24;
    private static final int SCOPE_SLOT = 31;
    private static final int SAVE_SLOT = 45;
    private static final int BACK_SLOT = 53;

    public CraftEventPresetEditorGUI(CustomRecipes plugin, Player player, CraftEventPreset preset, CraftEventsMenuGUI parentGUI) {
        this.plugin = plugin;
        this.player = player;
        this.preset = preset;
        this.parentGUI = parentGUI;
        this.lang = plugin.getLanguageManager();
        this.inventory = Bukkit.createInventory(
                null,
                54,
                MessageUtil.createGradientMenuTitle(lang.getMessage("craft_events.editor_title")
                        .replace("{name}", preset.getName()))
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
        addPresetNameDisplay();
        addSoundsButton();
        addParticlesButton();
        addCommandsButton();
        addScopeButton();
        addSaveButton();
        addBackButton();
    }

    private void fillBorders() {
        ItemStack borderPane = new ItemStack(Material.LIGHT_BLUE_STAINED_GLASS_PANE);
        ItemMeta meta = borderPane.getItemMeta();
        meta.displayName(Component.empty());
        borderPane.setItemMeta(meta);

        for (int i = 0; i < 54; i++) {
            inventory.setItem(i, borderPane);
        }
    }

    private void addPresetNameDisplay() {
        ItemStack nameItem = new ItemStack(Material.NAME_TAG);
        ItemMeta meta = nameItem.getItemMeta();
        meta.displayName(Component.text(preset.getName(), NamedTextColor.GOLD)
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text(lang.getMessage("craft_events.editing_preset"), NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));

        meta.lore(lore);
        nameItem.setItemMeta(meta);
        inventory.setItem(4, nameItem);
    }

    private void addSoundsButton() {
        ItemStack button = new ItemStack(Material.NOTE_BLOCK);
        ItemMeta meta = button.getItemMeta();
        meta.displayName(Component.text(lang.getMessage("craft_events.sounds_button"), NamedTextColor.AQUA)
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text(lang.getMessage("craft_events.sounds_desc"), NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(Component.text(lang.getMessage("craft_events.configured_count")
                        .replace("{count}", String.valueOf(preset.getSounds().size())), NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));

        if (!preset.getSounds().isEmpty()) {
            lore.add(Component.empty());
            int shown = 0;
            for (CraftEventPreset.SoundEvent sound : preset.getSounds()) {
                if (shown >= 5) {
                    lore.add(Component.text("  ..." + (preset.getSounds().size() - shown) + " more", NamedTextColor.DARK_GRAY)
                            .decoration(TextDecoration.ITALIC, false));
                    break;
                }
                lore.add(Component.text("  • " + sound.getSound().name(), NamedTextColor.GREEN)
                        .decoration(TextDecoration.ITALIC, false));
                shown++;
            }
        }

        lore.add(Component.empty());
        lore.add(Component.text("» " + lang.getMessage("lore.click_to_configure"), NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, true));

        meta.lore(lore);
        button.setItemMeta(meta);
        inventory.setItem(SOUNDS_SLOT, button);
    }

    private void addParticlesButton() {
        ItemStack button = new ItemStack(Material.BLAZE_POWDER);
        ItemMeta meta = button.getItemMeta();
        meta.displayName(Component.text(lang.getMessage("craft_events.particles_button"), NamedTextColor.LIGHT_PURPLE)
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text(lang.getMessage("craft_events.particles_desc"), NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
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
                lore.add(Component.text("  • " + particle.getParticle().name() + " x" + particle.getCount(), NamedTextColor.GREEN)
                        .decoration(TextDecoration.ITALIC, false));
                shown++;
            }
        }

        lore.add(Component.empty());
        lore.add(Component.text("» " + lang.getMessage("lore.click_to_configure"), NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, true));

        meta.lore(lore);
        button.setItemMeta(meta);
        inventory.setItem(PARTICLES_SLOT, button);
    }

    private void addCommandsButton() {
        ItemStack button = new ItemStack(Material.COMMAND_BLOCK);
        ItemMeta meta = button.getItemMeta();
        meta.displayName(Component.text(lang.getMessage("craft_events.commands_button"), NamedTextColor.RED)
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text(lang.getMessage("craft_events.commands_desc"), NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(Component.text(lang.getMessage("craft_events.configured_count")
                        .replace("{count}", String.valueOf(preset.getCommands().size())), NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));

        if (!preset.getCommands().isEmpty()) {
            lore.add(Component.empty());
            int shown = 0;
            for (CraftEventPreset.CommandEvent cmd : preset.getCommands()) {
                if (shown >= 3) {
                    lore.add(Component.text("  ..." + (preset.getCommands().size() - shown) + " more", NamedTextColor.DARK_GRAY)
                            .decoration(TextDecoration.ITALIC, false));
                    break;
                }
                String cmdText = cmd.getCommand();
                if (cmdText.length() > 25) {
                    cmdText = cmdText.substring(0, 22) + "...";
                }
                lore.add(Component.text("  • [" + cmd.getType().name() + "] " + cmdText, NamedTextColor.GREEN)
                        .decoration(TextDecoration.ITALIC, false));
                shown++;
            }
        }

        lore.add(Component.empty());
        lore.add(Component.text("» " + lang.getMessage("lore.click_to_configure"), NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, true));

        meta.lore(lore);
        button.setItemMeta(meta);
        inventory.setItem(COMMANDS_SLOT, button);
    }

    private void addScopeButton() {
        CraftEventPreset.RecipeScope scope = preset.getScope();
        Material material = switch (scope) {
            case ALL -> Material.ENDER_EYE;
            case CUSTOM_ONLY -> Material.CRAFTING_TABLE;
            case VANILLA_ONLY -> Material.FURNACE;
        };

        ItemStack button = new ItemStack(material);
        ItemMeta meta = button.getItemMeta();
        meta.displayName(Component.text(lang.getMessage("craft_events.scope_button"), NamedTextColor.GOLD)
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text(lang.getMessage("craft_events.scope_desc"), NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());

        String scopeText = switch (scope) {
            case ALL -> lang.getMessage("craft_events.scope_all");
            case CUSTOM_ONLY -> lang.getMessage("craft_events.scope_custom");
            case VANILLA_ONLY -> lang.getMessage("craft_events.scope_vanilla");
        };
        lore.add(Component.text(lang.getMessage("info.current") + ": ", NamedTextColor.GRAY)
                .append(Component.text(scopeText, NamedTextColor.AQUA))
                .decoration(TextDecoration.ITALIC, false));

        lore.add(Component.empty());
        lore.add(Component.text("» " + lang.getMessage("lore.click_to_toggle"), NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, true));

        meta.lore(lore);
        button.setItemMeta(meta);
        inventory.setItem(SCOPE_SLOT, button);
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
        meta.displayName(Component.text("« " + lang.getMessage("button.back_to_craft_events"), NamedTextColor.YELLOW)
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

        switch (slot) {
            case BACK_SLOT -> {
                if (parentGUI != null) {
                    parentGUI.refreshAndOpen();
                } else {
                    player.closeInventory();
                }
            }
            case SAVE_SLOT -> {
                plugin.getCraftEventPresetManager().addPreset(preset);
                MessageUtil.sendAdminSuccess(player, lang.getMessage("craft_events.preset_saved")
                        .replace("{name}", preset.getName()));
                if (parentGUI != null) {
                    parentGUI.refreshAndOpen();
                } else {
                    player.closeInventory();
                }
            }
            case SOUNDS_SLOT -> {
                SoundSelectorGUI soundGUI = new SoundSelectorGUI(plugin, player, preset, this);
                soundGUI.open();
            }
            case PARTICLES_SLOT -> {
                ParticleSelectorGUI particleGUI = new ParticleSelectorGUI(plugin, player, preset, this);
                particleGUI.open();
            }
            case COMMANDS_SLOT -> {
                CommandEditorGUI commandGUI = new CommandEditorGUI(plugin, player, preset, this);
                commandGUI.open();
            }
            case SCOPE_SLOT -> {
                
                CraftEventPreset.RecipeScope currentScope = preset.getScope();
                CraftEventPreset.RecipeScope newScope = switch (currentScope) {
                    case ALL -> CraftEventPreset.RecipeScope.CUSTOM_ONLY;
                    case CUSTOM_ONLY -> CraftEventPreset.RecipeScope.VANILLA_ONLY;
                    case VANILLA_ONLY -> CraftEventPreset.RecipeScope.ALL;
                };
                preset.setScope(newScope);
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
        updateInventory();
        player.openInventory(inventory);
    }
}
