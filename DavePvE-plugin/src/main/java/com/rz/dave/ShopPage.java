package com.rz.dave;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public final class ShopPage implements InventoryHolder {
    public static final int BACK_SLOT = 53;
    public static final int PREV_SLOT = 52;

    private final Inventory inventory;
    private final ShopCategory category;

    private ShopPage(ShopCategory category) {
        this.category = category;
        this.inventory = Bukkit.createInventory(this, 54, category.title());
        List<ShopItem> items = category.items();
        for (int i = 0; i < items.size(); i++) {
            inventory.setItem(i, displayIcon(items.get(i)));
        }
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta backMeta = back.getItemMeta();
        backMeta.setItemName("返回主菜单");
        backMeta.setLore(List.of(ChatColor.YELLOW + "点击返回"));
        back.setItemMeta(backMeta);
        inventory.setItem(BACK_SLOT, back);
        if (category.key().startsWith("book_")) {
            ItemStack prev = new ItemStack(Material.ARROW);
            ItemMeta prevMeta = prev.getItemMeta();
            prevMeta.setItemName("返回上一页");
            prevMeta.setLore(List.of(ChatColor.YELLOW + "返回附魔商店"));
            prev.setItemMeta(prevMeta);
            inventory.setItem(PREV_SLOT, prev);
        }
    }

    public static ShopPage forCategory(ShopCategory category) {
        return new ShopPage(category);
    }

    public ShopCategory category() {
        return category;
    }

    public boolean isExchange() {
        return category.exchange();
    }

    private static ItemStack displayIcon(ShopItem shopItem) {
        ItemStack display = shopItem.product().clone();
        ItemMeta meta = display.getItemMeta();
        List<String> lore = new ArrayList<>(meta.hasLore() ? meta.getLore() : List.of());
        lore.add(ChatColor.GRAY + "价格：" + shopItem.price() + " " + shopItem.currency().displayName());
        if (shopItem.description() != null && !shopItem.description().isEmpty()) {
            lore.add(ChatColor.YELLOW + shopItem.description());
        }
        lore.add(ChatColor.GREEN + "点击购买");
        meta.setLore(lore);
        display.setItemMeta(meta);
        return display;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
