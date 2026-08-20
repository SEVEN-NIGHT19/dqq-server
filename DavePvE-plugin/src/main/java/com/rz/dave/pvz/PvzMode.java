package com.rz.dave.pvz;
import com.rz.dave.DaveManager;
import com.rz.dave.monster.MonsterManager;
import com.rz.dave.monster.SpawnContext;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.DragonFireball;
import org.bukkit.entity.LargeFireball;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.SmallFireball;
import org.bukkit.entity.Snowball;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;
import org.bukkit.scoreboard.Scoreboard;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 随机植物对战随机僵尸模式（PVZ）核心状态机。
 * 与经典 Dave PvE 大模式完全独立：独立场地（config 配置）、独立职业、独立怪物
 * （pvz_ 前缀标签，经典模式的 isMonster/isDave 不会触碰）、独立胜负流程。
 *
 * 规则（按用户需求 + 已确认的缺陷补全）：
 * - 五条数字路（one~five，显示 1路~5路），与四色队伍完全解耦；一路 = 一个队伍，
 *   每队最多 5 名玩家（默认 pvz.players-per-lane）共同守这一条路，一局上限 5×5=25 人；
 *   分路按顺序优先凑满前面的队伍，超出上限的玩家本局不进入；本模式不能中途加入；
 * - 玩家开局随机职业（机枪射手/寒冰射手/坚果，可重复），死亡转观察者不复活；
 * - 玩家默认血量 40（10 颗红心 × 4？不——40 点即 20 颗红心）；坚果职业 120 点血量、只受 10%
 *   伤害、体型 1.5 倍（游戏结束恢复）；护甲均为染色皮革套且无限耐久；
 * - PVZ 玩家默认无法自然回血（清空饱和度 + 拦截自然回血事件），后续由职业机制回血；
 * - PVZ 怪物默认免疫击退（拦截 EntityKnockbackEvent），后续由其他机制代替击退；
 * - 每路基地生命（默认 10），怪物走到路终点扣 1 点，归零该路淘汰；
 * - 一路全员阵亡即淘汰；坚持到其他所有路失败即获胜；
 * - 开局只有一路有玩家 → 正常开始，全员阵亡才结束；
 * - 主路怪物只做「盲盒僵尸」（固定 25 点血），死亡后随机召唤：50% 苦力怕 / 20% 普通僵尸 /
 *   10% 巨人僵尸 / 10% 路障僵尸 / 10% 铁桶僵尸（均不掉落、不惧阳光，召唤不播报）；
 * - 波次无限，难度随时间递增；与经典模式可同时运行。
 */
public final class PvzMode {

    public static final String TAG_MONSTER = MonsterManager.TAG_MONSTER;
    public static final String TAG_BLINDBOX = MonsterManager.TAG_BLINDBOX;
    public static final String TAG_SUMMON = MonsterManager.TAG_SUMMON;

    /** PVZ 五条数字路（one~five），不再使用四色队伍。 */
    public static final List<String> LANE_IDS = List.of("one", "two", "three", "four", "five");
    public static final Map<String, String> LANE_DISPLAYS = Map.of(
            "one", "1路", "two", "2路", "three", "3路", "four", "4路", "five", "5路");
    /** 旧版 PVZ 曾用四色队伍作为路；启动时一次性清理其 config 坐标。 */
    private static final List<String> LEGACY_COLOR_LANES = List.of("red", "blue", "yellow", "green");

    /** 每条路（每个队伍）玩家上限的默认值。 */
    public static final int DEFAULT_PLAYERS_PER_LANE = 5;

    /** 怪物对玩家的主动仇恨范围：原版 AI 找玩家、受击反打（HurtByTargetGoal）都受 FOLLOW_RANGE
     *  属性限制（2 格外怪物完全不对玩家产生仇恨，防走位拖怪堆叠）；本路玩家进入 2 格内才被攻击。 */
    public static final double MOB_MELEE_RANGE = 2.0;
    private static final int ARRIVAL_DISTANCE_SQ = 3; // ~1.7 格水平距离
    private static final int SPAWN_TIMER_STEP_TICKS = 20; // tick() 每秒执行一次

    private final Plugin plugin;
    private final DaveManager manager;
    private final MonsterManager monsterManager;
    private final NamespacedKey laneKey;

    // config 数值
    private int baseHealth = 10;
    private int waveDurationTicks = 600;          // 每 30 秒升一波
    private int spawnIntervalTicksBase = 160;     // 基础刷怪间隔（每路每只）
    private int spawnIntervalTicksStep = 10;      // 每波缩短
    private int spawnsPerWaveBase = 3;            // 第 1 波每路总量
    private int spawnsPerWaveGrowth = 1;          // 每波 +1
    private double monsterAttackMultiplier = 0.5;
    private int playersPerLane = DEFAULT_PLAYERS_PER_LANE; // 每条路（每队）玩家上限

    // ---- RPRZ 装备与数值常量 ----
    public static final double BLIND_BOX_HEALTH = 25.0;       // 盲盒僵尸固定血量
    public static final double PLAYER_BASE_HEALTH = 40.0;     // RPRZ 玩家默认血量（20 颗红心）
    public static final double WALLNUT_HEALTH = 120.0;        // 坚果血量
    public static final double WALLNUT_DAMAGE_RATIO = 0.10;   // 坚果所受伤害比例
    public static final double WALLNUT_SCALE = 1.5;           // 坚果体型倍数（结束恢复 1.0）
    public static final long SHOOTER_COOLDOWN_MS = 1400L;     // 射手攻击冷却 1.4 秒
    public static final double SHOOTER_BULLET_DAMAGE = 2.0;   // 每发子弹伤害
    public static final double SHOOTER_BULLET_SPEED = 1.8;    // 子弹基础速度
    public static final int MACHINE_BURST_COUNT = 5;          // 机枪射手一次连发数（4→5）
    public static final int ICE_BURST_COUNT = 2;              // 寒冰射手一次连发数
    public static final int DUAL_BURST_COUNT = 2;             // 双发射手一次连发数（普豌双发）
    public static final long DUAL_BULLET_INTERVAL_TICKS = 4;  // 双发两弹发射间隔
    public static final long SNIPER_COOLDOWN_MS = 7000L;      // 狙击豌豆冷却 7 秒
    public static final double SNIPER_BULLET_DAMAGE = 40.0;   // 狙击对无甲怪伤害
    public static final double SNIPER_BULLET_SPEED = 15.0;    // 大模式高速子弹速度
    public static final double SNIPER_IGNORE_ARMOR_DAMAGE = 9999.0; // 无视二/三类防具：直接打空本体
    public static final double ICICLE_CHANCE = 0.10;          // 每次射击额外冰棱概率 10%
    public static final double ICICLE_DAMAGE = 4.0;           // 冰棱穿透伤害
    public static final double ICICLE_SCALE = 0.5;            // 冰棱模型为 0.5 倍方块
    public static final double ICICLE_SPEED_MULTIPLIER = 1.5; // 冰棱弹速倍数
    public static final int ICICLE_MAX_TICKS = 100;           // 冰棱最长飞行 5 秒
    public static final int ICE_SLOW_DURATION = 200;          // 缓慢三时长 10 秒
    public static final int ICE_SLOW_AMPLIFIER = 2;           // 缓慢等级 3（amplifier 2）
    public static final int ICE_FREEZE_TICKS = 36;            // 冻结 1.8 秒
    public static final double ICE_FREEZE_BONUS = 2.0;        // 已有缓慢 → 冻结时的额外伤害
    public static final double ICE_FROZEN_BONUS = 4.0;        // 正在冻结 → 重置冻结时的额外伤害

