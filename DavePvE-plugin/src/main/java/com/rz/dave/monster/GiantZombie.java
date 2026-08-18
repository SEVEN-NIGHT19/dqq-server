package com.rz.dave.monster;

import io.papermc.paper.event.entity.EntityMoveEvent;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * 巨人僵尸（盲盒僵尸死亡后的产物）：原版僵尸 2 倍体型（SCALE 属性 2.0）、4 倍血量，昼间不燃。
 */
public final class GiantZombie extends Monster {
    public static final double GIANT_ZOMBIE_SCALE = 2.0;
    public static final double GIANT_ZOMBIE_HEALTH_MULTIPLIER = 4.0;
    public static final double GIANT_ZOMBIE_ATTACK_DISTANCE = 3;
    public static final double GIANT_ZOMBIE_ATTACK_RADIANS = Math.toRadians(22.5);

    public GiantZombie(Plugin plugin) {
        super(plugin);
    }

    private final Map<Zombie, ZombieInstance> zombieInstances = new HashMap<>();
    private BukkitTask task;

    @Override
    public void onEnable() {
        task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (var spawnedZombie : spawned()) {
                Zombie zombie = (Zombie) spawnedZombie;
                ZombieInstance ins = zombieInstances.get(zombie);
                if (ins == null) {
                    continue;
                }
                if (!ins.isAttacking() && isAnyPlayerInAttackRange(zombie)) {
                    ins.attack();
                }
            }
        }, 0, 0);
    }

    @Override
    public void onDisable() {
        if (task != null) {
            task.cancel();
        }
    }

    @Override
    public LivingEntity onSpawn(World world, Location loc, SpawnContext context) {
        Zombie zombie = world.spawn(loc, Zombie.class, false, z -> {
            z.setCanPickupItems(false);
            z.setShouldBurnInDay(false);            // 不受阳光灼烧

            AttributeInstance scaleAttr = z.getAttribute(Attribute.SCALE);
            scaleAttr.setBaseValue(GIANT_ZOMBIE_SCALE);

            AttributeInstance damageAttr = z.getAttribute(Attribute.ATTACK_DAMAGE);
            damageAttr.setBaseValue(0);

            z.setCustomName(ChatColor.DARK_RED + "巨人僵尸");
            z.setCustomNameVisible(true);
            z.addScoreboardTag(MonsterManager.TAG_MONSTER);
            z.addScoreboardTag(MonsterManager.TAG_SUMMON);
            z.setRemoveWhenFarAway(false);
            z.setPersistent(true);
            z.getPersistentDataContainer().set(context.laneKey(), PersistentDataType.STRING,
                    context.laneId());
        });
        double hp = zombie.getHealth() * GIANT_ZOMBIE_HEALTH_MULTIPLIER;
        zombie.setMaxHealth(hp);
        zombie.setHealth(hp);

        ItemDisplay axe = world.spawn(loc, ItemDisplay.class, e -> {
            e.setItemStack(new ItemStack(Material.WOODEN_AXE));
            e.setTransformation(
                new Transformation(
                    new Vector3f(-0.8f, 0, 2.425f),
                    new AxisAngle4f(),
                    new Vector3f(3f, 3f, 3f),
                    new AxisAngle4f()
                )
            );
            e.setRotation(0, 90);
        });
        zombieInstances.put(zombie, new ZombieInstance(this, zombie, axe));

        return zombie;
    }

    @Override
    protected void onRemove(LivingEntity zombie) {
        if (zombieInstances.containsKey(zombie)) {
            zombieInstances.remove(zombie).onRemove();
        }
    }

    private static boolean isPlayerInAttackRangeIgnoreY(Location center, Player player) {
        double yaw = center.getYaw();
        double radius = GIANT_ZOMBIE_ATTACK_DISTANCE;

        Location playerLoc = player.getLocation();
        if (playerLoc.getWorld() == null || !playerLoc.getWorld().equals(center.getWorld())) {
            return false;
        }
        // 水平距离（忽略 Y 轴）
        double dx = playerLoc.getX() - center.getX();
        double dz = playerLoc.getZ() - center.getZ();
        double distanceSq = dx * dx + dz * dz;
        if (distanceSq > radius * radius) {
            return false;
        }
        // 距离为 0 时直接在眼前，视为命中
        if (distanceSq == 0) {
            return true;
        }
        // 僵尸正前方方向向量（yaw=0 时朝向 +Z，旋转 yaw 度后方向变化）
        double forwardX = -Math.sin(Math.toRadians(yaw));
        double forwardZ = Math.cos(Math.toRadians(yaw));
        double forwardLen = Math.sqrt(forwardX * forwardX + forwardZ * forwardZ);
        forwardX /= forwardLen;
        forwardZ /= forwardLen;
        // 玩家相对僵尸的方向向量
        double playerX = dx / Math.sqrt(distanceSq);
        double playerZ = dz / Math.sqrt(distanceSq);
        // 点积判断夹角：cos(夹角) >= cos(半角) 表示在扇形内
        double dot = forwardX * playerX + forwardZ * playerZ;
        return dot >= Math.cos(GIANT_ZOMBIE_ATTACK_RADIANS);
    }

    private static Collection<Player> getPlayersInAttackRange(Zombie zombie) {
        Location zombieLoc = zombie.getLocation();
        double radius = GIANT_ZOMBIE_ATTACK_DISTANCE;

        return zombie.getWorld()
            .getNearbyPlayers(zombieLoc, radius, radius, zombie.getHeight())
            .stream()
            .filter(p -> isPlayerInAttackRangeIgnoreY(zombieLoc, p))
            .toList();
    }

    private static boolean isAnyPlayerInAttackRange(Zombie zombie) {
        Location zombieLoc = zombie.getLocation();
        double radius = GIANT_ZOMBIE_ATTACK_DISTANCE;

        return zombie.getWorld()
            .getNearbyPlayers(zombieLoc, radius, radius, zombie.getHeight())
            .stream()
            .anyMatch(p -> isPlayerInAttackRangeIgnoreY(zombieLoc, p));
    }

    private static class ZombieInstance {
        private final GiantZombie monster;
        private final Zombie zombie;
        private final ItemDisplay axe;
        private boolean attacking;
        private BukkitTask animationTask;
        private BukkitTask attackTask;

        public ZombieInstance(GiantZombie monster, Zombie zombie, ItemDisplay axe) {
            this.monster = monster;
            this.zombie = zombie;
            this.axe = axe;
        }

        public void attack() {
            attacking = true;
            axe.setInterpolationDuration(15);
            axe.setTransformation(
                new Transformation(
                    new Vector3f(-0.8f, 1.5f, 0.925f),
                    new AxisAngle4f(-0.63667f, 0f, 0f, 0.77111f),
                    new Vector3f(3f, 3f, 3f),
                    new AxisAngle4f()
                )
            );

            animationTask = Bukkit.getScheduler().runTaskLater(monster.plugin, () -> {
                axe.setInterpolationDuration(2);
                axe.setTransformation(
                    new Transformation(
                        new Vector3f(-0.8f, 1.5f, 2.925f),
                        new AxisAngle4f(-0.56165f, 0f, 0f, 0.82737f),
                        new Vector3f(3f, 3f, 3f),
                        new AxisAngle4f()
                    )
                );

                animationTask = Bukkit.getScheduler().runTaskLater(monster.plugin, () -> {
                    axe.setInterpolationDuration(10);
                    axe.setTransformation(
                        new Transformation(
                            new Vector3f(-0.8f, 0, 2.425f),
                            new AxisAngle4f(),
                            new Vector3f(3f, 3f, 3f),
                            new AxisAngle4f()
                        )
                    );
                    attacking = false;      // 挥斧动画结束，允许下一次攻击
                }, 3);
            }, 14);

            attackTask = Bukkit.getScheduler().runTaskLater(monster.plugin, () -> {
                DamageSource source = DamageSource.builder(DamageType.MOB_ATTACK)
                    .withCausingEntity(zombie)
                    .build();
                getPlayersInAttackRange(zombie).forEach(p -> p.damage(30, source));
            }, 16);
        }

        public boolean isAttacking() {
            return attacking;
        }

        public void cancelAttack() {
            if (animationTask != null) {
                animationTask.cancel();
            }
            if (attackTask != null) {
                attackTask.cancel();
            }
            attacking = false;
            axe.setInterpolationDuration(0);
            axe.setTransformation(
                new Transformation(
                    new Vector3f(-0.8f, 0, 2.425f),
                    new AxisAngle4f(),
                    new Vector3f(3f, 3f, 3f),
                    new AxisAngle4f()
                )
            );
        }

        public void onRemove() {
            cancelAttack();
            axe.remove();
        }
    }

    @EventHandler
    public void onEntityMove(EntityMoveEvent event) {
        if (event.isCancelled()) {
            return;
        }
        if (!(event.getEntity() instanceof Zombie zombie)) {
            return;
        }
        ZombieInstance ins = zombieInstances.get(zombie);
        if (ins != null) {
            ins.axe.teleport(event.getTo());
        }
    }
}
