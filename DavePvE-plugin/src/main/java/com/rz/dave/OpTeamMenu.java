package com.rz.dave;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.ArrayList;
import java.util.List;

public final class OpTeamMenu implements InventoryHolder {
    public static final String TITLE = "选择队伍";
    public static final int BACK_SLOT = 26;

    private final Inventory inventory;
    private final boolean bindMode;
    private final List<String> teamNames = new ArrayList<>();

    public OpTeamMenu(boolean bindMode) {
        this.bindMode = bindMode;
        this.inventory = Bukkit.createInventory(this, 27, TITLE);
        Scoreboard main = Bukkit.getScoreboardManager().getMainScoreboard();
        int slot = 0;
        for (Team team : main.getTeams()) {
            if (slot >= BACK_SLOT) {
                break;
            }
            teamNames.add(team.getName());
            ItemStack stack = new ItemStack(Material.PLAYER_HEAD);
            ItemMeta meta = stack.getItemMeta();
            meta.setItemName(team.getDisplayName() == null ? team.getName() : team.getDisplayName());
            meta.setLore(List.of(
                    ChatColor.GRAY + (bindMode ? "/davepve bind " + team.getName() + " 16" : "/davepve kill " + team.getName()),
                    ChatColor.YELLOW + "左键执行"));
            stack.setItemMeta(meta);
            inventory.setItem(slot++, stack);
        }
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta backMeta = back.getItemMeta();
        backMeta.setItemName("返回");
        backMeta.setLore(List.of(ChatColor.YELLOW + "返回管理命令"));
        back.setItemMeta(backMeta);
        inventory.setItem(BACK_SLOT, back);
    }

    public boolean bindMode() {
        return bindMode;
    }

    public String teamForSlot(int slot) {
        return slot >= 0 && slot < teamNames.size() ? teamNames.get(slot) : null;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
