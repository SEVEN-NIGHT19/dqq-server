package com.rz.dave;
import com.rz.dave.DaveManager;
import com.rz.dave.monster.MonsterManager;
import com.rz.dave.pvz.PvzMode;
import com.rz.dave.shop.ShopItem;
import com.rz.dave.listener.GameListener;
import com.rz.dave.shop.ShopCurrency;

import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Zombie;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.scoreboard.Scoreboard;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 用 MockBukkit 验证商店货币、旁观者限制、刷怪点交互与坠落免疫。 */
class DaveManagerMockTest {

    private ServerMock server;
    private DaveManager manager;
    private GameListener listener;

    @BeforeEach
    void setUp() throws Exception {
        server = MockBukkit.mock();
        Plugin plugin = newBarePlugin();
        manager = new DaveManager(plugin, new MonsterManager(plugin));
        listener = new GameListener(manager);
    }

    /** MockBukkit 4.110.0 的 createMockPlugin 在 JDK 21 下会因缺少 jar 报错，这里用动态代理构造最小插件实例。 */
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
                    if (method.getName().equals("isEnabled")) {
                        return true;
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

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void shopPurchaseReturnsChange() {
        PlayerMock player = server.addPlayer();
        manager.addCurrency(player, ShopCurrency.GOLD, 1);

        ShopItem food = new ShopItem("食物", new ItemStack(Material.BREAD), ShopCurrency.SILVER, 1, null);
        assertTrue(manager.purchase(player, food));

        assertEquals(0, countCurrency(player, ShopCurrency.GOLD));
        assertEquals(9, countCurrency(player, ShopCurrency.SILVER));
    }

    @Test
    void deathModeConsolidationKeepsValue() {
        PlayerMock player = server.addPlayer();
        manager.addCurrency(player, ShopCurrency.SILVER, 10);
        manager.deathMode = true;

        manager.consolidateCurrencies(player);

        assertEquals(0, countCurrency(player, ShopCurrency.SILVER));
        assertEquals(1, countCurrency(player, ShopCurrency.GOLD));
    }

    @Test
    void spectatorCannotInteract() {
        PlayerMock player = server.addPlayer();
        player.setGameMode(GameMode.SPECTATOR);

        PlayerInteractEvent event = new PlayerInteractEvent(player, Action.RIGHT_CLICK_AIR,
                new ItemStack(Material.STICK), null, null, EquipmentSlot.HAND);
        listener.onInteract(event);

        assertTrue(event.isCancelled());
    }

    @Test
    void spawnPointInteractionIsCancelled() {
        PlayerMock player = server.addPlayer();
        var world = server.addSimpleWorld("world");
        ArmorStand stand = world.spawn(new Location(world, 0, 0, 0), ArmorStand.class);
        stand.addScoreboardTag("rz");
        stand.addScoreboardTag("summon_point");

        PlayerInteractEntityEvent event = new PlayerInteractEntityEvent(player, stand, EquipmentSlot.HAND);
        listener.onSpawnPointInteract(event);

        assertTrue(event.isCancelled());
    }

    @Test
    void windMaceFallImmunityCancelsFallDamage() {
        PlayerMock player = server.addPlayer();
        manager.grantWindMaceFallImmunity(player);

        EntityDamageEvent event = new EntityDamageEvent(player, EntityDamageEvent.DamageCause.FALL, 10.0);
        listener.onFallDamage(event);

        assertTrue(event.isCancelled());
        assertTrue(manager.isWindMaceFallImmune(player));
    }

    @Test
    void fallDamageAppliesWithoutImmunity() {
        PlayerMock player = server.addPlayer();

        EntityDamageEvent event = new EntityDamageEvent(player, EntityDamageEvent.DamageCause.FALL, 10.0);
        listener.onFallDamage(event);

        assertFalse(event.isCancelled());
    }

    @Test
    void classicCleanupDoesNotRemovePvzMonsters() throws Exception {
        var world = server.addSimpleWorld("world");
        Zombie pvzMonster = world.spawn(new Location(world, 0, 0, 0), Zombie.class);
        pvzMonster.addScoreboardTag(PvzMode.TAG_MONSTER);
        Zombie classicMonster = world.spawn(new Location(world, 1, 0, 0), Zombie.class);
        classicMonster.addScoreboardTag("rz");
        classicMonster.addScoreboardTag("monster");

        Method cleanup = DaveManager.class.getDeclaredMethod("killAllMonsters");
        cleanup.setAccessible(true);
        cleanup.invoke(manager);

        assertTrue(pvzMonster.isValid(), "经典模式清理不得移除 PVZ 怪物");
        assertFalse(classicMonster.isValid(), "经典模式清理应移除经典怪物");
    }

    @Test
    void classicEndDoesNotResetPvzPlayer() {
        PlayerMock pvzPlayer = server.addPlayer("PvzPlayer");
        pvzPlayer.setGameMode(GameMode.SPECTATOR);
        pvzPlayer.getInventory().setItem(0, new ItemStack(Material.BOW));
        manager.setPvzPlayer(pvzPlayer.getUniqueId(), true);

        assertFalse(manager.isClassicPlayer(pvzPlayer),
                "经典模式结束流程必须跳过 PVZ 玩家");
        assertEquals(GameMode.SPECTATOR, pvzPlayer.getGameMode());
        assertEquals(Material.BOW, pvzPlayer.getInventory().getItem(0).getType());
    }

    @Test
    void classicScoreboardResetDoesNotReplacePvzScoreboard() throws Exception {
        PlayerMock pvzPlayer = server.addPlayer("PvzBoardPlayer");
        manager.setPvzPlayer(pvzPlayer.getUniqueId(), true);
        Scoreboard pvzBoard = server.getScoreboardManager().getNewScoreboard();
        pvzPlayer.setScoreboard(pvzBoard);

        Method reset = DaveManager.class.getDeclaredMethod("resetGameScoreboard");
        reset.setAccessible(true);
        reset.invoke(manager);

        assertSame(pvzBoard, pvzPlayer.getScoreboard(),
                "经典模式结束不得替换 PVZ 独立记分板");
    }

    @Test
    void classicDamageHandlerIgnoresPvzPlayers() {
        PlayerMock pvzPlayer = server.addPlayer("PvzDamagePlayer");
        PlayerMock target = server.addPlayer("ClassicTarget");
        manager.setPvzPlayer(pvzPlayer.getUniqueId(), true);

        EntityDamageByEntityEvent event = new EntityDamageByEntityEvent(
                pvzPlayer, target, EntityDamageEvent.DamageCause.ENTITY_ATTACK, 1.0);
        manager.handleDamage(event);

        assertFalse(event.isCancelled(), "经典伤害逻辑不得处理 PVZ 玩家");
    }

    private int countCurrency(PlayerMock player, ShopCurrency currency) {
        int total = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null || item.getType() != currency.material()
                    || item.getItemMeta() == null) {
                continue;
            }
            if (currency.displayName().equals(item.getItemMeta().getItemName())) {
                total += item.getAmount();
            }
        }
        return total;
    }
}
