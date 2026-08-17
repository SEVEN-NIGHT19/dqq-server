package com.rz.dave;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 植物大战僵尸模式（PVZ）核心状态机。
 * 与经典 Dave PvE 大模式完全独立：独立场地（config 配置）、独立职业、独立怪物
 * （pvz_ 前缀标签，经典模式的 isMonster/isDave 不会触碰）、独立胜负流程。
 *
 * 规则（按用户需求 + 已确认的缺陷补全）：
 * - 每队一条路；玩家开局随机职业（剑士/弓箭手，可重复），死亡转观察者不复活；
 * - 每队基地生命（默认 10），怪物走到路终点扣 1 点，归零该队淘汰；
 * - 队伍全灭（全部玩家死亡）即淘汰；坚持到其他所有队伍失败即获胜；
 * - 开局只有一队有玩家 → 正常开始，队伍全灭才结束；
 * - 主路怪物只做「盲盒僵尸」，死亡后随机召唤原版僵尸（不掉落、不燃）或原版苦力怕（不掉落）；
 * - 波次无限，难度随时间递增；与经典模式可同时运行。
 */
public final class PvzMode {

    public static final String TAG_MONSTER = "pvz_monster";
    public static final String TAG_BLINDBOX = "pvz_blindbox";
    public static final String TAG_SUMMON = "pvz_summon";

    private static final int ARRIVAL_DISTANCE_SQ = 3; // ~1.7 格水平距离
    private static final double TARGET_RANGE = 8.0;
    private static final int SPAWN_TIMER_STEP_TICKS = 20; // tick() 每秒执行一次

    private final Plugin plugin;
    private final DaveManager manager;
    private final NamespacedKey laneKey;

    // config 数值
    private int baseHealth = 10;
    private int waveDurationTicks = 600;          // 每 30 秒升一波
    private int spawnIntervalTicksBase = 160;     // 基础刷怪间隔（每路每只）
    private int spawnIntervalTicksStep = 10;      // 每波缩短
    private int spawnsPerWaveBase = 3;            // 第 1 波每路总量
    private int spawnsPerWaveGrowth = 1;          // 每波 +1
    private double monsterHealthBase = 20.0;
    private double monsterHealthGrowth = 2.0;
    private double monsterAttackMultiplier = 0.5;

    private final Map<String, PvzLane> lanes = new LinkedHashMap<>();
    private final Set<UUID> pvzReady = new java.util.LinkedHashSet<>();
    private final Set<UUID> pvzPlayers = new java.util.LinkedHashSet<>();
    private final Map<UUID, PvzClass> playerClass = new HashMap<>();
    private final Map<UUID, String> playerLane = new HashMap<>();
    private final Map<String, BossBar> laneBars = new LinkedHashMap<>();

    private boolean running;
    private int waveIndex;
    private long waveStartedTick;
    private BukkitTask tickTask;
    private Scoreboard pvzBoard;
    private Objective sidebar;
    private String winnerLane;

    public PvzMode(Plugin plugin, DaveManager manager) {
        this.plugin = plugin;
        this.manager = manager;
        this.laneKey = new NamespacedKey(plugin, "pvz_lane");
        for (String id : DaveManager.TEAM_IDS) {
            lanes.put(id, new PvzLane(id, DaveManager.TEAM_DISPLAYS.getOrDefault(id, id), baseHealth));
        }
    }

    // ---------------------------------------------------------------- 生命周期

    public void enable() {
        loadConfig();
    }

    public void disable() {
        if (running) {
            endGameInternal(null, false);
        }
    }

    public boolean isRunning() {
        return running;
    }

    /** 上一局获胜队伍 id（无胜者为 null）。 */
    public String lastWinner() {
        return winnerLane;
    }

    public boolean isPlaying(Player player) {
        return player != null && pvzPlayers.contains(player.getUniqueId());
    }

    public boolean isPlaying(UUID uuid) {
        return uuid != null && pvzPlayers.contains(uuid);
    }

    public PvzClass classOf(Player player) {
        return playerClass.get(player.getUniqueId());
    }

    public String laneOf(Player player) {
        return playerLane.get(player.getUniqueId());
    }

