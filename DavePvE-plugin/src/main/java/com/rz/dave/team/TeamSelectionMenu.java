package com.rz.dave.team;
import com.rz.dave.DaveManager;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public final class TeamSelectionMenu implements InventoryHolder {
    public static final int RED_SLOT = 0;
    public static final int BLUE_SLOT = 1;
    public static final int YELLOW_SLOT = 2;
    public static final int GREEN_SLOT = 3;
    public static final int RANDOM_SLOT = 4;
    public static final int BACK_SLOT = 8;

    private final Inventory inventory;

    public TeamSelectionMenu(DaveManager manager) {
        this.inventory = Bukkit.createInventory(this, 9, "选择队伍");
        inventory.setItem(RED_SLOT, team(Material.RED_WOOL, "红队", manager.teamPreferenceCount("red")));
        inventory.setItem(BLUE_SLOT, team(Material.BLUE_WOOL, "蓝队", manager.teamPreferenceCount("blue")));
        inventory.setItem(YELLOW_SLOT, team(Material.YELLOW_WOOL, "黄队", manager.teamPreferenceCount("yellow")));
        inventory.setItem(GREEN_SLOT, team(Material.GREEN_WOOL, "绿队", manager.teamPreferenceCount("green")));
        inventory.setItem(RANDOM_SLOT, randomButton());
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta backMeta = back.getItemMeta();
        backMeta.setItemName("返回");
        backMeta.setLore(List.of(ChatColor.YELLOW + "返回玩游戏菜单"));
        back.setItemMeta(backMeta);
        inventory.setItem(BACK_SLOT, back);
    }

    private static ItemStack team(Material material, String name, int count) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        meta.setItemName(name);
        meta.setLore(List.of(
                ChatColor.GRAY + "目前选择该队玩家数：" + count + "/5",
                ChatColor.YELLOW + "点击选择该队伍"));
        stack.setItemMeta(meta);
        return stack;
    }

    private static ItemStack randomButton() {
        ItemStack stack = new ItemStack(Material.STRUCTURE_VOID);
        ItemMeta meta = stack.getItemMeta();
        meta.setItemName("随机");
        meta.setLore(List.of(ChatColor.YELLOW + "点击选择随机分配"));
        stack.setItemMeta(meta);
        return stack;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
