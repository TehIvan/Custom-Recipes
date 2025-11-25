package org.hikarii.customrecipes.command.subcommands;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.hikarii.customrecipes.CustomRecipes;
import org.hikarii.customrecipes.command.CustomRecipesCommand;
import org.hikarii.customrecipes.gui.RecipeListGUI;
import org.hikarii.customrecipes.util.MessageUtil;
import java.util.Collections;
import java.util.List;

public class GuiSubcommand implements CustomRecipesCommand.SubCommand {
    private final CustomRecipes plugin;

    public GuiSubcommand(CustomRecipes plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "gui";
    }

    @Override
    public List<String> getAliases() {
        return List.of("menu", "open");
    }

    @Override
    public String getDescription() {
        return plugin.getLanguageManager().getMessage("commands.gui_description");
    }

    @Override
    public String getUsage() {
        return plugin.getLanguageManager().getMessage("commands.gui_usage");
    }

    @Override
    public String getPermission() {
        return "customrecipes.gui";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            MessageUtil.sendError(sender, plugin.getLanguageManager().getMessage("general.player_only"));
            return;
        }
        RecipeListGUI gui = new RecipeListGUI(plugin, player);
        gui.open();
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}