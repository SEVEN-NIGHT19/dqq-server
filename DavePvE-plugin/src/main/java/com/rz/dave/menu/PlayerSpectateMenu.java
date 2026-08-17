package com.rz.dave.menu;
import com.rz.dave.DaveManager;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class PlayerSpectateMenu implements InventoryHolder {
    public static final String TITLE = "选择旁观目标";
    public static final int BACK_SLOT = 26;

    private final Inventory inventory;
    private final List<UUID> targets = new ArrayList<>();

    public PlayerSpectateMenu(DaveManager manager) {
        this.inventory = Bukkit.createInventory(this, 27, TITLE);
        int slot = 0;
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (slot >= BACK_SLOT) {
                break;
            }
            if (!manager.isPlaying(player)) {
                continue;
            }
            targets.add(player.getUniqueId());
            ItemStack stack = new ItemStack(Material.PLAYER_HEAD);
            ItemMeta meta = stack.getItemMeta();
            meta.setItemName(player.getName());
            meta.setLore(List.of(ChatColor.GRAY + "点击旁观该玩家"));
            stack.setItemMeta(meta);
            inventory.setItem(slot++, stack);
        }
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta backMeta = back.getItemMeta();
        backMeta.setItemName("返回");
        backMeta.setLore(List.of(ChatColor.YELLOW + "返回主菜单"));
        back.setItemMeta(backMeta);
        inventory.setItem(BACK_SLOT, back);
    }

    public Player targetForSlot(int slot) {
        if (slot < 0 || slot >= targets.size()) {
            return null;
        }
        return Bukkit.getPlayer(targets.get(slot));
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
