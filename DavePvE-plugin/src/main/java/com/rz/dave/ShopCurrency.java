package com.rz.dave;

import org.bukkit.Material;

public enum ShopCurrency {
    SILVER("银币", Material.IRON_NUGGET),
    GOLD("金币", Material.GOLD_NUGGET),
    DIAMOND("钻币", Material.DIAMOND),
    STAR("星币", Material.NETHER_STAR);

    private final String displayName;
    private final Material material;

    ShopCurrency(String displayName, Material material) {
        this.displayName = displayName;
        this.material = material;
    }

    public String displayName() {
        return displayName;
    }

    public Material material() {
        return material;
    }
}
