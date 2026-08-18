package com.rz.dave.monster;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.bukkit.plugin.Plugin;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * 自定义怪物抽象基类。
 *
 * <p>每种怪物在 {@link MonsterManager} 中注册为唯一实例（每个类型全局仅一个），
 * 通过 {@link #spawn(World, Location, SpawnContext)} 反复生成实体，
 * 各自的生成逻辑在 {@link #onSpawn(World, Location, SpawnContext)} 中实现，
 * 便于后续扩展精英怪、Boss 等新怪物。
 *
 * <p>本类实现 {@link Listener}：每次生成的实体登记到 {@link #spawned} 集合，
 * 实体被移除（死亡/卸载/清除）时由 {@link #onEntityRemove(EntityRemoveEvent)} 自动清理，
 * 可实时查询“该怪物当前在场的实体”。
 */
public abstract class Monster implements Listener {
    protected final Plugin plugin;

    /** 本怪物实例已生成且仍在场的实体集合（实体移除时自动清理）。 */
    private final Set<LivingEntity> spawned = new HashSet<>();

    protected Monster(Plugin plugin) {
        this.plugin = plugin;
    }

    public void onEnable() {
    }

    public void onDisable() {
    }

    /** 生成怪物实体的核心逻辑，由各怪物实现；统一入口为 {@link #spawn(World, Location, SpawnContext)}。 */
    public abstract LivingEntity onSpawn(World world, Location loc, SpawnContext context);

    protected void onRemove(LivingEntity monster) {
    }

    /**
     * 统一生成入口：调用 {@link #onSpawn(World, Location, SpawnContext)} 生成实体，
     * 并将实体登记到 {@link #spawned} 中，实体移除时由事件监听自动清理。
     */
    public final LivingEntity spawn(World world, Location loc, SpawnContext context) {
        LivingEntity mob = onSpawn(world, loc, context);
        if (mob != null) {
            spawned.add(mob);
        }
        return mob;
    }

    /** 实体被移除（死亡/卸载/清除）时从登记集合中删除。 */
    @EventHandler
    public void onEntityRemove(EntityRemoveEvent event) {
        var entity = event.getEntity();
        if (spawned.contains(entity)) {
            spawned.remove(entity);
            onRemove((LivingEntity) entity);
        }
    }

    /** 本怪物实例当前在场的实体（只读视图）。 */
    public final Set<LivingEntity> spawned() {
        return Collections.unmodifiableSet(spawned);
    }

    /** 用波次血量与攻击倍率覆盖怪物血量/攻击。 */
    protected void applyHealthAndAttack(LivingEntity mob, SpawnContext context) {
        mob.setMaxHealth(context.maxHp());
        mob.setHealth(context.maxHp());
        AttributeInstance atk = mob.getAttribute(Attribute.ATTACK_DAMAGE);
        if (atk != null) {
            atk.setBaseValue(atk.getBaseValue() * context.attackMultiplier());
        }
    }
}
