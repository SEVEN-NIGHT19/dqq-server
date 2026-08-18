package com.rz.dave.monster;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

/**
 * 盲盒僵尸：戴着盲盒头盔的普通僵尸，不惧阳光（昼间不燃烧），
 * 血量随波次成长，死亡后由 PVZ 逻辑随机召唤其他怪物。
 */
public final class BlindBoxZombie extends Monster {

    public BlindBoxZombie(Plugin plugin) {
        super(plugin);
    }

    @Override
    public LivingEntity onSpawn(World world, Location loc, SpawnContext context) {
        Zombie zombie = world.spawn(loc, Zombie.class, false, z -> {
            z.setCanPickupItems(false);
            z.setShouldBurnInDay(false);            // 不受阳光灼烧
            z.getEquipment().clear();
            z.getEquipment().setHelmet(blindBoxHelmet());
            z.getEquipment().setHelmetDropChance(0.0f);
            z.setCustomName(ChatColor.DARK_RED + "盲盒僵尸");
            z.setCustomNameVisible(true);
            z.addScoreboardTag(MonsterManager.TAG_MONSTER);
            z.addScoreboardTag(MonsterManager.TAG_BLINDBOX);
            z.setRemoveWhenFarAway(false);
            z.setPersistent(true);
            z.getPersistentDataContainer().set(context.laneKey(), PersistentDataType.STRING,
                    context.laneId());
        });
        applyHealthAndAttack(zombie, context);
        return zombie;
    }

    /** 盲盒僵尸戴的盲盒头盔（干草块，玩家一眼能认出）。 */
    public static ItemStack blindBoxHelmet() {
        return new ItemStack(Material.HAY_BLOCK);
    }
}
