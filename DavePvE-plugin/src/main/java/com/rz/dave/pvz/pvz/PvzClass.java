package com.rz.dave.pvz;

import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;

import java.util.concurrent.ThreadLocalRandom;

/**
 * PVZ（随机植物对战随机僵尸）模式职业。
 *
 * <p>武器均为 Dispenser 造型的发射器，射击参数（伤害/冷却/冰系特效）统一集中在
 * {@link PvzMode}，本类只负责外观（武器/护甲）与职业标签；
 * 护甲一律为对应颜色的皮革套装，并由模式统一设置为无限耐久（PVZ 模式护甲默认不损耗）。
 */
public enum PvzClass {
    MACHINE_GUNNER("机枪射手", PvzClassArmor.LIME),
    ICE_SHOOTER("寒冰射手", PvzClassArmor.LIGHT_BLUE),
    WALLNUT("坚果", PvzClassArmor.BROWN);

    private final String display;
    private final PvzClassArmor armor;

    PvzClass(String display, PvzClassArmor armor) {
        this.display = display;
        this.armor = armor;
    }

    public String displayName() {
        return display;
    }

    /** 是否为发射器职业（手持武器右键射击）。 */
    public boolean isShooter() {
        return this != WALLNUT;
    }

    /** 是否有武器（坚果没有武器）。 */
    public boolean hasWeapon() {
        return isShooter();
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

    /** 构造该职业的整套物品（发射器武器；坚果无武器）。PDC 射击标记由 PvzMode 开局时补充。 */
    public ItemStack[] createKit() {
        if (this == WALLNUT) {
            return new ItemStack[0];
        }
        ItemStack main = new ItemStack(Material.DISPENSER);
        ItemMeta meta = main.getItemMeta();
        meta.setItemName(ChatColor.AQUA + display);
        meta.setUnbreakable(true);
        main.setItemMeta(meta);
        return new ItemStack[]{main};
    }

    /** 该职业的护甲（对应颜色染色皮革套，无限耐久）。 */
    public ItemStack[] createArmor() {
        ItemStack helmet = dyed(Material.LEATHER_HELMET);
        ItemStack chest = dyed(Material.LEATHER_CHESTPLATE);
        ItemStack legs = dyed(Material.LEATHER_LEGGINGS);
        ItemStack boots = dyed(Material.LEATHER_BOOTS);
        return new ItemStack[]{boots, legs, chest, helmet};
    }

    private ItemStack dyed(Material piece) {
        ItemStack stack = new ItemStack(piece);
        LeatherArmorMeta meta = (LeatherArmorMeta) stack.getItemMeta();
        meta.setColor(armor.color);
        meta.setUnbreakable(true);
        stack.setItemMeta(meta);
        return stack;
    }

    /** 各职业护甲染色（对应染料色：浅绿 / 浅蓝 / 棕）。 */
    private enum PvzClassArmor {
        LIME(Color.fromRGB(0x7FCC19)),
        LIGHT_BLUE(Color.fromRGB(0x3AB3DA)),
        BROWN(Color.fromRGB(0x835432));

        private final Color color;

        PvzClassArmor(Color color) {
            this.color = color;
        }
    }
}