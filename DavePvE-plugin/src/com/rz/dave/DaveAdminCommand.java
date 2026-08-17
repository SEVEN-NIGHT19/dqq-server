package com.rz.dave;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Team;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class DaveAdminCommand implements CommandExecutor, TabCompleter {
    private final DaveManager manager;

    public DaveAdminCommand(DaveManager manager) {
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(org.bukkit.ChatColor.GOLD + "用法: /davepve ready|unready  |  /davepve balance|start|end|wave|bind|kill|killnearest|info");
            return true;
        }
        String sub = args[0].toLowerCase();
        if (sub.equals("ready") || sub.equals("unready")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(org.bukkit.ChatColor.GRAY + "该命令只能由玩家使用");
                return true;
            }
            boolean ready = sub.equals("ready");
            if (ready && manager.isGameRunning()) {
                manager.joinMidGame(player);
                return true;
            }
            boolean changed = manager.setReady(player, ready);
            String message = ready
                    ? (changed ? "你已准备（当前 " + manager.readyCount() + " 人准备）" : "你已经在准备状态")
                    : (changed ? "你已取消准备（当前 " + manager.readyCount() + " 人准备）" : "你本来就没在准备状态");
            player.sendMessage(org.bukkit.ChatColor.GREEN + "【准备】" + message);
            if (changed) {
                Bukkit.broadcastMessage(org.bukkit.ChatColor.GREEN + "【准备】" + player.getName() + (ready ? " 已准备" : " 取消准备")
                        + "（当前 " + manager.readyCount() + " 人准备）");
                manager.refreshPlayerListName(player);
            }
            return true;
        }
        if (sub.equals("join")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(org.bukkit.ChatColor.GRAY + "该命令只能由玩家使用");
                return true;
            }
            manager.joinMidGame(player);
            return true;
        }
        if (sub.equals("pvz") && args.length >= 2
                && (args[1].equalsIgnoreCase("ready") || args[1].equalsIgnoreCase("unready"))) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(org.bukkit.ChatColor.GRAY + "该命令只能由玩家使用");
                return true;
            }
            PvzMode pvz = manager.pvzMode();
            if (pvz == null) {
                player.sendMessage(org.bukkit.ChatColor.RED + "【PVZ】PVZ 模式未加载");
                return true;
            }
            boolean ready = args[1].equalsIgnoreCase("ready");
            boolean changed = pvz.setReady(player, ready);
            String message = ready
                    ? (changed ? "你已准备 PVZ（当前 " + pvz.readyCount() + " 人准备）" : "你已经在 PVZ 准备状态")
                    : (changed ? "你已取消 PVZ 准备" : "你本来就没在 PVZ 准备状态");
            player.sendMessage(org.bukkit.ChatColor.GREEN + "【PVZ】" + message);
            if (changed && ready) {
                Bukkit.broadcastMessage(org.bukkit.ChatColor.GREEN + "【PVZ】" + player.getName()
                        + " 已准备 PVZ（当前 " + pvz.readyCount() + " 人准备）");
            }
            return true;
        }
        if (!sender.hasPermission("davepve.admin")) {
            sender.sendMessage(org.bukkit.ChatColor.RED + "你没有权限使用此命令");
            return true;
        }
        switch (sub) {
            case "bind": {
                if (args.length < 2) {
                    sender.sendMessage(org.bukkit.ChatColor.GOLD + "用法: /davepve bind <队伍名> [半径]");
                    return true;
                }
                String team = args[1];
                double radius = args.length >= 3 ? parseDouble(args[2], 16.0) : 16.0;
                Player center = sender instanceof Player p ? p : null;
                int count = manager.bindNearby(team, radius, center);
                sender.sendMessage(org.bukkit.ChatColor.GOLD + "已绑定 " + count + " 个戴夫到队伍 " + manager.displayName(team));
                return true;
            }
            case "kill": {
                if (args.length < 2) {
                    sender.sendMessage(org.bukkit.ChatColor.GOLD + "用法: /davepve kill <队伍名>");
                    return true;
                }
                int count = manager.killTeamDave(args[1]);
                sender.sendMessage(org.bukkit.ChatColor.GOLD + "已杀死 " + manager.displayName(args[1]) + " 队的戴夫 " + count + " 个");
                return true;
            }
            case "killnearest": {
                if (!(sender instanceof Player killer)) {
                    sender.sendMessage(org.bukkit.ChatColor.GRAY + "该命令只能由玩家使用");
                    return true;
                }
                sender.sendMessage(manager.killNearestDave(killer));
                return true;
            }
            case "balance":
                sender.sendMessage(org.bukkit.ChatColor.GOLD + manager.balanceTeams());
                return true;
            case "start":
                manager.startGame();
                return true;
            case "end":
                manager.endGame();
                Bukkit.broadcastMessage(org.bukkit.ChatColor.GOLD + "【戴夫】游戏已被管理员结束");
                return true;
            case "wave": {
                if (args.length < 2) {
                    sender.sendMessage(org.bukkit.ChatColor.GOLD + "用法: /davepve wave <1-12>");
                    return true;
                }
                try {
                    int wave = Integer.parseInt(args[1]);
                    sender.sendMessage(org.bukkit.ChatColor.GOLD + manager.jumpToWave(wave));
                } catch (NumberFormatException e) {
                    sender.sendMessage(org.bukkit.ChatColor.RED + "波次必须是数字");
                }
                return true;
            }
            case "info":
                sender.sendMessage(org.bukkit.ChatColor.GOLD + manager.infoSummary());
                return true;
            case "pvz": {
                PvzMode pvz = manager.pvzMode();
                if (pvz == null) {
                    sender.sendMessage(org.bukkit.ChatColor.RED + "【PVZ】PVZ 模式未加载");
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(org.bukkit.ChatColor.GOLD
                            + "用法: /davepve pvz start|stop|status|setspawn|setbase|setplayer <队伍>");
                    return true;
                }
                String action = args[1].toLowerCase();
                switch (action) {
                    case "start":
                        pvz.startGame(sender);
                        return true;
                    case "stop":
                        pvz.stopGame(sender);
                        return true;
                    case "status":
                        sender.sendMessage(pvz.statusSummary());
                        return true;
                    case "setspawn":
                    case "setbase":
                    case "setplayer": {
                        if (args.length < 3) {
                            sender.sendMessage(org.bukkit.ChatColor.GOLD
                                    + "用法: /davepve pvz " + action + " <red|blue|yellow|green> [x y z]");
                            return true;
                        }
                        String team = args[2];
                        String result;
                        if (args.length >= 6) {
                            try {
                                double x = Double.parseDouble(args[3]);
                                double y = Double.parseDouble(args[4]);
                                double z = Double.parseDouble(args[5]);
                                String worldName = sender instanceof Player p
                                        ? p.getWorld().getName()
                                        : (Bukkit.getWorlds().isEmpty() ? "world" : Bukkit.getWorlds().get(0).getName());
                                result = switch (action) {
                                    case "setspawn" -> pvz.setLaneSpawn(team, worldName, x, y, z);
                                    case "setbase" -> pvz.setLaneBase(team, worldName, x, y, z);
                                    default -> pvz.setLanePlayerSpawn(team, worldName, x, y, z);
                                };
                            } catch (NumberFormatException e) {
                                sender.sendMessage(org.bukkit.ChatColor.RED + "坐标必须是数字");
                                return true;
                            }
                        } else if (sender instanceof Player admin) {
                            result = switch (action) {
                                case "setspawn" -> pvz.setLaneSpawn(team, admin);
                                case "setbase" -> pvz.setLaneBase(team, admin);
                                default -> pvz.setLanePlayerSpawn(team, admin);
                            };
                        } else {
                            sender.sendMessage(org.bukkit.ChatColor.RED
                                    + "控制台设置坐标请提供 x y z: /davepve pvz " + action + " " + team + " <x> <y> <z>");
                            return true;
                        }
                        sender.sendMessage(result);
                        return true;
                    }
                    default:
                        sender.sendMessage(org.bukkit.ChatColor.GRAY
                                + "未知 pvz 子命令，可用: start / stop / status / setspawn / setbase / setplayer");
                        return true;
                }
            }
            default:
                sender.sendMessage(org.bukkit.ChatColor.GRAY + "未知子命令，可用: ready / unready / balance / start / end / wave / bind / kill / killnearest / info");
                return true;
        }
    }

    private static double parseDouble(String text, double fallback) {
        try {
            return Double.parseDouble(text);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> subcommands = new ArrayList<>();
            if (sender.hasPermission("davepve.admin")) {
                subcommands.addAll(List.of("ready", "unready", "join", "balance", "start", "end", "wave", "bind", "kill", "killnearest", "info", "pvz"));
            } else {
                subcommands.addAll(List.of("ready", "unready", "join"));
            }
            return filter(subcommands, args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("pvz")) {
            List<String> pvzSub = new ArrayList<>(List.of("start", "stop", "status", "setspawn", "setbase", "setplayer"));
            if (!sender.hasPermission("davepve.admin")) {
                pvzSub.clear();
                pvzSub.add("ready");
                pvzSub.add("unready");
            }
            return filter(pvzSub, args[1]);
        }
        if (args.length == 3 && (args[1].equalsIgnoreCase("setspawn")
                || args[1].equalsIgnoreCase("setbase")
                || args[1].equalsIgnoreCase("setplayer"))) {
            List<String> teams = new ArrayList<>();
            for (Team team : Bukkit.getScoreboardManager().getMainScoreboard().getTeams()) {
                teams.add(team.getName());
            }
            return filter(teams, args[2]);
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("bind") || args[0].equalsIgnoreCase("kill"))) {
            List<String> teams = new ArrayList<>();
            for (Team team : Bukkit.getScoreboardManager().getMainScoreboard().getTeams()) {
                teams.add(team.getName());
            }
            return filter(teams, args[1]);
        }
        return List.of();
    }

    private static List<String> filter(List<String> options, String prefix) {
        String lower = prefix.toLowerCase(Locale.ROOT);
        List<String> result = new ArrayList<>();
        for (String option : options) {
            if (option.toLowerCase(Locale.ROOT).startsWith(lower)) {
                result.add(option);
            }
        }
        return result;
    }
}
