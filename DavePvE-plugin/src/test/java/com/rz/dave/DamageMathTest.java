package com.rz.dave;

import org.junit.jupiter.api.Test;
import org.bukkit.enchantments.Enchantment;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** DamageMath 数值常量与 DaveManager 纯计算方法的回归测试。 */
class DamageMathTest {

    private static final double EPS = 1e-9;

    @Test
    void bossBalanceConstants() {
        assertEquals(0.4, DamageMath.BIG_BOSS_INCOMING_MULTIPLIER, EPS);
        assertEquals(0.25, DamageMath.WARDEN_ATTACK_MULTIPLIER, EPS);
        assertEquals(0.5, DamageMath.WITHER_ATTACK_MULTIPLIER, EPS);
        assertEquals(0.5, DamageMath.WITHER_SKULL_ATTACK_MULTIPLIER, EPS);
        assertEquals(2.0 / 3.0, DamageMath.CREEPER_ATTACK_MULTIPLIER, EPS);
        assertEquals(1.5, DamageMath.REGULAR_MOB_PROJECTILE_MULTIPLIER, EPS);
    }

    @Test
    void meteorBalanceConstants() {
        assertEquals(40.0, DamageMath.METEOR_START_HEIGHT, EPS);
        assertEquals(0.625, DamageMath.METEOR_FALL_PER_TICK, EPS);
        assertEquals(30.0, DamageMath.METEOR_DAMAGE, EPS);
        assertEquals(10, DamageMath.METEOR_RADIUS);
    }

    @Test
    void wardenSlamBalanceConstants() {
        assertEquals(10, DamageMath.WARDEN_SLAM_DAMAGE_RADIUS);
        assertEquals(15, DamageMath.WARDEN_SLAM_KNOCKBACK_RADIUS);
        assertEquals(10.0, DamageMath.WARDEN_SLAM_DAMAGE, EPS);
        assertEquals(2.0, DamageMath.WARDEN_SLAM_KNOCKBACK_STRENGTH, EPS);
        assertEquals(0.8, DamageMath.WARDEN_SLAM_KNOCKBACK_UP, EPS);
    }

    @Test
    void swordLevelBonusMatchesDocumentedCurve() {
        assertEquals(0.5, DaveManager.rangedBonusForLevel(EquipmentCatalog.Kind.SWORD, 1), EPS);
        assertEquals(2.5, DaveManager.rangedBonusForLevel(EquipmentCatalog.Kind.SWORD, 5), EPS);
        assertEquals(3.5, DaveManager.rangedBonusForLevel(EquipmentCatalog.Kind.SWORD, 6), EPS);
    }

    @Test
    void rangedWeaponDamageAddsBaseAndBonus() {
        assertEquals(8.5, DaveManager.rangedWeaponDamage(8.0, EquipmentCatalog.Kind.SWORD, 1), EPS);
        assertEquals(13.0, DaveManager.rangedWeaponDamage(8.0, EquipmentCatalog.Kind.BOW, 5), EPS);
        assertEquals(10.0, DaveManager.rangedWeaponDamage(5.0, EquipmentCatalog.Kind.SMALL_PUFFSHROOM, 5), EPS);
        assertEquals(10.5, DaveManager.rangedWeaponDamage(8.0, EquipmentCatalog.Kind.CROSSBOW, 5), EPS);
    }

    @Test
    void cactusBalanceConstants() {
        assertEquals(3, DamageMath.CACTUS_PIERCE_MAX);
        assertEquals(10.0, DamageMath.CACTUS_SKILL_BULLET_DAMAGE, EPS);
    }

    @Test
    void spearDashVelocityConstant() {
        assertEquals(1.0, DamageMath.SPEAR_DASH_VELOCITY, EPS);
    }

    @Test
    void bowExplosionCenterDamageByTier() {
        assertEquals(20.0, DamageMath.bowExplosionCenterDamage(5), EPS);
        assertEquals(30.0, DamageMath.bowExplosionCenterDamage(10), EPS);
        assertEquals(50.0, DamageMath.bowExplosionCenterDamage(15), EPS);
    }

    @Test
    void deathRestDiamondsByEconomyTier() {
        assertEquals(1, DamageMath.deathRestDiamonds(15));
        assertEquals(2, DamageMath.deathRestDiamonds(30));
        assertEquals(4, DamageMath.deathRestDiamonds(50));
    }

    @Test
    void paymentPlanGivesChangeForOverpay() {
        DamageMath.PaymentPlan plan = DamageMath.paymentPlan(1, Map.of(ShopCurrency.GOLD, 1));
        assertEquals(Map.of(ShopCurrency.GOLD, 1), plan.taken());
        assertEquals(9, plan.changeSilver());
    }

    @Test
    void paymentPlanUsesDiamondThenGold() {
        DamageMath.PaymentPlan plan = DamageMath.paymentPlan(105, Map.of(ShopCurrency.DIAMOND, 1, ShopCurrency.GOLD, 1));
        assertEquals(Map.of(ShopCurrency.DIAMOND, 1, ShopCurrency.GOLD, 1), plan.taken());
        assertEquals(5, plan.changeSilver());
    }

    @Test
    void paymentPlanExactPaymentHasNoChange() {
        DamageMath.PaymentPlan plan = DamageMath.paymentPlan(100, Map.of(ShopCurrency.DIAMOND, 1));
        assertEquals(Map.of(ShopCurrency.DIAMOND, 1), plan.taken());
        assertEquals(0, plan.changeSilver());
    }

    @Test
    void enchantDisplayNamesAreChinese() {
        assertEquals("突刺", DaveManager.enchantDisplayName(Enchantment.LUNGE));
        assertEquals("摔落保护", DaveManager.enchantDisplayName(Enchantment.FEATHER_FALLING));
    }
}
