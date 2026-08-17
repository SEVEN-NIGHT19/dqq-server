package com.rz.dave;

import org.bukkit.inventory.ItemStack;

import java.util.List;

public final class ShopCategory {
    private final String key;
    private final String title;
    private final ItemStack icon;
    private final List<ShopItem> items;
    private final boolean exchange;

    public ShopCategory(String key, String title, ItemStack icon, List<ShopItem> items, boolean exchange) {
        this.key = key;
        this.title = title;
        this.icon = icon;
        this.items = items;
        this.exchange = exchange;
    }

    public String key() {
        return key;
    }

    public String title() {
        return title;
    }

    public ItemStack icon() {
        return icon;
    }

    public List<ShopItem> items() {
        return items;
    }

    public boolean exchange() {
        return exchange;
    }
}
