package com.rz.dave;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public final class MainMenu implements InventoryHolder {
    public static final String TITLE = "主菜单";
    public static final int PLAY_SLOT = 0;
    public static final int INFO_SLOT = 1;
    public static final int COMMANDS_SLOT = 2;
    public static final int MANAGE_SLOT = 4;

    private final Inventory inventory;

    public MainMenu(Player player, DaveManager manager) {
        this.inventory = Bukkit.createInventory(this, 9, TITLE);
        inventory.setItem(PLAY_SLOT, entry(Material.GRASS_BLOCK, "玩游戏", "准备 / 不准备 / 旁观"));
        inventory.setItem(INFO_SLOT, entry(Material.BOOK, "模式玩法介绍", "了解斗蛐蛐 PvE 的规则"));
        inventory.setItem(COMMANDS_SLOT, entry(Material.PAPER, "常用指令表", "查看普通玩家可用的指令"));
        if (player.isOp()) {
            inventory.setItem(MANAGE_SLOT, entry(Material.NETHER_STAR, "管理菜单", "管理员功能"));
        }
    }

    private static ItemStack entry(Material material, String name, String lore) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        meta.setItemName(name);
        meta.setLore(List.of(ChatColor.GRAY + lore));
        stack.setItemMeta(meta);
        return stack;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
