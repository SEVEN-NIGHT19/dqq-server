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
 * 铁桶僵尸（仿 PVZ 铁桶）：137 点血量，头戴铁块；
 * 血量降到 20 点以下时帽子损坏（破甲），昼间不燃，死亡无掉落。
 */
public final class BucketheadZombie extends Monster {

    public static final double HEALTH = 137.0;
    /** 破甲阈值：血量低于该值时帽子损坏。 */
    public static final double ARMOR_BREAK_HP = 20.0;
    public static final Material HELMET = Material.IRON_BLOCK;

    public BucketheadZombie(Plugin plugin) {
        super(plugin);
    }

    @Override
    public LivingEntity onSpawn(World world, Location loc, SpawnContext context) {
        Zombie zombie = world.spawn(loc, Zombie.class, false, z -> {
            z.setBaby(false);
            z.setCanPickupItems(false);
            z.setShouldBurnInDay(false);            // 不受阳光灼烧
            z.getEquipment().setHelmet(new ItemStack(HELMET));
            z.getEquipment().setHelmetDropChance(0.0f);
            z.setCustomName(ChatColor.DARK_RED + "铁桶僵尸");
            z.setCustomNameVisible(true);
            z.addScoreboardTag(MonsterManager.TAG_MONSTER);
            z.addScoreboardTag(MonsterManager.TAG_SUMMON);
            z.addScoreboardTag(MonsterManager.TAG_ARMORED);
            z.setRemoveWhenFarAway(false);
            z.setPersistent(true);
            z.getPersistentDataContainer().set(context.laneKey(), PersistentDataType.STRING, context.laneId());
        });
        zombie.setMaxHealth(HEALTH);
        zombie.setHealth(HEALTH);
        return zombie;
    }
}