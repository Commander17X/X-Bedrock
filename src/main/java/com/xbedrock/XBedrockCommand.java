package com.xbedrock;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import com.xbedrock.pvp.PvPManager.PvPMode;
import com.xbedrock.resource.ResourcePackManager;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class XBedrockCommand implements CommandExecutor, TabCompleter {
    private final XBedrockPlugin plugin;

    public XBedrockCommand(XBedrockPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§6X-Bedrock Commands:");
            sender.sendMessage("§6/xbedrock reload §7- Reload the plugin configuration");
            sender.sendMessage("§6/xbedrock status §7- Show plugin status");
            sender.sendMessage("§6/xbedrock pvp <1.8|modern> §7- Set PvP mode");
            sender.sendMessage("§6/xbedrock resourcepack <add|remove|list> [name] §7- Manage resource packs");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload":
                plugin.reloadConfig();
                sender.sendMessage("§aConfiguration reloaded!");
                break;

            case "status":
                sender.sendMessage("§6X-Bedrock Status:");
                sender.sendMessage("§7PvP Mode: §e" + plugin.getPvPManager().getCurrentMode().getName());
                sender.sendMessage("§7Resource Packs: §e" + plugin.getResourcePackManager().getResourcePacks().size());
                break;

            case "pvp":
                if (args.length < 2) {
                    sender.sendMessage("§cUsage: /xbedrock pvp <1.8|modern>");
                    return true;
                }
                try {
                    PvPMode mode = PvPMode.valueOf(args[1].toUpperCase());
                    plugin.getPvPManager().setPvPMode(mode);
                    sender.sendMessage("§aPvP mode set to: " + mode.getName());
                } catch (IllegalArgumentException e) {
                    sender.sendMessage("§cInvalid PvP mode! Use '1.8' or 'modern'");
                }
                break;

            case "resourcepack":
                if (args.length < 2) {
                    sender.sendMessage("§cUsage: /xbedrock resourcepack <add|remove|list> [name]");
                    return true;
                }
                handleResourcePackCommand(sender, args);
                break;

            default:
                sender.sendMessage("§cUnknown command! Use /xbedrock for help.");
                break;
        }

        return true;
    }

    private void handleResourcePackCommand(CommandSender sender, String[] args) {
        switch (args[1].toLowerCase()) {
            case "add":
                if (args.length < 3) {
                    sender.sendMessage("§cUsage: /xbedrock resourcepack add <filename>");
                    return;
                }
                File packFile = new File(plugin.getDataFolder(), "resourcepacks/" + args[2]);
                if (!packFile.exists()) {
                    sender.sendMessage("§cResource pack file not found!");
                    return;
                }
                plugin.getResourcePackManager().addResourcePack(packFile);
                sender.sendMessage("§aResource pack added successfully!");
                break;

            case "remove":
                if (args.length < 3) {
                    sender.sendMessage("§cUsage: /xbedrock resourcepack remove <name>");
                    return;
                }
                plugin.getResourcePackManager().removeResourcePack(args[2]);
                sender.sendMessage("§aResource pack removed successfully!");
                break;

            case "list":
                sender.sendMessage("§6Loaded Resource Packs:");
                plugin.getResourcePackManager().getResourcePacks().keySet()
                        .forEach(name -> sender.sendMessage("§7- " + name));
                break;

            default:
                sender.sendMessage("§cUnknown resource pack command! Use add, remove, or list.");
                break;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("reload", "status", "pvp", "resourcepack");
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("pvp")) {
                return Arrays.asList("1.8", "modern");
            } else if (args[0].equalsIgnoreCase("resourcepack")) {
                return Arrays.asList("add", "remove", "list");
            }
        } else if (args.length == 3) {
            if (args[0].equalsIgnoreCase("resourcepack") && args[1].equalsIgnoreCase("remove")) {
                return new ArrayList<>(plugin.getResourcePackManager().getResourcePacks().keySet());
            }
        }
        return new ArrayList<>();
    }
}