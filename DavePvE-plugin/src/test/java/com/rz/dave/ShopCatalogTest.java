package com.rz.dave;
import com.rz.dave.shop.ShopCatalog;
import com.rz.dave.shop.ShopItem;
import com.rz.dave.shop.ShopCategory;
import com.rz.dave.shop.ShopCurrency;

import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 商店目录测试：使用 MockBukkit 提供 Bukkit 物品工厂。 */
class ShopCatalogTest {

    private ServerMock server;

    // Submenu categories (e.g. "upgrade") open a dedicated menu and have no direct items.
    private static final Set<String> SUBMENU_CATEGORIES = Set.of("upgrade");

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void categoriesContainEightKnownSections() {
        List<ShopCategory> categories = ShopCatalog.categories();
        assertEquals(8, categories.size());
        List<String> keys = categories.stream().map(ShopCategory::key).toList();
        assertTrue(keys.containsAll(List.of(
                "weapon", "upgrade", "special_weapon", "food", "arrow", "potion", "pet", "dave_buff")));
    }

    @Test
    void everyCategoryHasTitleIconAndItems() {
        for (ShopCategory category : ShopCatalog.categories()) {
            assertNotNull(category.title());
            assertFalse(category.title().isBlank(), category.key() + " 缺少标题");
            assertNotNull(category.icon(), category.key() + " 缺少图标");
            if (!SUBMENU_CATEGORIES.contains(category.key())) {
                assertFalse(category.items().isEmpty(), category.key() + " 商品列表为空");
            }
        }
    }

    @Test
    void everyShopItemHasProductCurrencyAndPositivePrice() {
        for (ShopCategory category : ShopCatalog.categories()) {
            for (ShopItem item : category.items()) {
                String where = category.key() + "/" + item.name();
                assertNotNull(item.product(), where + " 缺少商品");
                assertNotNull(item.currency(), where + " 缺少货币");
                assertTrue(item.price() > 0, where + " 价格必须为正数");
            }
        }
    }

    @Test
    void serverIsMocked() {
        assertEquals(0, server.getOnlinePlayers().size());
    }
}
