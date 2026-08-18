package com.rz.dave.pvz;
import com.rz.dave.DaveManager;

import io.papermc.paper.event.entity.EntityKnockbackEvent;
import org.bukkit.GameMode;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;

/**
 * PVZ 模式事件监听。所有逻辑都先经 PvzMode 的 isPlaying/isPvzMonster 守卫，
 * 与经典模式的处理（GameListener）天然隔离：PVZ 怪物不带经典 rz/monster 标签，
 * PVZ 玩家不在经典 participants 集合中。
 */
public final class PvzListener implements Listener {

    private final PvzMode pvz;

    public PvzListener(PvzMode pvz) {
        this.pvz = pvz;
    }

    /** PVZ 怪物不得被点燃：拦截阳光灼烧等一切点燃来源（setShouldBurnInDay 在实机不可靠）。 */
    @EventHandler(ignoreCancelled = true)
    public void onCombust(EntityCombustEvent event) {
        if (pvz.isPvzMonster(event.getEntity())) {
            event.setCancelled(true);
        }
    }

    /** PVZ 怪物死亡：清掉落；盲盒僵尸触发随机召唤。 */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMonsterDeath(EntityDeathEvent event) {
        if (pvz.isPvzMonster(event.getEntity())) {
            pvz.onMonsterDeath(event);
        }
    }

    /** PVZ 玩家死亡：转观察者，不复活，交给 PvzMode 判定队伍淘汰。 */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerDeath(org.bukkit.event.entity.PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (!pvz.isPlaying(player)) {
            return;
        }
        event.setKeepInventory(true);
        event.setKeepLevel(true);
        event.setDroppedExp(0);
        pvz.onPlayerDeath(player);
    }

    /** PVZ 玩家重生成：保持观察者且回到本路游玩场地（观战），避免复活到大厅/世界出生点。 */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        if (!pvz.isPlaying(player)) {
            return;
        }
        event.setRespawnLocation(pvz.respawnLocation(player));
        player.setGameMode(GameMode.SPECTATOR);
        player.sendMessage(org.bukkit.ChatColor.RED + "【PVZ】你已阵亡，本局不会复活，观战中；游戏结束约 10 秒后返回大厅。");
    }

    /** PVZ 苦力怕爆炸不破坏地形（保留实体伤害）。 */
    @EventHandler(ignoreCancelled = true)
    public void onExplode(EntityExplodeEvent event) {
        pvz.handleExplode(event);
    }

    /** PVZ 怪物只索敌 PVZ 玩家，避免被经典模式玩家引走。 */
    @EventHandler(ignoreCancelled = true)
    public void onTarget(EntityTargetEvent event) {
        if (!pvz.isPvzMonster(event.getEntity())) {
            return;
        }
        if (!(event.getTarget() instanceof Player player) || !pvz.canTarget(event.getEntity(), player)) {
            event.setCancelled(true);
        }
    }

    /** PVZ 与经典模式的伤害边界：双方玩家、怪物和投射物不得跨模式互相影响。 */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        Entity damager = event.getDamager();
        Entity target = event.getEntity();
        if (target instanceof Player targetPlayer && pvz.isPlaying(targetPlayer)) {
            if (!(damager instanceof Entity mob && pvz.isPvzMonster(mob)
                    && pvz.canTarget(mob, targetPlayer))) {
                event.setCancelled(true);
            }
            return;
        }
        if (pvz.isPvzMonster(target)) {
            if (!isPvzPlayerDamager(damager)) {
                event.setCancelled(true);
            }
            return;
        }
        if (isPvzPlayerDamager(damager)) {
            event.setCancelled(true);
        }
    }

    /** PVZ 玩家不丢弃物品（职业武器唯一，防误丢）。 */
    @EventHandler(ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        if (pvz.isPlaying(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    /** PVZ 玩家不得拾取场地掉落物，保持职业装备纯净。 */
    @EventHandler(ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player player && pvz.isPlaying(player)) {
            event.setCancelled(true);
        }
    }

    /** PVZ 玩家不得交换主副手，职业武器保持固定。 */
    @EventHandler(ignoreCancelled = true)
    public void onSwapHands(PlayerSwapHandItemsEvent event) {
        if (pvz.isPlaying(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    /** PVZ 玩家不得通过点击或拖拽修改背包。 */
    @EventHandler(ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player && pvz.isPlaying(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getWhoClicked() instanceof Player player && pvz.isPlaying(player)) {
            event.setCancelled(true);
        }
    }

    private boolean isPvzPlayerDamager(Entity damager) {
        if (damager instanceof Player player) {
            return pvz.isPlaying(player);
        }
        if (damager instanceof Projectile projectile
                && projectile.getShooter() instanceof Player player) {
            return pvz.isPlaying(player);
        }
        return false;
    }

    /** PVZ 玩家默认无法自然回血：拦截自然再生/饱食/进食回血（保留药水与插件自定义来源，
     *  供后续回血职业使用；职业回血亦可直接 setHealth，不触发本事件）。 */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onRegainHealth(EntityRegainHealthEvent event) {
        if (!(event.getEntity() instanceof Player player) || !pvz.isPlaying(player)) {
            return;
        }
        EntityRegainHealthEvent.RegainReason reason = event.getRegainReason();
        if (reason == EntityRegainHealthEvent.RegainReason.REGEN
                || reason == EntityRegainHealthEvent.RegainReason.SATIATED
                || reason == EntityRegainHealthEvent.RegainReason.EATING) {
            event.setCancelled(true);
        }
    }

    /** PVZ 怪物默认免疫击退（近战/横扫/投射物/爆炸等一切来源），后续由其他机制代替击退。 */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onKnockback(EntityKnockbackEvent event) {
        if (pvz.isPvzMonster(event.getEntity())) {
            event.setCancelled(true);
        }
    }

    /** PVZ 射手职业右键发射（机枪射手/寒冰射手）。
     *  注意：不能 ignoreCancelled —— 右键空气事件可能被其他监听器（如菜单/保护）cancel，
     *  与大模式一致，无论空气还是方块右键都照常发射。 */
    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!pvz.isPlaying(player)) {
            return;
        }
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        ItemStack held = player.getInventory().getItemInMainHand();
        if (held.getType() == org.bukkit.Material.DISPENSER && pvz.isShooterWeapon(held)) {
            event.setCancelled(true);   // 武器不可用于与方块交互
            pvz.fireShooter(player);
        }
    }

    /** PVZ 子弹命中怪物：结算伤害/冰冻。 */
    @EventHandler(ignoreCancelled = true)
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!pvz.isPvzBullet(event.getEntity())) {
            return;
        }
        if (event.getHitEntity() instanceof Mob mob && pvz.isPvzMonster(mob)) {
            pvz.onShooterBulletHit(event.getEntity(), mob);
        }
    }

    /**
     * 通用伤害事件：坚果职业只受 10% 伤害（一切伤害来源）；
     * 带甲僵尸（路障/铁桶）血量跌破阈值时破甲（摘帽）。
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Mob mob && pvz.isPvzMonster(mob)) {
            pvz.maybeBreakArmor(mob);
            return;
        }
        if (event.getEntity() instanceof Player player && pvz.isPlaying(player)) {
            if (pvz.classOf(player) == PvzClass.WALLNUT) {
                event.setDamage(event.getDamage() * PvzMode.WALLNUT_DAMAGE_RATIO);
            }
        }
    }

    /** PVZ 玩家中途退出：视同阵亡，可能触发队伍淘汰。 */
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        pvz.onPlayerQuit(event.getPlayer());
    }
}
