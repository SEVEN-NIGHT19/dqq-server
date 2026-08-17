package com.rz.dave;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class LobbyCommand implements CommandExecutor {
    private final DaveManager manager;

    public LobbyCommand(DaveManager manager) {
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.GRAY + "该命令只能由玩家使用");
            return true;
        }
        manager.returnToLobby(player);
        player.sendMessage(ChatColor.AQUA + "【大厅】已返回大厅");
        return true;
    }
}
