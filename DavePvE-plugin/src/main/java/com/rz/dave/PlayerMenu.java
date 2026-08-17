package com.rz.dave;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public final class PlayerMenu implements InventoryHolder {
    public static final String TITLE = "玩游戏";
    public static final int TEAM_SLOT = 0;
    public static final int MODE_NORMAL_SLOT = 1;
    public static final int MODE_DEATH_SLOT = 2;
    public static final int MODE_PVZ_SLOT = 3;
    public static final int BACK_SLOT = 7;
    public static final int SPECTATE_SLOT = 8;

    private final Inventory inventory;

    public PlayerMenu(Player player, DaveManager manager) {
        this.inventory = Bukkit.createInventory(this, 9, TITLE);
        inventory.setItem(TEAM_SLOT, button(Material.NAME_TAG, "选择队伍",
                manager.isReady(player) ? "选择你希望加入的队伍或随机" : "请先点击准备"));
        inventory.setItem(MODE_NORMAL_SLOT, button(Material.GREEN_WOOL, "正常模式选择",
                "进入正常模式界面，准备并投票"));
        inventory.setItem(MODE_DEATH_SLOT, button(Material.RED_WOOL, "死战模式选择",
                "进入死战模式界面，准备并投票经济局"));
        inventory.setItem(MODE_PVZ_SLOT, button(Material.GOLDEN_CARROT, "随机植物对战随机僵尸",
                "开局随机职业、随机分路（五条数字路），守路到最后获胜"));
        boolean spectator = player.getGameMode() == GameMode.SPECTATOR;
        inventory.setItem(SPECTATE_SLOT, button(spectator ? Material.ENDER_EYE : Material.SPYGLASS,
                spectator ? "退出旁观" : "旁观",
                spectator ? "当前状态：旁观者" : "选择观看游玩中的玩家"));
        inventory.setItem(BACK_SLOT, backButton());
    }

    private static ItemStack button(Material material, String name, String status) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        meta.setItemName(name);
        meta.setLore(List.of(ChatColor.GRAY + status));
        stack.setItemMeta(meta);
        return stack;
    }

    private static ItemStack backButton() {
        ItemStack stack = new ItemStack(Material.ARROW);
        ItemMeta meta = stack.getItemMeta();
        meta.setItemName("返回");
        meta.setLore(List.of(ChatColor.YELLOW + "返回主菜单"));
        stack.setItemMeta(meta);
        return stack;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
