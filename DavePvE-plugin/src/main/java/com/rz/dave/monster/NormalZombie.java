package com.rz.dave.monster;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Zombie;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

/**
 * 召唤系原版僵尸（盲盒僵尸死亡后的产物）：PVZ 标签、昼间不燃，保持原版外形。
 */
public final class NormalZombie extends Monster {

    public NormalZombie(Plugin plugin) {
        super(plugin);
    }

    @Override
    public LivingEntity onSpawn(World world, Location loc, SpawnContext context) {
        return world.spawn(loc, Zombie.class, z -> {
            z.setBaby(false);
            z.setCanPickupItems(false);
            z.setShouldBurnInDay(false);            // 不受阳光灼烧
            z.getEquipment().clear();
            z.addScoreboardTag(MonsterManager.TAG_MONSTER);
            z.addScoreboardTag(MonsterManager.TAG_SUMMON);
            z.setRemoveWhenFarAway(false);
            z.setPersistent(true);
            z.getPersistentDataContainer().set(context.laneKey(), PersistentDataType.STRING,
                    context.laneId());
        });
    }
}
