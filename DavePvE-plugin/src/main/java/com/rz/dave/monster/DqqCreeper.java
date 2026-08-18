package com.rz.dave.monster;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.LivingEntity;
import org.bukkit.persistence.PersistentDataType;

/** 召唤系苦力怕（盲盒僵尸死亡后的产物）：PVZ 标签。 */
public final class DqqCreeper extends Monster {

    public DqqCreeper(SpawnContext context) {
        super(context);
    }

    @Override
    public LivingEntity spawn(World world, Location loc) {
        return world.spawn(loc, Creeper.class, c -> {
            c.setCanPickupItems(false);
            c.addScoreboardTag(MonsterManager.TAG_MONSTER);
            c.addScoreboardTag(MonsterManager.TAG_SUMMON);
            c.setRemoveWhenFarAway(false);
            c.setPersistent(true);
            c.getPersistentDataContainer().set(laneKey(), PersistentDataType.STRING, laneId());
        });
    }
}
