package com.rz.dave.monster;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.persistence.PersistentDataType;

/**
 * 自定义怪物管理器：统一注册与生成自定义怪物。
 *
 * <p>PVZ（随机植物对战随机僵尸）模式用到的怪物集中在这里配置，
 * 后续新增怪物（精英怪、Boss 等）也在此注册，便于统一维护
 * 体型、标签、血量与攻击等属性。
 *
 * <p>掉落规则：所有带 {@link #TAG_MONSTER} 标签的怪物死亡时由
 * PVZ 死亡监听统一清空掉落物（击杀无任何掉落）。
 */
public final class MonsterManager {

    /** PVZ 怪物归属标签（与 {@code PvzMode.TAG_MONSTER} 保持一致，常量转发避免漂移）。 */
    public static final String TAG_MONSTER = "pvz_monster";
    /** 盲盒僵尸专属标签：死亡时触发随机召唤。 */
    public static final String TAG_BLINDBOX = "pvz_blindbox";
    /** 盲盒僵尸死亡后召唤出的怪物标签。 */
    public static final String TAG_SUMMON = "pvz_summon";

    /** 巨人僵尸：体型为原版僵尸的倍数（1.21 实体 scale 属性）。 */
    public static final double GIANT_ZOMBIE_SCALE = 4.0;
    /** 巨人僵尸：血量按原版僵尸血量放大倍数。 */
    public static final double GIANT_ZOMBIE_HEALTH_MULTIPLIER = 4.0;
    /** 原版僵尸基础血量（用于巨人僵尸血量计算）。 */
    public static final double BASE_ZOMBIE_HEALTH = 20.0;

    /** 怪物种类注册表（供生成入口与后续扩展使用）。 */
    public enum MonsterType {
        BLIND_BOX_ZOMBIE("盲盒僵尸"),
        PLAIN_ZOMBIE("原版僵尸"),
        GIANT_ZOMBIE("巨人僵尸"),
        SUMMON_CREEPER("苦力怕");

        private final String displayName;

        MonsterType(String displayName) {
            this.displayName = displayName;
        }

        public String displayName() {
            return displayName;
        }
    }

    private MonsterManager() {
    }

    /** 判断实体是否为 PVZ 归属怪物（带 {@value #TAG_MONSTER} 标签）。 */
    public static boolean isPvzMonster(Entity entity) {
        return entity != null && entity.getScoreboardTags().contains(TAG_MONSTER);
    }

    /**
     * 生成盲盒僵尸：戴着盲盒头盔的普通僵尸，不惧阳光（昼间不燃烧），
     * 血量随波次成长，死亡后由 PVZ 逻辑随机召唤其他怪物。
     */
    public static Zombie spawnBlindBoxZombie(World world, Location loc, String laneId,
                                             NamespacedKey laneKey, double maxHp,
                                             double attackMultiplier) {
        Zombie zombie = world.spawn(loc, Zombie.class, z -> {
            z.setBaby(false);
            z.setCanPickupItems(false);
            z.setShouldBurnInDay(false);            // 不受阳光灼烧
            z.getEquipment().clear();
            z.getEquipment().setHelmet(blindBoxHelmet());
            z.getEquipment().setHelmetDropChance(0.0f);
            z.setCustomName(ChatColor.DARK_RED + "盲盒僵尸");
            z.setCustomNameVisible(true);
            z.addScoreboardTag(TAG_MONSTER);
            z.addScoreboardTag(TAG_BLINDBOX);
            z.setRemoveWhenFarAway(false);
            z.setPersistent(true);
            z.getPersistentDataContainer().set(laneKey, PersistentDataType.STRING, laneId);
        });
        if (zombie != null) {
            applyHealthAndAttack(zombie, maxHp, attackMultiplier);
        }
        return zombie;
    }

    /**
     * 生成召唤系僵尸（盲盒僵尸死亡后的产物）：PVZ 标签、昼间不燃。
     *
     * @param giant 是否为巨人僵尸（原版僵尸 4 倍体型、4 倍血量）
     */
    public static Zombie spawnSummonZombie(World world, Location loc, String laneId,
                                           NamespacedKey laneKey, boolean giant) {
        Zombie zombie = world.spawn(loc, Zombie.class, z -> {
            z.setBaby(false);
            z.setCanPickupItems(false);
            z.setShouldBurnInDay(false);            // 不受阳光灼烧
            z.getEquipment().clear();
            if (giant) {
                AttributeInstance scale = z.getAttribute(Attribute.SCALE);
                if (scale != null) {
                    scale.setBaseValue(GIANT_ZOMBIE_SCALE);
                }
                z.setCustomName(ChatColor.DARK_RED + "巨人僵尸");
                z.setCustomNameVisible(true);
            }
            z.addScoreboardTag(TAG_MONSTER);
            z.addScoreboardTag(TAG_SUMMON);
            z.setRemoveWhenFarAway(false);
            z.setPersistent(true);
            z.getPersistentDataContainer().set(laneKey, PersistentDataType.STRING, laneId);
        });
        if (zombie != null && giant) {
            double hp = BASE_ZOMBIE_HEALTH * GIANT_ZOMBIE_HEALTH_MULTIPLIER;
            zombie.setMaxHealth(hp);
            zombie.setHealth(hp);
        }
        return zombie;
    }

    /** 生成召唤系苦力怕（盲盒僵尸死亡后的产物）：PVZ 标签。 */
    public static Creeper spawnSummonCreeper(World world, Location loc, String laneId,
                                             NamespacedKey laneKey) {
        return world.spawn(loc, Creeper.class, c -> {
            c.setCanPickupItems(false);
            c.addScoreboardTag(TAG_MONSTER);
            c.addScoreboardTag(TAG_SUMMON);
            c.setRemoveWhenFarAway(false);
            c.setPersistent(true);
            c.getPersistentDataContainer().set(laneKey, PersistentDataType.STRING, laneId);
        });
    }

    /** 用波次血量与攻击倍率覆盖怪物血量/攻击。 */
    private static void applyHealthAndAttack(LivingEntity mob, double maxHp,
                                             double attackMultiplier) {
        mob.setMaxHealth(maxHp);
        mob.setHealth(maxHp);
        AttributeInstance atk = mob.getAttribute(Attribute.ATTACK_DAMAGE);
        if (atk != null) {
            atk.setBaseValue(atk.getBaseValue() * attackMultiplier);
        }
    }

    /** 盲盒僵尸戴的盲盒头盔（干草块，玩家一眼能认出）。 */
    public static ItemStack blindBoxHelmet() {
        return new ItemStack(Material.HAY_BLOCK);
    }
}