package com.rz.dave;
import com.rz.dave.pvz.PvzMode;
import com.rz.dave.pvz.PvzLane;
import com.rz.dave.pvz.PvzClass;

import org.bukkit.Location;
import org.bukkit.Material;
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
    void machineGunnerKitIsUnbreakableDispenser() {
        ItemStack[] kit = PvzClass.MACHINE_GUNNER.createKit();
        assertEquals(1, kit.length);
        assertEquals(Material.DISPENSER, kit[0].getType());
        assertTrue(kit[0].getItemMeta().isUnbreakable());
        assertTrue(PvzClass.MACHINE_GUNNER.isShooter());
    }

    @Test
    void iceShooterKitIsUnbreakableDispenser() {
        ItemStack[] kit = PvzClass.ICE_SHOOTER.createKit();
        assertEquals(1, kit.length);
        assertEquals(Material.DISPENSER, kit[0].getType());
        assertTrue(kit[0].getItemMeta().isUnbreakable());
        assertTrue(PvzClass.ICE_SHOOTER.isShooter());
    }

    @Test
    void wallnutHasNoWeapon() {
        ItemStack[] kit = PvzClass.WALLNUT.createKit();
        assertEquals(0, kit.length, "坚果没有武器");
        assertFalse(PvzClass.WALLNUT.hasWeapon());
    }

    @Test
    void shooterArmorsAreDyedLeatherAndUnbreakable() {
        for (PvzClass clazz : PvzClass.values()) {
            ItemStack[] armor = clazz.createArmor();
            assertEquals(4, armor.length, clazz + " 护甲应为 4 件");
            for (ItemStack piece : armor) {
                assertTrue(piece.getType().name().startsWith("LEATHER_"), clazz + " 护甲应为皮革套");
                assertTrue(piece.getItemMeta().isUnbreakable(), clazz + " 护甲默认无限耐久");
                assertNotNull(((org.bukkit.inventory.meta.LeatherArmorMeta) piece.getItemMeta()).getColor(),
                        clazz + " 护甲应有染色");
            }
        }
        // 三职业颜色不同：浅绿 / 浅蓝 / 棕
        org.bukkit.inventory.meta.LeatherArmorMeta machine =
                (org.bukkit.inventory.meta.LeatherArmorMeta) PvzClass.MACHINE_GUNNER.createArmor()[0].getItemMeta();
        org.bukkit.inventory.meta.LeatherArmorMeta ice =
                (org.bukkit.inventory.meta.LeatherArmorMeta) PvzClass.ICE_SHOOTER.createArmor()[0].getItemMeta();
        org.bukkit.inventory.meta.LeatherArmorMeta wallnut =
                (org.bukkit.inventory.meta.LeatherArmorMeta) PvzClass.WALLNUT.createArmor()[0].getItemMeta();
        assertTrue(!machine.getColor().equals(ice.getColor()) && !ice.getColor().equals(wallnut.getColor()),
                "三职业护甲颜色应各不相同");
    }

    @Test
    void randomClassIsAlwaysOneOfThree() {
        for (int i = 0; i < 200; i++) {
            PvzClass clazz = PvzClass.random();
            assertTrue(clazz == PvzClass.MACHINE_GUNNER || clazz == PvzClass.ICE_SHOOTER
                    || clazz == PvzClass.WALLNUT);
        }
    }

    @Test
    void randomClassSelectionUsesReplacement() {
        assertEquals(PvzClass.MACHINE_GUNNER, PvzClass.fromRandomIndex(0));
        assertEquals(PvzClass.ICE_SHOOTER, PvzClass.fromRandomIndex(1));
        assertEquals(PvzClass.WALLNUT, PvzClass.fromRandomIndex(2));
        assertEquals(PvzClass.MACHINE_GUNNER, PvzClass.fromRandomIndex(3));
    }

    @Test
    void blindBoxSummonDistribution() {
        // 50% 苦力怕 / 20% 普通 / 10% 巨人 / 10% 路障 / 10% 铁桶
        assertEquals(com.rz.dave.monster.MonsterManager.MonsterType.SUMMON_CREEPER,
                PvzMode.pickSummonType(0.0));
        assertEquals(com.rz.dave.monster.MonsterManager.MonsterType.SUMMON_CREEPER,
                PvzMode.pickSummonType(0.4999));
        assertEquals(com.rz.dave.monster.MonsterManager.MonsterType.PLAIN_ZOMBIE,
                PvzMode.pickSummonType(0.5));
        assertEquals(com.rz.dave.monster.MonsterManager.MonsterType.PLAIN_ZOMBIE,
                PvzMode.pickSummonType(0.6999));
        assertEquals(com.rz.dave.monster.MonsterManager.MonsterType.GIANT_ZOMBIE,
                PvzMode.pickSummonType(0.7));
        assertEquals(com.rz.dave.monster.MonsterManager.MonsterType.GIANT_ZOMBIE,
                PvzMode.pickSummonType(0.7999));
        assertEquals(com.rz.dave.monster.MonsterManager.MonsterType.CONEHEAD_ZOMBIE,
                PvzMode.pickSummonType(0.8));
        assertEquals(com.rz.dave.monster.MonsterManager.MonsterType.CONEHEAD_ZOMBIE,
                PvzMode.pickSummonType(0.8999));
        assertEquals(com.rz.dave.monster.MonsterManager.MonsterType.BUCKETHEAD_ZOMBIE,
                PvzMode.pickSummonType(0.9));
        assertEquals(com.rz.dave.monster.MonsterManager.MonsterType.BUCKETHEAD_ZOMBIE,
                PvzMode.pickSummonType(0.9999));
    }

    @Test
    void laneHitBaseDecrementsAndEliminatesAtZero() {
        PvzLane lane = new PvzLane("one", "1路", 10);
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
        PvzLane lane = new PvzLane("two", "2路", 10);
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
        PvzLane lane = new PvzLane("three", "3路", 10);
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
        assertEquals(25.0, PvzMode.BLIND_BOX_HEALTH, 1e-9, "盲盒僵尸血量固定 25");
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