    // ---- 双发射手七种子弹数值（都是 1/7 概率）----
    public static final double FIRE_SMALL_DAMAGE = 4.0;       // 火豆：小火焰弹
    public static final double FIRE_SMALL_SPLASH = 1.5;
    public static final double FIRE_ORANGE_DAMAGE = 7.0;      // 橙色火豆：恶魂大火焰弹
    public static final double FIRE_ORANGE_SPLASH = 3.0;
    public static final double FIRE_ORANGE_ARMOR_BONUS = 7.0; // 对二类防具额外伤害（无溅射）
    public static final double FIRE_RED_DAMAGE = 11.0;        // 红色火豆：红色玻璃 0.3 倍
    public static final double FIRE_RED_SPLASH = 5.0;
    public static final double FIRE_RED_ARMOR_BONUS = 11.0;
    public static final double ICEFIRE_DAMAGE = 4.0;          // 冰火豌豆：紫色玻璃 0.3 倍（减速同冰豆）
    public static final double ICEFIRE_SPLASH = 1.5;
    public static final double ICEFIRE_ARMOR_BONUS = 4.0;
    public static final double DRAGON_DAMAGE = 7.0;           // 幽蓝火焰豌豆：龙息弹（减速同冰豆）
    public static final double DRAGON_SPLASH = 3.0;
    public static final double DRAGON_ARMOR_BONUS = 7.0;

    // ---- 小喷菇 / 海蘑菇（喷菇体系）----
    public static final double PUFF_DAMAGE = 2.0;             // 每发子弹伤害 2
    public static final double SMALL_PUFF_RANGE = 10.0;       // 小喷菇子弹射程 10 格
    public static final double SEA_MUSHROOM_RANGE = 14.0;     // 海蘑菇子弹射程 14 格
    public static final double SMALL_PUFF_SCALE = 0.7;        // 小喷菇体型 0.7 倍
    public static final double SEA_MUSHROOM_SCALE = 0.5;      // 海蘑菇体型 0.5 倍
    public static final long PUFF_BURST_INTERVAL_TICKS = 2;   // 海蘑菇每轮内 2 连发的间隔
    /** 黄心映射：原版黄心（吸收值）上限 20 点（10 颗），1 颗 = ABSORPTION_SCALE 点虚拟黄心。
     *  小喷菇上限 160 虚拟 = 10 颗满；海蘑菇上限 120 虚拟 = 7.5 颗。 */
    public static final double ABSORPTION_SCALE = 16.0;
    public static final double PUFF_HEARTS_PER_PULSE = 40.0;  // 每次脉冲 +40 虚拟黄心
    public static final long SMALL_PUFF_PULSE_TICKS = 800L;   // 小喷菇每 40 秒一次脉冲
    public static final long SEA_MUSHROOM_PULSE_TICKS = 3200L;// 海蘑菇每 160 秒一次脉冲
    public static final double SMALL_PUFF_ABSORPTION_CAP = 10.0;  // 160 虚拟 = 10 颗黄心
    public static final double SEA_MUSHROOM_ABSORPTION_CAP = 7.5; // 120 虚拟 = 7.5 颗黄心
    public static final double PUFF_SCATTER_DEG = 4.0;        // 散射幅度（±4°，尽量小、对准视线）
    public static final int DISPLAY_BULLET_MAX_TICKS = 100;   // 方块子弹（玻璃弹）最长飞行 5 秒

    private final NamespacedKey shooterKey;      // 武器标记（string: machine/ice）
    private final NamespacedKey bulletKey;       // PVZ 子弹标记（byte）
    private final NamespacedKey bulletKindKey;   // 子弹类型（string: machine/ice）
    private final NamespacedKey bulletDamageKey; // 子弹伤害（double）
    private final NamespacedKey bulletOwnerKey;  // 子弹发射者 UUID（string）
    private final Map<UUID, Long> shooterCooldowns = new HashMap<>();
    private final Map<UUID, Long> frozenUntil = new HashMap<>(); // 怪物 UUID → 冻结到期 tick
    private final Map<UUID, Set<UUID>> icicleHits = new HashMap<>();

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
    private BukkitTask monsterTickTask;
    /** 喷菇职业的加黄心脉冲任务（按玩家独立：同职业多人互不影响）。 */
    private final java.util.Map<java.util.UUID, BukkitTask> puffPulseTasks = new java.util.HashMap<>();
    /** 游戏结束后的 10 秒缓冲：此窗口内玩家留在场地观战，随后统一清退回大厅。 */
    private List<UUID> pendingEndPlayers;
    private BukkitTask pendingEndTask;
    private Scoreboard pvzBoard;
    private Objective sidebar;
    private String winnerLane;

    public PvzMode(Plugin plugin, DaveManager manager, MonsterManager monsterManager) {
        this.plugin = plugin;
        this.manager = manager;
        this.monsterManager = monsterManager;
        this.laneKey = new NamespacedKey(plugin, "pvz_lane");
        this.shooterKey = new NamespacedKey(plugin, "pvz_shooter");
        this.bulletKey = new NamespacedKey(plugin, "pvz_bullet");
        this.bulletKindKey = new NamespacedKey(plugin, "pvz_bullet_kind");
        this.bulletDamageKey = new NamespacedKey(plugin, "pvz_bullet_damage");
        this.bulletOwnerKey = new NamespacedKey(plugin, "pvz_bullet_owner");
        for (String id : LANE_IDS) {
            lanes.put(id, new PvzLane(id, LANE_DISPLAYS.getOrDefault(id, id), baseHealth));
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

    /** 上一局获胜路 id（无胜者为 null）。 */
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
        monsterAttackMultiplier = cfg.getDouble("pvz.monster-attack-multiplier", 0.5);
        playersPerLane = Math.max(1, cfg.getInt("pvz.players-per-lane", DEFAULT_PLAYERS_PER_LANE));
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
            // 旧版用四色队伍（red/blue/yellow/green）作为路；PVZ 改为数字路后，
            // 一次性删除测试期留下的四色队伍坐标（以后四色队伍只用于大模式）。
            boolean removedLegacy = false;
            for (String legacy : LEGACY_COLOR_LANES) {
                if (sec.contains(legacy)) {
                    cfg.set("pvz.lanes." + legacy, null);
                    removedLegacy = true;
                }
            }
            if (removedLegacy) {
                plugin.saveConfig();
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
            return ChatColor.RED + "未知路: " + team;
        }
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return ChatColor.RED + "世界不存在: " + worldName;
        }
        lane.setSpawn(new Location(world, x, y, z));
        lane.setWorld(worldName);
        saveLaneConfig(team);
        return ChatColor.GREEN + "已设置 " + lane.display() + " 的怪物生成点";
    }

    public String setLaneBase(String team, Player admin) {
        return setLaneBase(team, admin.getWorld().getName(), admin.getLocation().getX(),
                admin.getLocation().getY(), admin.getLocation().getZ());
    }

