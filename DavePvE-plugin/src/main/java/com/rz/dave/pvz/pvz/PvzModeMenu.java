package com.rz.dave.pvz;
import com.rz.dave.DaveManager;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

/** 随机植物对战随机僵尸模式选择界面：准备 / 取消准备 / 规则说明 / 状态 / 返回。 */
public final class PvzModeMenu implements InventoryHolder {
    public static final String TITLE = "随机植物对战随机僵尸";
    public static final int READY_SLOT = 0;
    public static final int INFO_SLOT = 1;
    public static final int STATUS_SLOT = 4;
    public static final int UNREADY_SLOT = 7;
    public static final int BACK_SLOT = 8;

    private final Inventory inventory;

    public PvzModeMenu(Player player, DaveManager manager) {
        this.inventory = Bukkit.createInventory(this, 9, TITLE);
        PvzMode pvz = manager.pvzMode();
        boolean ready = pvz != null && pvz.isReady(player);
        inventory.setItem(READY_SLOT, button(ready ? Material.LIME_WOOL : Material.GREEN_WOOL,
                ready ? "已准备（PVZ）" : "准备（PVZ）",
                ready ? "点击取消后再重新准备" : "开局随机分配职业（剑士/弓箭手）与五条路之一"));
        inventory.setItem(INFO_SLOT, info(Material.BOOK, "模式规则",
                "一路 = 一个队伍，每队最多 5 人共同守路（一局最多 25 人）；"
                        + "守住基地（10 点生命），死亡不复活；一路全员阵亡即淘汰，"
                        + "坚持到最后获胜；开局后不能中途加入，玩家默认无法自然回血。"));
        if (pvz != null) {
            inventory.setItem(STATUS_SLOT, statusItem(pvz));
        }
        inventory.setItem(UNREADY_SLOT, button(Material.RED_WOOL, "取消准备",
                "取消本局 PVZ 准备状态"));
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

    private static ItemStack statusItem(PvzMode pvz) {
        String title;
        List<String> lore;
        if (pvz.isRunning()) {
            title = ChatColor.GREEN + "进行中";
            lore = List.of(ChatColor.GRAY + "PVZ 模式进行中，等待下一局开始");
        } else {
            title = ChatColor.GRAY + "未开始";
            lore = List.of(ChatColor.GRAY + "当前准备人数：" + pvz.readyCount(),
                    ChatColor.GRAY + "由管理员 /davepve pvz start 开局");
        }
        ItemStack stack = new ItemStack(Material.CLOCK);
        ItemMeta meta = stack.getItemMeta();
        meta.setItemName(title);
        meta.setLore(lore);
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
