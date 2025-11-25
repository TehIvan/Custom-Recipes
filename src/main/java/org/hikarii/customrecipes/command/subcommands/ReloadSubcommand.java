package org.hikarii.customrecipes.command.subcommands;

import org.bukkit.command.CommandSender;
import org.hikarii.customrecipes.CustomRecipes;
import org.hikarii.customrecipes.command.CustomRecipesCommand;
import org.hikarii.customrecipes.language.LanguageManager;
import org.hikarii.customrecipes.util.MessageUtil;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ReloadSubcommand implements CustomRecipesCommand.SubCommand {
    private final CustomRecipes plugin;

    public ReloadSubcommand(CustomRecipes plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "reload";
    }

    @Override
    public List<String> getAliases() {
        return List.of("rl", "refresh");
    }

    @Override
    public String getDescription() {
        return "Reload plugin configuration and recipes";
    }

    @Override
    public String getUsage() {
        return "customrecipes reload";
    }

    @Override
    public String getPermission() {
        return "customrecipes.reload";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        LanguageManager langManager = plugin.getLanguageManager();

        MessageUtil.sendInfo(sender, langManager.getMessage("general.reload_start"));
        long startTime = System.currentTimeMillis();

        langManager.reload();

        boolean success = plugin.loadConfiguration();
        long duration = System.currentTimeMillis() - startTime;

        if (success) {
            int recipeCount = plugin.getRecipeManager().getRecipeCount();
            Map<String, String> placeholders = Map.of(
                "count", String.valueOf(recipeCount),
                "time", String.valueOf(duration)
            );
            MessageUtil.sendSuccess(sender, langManager.getMessage("general.reload_success", placeholders));
        } else {
            MessageUtil.sendError(sender, langManager.getMessage("general.reload_failed"));
        }
    }
    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}