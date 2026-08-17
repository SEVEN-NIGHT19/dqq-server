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

public final class ModeNormalMenu implements InventoryHolder {
    public static final String TITLE = "正常模式选择";
    public static final int READY_SLOT = 0;
    public static final int UNREADY_SLOT = 7;
    public static final int BACK_SLOT = 8;

    private final Inventory inventory;

    public ModeNormalMenu(Player player, DaveManager manager) {
        this.inventory = Bukkit.createInventory(this, 9, TITLE);
        inventory.setItem(READY_SLOT, button(Material.GREEN_WOOL, "准备（正常模式）",
                "点击后准备并自动投「正常模式」票"));
        inventory.setItem(1, info(Material.BOOK, "正常模式",
                "货币掉落与助攻系统正常，死亡后约 10 秒自动复活。"));
        inventory.setItem(UNREADY_SLOT, button(Material.RED_WOOL, "取消准备",
                "取消本局准备状态"));
        inventory.setItem(BACK_SLOT, backButton());
    }

    private static ItemStack button(Material material, String name, String lore) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        meta.setItemName(name);
        meta.setLore(List.of(ChatColor.GRAY + lore, ChatColor.YELLOW + "点击执行"));
        stack.setItemMeta(meta);
        return stack;
    }

    private static ItemStack info(Material material, String name, String lore) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        meta.setItemName(ChatColor.AQUA + name);
        meta.setLore(List.of(ChatColor.GRAY + lore));
        stack.setItemMeta(meta);
        return stack;
    }

    private static ItemStack backButton() {
        ItemStack stack = new ItemStack(Material.ARROW);
        ItemMeta meta = stack.getItemMeta();
        meta.setItemName("返回");
        meta.setLore(List.of(ChatColor.YELLOW + "返回玩游戏界面"));
        stack.setItemMeta(meta);
        return stack;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
