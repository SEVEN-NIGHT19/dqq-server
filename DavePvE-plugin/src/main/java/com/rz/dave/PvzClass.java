package com.rz.dave;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.concurrent.ThreadLocalRandom;

/**
 * PVZ 模式职业。目前两种：剑士（铁剑）/ 弓箭手（弓 + 无限附魔 + 1 支箭）。
 * 严格按需求：裸装单武器，无护甲无食物；PVZ 模式无经济/商店/技能。
 */
public enum PvzClass {
    SWORDSMAN("剑士", Material.IRON_SWORD, false),
    ARCHER("弓箭手", Material.BOW, true);

    private final String display;
    private final Material weapon;
    private final boolean ranged;

    PvzClass(String display, Material weapon, boolean ranged) {
        this.display = display;
        this.weapon = weapon;
        this.ranged = ranged;
    }

    public String displayName() {
        return display;
    }

    public boolean isRanged() {
        return ranged;
    }

    /** 开局随机职业，允许重复。 */
    public static PvzClass random() {
        return fromRandomIndex(ThreadLocalRandom.current().nextInt(values().length));
    }

    /** 从随机索引取职业；索引不绑定玩家，天然支持有放回重复分配。 */
    public static PvzClass fromRandomIndex(int index) {
        PvzClass[] classes = values();
        return classes[Math.floorMod(index, classes.length)];
    }

    /** 构造该职业的整套物品（只有武器本身，弓箭手附带 1 支箭配合无限附魔）。 */
    public ItemStack[] createKit() {
        ItemStack main = new ItemStack(weapon);
        ItemMeta meta = main.getItemMeta();
        meta.setItemName(ChatColor.AQUA + display + (ranged ? "之弓" : "之剑"));
        meta.setUnbreakable(true);
        if (ranged) {
            meta.addEnchant(Enchantment.INFINITY, 1, true);
        }
        main.setItemMeta(meta);
        if (ranged) {
            return new ItemStack[]{main, new ItemStack(Material.ARROW, 1)};
        }
        return new ItemStack[]{main};
    }
}
