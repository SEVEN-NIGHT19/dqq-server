package com.rz.dave.team;
import com.rz.dave.DaveManager;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class TeamChestManager {
    private final Plugin plugin;
    private final DaveManager manager;
    private final File chestDir;
    private final Map<String, Inventory> cached = new HashMap<>();

    public TeamChestManager(Plugin plugin, DaveManager manager) {
        this.plugin = plugin;
        this.manager = manager;
        this.chestDir = new File(plugin.getDataFolder(), "teamchests");
        chestDir.mkdirs();
    }

    public Inventory getChest(String team) {
        Inventory inventory = cached.get(team);
        if (inventory == null) {
            inventory = loadChest(team);
            cached.put(team, inventory);
        }
        return inventory;
    }

    public void saveChest(String team, Inventory inventory) {
        File file = new File(chestDir, sanitize(team) + ".yml");
        YamlConfiguration config = new YamlConfiguration();
        config.set("size", inventory.getSize());
        List<Map<String, Object>> items = new ArrayList<>();
        ItemStack[] contents = inventory.getContents();
        for (int i = 0; i < contents.length; i++) {
            if (contents[i] == null) {
                continue;
            }
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("slot", i);
            entry.put("item", contents[i]);
            items.add(entry);
        }
        config.set("items", items);
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("保存团队箱子失败: " + team + " " + e.getMessage());
        }
    }

    public void saveAll() {
        for (Map.Entry<String, Inventory> entry : cached.entrySet()) {
            saveChest(entry.getKey(), entry.getValue());
        }
    }

    public void clearAllTeamChests() {
        for (String id : DaveManager.TEAM_IDS) {
            Inventory inventory = getChest(id);
            inventory.clear();
            saveChest(id, inventory);
        }
    }

    private Inventory loadChest(String team) {
        Inventory inventory = Bukkit.createInventory(new TeamChestHolder(team), manager.teamChestSize(), "团队箱子");
        File file = new File(chestDir, sanitize(team) + ".yml");
        if (!file.exists()) {
            return inventory;
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        for (Map<?, ?> entry : config.getMapList("items")) {
            Object slotObject = entry.get("slot");
            Object itemObject = entry.get("item");
            if (slotObject instanceof Integer slot && itemObject instanceof ItemStack item) {
                inventory.setItem(slot, item);
            }
        }
        return inventory;
    }

    private static String sanitize(String team) {
        return team.replaceAll("[^A-Za-z0-9_\\-]", "_");
    }
}
