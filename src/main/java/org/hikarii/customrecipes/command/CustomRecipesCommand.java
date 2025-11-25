package org.hikarii.customrecipes.command;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.hikarii.customrecipes.CustomRecipes;
import org.hikarii.customrecipes.command.subcommands.GuiSubcommand;
import org.hikarii.customrecipes.command.subcommands.ListSubcommand;
import org.hikarii.customrecipes.command.subcommands.ReloadSubcommand;
import org.hikarii.customrecipes.util.MessageUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.*;

public class CustomRecipesCommand implements CommandExecutor, TabCompleter {
    private final CustomRecipes plugin;
    private final Map<String, SubCommand> subCommands;
    public CustomRecipesCommand(CustomRecipes plugin) {
        this.plugin = plugin;
        this.subCommands = new HashMap<>();
        registerSubcommand(new ReloadSubcommand(plugin));
        registerSubcommand(new GuiSubcommand(plugin));
        registerSubcommand(new ListSubcommand(plugin));
    }

    private void registerSubcommand(SubCommand subCommand) {
        subCommands.put(subCommand.getName().toLowerCase(), subCommand);
        for (String alias : subCommand.getAliases()) {
            subCommands.put(alias.toLowerCase(), subCommand);
        }
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args) {
        if (args.length == 0) {
            if (!sender.hasPermission("customrecipes.manage") &&
                !sender.hasPermission("customrecipes.reload") &&
                !sender.hasPermission("customrecipes.gui")) {

                SubCommand listCommand = subCommands.get("list");
                if (listCommand != null && sender.hasPermission(listCommand.getPermission())) {
                    listCommand.execute(sender, new String[0]);
                    return true;
                }
                MessageUtil.sendError(sender, plugin.getLanguageManager().getMessage("commands.no_permission"));
                return true;
            }
            showHelp(sender);
            return true;
        }

        String subCommandName = args[0].toLowerCase();
        if (subCommandName.equals("help") || subCommandName.equals(plugin.getLanguageManager().getMessage("commands.help_command"))) {
            if (!sender.hasPermission("customrecipes.help")) {
                MessageUtil.sendError(sender, plugin.getLanguageManager().getMessage("commands.no_permission"));
                return true;
            }
            showHelp(sender);
            return true;
        }

        SubCommand subCommand = subCommands.get(subCommandName);
        if (subCommand != null) {
            if (!sender.hasPermission(subCommand.getPermission())) {
                MessageUtil.sendError(sender, plugin.getLanguageManager().getMessage("commands.no_permission"));
                return true;
            }
            String[] subArgs = Arrays.copyOfRange(args, 1, args.length);
            subCommand.execute(sender, subArgs);
            return true;
        }
        MessageUtil.sendError(sender, plugin.getLanguageManager().getMessage("commands.unknown_subcommand"));
        return true;
    }

    private void showHelp(CommandSender sender) {
        String helpHeader = plugin.getLanguageManager().getMessage("commands.help_header");
        sender.sendMessage(MessageUtil.colorize(
                "<gradient:#00D4FF:#7B2CBF>====== " + helpHeader + " ======</gradient>"
        ));
        sender.sendMessage(Component.empty());
        Set<SubCommand> uniqueCommands = new HashSet<>(subCommands.values());
        for (SubCommand subCommand : uniqueCommands) {
            if (sender.hasPermission(subCommand.getPermission())) {
                Component commandComponent = Component.text("  " + subCommand.getUsage(), NamedTextColor.AQUA);
                Component descComponent = Component.text(" - " + subCommand.getDescription(), NamedTextColor.GRAY);
                sender.sendMessage(commandComponent.append(descComponent));
            }
        }
        sender.sendMessage(Component.empty());
        String helpFooter = plugin.getLanguageManager().getMessage("commands.help_footer");
        sender.sendMessage(MessageUtil.colorize("<gray>" + helpFooter));
    }

    @Override
    public @Nullable List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            if (sender.hasPermission("customrecipes.manage") ||
                sender.hasPermission("customrecipes.reload") ||
                sender.hasPermission("customrecipes.gui")) {
                completions.add(plugin.getLanguageManager().getMessage("commands.help_command"));
            }
            for (SubCommand subCommand : new HashSet<>(subCommands.values())) {
                if (sender.hasPermission(subCommand.getPermission())) {
                    completions.add(subCommand.getName());
                }
            }
            String input = args[0].toLowerCase();
            return completions.stream()
                    .filter(s -> s.toLowerCase().startsWith(input))
                    .sorted()
                    .toList();
        }
        if (args.length > 1) {
            SubCommand subCommand = subCommands.get(args[0].toLowerCase());
            if (subCommand != null && sender.hasPermission(subCommand.getPermission())) {
                String[] subArgs = Arrays.copyOfRange(args, 1, args.length);
                return subCommand.tabComplete(sender, subArgs);
            }
        }
        return completions;
    }

    public interface SubCommand {
        String getName();
        List<String> getAliases();
        String getDescription();
        String getUsage();
        String getPermission();
        void execute(CommandSender sender, String[] args);
        List<String> tabComplete(CommandSender sender, String[] args);
    }
}