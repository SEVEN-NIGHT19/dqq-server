package com.rz.dave.pvz;
import com.rz.dave.DaveManager;

import org.bukkit.Location;

/**
 * PVZ 模式的一条路（= 一条数字路，one~five）。包含怪物生成点、路的终点（基地）、
 * 玩家出生点、基地生命值与存活状态。纯状态对象，逻辑可单测。
 */
public final class PvzLane {

    private final String id;
    private final String display;
    private String world;
    private Location spawn;
    private Location base;
    private Location playerSpawn;
    private int maxHealth;
    private int baseHealth;
    private boolean eliminated;
    private int alivePlayers;
    private int spawnTicks;

    public PvzLane(String id, String display, int maxHealth) {
        this.id = id;
        this.display = display;
        this.maxHealth = maxHealth;
        this.baseHealth = maxHealth;
    }

    public String id() {
        return id;
    }

    public String display() {
        return display;
    }

    public String world() {
        return world;
    }

    public void setWorld(String world) {
        this.world = world;
    }

    public Location spawn() {
        return spawn;
    }

    public void setSpawn(Location spawn) {
        this.spawn = spawn;
    }

    public Location base() {
        return base;
    }

    public void setBase(Location base) {
        this.base = base;
    }

    public Location playerSpawn() {
        return playerSpawn;
    }

    public void setPlayerSpawn(Location playerSpawn) {
        this.playerSpawn = playerSpawn;
    }

    public int maxHealth() {
        return maxHealth;
    }

    public int baseHealth() {
        return baseHealth;
    }

    public boolean eliminated() {
        return eliminated;
    }

    public int alivePlayers() {
        return alivePlayers;
    }

    public void setAlivePlayers(int alivePlayers) {
        this.alivePlayers = Math.max(0, alivePlayers);
    }

    public int spawnTicks() {
        return spawnTicks;
    }

    public void setSpawnTicks(int spawnTicks) {
        this.spawnTicks = spawnTicks;
    }

    /** 场地坐标是否已完整配置（世界 + 生成点 + 终点 + 玩家出生点）。 */
    public boolean isConfigured() {
        return world != null && spawn != null && base != null && playerSpawn != null;
    }

    /** 该路当前是否仍在游戏（未被淘汰、基地未破、仍有存活玩家）。 */
    public boolean isActive() {
        return !eliminated && baseHealth > 0 && alivePlayers > 0;
    }

    /** 基地被怪物击中一次；返回是否因此被击破。 */
    public boolean hitBase() {
        baseHealth = Math.max(0, baseHealth - 1);
        return baseHealth == 0;
    }

    /** 标记该路淘汰。 */
    public void eliminate() {
        eliminated = true;
    }

    /** 重置为初始状态（用于下一局）。 */
    public void reset(int maxHealth) {
        this.maxHealth = maxHealth;
        this.baseHealth = maxHealth;
        this.eliminated = false;
        this.alivePlayers = 0;
        this.spawnTicks = 0;
    }
}
