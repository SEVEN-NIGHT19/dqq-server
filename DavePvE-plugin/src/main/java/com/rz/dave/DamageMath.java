package com.rz.dave;
import com.rz.dave.shop.ShopCurrency;

import java.util.HashMap;
import java.util.Map;

/**
 * 集中管理伤害/Boss 技能相关的数值常量（纯计算，无 Bukkit 依赖），
 * 便于单元测试与数值调整。改数值时优先改这里并同步更新测试。
 */
public final class DamageMath {
    private DamageMath() {
    }

    // ---- 怪物攻击伤害倍率（DaveManager.handleDamage 使用）----
    public static final double WARDEN_ATTACK_MULTIPLIER = 0.25;
    public static final double CREEPER_ATTACK_MULTIPLIER = 2.0 / 3.0;
    public static final double WITHER_ATTACK_MULTIPLIER = 0.5;
    public static final double WITHER_SKULL_ATTACK_MULTIPLIER = 0.5;
    public static final double BIG_BOSS_INCOMING_MULTIPLIER = 0.4;
    public static final double REGULAR_MOB_PROJECTILE_MULTIPLIER = 1.5;

    // ---- 陨石（spawnWitherMeteor）----
    public static final double METEOR_START_HEIGHT = 40.0;
    public static final double METEOR_FALL_PER_TICK = 0.625;
    public static final double METEOR_DAMAGE = 30.0;
    public static final int METEOR_RADIUS = 10;

    // ---- 监守者震地（wardenSlamLanding）----
    public static final int WARDEN_SLAM_DAMAGE_RADIUS = 10;
    public static final int WARDEN_SLAM_KNOCKBACK_RADIUS = 15;
    public static final double WARDEN_SLAM_DAMAGE = 10.0;
    public static final double WARDEN_SLAM_KNOCKBACK_STRENGTH = 2.0;
    public static final double WARDEN_SLAM_KNOCKBACK_UP = 0.8;

    // ---- 仙人掌射手 ----
    public static final int CACTUS_PIERCE_MAX = 3;               // 普通子弹穿透敌人上限
    public static final double CACTUS_SKILL_BULLET_DAMAGE = 10.0; // 技能子弹伤害（闪电伤害不变）

    // ---- 长矛突进 ----
    public static final double SPEAR_DASH_VELOCITY = 1.0;         // 水平初速度，位移约 10 格（需服务器微调）

    /** 货币换算倍率：银=1、金=10、钻=100。 */
    public static int currencyMultiplier(ShopCurrency currency) {
        return switch (currency) {
            case SILVER -> 1;
            case GOLD -> 10;
            case DIAMOND -> 100;
            case STAR -> 0;
        };
    }

    /** 弓技能爆炸中心伤害：5/10/15 级 → 20/30/50，爆炸范围不变。 */
    public static double bowExplosionCenterDamage(int tier) {
        return tier >= 15 ? 50.0 : tier >= 10 ? 30.0 : 20.0;
    }

    /** 死战休整期钻币发放：低(15)→1、中(30)→2、高(50)→4。 */
    public static int deathRestDiamonds(int economyTier) {
        return switch (economyTier) {
            case 15 -> 1;
            case 50 -> 4;
            default -> 2;
        };
    }

    /** 支付方案：各货币应扣数量 + 找零银币数（钻→金→银 顺序扣款）。 */
    public record PaymentPlan(Map<ShopCurrency, Integer> taken, int changeSilver) {
    }

    public static PaymentPlan paymentPlan(int priceSilver, Map<ShopCurrency, Integer> available) {
        Map<ShopCurrency, Integer> taken = new HashMap<>();
        int need = priceSilver;
        for (ShopCurrency currency : new ShopCurrency[]{ShopCurrency.DIAMOND, ShopCurrency.GOLD, ShopCurrency.SILVER}) {
            if (need <= 0) {
                break;
            }
            int multiplier = currencyMultiplier(currency);
            int avail = available.getOrDefault(currency, 0);
            if (avail <= 0) {
                continue;
            }
            int take = Math.min(avail, (need + multiplier - 1) / multiplier);
            taken.put(currency, take);
            need -= take * multiplier;
        }
        return new PaymentPlan(taken, need > 0 ? 0 : -need);
    }
}