    public boolean isReady(Player player) {
        return pvzReady.contains(player.getUniqueId());
    }

    public int readyCount() {
        int count = 0;
        for (UUID id : pvzReady) {
            Player p = Bukkit.getPlayer(id);
            if (p != null && p.isOnline()) {
                count++;
            }
        }
        return count;
    }

    /** PVZ 准备/取消准备。PVZ 准备会同时把玩家从经典模式准备中移除（模式互斥于同一玩家）。 */
    public boolean setReady(Player player, boolean ready) {
        boolean changed = ready ? pvzReady.add(player.getUniqueId()) : pvzReady.remove(player.getUniqueId());
        if (ready) {
            manager.markUnready(player);
        }
        return changed;
    }

    // ---------------------------------------------------------------- 配置

    private void loadConfig() {
        FileConfiguration cfg = plugin.getConfig();
        baseHealth = cfg.getInt("pvz.base-health", 10);
        waveDurationTicks = cfg.getInt("pvz.wave-duration-seconds", 30) * 20;
        spawnIntervalTicksBase = cfg.getInt("pvz.spawn-interval-ticks-base", 160);
        spawnIntervalTicksStep = cfg.getInt("pvz.spawn-interval-ticks-step", 10);
        spawnsPerWaveBase = cfg.getInt("pvz.spawns-per-wave-base", 3);
        spawnsPerWaveGrowth = cfg.getInt("pvz.spawns-per-wave-growth", 1);
        monsterHealthBase = cfg.getDouble("pvz.monster-health-base", 20.0);
        monsterHealthGrowth = cfg.getDouble("pvz.monster-health-growth", 2.0);
        monsterAttackMultiplier = cfg.getDouble("pvz.monster-attack-multiplier", 0.5);
        ConfigurationSection sec = cfg.getConfigurationSection("pvz.lanes");
        if (sec != null) {
            for (String id : lanes.keySet()) {
                ConfigurationSection laneSec = sec.getConfigurationSection(id);
                if (laneSec == null) {
                    continue;
                }
                PvzLane lane = lanes.get(id);
                lane.setWorld(laneSec.getString("world"));
                lane.setSpawn(readLoc(laneSec, "spawn"));
                lane.setBase(readLoc(laneSec, "base"));
                lane.setPlayerSpawn(readLoc(laneSec, "player"));
            }
        }
    }

    private static Location readLoc(ConfigurationSection section, String key) {
        ConfigurationSection sub = section.getConfigurationSection(key);
        if (sub == null) {
            return null;
        }
        String worldName = section.getString("world");
        World world = worldName == null ? null : Bukkit.getWorld(worldName);
        return new Location(world, sub.getDouble("x", 0), sub.getDouble("y", 0), sub.getDouble("z", 0));
    }

    private void saveLaneConfig(String team) {
        PvzLane lane = lanes.get(team);
        if (lane == null) {
            return;
        }
        FileConfiguration cfg = plugin.getConfig();
        String path = "pvz.lanes." + team;
        cfg.set(path + ".world", lane.world());
        writeLoc(cfg, path + ".spawn", lane.spawn());
        writeLoc(cfg, path + ".base", lane.base());
        writeLoc(cfg, path + ".player", lane.playerSpawn());
        plugin.saveConfig();
    }

    private static void writeLoc(FileConfiguration cfg, String path, Location loc) {
        if (loc == null) {
            cfg.set(path, null);
            return;
        }
        cfg.set(path + ".x", loc.getX());
        cfg.set(path + ".y", loc.getY());
        cfg.set(path + ".z", loc.getZ());
    }

    // ---------------------------------------------------------------- 场地设置（管理员现场圈定）

    public String setLaneSpawn(String team, Player admin) {
        return setLaneSpawn(team, admin.getWorld().getName(), admin.getLocation().getX(),
                admin.getLocation().getY(), admin.getLocation().getZ());
    }

