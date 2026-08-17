package com.rz.dave;
import com.rz.dave.DaveManager;
import com.rz.dave.DavePvEPlugin;

import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.bukkit.Material;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** MockBukkit 冒烟测试：验证模拟服务器与模拟玩家可用。 */
class MockBukkitSmokeTest {

    private ServerMock server;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void mockServerCanAddPlayers() {
        assertEquals(0, server.getOnlinePlayers().size());
        PlayerMock player = server.addPlayer();
        assertEquals(1, server.getOnlinePlayers().size());
        assertNotNull(player.getUniqueId());
        assertTrue(player.isOnline());
    }

    @Test
    void itemStackCanBeCreatedUnderMock() {
        org.bukkit.inventory.ItemStack stack = new org.bukkit.inventory.ItemStack(Material.IRON_SWORD);
        assertNotNull(stack);
        assertEquals(Material.IRON_SWORD, stack.getType());
    }

    /**
     * 示例：加载真实插件。DaveManager 初始化依赖较多服务器行为，
     * 脚手架阶段默认禁用；等核心逻辑拆出可测方法后再启用。
     */
    @Test
    @Disabled("DaveManager.enable() 依赖较多服务器行为，待逻辑可测化后启用")
    void pluginCanBeLoaded() {
        MockBukkit.load(DavePvEPlugin.class);
        assertTrue(server.getPluginManager().isPluginEnabled("DavePvE"));
    }
}
