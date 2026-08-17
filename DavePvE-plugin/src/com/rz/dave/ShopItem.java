package com.rz.dave;

import org.bukkit.inventory.ItemStack;

public final class ShopItem {
    public enum ShopAction {
        NONE, LEVEL, DAVE_HEAL, DAVE_RESISTANCE, WOLF, WOLF_HEALTH, WOLF_DAMAGE, WOLF_SPEED
    }

    private final String name;
    private final ItemStack product;
    private final ShopCurrency currency;
    private final int price;
    private final String description;
    private final ShopAction action;

    public ShopItem(String name, ItemStack product, ShopCurrency currency, int price, String description) {
        this(name, product, currency, price, description, ShopAction.NONE);
    }

    public ShopItem(String name, ItemStack product, ShopCurrency currency, int price, String description, ShopAction action) {
        this.name = name;
        this.product = product;
        this.currency = currency;
        this.price = price;
        this.description = description;
        this.action = action;
    }

    public String name() {
        return name;
    }

    public ItemStack product() {
        return product;
    }

    public ShopCurrency currency() {
        return currency;
    }

    public int price() {
        return price;
    }

    public String description() {
        return description;
    }

    public boolean grantsLevel() {
        return action == ShopAction.LEVEL;
    }

    public ShopAction action() {
        return action;
    }
}
