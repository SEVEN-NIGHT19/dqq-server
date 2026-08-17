package com.rz.dave.menu;
import com.rz.dave.DaveManager;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;

import java.util.ArrayList;
import java.util.List;

public final class EquipmentCatalog {
    private EquipmentCatalog() {
    }

    public static final int SHOOTER_FIRE_SLOT = 12;
    public static final int SHOOTER_NORMAL_SLOT = 13;
    public static final int SHOOTER_ICE_SLOT = 14;
    public static final int SHOOTER_SNIPER_SLOT = 15;

    public enum Kind {
        SWORD("剑", Material.IRON_SWORD, true),
        AXE("斧", Material.IRON_AXE, true),
        SPEAR("长矛", Material.IRON_SPEAR, true),
        BOW("弓", Material.BOW, false),
        CROSSBOW("弩", Material.CROSSBOW, false),
        TRIDENT("三叉戟", Material.TRIDENT, false),
        MACE("风暴重锤", Material.MACE, false),
        SHOOTER("射手", Material.DISPENSER, true),
        CACTUS_SHOOTER("仙人掌射手", Material.DISPENSER, false),
        BIG_PUFFSHROOM("大喷菇", Material.AMETHYST_CLUSTER, false),
        SMALL_PUFFSHROOM("小喷菇", Material.BROWN_MUSHROOM, false),
        TIMID_SHROOM("胆小菇", Material.AMETHYST_SHARD, false),
        HELMET("头盔", Material.IRON_HELMET, true),
        CHESTPLATE("胸甲", Material.IRON_CHESTPLATE, true),
        LEGGINGS("护腿", Material.IRON_LEGGINGS, true),
        BOOTS("靴子", Material.IRON_BOOTS, true);

        private final String display;
        private final Material icon;
        private final boolean materialUpgrade;

        Kind(String display, Material icon, boolean materialUpgrade) {
            this.display = display;
            this.icon = icon;
            this.materialUpgrade = materialUpgrade;
        }

        public String display() {
            return display;
        }

        public Material icon() {
            return icon;
        }

        public boolean hasMaterialUpgrade() {
            return materialUpgrade;
        }
    }

    public record EnchantEntry(Enchantment enchantment, int basePriceSilver, int maxLevel) {
    }

    private static final EnchantEntry SHARPNESS = new EnchantEntry(Enchantment.SHARPNESS, 20, 5);
    private static final EnchantEntry SMITE = new EnchantEntry(Enchantment.SMITE, 30, 5);
    private static final EnchantEntry BANE = new EnchantEntry(Enchantment.BANE_OF_ARTHROPODS, 10, 5);
    private static final EnchantEntry FIRE_ASPECT = new EnchantEntry(Enchantment.FIRE_ASPECT, 30, 2);
    private static final EnchantEntry KNOCKBACK = new EnchantEntry(Enchantment.KNOCKBACK, 30, 2);
    private static final EnchantEntry SWEEPING = new EnchantEntry(Enchantment.SWEEPING_EDGE, 30, 3);
    private static final EnchantEntry LUNGE = new EnchantEntry(Enchantment.LUNGE, 10, 3);
    private static final EnchantEntry PROTECTION = new EnchantEntry(Enchantment.PROTECTION, 20, 4);
    private static final EnchantEntry BLAST_PROTECTION = new EnchantEntry(Enchantment.BLAST_PROTECTION, 10, 4);
    private static final EnchantEntry PROJECTILE_PROTECTION = new EnchantEntry(Enchantment.PROJECTILE_PROTECTION, 10, 4);
    private static final EnchantEntry THORNS = new EnchantEntry(Enchantment.THORNS, 10, 3);
    private static final EnchantEntry SWIFT_SNEAK = new EnchantEntry(Enchantment.SWIFT_SNEAK, 3, 3);
    private static final EnchantEntry FEATHER_FALLING = new EnchantEntry(Enchantment.FEATHER_FALLING, 30, 4);

    private static final EnchantEntry POWER = new EnchantEntry(Enchantment.POWER, 10, 5);
    private static final EnchantEntry PUNCH = new EnchantEntry(Enchantment.PUNCH, 30, 2);
    private static final EnchantEntry FLAME = new EnchantEntry(Enchantment.FLAME, 50, 1);
    private static final EnchantEntry INFINITY = new EnchantEntry(Enchantment.INFINITY, 50, 1);
    private static final EnchantEntry PIERCING = new EnchantEntry(Enchantment.PIERCING, 20, 4);
    private static final EnchantEntry QUICK_CHARGE = new EnchantEntry(Enchantment.QUICK_CHARGE, 20, 3);
    private static final EnchantEntry MULTISHOT = new EnchantEntry(Enchantment.MULTISHOT, 50, 1);
    private static final EnchantEntry LOYALTY = new EnchantEntry(Enchantment.LOYALTY, 10, 3);
    private static final EnchantEntry IMPALING = new EnchantEntry(Enchantment.IMPALING, 10, 5);
    private static final EnchantEntry CHANNELING = new EnchantEntry(Enchantment.CHANNELING, 50, 1);
    private static final EnchantEntry DENSITY = new EnchantEntry(Enchantment.DENSITY, 20, 5);
    private static final EnchantEntry BREACH = new EnchantEntry(Enchantment.BREACH, 20, 4);
    private static final EnchantEntry WIND_BURST = new EnchantEntry(Enchantment.WIND_BURST, 30, 3);

    public static List<EnchantEntry> enchantsFor(Kind kind) {
        List<EnchantEntry> list = new ArrayList<>();
        switch (kind) {
            case SWORD -> {
                list.add(SHARPNESS);
                list.add(SMITE);
                list.add(BANE);
                list.add(FIRE_ASPECT);
                list.add(KNOCKBACK);
                list.add(SWEEPING);
            }
            case SPEAR -> {
                list.add(SHARPNESS);
                list.add(SMITE);
                list.add(BANE);
                list.add(FIRE_ASPECT);
                list.add(KNOCKBACK);
                list.add(SWEEPING);
                list.add(LUNGE);
            }
            case AXE -> {
                list.add(SHARPNESS);
                list.add(SMITE);
                list.add(BANE);
                list.add(FIRE_ASPECT);
                list.add(KNOCKBACK);
            }
            case BOW -> {
                list.add(POWER);
                list.add(PUNCH);
                list.add(FLAME);
                list.add(INFINITY);
            }
            case CROSSBOW -> {
                list.add(PIERCING);
                list.add(QUICK_CHARGE);
                list.add(MULTISHOT);
                list.add(INFINITY);
            }
            case TRIDENT -> {
                list.add(LOYALTY);
                list.add(IMPALING);
                list.add(CHANNELING);
            }
            case MACE -> {
                list.add(DENSITY);
                list.add(BREACH);
                list.add(WIND_BURST);
            }
            case HELMET, CHESTPLATE, LEGGINGS -> {
                list.add(PROTECTION);
                list.add(BLAST_PROTECTION);
                list.add(PROJECTILE_PROTECTION);
                list.add(THORNS);
                if (kind == Kind.LEGGINGS) {
                    list.add(SWIFT_SNEAK);
                }
            }
            case BOOTS -> {
                list.add(PROTECTION);
                list.add(BLAST_PROTECTION);
                list.add(PROJECTILE_PROTECTION);
                list.add(THORNS);
                list.add(FEATHER_FALLING);
            }
            case SHOOTER -> { }
        }
        return list;
    }

    public static int enchantPrice(EnchantEntry entry, int targetLevel) {
        return entry.basePriceSilver() * (1 << (targetLevel - 1));
    }
}
