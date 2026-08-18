package com.rz.dave;
import com.rz.dave.command.DaveAdminCommand;
import com.rz.dave.listener.GameListener;
import com.rz.dave.monster.MonsterManager;
import com.rz.dave.command.LobbyCommand;
import com.rz.dave.command.OpMenuCommand;
import com.rz.dave.command.PlayerMenuCommand;
import com.rz.dave.pvz.PvzListener;

import org.bukkit.plugin.java.JavaPlugin;

public final class DavePvEPlugin extends JavaPlugin {
    private MonsterManager monsterManager;
    private DaveManager manager;

    @Override
    public void onEnable() {
        monsterManager = new MonsterManager(this);
        monsterManager.enableAll();
        manager = new DaveManager(this, monsterManager);
        manager.enable();
        getServer().getPluginManager().registerEvents(new GameListener(manager), this);
        if (manager.pvzMode() != null) {
            getServer().getPluginManager().registerEvents(new PvzListener(manager.pvzMode()), this);
        }
        if (getCommand("davepve") != null) {
            DaveAdminCommand adminCommand = new DaveAdminCommand(manager);
            getCommand("davepve").setExecutor(adminCommand);
            getCommand("davepve").setTabCompleter(adminCommand);
        }
        if (getCommand("cf") != null) {
            getCommand("cf").setExecutor(new PlayerMenuCommand(manager));
        }
        if (getCommand("cv") != null) {
            getCommand("cv").setExecutor(new OpMenuCommand(manager));
        }
        if (getCommand("lb") != null) {
            getCommand("lb").setExecutor(new LobbyCommand(manager));
        }
        getLogger().info("DavePvE enabled");
    }

    @Override
    public void onDisable() {
        if (manager != null) {
            manager.disable();
            manager = null;
        }
        monsterManager.disableAll();
        monsterManager = null;
        getLogger().info("DavePvE disabled");
    }

    /** 由插件主类实例化的怪物管理器（含所有怪物单例实例）。 */
    public MonsterManager monsterManager() {
        return monsterManager;
    }
}
