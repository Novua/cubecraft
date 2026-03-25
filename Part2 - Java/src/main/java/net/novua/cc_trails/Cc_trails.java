package net.novua.cc_trails;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public final class Cc_trails extends JavaPlugin implements TabExecutor {

    private TrailManager trailManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.trailManager = new TrailManager(this);
        this.trailManager.start();
        getServer().getPluginManager().registerEvents(new TrailPlayerListener(trailManager), this);

        if (getCommand("trail") != null) {
            getCommand("trail").setExecutor(this);
            getCommand("trail").setTabCompleter(this);
        }

        getLogger().info("cc_trails enabled: flat helix/orbit/wave trails ready.");
    }

    @Override
    public void onDisable() {
        if (trailManager != null) {
            trailManager.stop();
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        if (args.length == 0) {
            sendUsage(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "on" -> {
                trailManager.enable(player);
                player.sendMessage(ChatColor.GREEN + "Particle trail enabled.");
            }
            case "off" -> {
                trailManager.disable(player);
                player.sendMessage(ChatColor.YELLOW + "Particle trail disabled.");
            }
            case "reload" -> {
                if (!player.hasPermission("cc_trails.reload")) {
                    player.sendMessage(ChatColor.RED + "You do not have permission.");
                    return true;
                }
                trailManager.reload();
                player.sendMessage(ChatColor.GREEN + "Trail config reloaded.");
            }
            case "helix", "orbit", "wave" -> {
                TrailStyle style = TrailStyle.from(args[0]);
                trailManager.enable(player, style);
                player.sendMessage(ChatColor.GREEN + "Particle trail style set to " + ChatColor.AQUA + style.id());
            }
            default -> sendUsage(player);
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1) {
            return List.of();
        }

        String input = args[0].toLowerCase();
        return List.of("on", "off", "reload", "helix", "orbit", "wave").stream()
                .filter(option -> option.startsWith(input))
                .toList();
    }

    private void sendUsage(Player player) {
        player.sendMessage(ChatColor.YELLOW + "Usage: /trail <on|off|reload|helix|orbit|wave>");
    }
}
