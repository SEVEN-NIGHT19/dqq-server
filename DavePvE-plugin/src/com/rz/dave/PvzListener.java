package com.rz.dave;

import org.bukkit.GameMode;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;

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

    /** PVZ 玩家重生成：强制保持观察者并回大厅，避免复活机制干扰。 */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        if (!pvz.isPlaying(player)) {
            return;
        }
        player.setGameMode(GameMode.SPECTATOR);
        player.sendMessage(org.bukkit.ChatColor.RED + "【PVZ】你已阵亡，本局不会复活。");
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

    /** PVZ 玩家中途退出：视同阵亡，可能触发队伍淘汰。 */
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        pvz.onPlayerQuit(event.getPlayer());
    }
}
