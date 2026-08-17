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

public final class ModeDeathMenu implements InventoryHolder {
    public static final String TITLE = "死战模式选择";
    public static final int READY_SLOT = 0;
    public static final int ECONOMY_LOW_SLOT = 2;
    public static final int ECONOMY_MID_SLOT = 3;
    public static final int ECONOMY_HIGH_SLOT = 4;
    public static final int UNREADY_SLOT = 7;
    public static final int BACK_SLOT = 8;

    private final Inventory inventory;

    public ModeDeathMenu(Player player, DaveManager manager) {
        this.inventory = Bukkit.createInventory(this, 9, TITLE);
        inventory.setItem(READY_SLOT, button(Material.RED_WOOL, "准备（死战模式）",
                "点击后准备并自动投「死战模式」票"));
        inventory.setItem(1, info(Material.BOOK, "死战模式",
                "开局发放初始钻币、无怪物货币掉落（星币除外），阵亡后休整期复活。"));
        Integer myEconomy = manager.economyVoteOf(player);
        inventory.setItem(ECONOMY_LOW_SLOT, economyButton(Material.IRON_NUGGET, "低经济局（15 钻）",
                manager.lowEconomyVotes(), myEconomy != null && myEconomy == 0));
        inventory.setItem(ECONOMY_MID_SLOT, economyButton(Material.GOLD_NUGGET, "中经济局（30 钻）",
                manager.midEconomyVotes(), myEconomy != null && myEconomy == 1));
        inventory.setItem(ECONOMY_HIGH_SLOT, economyButton(Material.DIAMOND, "高经济局（50 钻）",
                manager.highEconomyVotes(), myEconomy != null && myEconomy == 2));
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

    private static ItemStack economyButton(Material material, String name, int votes, boolean mine) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        meta.setItemName(ChatColor.AQUA + name);
        meta.setLore(List.of(
                ChatColor.GRAY + "当前票数：" + votes,
                ChatColor.GRAY + (mine ? "你的选择：是" : "你的选择：否"),
                ChatColor.YELLOW + "点击投票"));
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
