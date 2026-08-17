package com.rz.dave;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 装备目录纯逻辑测试：不需要模拟服务器。 */
class EquipmentCatalogTest {

    @Test
    void enchantPriceDoublesPerLevel() {
        EquipmentCatalog.EnchantEntry entry = new EquipmentCatalog.EnchantEntry(null, 20, 5);
        assertEquals(20, EquipmentCatalog.enchantPrice(entry, 1));
        assertEquals(40, EquipmentCatalog.enchantPrice(entry, 2));
        assertEquals(80, EquipmentCatalog.enchantPrice(entry, 3));
        assertEquals(160, EquipmentCatalog.enchantPrice(entry, 4));
        assertEquals(320, EquipmentCatalog.enchantPrice(entry, 5));
    }

    @Test
    void swordGetsExpectedEnchants() {
        List<EquipmentCatalog.EnchantEntry> enchants = EquipmentCatalog.enchantsFor(EquipmentCatalog.Kind.SWORD);
        assertEquals(6, enchants.size());
        assertEquals(EquipmentCatalog.EnchantEntry.class, enchants.get(0).getClass());
        assertTrue(enchants.stream().anyMatch(e -> e.maxLevel() == 5));
    }

    @Test
    void spearHasLungeInAdditionToSwordEnchants() {
        List<EquipmentCatalog.EnchantEntry> sword = EquipmentCatalog.enchantsFor(EquipmentCatalog.Kind.SWORD);
        List<EquipmentCatalog.EnchantEntry> spear = EquipmentCatalog.enchantsFor(EquipmentCatalog.Kind.SPEAR);
        assertEquals(sword.size() + 1, spear.size());
    }

    @Test
    void shooterHasNoEnchants() {
        assertTrue(EquipmentCatalog.enchantsFor(EquipmentCatalog.Kind.SHOOTER).isEmpty());
    }

    @Test
    void allKindsHaveDisplayAndIcon() {
        for (EquipmentCatalog.Kind kind : EquipmentCatalog.Kind.values()) {
            assertNotNull(kind.display());
            assertFalse(kind.display().isBlank(), kind.name() + " 缺少显示名");
            assertNotNull(kind.icon(), kind.name() + " 缺少图标");
        }
    }

    @Test
    void materialUpgradeFlags() {
        assertTrue(EquipmentCatalog.Kind.SWORD.hasMaterialUpgrade());
        assertTrue(EquipmentCatalog.Kind.CHESTPLATE.hasMaterialUpgrade());
        assertFalse(EquipmentCatalog.Kind.BOW.hasMaterialUpgrade());
        assertFalse(EquipmentCatalog.Kind.TRIDENT.hasMaterialUpgrade());
    }
}
