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
    void randomClassIsAlwaysOneOfFive() {
        for (int i = 0; i < 200; i++) {
            PvzClass clazz = PvzClass.random();
            assertTrue(clazz == PvzClass.MACHINE_GUNNER || clazz == PvzClass.ICE_SHOOTER
                    || clazz == PvzClass.WALLNUT || clazz == PvzClass.SNIPER
                    || clazz == PvzClass.DUAL_SHOOTER);
        }
    }

    @Test
    void randomClassSelectionUsesReplacement() {
        assertEquals(PvzClass.MACHINE_GUNNER, PvzClass.fromRandomIndex(0));
        assertEquals(PvzClass.ICE_SHOOTER, PvzClass.fromRandomIndex(1));
        assertEquals(PvzClass.WALLNUT, PvzClass.fromRandomIndex(2));
        assertEquals(PvzClass.SNIPER, PvzClass.fromRandomIndex(3));
        assertEquals(PvzClass.DUAL_SHOOTER, PvzClass.fromRandomIndex(4));
        assertEquals(PvzClass.MACHINE_GUNNER, PvzClass.fromRandomIndex(5));
    }

    @Test
    void sniperAndDualKits() {
        ItemStack sniper = PvzClass.SNIPER.createKit()[0];
        assertEquals(Material.SPYGLASS, sniper.getType(), "狙击豌豆武器为狙击镜");
        assertTrue(sniper.getItemMeta().isUnbreakable());
        ItemStack dual = PvzClass.DUAL_SHOOTER.createKit()[0];
        assertEquals(Material.DISPENSER, dual.getType(), "双发射手武器为发射器（大模式双发）");
    }

    @Test
    void sniperAndDualArmorColors() {
        org.bukkit.inventory.meta.LeatherArmorMeta sniperHelmet = (org.bukkit.inventory.meta.LeatherArmorMeta)
                PvzClass.SNIPER.createArmor()[3].getItemMeta();
        assertEquals(org.bukkit.Color.fromRGB(0x000000), sniperHelmet.getColor(), "狙击头盔应为黑色");
        org.bukkit.inventory.meta.LeatherArmorMeta sniperChest = (org.bukkit.inventory.meta.LeatherArmorMeta)
                PvzClass.SNIPER.createArmor()[2].getItemMeta();
        org.bukkit.inventory.meta.LeatherArmorMeta dualHelmet = (org.bukkit.inventory.meta.LeatherArmorMeta)
                PvzClass.DUAL_SHOOTER.createArmor()[3].getItemMeta();
        org.bukkit.inventory.meta.LeatherArmorMeta dualChest = (org.bukkit.inventory.meta.LeatherArmorMeta)
                PvzClass.DUAL_SHOOTER.createArmor()[2].getItemMeta();
        assertTrue(!sniperChest.getColor().equals(sniperHelmet.getColor()), "狙击套与帽子颜色应不同");
        assertTrue(!dualChest.getColor().equals(dualHelmet.getColor()), "双发套与帽子颜色应不同");
    }

    @Test
    void randomClassDistributionIsUniform() {
        // 独立同分布验证：9000 次采样，5 个职业每个占比应接近 1/5（阈值取 ±5% 防抖动）
        int[] counts = new int[PvzClass.values().length];
        int n = 9000;
        for (int i = 0; i < n; i++) {
            counts[PvzClass.random().ordinal()]++;
        }
        for (int count : counts) {
            double ratio = (double) count / n;
            assertTrue(ratio > 0.15 && ratio < 0.25,
                    "职业分布应接近 1/5，实际 " + ratio);
        }
    }

    @Test
    void blindBoxSummonDistribution() {
        // 10% 苦力怕 / 22.5% 普通 / 22.5% 巨人 / 22.5% 路障 / 22.5% 铁桶
        assertEquals(com.rz.dave.monster.MonsterManager.MonsterType.SUMMON_CREEPER,
                PvzMode.pickSummonType(0.0));
        assertEquals(com.rz.dave.monster.MonsterManager.MonsterType.SUMMON_CREEPER,
                PvzMode.pickSummonType(0.0999));
        assertEquals(com.rz.dave.monster.MonsterManager.MonsterType.PLAIN_ZOMBIE,
                PvzMode.pickSummonType(0.1));
        assertEquals(com.rz.dave.monster.MonsterManager.MonsterType.PLAIN_ZOMBIE,
                PvzMode.pickSummonType(0.3249));
        assertEquals(com.rz.dave.monster.MonsterManager.MonsterType.GIANT_ZOMBIE,
                PvzMode.pickSummonType(0.325));
        assertEquals(com.rz.dave.monster.MonsterManager.MonsterType.GIANT_ZOMBIE,
                PvzMode.pickSummonType(0.5499));
        assertEquals(com.rz.dave.monster.MonsterManager.MonsterType.CONEHEAD_ZOMBIE,
                PvzMode.pickSummonType(0.55));
        assertEquals(com.rz.dave.monster.MonsterManager.MonsterType.CONEHEAD_ZOMBIE,
                PvzMode.pickSummonType(0.7749));
        assertEquals(com.rz.dave.monster.MonsterManager.MonsterType.BUCKETHEAD_ZOMBIE,
                PvzMode.pickSummonType(0.775));
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
        assertEquals(5, PvzMode.MACHINE_BURST_COUNT, "机枪射手 5 连发");
        assertEquals(7000L, PvzMode.SNIPER_COOLDOWN_MS, "狙击豌豆冷却 7 秒");
    }

    @Test
    void dualKindDistributionIsSevenEqual() {
        // 七种子弹各 1/7：豌豆/冰豆/火豆/橙火豆/红火豆/冰火豌豆/幽蓝火焰豌豆
        assertEquals("machine", PvzMode.pickDualKind(0.0));
        assertEquals("machine", PvzMode.pickDualKind(1.0 / 7 - 0.001));
        assertEquals("ice", PvzMode.pickDualKind(1.0 / 7));
        assertEquals("ice", PvzMode.pickDualKind(2.0 / 7 - 0.001));
        assertEquals("fire_small", PvzMode.pickDualKind(2.0 / 7));
        assertEquals("fire_small", PvzMode.pickDualKind(3.0 / 7 - 0.001));
        assertEquals("fire_orange", PvzMode.pickDualKind(3.0 / 7));
        assertEquals("fire_orange", PvzMode.pickDualKind(4.0 / 7 - 0.001));
        assertEquals("fire_red", PvzMode.pickDualKind(4.0 / 7));
        assertEquals("fire_red", PvzMode.pickDualKind(5.0 / 7 - 0.001));
        assertEquals("ice_fire", PvzMode.pickDualKind(5.0 / 7));
        assertEquals("ice_fire", PvzMode.pickDualKind(6.0 / 7 - 0.001));
        assertEquals("dragon", PvzMode.pickDualKind(6.0 / 7));
        assertEquals("dragon", PvzMode.pickDualKind(0.9999));
    }

    @Test
    void armorTypeTagsDefined() {
        assertEquals("pvz_armored", com.rz.dave.monster.MonsterManager.TAG_ARMORED, "二类防具（帽子）");
        assertEquals("pvz_shield_armored", com.rz.dave.monster.MonsterManager.TAG_SHIELDED, "三类防具（手持盾）");
        assertEquals(0.7, com.rz.dave.monster.MonsterManager.MOVEMENT_SPEED_MULTIPLIER, 1e-9,
                "全体怪物移速 0.7 倍");
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
