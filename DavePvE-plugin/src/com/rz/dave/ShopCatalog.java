package com.rz.dave;

import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public final class ShopCatalog {
    private ShopCatalog() {
    }

    public static List<ShopCategory> categories() {
        List<ShopCategory> list = new ArrayList<>();
        list.add(new ShopCategory("weapon", "武器", named(Material.IRON_SWORD, "武器"), weapons(), false));
        list.add(new ShopCategory("upgrade", "装备升级", named(Material.ANVIL, "装备升级"), List.of(), false));
        list.add(new ShopCategory("special_weapon", "特殊武器", named(Material.RED_WOOL, "特殊武器"), specialWeapons(), false));
        list.add(new ShopCategory("food", "食物", named(Material.COOKED_BEEF, "食物"), food(), false));
        list.add(new ShopCategory("arrow", "弹药箭矢", named(Material.ARROW, "弹药箭矢"), arrows(), false));
        list.add(new ShopCategory("potion", "药水", named(Material.POTION, "药水"), potions(), false));
        list.add(new ShopCategory("pet", "宠物", named(Material.WOLF_SPAWN_EGG, "宠物"), pets(), false));
        list.add(new ShopCategory("dave_buff", "戴夫增益", named(Material.EMERALD, "戴夫增益"), daveBuffs(), false));
        return list;
    }

    private static List<ShopItem> weapons() {
        return Arrays.asList(
                item("弓", shopBow(), ShopCurrency.GOLD, 3, "射击伤害为原版两倍"),
                item("弩", unbreakableNamed(Material.CROSSBOW, "弩"), ShopCurrency.SILVER, 25, null),
                item("三叉戟", unbreakableNamed(Material.TRIDENT, "三叉戟"), ShopCurrency.GOLD, 1, null),
                item("风暴重锤", windMace(), ShopCurrency.DIAMOND, 1, "右键释放风弹，冷却 3 秒"),
                item("仙人掌射手", cactusShooter(), ShopCurrency.GOLD, 5, "右键发射穿透箭，可升级伤害"),
                item("大喷菇", bigPuffShroom(), ShopCurrency.SILVER, 25, "右键 15×3 范围 10 伤，冷却 1.5 秒，可升级伤害"),
                item("小喷菇", smallPuffShroom(), ShopCurrency.GOLD, 1, "紫玻璃弹命中 5 伤+凋零 III，射程 15 格，可升级伤害"),
                item("胆小菇", timidShroom(), ShopCurrency.SILVER, 25, "连射冷却逐发递减，5 伤/发，可升级伤害"));
    }

    private static List<ShopItem> specialWeapons() {
        return Arrays.asList(
                item("樱桃炸弹", cherryBomb(), ShopCurrency.GOLD, 5, "右键自爆，TNT 两倍伤害，冷却 30 秒"),
                item("毁灭菇", destroyShroom(), ShopCurrency.DIAMOND, 2, "右键自爆，TNT 五倍伤害、五倍范围，冷却 2 分钟"),
                item("寒冰菇", iceShroom(), ShopCurrency.DIAMOND, 2, "右键冻结 15×15 内怪物 3 秒并缓慢，冷却 30 秒"),
                item("坚果", nut(), ShopCurrency.GOLD, 3, "右键获得 10 秒抗性 II + 缓速 III，冷却 30 秒"),
                item("高坚果", bigNut(), ShopCurrency.DIAMOND, 1, "右键获得 10 秒抗性 V + 缓速 V，并给周围玩家抗性 I"));
    }

    private static List<ShopItem> food() {
        return Arrays.asList(
                item("牛排 ×3", namedCount(Material.COOKED_BEEF, "牛排", 3), ShopCurrency.SILVER, 1, "3 个/单位"),
                item("面包 ×8", namedCount(Material.BREAD, "面包", 8), ShopCurrency.SILVER, 1, "8 个/单位"),
                item("金胡萝卜", named(Material.GOLDEN_CARROT, "金胡萝卜"), ShopCurrency.SILVER, 1, null),
                item("金苹果", named(Material.GOLDEN_APPLE, "金苹果"), ShopCurrency.GOLD, 1, null),
                item("附魔金苹果", named(Material.ENCHANTED_GOLDEN_APPLE, "附魔金苹果"), ShopCurrency.GOLD, 3, null),
                item("谜之炖菜", named(Material.SUSPICIOUS_STEW, "谜之炖菜"), ShopCurrency.SILVER, 1, null),
                item("干海带", namedCount(Material.DRIED_KELP, "干海带", 32), ShopCurrency.SILVER, 1, "32 个/单位"));
    }

    private static List<ShopItem> arrows() {
        return Arrays.asList(
                item("箭", namedCount(Material.ARROW, "箭", 10), ShopCurrency.SILVER, 5, "10 支/单位"),
                item("光灵箭", namedCount(Material.SPECTRAL_ARROW, "光灵箭", 10), ShopCurrency.GOLD, 3, "10 支/单位"),
                item("治疗之箭一", tippedArrow("治疗之箭一", 10, PotionType.HEALING), ShopCurrency.GOLD, 4, "10 支/单位"),
                item("烟花火箭（一烟火之星）", firework("烟花火箭（一烟火之星）", 10), ShopCurrency.GOLD, 7, "10 个/单位"));
    }

    private static List<ShopItem> potions() {
        return Arrays.asList(
                item("迅捷药水", potion("迅捷药水", PotionType.SWIFTNESS, null), ShopCurrency.GOLD, 2, null),
                item("跳跃药水", potion("跳跃药水", PotionType.LEAPING, null), ShopCurrency.GOLD, 1, null),
                item("治疗药水", potion("治疗药水", PotionType.HEALING, null), ShopCurrency.GOLD, 2, null),
                item("再生药水", potion("再生药水", PotionType.REGENERATION, null), ShopCurrency.SILVER, 15, null),
                item("力量药水", potion("力量药水", PotionType.STRENGTH, null), ShopCurrency.GOLD, 5, null),
                item("神龟药水", potion("神龟药水", PotionType.TURTLE_MASTER, null), ShopCurrency.GOLD, 3, null),
                item("肾上腺素（神药）", potion("肾上腺素（神药）", PotionType.WATER, List.of(
                                new PotionEffect(PotionEffectType.SPEED, 600, 6, false, true, true),
                                new PotionEffect(PotionEffectType.STRENGTH, 600, 1, false, true, true),
                                new PotionEffect(PotionEffectType.JUMP_BOOST, 600, 4, false, true, true),
                                new PotionEffect(PotionEffectType.ABSORPTION, 600, 9, false, true, true))),
                        ShopCurrency.DIAMOND, 1, "速度7 力量2 跳跃提升5 两排金心 半分钟"));
    }

    private static List<ShopItem> pets() {
        return Arrays.asList(
                item("狼", named(Material.WOLF_SPAWN_EGG, "狼"), ShopCurrency.GOLD, 5, "购买后驯服归属自己，每人限 1 只",
                        ShopItem.ShopAction.WOLF),
                item("狼生命 +2", named(Material.BONE, "狼生命 +2"), ShopCurrency.GOLD, 1, "为你的狼增加 2 点血量上限，不限次数",
                        ShopItem.ShopAction.WOLF_HEALTH),
                item("狼伤害 +1", named(Material.IRON_SWORD, "狼伤害 +1"), ShopCurrency.GOLD, 5, "为你的狼增加 1 点攻击伤害，不限次数",
                        ShopItem.ShopAction.WOLF_DAMAGE),
                item("狼移速 +0.1 倍", named(Material.SUGAR, "狼移速 +0.1 倍"), ShopCurrency.GOLD, 3, "为你的狼提升 0.1 倍移动速度，最多 10 次",
                        ShopItem.ShopAction.WOLF_SPEED));
    }

    private static List<ShopItem> daveBuffs() {
        return Arrays.asList(
                item("戴夫恢复", named(Material.GOLDEN_APPLE, "戴夫恢复"), ShopCurrency.GOLD, 1, "为本队戴夫恢复 10 点生命（满血不扣钱）",
                        ShopItem.ShopAction.DAVE_HEAL),
                item("戴夫抗性", named(Material.TOTEM_OF_UNDYING, "戴夫抗性"), ShopCurrency.GOLD, 3, "为本队戴夫提供 60 秒抗性提升 I",
                        ShopItem.ShopAction.DAVE_RESISTANCE));
    }

    private static List<ShopItem> exchange() {
        return Arrays.asList(
                item("10 银币 → 1 金币", namedCount(ShopCurrency.GOLD.material(), "金币", 1),
                        ShopCurrency.SILVER, 10, "1金=10银"),
                item("1 金币 → 10 银币", namedCount(ShopCurrency.SILVER.material(), "银币", 10),
                        ShopCurrency.GOLD, 1, "1金=10银"),
                item("10 金币 → 1 钻币", namedCount(ShopCurrency.DIAMOND.material(), "钻币", 1),
                        ShopCurrency.GOLD, 10, "1钻=10金"),
                item("1 钻币 → 10 金币", namedCount(ShopCurrency.GOLD.material(), "金币", 10),
                        ShopCurrency.DIAMOND, 1, "1钻=10金"));
    }

    private static ShopItem item(String name, ItemStack product, ShopCurrency currency, int price, String description) {
        return new ShopItem(name, product, currency, price, description);
    }

    private static ShopItem item(String name, ItemStack product, ShopCurrency currency, int price, String description, ShopItem.ShopAction action) {
        return new ShopItem(name, product, currency, price, description, action);
    }

    private static ItemStack shopBow() {
        ItemStack stack = named(Material.BOW, "弓");
        ItemMeta meta = stack.getItemMeta();
        meta.setUnbreakable(true);
        meta.getPersistentDataContainer().set(DaveManager.shopBowKey(), PersistentDataType.BYTE, (byte) 1);
        stack.setItemMeta(meta);
        return stack;
    }

    private static ItemStack cherryBomb() {
        ItemStack stack = new ItemStack(Material.RED_WOOL);
        ItemMeta meta = stack.getItemMeta();
        meta.setUnbreakable(true);
        meta.setItemName("樱桃炸弹");
        meta.getPersistentDataContainer().set(DaveManager.cherryBombKey(), PersistentDataType.BYTE, (byte) 1);
        stack.setItemMeta(meta);
        return stack;
    }

    private static ItemStack destroyShroom() {
        ItemStack stack = new ItemStack(Material.BLACK_WOOL);
        ItemMeta meta = stack.getItemMeta();
        meta.setUnbreakable(true);
        meta.setItemName("毁灭菇");
        meta.getPersistentDataContainer().set(DaveManager.destroyShroomKey(), PersistentDataType.BYTE, (byte) 1);
        stack.setItemMeta(meta);
        return stack;
    }

    private static ItemStack nut() {
        ItemStack stack = new ItemStack(Material.BROWN_CARPET);
        ItemMeta meta = stack.getItemMeta();
        meta.setUnbreakable(true);
        meta.setItemName("坚果");
        meta.getPersistentDataContainer().set(DaveManager.nutKey(), PersistentDataType.BYTE, (byte) 1);
        stack.setItemMeta(meta);
        return stack;
    }

    private static ItemStack bigNut() {
        ItemStack stack = new ItemStack(Material.BROWN_WOOL);
        ItemMeta meta = stack.getItemMeta();
        meta.setUnbreakable(true);
        meta.setItemName("高坚果");
        meta.getPersistentDataContainer().set(DaveManager.bigNutKey(), PersistentDataType.BYTE, (byte) 1);
        stack.setItemMeta(meta);
        return stack;
    }

    private static ItemStack cactusShooter() {
        ItemStack stack = new ItemStack(Material.CACTUS);
        ItemMeta meta = stack.getItemMeta();
        meta.setUnbreakable(true);
        meta.setItemName("仙人掌射手");
        meta.setLore(List.of(
                ChatColor.GRAY + "右键发射穿透箭，命中怪物造成 10 点伤害",
                ChatColor.GRAY + "可花钻币升级伤害，冷却 1 秒"));
        meta.getPersistentDataContainer().set(DaveManager.cactusShooterKey(), PersistentDataType.BYTE, (byte) 1);
        meta.getPersistentDataContainer().set(DaveManager.cactusDamageKey(), PersistentDataType.INTEGER, 0);
        stack.setItemMeta(meta);
        return stack;
    }

    private static ItemStack iceShroom() {
        ItemStack stack = new ItemStack(Material.BLUE_ICE);
        ItemMeta meta = stack.getItemMeta();
        meta.setUnbreakable(true);
        meta.setItemName("寒冰菇");
        meta.setLore(List.of(
                ChatColor.GRAY + "右键释放：15×15 范围敌方生物 1 伤 + 缓慢 10 秒 + 冻结 AI 3 秒",
                ChatColor.GRAY + "冷却 30 秒"));
        meta.getPersistentDataContainer().set(DaveManager.iceShroomKey(), PersistentDataType.BYTE, (byte) 1);
        stack.setItemMeta(meta);
        return stack;
    }

    private static ItemStack bigPuffShroom() {
        ItemStack stack = new ItemStack(Material.AMETHYST_CLUSTER);
        ItemMeta meta = stack.getItemMeta();
        meta.setUnbreakable(true);
        meta.setItemName("大喷菇");
        meta.setLore(List.of(
                ChatColor.GRAY + "右键朝视线 15×3 区域喷射，命中怪物 10 点伤害",
                ChatColor.GRAY + "冷却 1.5 秒，可花钻币升级伤害"));
        meta.getPersistentDataContainer().set(DaveManager.bigPuffKey(), PersistentDataType.BYTE, (byte) 1);
        meta.getPersistentDataContainer().set(DaveManager.bigPuffDamageKey(), PersistentDataType.INTEGER, 0);
        stack.setItemMeta(meta);
        return stack;
    }

    private static ItemStack smallPuffShroom() {
        ItemStack stack = new ItemStack(Material.BROWN_MUSHROOM);
        ItemMeta meta = stack.getItemMeta();
        meta.setUnbreakable(true);
        meta.setItemName("小喷菇");
        meta.setLore(List.of(
                ChatColor.GRAY + "右键发射紫玻璃弹，命中首怪 5 伤 + 凋零 III 5 秒",
                ChatColor.GRAY + "射程 15 格，冷却 1.5 秒，可花钻币升级伤害"));
        meta.getPersistentDataContainer().set(DaveManager.smallPuffKey(), PersistentDataType.BYTE, (byte) 1);
        meta.getPersistentDataContainer().set(DaveManager.smallPuffDamageKey(), PersistentDataType.INTEGER, 0);
        stack.setItemMeta(meta);
        return stack;
    }

    private static ItemStack timidShroom() {
        ItemStack stack = new ItemStack(Material.AMETHYST_SHARD);
        ItemMeta meta = stack.getItemMeta();
        meta.setUnbreakable(true);
        meta.setItemName("胆小菇");
        meta.setLore(List.of(
                ChatColor.GRAY + "右键发射豌豆弹，连射冷却逐发递减（1.5→0.1 秒）",
                ChatColor.GRAY + "每发 5 伤，可花钻币升级伤害"));
        meta.getPersistentDataContainer().set(DaveManager.timidKey(), PersistentDataType.BYTE, (byte) 1);
        meta.getPersistentDataContainer().set(DaveManager.timidDamageKey(), PersistentDataType.INTEGER, 0);
        stack.setItemMeta(meta);
        return stack;
    }

    private static ItemStack windMace() {
        ItemStack stack = named(Material.MACE, "风暴重锤");
        ItemMeta meta = stack.getItemMeta();
        meta.setUnbreakable(true);
        meta.setLore(List.of(
                ChatColor.GRAY + "使用说明：",
                ChatColor.WHITE + "主手持握时右键释放一枚风弹",
                ChatColor.WHITE + "风弹沿视线方向飞出，命中产生风爆",
                ChatColor.YELLOW + "冷却时间：3 秒"));
        meta.getPersistentDataContainer().set(DaveManager.windMaceKey(), PersistentDataType.BYTE, (byte) 1);
        stack.setItemMeta(meta);
        return stack;
    }

    private static ItemStack unbreakableNamed(Material material, String name) {
        ItemStack stack = named(material, name);
        ItemMeta meta = stack.getItemMeta();
        meta.setUnbreakable(true);
        stack.setItemMeta(meta);
        return stack;
    }

    private static ItemStack named(Material material, String name) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        meta.setItemName(name);
        stack.setItemMeta(meta);
        return stack;
    }

    private static ItemStack namedCount(Material material, String name, int amount) {
        ItemStack stack = named(material, name);
        stack.setAmount(amount);
        return stack;
    }

    private static ItemStack potion(String name, PotionType baseType, List<PotionEffect> customEffects) {
        ItemStack stack = new ItemStack(Material.POTION);
        PotionMeta meta = (PotionMeta) stack.getItemMeta();
        meta.setItemName(name);
        meta.setBasePotionType(baseType);
        if (customEffects != null) {
            for (PotionEffect effect : customEffects) {
                meta.addCustomEffect(effect, true);
            }
        }
        stack.setItemMeta(meta);
        return stack;
    }

    private static ItemStack tippedArrow(String name, int amount, PotionType baseType) {
        ItemStack stack = new ItemStack(Material.TIPPED_ARROW, amount);
        PotionMeta meta = (PotionMeta) stack.getItemMeta();
        meta.setItemName(name);
        meta.setBasePotionType(baseType);
        stack.setItemMeta(meta);
        return stack;
    }

    private static ItemStack firework(String name, int amount) {
        ItemStack stack = new ItemStack(Material.FIREWORK_ROCKET, amount);
        FireworkMeta meta = (FireworkMeta) stack.getItemMeta();
        meta.setItemName(name);
        meta.addEffect(org.bukkit.FireworkEffect.builder()
                .withColor(Color.WHITE)
                .with(org.bukkit.FireworkEffect.Type.BURST)
                .build());
        meta.setPower(1);
        stack.setItemMeta(meta);
        return stack;
    }
}
