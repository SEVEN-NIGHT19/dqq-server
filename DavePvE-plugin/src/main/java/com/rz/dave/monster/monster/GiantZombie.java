package com.rz.dave.monster;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Zombie;
import org.bukkit.persistence.PersistentDataType;

/**
 * 巨人僵尸（盲盒僵尸死亡后的产物）：原版僵尸 4 倍体型、4 倍血量，昼间不燃。
 */
public final class GiantZombie extends Monster {

    public GiantZombie(SpawnContext context) {
        super(context);
    }

    @Override
    public LivingEntity spawn(World world, Location loc) {
        Zombie zombie = world.spawn(loc, Zombie.class, false, z -> {
            z.setCanPickupItems(false);
            z.setShouldBurnInDay(false);            // 不受阳光灼烧
            z.getEquipment().clear();
            AttributeInstance scale = z.getAttribute(Attribute.SCALE);
            if (scale != null) {
                scale.setBaseValue(MonsterManager.GIANT_ZOMBIE_SCALE);
            }
            z.setCustomName(ChatColor.DARK_RED + "巨人僵尸");
            z.setCustomNameVisible(true);
            z.addScoreboardTag(MonsterManager.TAG_MONSTER);
            z.addScoreboardTag(MonsterManager.TAG_SUMMON);
            z.setRemoveWhenFarAway(false);
            z.setPersistent(true);
            z.getPersistentDataContainer().set(laneKey(), PersistentDataType.STRING, laneId());
        });
        double hp = MonsterManager.BASE_ZOMBIE_HEALTH
            * MonsterManager.GIANT_ZOMBIE_HEALTH_MULTIPLIER;
        zombie.setMaxHealth(hp);
        zombie.setHealth(hp);
        return zombie;
    }
}