    public String setLaneSpawn(String team, String worldName, double x, double y, double z) {
        PvzLane lane = lanes.get(team);
        if (lane == null) {
            return ChatColor.RED + "未知队伍: " + team;
        }
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return ChatColor.RED + "世界不存在: " + worldName;
        }
        lane.setSpawn(new Location(world, x, y, z));
        lane.setWorld(worldName);
        saveLaneConfig(team);
        return ChatColor.GREEN + "已设置 " + lane.display() + " 路怪物生成点";
    }

    public String setLaneBase(String team, Player admin) {
        return setLaneBase(team, admin.getWorld().getName(), admin.getLocation().getX(),
                admin.getLocation().getY(), admin.getLocation().getZ());
    }

    public String setLaneBase(String team, String worldName, double x, double y, double z) {
        PvzLane lane = lanes.get(team);
        if (lane == null) {
            return ChatColor.RED + "未知队伍: " + team;
        }
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return ChatColor.RED + "世界不存在: " + worldName;
        }
        lane.setBase(new Location(world, x, y, z));
        lane.setWorld(worldName);
        saveLaneConfig(team);
        return ChatColor.GREEN + "已设置 " + lane.display() + " 路终点（基地）";
    }

    public String setLanePlayerSpawn(String team, Player admin) {
        return setLanePlayerSpawn(team, admin.getWorld().getName(), admin.getLocation().getX(),
                admin.getLocation().getY(), admin.getLocation().getZ());
    }

    public String setLanePlayerSpawn(String team, String worldName, double x, double y, double z) {
        PvzLane lane = lanes.get(team);
        if (lane == null) {
            return ChatColor.RED + "未知队伍: " + team;
        }
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return ChatColor.RED + "世界不存在: " + worldName;
        }
        lane.setPlayerSpawn(new Location(world, x, y, z));
        lane.setWorld(worldName);
        saveLaneConfig(team);
        return ChatColor.GREEN + "已设置 " + lane.display() + " 路玩家出生点";
    }

    // ---------------------------------------------------------------- 波次数值（纯逻辑，可单测）

    public static int spawnsPerWave(int wave) {
        return 3 + wave * 1;
    }

    public static double monsterHealth(int wave) {
        return 20.0 + wave * 2.0;
    }

    public static int spawnIntervalTicks(int wave) {
        return Math.max(20, 160 - wave * 10);
    }

    public static int spawnTimerStepTicks() {
        return SPAWN_TIMER_STEP_TICKS;
    }

    public static ItemStack blindBoxHelmet() {
        return new ItemStack(org.bukkit.Material.HAY_BLOCK);
    }

    // ---------------------------------------------------------------- 开局 / 结束

    public void startGame(CommandSender requester) {
        if (running) {
            requester.sendMessage(ChatColor.GOLD + "【PVZ】PVZ 模式正在进行中");
            return;
        }
        List<Player> ready = new ArrayList<>();
        for (UUID id : pvzReady) {
            Player p = Bukkit.getPlayer(id);
            if (p != null && p.isOnline()) {
                ready.add(p);
            }
        }
        if (ready.isEmpty()) {
            requester.sendMessage(ChatColor.RED + "【PVZ】没有玩家准备 PVZ 模式");
            return;
        }
        // 校验场地
        for (PvzLane lane : lanes.values()) {
            if (!lane.isConfigured()) {
                requester.sendMessage(ChatColor.RED + "【PVZ】" + lane.display() + " 路场地未配置，请先使用 "
                        + ChatColor.YELLOW + "/davepve pvz setspawn|setbase|setplayer " + lane.id());
                return;
            }
            World world = Bukkit.getWorld(lane.world());
            if (world == null) {
                requester.sendMessage(ChatColor.RED + "【PVZ】" + lane.display() + " 路世界不存在: " + lane.world());
                return;
            }
        }
        // 按路分组
        Map<String, List<Player>> byLane = new LinkedHashMap<>();
        for (String id : lanes.keySet()) {
            byLane.put(id, new ArrayList<>());
        }
        for (Player p : ready) {
            String laneId = pickLaneFor(p, byLane);
            byLane.get(laneId).add(p);
        }
        // 准备就绪：发职业、传送、入场
        waveIndex = 0;
        waveStartedTick = Bukkit.getCurrentTick();
        for (Map.Entry<String, List<Player>> e : byLane.entrySet()) {
            PvzLane lane = lanes.get(e.getKey());
            lane.reset(baseHealth);
            lane.setAlivePlayers(e.getValue().size());
            for (Player p : e.getValue()) {
                PvzClass clazz = PvzClass.random();
                playerClass.put(p.getUniqueId(), clazz);
                playerLane.put(p.getUniqueId(), lane.id());
                pvzPlayers.add(p.getUniqueId());
                manager.setPvzPlayer(p.getUniqueId(), true);
                p.getInventory().clear();
                p.getEnderChest().clear();
                for (ItemStack item : clazz.createKit()) {
                    p.getInventory().addItem(item);
                }
                p.setGameMode(GameMode.ADVENTURE);
                p.setHealth(p.getMaxHealth());
                p.setFoodLevel(20);
                p.setSaturation(20);
                p.clearActivePotionEffects();
                World world = Bukkit.getWorld(lane.world());
                Location spawn = lane.playerSpawn();
                p.teleport(new Location(world, spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5));
                p.sendTitle(ChatColor.GOLD + "PVZ 模式开始", "你的职业：" + clazz.displayName(), 10, 60, 10);
                refreshListName(p);
            }
        }
        for (Player p : ready) {
            manager.markUnready(p);
        }
        running = true;
        winnerLane = null;
        pvzReady.clear();
        buildSidebar();
        buildBars();
        tickTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
        Bukkit.broadcastMessage(ChatColor.GOLD + "【PVZ】植物大战僵尸模式开始！"
                + ChatColor.GRAY + "守住你的路，坚持到其他队伍全部失败！");
        Bukkit.broadcastMessage(ChatColor.GRAY + "【PVZ】第 1 波即将来袭；死亡不会复活，基地生命 "
                + baseHealth + " 点。");
    }

    /** 为玩家选择路：优先已选队伍，其次当前队伍，最后人数最少的路。 */
    private String pickLaneFor(Player player, Map<String, List<Player>> byLane) {
        String pref = manager.teamPreference(player);
        if (pref != null && lanes.containsKey(pref)) {
            return pref;
        }
        String current = manager.getTeamName(player);
        if (current != null && lanes.containsKey(current)) {
            return current;
        }
        String best = null;
        int min = Integer.MAX_VALUE;
        for (Map.Entry<String, List<Player>> e : byLane.entrySet()) {
            if (e.getValue().size() < min) {
                min = e.getValue().size();
                best = e.getKey();
            }
        }
        return best;
    }

    public void stopGame(CommandSender requester) {
        if (!running) {
            requester.sendMessage(ChatColor.RED + "【PVZ】PVZ 模式未在进行中");
            return;
        }
        Bukkit.broadcastMessage(ChatColor.GOLD + "【PVZ】管理员已结束 PVZ 模式");
        endGameInternal(null, true);
    }

    public String statusSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append(ChatColor.GOLD).append("【PVZ】状态：");
        if (running) {
            sb.append(ChatColor.GREEN).append("进行中 第 ").append(waveIndex + 1).append(" 波");
        } else {
            sb.append(ChatColor.GRAY).append("未开始（准备 ").append(readyCount()).append(" 人）");
        }
        for (PvzLane lane : lanes.values()) {
            sb.append('\n').append(ChatColor.GRAY).append(lane.display()).append("：");
            if (!lane.isConfigured()) {
                sb.append(ChatColor.RED).append("未配置场地");
            } else if (lane.eliminated()) {
                sb.append(ChatColor.RED).append("已淘汰");
            } else {
                sb.append(ChatColor.GREEN).append("基地 ").append(lane.baseHealth()).append('/')
                        .append(lane.maxHealth()).append("，存活玩家 ").append(lane.alivePlayers());
            }
        }
        return sb.toString();
    }

    private void endGameInternal(String winner, boolean adminStop) {
        if (tickTask != null) {
            tickTask.cancel();
            tickTask = null;
        }
        running = false;
        winnerLane = winner;
        removeAllPvzMobs();
        removeBars();
        clearSidebar();
        for (UUID id : new ArrayList<>(pvzPlayers)) {
            Player p = Bukkit.getPlayer(id);
            if (p != null && p.isOnline()) {
                returnPlayerToLobby(p, false);
            }
        }
        if (winner != null) {
            PvzLane lane = lanes.get(winner);
            Bukkit.broadcastMessage(ChatColor.GOLD + "【PVZ】" + lane.display() + " 获胜！坚持到了最后！");
        } else if (!adminStop) {
            Bukkit.broadcastMessage(ChatColor.GOLD + "【PVZ】所有队伍均失败，本局无胜者");
        }
        pvzPlayers.clear();
        playerClass.clear();
        playerLane.clear();
        for (PvzLane lane : lanes.values()) {
            lane.reset(baseHealth);
        }
    }

    // ---------------------------------------------------------------- 每 tick

    private void tick() {
        if (!running) {
            return;
        }
        long now = Bukkit.getCurrentTick();
        if (now - waveStartedTick >= waveDurationTicks) {
            waveIndex++;
            waveStartedTick = now;
            Bukkit.broadcastMessage(ChatColor.GOLD + "【PVZ】第 " + (waveIndex + 1)
                    + " 波来袭！怪物更强了！");
        }
        for (PvzLane lane : lanes.values()) {
            if (!lane.isActive()) {
                continue;
            }
            lane.setSpawnTicks(lane.spawnTicks() - SPAWN_TIMER_STEP_TICKS);
            if (lane.spawnTicks() <= 0) {
                spawnBlindBox(lane);
                lane.setSpawnTicks(spawnIntervalTicks(waveIndex));
            }
        }
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (!(entity instanceof Mob mob) || !mob.getScoreboardTags().contains(TAG_MONSTER)) {
                    continue;
                }
                if (!mob.isValid()) {
                    continue;
                }
                PvzLane lane = laneOfMob(mob);
                if (lane == null || !lane.isActive()) {
                    if (lane == null || lane.eliminated()) {
                        mob.remove();
                    }
                    continue;
                }
                Player target = nearestPvzPlayer(mob.getLocation(), lane, TARGET_RANGE);
                if (target != null) {
                    mob.setTarget(target);
                } else if (mob.getTarget() != null) {
                    mob.setTarget(null);
                }
                if (lane.base() == null) {
                    continue;
                }
                mob.getPathfinder().moveTo(lane.base(), 1.0);
                if (mob.getLocation().distanceSquared(lane.base()) <= ARRIVAL_DISTANCE_SQ) {
                    onMobArrived(mob, lane);
                }
            }
        }
        refreshSidebar();
        refreshBars();
    }

    private Player nearestPvzPlayer(Location from, PvzLane lane, double range) {
        double best = range * range;
        Player found = null;
        for (UUID id : pvzPlayers) {
            Player p = Bukkit.getPlayer(id);
            if (p == null || !p.isOnline() || p.getGameMode() == GameMode.SPECTATOR
                    || !lane.id().equals(playerLane.get(id))) {
                continue;
            }
            double d = p.getLocation().distanceSquared(from);
            if (d <= best) {
                best = d;
                found = p;
            }
        }
        return found;
    }

    private void onMobArrived(Mob mob, PvzLane lane) {
        mob.remove();
        if (!running || !lane.isActive()) {
            return;
        }
        boolean broken = lane.hitBase();
        Bukkit.broadcastMessage(ChatColor.RED + "【PVZ】" + lane.display() + " 基地受到攻击！剩余 "
                + lane.baseHealth() + "/" + lane.maxHealth());
        if (broken) {
            eliminateLane(lane, "基地被攻破");
        }
    }

    // ---------------------------------------------------------------- 怪物生成

    private PvzLane laneOfMob(Entity mob) {
        String id = mob.getPersistentDataContainer().get(laneKey, PersistentDataType.STRING);
        return id == null ? null : lanes.get(id);
    }

    private void spawnBlindBox(PvzLane lane) {
        World world = Bukkit.getWorld(lane.world());
        if (world == null) {
            return;
        }
        Location spawn = lane.spawn();
        Location loc = spawn.clone().add(
                (Math.random() - 0.5) * 2.0, 0, (Math.random() - 0.5) * 2.0);
        Zombie zombie = world.spawn(loc, Zombie.class, z -> {
            z.setBaby(false);
            z.setCanPickupItems(false);
            z.setShouldBurnInDay(false);
            z.getEquipment().clear();
            z.getEquipment().setHelmet(blindBoxHelmet());
            z.getEquipment().setHelmetDropChance(0.0f);
            z.setCustomName(ChatColor.DARK_RED + "盲盒僵尸");
            z.setCustomNameVisible(true);
            z.addScoreboardTag(TAG_MONSTER);
            z.addScoreboardTag(TAG_BLINDBOX);
            z.setRemoveWhenFarAway(false);
            z.setPersistent(true);
            z.getPersistentDataContainer().set(laneKey, PersistentDataType.STRING, lane.id());
        });
        if (zombie != null) {
            double hp = monsterHealth(waveIndex);
            zombie.setMaxHealth(hp);
            zombie.setHealth(hp);
            org.bukkit.attribute.AttributeInstance atk = zombie.getAttribute(
                    org.bukkit.attribute.Attribute.ATTACK_DAMAGE);
            if (atk != null) {
                atk.setBaseValue(atk.getBaseValue() * monsterAttackMultiplier);
            }
        }
    }

    /** 盲盒僵尸死亡 → 随机召唤原版僵尸或苦力怕（均为 PVZ 独立标签怪物）。 */
    public void onBlindBoxDeath(Mob mob) {
        PvzLane lane = laneOfMob(mob);
        if (lane == null) {
            return;
        }
        Location loc = mob.getLocation();
        boolean zombie = Math.random() < 0.5;
        if (zombie) {
            World world = loc.getWorld();
            if (world == null) {
                return;
            }
            Zombie z = world.spawn(loc, Zombie.class, e -> {
                e.setBaby(false);
                e.setCanPickupItems(false);
                e.setShouldBurnInDay(false);
                e.getEquipment().clear();
                e.addScoreboardTag(TAG_MONSTER);
                e.addScoreboardTag(TAG_SUMMON);
                e.setRemoveWhenFarAway(false);
                e.setPersistent(true);
                e.getPersistentDataContainer().set(laneKey, PersistentDataType.STRING, lane.id());
            });
            if (z != null) {
                Bukkit.broadcastMessage(ChatColor.YELLOW + "【PVZ】盲盒僵尸死亡，召唤出一只原版僵尸！");
            }
        } else {
            World world = loc.getWorld();
            if (world == null) {
                return;
            }
            Creeper c = world.spawn(loc, Creeper.class, e -> {
                e.setCanPickupItems(false);
                e.addScoreboardTag(TAG_MONSTER);
                e.addScoreboardTag(TAG_SUMMON);
                e.setRemoveWhenFarAway(false);
                e.setPersistent(true);
                e.getPersistentDataContainer().set(laneKey, PersistentDataType.STRING, lane.id());
            });
            if (c != null) {
                Bukkit.broadcastMessage(ChatColor.YELLOW + "【PVZ】盲盒僵尸死亡，召唤出一只原版苦力怕！");
            }
        }
    }

    public boolean isPvzMonster(Entity entity) {
        return entity != null && entity.getScoreboardTags().contains(TAG_MONSTER);
    }

    /** 判断 PVZ 怪物是否可以攻击指定玩家：必须同路、在线且仍为存活玩家。 */
    public boolean canTarget(Entity monster, Player target) {
        if (!isPvzMonster(monster) || target == null || !isPlaying(target)
                || target.getGameMode() == GameMode.SPECTATOR) {
            return false;
        }
        PvzLane lane = laneOfMob(monster);
        return lane != null && lane.id().equals(playerLane.get(target.getUniqueId()));
    }

    /** 监听器入口：PVZ 怪物死亡（清掉落；盲盒僵尸额外触发召唤）。 */
    public void onMonsterDeath(EntityDeathEvent event) {
        event.getDrops().clear();
        if (event.getEntity() instanceof Mob mob && mob.getScoreboardTags().contains(TAG_BLINDBOX)) {
            onBlindBoxDeath(mob);
        }
    }

    /** 监听器入口：PVZ 苦力怕爆炸不破坏地形（保留对实体的伤害）。 */
    public void handleExplode(EntityExplodeEvent event) {
        if (isPvzMonster(event.getEntity())) {
            event.blockList().clear();
        }
    }

    private void removeAllPvzMobs() {
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity.getScoreboardTags().contains(TAG_MONSTER)) {
                    entity.remove();
                }
            }
        }
    }

    // ---------------------------------------------------------------- 玩家死亡 / 退出

    /** 监听器入口：PVZ 玩家死亡 → 观察者，不复活；队伍全灭即淘汰。 */
    public void onPlayerDeath(Player player) {
        if (!isPlaying(player)) {
            return;
        }
        player.setGameMode(GameMode.SPECTATOR);
        player.sendMessage(ChatColor.RED + "【PVZ】你已阵亡，本局不会复活，请以旁观者身份观战。");
        decrementAlive(player);
    }

    /** 监听器入口：PVZ 玩家中途退出 → 视同阵亡。 */
    public void onPlayerQuit(Player player) {
        if (!isPlaying(player)) {
            return;
        }
        pvzPlayers.remove(player.getUniqueId());
        playerClass.remove(player.getUniqueId());
        String laneId = playerLane.remove(player.getUniqueId());
        manager.setPvzPlayer(player.getUniqueId(), false);
        if (laneId != null) {
            PvzLane lane = lanes.get(laneId);
            if (lane != null) {
                lane.setAlivePlayers(lane.alivePlayers() - 1);
                if (running && lane.isActive() == false && !lane.eliminated()) {
                    eliminateLane(lane, "队伍全灭");
                }
            }
        }
    }

    private void decrementAlive(Player player) {
        String laneId = playerLane.get(player.getUniqueId());
        if (laneId == null) {
            return;
        }
        PvzLane lane = lanes.get(laneId);
        if (lane == null) {
            return;
        }
        lane.setAlivePlayers(lane.alivePlayers() - 1);
        if (running && !lane.eliminated() && lane.alivePlayers() <= 0) {
            eliminateLane(lane, "队伍全灭");
        }
    }

    /** 淘汰一条路：清怪、广播、判定剩余队伍。 */
    private void eliminateLane(PvzLane lane, String reason) {
        if (lane.eliminated()) {
            return;
        }
        lane.eliminate();
        Bukkit.broadcastMessage(ChatColor.RED + "【PVZ】" + lane.display() + " 被淘汰（" + reason + "）！");
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity.getScoreboardTags().contains(TAG_MONSTER)
                        && lane.id().equals(entity.getPersistentDataContainer()
                                .get(laneKey, PersistentDataType.STRING))) {
                    entity.remove();
                }
            }
        }
        if (!running) {
            return;
        }
        List<String> remaining = new ArrayList<>();
        for (PvzLane l : lanes.values()) {
            if (!l.eliminated() && l.alivePlayers() > 0) {
                remaining.add(l.id());
            }
        }
        if (remaining.size() == 1) {
            endGameInternal(remaining.get(0), false);
        } else if (remaining.isEmpty()) {
            endGameInternal(null, false);
        }
    }

    // ---------------------------------------------------------------- 玩家回大厅 / 退出 PVZ

    /** /lb 或管理员操作：PVZ 玩家主动离开。 */
    public void returnPlayerToLobby(Player player) {
        returnPlayerToLobby(player, true);
    }

    private void returnPlayerToLobby(Player player, boolean checkElimination) {
        if (!isPlaying(player)) {
            return;
        }
        pvzPlayers.remove(player.getUniqueId());
        playerClass.remove(player.getUniqueId());
        String laneId = playerLane.remove(player.getUniqueId());
        manager.setPvzPlayer(player.getUniqueId(), false);
        player.getInventory().clear();
        player.getEnderChest().clear();
        player.setGameMode(GameMode.ADVENTURE);
        player.setHealth(player.getMaxHealth());
        player.setFoodLevel(20);
        player.clearActivePotionEffects();
        player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        manager.applyLobbyItems(player);
        manager.teleportToLobby(player);
        manager.refreshPlayerListName(player);
        player.sendMessage(ChatColor.GRAY + "【PVZ】你已离开 PVZ 模式");
        if (checkElimination && running && laneId != null) {
            PvzLane lane = lanes.get(laneId);
            if (lane != null && !lane.eliminated()) {
                lane.setAlivePlayers(lane.alivePlayers() - 1);
                if (lane.alivePlayers() <= 0) {
                    eliminateLane(lane, "队伍全灭");
                }
            }
        }
    }

    // ---------------------------------------------------------------- 显示

    public void refreshListName(Player player) {
        String laneId = playerLane.get(player.getUniqueId());
        PvzClass clazz = playerClass.get(player.getUniqueId());
        StringBuilder sb = new StringBuilder();
        sb.append(ChatColor.GOLD).append("[PVZ] ");
        if (laneId != null) {
            sb.append(teamColor(laneId)).append(DaveManager.TEAM_DISPLAYS.getOrDefault(laneId, laneId)).append(' ');
        }
        sb.append(ChatColor.WHITE).append(player.getName());
        if (clazz != null) {
            sb.append(ChatColor.GRAY).append(" · ").append(clazz.displayName());
        }
        player.setPlayerListName(sb.toString());
        player.setDisplayName(sb.toString());
    }

    private static ChatColor teamColor(String team) {
        return switch (team) {
            case "red" -> ChatColor.RED;
            case "blue" -> ChatColor.BLUE;
            case "yellow" -> ChatColor.YELLOW;
            case "green" -> ChatColor.GREEN;
            default -> ChatColor.GRAY;
        };
    }

    private void buildSidebar() {
        pvzBoard = Bukkit.getScoreboardManager().getNewScoreboard();
        sidebar = pvzBoard.registerNewObjective("pvz_mode", "dummy", "植物大战僵尸");
        sidebar.setDisplaySlot(DisplaySlot.SIDEBAR);
        for (UUID id : pvzPlayers) {
            Player p = Bukkit.getPlayer(id);
            if (p != null && p.isOnline()) {
                p.setScoreboard(pvzBoard);
            }
        }
    }

    private void refreshSidebar() {
        if (sidebar == null || !running) {
            return;
        }
        for (String entry : pvzBoard.getEntries()) {
            pvzBoard.resetScores(entry);
        }
        sidebar.getScore(ChatColor.GOLD + "第 " + (waveIndex + 1) + " 波").setScore(10);
        int score = 9;
        for (PvzLane lane : lanes.values()) {
            String line;
            if (lane.eliminated()) {
                line = ChatColor.RED + lane.display() + " 已淘汰";
            } else {
                line = teamColor(lane.id()) + lane.display() + " " + lane.baseHealth() + "/"
                        + lane.maxHealth() + ChatColor.GRAY + " 存活" + lane.alivePlayers();
            }
            sidebar.getScore(line).setScore(score--);
        }
    }

    private void clearSidebar() {
        if (pvzBoard != null) {
            for (UUID id : pvzPlayers) {
                Player p = Bukkit.getPlayer(id);
                if (p != null && p.isOnline()) {
                    p.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
                }
            }
            pvzBoard = null;
            sidebar = null;
        }
    }

    private void buildBars() {
        for (PvzLane lane : lanes.values()) {
            BossBar bar = Bukkit.createBossBar(lane.display() + "基地 " + lane.maxHealth() + "/" + lane.maxHealth(),
                    barColor(lane.id()), BarStyle.SOLID);
            bar.setProgress(1.0);
            for (UUID id : pvzPlayers) {
                Player p = Bukkit.getPlayer(id);
                if (p != null && p.isOnline()) {
                    bar.addPlayer(p);
                }
            }
            laneBars.put(lane.id(), bar);
        }
    }

    private void refreshBars() {
        for (PvzLane lane : lanes.values()) {
            BossBar bar = laneBars.get(lane.id());
            if (bar == null) {
                continue;
            }
            bar.setTitle(lane.display() + "基地 " + lane.baseHealth() + "/" + lane.maxHealth());
            bar.setProgress(Math.max(0.0, (double) lane.baseHealth() / lane.maxHealth()));
            bar.setVisible(!lane.eliminated());
        }
    }

    private void removeBars() {
        for (BossBar bar : laneBars.values()) {
            bar.removeAll();
        }
        laneBars.clear();
    }

    private static BarColor barColor(String team) {
        return switch (team) {
            case "red" -> BarColor.RED;
            case "blue" -> BarColor.BLUE;
            case "yellow" -> BarColor.YELLOW;
            case "green" -> BarColor.GREEN;
            default -> BarColor.WHITE;
        };
    }
}
