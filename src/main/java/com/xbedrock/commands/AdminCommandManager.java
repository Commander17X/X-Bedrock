package com.xbedrock.commands;

import com.xbedrock.XBedrockPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AdminCommandManager implements CommandExecutor, TabCompleter {
    private final XBedrockPlugin plugin;

    public AdminCommandManager(XBedrockPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("xbedrock.admin")) {
            sender.sendMessage("§cYou don't have permission to use this command!");
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "toggle":
                handleToggle(sender, args);
                break;
            case "prefix":
                handlePrefix(sender, args);
                break;
            case "webstore":
                handleWebstore(sender, args);
                break;
            case "roblox":
                handleRoblox(sender, args);
                break;
            default:
                sendHelp(sender);
                break;
        }

        return true;
    }

    private void handleToggle(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /xbedrock toggle <feature>");
            return;
        }

        String feature = args[1].toLowerCase();
        switch (feature) {
            case "cosmetics":
                plugin.getCosmeticsManager().setEnabled(!plugin.getCosmeticsManager().isEnabled());
                sender.sendMessage(
                        "§aCosmetics " + (plugin.getCosmeticsManager().isEnabled() ? "enabled" : "disabled"));
                break;
            case "webstore":
                plugin.getWebstoreManager().setEnabled(!plugin.getWebstoreManager().isEnabled());
                sender.sendMessage("§aWebstore " + (plugin.getWebstoreManager().isEnabled() ? "enabled" : "disabled"));
                break;
            case "roblox":
                plugin.getRobloxManager().setEnabled(!plugin.getRobloxManager().isEnabled());
                sender.sendMessage(
                        "§aRoblox integration " + (plugin.getRobloxManager().isEnabled() ? "enabled" : "disabled"));
                break;
            default:
                sender.sendMessage("§cUnknown feature: " + feature);
                break;
        }
    }

    private void handlePrefix(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§cUsage: /xbedrock prefix <player> <prefix>");
            return;
        }

        Player target = plugin.getServer().getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage("§cPlayer not found!");
            return;
        }

        String prefix = args[2];
        plugin.getPlayerManager().setPrefix(target, prefix);
        sender.sendMessage("§aSet prefix for " + target.getName() + " to: " + prefix);
    }

    private void handleWebstore(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /xbedrock webstore <reload|status>");
            return;
        }

        switch (args[1].toLowerCase()) {
            case "reload":
                plugin.getWebstoreManager().reload();
                sender.sendMessage("§aWebstore configuration reloaded!");
                break;
            case "status":
                sender.sendMessage("§aWebstore Status:");
                sender.sendMessage("§7- Enabled: " + plugin.getWebstoreManager().isEnabled());
                sender.sendMessage("§7- Connected Players: " + plugin.getWebstoreManager().getConnectedPlayers());
                break;
            default:
                sender.sendMessage("§cUnknown webstore command!");
                break;
        }
    }

    private void handleRoblox(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /xbedrock roblox <reload|status|sync>");
            return;
        }

        switch (args[1].toLowerCase()) {
            case "reload":
                plugin.getRobloxManager().reload();
                sender.sendMessage("§aRoblox integration reloaded!");
                break;
            case "status":
                sender.sendMessage("§aRoblox Integration Status:");
                sender.sendMessage("§7- Enabled: " + plugin.getRobloxManager().isEnabled());
                sender.sendMessage(
                        "§7- Connected Roblox Players: " + plugin.getRobloxManager().getConnectedRobloxPlayers());
                break;
            case "sync":
                plugin.getRobloxManager().syncAllPlayers();
                sender.sendMessage("§aSynced all players with Roblox!");
                break;
            default:
                sender.sendMessage("§cUnknown roblox command!");
                break;
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§6=== X-Bedrock Admin Commands ===");
        sender.sendMessage("§6/xbedrock toggle <feature> §7- Toggle features");
        sender.sendMessage("§6/xbedrock prefix <player> <prefix> §7- Set player prefix");
        sender.sendMessage("§6/xbedrock webstore <reload|status> §7- Manage webstore");
        sender.sendMessage("§6/xbedrock roblox <reload|status|sync> §7- Manage Roblox integration");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(Arrays.asList("toggle", "prefix", "webstore", "roblox"));
        } else if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "toggle":
                    completions.addAll(Arrays.asList("cosmetics", "webstore", "roblox"));
                    break;
                case "prefix":
                    plugin.getServer().getOnlinePlayers().forEach(player -> completions.add(player.getName()));
                    break;
                case "webstore":
                    completions.addAll(Arrays.asList("reload", "status"));
                    break;
                case "roblox":
                    completions.addAll(Arrays.asList("reload", "status", "sync"));
                    break;
            }
        }

        return completions;
    }
}