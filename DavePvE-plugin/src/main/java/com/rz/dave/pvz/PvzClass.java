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
 * <p>武器外观（发射器/狙击镜）与护甲外观在此定义，射击参数（伤害/冷却/特效）
 * 统一集中在 {@link PvzMode}；护甲一律为对应颜色的皮革套装，由模式统一设置为
 * 无限耐久（PVZ 模式护甲默认不损耗）。各职业攻击方式统一为右键发射。
 */
public enum PvzClass {
    MACHINE_GUNNER("机枪射手", PvzClassArmor.LIME),
    ICE_SHOOTER("寒冰射手", PvzClassArmor.LIGHT_BLUE),
    WALLNUT("坚果", PvzClassArmor.BROWN),
    SNIPER("狙击豌豆", PvzClassArmor.LIME_BLACK_HAT),
    DUAL_SHOOTER("双发射手", PvzClassArmor.PALE_GREEN_DARK_HAT),
    SMALL_PUFF("小喷菇", PvzClassArmor.PURPLE_WHITE_LIMBS),
    SEA_MUSHROOM("海蘑菇", PvzClassArmor.DARK_GREEN_BLACK_LIMBS);

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

    /**
     * 开局随机职业：每名玩家分配时独立调用一次（A 玩家的职业不影响 B 玩家），
     * 从职业池中等概率随机一个，允许重复（连续同职业是正常随机现象）。
     */
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
        Material base = switch (this) {
            case SNIPER -> Material.SPYGLASS;
            case SMALL_PUFF -> Material.BROWN_MUSHROOM;
            case SEA_MUSHROOM -> Material.WARPED_FUNGUS;
            default -> Material.DISPENSER;
        };
        ItemStack main = new ItemStack(base);
        ItemMeta meta = main.getItemMeta();
        meta.setItemName(ChatColor.AQUA + display);
        meta.setUnbreakable(true);
        main.setItemMeta(meta);
        return new ItemStack[]{main};
    }

    /** 该职业的护甲（对应颜色染色皮革套，无限耐久；头盔/护腿/鞋子色可独立覆写）。 */
    public ItemStack[] createArmor() {
        ItemStack helmet = dyed(Material.LEATHER_HELMET, armor.helmet != null ? armor.helmet : armor.color);
        ItemStack chest = dyed(Material.LEATHER_CHESTPLATE, armor.color);
        ItemStack legs = dyed(Material.LEATHER_LEGGINGS, armor.legs != null ? armor.legs : armor.color);
        ItemStack boots = dyed(Material.LEATHER_BOOTS, armor.boots != null ? armor.boots : armor.color);
        return new ItemStack[]{boots, legs, chest, helmet};
    }

    private ItemStack dyed(Material piece, Color color) {
        ItemStack stack = new ItemStack(piece);
        LeatherArmorMeta meta = (LeatherArmorMeta) stack.getItemMeta();
        meta.setColor(color);
        meta.setUnbreakable(true);
        stack.setItemMeta(meta);
        return stack;
    }

    /** 各职业护甲染色（头盔/护腿/鞋子可单独配色；null 表示与套装同色）。 */
    private enum PvzClassArmor {
        LIME(Color.fromRGB(0x7FCC19), null, null, null),                 // 机枪：浅绿
        LIGHT_BLUE(Color.fromRGB(0x3AB3DA), null, null, null),           // 寒冰：浅蓝
        BROWN(Color.fromRGB(0x835432), null, null, null),                // 坚果：棕
        LIME_BLACK_HAT(Color.fromRGB(0x7FCC19), Color.fromRGB(0x000000), null, null),  // 狙击：浅绿套 + 黑帽
        PALE_GREEN_DARK_HAT(Color.fromRGB(0x9FE22E), Color.fromRGB(0x1E7F1E), null, null), // 双发：淡绿套 + 深绿帽
        PURPLE_WHITE_LIMBS(Color.fromRGB(0x8E44AD), null,
                Color.fromRGB(0xFFFFFF), Color.fromRGB(0xFFFFFF)),        // 小喷菇：紫帽胸 + 白腿鞋
        DARK_GREEN_BLACK_LIMBS(Color.fromRGB(0x1B5E20), null,
                Color.fromRGB(0x000000), Color.fromRGB(0x000000));        // 海蘑菇：深绿帽胸 + 黑腿鞋

        private final Color color;
        private final Color helmet;
        private final Color legs;
        private final Color boots;

        PvzClassArmor(Color color, Color helmet, Color legs, Color boots) {
            this.color = color;
            this.helmet = helmet;
            this.legs = legs;
            this.boots = boots;
        }
    }
}