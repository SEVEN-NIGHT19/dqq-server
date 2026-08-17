package com.rz.dave;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BrewingStand;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class BrewingStandManager {
    private static final int STAND_COUNT = 30;
    private static final int COLS = 6;
    private static final int SPACING = 2;

    private final Plugin plugin;
    private final File mappingFile;
    private final YamlConfiguration mapping = new YamlConfiguration();
    private final Map<UUID, Integer> assigned = new HashMap<>();
    private String worldName;
    private int roomX;
    private int roomY;
    private int roomZ;

    public BrewingStandManager(Plugin plugin, String worldName, int roomX, int roomY, int roomZ) {
        this.plugin = plugin;
        this.worldName = worldName;
        this.roomX = roomX;
        this.roomY = roomY;
        this.roomZ = roomZ;
        this.mappingFile = new File(plugin.getDataFolder(), "brewing_stands.yml");
    }

    public void load() {
        if (mappingFile.exists()) {
            try {
                mapping.load(mappingFile);
                for (String key : mapping.getKeys(false)) {
                    try {
                        assigned.put(UUID.fromString(key), mapping.getInt(key));
                    } catch (IllegalArgumentException ignored) {
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().warning("读取炼药台映射失败: " + e.getMessage());
            }
        }
        World world = Bukkit.getWorld(worldName);
        if (world == null && !Bukkit.getWorlds().isEmpty()) {
            world = Bukkit.getWorlds().get(0);
            worldName = world.getName();
        }
        if (world != null) {
            int x0 = (roomX - 2) >> 4;
            int x1 = (roomX + 12) >> 4;
            int z0 = (roomZ - 2) >> 4;
            int z1 = (roomZ + 10) >> 4;
            for (int cx = x0; cx <= x1; cx++) {
                for (int cz = z0; cz <= z1; cz++) {
                    world.addPluginChunkTicket(cx, cz, plugin);
                }
            }
        }
    }

    public void openStand(Player player) {
        World world = Bukkit.getWorld(worldName);
        if (world == null && !Bukkit.getWorlds().isEmpty()) {
            world = Bukkit.getWorlds().get(0);
            worldName = world.getName();
        }
        if (world == null) {
            player.sendMessage(org.bukkit.ChatColor.RED + "【炼药台】炼药台暂时不可用");
            return;
        }
        int index = allocate(player);
        Location loc = standLocation(world, index);
        if (world.getBlockAt(loc).getType() != Material.BREWING_STAND) {
            buildPlatform(world);
            world.getBlockAt(loc).setType(Material.BREWING_STAND);
        }
        BrewingStand stand = (BrewingStand) world.getBlockAt(loc).getState();
        ensureFuel(world, index);
        player.openInventory(stand.getInventory());
        save();
    }

    public void refillFuel() {
        World world = Bukkit.getWorld(worldName);
        if (world == null && !Bukkit.getWorlds().isEmpty()) {
            world = Bukkit.getWorlds().get(0);
            worldName = world.getName();
        }
        if (world == null) {
            return;
        }
        for (int i = 0; i < STAND_COUNT; i++) {
            ensureFuel(world, i);
        }
    }

    private void ensureFuel(World world, int index) {
        Location loc = standLocation(world, index);
        if (world.getBlockAt(loc).getType() != Material.BREWING_STAND) {
            return;
        }
        BrewerInventory inventory = ((BrewingStand) world.getBlockAt(loc).getState()).getInventory();
        ItemStack fuel = inventory.getFuel();
        if (fuel == null || fuel.getType() != Material.BLAZE_POWDER) {
            inventory.setFuel(new ItemStack(Material.BLAZE_POWDER, 1));
        }
    }

    public boolean isOwnedStand(Location loc) {
        World world = Bukkit.getWorld(worldName);
        if (world == null && !Bukkit.getWorlds().isEmpty()) {
            world = Bukkit.getWorlds().get(0);
        }
        if (world == null || loc == null || !world.equals(loc.getWorld())) {
            return false;
        }
        int bx = loc.getBlockX();
        int by = loc.getBlockY();
        int bz = loc.getBlockZ();
        for (int i = 0; i < STAND_COUNT; i++) {
            Location stand = standLocation(world, i);
            if (stand.getBlockX() == bx && stand.getBlockY() == by && stand.getBlockZ() == bz) {
                return true;
            }
        }
        return false;
    }

    public void clearAll() {
        World world = Bukkit.getWorld(worldName);
        if (world == null && !Bukkit.getWorlds().isEmpty()) {
            world = Bukkit.getWorlds().get(0);
            worldName = world.getName();
        }
        if (world == null) {
            return;
        }
        for (int i = 0; i < STAND_COUNT; i++) {
            Location loc = standLocation(world, i);
            if (world.getBlockAt(loc).getType() == Material.BREWING_STAND) {
                BrewingStand stand = (BrewingStand) world.getBlockAt(loc).getState();
                stand.getInventory().clear();
            }
        }
    }

    private int allocate(Player player) {
        Integer existing = assigned.get(player.getUniqueId());
        if (existing != null) {
            return existing;
        }
        boolean[] used = new boolean[STAND_COUNT];
        for (int value : assigned.values()) {
            if (value >= 0 && value < STAND_COUNT) {
                used[value] = true;
            }
        }
        int index = -1;
        for (int i = 0; i < STAND_COUNT; i++) {
            if (!used[i]) {
                index = i;
                break;
            }
        }
        if (index < 0) {
            index = assigned.size() % STAND_COUNT;
        }
        assigned.put(player.getUniqueId(), index);
        mapping.set(player.getUniqueId().toString(), index);
        return index;
    }

    private Location standLocation(World world, int index) {
        int row = index / COLS;
        int col = index % COLS;
        return new Location(world, roomX + col * SPACING, roomY + 1, roomZ + row * SPACING);
    }

    private void buildPlatform(World world) {
        int x0 = roomX - 1;
        int x1 = roomX + (COLS - 1) * SPACING + 1;
        int z0 = roomZ - 1;
        int z1 = roomZ + ((STAND_COUNT + COLS - 1) / COLS - 1) * SPACING + 1;
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                world.getBlockAt(x, roomY, z).setType(Material.STONE);
                world.getBlockAt(x, roomY + 1, z).setType(Material.AIR);
                world.getBlockAt(x, roomY + 2, z).setType(Material.AIR);
            }
        }
    }

    public void save() {
        try {
            mapping.save(mappingFile);
        } catch (IOException e) {
            plugin.getLogger().warning("保存炼药台映射失败: " + e.getMessage());
        }
    }
}