    public String setLaneBase(String team, String worldName, double x, double y, double z) {
        PvzLane lane = lanes.get(team);
        if (lane == null) {
            return ChatColor.RED + "未知路: " + team;
        }
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return ChatColor.RED + "世界不存在: " + worldName;
        }
        lane.setBase(new Location(world, x, y, z));
        lane.setWorld(worldName);
        saveLaneConfig(team);
        return ChatColor.GREEN + "已设置 " + lane.display() + " 的终点（基地）";
    }

    public String setLanePlayerSpawn(String team, Player admin) {
        return setLanePlayerSpawn(team, admin.getWorld().getName(), admin.getLocation().getX(),
                admin.getLocation().getY(), admin.getLocation().getZ());
    }

    public String setLanePlayerSpawn(String team, String worldName, double x, double y, double z) {
        PvzLane lane = lanes.get(team);
        if (lane == null) {
            return ChatColor.RED + "未知路: " + team;
        }
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return ChatColor.RED + "世界不存在: " + worldName;
        }
        lane.setPlayerSpawn(new Location(world, x, y, z));
        lane.setWorld(worldName);
        saveLaneConfig(team);
        return ChatColor.GREEN + "已设置 " + lane.display() + " 的玩家出生点";
    }

    // ---------------------------------------------------------------- 波次数值（纯逻辑，可单测）

    public static int spawnsPerWave(int wave) {
        return 3 + wave * 1;
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
        // 上一局若还有未执行的 10 秒清退，立即完成，避免旧局玩家残留 PVZ 状态。
        flushPendingEnd();
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
                requester.sendMessage(ChatColor.RED + "【PVZ】" + lane.display() + " 场地未配置，请先使用 "
                        + ChatColor.YELLOW + "/davepve pvz setspawn|setbase|setplayer " + lane.id());
                return;
            }
            World world = Bukkit.getWorld(lane.world());
            if (world == null) {
                requester.sendMessage(ChatColor.RED + "【PVZ】" + lane.display() + " 世界不存在: " + lane.world());
                return;
            }
        }
        // 本模式不能中途加入：开局时一次性快照准备名单，进行中不再接收新玩家
        // 人数上限：5 路 × 每路上限（默认 5 人）= 25 人；超出部分随机剔除，本局不进入
        int maxPlayers = lanes.size() * playersPerLane;
        if (ready.size() > maxPlayers) {
            Collections.shuffle(ready);
            for (Player excluded : ready.subList(maxPlayers, ready.size())) {
                excluded.sendMessage(ChatColor.YELLOW + "【PVZ】本局名额已满（上限 " + maxPlayers
                        + " 人），你未入选本局游戏，可准备下一局。");
            }
            ready = new ArrayList<>(ready.subList(0, maxPlayers));
        }
        // 分路：一路 = 一个队伍，按顺序优先凑满前面的队伍（每队最多 playersPerLane 人）
        Map<String, List<Player>> byLane = new LinkedHashMap<>();
        for (String id : lanes.keySet()) {
            byLane.put(id, new ArrayList<>());
        }
        int laneIndex = 0;
        for (Player p : ready) {
            while (byLane.get(LANE_IDS.get(laneIndex)).size() >= playersPerLane) {
                laneIndex++;
            }
            byLane.get(LANE_IDS.get(laneIndex)).add(p);
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
                // 职业数值：默认 40 点血；坚果 120 点血 + 1.5 倍体型；小喷菇 0.7 倍、海蘑菇 0.5 倍
                boolean wallnut = clazz == PvzClass.WALLNUT;
                p.setMaxHealth(wallnut ? WALLNUT_HEALTH : PLAYER_BASE_HEALTH);
                p.setHealth(p.getMaxHealth());
                p.setAbsorptionAmount(0.0);
                double scale = wallnut ? WALLNUT_SCALE
                        : clazz == PvzClass.SMALL_PUFF ? SMALL_PUFF_SCALE
                        : clazz == PvzClass.SEA_MUSHROOM ? SEA_MUSHROOM_SCALE : 1.0;
                if (scale != 1.0) {
                    AttributeInstance scaleAttr = p.getAttribute(Attribute.SCALE);
                    if (scaleAttr != null) {
                        scaleAttr.setBaseValue(scale);
                    }
                }
                for (ItemStack item : clazz.createKit()) {
                    p.getInventory().addItem(item);
                }
                ItemStack[] armor = clazz.createArmor();
                if (armor.length > 0) {
                    p.getInventory().setArmorContents(armor);
                }
                if (clazz.isShooter()) {
                    markShooterWeapon(p, clazz);
                }
                // 喷菇职业：独立脉冲加黄心（40 秒 / 160 秒一次；per-player 互不影响）
                if (clazz == PvzClass.SMALL_PUFF || clazz == PvzClass.SEA_MUSHROOM) {
                    final PvzClass puffClass = clazz;
                    long period = clazz == PvzClass.SMALL_PUFF
                            ? SMALL_PUFF_PULSE_TICKS : SEA_MUSHROOM_PULSE_TICKS;
                    BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                        if (!running || !isPlaying(p)
                                || !puffClass.equals(playerClass.get(p.getUniqueId()))) {
                            return;
                        }
                        givePuffHearts(p, puffClass);
                    }, period, period);
                    puffPulseTasks.put(p.getUniqueId(), task);
                }
                p.setGameMode(GameMode.ADVENTURE);
                p.setFoodLevel(20);
                p.setSaturation(20);
                p.clearActivePotionEffects();
                World world = Bukkit.getWorld(lane.world());
                Location spawn = lane.playerSpawn();
                p.teleport(new Location(world, spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5));
                p.sendTitle(ChatColor.GOLD + "随机植物对战随机僵尸",
                        "你的路：" + lane.display() + " · 职业：" + clazz.displayName(), 10, 60, 10);
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
        // 怪物仇恨/移动/攻击每 tick 管控（清目标防拖怪、近战攻击、苦力怕爆炸、向基地推进）
        monsterTickTask = Bukkit.getScheduler().runTaskTimer(plugin, this::monsterTick, 1L, 1L);
        StringBuilder roster = new StringBuilder();
        for (String id : LANE_IDS) {
            if (!byLane.get(id).isEmpty()) {
                if (roster.length() > 0) {
                    roster.append("，");
                }
                roster.append(LANE_DISPLAYS.get(id)).append(' ').append(byLane.get(id).size()).append(" 人");
            }
        }
        Bukkit.broadcastMessage(ChatColor.GOLD + "【PVZ】随机植物对战随机僵尸开始！"
                + ChatColor.GRAY + "守住你的路，坚持到其他路全部失败！");
        Bukkit.broadcastMessage(ChatColor.GRAY + "【PVZ】本局 " + ready.size() + " 人：" + roster
                + "；本模式不能中途加入。");
        Bukkit.broadcastMessage(ChatColor.GRAY + "【PVZ】第 1 波即将来袭；死亡不会复活，基地生命 "
                + baseHealth + " 点。");
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
        cancelPendingEnd();
        if (tickTask != null) {
            tickTask.cancel();
            tickTask = null;
        }
        if (monsterTickTask != null) {
            monsterTickTask.cancel();
            monsterTickTask = null;
        }
        running = false;
        winnerLane = winner;
        // 停止全部喷菇职业脉冲任务并清空黄心（黄心/档位/发射数不跨局残留）
        for (BukkitTask task : puffPulseTasks.values()) {
            task.cancel();
        }
        puffPulseTasks.clear();
        for (UUID id : new ArrayList<>(pvzPlayers)) {
            Player p = Bukkit.getPlayer(id);
            if (p != null && p.isOnline()) {
                p.setAbsorptionAmount(0.0);
            }
        }
        removeAllPvzMobs();
        removeBars();
        clearSidebar();
        // 结束即清空参与者背包（职业装备不残留），红字在屏幕中央公布结束（仅限 rprz 玩家）。
        List<UUID> ended = new ArrayList<>(pvzPlayers);
        for (UUID id : ended) {
            Player p = Bukkit.getPlayer(id);
            if (p != null && p.isOnline()) {
                p.getInventory().clear();
                p.getEnderChest().clear();
            }
        }
        String subtitle;
        if (winner != null) {
            PvzLane wl = lanes.get(winner);
            subtitle = (wl != null ? wl.display() : winner) + " 获胜！坚持到了最后！";
        } else if (adminStop) {
            subtitle = "管理员已结束游戏";
        } else {
            subtitle = "所有路均失败，本局无胜者";
        }
        for (UUID id : ended) {
            Player p = Bukkit.getPlayer(id);
            if (p != null && p.isOnline()) {
                p.sendTitle(ChatColor.RED + "游戏结束", subtitle, 10, 70, 10);
            }
        }
        if (winner != null) {
            PvzLane lane = lanes.get(winner);
            Bukkit.broadcastMessage(ChatColor.GOLD + "【PVZ】" + lane.display() + " 获胜！坚持到了最后！");
        } else if (!adminStop) {
            Bukkit.broadcastMessage(ChatColor.GOLD + "【PVZ】所有路均失败，本局无胜者");
        }
        // 10 秒缓冲后统一清退回大厅；管理员强制结束则立即清退。
        pendingEndPlayers = ended;
        if (adminStop) {
            finishPendingEnd();
        } else {
            pendingEndTask = Bukkit.getScheduler().runTaskLater(plugin, this::finishPendingEnd, 200L);
        }
    }

    /** 结束收尾：把记录在案的 PVZ 玩家清退回大厅，并重置本局状态。 */
    private void finishPendingEnd() {
        pendingEndTask = null;
        if (pendingEndPlayers != null) {
            for (UUID id : pendingEndPlayers) {
                Player p = Bukkit.getPlayer(id);
                if (p != null && p.isOnline()) {
                    try {
                        returnPlayerToLobby(p, false);
                    } catch (RuntimeException e) {
                        plugin.getLogger().warning("【PVZ】结束清退玩家失败: " + id + " - " + e.getMessage());
                    }
                }
            }
            pendingEndPlayers = null;
        }
        pvzPlayers.clear();
        playerClass.clear();
        playerLane.clear();
        shooterCooldowns.clear();
        frozenUntil.clear();
        icicleHits.clear();
        for (PvzLane lane : lanes.values()) {
            lane.reset(baseHealth);
        }
    }

    /** 取消尚未执行的结束清退任务（不执行清退）。 */
    private void cancelPendingEnd() {
        if (pendingEndTask != null) {
            pendingEndTask.cancel();
            pendingEndTask = null;
        }
    }

    /** 若存在未执行的结束清退，立即执行完成（新局开始前调用）。 */
    private void flushPendingEnd() {
        if (pendingEndTask != null || pendingEndPlayers != null) {
            finishPendingEnd();
        }
    }

    // ---------------------------------------------------------------- 每 tick

    private void tick() {
        if (!running) {
            return;
        }
        // PVZ 默认无自然回血：维持满饥饿但清空饱和度（自然回血需饱和度过 0），同时避免饿死；
        // 后续回血职业请用 setHealth 或 MAGIC/CUSTOM 来源，不受此拦截影响。
        for (UUID id : pvzPlayers) {
            Player p = Bukkit.getPlayer(id);
            if (p != null && p.isOnline() && p.getGameMode() != GameMode.SPECTATOR) {
                p.setFoodLevel(20);
                p.setSaturation(0.0f);
            }
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
        refreshSidebar();
        refreshBars();
    }

    /**
     * 怪物每 tick 管控（原本在 20 tick 的 tick() 中，仇恨压制需要每 tick 才能压住原版 AI）。
     *
     * <p>规则（需求：怪物不因仇恨追逐玩家，防走位拖怪导致堆叠）：
     * <ul>
     *   <li>怪物生成时统一把 FOLLOW_RANGE 压到 {@link #MOB_MELEE_RANGE}（2 格）：
     *       原版 AI 找玩家、受击反打全部被限制在 2 格内，2 格外怪物不会对玩家产生任何仇恨；</li>
     *   <li>此处每 tick 兜底覆盖目标：2 格内本路存活玩家 → 交给原版 AI 攻击（自带攻击停顿）；
     *       2 格外 → 清空目标，怪物始终向本路基地（终点）推进；</li>
     *   <li>苦力怕：2 格内靠近玩家由原版 AI 引信爆炸；2 格外向基地推进；</li>
     *   <li>巨人僵尸：挥斧由自身动画调度（SLOWNESS 255 攻击期定身），其余同一规则；</li>
     *   <li>到达基地按原逻辑消失并扣基地血。</li>
     * </ul>
     */
    private void monsterTick() {
        if (!running) {
            return;
        }
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (!(entity instanceof Mob mob) || !mob.getScoreboardTags().contains(TAG_MONSTER)) {
                    continue;
                }
                if (!mob.isValid()) {
                    continue;
                }
                // 破甲与灼烧的兜底（实机上事件路径不可靠）：每 tick 检查一次
                maybeBreakArmor(mob);
                if (mob.getFireTicks() > 0) {
                    mob.setFireTicks(0);
                }
                PvzLane lane = laneOfMob(mob);
                if (lane == null || !lane.isActive()) {
                    if (lane == null || lane.eliminated()) {
                        mob.remove();
                    }
                    continue;
                }
                // 仇恨控制：仅 2 格内本路存活玩家会被主动仇恨（原版 AI 攻击自带停顿/追打）
                Player hate = nearestPvzPlayer(mob.getLocation(), lane, MOB_MELEE_RANGE);
                if (hate != null) {
                    mob.setTarget(hate);
                } else if (mob.getTarget() != null) {
                    mob.setTarget(null);
                }
                if (isFrozen(mob)) {
                    continue;   // 冻结：AI 已关，完全不动
                }
                if (lane.base() == null) {
                    continue;
                }
                // 无仇恨时始终向基地推进（每 10 tick 强制重寻一次，覆盖原版随机游荡）
                if (hate == null
                        && (!mob.getPathfinder().hasPath() || Bukkit.getCurrentTick() % 10 == 0)) {
                    mob.getPathfinder().moveTo(lane.base(), 1.0);
                }
                if (mob.getLocation().distanceSquared(lane.base()) <= ARRIVAL_DISTANCE_SQ) {
                    onMobArrived(mob, lane);
                }
            }
        }
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
        monsterManager.spawn(MonsterManager.MonsterType.BLIND_BOX_ZOMBIE, world, loc,
                new SpawnContext(lane.id(), laneKey,
                        BLIND_BOX_HEALTH, monsterAttackMultiplier));
    }

    /**
     * 盲盒僵尸死亡 → 按概率随机召唤（10% 苦力怕 / 22.5% 普通僵尸 / 22.5% 巨人 /
     * 22.5% 路障 / 22.5% 铁桶），不向聊天栏播报开出内容（怪多人多防刷屏）。
     */
    public void onBlindBoxDeath(Mob mob) {
        PvzLane lane = laneOfMob(mob);
        if (lane == null) {
            return;
        }
        Location loc = mob.getLocation();
        World world = loc.getWorld();
        if (world == null) {
            return;
        }
        MonsterManager.MonsterType type = pickSummonType(Math.random());
        SpawnContext ctx = switch (type) {
            case CONEHEAD_ZOMBIE -> new SpawnContext(lane.id(), laneKey,
                    com.rz.dave.monster.ConeheadZombie.HEALTH, 1.0);
            case BUCKETHEAD_ZOMBIE -> new SpawnContext(lane.id(), laneKey,
                    com.rz.dave.monster.BucketheadZombie.HEALTH, 1.0);
            default -> SpawnContext.basic(lane.id(), laneKey);
        };
        monsterManager.spawn(type, world, loc, ctx);
    }

    /** 盲盒僵尸死亡召唤的随机类型（0..1 均匀随机数 → 类型；纯逻辑可单测）。
     *  每次召唤独立随机，概率：10% 苦力怕 / 22.5% 普通 / 22.5% 巨人 /
     *  22.5% 路障 / 22.5% 铁桶。 */
    public static MonsterManager.MonsterType pickSummonType(double roll) {
        if (roll < 0.10) {
            return MonsterManager.MonsterType.SUMMON_CREEPER;
        }
        if (roll < 0.325) {
            return MonsterManager.MonsterType.PLAIN_ZOMBIE;
        }
        if (roll < 0.55) {
            return MonsterManager.MonsterType.GIANT_ZOMBIE;
        }
        if (roll < 0.775) {
            return MonsterManager.MonsterType.CONEHEAD_ZOMBIE;
        }
        return MonsterManager.MonsterType.BUCKETHEAD_ZOMBIE;
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

    // ---------------------------------------------------------------- 射手职业（机枪/寒冰）与冰冻

    /** 是否为 PVZ 射手武器（手持物品带射击标记）。 */
    public boolean isShooterWeapon(ItemStack stack) {
        return stack != null && stack.hasItemMeta()
                && stack.getItemMeta().getPersistentDataContainer()
                        .has(shooterKey, PersistentDataType.STRING);
    }

    /** 读取武器射击类型（machine/ice/dual/sniper）；非 PVZ 武器返回 null。 */
    public String shooterKind(ItemStack stack) {
        if (stack == null || !stack.hasItemMeta()) {
            return null;
        }
        return stack.getItemMeta().getPersistentDataContainer()
                .get(shooterKey, PersistentDataType.STRING);
    }

    /** 是否为 PVZ 子弹（带 PVZ 子弹标记的投射物）。 */
    public boolean isPvzBullet(Projectile projectile) {
        return projectile != null
                && projectile.getPersistentDataContainer().has(bulletKey, PersistentDataType.BYTE);
    }

    /** 是否携带二类（帽子）/三类（手持盾）防具：狙击豌豆无视其伤害直击本体。 */
    public boolean hasArmorDefense(Mob mob) {
        return mob.getScoreboardTags().contains(MonsterManager.TAG_ARMORED)
                || mob.getScoreboardTags().contains(MonsterManager.TAG_SHIELDED);
    }

    /** PVZ 子弹命中怪物（普通投射物路径）：按子弹类型结算。 */
    public void onShooterBulletHit(Projectile projectile, Mob mob) {
        if (mob == null || !isPvzMonster(mob) || !isPvzBullet(projectile)) {
            return;
        }
        String kind = projectile.getPersistentDataContainer().get(bulletKindKey, PersistentDataType.STRING);
        Double damage = projectile.getPersistentDataContainer().get(bulletDamageKey, PersistentDataType.DOUBLE);
        String owner = projectile.getPersistentDataContainer().get(bulletOwnerKey, PersistentDataType.STRING);
        if (kind == null || damage == null || owner == null) {
            return;
        }
        Player shooter = Bukkit.getPlayer(UUID.fromString(owner));
        if (shooter == null) {
            return;
        }
        resolveBulletHit(kind, mob, damage, shooter);
    }

    /** 子弹结算（投射物命中与显示子弹每 tick 命中共用）。 */
    private void resolveBulletHit(String kind, Mob mob, double damage, Player shooter) {
        switch (kind) {
            case "machine" -> damagePvzMonster(mob, damage, shooter);
            case "shrimp", "sea" -> damagePvzMonster(mob, PUFF_DAMAGE, shooter);
            case "ice", "icicle" -> applyIceHit(mob, damage, shooter);
            case "fire_small" -> {
                damagePvzMonster(mob, FIRE_SMALL_DAMAGE, shooter);
                splashAround(mob, FIRE_SMALL_SPLASH, shooter);
            }
            case "fire_orange" -> {
                damagePvzMonster(mob, FIRE_ORANGE_DAMAGE
                        + (hasArmorDefense(mob) ? FIRE_ORANGE_ARMOR_BONUS : 0.0), shooter);
                splashAround(mob, FIRE_ORANGE_SPLASH, shooter);
            }
            case "fire_red" -> {
                damagePvzMonster(mob, FIRE_RED_DAMAGE
                        + (hasArmorDefense(mob) ? FIRE_RED_ARMOR_BONUS : 0.0), shooter);
                splashAround(mob, FIRE_RED_SPLASH, shooter);
            }
            case "ice_fire" -> {
                applyIceHit(mob, ICEFIRE_DAMAGE
                        + (hasArmorDefense(mob) ? ICEFIRE_ARMOR_BONUS : 0.0), shooter);
                splashAround(mob, ICEFIRE_SPLASH, shooter);
            }
            case "dragon" -> {
                applyIceHit(mob, DRAGON_DAMAGE
                        + (hasArmorDefense(mob) ? DRAGON_ARMOR_BONUS : 0.0), shooter);
                splashAround(mob, DRAGON_SPLASH, shooter);
            }
            case "sniper" -> damagePvzMonster(mob,
                    hasArmorDefense(mob) ? SNIPER_IGNORE_ARMOR_DAMAGE : damage, shooter);
            default -> damagePvzMonster(mob, damage, shooter);
        }
    }

    /** 对 PVZ 怪物结算伤害：清空无敌帧保证连发/溅射全部生效，随后检查破甲。 */
    private void damagePvzMonster(Mob mob, double damage, Player shooter) {
        mob.setNoDamageTicks(0);
        mob.damage(damage, shooter);
        maybeBreakArmor(mob);
    }

    /** 溅射：命中怪附近 1 格内**其他** PVZ 怪物吃纯溅射伤害（不带额外、不带减速）。
     *  注意：Bukkit 实体对象每次获取可能是不同代理实例，必须用 UUID 排除命中怪本身。 */
    private void splashAround(Mob hit, double splash, Player shooter) {
        for (Entity entity : hit.getWorld().getNearbyEntities(hit.getLocation(), 1.0, 1.0, 1.0)) {
            if (entity.getUniqueId().equals(hit.getUniqueId())) {
                continue;
            }
            if (entity instanceof Mob mob && isPvzMonster(mob)) {
                damagePvzMonster(mob, splash, shooter);
            }
        }
    }

    /**
     * 寒冰命中结算：无缓慢 → 施加 10 秒缓慢三；已有缓慢三 → 冻结 1.8 秒 + 额外 2 伤害；
     * 正在冻结 → 重置冻结 1.8 秒（不叠加）+ 额外 4 伤害。
     */
    private void applyIceHit(Mob mob, double damage, Player shooter) {
        if (isFrozen(mob)) {
            freeze(mob);
            damagePvzMonster(mob, damage + ICE_FROZEN_BONUS, shooter);
        } else if (mob.hasPotionEffect(PotionEffectType.SLOWNESS)) {
            freeze(mob);
            damagePvzMonster(mob, damage + ICE_FREEZE_BONUS, shooter);
        } else {
            mob.addPotionEffect(new PotionEffect(
                    PotionEffectType.SLOWNESS, ICE_SLOW_DURATION, ICE_SLOW_AMPLIFIER));
            damagePvzMonster(mob, damage, shooter);
        }
    }

    private boolean isFrozen(Mob mob) {
        Long until = frozenUntil.get(mob.getUniqueId());
        return until != null && until > Bukkit.getCurrentTick();
    }

    /** 冻结怪物：关闭 AI 1.8 秒（完全停止移动/攻击）；重复冻结仅重置计时，不叠加。 */
    private void freeze(Mob mob) {
        long until = Bukkit.getCurrentTick() + ICE_FREEZE_TICKS;
        frozenUntil.put(mob.getUniqueId(), until);
        mob.setAI(false);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (mob.isValid() && until == frozenUntil.getOrDefault(mob.getUniqueId(), -1L)) {
                mob.setAI(true);
                frozenUntil.remove(mob.getUniqueId());
            }
        }, ICE_FREEZE_TICKS);
    }

    /** 右键发射器武器入口（机枪射手/寒冰射手/狙击豌豆/双发射手，统一右键）。 */
    public void fireShooter(Player player) {
        if (!isPlaying(player)) {
            return;
        }
        ItemStack held = player.getInventory().getItemInMainHand();
        String kind = held.getItemMeta() != null
                ? held.getItemMeta().getPersistentDataContainer()
                        .get(shooterKey, PersistentDataType.STRING)
                : null;
        if (kind == null) {
            return;
        }
        long now = System.currentTimeMillis();
        long cooldownMs = "sniper".equals(kind) ? SNIPER_COOLDOWN_MS : SHOOTER_COOLDOWN_MS;
        long last = shooterCooldowns.getOrDefault(player.getUniqueId(), 0L);
        long remaining = cooldownMs - (now - last);
        String display = switch (kind) {
            case "sniper" -> "狙击豌豆";
            case "dual" -> "双发射手";
            case "ice" -> "寒冰射手";
            case "shrimp" -> "小喷菇";
            case "sea" -> "海蘑菇";
            default -> "机枪射手";
        };
        if (remaining > 0) {
            long seconds = Math.max(1, (remaining + 999) / 1000);
            player.sendActionBar(ChatColor.YELLOW + "【" + display + "】冷却中，还需 " + seconds + " 秒");
            return;
        }
        shooterCooldowns.put(player.getUniqueId(), now);
        Runnable burst = () -> {
            if (!player.isOnline() || !isPlaying(player)) {
                return;
            }
            spawnBullet(player, kind);
        };
        switch (kind) {
            case "sniper" -> burst.run();
            case "dual" -> {
                for (int i = 0; i < DUAL_BURST_COUNT; i++) {
                    final int shot = i;
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        if (!player.isOnline() || !isPlaying(player)) {
                            return;
                        }
                        // 每发子弹独立 1/7 随机（可能出现两个同种弹）
                        spawnBullet(player, pickDualKind(Math.random()));
                    }, shot * DUAL_BULLET_INTERVAL_TICKS);
                }
            }
            case "shrimp" -> {
                // 黄心档位（0/40/80/120/160 → 1/2/3/4/5 发），同 tick 小散射齐射对准视线
                int count = puffTier(player.getAbsorptionAmount() * ABSORPTION_SCALE);
                for (int i = 0; i < count; i++) {
                    spawnPuffBullet(player, "shrimp", SMALL_PUFF_RANGE, 0.0, true);
                }
            }
            case "sea" -> {
                // 黄心档位（0/40/80/120 → 1/2/3/4 轮），每轮连续 2 发，轮与轮小角度错开
                int rounds = puffTier(Math.min(
                        player.getAbsorptionAmount() * ABSORPTION_SCALE, 120.0));
                for (int r = 0; r < rounds; r++) {
                    final double yawOffset = (r - (rounds - 1) / 2.0) * PUFF_SCATTER_DEG;
                    for (int b = 0; b < 2; b++) {
                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            if (!player.isOnline() || !isPlaying(player)) {
                                return;
                            }
                            spawnPuffBullet(player, "sea", SEA_MUSHROOM_RANGE, yawOffset, false);
                        }, r * 2L + b * PUFF_BURST_INTERVAL_TICKS);
                    }
                }
            }
            case "ice" -> {
                for (int i = 0; i < ICE_BURST_COUNT; i++) {
                    final int shot = i;
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        if (!player.isOnline() || !isPlaying(player)) {
                            return;
                        }
                        spawnBullet(player, "ice");
                    }, shot * DUAL_BULLET_INTERVAL_TICKS);
                }
                if (Math.random() < ICICLE_CHANCE) {
                    spawnIcicle(player);
                }
            }
            default -> {
                for (int i = 0; i < MACHINE_BURST_COUNT; i++) {
                    final int shot = i;
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        if (!player.isOnline() || !isPlaying(player)) {
                            return;
                        }
                        spawnBullet(player, "machine");
                    }, shot * DUAL_BULLET_INTERVAL_TICKS);
                }
            }
        }
        player.sendActionBar(ChatColor.GREEN + "【" + display + "】已发射！");
    }

    /** 双发射手七种子弹：1/7 等概率（纯逻辑可单测）。 */
    public static String pickDualKind(double roll) {
        if (roll < 1.0 / 7) {
            return "machine";
        }
        if (roll < 2.0 / 7) {
            return "ice";
        }
        if (roll < 3.0 / 7) {
            return "fire_small";
        }
        if (roll < 4.0 / 7) {
            return "fire_orange";
        }
        if (roll < 5.0 / 7) {
            return "fire_red";
        }
        if (roll < 6.0 / 7) {
            return "ice_fire";
        }
        return "dragon";
    }

    /** 喷菇体系子弹（小喷菇/海蘑菇共用）：紫色玻璃雪球弹，纯伤害无凋零，带射程。
     *  @param yawOffsetDeg 相对视线的固定水平偏移（海蘑菇轮间错开；小喷菇传 0）
     *  @param randomSpread 是否每发随机小散射（小喷菇档位齐射用） */
    private void spawnPuffBullet(Player player, String kind, double range,
                                 double yawOffsetDeg, boolean randomSpread) {
        Location eye = player.getEyeLocation();
        org.bukkit.util.Vector dir = player.getLocation().getDirection().clone().normalize();
        double extra = randomSpread ? (Math.random() - 0.5) * 2.0 * PUFF_SCATTER_DEG : 0.0;
        final org.bukkit.util.Vector shotDir = dir.rotateAroundY(Math.toRadians(yawOffsetDeg + extra)).normalize();
        Location origin = eye.clone();
        Snowball ball = eye.getWorld().spawn(eye, Snowball.class, s -> {
            s.setItem(new ItemStack(Material.PURPLE_STAINED_GLASS));
            s.setVelocity(shotDir.clone().multiply(SHOOTER_BULLET_SPEED));
            s.setShooter(player);
            s.setGravity(false);
            markBullet(s, kind, PUFF_DAMAGE, player);
        });
        trackPuffRange(ball, origin, range);
    }

    /** 子弹射程：每 tick 检查，超程直接移除（命中结算因此天然失效）。 */
    private void trackPuffRange(Snowball ball, Location origin, double range) {
        double rangeSq = range * range;
        Bukkit.getScheduler().runTaskTimer(plugin, task -> {
            if (!ball.isValid() || ball.isDead()) {
                task.cancel();
                return;
            }
            if (ball.getLocation().distanceSquared(origin) >= rangeSq) {
                ball.remove();
                task.cancel();
            }
        }, 1L, 1L);
    }

    /** 脉冲加黄心：每次 +40 虚拟黄心，不超过职业上限（小喷菇 160 / 海蘑菇 120 虚拟）。 */
    private void givePuffHearts(Player p, PvzClass clazz) {
        double cap = clazz == PvzClass.SMALL_PUFF
                ? SMALL_PUFF_ABSORPTION_CAP : SEA_MUSHROOM_ABSORPTION_CAP;
        double next = Math.min(cap, p.getAbsorptionAmount() + PUFF_HEARTS_PER_PULSE / ABSORPTION_SCALE);
        p.setAbsorptionAmount(Math.max(0.0, next));
    }

    /** 黄心虚拟值 → 发射档位（次数/轮数）：
     *  0 及以下 → 1；0–40 → 2；40–80 → 3；80–120 → 4；120–160 → 5。纯逻辑可单测。 */
    public static int puffTier(double virtualHearts) {
        if (virtualHearts <= 0.0) {
            return 1;
        }
        return (int) Math.ceil(virtualHearts / 40.0) + 1;
    }

    /** 发射 PVZ 子弹（按类型生成对应实体与模型）。 */
    private void spawnBullet(Player player, String kind) {
        Location eye = player.getEyeLocation();
        org.bukkit.util.Vector dir = player.getLocation().getDirection().clone().normalize();
        switch (kind) {
            case "fire_small" -> eye.getWorld().spawn(eye, SmallFireball.class, f -> {
                f.setDirection(dir.clone().multiply(SHOOTER_BULLET_SPEED));
                f.setVelocity(dir.clone().multiply(SHOOTER_BULLET_SPEED));
                f.setShooter(player);
                f.setYield(0.0f);
                f.setIsIncendiary(false);
                markBullet(f, kind, FIRE_SMALL_DAMAGE, player);
            });
            case "fire_orange" -> eye.getWorld().spawn(eye, LargeFireball.class, f -> {
                f.setDirection(dir.clone().multiply(SHOOTER_BULLET_SPEED));
                f.setVelocity(dir.clone().multiply(SHOOTER_BULLET_SPEED));
                f.setShooter(player);
                f.setYield(0.0f);
                f.setIsIncendiary(false);
                markBullet(f, kind, FIRE_ORANGE_DAMAGE, player);
            });
            case "dragon" -> eye.getWorld().spawn(eye, DragonFireball.class, f -> {
                f.setDirection(dir.clone().multiply(SHOOTER_BULLET_SPEED));
                f.setVelocity(dir.clone().multiply(SHOOTER_BULLET_SPEED));
                f.setShooter(player);
                markBullet(f, kind, DRAGON_DAMAGE, player);
            });
            case "fire_red" -> spawnDisplayBullet(player, Material.RED_STAINED_GLASS,
                    0.3, "fire_red", FIRE_RED_DAMAGE, 1.0, false);
            case "ice_fire" -> spawnDisplayBullet(player, Material.PURPLE_STAINED_GLASS,
                    0.3, "ice_fire", ICEFIRE_DAMAGE, 1.0, false);
            case "sniper" -> eye.getWorld().spawn(eye, Snowball.class, s -> {
                s.setItem(new ItemStack(Material.ARROW));
                s.setVelocity(dir.multiply(SNIPER_BULLET_SPEED));
                s.setShooter(player);
                s.setGravity(false);
                markBullet(s, kind, SNIPER_BULLET_DAMAGE, player);
            });
            case "ice" -> eye.getWorld().spawn(eye, Snowball.class, s -> {
                s.setItem(new ItemStack(Material.SNOWBALL));
                s.setVelocity(dir.multiply(SHOOTER_BULLET_SPEED));
                s.setShooter(player);
                s.setGravity(false);
                markBullet(s, kind, SHOOTER_BULLET_DAMAGE, player);
            });
            default -> eye.getWorld().spawn(eye, Snowball.class, s -> {
                s.setItem(new ItemStack(Material.SLIME_BALL));
                s.setVelocity(dir.multiply(SHOOTER_BULLET_SPEED));
                s.setShooter(player);
                s.setGravity(false);
                markBullet(s, kind, SHOOTER_BULLET_DAMAGE, player);
            });
        }
    }

    /** 给子弹实体打上 PVZ 子弹标记（类型/伤害/发射者）。 */
    private void markBullet(Projectile bullet, String kind, double damage, Player player) {
        bullet.getPersistentDataContainer().set(bulletKey, PersistentDataType.BYTE, (byte) 1);
        bullet.getPersistentDataContainer().set(bulletKindKey, PersistentDataType.STRING, kind);
        bullet.getPersistentDataContainer().set(bulletDamageKey, PersistentDataType.DOUBLE, damage);
        bullet.getPersistentDataContainer().set(bulletOwnerKey, PersistentDataType.STRING,
                player.getUniqueId().toString());
    }

    /** 寒冰射手 10% 概率的额外冰棱：0.5 倍浮冰方块、1.5 倍弹速、无限穿透。
     *  注意：BlockDisplay 的 velocity 不保证被服务器驱动（实测会停在原地），
     *  因此显示类子弹统一由 trackDisplayBullet 每 tick 位移推进。 */
    private void spawnIcicle(Player player) {
        spawnDisplayBullet(player, Material.PACKED_ICE, ICICLE_SCALE,
                "icicle", ICICLE_DAMAGE, ICICLE_SPEED_MULTIPLIER, true);
    }

    /** 显示类子弹（冰棱/红玻璃/紫玻璃）：BlockDisplay 造型，每 tick 位移推进。 */
    private void spawnDisplayBullet(Player player, Material block, double scale,
                                    String kind, double damage, double speedMultiplier,
                                    boolean pierce) {
        org.bukkit.util.Vector dir = player.getLocation().getDirection().clone().normalize();
        Location eye = player.getEyeLocation();
        BlockDisplay display = eye.getWorld().spawn(eye, BlockDisplay.class, d -> {
            d.setBlock(Bukkit.createBlockData(block));
            d.setTransformation(new Transformation(
                    new Vector3f(), new AxisAngle4f(),
                    new Vector3f((float) scale, (float) scale, (float) scale),
                    new AxisAngle4f()));
            d.setGravity(false);
            d.setPersistent(true);
        });
        trackDisplayBullet(display, player, dir, kind, damage, speedMultiplier, pierce);
    }

    /**
     * 显示子弹飞行跟踪：每 tick 位移推进（分 3 子步防高速漏判），命中附近怪物。
     * pierce=true 穿透（命中不消失，标记防重复伤害）；否则命中第一个即消失。
     * 5 秒或失效后消失。
     */
    private void trackDisplayBullet(BlockDisplay display, Player player, org.bukkit.util.Vector dir,
                                    String kind, double damage, double speedMultiplier,
                                    boolean pierce) {
        Set<UUID> hit = new java.util.HashSet<>();
        icicleHits.put(display.getUniqueId(), hit);
        long born = Bukkit.getCurrentTick();
        final double step = SHOOTER_BULLET_SPEED * speedMultiplier;
        Bukkit.getScheduler().runTaskTimer(plugin, task -> {
            if (!display.isValid() || display.isDead()
                    || Bukkit.getCurrentTick() - born > DISPLAY_BULLET_MAX_TICKS) {
                display.remove();
                icicleHits.remove(display.getUniqueId());
                task.cancel();
                return;
            }
            for (int s = 0; s < 3; s++) {
                display.teleport(display.getLocation().add(dir.clone().multiply(step / 3.0)));
                for (Entity entity : display.getWorld().getNearbyEntities(
                        display.getLocation(), 1.5, 1.5, 1.5)) {
                    if (!(entity instanceof Mob mob) || !isPvzMonster(mob)
                            || hit.contains(mob.getUniqueId())) {
                        continue;
                    }
                    hit.add(mob.getUniqueId());
                    resolveBulletHit(kind, mob, damage, player);
                    if (!pierce) {
                        display.remove();
                        icicleHits.remove(display.getUniqueId());
                        task.cancel();
                        return;
                    }
                }
            }
        }, 1L, 1L);
    }

    /** 带甲僵尸破甲：血量跌破阈值时摘掉帽子（路障/铁桶僵尸）；用 AIR 而非 null 确保移除，
     *  摘帽后移除二类防具标签（防具已损坏，狙击不再视其为带甲怪）。 */
    public void maybeBreakArmor(Mob mob) {
        if (!mob.getScoreboardTags().contains(MonsterManager.TAG_ARMORED)) {
            return;
        }
        if (mob.getHealth() >= com.rz.dave.monster.ConeheadZombie.ARMOR_BREAK_HP) {
            return;
        }
        if (mob.getEquipment() != null && mob.getEquipment().getHelmet() != null
                && mob.getEquipment().getHelmet().getType() != Material.AIR) {
            mob.getEquipment().setHelmet(new ItemStack(Material.AIR));
            mob.getEquipment().setHelmetDropChance(0.0f);
            mob.removeScoreboardTag(MonsterManager.TAG_ARMORED);
        }
    }

    /** 开局为射手职业的武器打上射击标记（机枪/寒冰/狙击/双发/小喷菇/海蘑菇）。 */
    private void markShooterWeapon(Player player, PvzClass clazz) {
        String kind = switch (clazz) {
            case ICE_SHOOTER -> "ice";
            case SNIPER -> "sniper";
            case DUAL_SHOOTER -> "dual";
            case SMALL_PUFF -> "shrimp";
            case SEA_MUSHROOM -> "sea";
            default -> "machine";
        };
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null) {
                continue;
            }
            Material type = item.getType();
            if (type == Material.DISPENSER || type == Material.SPYGLASS
                    || type == Material.BROWN_MUSHROOM || type == Material.WARPED_FUNGUS) {
                item.editMeta(meta -> meta.getPersistentDataContainer().set(
                        shooterKey, PersistentDataType.STRING, kind));
            }
        }
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

    /** 监听器入口：PVZ 玩家死亡 → 观察者，不复活；一路全员阵亡即淘汰。 */
    public void onPlayerDeath(Player player) {
        if (!isPlaying(player)) {
            return;
        }
        player.setGameMode(GameMode.SPECTATOR);
        player.sendMessage(ChatColor.RED + "【PVZ】你已阵亡，本局不会复活，观战中；游戏结束约 10 秒后返回大厅。");
        decrementAlive(player);
        // 防呆：若因任何边缘情况未能触发淘汰判定，确保最后一名玩家死亡时结束游戏。
        if (running && noAlivePlayersLeft()) {
            endGameInternal(null, false);
        }
    }

    /** 是否所有路都没有存活玩家（最后一名玩家死亡时的结束兜底）。 */
    private boolean noAlivePlayersLeft() {
        for (PvzLane lane : lanes.values()) {
            if (!lane.eliminated() && lane.alivePlayers() > 0) {
                return false;
            }
        }
        return true;
    }

    /** 死亡玩家的观战重生点：本路游玩场地（玩家出生点），绝不落在大厅/世界出生点。 */
    public Location respawnLocation(Player player) {
        String laneId = playerLane.get(player.getUniqueId());
        if (laneId != null) {
            PvzLane lane = lanes.get(laneId);
            if (lane != null) {
                World world = Bukkit.getWorld(lane.world());
                if (world != null) {
                    Location sp = lane.playerSpawn();
                    return new Location(world, sp.getX() + 0.5, sp.getY(), sp.getZ() + 0.5);
                }
            }
        }
        // 兜底：留在死亡点（场上）观战。
        return player.getLocation();
    }

    /** 监听器入口：PVZ 玩家中途退出 → 视同阵亡。 */
    public void onPlayerQuit(Player player) {
        if (!isPlaying(player)) {
            return;
        }
        // 中途退出视同阵亡：清掉职业装备，防止游戏物品残留到下次上线/游戏结束。
        player.getInventory().clear();
        player.getEnderChest().clear();
        pvzPlayers.remove(player.getUniqueId());
        playerClass.remove(player.getUniqueId());
        String laneId = playerLane.remove(player.getUniqueId());
        manager.setPvzPlayer(player.getUniqueId(), false);
        if (laneId != null) {
            PvzLane lane = lanes.get(laneId);
            if (lane != null) {
                lane.setAlivePlayers(lane.alivePlayers() - 1);
                if (running && lane.isActive() == false && !lane.eliminated()) {
                    eliminateLane(lane, "全员阵亡");
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
            eliminateLane(lane, "全员阵亡");
        }
    }

    /** 淘汰一条路：清怪、广播、判定剩余路并结束游戏。 */
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
        shooterCooldowns.remove(player.getUniqueId());
        player.getInventory().clear();
        player.getEnderChest().clear();
        // 停止该玩家的喷菇脉冲任务（若有）并清空黄心
        BukkitTask puffTask = puffPulseTasks.remove(player.getUniqueId());
        if (puffTask != null) {
            puffTask.cancel();
        }
        player.setAbsorptionAmount(0.0);
        // 恢复玩家常规数值：血量 20、体型 1.0（坚果 1.5 倍/喷菇缩小体型必须还原）
        player.setMaxHealth(20.0);
        player.setHealth(20.0);
        AttributeInstance scale = player.getAttribute(Attribute.SCALE);
        if (scale != null) {
            scale.setBaseValue(1.0);
        }
        player.setGameMode(GameMode.ADVENTURE);
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
                    eliminateLane(lane, "全员阵亡");
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
            sb.append(laneColor(laneId)).append(LANE_DISPLAYS.getOrDefault(laneId, laneId)).append(' ');
        }
        sb.append(ChatColor.WHITE).append(player.getName());
        if (clazz != null) {
            sb.append(ChatColor.GRAY).append(" · ").append(clazz.displayName());
        }
        player.setPlayerListName(sb.toString());
        player.setDisplayName(sb.toString());
    }

    private static ChatColor laneColor(String lane) {
        return switch (lane) {
            case "one" -> ChatColor.WHITE;
            case "two" -> ChatColor.GOLD;
            case "three" -> ChatColor.AQUA;
            case "four" -> ChatColor.LIGHT_PURPLE;
            case "five" -> ChatColor.DARK_AQUA;
            default -> ChatColor.GRAY;
        };
    }

    private void buildSidebar() {
        pvzBoard = Bukkit.getScoreboardManager().getNewScoreboard();
        sidebar = pvzBoard.registerNewObjective("pvz_mode", "dummy", "随机植物对战随机僵尸");
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
                line = laneColor(lane.id()) + lane.display() + " " + lane.baseHealth() + "/"
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

    private static BarColor barColor(String lane) {
        return switch (lane) {
            case "one" -> BarColor.WHITE;
            case "two" -> BarColor.YELLOW;
            case "three" -> BarColor.BLUE;
            case "four" -> BarColor.PINK;
            case "five" -> BarColor.PURPLE;
            default -> BarColor.WHITE;
        };
    }
}
