package com.rz.dave.team;
import com.rz.dave.DaveManager;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public final class TeamChestHolder implements InventoryHolder {
    private final String team;

    public TeamChestHolder(String team) {
        this.team = team;
    }

    public String team() {
        return team;
    }

    @Override
    public Inventory getInventory() {
        return null;
    }
}
