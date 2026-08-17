package com.rz.dave;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public final class WaveMenu implements InventoryHolder {
    public static final String TITLE = "调整波次";
    public static final int BACK_SLOT = 26;
    public static final int FIRST_WAVE_SLOT = 0;

    private final Inventory inventory;

    public WaveMenu() {
        this.inventory = Bukkit.createInventory(this, 27, TITLE);
        for (int i = 0; i < 12; i++) {
            ItemStack stack = new ItemStack(Material.PAPER);
            ItemMeta meta = stack.getItemMeta();
            meta.setItemName("第 " + (i + 1) + " 波");
            meta.setLore(List.of(ChatColor.YELLOW + "点击跳转到该波"));
            stack.setItemMeta(meta);
            inventory.setItem(FIRST_WAVE_SLOT + i, stack);
        }
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta backMeta = back.getItemMeta();
        backMeta.setItemName("返回");
        backMeta.setLore(List.of(ChatColor.YELLOW + "返回游戏控制"));
        back.setItemMeta(backMeta);
        inventory.setItem(BACK_SLOT, back);
    }

    public int waveForSlot(int slot) {
        if (slot < FIRST_WAVE_SLOT || slot >= FIRST_WAVE_SLOT + 12) {
            return -1;
        }
        return slot - FIRST_WAVE_SLOT + 1;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
