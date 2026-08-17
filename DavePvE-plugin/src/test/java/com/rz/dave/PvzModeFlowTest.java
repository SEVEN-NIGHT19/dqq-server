package com.rz.dave;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Zombie;
import org.bukkit.entity.Item;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;
import org.bukkit.plugin.Plugin;
import org.bukkit.persistence.PersistentDataType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PVZ 模式多路流程集成测试（MockBukkit）：
 * 配置五条数字路 → 模拟玩家准备 → 开局（随机职业/发装备/传送/随机分路）
 * → 淘汰若干路 → 判定获胜路。
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

    @Test
    void startGameAssignsClassesAndLanes() {
        PlayerMock red = server.addPlayer("RedPlayer");
        PlayerMock blue = server.addPlayer("BluePlayer");
        pvz.setReady(red, true);
        pvz.setReady(blue, true);

        pvz.startGame(server.getConsoleSender());

        assertTrue(pvz.isRunning());
        // 随机分路：人少的路优先，两人依次分到 1路、2路
        assertEquals("one", pvz.laneOf(red));
        assertEquals("two", pvz.laneOf(blue));
        assertNotNull(pvz.classOf(red));
        assertNotNull(pvz.classOf(blue));
        assertTrue(pvz.isPlaying(red));
        assertTrue(pvz.isPlaying(blue));
        String status = pvz.statusSummary();
        assertTrue(status.contains("1路") && status.contains("存活玩家 1"), status);
        assertTrue(status.contains("2路") && status.contains("存活玩家 1"), status);
        // 职业装备已发放
        assertTrue(red.getInventory().contains(org.bukkit.Material.IRON_SWORD)
                || red.getInventory().contains(org.bukkit.Material.BOW));
    }

    @Test
    void eliminatingOneLaneDeclaresTheOtherWinner() {
        PlayerMock red1 = server.addPlayer("RedOne");
        PlayerMock red2 = server.addPlayer("RedTwo");
        PlayerMock blue1 = server.addPlayer("BlueOne");
        pvz.setReady(red1, true);
        pvz.setReady(red2, true);
        pvz.setReady(blue1, true);
        pvz.startGame(server.getConsoleSender());
        assertTrue(pvz.isRunning());
        assertEquals("one", pvz.laneOf(red1));
        assertEquals("two", pvz.laneOf(red2));
        assertEquals("three", pvz.laneOf(blue1));

        // 1路、2路各一人阵亡 → 两路淘汰 → 3路获胜
        pvz.onPlayerDeath(red1);
        assertTrue(pvz.isRunning(), "1路淘汰后还有 2路/3路，不应结束");
        pvz.onPlayerDeath(red2);
        assertFalse(pvz.isRunning());
        assertEquals("three", pvz.lastWinner());
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
        PlayerMock red = server.addPlayer("RedTarget");
        PlayerMock blue = server.addPlayer("BlueTarget");
        pvz.setReady(red, true);
        pvz.setReady(blue, true);
        pvz.startGame(server.getConsoleSender());

        Zombie monster = server.getWorld("world").spawn(red.getLocation(), Zombie.class);
        monster.addScoreboardTag(PvzMode.TAG_MONSTER);
        monster.getPersistentDataContainer().set(
                new NamespacedKey(plugin, "pvz_lane"),
                PersistentDataType.STRING,
                "one");

        EntityTargetEvent event = new EntityTargetEvent(monster, blue, EntityTargetEvent.TargetReason.CUSTOM);
        new PvzListener(pvz).onTarget(event);

        assertTrue(event.isCancelled(), "1路怪物不得索敌其他路的玩家");
    }

    @Test
    void pvzMonsterCannotTargetSpectator() {
        PlayerMock red = server.addPlayer("RedAlive");
        PlayerMock blue = server.addPlayer("BlueDead");
        pvz.setReady(red, true);
        pvz.setReady(blue, true);
        pvz.startGame(server.getConsoleSender());
        pvz.onPlayerDeath(blue);

        Zombie monster = server.getWorld("world").spawn(red.getLocation(), Zombie.class);
        monster.addScoreboardTag(PvzMode.TAG_MONSTER);
        monster.getPersistentDataContainer().set(
                new NamespacedKey(plugin, "pvz_lane"),
                PersistentDataType.STRING,
                "one");

        EntityTargetEvent event = new EntityTargetEvent(monster, blue, EntityTargetEvent.TargetReason.CUSTOM);
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
                new NamespacedKey(plugin, "pvz_lane"),
                PersistentDataType.STRING,
                "one");
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

}
