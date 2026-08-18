package com.rz.dave.monster;

import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;

/**
 * 自定义怪物抽象基类。
 *
 * <p>PVZ（随机植物对战随机僵尸）模式中每种怪物都继承本类，
 * 在 {@link #spawn(World, Location)} 中实现自身的生成逻辑；
 * 具体怪物类由 {@link MonsterManager} 统一注册，通过类型入口统一生成，
 * 便于后续扩展精英怪、Boss 等新怪物。
 */
public abstract class Monster {

    protected final SpawnContext context;

    protected Monster(SpawnContext context) {
        this.context = context;
    }

    /** 生成怪物实体到指定世界与位置。 */
    public abstract LivingEntity spawn(World world, Location loc);

    /** 所属路线 ID。 */
    public final String laneId() {
        return context.laneId();
    }

    /** 路线持久化 Key。 */
    public final NamespacedKey laneKey() {
        return context.laneKey();
    }

    /** 用波次血量与攻击倍率覆盖怪物血量/攻击。 */
    protected void applyHealthAndAttack(LivingEntity mob) {
        mob.setMaxHealth(context.maxHp());
        mob.setHealth(context.maxHp());
        AttributeInstance atk = mob.getAttribute(Attribute.ATTACK_DAMAGE);
        if (atk != null) {
            atk.setBaseValue(atk.getBaseValue() * context.attackMultiplier());
        }
    }
}
