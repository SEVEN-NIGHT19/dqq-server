package com.rz.dave;
import com.rz.dave.menu.CommandsMenu;

import org.bukkit.Material;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CommandsMenuTest {

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
    void ordinaryPlayerDoesNotSeeAdminGameControls() {
        PlayerMock player = server.addPlayer("Regular");

        assertNull(new CommandsMenu(player).getInventory().getItem(4));
    }

    @Test
    void adminSeesAdminGameControls() {
        PlayerMock player = server.addPlayer("Admin");
        player.setOp(true);

        assertEquals(Material.COMMAND_BLOCK, new CommandsMenu(player).getInventory().getItem(4).getType());
    }
}
