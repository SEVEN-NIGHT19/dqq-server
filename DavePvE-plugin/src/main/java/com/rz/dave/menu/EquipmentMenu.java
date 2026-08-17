package com.rz.dave.menu;
import com.rz.dave.DaveManager;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public final class EquipmentMenu implements InventoryHolder {
    public static final String TITLE = "装备升级";
    public static final int BACK_SLOT = 26;
    private static final EquipmentCatalog.Kind[] KINDS = {
            EquipmentCatalog.Kind.HELMET,
            EquipmentCatalog.Kind.CHESTPLATE,
            EquipmentCatalog.Kind.LEGGINGS,
            EquipmentCatalog.Kind.BOOTS,
            EquipmentCatalog.Kind.SWORD,
            EquipmentCatalog.Kind.AXE,
            EquipmentCatalog.Kind.SPEAR,
            EquipmentCatalog.Kind.BOW,
            EquipmentCatalog.Kind.CROSSBOW,
            EquipmentCatalog.Kind.TRIDENT,
            EquipmentCatalog.Kind.MACE,
            EquipmentCatalog.Kind.SHOOTER,
            EquipmentCatalog.Kind.CACTUS_SHOOTER,
            EquipmentCatalog.Kind.BIG_PUFFSHROOM,
            EquipmentCatalog.Kind.SMALL_PUFFSHROOM,
            EquipmentCatalog.Kind.TIMID_SHROOM
    };

    private final Inventory inventory;

    public EquipmentMenu() {
        this.inventory = Bukkit.createInventory(this, 27, TITLE);
        for (int i = 0; i < KINDS.length; i++) {
            inventory.setItem(i, entry(KINDS[i]));
        }
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta backMeta = back.getItemMeta();
        backMeta.setItemName("返回");
        backMeta.setLore(List.of(ChatColor.YELLOW + "返回戴夫商店第一页"));
        back.setItemMeta(backMeta);
        inventory.setItem(BACK_SLOT, back);
    }

    public EquipmentCatalog.Kind kindForSlot(int slot) {
        if (slot < 0 || slot >= KINDS.length) {
            return null;
        }
        return KINDS[slot];
    }

    private static ItemStack entry(EquipmentCatalog.Kind kind) {
        ItemStack stack = new ItemStack(kind.icon());
        ItemMeta meta = stack.getItemMeta();
        meta.setItemName(kind.display());
        meta.setLore(List.of(
                ChatColor.GRAY + (kind.hasMaterialUpgrade() ? "材质升级 + 附魔升级" : "附魔升级"),
                ChatColor.YELLOW + "点击进入"));
        stack.setItemMeta(meta);
        return stack;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
