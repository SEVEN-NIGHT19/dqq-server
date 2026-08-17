package com.rz.dave;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public final class GameplayInfoMenu implements InventoryHolder {
    public static final String TITLE = "模式玩法介绍";
    public static final int BACK_SLOT = 8;

    private final Inventory inventory;

    public GameplayInfoMenu() {
        this.inventory = Bukkit.createInventory(this, 9, TITLE);
        ItemStack book = new ItemStack(Material.WRITABLE_BOOK);
        ItemMeta bookMeta = book.getItemMeta();
        bookMeta.setItemName(ChatColor.GOLD + "斗蛐蛐 PvE 玩法");
        bookMeta.setLore(List.of(
                ChatColor.WHITE + "1. 进服先点“准备”，自动分队（最多 20 人、红蓝黄绿 4 队），可自选队伍。",
                ChatColor.WHITE + "2. 守护本队戴夫，抵御 12 波怪物（含小boss/大boss波）；击杀掉落货币自动入包，助攻(>10伤)得半额，小boss 掉星币。",
                ChatColor.WHITE + "3. 每造成 1 点伤害累计 1 分、击杀 +20，局末公屏排名。",
                ChatColor.WHITE + "4. 右键戴夫打开商店；“装备升级”内升级材质、附魔与武器攻击力（10 级后每次 2 钻）。",
                ChatColor.WHITE + "5. 武器攻击力 5/10/15 级解锁劣化/原版/终极技能（剑/斧右键，弓/弩/三叉戟/长矛/重锤按 Q，仙人掌 Q/F）。"));
        book.setItemMeta(bookMeta);
        inventory.setItem(0, book);
        ItemStack extra = new ItemStack(Material.PAPER);
        ItemMeta extraMeta = extra.getItemMeta();
        extraMeta.setItemName(ChatColor.GOLD + "武器与模式补充");
        extraMeta.setLore(List.of(
                ChatColor.WHITE + "6. 射手分火/普/冰/狙击：普豌可升到机机射手、真正的机枪射手（随机爆发 80 发）；狙击左键发射、开镜按 F。",
                ChatColor.WHITE + "7. 蘑菇武器：大/小/胆小菇可升级伤害，寒冰菇大范围冻结；只伤怪物、穿透友方。",
                ChatColor.WHITE + "8. 星币商店效果本局永久；戴夫阵亡淘汰、打完所有波次获胜；死亡在戴夫旁复活。",
                ChatColor.WHITE + "9. 死战模式：经济局 15/30/50 钻（默认中）、无怪物货币、阵亡旁观、休整期复活、开局休整 1 分钟、刷怪间隔 20/15/8 秒。",
                ChatColor.WHITE + "10. 模式入口：在「玩游戏」→ 正常/死战界面准备并投票，中途可随时点准备加入。"));
        extra.setItemMeta(extraMeta);
        inventory.setItem(1, extra);
        inventory.setItem(BACK_SLOT, backButton());
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
