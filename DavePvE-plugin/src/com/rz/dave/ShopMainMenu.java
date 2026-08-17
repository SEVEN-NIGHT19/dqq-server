package com.rz.dave;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ShopMainMenu implements InventoryHolder {
    public static final String TITLE = "戴夫商店";
    public static final int PAGE_PRODUCTS = 0;
    public static final int PAGE_STAR = 1;
    public static final int NAV_SLOT = 53;
    public static final int TEAM_CHEST_SLOT = 36;
    public static final int ENDER_CHEST_SLOT = 37;
    public static final int TRASH_SLOT = 38;
    public static final int STAR_STRENGTH_SLOT = 0;
    public static final int STAR_RESISTANCE_SLOT = 1;
    public static final int STAR_SLOW_SLOT = 2;
    public static final int STAR_MOVEMENT_SLOT = 3;
    public static final int STAR_DAVE_RESISTANCE_SLOT = 4;
    public static final int STAR_DAVE_REGEN_SLOT = 5;
    public static final int STAR_HASTE_SLOT = 6;

    private final Inventory inventory;
    private final int page;
    private final Map<Integer, ShopCategory> categorySlots = new HashMap<>();

    public ShopMainMenu(Player player) {
        this(player, PAGE_PRODUCTS);
    }

    public ShopMainMenu(Player player, int page) {
        this.page = page;
        this.inventory = Bukkit.createInventory(this, 54, TITLE);
        if (page == PAGE_PRODUCTS) {
            List<ShopCategory> categories = ShopCatalog.categories();
            for (int i = 0; i < categories.size(); i++) {
                ShopCategory category = categories.get(i);
                ItemStack icon = category.icon().clone();
                ItemMeta meta = icon.getItemMeta();
                List<String> lore = new ArrayList<>();
                lore.add(category.exchange()
                        ? ChatColor.GRAY + "1金=10银，1钻=10金"
                        : ChatColor.GRAY + String.valueOf(category.items().size()) + " 种商品");
                lore.add(ChatColor.YELLOW + "点击进入");
                meta.setLore(lore);
                icon.setItemMeta(meta);
                inventory.setItem(i, icon);
                categorySlots.put(i, category);
            }
            inventory.setItem(TEAM_CHEST_SLOT, actionIcon(Material.CHEST, "团队箱子", "存放团队公共物品"));
            inventory.setItem(ENDER_CHEST_SLOT, actionIcon(Material.ENDER_CHEST, "个人箱子（末影箱）", "存放个人物品"));
            inventory.setItem(TRASH_SLOT, actionIcon(Material.BARREL, "垃圾桶", "放入物品，关闭后销毁"));
            inventory.setItem(NAV_SLOT, navIcon("下一页", "星币商店"));
        } else {
            inventory.setItem(STAR_STRENGTH_SLOT, starItem(Material.IRON_SWORD, "团队力量 II", "购买后本队所有队员获得永久的 力量 II", "即使死亡也不会消失，每队限购一次"));
            inventory.setItem(STAR_RESISTANCE_SLOT, starItem(Material.SHIELD, "团队抗性 II", "购买后本队所有队员获得永久的 抗性提升 II", "即使死亡也不会消失，每队限购一次"));
            inventory.setItem(STAR_SLOW_SLOT, starItem(Material.COBWEB, "戴夫缓速光环", "本队戴夫 15 格内怪物获得 缓速 II", "死亡不影响，本局有效，每队限购一次"));
            inventory.setItem(STAR_MOVEMENT_SLOT, starItem(Material.FEATHER, "玩家移动升级", "购买后本队所有玩家与宠物获得永久的 速度 III 与 跳跃提升 IV", "即使死亡也不会消失，每队限购一次"));
            inventory.setItem(STAR_DAVE_RESISTANCE_SLOT, starItem(Material.GOLDEN_APPLE, "戴夫抗性 II（永久）", "本队戴夫获得永久的 抗性提升 II", "本局有效，每队限购一次"));
            inventory.setItem(STAR_DAVE_REGEN_SLOT, starItem(Material.APPLE, "戴夫恢复 II（永久）", "本队戴夫获得永久的 生命恢复 II", "本局有效，每队限购一次"));
            inventory.setItem(STAR_HASTE_SLOT, starItem(Material.GOLDEN_PICKAXE, "玩家攻速升级", "购买后本队所有玩家与宠物获得永久的 急迫 III", "即使死亡也不会消失，每队限购一次"));
            inventory.setItem(48, infoItem(Material.NETHER_STAR, "星币说明", "星币由小boss波怪物掉落，不与银/金/钻互兑", "目前暂无获取途径"));
            inventory.setItem(NAV_SLOT, navIcon("上一页", "商品分类"));
        }
        inventory.setItem(45, balanceIcon(ShopCurrency.SILVER, player));
        inventory.setItem(46, balanceIcon(ShopCurrency.GOLD, player));
        inventory.setItem(47, balanceIcon(ShopCurrency.DIAMOND, player));
        inventory.setItem(48, balanceIcon(ShopCurrency.STAR, player));
    }

    public int page() {
        return page;
    }

    public ShopCategory categoryForSlot(int slot) {
        return categorySlots.get(slot);
    }

    private static ItemStack navIcon(String name, String description) {
        ItemStack stack = new ItemStack(Material.ARROW);
        ItemMeta meta = stack.getItemMeta();
        meta.setItemName(name);
        meta.setLore(List.of(ChatColor.GRAY + description, ChatColor.YELLOW + "点击切换"));
        stack.setItemMeta(meta);
        return stack;
    }

    private static ItemStack balanceIcon(ShopCurrency currency, Player player) {
        ItemStack stack = new ItemStack(currency.material());
        ItemMeta meta = stack.getItemMeta();
        meta.setItemName(currency.displayName());
        meta.setLore(List.of(ChatColor.GRAY + "持有：" + DaveManager.countCurrency(player, currency)));
        stack.setItemMeta(meta);
        return stack;
    }

    private static ItemStack actionIcon(Material material, String name, String description) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        meta.setItemName(name);
        meta.setLore(List.of(ChatColor.GRAY + description, ChatColor.YELLOW + "点击打开"));
        stack.setItemMeta(meta);
        return stack;
    }

    private static ItemStack starItem(Material material, String name, String description, String extra) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        meta.setItemName(ChatColor.AQUA + name);
        meta.setLore(List.of(
                ChatColor.GOLD + "售价：1 星币",
                ChatColor.GRAY + description,
                ChatColor.GRAY + extra,
                ChatColor.YELLOW + "点击购买"));
        stack.setItemMeta(meta);
        return stack;
    }

    private static ItemStack infoItem(Material material, String name, String description, String extra) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        meta.setItemName(ChatColor.AQUA + name);
        meta.setLore(List.of(
                ChatColor.GRAY + description,
                ChatColor.GRAY + extra));
        stack.setItemMeta(meta);
        return stack;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
