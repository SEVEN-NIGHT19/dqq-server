package com.rz.dave.monster;

import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;

import java.util.EnumMap;
import java.util.Map;

/**
 * 自定义怪物管理器：统一注册与生成自定义怪物。
 *
 * <p>每种怪物继承 {@link Monster} 抽象类，并在本类中通过
 * {@link #register(MonsterType, MonsterFactory)} 注册到类型注册表；
 * 生成时按 {@link MonsterType} 从注册表取出对应怪物类实例化并生成实体，
 * 便于统一维护体型、标签、血量与攻击等属性。
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

    /** 怪物工厂：根据生成上下文创建具体怪物实例（未生成实体）。 */
    @FunctionalInterface
    public interface MonsterFactory {
        Monster create(SpawnContext context);
    }

    /** 怪物类型 → 怪物工厂 注册表。 */
    private static final Map<MonsterType, MonsterFactory> REGISTRY = new EnumMap<>(MonsterType.class);

    static {
        // 统一注册所有怪物类，新增怪物在此登记后即可通过统一入口生成。
        REGISTRY.put(MonsterType.BLIND_BOX_ZOMBIE, BlindBoxZombie::new);
        REGISTRY.put(MonsterType.PLAIN_ZOMBIE, NormalZombie::new);
        REGISTRY.put(MonsterType.GIANT_ZOMBIE, GiantZombie::new);
        REGISTRY.put(MonsterType.SUMMON_CREEPER, DqqCreeper::new);
    }

    private MonsterManager() {
    }

    /**
     * 注册自定义怪物：将怪物类型与工厂绑定，供统一入口生成。
     * 已存在的类型会被覆盖。
     */
    public static void register(MonsterType type, MonsterFactory factory) {
        REGISTRY.put(type, factory);
    }

    /** 按类型创建怪物实例（未生成实体），用于需要直接操作实例的场景。 */
    public static Monster create(MonsterType type, SpawnContext context) {
        MonsterFactory factory = REGISTRY.get(type);
        if (factory == null) {
            throw new IllegalArgumentException("未注册的怪物类型: " + type);
        }
        return factory.create(context);
    }

    /** 统一生成入口：按注册的怪物类型生成实体。 */
    public static LivingEntity spawn(MonsterType type, World world, Location loc,
                                     SpawnContext context) {
        return create(type, context).spawn(world, loc);
    }

    /** 判断实体是否为 PVZ 归属怪物（带 {@value #TAG_MONSTER} 标签）。 */
    public static boolean isPvzMonster(Entity entity) {
        return entity != null && entity.getScoreboardTags().contains(TAG_MONSTER);
    }

    /** 盲盒僵尸头盔（转发 {@link BlindBoxZombie#blindBoxHelmet()}，兼容旧入口）。 */
    public static ItemStack blindBoxHelmet() {
        return BlindBoxZombie.blindBoxHelmet();
    }
}
