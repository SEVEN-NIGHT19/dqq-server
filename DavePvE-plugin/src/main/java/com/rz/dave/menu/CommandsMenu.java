package com.rz.dave.menu;
import com.rz.dave.DaveManager;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.entity.Player;

import java.util.List;

public final class CommandsMenu implements InventoryHolder {
    public static final String TITLE = "常用指令表";
    public static final int BACK_SLOT = 8;

    private final Inventory inventory;

    public CommandsMenu() {
        this(null);
    }

    public CommandsMenu(Player viewer) {
        this.inventory = Bukkit.createInventory(this, 9, TITLE);
        inventory.setItem(0, commandItem("/davepve ready", "进入准备状态"));
        inventory.setItem(1, commandItem("/davepve unready", "取消准备状态"));
        inventory.setItem(2, commandItem("/cf", "打开主菜单"));
        inventory.setItem(3, commandItem("/lb", "返回大厅（游玩中退出本局）"));
        if (viewer != null && viewer.hasPermission("davepve.admin")) {
            inventory.setItem(4, commandItem("/davepve pvz", "PVZ 模式管理（管理员）：start/stop/status"));
        }
        inventory.setItem(5, noteItem("右键快捷栏最后的“主菜单”钟也可打开菜单。"));
        inventory.setItem(BACK_SLOT, backButton());
    }

    private static ItemStack commandItem(String command, String description) {
        ItemStack stack = new ItemStack(Material.COMMAND_BLOCK);
        ItemMeta meta = stack.getItemMeta();
        meta.setItemName(ChatColor.AQUA + command);
        meta.setLore(List.of(ChatColor.GRAY + description));
        stack.setItemMeta(meta);
        return stack;
    }

    private static ItemStack noteItem(String text) {
        ItemStack stack = new ItemStack(Material.PAPER);
        ItemMeta meta = stack.getItemMeta();
        meta.setItemName(ChatColor.YELLOW + "提示");
        meta.setLore(List.of(ChatColor.GRAY + text));
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
