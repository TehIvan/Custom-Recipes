package org.hikarii.customrecipes.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.hikarii.customrecipes.CustomRecipes;
import org.hikarii.customrecipes.language.LanguageManager;
import org.hikarii.customrecipes.recipe.CraftEventPreset;
import org.hikarii.customrecipes.util.MessageUtil;

import java.util.ArrayList;
import java.util.List;

public class CommandEditorGUI implements Listener, GUIBase {
    private final CustomRecipes plugin;
    private final Player player;
    private final Inventory inventory;
    private final LanguageManager lang;
    private final CraftEventPreset preset;
    private final CraftEventPresetEditorGUI parentGUI;

    private int currentPage = 0;
    private static final int COMMANDS_PER_PAGE = 21;

    private static final int ADD_COMMAND_SLOT = 4;
    private static final int BACK_SLOT = 45;
    private static final int PREV_PAGE_SLOT = 48;
    private static final int PAGE_INFO_SLOT = 49;
    private static final int NEXT_PAGE_SLOT = 50;
    private static final int PLACEHOLDERS_INFO_SLOT = 53;

    private boolean awaitingInput = false;
    private CraftEventPreset.CommandEvent.CommandType pendingCommandType = null;

    public CommandEditorGUI(CustomRecipes plugin, Player player, CraftEventPreset preset, CraftEventPresetEditorGUI parentGUI) {
        this.plugin = plugin;
        this.player = player;
        this.preset = preset;
        this.parentGUI = parentGUI;
        this.lang = plugin.getLanguageManager();
        this.inventory = Bukkit.createInventory(
                null,
                54,
                MessageUtil.createGradientMenuTitle(lang.getMessage("craft_events.commands_title"))
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
        addCommandTypeButtons();
        addCommandList();
        addNavigationButtons();
        addPlaceholdersInfo();
        addBackButton();
    }

    private void fillBorders() {
        ItemStack borderPane = new ItemStack(Material.YELLOW_STAINED_GLASS_PANE);
        ItemMeta meta = borderPane.getItemMeta();
        meta.displayName(Component.empty());
        borderPane.setItemMeta(meta);

        inventory.setItem(0, borderPane);
        inventory.setItem(8, borderPane);

        for (int i = 45; i < 54; i++) {
            inventory.setItem(i, borderPane);
        }

        for (int row = 1; row < 5; row++) {
            inventory.setItem(row * 9, borderPane);
            inventory.setItem(row * 9 + 8, borderPane);
        }
    }

    private void addCommandTypeButtons() {
        
        addCommandTypeButton(2, CraftEventPreset.CommandEvent.CommandType.CONSOLE,
                Material.COMMAND_BLOCK, NamedTextColor.GOLD);

        addCommandTypeButton(4, CraftEventPreset.CommandEvent.CommandType.PLAYER,
                Material.PLAYER_HEAD, NamedTextColor.GREEN);

        addCommandTypeButton(6, CraftEventPreset.CommandEvent.CommandType.OP,
                Material.NETHER_STAR, NamedTextColor.RED);
    }

    private void addCommandTypeButton(int slot, CraftEventPreset.CommandEvent.CommandType type, Material material, NamedTextColor color) {
        ItemStack button = new ItemStack(material);
        ItemMeta meta = button.getItemMeta();

        String typeKey = type.name().toLowerCase();
        meta.displayName(Component.text("+ " + lang.getMessage("craft_events.type_" + typeKey), color)
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text(lang.getMessage("craft_events.type_" + typeKey + "_desc"), NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(Component.text("» " + lang.getMessage("craft_events.add_command"), NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, true));

        meta.lore(lore);
        button.setItemMeta(meta);
        inventory.setItem(slot, button);
    }

    private void addCommandList() {
        List<CraftEventPreset.CommandEvent> commands = preset.getCommands();
        int start = currentPage * COMMANDS_PER_PAGE;
        int end = Math.min(start + COMMANDS_PER_PAGE, commands.size());

        int[] slots = getCommandSlots();
        int slotIndex = 0;

        for (int i = start; i < end && slotIndex < slots.length; i++, slotIndex++) {
            CraftEventPreset.CommandEvent command = commands.get(i);
            addCommandButton(slots[slotIndex], command, i);
        }
    }

    private int[] getCommandSlots() {
        return new int[]{
                10, 11, 12, 13, 14, 15, 16,
                19, 20, 21, 22, 23, 24, 25,
                28, 29, 30, 31, 32, 33, 34
        };
    }

    private void addCommandButton(int slot, CraftEventPreset.CommandEvent command, int index) {
        Material material = switch (command.getType()) {
            case CONSOLE -> Material.COMMAND_BLOCK;
            case PLAYER -> Material.PAPER;
            case OP -> Material.GOLDEN_APPLE;
        };

        NamedTextColor color = switch (command.getType()) {
            case CONSOLE -> NamedTextColor.GOLD;
            case PLAYER -> NamedTextColor.GREEN;
            case OP -> NamedTextColor.RED;
        };

        ItemStack button = new ItemStack(material);
        ItemMeta meta = button.getItemMeta();

        String displayCommand = command.getCommand();
        if (displayCommand.length() > 25) {
            displayCommand = displayCommand.substring(0, 22) + "...";
        }

        meta.displayName(Component.text("/" + displayCommand, color)
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text(lang.getMessage("craft_events.command_type") + ": ", NamedTextColor.GRAY)
                .append(Component.text(lang.getMessage("craft_events.type_" + command.getType().name().toLowerCase()), color))
                .decoration(TextDecoration.ITALIC, false));

        if (command.getCommand().length() > 25) {
            lore.add(Component.empty());
            lore.add(Component.text(lang.getMessage("info.full_command") + ":", NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false));
            
            String fullCmd = command.getCommand();
            int lineLen = 35;
            for (int i = 0; i < fullCmd.length(); i += lineLen) {
                String part = fullCmd.substring(i, Math.min(i + lineLen, fullCmd.length()));
                lore.add(Component.text("  " + part, NamedTextColor.WHITE)
                        .decoration(TextDecoration.ITALIC, false));
            }
        }

        lore.add(Component.empty());
        lore.add(Component.text("» " + lang.getMessage("craft_events.shift_click_remove"), NamedTextColor.RED)
                .decoration(TextDecoration.ITALIC, true));

        meta.lore(lore);
        button.setItemMeta(meta);
        inventory.setItem(slot, button);
    }

    private void addNavigationButtons() {
        List<CraftEventPreset.CommandEvent> commands = preset.getCommands();
        int totalPages = (int) Math.ceil((double) commands.size() / COMMANDS_PER_PAGE);
        if (totalPages == 0) totalPages = 1;

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
                        .replace("{total}", String.valueOf(totalPages)), NamedTextColor.WHITE)
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text(lang.getMessage("craft_events.configured_count")
                        .replace("{count}", String.valueOf(commands.size())), NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);
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

    private void addPlaceholdersInfo() {
        ItemStack button = new ItemStack(Material.OAK_SIGN);
        ItemMeta meta = button.getItemMeta();
        meta.displayName(Component.text(lang.getMessage("craft_events.placeholders_info"), NamedTextColor.AQUA)
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text("{player}", NamedTextColor.YELLOW)
                .append(Component.text(" - " + lang.getMessage("info.player_name"), NamedTextColor.GRAY))
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("{uuid}", NamedTextColor.YELLOW)
                .append(Component.text(" - " + lang.getMessage("info.player_uuid"), NamedTextColor.GRAY))
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("{world}", NamedTextColor.YELLOW)
                .append(Component.text(" - " + lang.getMessage("info.world_name"), NamedTextColor.GRAY))
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("{x}, {y}, {z}", NamedTextColor.YELLOW)
                .append(Component.text(" - " + lang.getMessage("info.coordinates"), NamedTextColor.GRAY))
                .decoration(TextDecoration.ITALIC, false));

        meta.lore(lore);
        button.setItemMeta(meta);
        inventory.setItem(PLACEHOLDERS_INFO_SLOT, button);
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
            List<CraftEventPreset.CommandEvent> commands = preset.getCommands();
            int totalPages = (int) Math.ceil((double) commands.size() / COMMANDS_PER_PAGE);
            if (currentPage < totalPages - 1) {
                currentPage++;
                updateInventory();
            }
            return;
        }

        if (slot == 2) {
            startCommandInput(CraftEventPreset.CommandEvent.CommandType.CONSOLE);
            return;
        }
        if (slot == 4) {
            startCommandInput(CraftEventPreset.CommandEvent.CommandType.PLAYER);
            return;
        }
        if (slot == 6) {
            startCommandInput(CraftEventPreset.CommandEvent.CommandType.OP);
            return;
        }

        int[] commandSlots = getCommandSlots();
        for (int i = 0; i < commandSlots.length; i++) {
            if (slot == commandSlots[i]) {
                int commandIndex = currentPage * COMMANDS_PER_PAGE + i;
                List<CraftEventPreset.CommandEvent> commands = preset.getCommands();

                if (commandIndex < commands.size() && clickType.isShiftClick()) {
                    CraftEventPreset.CommandEvent command = commands.get(commandIndex);
                    preset.removeCommand(command);
                    MessageUtil.sendInfo(player, lang.getMessage("craft_events.command_removed"));
                    updateInventory();
                }
                return;
            }
        }
    }

    private void startCommandInput(CraftEventPreset.CommandEvent.CommandType type) {
        awaitingInput = true;
        pendingCommandType = type;
        player.closeInventory();
        MessageUtil.sendInfo(player, lang.getMessage("craft_events.enter_command"));
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        if (!event.getPlayer().equals(player) || !awaitingInput) {
            return;
        }

        event.setCancelled(true);
        awaitingInput = false;

        String message = event.getMessage().trim();

        if (message.equalsIgnoreCase("cancel")) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                updateInventory();
                open();
            });
            return;
        }

        if (message.startsWith("/")) {
            message = message.substring(1);
        }

        String finalMessage = message;
        Bukkit.getScheduler().runTask(plugin, () -> {
            CraftEventPreset.CommandEvent newCommand = new CraftEventPreset.CommandEvent(finalMessage, pendingCommandType);
            preset.addCommand(newCommand);
            MessageUtil.sendInfo(player, lang.getMessage("craft_events.command_added"));
            pendingCommandType = null;
            updateInventory();
            open();
        });
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().equals(inventory) && event.getPlayer().equals(player)) {
            if (!awaitingInput) {
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (!inventory.equals(player.getOpenInventory().getTopInventory())) {
                        InventoryClickEvent.getHandlerList().unregister(this);
                        InventoryCloseEvent.getHandlerList().unregister(this);
                        AsyncPlayerChatEvent.getHandlerList().unregister(this);
                    }
                }, 1L);
            }
        }
    }

    public void refreshAndOpen() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        updateInventory();
        player.openInventory(inventory);
    }
}
