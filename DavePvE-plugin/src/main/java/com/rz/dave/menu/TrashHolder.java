package com.rz.dave.menu;
import com.rz.dave.DaveManager;

import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public final class TrashHolder implements InventoryHolder {
    public static final String TITLE = "垃圾桶";

    private final Inventory inventory;

    public TrashHolder() {
        this.inventory = Bukkit.createInventory(this, 27, TITLE);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
