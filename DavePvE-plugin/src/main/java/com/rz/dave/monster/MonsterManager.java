package com.rz.dave.monster;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.EnumMap;
import java.util.Map;

/**
 * 自定义怪物管理器：由插件主类实例化，统一注册与生成自定义怪物。
 *
 * <p>注册表直接存储「怪物类型 → 怪物实例」的单例映射：每种怪物在管理器构造时
 * 实例化一次，全局仅有一个实例（单例）；由插件主类调用 {@link #enableAll()}
 * 统一注册为 Bukkit 监听器。生成实体时通过
 * {@link #spawn(MonsterType, World, Location, SpawnContext)} 传入当次的
 * 路线/波次上下文，便于统一维护体型、标签、血量与攻击等属性。
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
    /** 戴盔甲怪物的标签（路障/铁桶僵尸）：血量降到阈值后破甲（摘帽）。 */
    public static final String TAG_ARMORED = "pvz_armored";

    /** 怪物种类注册表（供生成入口与后续扩展使用）。 */
    public enum MonsterType {
        BLIND_BOX_ZOMBIE("盲盒僵尸"),
        PLAIN_ZOMBIE("普通僵尸"),
        GIANT_ZOMBIE("巨人僵尸"),
        CONEHEAD_ZOMBIE("路障僵尸"),
        BUCKETHEAD_ZOMBIE("铁桶僵尸"),
        SUMMON_CREEPER("苦力怕");

        private final String displayName;

        MonsterType(String displayName) {
            this.displayName = displayName;
        }

        public String displayName() {
            return displayName;
        }
    }

    /** 怪物类型 → 怪物实例 的单例注册表（每种类型全局仅一个实例）。 */
    private final Map<MonsterType, Monster> registry = new EnumMap<>(MonsterType.class);

    /** 插件实例：用于注册怪物监听器。 */
    private final Plugin plugin;

    /**
     * 由插件主类实例化：构造时创建所有怪物单例实例并放入注册表。
     * 新增怪物在此登记后即可通过统一入口生成。
     */
    public MonsterManager(Plugin plugin) {
        this.plugin = plugin;
        register(MonsterType.BLIND_BOX_ZOMBIE, new BlindBoxZombie(plugin));
        register(MonsterType.PLAIN_ZOMBIE, new NormalZombie(plugin));
        register(MonsterType.GIANT_ZOMBIE, new GiantZombie(plugin));
        register(MonsterType.CONEHEAD_ZOMBIE, new ConeheadZombie(plugin));
        register(MonsterType.BUCKETHEAD_ZOMBIE, new BucketheadZombie(plugin));
        register(MonsterType.SUMMON_CREEPER, new DqqCreeper(plugin));
    }

    /** 注册怪物单例实例：放入注册表（实体移除时自动清理跟踪集合）。 */
    private void register(MonsterType type, Monster monster) {
        registry.put(type, monster);
    }

    public void enableAll() {
        for (Monster monster : registry.values()) {
            Bukkit.getPluginManager().registerEvents(monster, plugin);
            monster.onEnable();
        }
    }

    public void disableAll() {
        for (Monster monster : registry.values()) {
            HandlerList.unregisterAll(monster);
            monster.onDisable();
        }
    }

    /** 按类型获取怪物单例实例（构造时已创建，每种类型全局仅一个实例）。 */
    public Monster monster(MonsterType type) {
        Monster monster = registry.get(type);
        if (monster == null) {
            throw new IllegalArgumentException("未注册的怪物类型: " + type);
        }
        return monster;
    }

    /** 统一生成入口：按注册的怪物类型生成实体。 */
    public LivingEntity spawn(MonsterType type, World world, Location loc,
                              SpawnContext context) {
        return monster(type).spawn(world, loc, context);
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
