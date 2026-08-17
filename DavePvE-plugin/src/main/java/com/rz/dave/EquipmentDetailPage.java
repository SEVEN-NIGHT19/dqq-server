package com.rz.dave;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public final class EquipmentDetailPage implements InventoryHolder {
    public static final int MATERIAL_SLOT = 0;
    public static final int SHOOTER_FIRE_SLOT = 0;
    public static final int SHOOTER_NORMAL_SLOT = 1;
    public static final int SHOOTER_ICE_SLOT = 2;
    public static final int SHOOTER_SNIPER_SLOT = 3;
    public static final int DAMAGE_SLOT = 8;
    public static final int ENCHANT_START_SLOT = 9;
    public static final int BACK_SLOT = 26;

    private final Inventory inventory;
    private final EquipmentCatalog.Kind kind;

    public EquipmentDetailPage(Player player, DaveManager manager, EquipmentCatalog.Kind kind) {
        this.kind = kind;
        this.inventory = Bukkit.createInventory(this, 27, kind.display() + "详情");
        if (kind == EquipmentCatalog.Kind.SHOOTER) {
            inventory.setItem(SHOOTER_FIRE_SLOT, manager.buildShooterBranchButton(player, EquipmentCatalog.SHOOTER_FIRE_SLOT));
            inventory.setItem(SHOOTER_NORMAL_SLOT, manager.buildShooterBranchButton(player, EquipmentCatalog.SHOOTER_NORMAL_SLOT));
            inventory.setItem(SHOOTER_ICE_SLOT, manager.buildShooterBranchButton(player, EquipmentCatalog.SHOOTER_ICE_SLOT));
            inventory.setItem(SHOOTER_SNIPER_SLOT, manager.buildShooterBranchButton(player, EquipmentCatalog.SHOOTER_SNIPER_SLOT));
            ItemStack back = new ItemStack(Material.ARROW);
            ItemMeta backMeta = back.getItemMeta();
            backMeta.setItemName("返回");
            backMeta.setLore(List.of(ChatColor.YELLOW + "返回装备升级菜单"));
            back.setItemMeta(backMeta);
            inventory.setItem(BACK_SLOT, back);
            return;
        }
        if (kind.hasMaterialUpgrade()) {
            inventory.setItem(MATERIAL_SLOT, manager.buildEquipmentUpgradeButton(player, kind));
        } else {
            inventory.setItem(MATERIAL_SLOT, noMaterialInfo(kind));
        }
        if (kind != EquipmentCatalog.Kind.SHOOTER) {
            inventory.setItem(DAMAGE_SLOT, manager.buildWeaponDamageButton(player, kind));
        }
        List<EquipmentCatalog.EnchantEntry> enchants = EquipmentCatalog.enchantsFor(kind);
        for (int i = 0; i < enchants.size(); i++) {
            inventory.setItem(ENCHANT_START_SLOT + i, manager.buildEnchantButton(player, kind, enchants.get(i)));
        }
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta backMeta = back.getItemMeta();
        backMeta.setItemName("返回");
        backMeta.setLore(List.of(ChatColor.YELLOW + "返回装备升级菜单"));
        back.setItemMeta(backMeta);
        inventory.setItem(BACK_SLOT, back);
    }

    public EquipmentCatalog.Kind kind() {
        return kind;
    }

    public EquipmentCatalog.EnchantEntry enchantForSlot(int slot) {
        int index = slot - ENCHANT_START_SLOT;
        List<EquipmentCatalog.EnchantEntry> enchants = EquipmentCatalog.enchantsFor(kind);
        if (index < 0 || index >= enchants.size()) {
            return null;
        }
        return enchants.get(index);
    }

    private static ItemStack noMaterialInfo(EquipmentCatalog.Kind kind) {
        ItemStack stack = new ItemStack(Material.BARRIER);
        ItemMeta meta = stack.getItemMeta();
        meta.setItemName(kind.display() + "（无材质升级）");
        meta.setLore(List.of(ChatColor.GRAY + "该武器没有材质升级路线，仅提供附魔升级"));
        stack.setItemMeta(meta);
        return stack;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
