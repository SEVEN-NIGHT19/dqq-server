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

public final class OpCategoryMenu implements InventoryHolder {
    public static final int CATEGORY_GAME = 0;
    public static final int CATEGORY_TEAM = 1;
    public static final int CATEGORY_SPAWN = 2;
    public static final int CATEGORY_BOSS = 3;
    public static final int BACK_SLOT = 26;
    public static final int BIND_SLOT = 3;
    public static final int KILL_SLOT = 4;
    public static final int WAVE_SLOT = 3;
    public static final int PVP_SLOT = 13;

    private final Inventory inventory;
    private final int category;

    public OpCategoryMenu(int category, DaveManager manager) {
        this.category = category;
        String title = switch (category) {
            case CATEGORY_GAME -> "游戏控制";
            case CATEGORY_TEAM -> "队伍与戴夫";
            case CATEGORY_SPAWN -> "刷怪点";
            case CATEGORY_BOSS -> "boss点";
            default -> "管理分类";
        };
        this.inventory = Bukkit.createInventory(this, 27, title);
        switch (category) {
            case CATEGORY_GAME -> {
                inventory.setItem(0, commandItem(Material.ENDER_EYE, "开始游戏", "/davepve start"));
                inventory.setItem(1, commandItem(Material.END_CRYSTAL, "结束游戏", "/davepve end"));
                inventory.setItem(2, commandItem(Material.PAPER, "状态信息", "/davepve info"));
                inventory.setItem(WAVE_SLOT, commandItem(Material.CLOCK, "调整波次", "/davepve wave"));
                inventory.setItem(PVP_SLOT, pvpItem(manager.isPvpEnabled()));
            }
            case CATEGORY_TEAM -> {
                inventory.setItem(0, commandItem(Material.ENDER_PEARL, "自动分队", "/davepve balance"));
                inventory.setItem(BIND_SLOT, commandItem(Material.NAME_TAG, "绑定戴夫到队伍", "/davepve bind <队伍>"));
                inventory.setItem(KILL_SLOT, commandItem(Material.WITHER_SKELETON_SKULL, "击杀某队戴夫", "/davepve kill <队伍>"));
                inventory.setItem(5, commandItem(Material.IRON_SWORD, "击杀最近戴夫", "/davepve killnearest"));
                inventory.setItem(6, commandItem(Material.VILLAGER_SPAWN_EGG, "生成戴夫", "/trigger rz.dave.create"));
            }
            case CATEGORY_SPAWN -> {
                inventory.setItem(0, commandItem(Material.ARMOR_STAND, "创建刷怪点", "/trigger rz.sp.create"));
                inventory.setItem(1, commandItem(Material.STICK, "删除刷怪点", "/trigger rz.sp.delete"));
                inventory.setItem(2, commandItem(Material.COMPASS, "查看刷怪点", "/trigger rz.sp.list"));
                inventory.setItem(3, commandItem(Material.CLOCK, "开始刷怪", "/trigger rz.sp.start"));
                inventory.setItem(4, commandItem(Material.BARRIER, "停止刷怪", "/trigger rz.sp.stop"));
            }
            case CATEGORY_BOSS -> {
                inventory.setItem(0, commandItem(Material.ZOMBIE_HEAD, "创建boss点", "/trigger rz.sp.boss.create"));
                inventory.setItem(1, commandItem(Material.SKELETON_SKULL, "删除boss点", "/trigger rz.sp.boss.delete"));
                inventory.setItem(2, commandItem(Material.COMPASS, "查看boss点", "/trigger rz.sp.boss.list"));
            }
            default -> { }
        }
        inventory.setItem(BACK_SLOT, backButton());
    }

    public int category() {
        return category;
    }

    public static String commandForSlot(int category, int slot) {
        return switch (category) {
            case CATEGORY_GAME -> switch (slot) {
                case 0 -> "davepve start";
                case 1 -> "davepve end";
                case 2 -> "davepve info";
                default -> null;
            };
            case CATEGORY_TEAM -> switch (slot) {
                case 0 -> "davepve balance";
                case 5 -> "davepve killnearest";
                case 6 -> "trigger rz.dave.create";
                default -> null;
            };
            case CATEGORY_SPAWN -> switch (slot) {
                case 0 -> "trigger rz.sp.create";
                case 1 -> "trigger rz.sp.delete";
                case 2 -> "trigger rz.sp.list";
                case 3 -> "trigger rz.sp.start";
                case 4 -> "trigger rz.sp.stop";
                default -> null;
            };
            case CATEGORY_BOSS -> switch (slot) {
                case 0 -> "trigger rz.sp.boss.create";
                case 1 -> "trigger rz.sp.boss.delete";
                case 2 -> "trigger rz.sp.boss.list";
                default -> null;
            };
            default -> null;
        };
    }

    private static ItemStack commandItem(Material material, String name, String command) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        meta.setItemName(name);
        meta.setLore(List.of(ChatColor.GRAY + command, ChatColor.YELLOW + "左键执行"));
        stack.setItemMeta(meta);
        return stack;
    }

    private static ItemStack pvpItem(boolean enabled) {
        ItemStack stack = new ItemStack(enabled ? Material.IRON_SWORD : Material.LEATHER_CHESTPLATE);
        ItemMeta meta = stack.getItemMeta();
        meta.setItemName(enabled ? "PVP：允许" : "PVP：禁止");
        meta.setLore(List.of(ChatColor.GRAY + "左键切换玩家互伤", ChatColor.YELLOW + "当前：" + (enabled ? "允许" : "禁止")));
        stack.setItemMeta(meta);
        return stack;
    }

    private static ItemStack backButton() {
        ItemStack stack = new ItemStack(Material.ARROW);
        ItemMeta meta = stack.getItemMeta();
        meta.setItemName("返回");
        meta.setLore(List.of(ChatColor.YELLOW + "返回管理菜单"));
        stack.setItemMeta(meta);
        return stack;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
