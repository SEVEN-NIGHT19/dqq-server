package com.rz.dave.monster;

import org.bukkit.NamespacedKey;

/**
 * 怪物生成上下文：携带生成怪物所需的基础参数。
 *
 * <p>各怪物类从上下文中取用自己关心的字段；
 * 与波次无关的怪物（如召唤系）可将不使用的字段置为默认值（0）。
 *
 * @param laneId           所属路线 ID
 * @param laneKey          路线持久化 Key
 * @param maxHp            波次血量（仅随波次成长的怪物使用）
 * @param attackMultiplier 波次攻击倍率（仅随波次成长的怪物使用）
 */
public record SpawnContext(String laneId, NamespacedKey laneKey,
                           double maxHp, double attackMultiplier) {

    /** 与波次无关的生成上下文（血量/攻击倍率使用默认值 0）。 */
    public static SpawnContext basic(String laneId, NamespacedKey laneKey) {
        return new SpawnContext(laneId, laneKey, 0.0, 0.0);
    }
}
