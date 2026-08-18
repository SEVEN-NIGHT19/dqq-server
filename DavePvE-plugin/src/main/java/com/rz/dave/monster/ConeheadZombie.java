package com.rz.dave.monster;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

/**
 * 路障僵尸（仿 PVZ 锥形路障）：38 点血量，头戴橡木木板；
 * 血量降到 20 点以下时帽子损坏（破甲），昼间不燃，死亡无掉落。
 */
public final class ConeheadZombie extends Monster {

    public static final double HEALTH = 38.0;
    /** 破甲阈值：血量低于该值时帽子损坏。 */
    public static final double ARMOR_BREAK_HP = 20.0;
    public static final Material HELMET = Material.OAK_PLANKS;

    public ConeheadZombie(SpawnContext context) {
        super(context);
    }

    @Override
    public LivingEntity spawn(World world, Location loc) {
        Zombie zombie = world.spawn(loc, Zombie.class, false, z -> {
            z.setBaby(false);
            z.setCanPickupItems(false);
            z.setShouldBurnInDay(false);            // 不受阳光灼烧
            z.getEquipment().clear();
            z.getEquipment().setHelmet(new ItemStack(HELMET));
            z.getEquipment().setHelmetDropChance(0.0f);
            z.setCustomName(ChatColor.DARK_RED + "路障僵尸");
            z.setCustomNameVisible(true);
            z.addScoreboardTag(MonsterManager.TAG_MONSTER);
            z.addScoreboardTag(MonsterManager.TAG_SUMMON);
            z.addScoreboardTag(MonsterManager.TAG_ARMORED);
            z.setRemoveWhenFarAway(false);
            z.setPersistent(true);
            z.getPersistentDataContainer().set(laneKey(), PersistentDataType.STRING, laneId());
        });
        zombie.setMaxHealth(HEALTH);
        zombie.setHealth(HEALTH);
        return zombie;
    }
}