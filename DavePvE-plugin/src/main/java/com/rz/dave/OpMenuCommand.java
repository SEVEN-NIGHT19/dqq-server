package com.rz.dave;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class OpMenuCommand implements CommandExecutor {
    private final DaveManager manager;

    public OpMenuCommand(DaveManager manager) {
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(org.bukkit.ChatColor.GRAY + "该命令只能由玩家使用");
            return true;
        }
        if (!sender.hasPermission("davepve.admin")) {
            sender.sendMessage(org.bukkit.ChatColor.RED + "你没有权限使用此命令");
            return true;
        }
        player.openInventory(new OpMenu().getInventory());
        return true;
    }
}
