package com.rz.dave;
import com.rz.dave.DaveManager;
import com.rz.dave.pvz.PvzListener;
import com.rz.dave.pvz.PvzMode;

import io.papermc.paper.event.entity.EntityKnockbackEvent;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Zombie;
import org.bukkit.entity.Item;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;
import org.bukkit.plugin.Plugin;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PVZ（随机植物对战随机僵尸）流程集成测试（MockBukkit）：
 * 配置五条数字路 → 模拟玩家准备 → 开局（随机职业/发装备/传送/顺序凑满分路）
 * → 淘汰若干路 → 判定获胜路；并验证 25 人上限、默认无自然回血、怪物免疫击退。
 */
class PvzModeFlowTest {

    private ServerMock server;
    private Plugin plugin;
    private DaveManager manager;
    private PvzMode pvz;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        server.addSimpleWorld("world");
        plugin = newBarePlugin();
        manager = new DaveManager(plugin);
        pvz = new PvzMode(plugin, manager);
        pvz.enable();
        // 配置全部 5 条数字路（world 世界，测试坐标）
        for (String id : PvzMode.LANE_IDS) {
            pvz.setLaneSpawn(id, "world", 1, 1, 33);
            pvz.setLaneBase(id, "world", 21, 1, 33);
            pvz.setLanePlayerSpawn(id, "world", 11, 1, 33);
        }
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    /** 与 DaveManagerMockTest 相同的裸插件动态代理，额外提供空的 FileConfiguration。 */
    private static Plugin newBarePlugin() {
        return (Plugin) Proxy.newProxyInstance(
                Plugin.class.getClassLoader(),
                new Class<?>[]{Plugin.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("namespace")) {
                        return "testplugin";
                    }
                    if (method.getName().equals("getName")) {
                        return "TestPlugin";
                    }
                    if (method.getName().equals("getConfig")) {
                        return YamlConfiguration.loadConfiguration(
                                new java.io.StringReader(""));
                    }
                    Class<?> returnType = method.getReturnType();
                    if (returnType == boolean.class) {
                        return false;
                    }
                    if (returnType == int.class) {
                        return 0;
                    }
                    if (returnType == double.class) {
                        return 0.0;
                    }
                    return null;
                });
    }

    /** 创建 count 名玩家并全部准备后开局。 */
    private List<PlayerMock> readyAndStart(int count, String namePrefix) {
        List<PlayerMock> players = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            PlayerMock p = server.addPlayer(namePrefix + i);
            pvz.setReady(p, true);
            players.add(p);
        }
        pvz.startGame(server.getConsoleSender());
        return players;
    }

    @Test
    void startGameAssignsClassesAndLanes() {
        PlayerMock a = server.addPlayer("P1");
        PlayerMock b = server.addPlayer("P2");
        pvz.setReady(a, true);
        pvz.setReady(b, true);
        pvz.startGame(server.getConsoleSender());
        assertTrue(pvz.isRunning());
        // 顺序凑满：两人同守 1路
        assertEquals("one", pvz.laneOf(a));
        assertEquals("one", pvz.laneOf(b));
        assertNotNull(pvz.classOf(a));
        assertNotNull(pvz.classOf(b));
        assertTrue(pvz.isPlaying(a) && pvz.isPlaying(b));
        String status = pvz.statusSummary();
        assertTrue(status.contains("1路") && status.contains("存活玩家 2"), status);
        // 职业装备已发放
        assertTrue(a.getInventory().contains(Material.IRON_SWORD)
                || a.getInventory().contains(Material.BOW));
    }

    @Test
    void laneFillsFivePlayersBeforeNextLane() {
        List<PlayerMock> players = readyAndStart(7, "Fill");
        assertEquals(7, players.size());
        int one = 0, two = 0;
        for (PlayerMock p : players) {
            assertTrue(pvz.isPlaying(p), "25 人上限内不应剔除玩家");
            if ("one".equals(pvz.laneOf(p))) {
                one++;
            } else if ("two".equals(pvz.laneOf(p))) {
                two++;
            }
        }
        assertEquals(5, one, "1路应优先凑满 5 人");
        assertEquals(2, two, "剩余 2 人进入 2路");
    }

    @Test
    void overCapacityKeepsOnlyTwentyFivePlayers() {
        List<PlayerMock> players = readyAndStart(26, "Over");
        int playing = 0;
        for (PlayerMock p : players) {
            if (pvz.isPlaying(p)) {
                playing++;
            }
        }
        assertEquals(25, playing, "超过 25 人随机剔除多余，只进 25 人");
    }

    @Test
    void eliminatingFullFirstLaneDeclaresSecondLaneWinner() {
        List<PlayerMock> players = readyAndStart(6, "Win");
        for (int i = 0; i < 5; i++) {
            assertEquals("one", pvz.laneOf(players.get(i)));
        }
        assertEquals("two", pvz.laneOf(players.get(5)));
        // 1路 5 人全部阵亡 → 1路淘汰 → 2路获胜
        for (int i = 0; i < 4; i++) {
            pvz.onPlayerDeath(players.get(i));
        }
        assertTrue(pvz.isRunning(), "1路还有 1 人存活，不应结束");
        pvz.onPlayerDeath(players.get(4));
        assertFalse(pvz.isRunning());
        assertEquals("two", pvz.lastWinner());
    }

    @Test
    void singleLaneStartRunsUntilTheOnlyLaneIsEliminated() {
        PlayerMock solo = server.addPlayer("Solo");
        pvz.setReady(solo, true);
        pvz.startGame(server.getConsoleSender());
        assertTrue(pvz.isRunning(), "仅一路开局应进入生存局");
        assertEquals("one", pvz.laneOf(solo));
        assertTrue(pvz.isPlaying(solo));
        pvz.onPlayerDeath(solo);
        assertFalse(pvz.isRunning(), "唯一路全员阵亡后才应结束");
        assertEquals(0, pvz.readyCount(), "准备状态应被清空");
    }

    @Test
    void pvzMonsterCannotTargetPlayerFromAnotherLane() {
        List<PlayerMock> players = readyAndStart(6, "Target");
        PlayerMock other = players.get(5); // 2路玩家
        assertEquals("two", pvz.laneOf(other));
        Zombie monster = server.getWorld("world").spawn(players.get(0).getLocation(), Zombie.class);
        monster.addScoreboardTag(PvzMode.TAG_MONSTER);
        monster.getPersistentDataContainer().set(
                new NamespacedKey(plugin, "pvz_lane"), PersistentDataType.STRING, "one");
        EntityTargetEvent event = new EntityTargetEvent(monster, other, EntityTargetEvent.TargetReason.CUSTOM);
        new PvzListener(pvz).onTarget(event);
        assertTrue(event.isCancelled(), "1路怪物不得索敌其他路的玩家");
    }

    @Test
    void pvzMonsterCannotTargetSpectator() {
        List<PlayerMock> players = readyAndStart(6, "Spectate");
        PlayerMock other = players.get(5);
        pvz.onPlayerDeath(other);
        Zombie monster = server.getWorld("world").spawn(players.get(0).getLocation(), Zombie.class);
        monster.addScoreboardTag(PvzMode.TAG_MONSTER);
        monster.getPersistentDataContainer().set(
                new NamespacedKey(plugin, "pvz_lane"), PersistentDataType.STRING, "one");
        EntityTargetEvent event = new EntityTargetEvent(monster, other, EntityTargetEvent.TargetReason.CUSTOM);
        new PvzListener(pvz).onTarget(event);
        assertTrue(event.isCancelled(), "PVZ 怪物不得索敌观察者");
    }

    @Test
    void pvzPlayerCannotPickupOrSwapItems() {
        PlayerMock player = server.addPlayer("PvzItems");
        pvz.setReady(player, true);
        pvz.startGame(server.getConsoleSender());
        PvzListener listener = new PvzListener(pvz);
        Item dropped = server.getWorld("world").dropItem(player.getLocation(), new ItemStack(Material.DIAMOND));
        EntityPickupItemEvent pickup = new EntityPickupItemEvent(player, dropped, 0);
        listener.onPickup(pickup);
        assertTrue(pickup.isCancelled(), "PVZ 玩家不得拾取场地物品");
        PlayerSwapHandItemsEvent swap = new PlayerSwapHandItemsEvent(
                player, new ItemStack(Material.IRON_SWORD), new ItemStack(Material.AIR));
        listener.onSwapHands(swap);
        assertTrue(swap.isCancelled(), "PVZ 玩家不得交换主副手");
    }

    @Test
    void crossModeDamageIsCancelled() {
        PlayerMock pvzPlayer = server.addPlayer("PvzCombatant");
        PlayerMock classicPlayer = server.addPlayer("ClassicCombatant");
        pvz.setReady(pvzPlayer, true);
        pvz.startGame(server.getConsoleSender());
        PvzListener listener = new PvzListener(pvz);
        Zombie pvzMonster = server.getWorld("world").spawn(pvzPlayer.getLocation(), Zombie.class);
        pvzMonster.addScoreboardTag(PvzMode.TAG_MONSTER);
        pvzMonster.getPersistentDataContainer().set(
                new NamespacedKey(plugin, "pvz_lane"), PersistentDataType.STRING, "one");
        EntityDamageByEntityEvent classicHitsPvzMonster = new EntityDamageByEntityEvent(
                classicPlayer, pvzMonster, EntityDamageEvent.DamageCause.ENTITY_ATTACK, 1.0);
        listener.onDamage(classicHitsPvzMonster);
        assertTrue(classicHitsPvzMonster.isCancelled(), "经典玩家不得攻击 PVZ 怪物");
        Zombie classicMonster = server.getWorld("world").spawn(classicPlayer.getLocation(), Zombie.class);
        EntityDamageByEntityEvent classicMonsterHitsPvz = new EntityDamageByEntityEvent(
                classicMonster, pvzPlayer, EntityDamageEvent.DamageCause.ENTITY_ATTACK, 1.0);
        listener.onDamage(classicMonsterHitsPvz);
        assertTrue(classicMonsterHitsPvz.isCancelled(), "经典怪物不得攻击 PVZ 玩家");
        EntityDamageByEntityEvent pvzHitsClassic = new EntityDamageByEntityEvent(
                pvzPlayer, classicMonster, EntityDamageEvent.DamageCause.ENTITY_ATTACK, 1.0);
        listener.onDamage(pvzHitsClassic);
        assertTrue(pvzHitsClassic.isCancelled(), "PVZ 玩家不得攻击经典怪物");
    }

    @Test
    void pvzPlayerCannotNaturallyRegenButMagicHealAllowed() {
        PlayerMock player = server.addPlayer("PvzHeal");
        pvz.setReady(player, true);
        pvz.startGame(server.getConsoleSender());
        PvzListener listener = new PvzListener(pvz);
        EntityRegainHealthEvent regen = new EntityRegainHealthEvent(
                player, 2.0, EntityRegainHealthEvent.RegainReason.REGEN);
        listener.onRegainHealth(regen);
        assertTrue(regen.isCancelled(), "PVZ 玩家不得自然回血");
        EntityRegainHealthEvent satiated = new EntityRegainHealthEvent(
                player, 2.0, EntityRegainHealthEvent.RegainReason.SATIATED);
        listener.onRegainHealth(satiated);
        assertTrue(satiated.isCancelled(), "PVZ 玩家不得饱食回血");
        EntityRegainHealthEvent magic = new EntityRegainHealthEvent(
                player, 2.0, EntityRegainHealthEvent.RegainReason.MAGIC);
        listener.onRegainHealth(magic);
        assertFalse(magic.isCancelled(), "药水/自定义回血保留供职业机制使用");
    }

    @Test
    void pvzMonsterKnockbackIsCancelledButNormalMobUnaffected() {
        PlayerMock player = server.addPlayer("PvzKnock");
        pvz.setReady(player, true);
        pvz.startGame(server.getConsoleSender());
        PvzListener listener = new PvzListener(pvz);
        Zombie pvzMonster = server.getWorld("world").spawn(player.getLocation(), Zombie.class);
        pvzMonster.addScoreboardTag(PvzMode.TAG_MONSTER);
        pvzMonster.getPersistentDataContainer().set(
                new NamespacedKey(plugin, "pvz_lane"), PersistentDataType.STRING, "one");
        EntityKnockbackEvent kb = new EntityKnockbackEvent(
                pvzMonster, EntityKnockbackEvent.Cause.ENTITY_ATTACK, new Vector(1, 0, 0));
        listener.onKnockback(kb);
        assertTrue(kb.isCancelled(), "PVZ 怪物免疫击退");
        Zombie normal = server.getWorld("world").spawn(player.getLocation(), Zombie.class);
        EntityKnockbackEvent kb2 = new EntityKnockbackEvent(
                normal, EntityKnockbackEvent.Cause.ENTITY_ATTACK, new Vector(1, 0, 0));
        listener.onKnockback(kb2);
        assertFalse(kb2.isCancelled(), "非 PVZ 怪物不受影响");
    }
}
