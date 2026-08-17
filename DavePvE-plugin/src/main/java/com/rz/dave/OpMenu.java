package com.rz.dave;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public final class OpMenu implements InventoryHolder {
    public static final String TITLE = "管理菜单";
    public static final int GAME_CTRL_SLOT = 0;
    public static final int TEAM_SLOT = 1;
    public static final int SPAWN_SLOT = 2;
    public static final int BOSS_SLOT = 3;

    private final Inventory inventory;

    public OpMenu() {
        this.inventory = Bukkit.createInventory(this, 27, TITLE);
        inventory.setItem(GAME_CTRL_SLOT, entry(Material.ENDER_EYE, "游戏控制", "开始 / 结束 / 状态 / PVP"));
        inventory.setItem(TEAM_SLOT, entry(Material.NAME_TAG, "队伍与戴夫", "分队 / 绑定 / 击杀 / 生成戴夫"));
        inventory.setItem(SPAWN_SLOT, entry(Material.ARMOR_STAND, "刷怪点", "创建 / 删除 / 列表 / 开始 / 停止"));
        inventory.setItem(BOSS_SLOT, entry(Material.ZOMBIE_HEAD, "boss点", "创建 / 删除 / 列表"));
    }

    private static ItemStack entry(Material material, String name, String lore) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        meta.setItemName(name);
        meta.setLore(List.of(ChatColor.GRAY + lore, ChatColor.YELLOW + "点击进入"));
        stack.setItemMeta(meta);
        return stack;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
