package com.rz.dave;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** PVZ 模式纯逻辑测试：职业装备、路状态机、波次数值曲线。 */
class PvzModeTest {

    private static ServerMock server;

    @BeforeAll
    static void setUp() {
        server = MockBukkit.mock();
    }

    @AfterAll
    static void tearDown() {
        MockBukkit.unmock();
    }

    private static Location loc(double x, double y, double z) {
        return new Location(null, x, y, z);
    }

    @Test
    void swordsmanKitIsSingleUnbreakableIronSword() {
        ItemStack[] kit = PvzClass.SWORDSMAN.createKit();
        assertEquals(1, kit.length);
        assertEquals(Material.IRON_SWORD, kit[0].getType());
        assertTrue(kit[0].getItemMeta().isUnbreakable());
    }

    @Test
    void archerKitIsInfinityBowPlusOneArrow() {
        ItemStack[] kit = PvzClass.ARCHER.createKit();
        assertEquals(2, kit.length);
        assertEquals(Material.BOW, kit[0].getType());
        assertTrue(kit[0].getItemMeta().isUnbreakable());
        assertTrue(kit[0].getItemMeta().hasEnchant(Enchantment.INFINITY));
        assertEquals(Material.ARROW, kit[1].getType());
        assertEquals(1, kit[1].getAmount());
    }

    @Test
    void randomClassIsAlwaysOneOfTwo() {
        for (int i = 0; i < 200; i++) {
            PvzClass clazz = PvzClass.random();
            assertTrue(clazz == PvzClass.SWORDSMAN || clazz == PvzClass.ARCHER);
        }
    }

    @Test
    void randomClassSelectionUsesReplacement() {
        assertEquals(PvzClass.SWORDSMAN, PvzClass.fromRandomIndex(0));
        assertEquals(PvzClass.ARCHER, PvzClass.fromRandomIndex(1));
        assertEquals(PvzClass.SWORDSMAN, PvzClass.fromRandomIndex(2));
        assertEquals(PvzClass.ARCHER, PvzClass.fromRandomIndex(3));
    }

    @Test
    void laneHitBaseDecrementsAndEliminatesAtZero() {
        PvzLane lane = new PvzLane("red", "红队", 10);
        lane.setSpawn(loc(0, 0, 0));
        lane.setBase(loc(10, 0, 0));
        lane.setPlayerSpawn(loc(5, 0, 0));
        lane.setWorld("world");
        assertTrue(lane.isConfigured());
        for (int i = 0; i < 9; i++) {
            assertFalse(lane.hitBase());
        }
        assertTrue(lane.hitBase());
        assertEquals(0, lane.baseHealth());
        assertFalse(lane.isActive());
    }

    @Test
    void laneInactiveWithoutAlivePlayersOrWhenEliminated() {
        PvzLane lane = new PvzLane("blue", "蓝队", 10);
        lane.setAlivePlayers(2);
        assertTrue(lane.isActive());
        lane.setAlivePlayers(0);
        assertFalse(lane.isActive());
        lane.setAlivePlayers(1);
        lane.eliminate();
        assertFalse(lane.isActive());
    }

    @Test
    void laneResetRestoresFullHealthAndState() {
        PvzLane lane = new PvzLane("green", "绿队", 10);
        lane.setAlivePlayers(3);
        lane.hitBase();
        lane.hitBase();
        lane.eliminate();
        lane.reset(10);
        assertEquals(10, lane.baseHealth());
        assertEquals(10, lane.maxHealth());
        assertFalse(lane.eliminated());
        assertEquals(0, lane.alivePlayers());
    }

    @Test
    void waveMathScalesMonstersAndSpawns() {
        assertEquals(3, PvzMode.spawnsPerWave(0));
        assertEquals(8, PvzMode.spawnsPerWave(5));
        assertEquals(20.0, PvzMode.monsterHealth(0), 1e-9);
        assertEquals(30.0, PvzMode.monsterHealth(5), 1e-9);
    }

    @Test
    void spawnIntervalShrinksButNeverBelowFloor() {
        assertEquals(160, PvzMode.spawnIntervalTicks(0));
        assertEquals(110, PvzMode.spawnIntervalTicks(5));
        assertEquals(20, PvzMode.spawnIntervalTicks(50));
        assertEquals(20, PvzMode.spawnIntervalTicks(100));
    }

    @Test
    void spawnTimerUsesTwentyTickTaskSteps() {
        assertEquals(20, PvzMode.spawnTimerStepTicks());
    }

    @Test
    void blindBoxUsesHayBlockHelmet() {
        assertEquals(Material.HAY_BLOCK, PvzMode.blindBoxHelmet().getType());
    }
}
