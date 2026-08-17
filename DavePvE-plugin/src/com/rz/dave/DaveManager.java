package com.rz.dave;

import com.google.common.collect.Multimap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Blaze;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.DragonFireball;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Item;
import org.bukkit.entity.LightningStrike;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.Slime;
import org.bukkit.entity.Snowball;
import org.bukkit.entity.SpectralArrow;
import org.bukkit.entity.Stray;
import org.bukkit.entity.Trident;
import org.bukkit.entity.Villager;
import org.bukkit.entity.Warden;
import org.bukkit.entity.WindCharge;
import org.bukkit.entity.Witch;
import org.bukkit.entity.Wither;
import org.bukkit.entity.WitherSkull;
import org.bukkit.entity.Wolf;
import org.bukkit.entity.Zombie;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Transformation;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Team;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class DaveManager {
    private static final long PENDING_TTL_MS = 5000L;
    private static final long RETARGET_INTERVAL = 200L;
    private static final long REFRESH_INTERVAL = 10L;
    private static final double MAX_HEALTH = 100.0;
    private static final double FOLLOW_RANGE = 64.0;
    private static final double PENDING_NEAR_DISTANCE = 64.0;
    private static final double BOSS_BAR_RADIUS = 25.0;
    private static final long REST_INTERVAL_TICKS = 3600L;
    private static final long REST_DURATION_TICKS = 600L;
    private static final long REST_INTERVAL_MILLIS = 180_000L;
    private static final long WIND_CHARGE_COOLDOWN_MS = 3_000L;
    private static final int SPAWN_INTERVAL_START_TICKS = 500;
    private static final int SPAWN_INTERVAL_MIN_TICKS = 60;
    private static final int SPAWN_INTERVAL_STEP_TICKS = 20;
    private static final int SPAWN_ACCELERATE_SECONDS = 60;
    private static final double SPAWN_POINT_DELETE_RADIUS = 4.0;
    private static final double GIANT_CHANCE = 0.005;
    private static final MonsterEntry GIANT_ENTRY = new MonsterEntry("giant", "巨人僵尸", 17, 1, false);
    private static final double BLACK_FOOTBALL_CHANCE = 0.005;
    private static final MonsterEntry BLACK_FOOTBALL_ENTRY = new MonsterEntry("black_football_skeleton", "黑橄榄球骷髅", 14, 1, false);
    private static final int MAX_PARTICIPANTS = 20;
    private static final int WAVE_GIANT_BOSS_INDEX = 3;
    private static final int WAVE_NORMAL = 0;
    private static final int WAVE_MINIBOSS = 1;
    private static final int WAVE_BIGBOSS = 2;
    private static final int[] WAVE_SEQUENCE = {
            WAVE_NORMAL, WAVE_NORMAL, WAVE_NORMAL, WAVE_MINIBOSS,
            WAVE_NORMAL, WAVE_NORMAL, WAVE_NORMAL, WAVE_MINIBOSS,
            WAVE_NORMAL, WAVE_NORMAL, WAVE_NORMAL, WAVE_BIGBOSS
    };

    private record MonsterEntry(String key, String displayName, int id, int weight, boolean migrated) {
    }

    private static final List<MonsterEntry> MONSTER_POOL = List.of(
            new MonsterEntry("normal_zombie", "普通僵尸", 1, 8, true),
            new MonsterEntry("iron_armor_zombie", "铁甲僵尸", 2, 8, true),
            new MonsterEntry("small_zombie", "小僵尸", 3, 8, true),
            new MonsterEntry("drowned", "溺尸", 4, 8, false),
            new MonsterEntry("creeper", "苦力怕", 5, 8, true),
            new MonsterEntry("skeleton", "骷髅", 6, 8, true),
            new MonsterEntry("stray", "流浪者", 7, 4, true),
            new MonsterEntry("time_creeper", "定时苦力怕", 8, 4, false),
            new MonsterEntry("lf_dispenser_zombie", "低频发射器僵尸", 9, 1, false),
            new MonsterEntry("mf_dispenser_zombie", "中频发射器僵尸", 10, 1, false),
            new MonsterEntry("hf_dispenser_zombie", "高频发射器僵尸", 11, 1, false),
            new MonsterEntry("newspaper_zombie", "读报僵尸", 12, 1, false),
            new MonsterEntry("football_skeleton", "橄榄球骷髅", 13, 8, false),
            new MonsterEntry("dancing_zombie", "舞王僵尸", 15, 4, false),
            new MonsterEntry("backup_dancer_skeleton", "伴舞骷髅", 16, 4, false),
            new MonsterEntry("blaze", "气球烈焰人", 18, 4, true),
            new MonsterEntry("large_slime", "大型破碎者跳跳", 21, 1, true),
            new MonsterEntry("spider", "相位蜘蛛", 22, 1, false),
            new MonsterEntry("large_magma_cube", "大型岩浆怪", 25, 1, false),
            new MonsterEntry("spider_queen", "蜘蛛女王", 26, 1, false),
            new MonsterEntry("sea_drowned", "海洋使徒", 32, 1, false),
            new MonsterEntry("witch", "女巫", 33, 1, true));

    private static final Map<String, String> MYTHIC_MOB_NAMES = Map.of(
            "normal_zombie", "RZNormalZombie",
            "iron_armor_zombie", "RZIronArmorZombie",
            "small_zombie", "RZSmallZombie",
            "skeleton", "RZSkeleton",
            "stray", "RZStray",
            "creeper", "RZCreeper",
            "blaze", "RZBlaze",
            "large_slime", "RZLargeSlime",
            "witch", "RZWitch");
    private static final List<PotionEffectType> STEW_BUFFS = List.of(
            PotionEffectType.STRENGTH, PotionEffectType.SPEED, PotionEffectType.HASTE,
            PotionEffectType.RESISTANCE, PotionEffectType.JUMP_BOOST, PotionEffectType.REGENERATION,
            PotionEffectType.FIRE_RESISTANCE);
    private static final List<PotionEffectType> STEW_DEBUFFS = List.of(
            PotionEffectType.WEAKNESS, PotionEffectType.SLOWNESS, PotionEffectType.MINING_FATIGUE,
            PotionEffectType.HUNGER, PotionEffectType.POISON, PotionEffectType.NAUSEA,
            PotionEffectType.BLINDNESS);
    public static final List<String> TEAM_IDS = List.of("red", "blue", "yellow", "green");
    public static final Map<String, String> TEAM_DISPLAYS = Map.of(
            "red", "红队",
            "blue", "蓝队",
            "yellow", "黄队",
            "green", "绿队");

    private final Plugin plugin;
    private final NamespacedKey ownerTeamKey;
    private static NamespacedKey oneShotAxeKey;
    private static NamespacedKey windMaceKey;
    private static NamespacedKey shopBowKey;
    private static NamespacedKey shopBowArrowKey;
    private static NamespacedKey shooterTierKey;
    private static NamespacedKey shooterProjectileKey;
    private static NamespacedKey shooterBranchKey;
    private static NamespacedKey shooterDamageKey;
    private static NamespacedKey shooterSlowKey;
    private static NamespacedKey shooterFireKey;
    private static NamespacedKey shooterDragonKey;
    private static NamespacedKey shooterIceBlockKey;
    private static NamespacedKey shooterIdKey;
    private static NamespacedKey cherryBombKey;
    private static NamespacedKey destroyShroomKey;
    private static NamespacedKey nutKey;
    private static NamespacedKey bigNutKey;
    private static NamespacedKey cactusShooterKey;
    private static NamespacedKey cactusDamageKey;
    private static NamespacedKey cactusSkillArrowKey;
    private static NamespacedKey cactusPierceHitKey;
    private static NamespacedKey sniperBulletKey;
    private static NamespacedKey sniperExplodeKey;
    private static NamespacedKey axeBulletKey;
    private static NamespacedKey weaponDamageKey;
    private static NamespacedKey rangedBonusKey;
    private static NamespacedKey skillArrowKey;
    private static NamespacedKey explosiveArrowKey;
    private static NamespacedKey pluginLightningKey;
    private static NamespacedKey iceShroomKey;
    private static NamespacedKey bigPuffKey;
    private static NamespacedKey bigPuffDamageKey;
    private static NamespacedKey smallPuffKey;
    private static NamespacedKey smallPuffDamageKey;
    private static NamespacedKey smallPuffBulletKey;
    private static NamespacedKey smallPuffWitherAmpKey;
    private static NamespacedKey timidKey;
    private static NamespacedKey timidDamageKey;
    private final NamespacedKey menuClockKey;
    private final NamespacedKey menuRedstoneKey;
    private final NamespacedKey shopReturnKey;
    private final NamespacedKey blindBoxOpenedKey;
    private final NamespacedKey spectatorExitKey;
    private final NamespacedKey waveBaseHpKey;
    private final NamespacedKey waveBaseAtkKey;
    private final NamespacedKey waveBaseSpdKey;
    private final NamespacedKey waveBuffMultKey;
    private final NamespacedKey waveSpeedMultKey;
    private final NamespacedKey waveMeleeBoostKey;
    private final Map<UUID, BossBar> bars = new HashMap<>();
    private final Map<UUID, BossBar> bossBars = new HashMap<>();
    private final Map<UUID, Long> pendingCreates = new HashMap<>();
    private final Map<UUID, Long> windCooldowns = new HashMap<>();
    private final Map<String, Long> skillCooldowns = new HashMap<>();
    private final Map<UUID, Long> bowExplosiveExpiry = new HashMap<>();
    private final Map<UUID, Double> bowExplosiveCenterDamage = new HashMap<>();
    private final Map<UUID, Double> bowExplosiveRadius = new HashMap<>();
    private final Map<UUID, Set<UUID>> crossbowSkillArrows = new HashMap<>();
    private final Map<UUID, Long> cactusCooldowns = new HashMap<>();
    private final Map<UUID, Long> cactusSkillCooldowns = new HashMap<>();
    private final Map<UUID, Boolean> cactusSkillNext = new HashMap<>();
    private final Map<UUID, Set<UUID>> cactusHitMonsters = new HashMap<>();
    private final Map<UUID, org.bukkit.util.Vector> ridingInput = new HashMap<>();
    private final Map<String, Long> shooterCooldowns = new HashMap<>();
    private final Map<UUID, Long> cherryCooldowns = new HashMap<>();
    private final Map<UUID, Long> destroyCooldowns = new HashMap<>();
    private final Map<UUID, Long> iceShroomCooldowns = new HashMap<>();
    private final Map<UUID, Long> bigPuffCooldowns = new HashMap<>();
    private final Map<UUID, Long> smallPuffCooldowns = new HashMap<>();
    private final Map<UUID, Long> daveHealCooldowns = new HashMap<>();
    private final Map<UUID, Long> timidLastFire = new HashMap<>();
    private final Map<UUID, Integer> timidConsecutive = new HashMap<>();
    private final Map<UUID, Long> timidCurrentCooldown = new HashMap<>();
    private final Map<UUID, BukkitTask> timidResetTasks = new HashMap<>();
    private final Map<UUID, Long> meleeReachLastAttack = new HashMap<>();
    private final Map<UUID, Long> lastDaveHit = new HashMap<>();
    private final Map<UUID, UUID> playerWolves = new HashMap<>();
    private final Map<UUID, Long> wolfRespawns = new HashMap<>();
    private final Map<UUID, int[]> wolfBuffs = new HashMap<>();
    private final Map<UUID, UUID> wolfKills = new HashMap<>();
    private final Map<UUID, UUID> bombKills = new HashMap<>();
    private final Map<UUID, Map<UUID, Double>> damageLogs = new HashMap<>();
    private final Map<UUID, Double> playerScores = new HashMap<>();
    private final Map<UUID, Integer> playerKills = new HashMap<>();
    private final Map<UUID, BossAttacker> bossAttackers = new HashMap<>();
    private final Set<UUID> readyPlayers = new LinkedHashSet<>();
    private final Map<UUID, String> teamPreferences = new HashMap<>();
    private final Map<UUID, Boolean> modeVotes = new HashMap<>();
    private final Map<UUID, Integer> economyVotes = new HashMap<>();
    private final Set<UUID> waitingRespawn = new HashSet<>();
    private final Map<String, TeamDef> teamDefs = new LinkedHashMap<>();
    private TeamChestManager chestManager;
    private int teamChestSize = 27;
    private int cleanupRadius = 32;
    private int spawnPointTeamRadius = 48;
    private String lobbyWorld = "world";
    private int lobbyX;
    private int lobbyY = 80;
    private int lobbyZ;
    private long autoStartCountdown;
    private int autoStartBracket;
    private int autoStartReadyCount;
    private final Set<UUID> participants = new HashSet<>();
    private final Set<UUID> gameParticipantSet = new HashSet<>();
    private final Set<UUID> pvzPlayers = new HashSet<>();
    private PvzMode pvzMode;
    private boolean gameRunning;
    private boolean pvpEnabled;
    boolean deathMode;
    private int deathRestDiamondTier = 30;
    private final java.util.Set<UUID> gameStartParticipants = new java.util.HashSet<>();
    private final Map<UUID, Long> windMaceFallImmunity = new HashMap<>();
    private boolean openingRest;
    private boolean restActive;
    private long restEndTick;
    private long activeGameTicks;
    private long phaseStartMillis;
    private int teamsAtGameStart;
    private BossBar lobbyBar;
    private BossBar autoStartBar;
    private BukkitTask retargetTask;
    private BukkitTask refreshTask;
    private BukkitTask lobbyTask;
    private BukkitTask gameTask;
    private BukkitTask currencyTask;
    private BukkitTask spawnTask;
    private BukkitTask lobbyBuffTask;
    private BukkitTask brewingFuelTask;
    private BukkitTask groundCleanupTask;
    private BukkitTask groundItemCleanupTask;
    private BukkitTask wolfRespawnTask;
    private BukkitTask shooterHealTask;
    private BukkitTask wolfRideTask;
    private boolean spawningEnabled;
    private int spawnSeconds;
    private int spawnCounterTicks;
    private int spawnIntervalStartTicks = SPAWN_INTERVAL_START_TICKS;
    private int waveIndex;
    private BrewingStandManager brewingManager;
    private String brewingWorld = "world";
    private int brewingX = 10000;
    private int brewingY = 1;
    private int brewingZ = 10000;
    private final Map<String, Map<PotionEffectType, Integer>> teamBuffs = new HashMap<>();
    private final Set<String> daveSlowAuraTeams = new HashSet<>();
    private final Map<String, Map<PotionEffectType, Integer>> daveBuffs = new HashMap<>();
    private final Set<UUID> wardenSlamActive = new HashSet<>();
    private double waveBuffMultiplier = 1.0;
    private double waveSpeedMultiplier = 1.0;
    private UUID gameId;
    private final Map<UUID, DisconnectSnapshot> disconnectSnapshots = new HashMap<>();
    private Scoreboard gameScoreboard;
    private Objective gameSidebar;
    private long gameStartMillis;

    public record TeamDef(String id, String display, String world, int x, int y, int z,
                          int playX, int playY, int playZ) {
    }

    private record DisconnectSnapshot(UUID gameId, long disconnectedAt, ItemStack[] inventory,
                                      ItemStack[] enderChest, int totalExperience, int level,
                                      float expProgress, List<PotionEffect> effects) {
    }

    private record BossAttacker(UUID attackerId, long timestamp) {
    }

    public DaveManager(Plugin plugin) {
        this.plugin = plugin;
        this.ownerTeamKey = new NamespacedKey(plugin, "owner_team");
        DaveManager.oneShotAxeKey = new NamespacedKey(plugin, "one_shot_axe");
        DaveManager.windMaceKey = new NamespacedKey(plugin, "wind_mace");
        DaveManager.shopBowKey = new NamespacedKey(plugin, "shop_bow");
        DaveManager.shopBowArrowKey = new NamespacedKey(plugin, "shop_bow_arrow");
        DaveManager.shooterTierKey = new NamespacedKey(plugin, "shooter_tier");
        DaveManager.shooterProjectileKey = new NamespacedKey(plugin, "shooter_projectile");
        DaveManager.shooterBranchKey = new NamespacedKey(plugin, "shooter_branch");
        DaveManager.shooterDamageKey = new NamespacedKey(plugin, "shooter_damage");
        DaveManager.shooterSlowKey = new NamespacedKey(plugin, "shooter_slow");
        DaveManager.shooterFireKey = new NamespacedKey(plugin, "shooter_fire");
        DaveManager.shooterDragonKey = new NamespacedKey(plugin, "shooter_dragon");
        DaveManager.shooterIceBlockKey = new NamespacedKey(plugin, "shooter_ice_block");
        DaveManager.shooterIdKey = new NamespacedKey(plugin, "shooter_id");
        DaveManager.cherryBombKey = new NamespacedKey(plugin, "cherry_bomb");
        DaveManager.destroyShroomKey = new NamespacedKey(plugin, "destroy_shroom");
        DaveManager.nutKey = new NamespacedKey(plugin, "nut");
        DaveManager.bigNutKey = new NamespacedKey(plugin, "big_nut");
        DaveManager.cactusShooterKey = new NamespacedKey(plugin, "cactus_shooter");
        DaveManager.cactusDamageKey = new NamespacedKey(plugin, "cactus_damage");
        DaveManager.cactusSkillArrowKey = new NamespacedKey(plugin, "cactus_skill_arrow");
        DaveManager.cactusPierceHitKey = new NamespacedKey(plugin, "cactus_pierce_hit");
        DaveManager.sniperBulletKey = new NamespacedKey(plugin, "sniper_bullet");
        DaveManager.sniperExplodeKey = new NamespacedKey(plugin, "sniper_explode");
        DaveManager.axeBulletKey = new NamespacedKey(plugin, "axe_bullet");
        DaveManager.weaponDamageKey = new NamespacedKey(plugin, "weapon_damage");
        DaveManager.rangedBonusKey = new NamespacedKey(plugin, "ranged_bonus");
        DaveManager.skillArrowKey = new NamespacedKey(plugin, "skill_arrow");
        DaveManager.explosiveArrowKey = new NamespacedKey(plugin, "explosive_arrow");
        DaveManager.pluginLightningKey = new NamespacedKey(plugin, "plugin_lightning");
        DaveManager.iceShroomKey = new NamespacedKey(plugin, "ice_shroom");
        DaveManager.bigPuffKey = new NamespacedKey(plugin, "big_puff");
        DaveManager.bigPuffDamageKey = new NamespacedKey(plugin, "big_puff_damage");
        DaveManager.smallPuffKey = new NamespacedKey(plugin, "small_puff");
        DaveManager.smallPuffDamageKey = new NamespacedKey(plugin, "small_puff_damage");
        DaveManager.smallPuffBulletKey = new NamespacedKey(plugin, "small_puff_bullet");
        DaveManager.smallPuffWitherAmpKey = new NamespacedKey(plugin, "small_puff_wither_amp");
        DaveManager.timidKey = new NamespacedKey(plugin, "timid_shroom");
        DaveManager.timidDamageKey = new NamespacedKey(plugin, "timid_damage");
        this.menuClockKey = new NamespacedKey(plugin, "menu_clock");
        this.menuRedstoneKey = new NamespacedKey(plugin, "menu_redstone");
        this.shopReturnKey = new NamespacedKey(plugin, "shop_return");
        this.blindBoxOpenedKey = new NamespacedKey(plugin, "blind_box_opened");
        this.spectatorExitKey = new NamespacedKey(plugin, "spectator_exit");
        this.waveBaseHpKey = new NamespacedKey(plugin, "wave_base_hp");
        this.waveBaseAtkKey = new NamespacedKey(plugin, "wave_base_atk");
        this.waveBaseSpdKey = new NamespacedKey(plugin, "wave_base_spd");
        this.waveBuffMultKey = new NamespacedKey(plugin, "wave_buff_mult");
        this.waveSpeedMultKey = new NamespacedKey(plugin, "wave_speed_mult");
        this.waveMeleeBoostKey = new NamespacedKey(plugin, "wave_melee_boost");
    }

    public static NamespacedKey oneShotAxeKey() {
        return oneShotAxeKey;
    }

    public static boolean isOneShotAxe(ItemStack item) {
        if (item == null || item.getType() != Material.GOLDEN_AXE) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.getPersistentDataContainer().has(oneShotAxeKey, PersistentDataType.BYTE);
    }

    public static NamespacedKey windMaceKey() {
        return windMaceKey;
    }

    public static NamespacedKey axeBulletKey() {
        return axeBulletKey;
    }

    public static NamespacedKey iceShroomKey() {
        return iceShroomKey;
    }

    public static NamespacedKey bigPuffKey() {
        return bigPuffKey;
    }

    public static NamespacedKey bigPuffDamageKey() {
        return bigPuffDamageKey;
    }

    public static NamespacedKey smallPuffKey() {
        return smallPuffKey;
    }

    public static NamespacedKey smallPuffDamageKey() {
        return smallPuffDamageKey;
    }

    public static NamespacedKey smallPuffBulletKey() {
        return smallPuffBulletKey;
    }

    public static NamespacedKey smallPuffWitherAmpKey() {
        return smallPuffWitherAmpKey;
    }

    public static NamespacedKey timidKey() {
        return timidKey;
    }

    public static NamespacedKey timidDamageKey() {
        return timidDamageKey;
    }

    public static NamespacedKey shopBowKey() {
        return shopBowKey;
    }

    public static NamespacedKey shopBowArrowKey() {
        return shopBowArrowKey;
    }

    public boolean isShopBow(ItemStack item) {
        return item != null && item.getItemMeta() != null
                && item.getItemMeta().getPersistentDataContainer().has(shopBowKey, PersistentDataType.BYTE);
    }

    public static boolean isWindMace(ItemStack item) {
        if (item == null || item.getType() != Material.MACE) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.getPersistentDataContainer().has(windMaceKey, PersistentDataType.BYTE);
    }

    public void fireWindCharge(Player player) {
        long now = System.currentTimeMillis();
        long last = windCooldowns.getOrDefault(player.getUniqueId(), 0L);
        long remaining = WIND_CHARGE_COOLDOWN_MS - (now - last);
        if (remaining > 0) {
            long seconds = (remaining + 999) / 1000;
            player.sendActionBar(ChatColor.GOLD + "【风暴重锤】冷却中，还需 " + seconds + " 秒");
            return;
        }
        WindCharge windCharge = player.launchProjectile(
                WindCharge.class, player.getEyeLocation().getDirection().multiply(1.5));
        if (windCharge != null) {
            windCharge.getPersistentDataContainer().set(windMaceKey, PersistentDataType.BYTE, (byte) 1);
        }
        windCooldowns.put(player.getUniqueId(), now);
        player.sendActionBar(ChatColor.GOLD + "【风暴重锤】已发射风弹！");
    }

    public void applyWindChargeLaunch(Player shooter, Location hit) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!shooter.isOnline() || !shooter.isValid()) {
                return;
            }
            double dist = shooter.getLocation().distance(hit);
            if (dist > 4.0) {
                return;
            }
            double minY = 1.15 * (1.0 - dist / 4.0);
            org.bukkit.util.Vector vel = shooter.getVelocity();
            if (vel.getY() < minY) {
                shooter.setVelocity(vel.setY(minY));
            }
        }, 1L);
    }

    public static NamespacedKey shooterTierKey() {
        return shooterTierKey;
    }

    public static NamespacedKey shooterProjectileKey() {
        return shooterProjectileKey;
    }

    public static NamespacedKey shooterDamageKey() {
        return shooterDamageKey;
    }

    public static NamespacedKey shooterSlowKey() {
        return shooterSlowKey;
    }

    public static NamespacedKey shooterFireKey() {
        return shooterFireKey;
    }

    public static NamespacedKey shooterDragonKey() {
        return shooterDragonKey;
    }

    public static NamespacedKey shooterIceBlockKey() {
        return shooterIceBlockKey;
    }

    public static NamespacedKey nutKey() {
        return nutKey;
    }

    public static NamespacedKey bigNutKey() {
        return bigNutKey;
    }

    public static NamespacedKey cactusShooterKey() {
        return cactusShooterKey;
    }

    public static NamespacedKey cactusDamageKey() {
        return cactusDamageKey;
    }

    public static NamespacedKey cactusSkillArrowKey() {
        return cactusSkillArrowKey;
    }

    public static NamespacedKey cactusPierceHitKey() {
        return cactusPierceHitKey;
    }

    public static NamespacedKey sniperBulletKey() {
        return sniperBulletKey;
    }

    public static NamespacedKey sniperExplodeKey() {
        return sniperExplodeKey;
    }

    public static NamespacedKey cherryBombKey() {
        return cherryBombKey;
    }

    public static NamespacedKey destroyShroomKey() {
        return destroyShroomKey;
    }

    public static NamespacedKey weaponDamageKey() {
        return weaponDamageKey;
    }

    public static NamespacedKey rangedBonusKey() {
        return rangedBonusKey;
    }

    public static NamespacedKey skillArrowKey() {
        return skillArrowKey;
    }

    public static NamespacedKey explosiveArrowKey() {
        return explosiveArrowKey;
    }

    public int shooterTier(ItemStack item) {
        if (item == null || item.getItemMeta() == null) {
            return -1;
        }
        if (item.getType() != Material.DISPENSER && item.getType() != Material.SPYGLASS) {
            return -1;
        }
        return item.getItemMeta().getPersistentDataContainer()
                .getOrDefault(shooterTierKey, PersistentDataType.INTEGER, -1);
    }

    public ItemStack createShooterItem(int tier) {
        return createShooterItem(BRANCH_NONE, tier);
    }

    public int shooterBranch(ItemStack item) {
        if (item == null || item.getItemMeta() == null) {
            return BRANCH_NONE;
        }
        if (item.getType() != Material.DISPENSER && item.getType() != Material.SPYGLASS) {
            return BRANCH_NONE;
        }
        return item.getItemMeta().getPersistentDataContainer()
                .getOrDefault(shooterBranchKey, PersistentDataType.INTEGER, BRANCH_NONE);
    }

    public ItemStack createShooterItem(int branch, int tier) {
        Material base = branch == BRANCH_SNIPER ? Material.SPYGLASS : Material.DISPENSER;
        ItemStack stack = new ItemStack(base);
        ItemMeta meta = stack.getItemMeta();
        meta.setItemName(shooterName(branch, tier));
        String bullet = branch == BRANCH_SNIPER ? "高速子弹"
                : branch == BRANCH_FIRE ? "烈焰人小火球，命中怪物造成 10 点伤害"
                : branch == BRANCH_ICE ? "雪球，命中怪物造成 5 点伤害并缓慢 II"
                : "史莱姆球子弹，命中怪物造成 5 点伤害";
        meta.setLore(List.of(
                ChatColor.GRAY + "主手右键发射" + bullet,
                ChatColor.GRAY + "连发数：" + shooterCount(branch, tier) + "，冷却 1 秒"));
        meta.getPersistentDataContainer().set(shooterTierKey, PersistentDataType.INTEGER, tier);
        meta.getPersistentDataContainer().set(shooterBranchKey, PersistentDataType.INTEGER, branch);
        meta.getPersistentDataContainer().set(shooterIdKey, PersistentDataType.STRING, UUID.randomUUID().toString());
        stack.setItemMeta(meta);
        return stack;
    }

    public static final int BRANCH_NONE = -1;
    public static final int BRANCH_FIRE = 0;
    public static final int BRANCH_NORMAL = 1;
    public static final int BRANCH_ICE = 2;
    public static final int BRANCH_SNIPER = 3;

    private static String shooterName(int branch, int tier) {
        if (branch == BRANCH_NONE) {
            return "豌豆射手";
        }
        if (branch == BRANCH_SNIPER) {
            return tier >= 3 ? "爆炸狙击豌豆射手" : tier == 2 ? "快速狙击豌豆射手" : "狙击豌豆射手";
        }
        if (branch == BRANCH_FIRE) {
            return tier >= 4 ? "机炎灯" : tier >= 3 ? "火机枪射手" : tier == 2 ? "火双发射手" : "火豌豆射手";
        }
        if (branch == BRANCH_ICE) {
            return tier >= 4 ? "机炎冰灯" : tier >= 3 ? "冰机枪射手" : tier == 2 ? "冰双发射手" : "冰豌射手";
        }
        return tier >= 4 ? "真正的机枪射手" : tier == 3 ? "机机射手" : tier == 2 ? "机枪射手" : "双发射手";
    }

    private static int shooterCount(int branch, int tier) {
        if (branch == BRANCH_NORMAL) {
            return tier >= 3 ? 8 : tier == 2 ? 4 : 2;
        }
        if (tier >= 3) {
            return 4;
        }
        if (tier == 2) {
            return 2;
        }
        return 1;
    }

    private static int maxShooterTier(int branch) {
        return branch == BRANCH_NORMAL ? 4 : branch == BRANCH_SNIPER ? 3 : 4;
    }

    private static int shooterPrice(int branch, int tier) {
        if (branch == BRANCH_SNIPER) {
            return tier == 1 ? 70 : tier == 2 ? 150 : 1000;
        }
        if (branch == BRANCH_FIRE) {
            return tier == 1 ? 50 : tier == 2 ? 150 : tier == 3 ? 300 : 1000;
        }
        if (branch == BRANCH_ICE) {
            return tier == 1 ? 50 : tier == 2 ? 100 : tier == 3 ? 150 : 1000;
        }
        return tier == 1 ? 50 : tier == 2 ? 100 : tier == 3 ? 200 : 700;
    }

    private static int shooterBranchForSlot(int slot) {
        if (slot == EquipmentCatalog.SHOOTER_SNIPER_SLOT) {
            return BRANCH_SNIPER;
        }
        if (slot == EquipmentCatalog.SHOOTER_FIRE_SLOT) {
            return BRANCH_FIRE;
        }
        if (slot == EquipmentCatalog.SHOOTER_NORMAL_SLOT) {
            return BRANCH_NORMAL;
        }
        return BRANCH_ICE;
    }

    public void fireShooter(Player player) {
        ItemStack held = player.getInventory().getItemInMainHand();
        int branch = shooterBranch(held);
        int tier = shooterTier(held);
        if (tier < 0) {
            return;
        }
        long now = System.currentTimeMillis();
        String cooldownKey = "shooter:" + player.getUniqueId();
        long last = shooterCooldowns.getOrDefault(cooldownKey, 0L);
        long remaining = 1000L - (now - last);
        if (remaining > 0) {
            long seconds = (remaining + 999) / 1000;
            player.sendActionBar(ChatColor.YELLOW + "【射手】冷却中，还需 " + seconds + " 秒");
            return;
        }
        shooterCooldowns.put(cooldownKey, now);
        int count = shooterCount(branch, tier);
        boolean dragon = branch == BRANCH_FIRE && tier >= 4;
        boolean iceBlock = branch == BRANCH_ICE && tier >= 4;
        double damage = dragon ? 20.0 : iceBlock ? 10.0 : branch == BRANCH_FIRE ? 10.0 : 5.0;
        boolean slow = branch == BRANCH_ICE;
        boolean fire = branch == BRANCH_FIRE;
        boolean twoRows = branch == BRANCH_NORMAL && tier >= 3;
        Material bulletItem = dragon ? Material.FIRE_CHARGE
                : iceBlock ? Material.ICE
                : branch == BRANCH_FIRE ? Material.FIRE_CHARGE
                : branch == BRANCH_ICE ? Material.SNOWBALL : Material.SLIME_BALL;
        for (int i = 0; i < count; i++) {
            final int shot = i;
            final double offset;
            final long delay;
            if (twoRows) {
                offset = shot < count / 2 ? 0.45 : -0.45;
                delay = (shot % (count / 2)) * 4L;
            } else {
                offset = 0.0;
                delay = shot * 4L;
            }
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!player.isOnline()) {
                    return;
                }
                shootShooterBullet(player, bulletItem, damage, slow, fire, dragon, iceBlock, offset);
            }, delay);
        }
        if (branch == BRANCH_NORMAL && tier >= 4 && Math.random() < 0.1) {
            startNormalBurst(player);
        }
        player.sendActionBar(ChatColor.GREEN + "【射手】已发射！");
    }

    private void startNormalBurst(Player player) {
        player.sendActionBar(ChatColor.GOLD + "【真正的机枪射手】爆发！");
        for (int tick = 0; tick < 40; tick++) {
            final int t = tick;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!player.isOnline()) {
                    return;
                }
                for (int k = 0; k < 2; k++) {
                    shootScatterBullet(player);
                }
            }, t);
        }
    }

    private void shootScatterBullet(Player player) {
        Location eye = player.getEyeLocation();
        org.bukkit.util.Vector dir = player.getLocation().getDirection().clone().normalize()
                .add(new org.bukkit.util.Vector(
                        (Math.random() - 0.5) * 0.8,
                        (Math.random() - 0.5) * 0.3,
                        (Math.random() - 0.5) * 0.8))
                .normalize();
        Snowball ball = eye.getWorld().spawn(eye, Snowball.class, s -> {
            s.setItem(new ItemStack(Material.SLIME_BALL));
            s.setVelocity(dir.multiply(1.8));
            s.setShooter(player);
            s.setGravity(false);
            s.getPersistentDataContainer().set(shooterProjectileKey, PersistentDataType.BYTE, (byte) 1);
            s.getPersistentDataContainer().set(shooterDamageKey, PersistentDataType.DOUBLE, 5.0);
        });
    }

    public void fireSniperShooter(Player player) {
        ItemStack held = player.getInventory().getItemInMainHand();
        int branch = shooterBranch(held);
        int tier = shooterTier(held);
        if (branch != BRANCH_SNIPER || tier < 1) {
            return;
        }
        long now = System.currentTimeMillis();
        long cooldownMs = tier >= 3 ? 1500L : tier == 2 ? 2000L : 3000L;
        String key = "shooter:" + player.getUniqueId();
        long last = shooterCooldowns.getOrDefault(key, 0L);
        long remaining = cooldownMs - (now - last);
        if (remaining > 0) {
            player.sendActionBar(ChatColor.YELLOW + "【狙击】冷却中，还需 "
                    + Math.max(1, (remaining + 999) / 1000) + " 秒");
            return;
        }
        shooterCooldowns.put(key, now);
        double damage = tier >= 3 ? 70.0 : tier == 2 ? 55.0 : 35.0;
        boolean explode = tier >= 3;
        Location eye = player.getEyeLocation();
        org.bukkit.util.Vector dir = player.getLocation().getDirection().clone().normalize();
        Snowball bullet = eye.getWorld().spawn(eye, Snowball.class, s -> {
            s.setItem(new ItemStack(Material.ARROW));
            s.setVelocity(dir.multiply(15.0));
            s.setShooter(player);
            s.setGravity(false);
            s.getPersistentDataContainer().set(sniperBulletKey, PersistentDataType.BYTE, (byte) 1);
            s.getPersistentDataContainer().set(shooterDamageKey, PersistentDataType.DOUBLE, damage);
            if (explode) {
                s.getPersistentDataContainer().set(sniperExplodeKey, PersistentDataType.BYTE, (byte) 1);
            }
        });
        player.sendActionBar(ChatColor.GREEN + "【狙击】已发射！");
    }

    private void shootShooterBullet(Player player, Material bulletItem, double damage,
                                    boolean slow, boolean fire, boolean dragon, boolean iceBlock,
                                    double lateralOffset) {
        Location eye = player.getEyeLocation();
        org.bukkit.util.Vector dir = player.getLocation().getDirection().clone();
        Location spawnLoc = eye;
        if (lateralOffset != 0.0) {
            org.bukkit.util.Vector right = dir.clone().normalize()
                    .crossProduct(new org.bukkit.util.Vector(0, 1, 0)).normalize();
            if (right.lengthSquared() < 0.01) {
                right = new org.bukkit.util.Vector(1, 0, 0);
            }
            spawnLoc = eye.clone().add(right.multiply(lateralOffset));
        }
        Snowball ball = spawnLoc.getWorld().spawn(spawnLoc, Snowball.class, s -> {
            s.setItem(new ItemStack(bulletItem));
            s.setVelocity(dir.normalize().multiply(1.8));
            s.setShooter(player);
            s.setGravity(false);
            s.getPersistentDataContainer().set(shooterProjectileKey, PersistentDataType.BYTE, (byte) 1);
            s.getPersistentDataContainer().set(shooterDamageKey, PersistentDataType.DOUBLE, damage);
            if (slow) {
                s.getPersistentDataContainer().set(shooterSlowKey, PersistentDataType.BYTE, (byte) 1);
            }
            if (fire) {
                s.getPersistentDataContainer().set(shooterFireKey, PersistentDataType.BYTE, (byte) 1);
            }
            if (dragon) {
                s.getPersistentDataContainer().set(shooterDragonKey, PersistentDataType.BYTE, (byte) 1);
            }
            if (iceBlock) {
                s.getPersistentDataContainer().set(shooterIceBlockKey, PersistentDataType.BYTE, (byte) 1);
            }
        });
        if (dragon) {
            DragonFireball visual = spawnLoc.getWorld().spawn(spawnLoc, DragonFireball.class, f -> {
                f.setShooter(player);
                f.setVelocity(new org.bukkit.util.Vector(0, 0, 0));
                f.setDirection(new org.bukkit.util.Vector(0, 0, 0));
                f.setGravity(false);
            });
            try {
                Method scaleMethod = visual.getClass().getMethod("setSize", float.class);
                scaleMethod.invoke(visual, 0.5f);
            } catch (Exception ignored) {
                // 兼容：无法缩放时保持原尺寸
            }
            Bukkit.getScheduler().runTaskTimer(plugin, task -> {
                if (!ball.isValid() || ball.isDead() || !visual.isValid()) {
                    if (visual.isValid()) {
                        visual.remove();
                    }
                    task.cancel();
                    return;
                }
                visual.teleport(ball.getLocation());
            }, 0L, 1L);
        }
    }

    public boolean isCactusShooter(ItemStack item) {
        return item != null && item.getItemMeta() != null
                && item.getItemMeta().getPersistentDataContainer().has(cactusShooterKey, PersistentDataType.BYTE);
    }

    public int cactusDamageLevel(ItemStack item) {
        if (item == null || item.getItemMeta() == null) {
            return 0;
        }
        return item.getItemMeta().getPersistentDataContainer()
                .getOrDefault(cactusDamageKey, PersistentDataType.INTEGER, 0);
    }

    public void fireCactusShooter(Player player) {
        ItemStack held = player.getInventory().getItemInMainHand();
        if (!isCactusShooter(held)) {
            return;
        }
        long now = System.currentTimeMillis();
        String cooldownKey = "shooter:" + player.getUniqueId();
        long last = shooterCooldowns.getOrDefault(cooldownKey, 0L);
        long remaining = 1000L - (now - last);
        if (remaining > 0) {
            player.sendActionBar(ChatColor.YELLOW + "【仙人掌射手】冷却中，还需 " + Math.max(1, (remaining + 999) / 1000) + " 秒");
            return;
        }
        shooterCooldowns.put(cooldownKey, now);
        double damage = rangedWeaponDamage(8.0,
                EquipmentCatalog.Kind.CACTUS_SHOOTER, cactusDamageLevel(held));
        boolean skill = Boolean.TRUE.equals(cactusSkillNext.remove(player.getUniqueId()));
        Location eye = player.getEyeLocation();
        org.bukkit.util.Vector dir = player.getLocation().getDirection().clone().normalize();
        Snowball ball = eye.getWorld().spawn(eye, Snowball.class, s -> {
            s.setItem(new ItemStack(Material.STICK));
            s.setVelocity(dir.multiply(2.0));
            s.setShooter(player);
            s.setGravity(false);
            s.getPersistentDataContainer().set(cactusShooterKey, PersistentDataType.BYTE, (byte) 1);
            s.getPersistentDataContainer().set(cactusDamageKey, PersistentDataType.DOUBLE, damage);
            if (skill) {
                s.getPersistentDataContainer().set(cactusSkillArrowKey, PersistentDataType.BYTE, (byte) 1);
            }
        });
        if (skill) {
        trackCactusLightning(player, ball, 4L);
        }
        trackCactusPierce(player, ball, damage, DamageMath.CACTUS_PIERCE_MAX);
        player.sendActionBar(ChatColor.GREEN + "【仙人掌射手】已发射！");
    }

    private void trackCactusPierce(Player player, Snowball ball, double damage, int maxPierce) {
        Bukkit.getScheduler().runTaskTimer(plugin, task -> {
            if (!ball.isValid() || ball.isDead()) {
                removeCactusTracker(ball);
                task.cancel();
                return;
            }
            for (Entity entity : ball.getWorld().getNearbyEntities(ball.getLocation(), 1.5, 1.5, 1.5)) {
                if (!(entity instanceof Mob mob) || !isMonster(mob)) {
                    continue;
                }
                if (hasCactusHit(ball, mob)) {
                    continue;
                }
                markCactusHit(ball, mob);
                recordPlayerDamage(mob, player, damage);
                mob.damage(damage, player);
                if (cactusHitMonsters.getOrDefault(ball.getUniqueId(), Set.of()).size() >= maxPierce) {
                    removeCactusTracker(ball);
                    ball.remove();
                    task.cancel();
                    return;
                }
            }
        }, 1L, 1L);
    }
    private void trackCactusLightning(Player player, Snowball ball, long interval) {
        Bukkit.getScheduler().runTaskTimer(plugin, task -> {
            if (!ball.isValid() || ball.isDead()) {
                task.cancel();
                return;
            }
            LightningStrike strike = ball.getWorld().strikeLightning(ball.getLocation());
            if (strike != null) {
                strike.getPersistentDataContainer().set(pluginLightningKey, PersistentDataType.BYTE, (byte) 1);
            }
        }, interval, interval);
    }

    public boolean tryCactusShooterSkill(Player player) {
        ItemStack held = player.getInventory().getItemInMainHand();
        if (!isCactusShooter(held) || cactusDamageLevel(held) < 5) {
            return false;
        }
        return tryCactusShooterSkill(player, held);
    }

    public boolean tryCactusShooterSkill(Player player, ItemStack weapon) {
        int level = cactusDamageLevel(weapon);
        if (!isCactusShooter(weapon) || level < 5) {
            return false;
        }
        int tier = level >= 15 ? 15 : level >= 10 ? 10 : 5;
        double damage = DamageMath.CACTUS_SKILL_BULLET_DAMAGE;
        long cooldown = tier >= 15 ? 10_000L : 15_000L;
        long lightningInterval = tier >= 15 ? 2L : tier >= 10 ? 4L : 8L;
        long now = System.currentTimeMillis();
        long last = cactusSkillCooldowns.getOrDefault(player.getUniqueId(), 0L);
        long remaining = cooldown - (now - last);
        if (remaining > 0) {
            player.sendActionBar(ChatColor.YELLOW + "【仙人掌射手技能】冷却中，还需 "
                    + Math.max(1, (remaining + 999) / 1000) + " 秒");
            return true;
        }
        cactusSkillCooldowns.put(player.getUniqueId(), now);
        Location eye = player.getEyeLocation();
        org.bukkit.util.Vector dir = player.getLocation().getDirection().clone().normalize();
        Snowball bullet = eye.getWorld().spawn(eye, Snowball.class, s -> {
            s.setItem(new ItemStack(Material.STICK));
            s.setVelocity(dir.multiply(0.9));
            s.setShooter(player);
            s.setGravity(false);
            s.getPersistentDataContainer().set(cactusShooterKey, PersistentDataType.BYTE, (byte) 1);
            s.getPersistentDataContainer().set(cactusDamageKey, PersistentDataType.DOUBLE, damage);
        });
        trackCactusPierce(player, bullet, damage, Integer.MAX_VALUE);
        trackCactusLightning(player, bullet, lightningInterval);
        player.sendMessage(ChatColor.GREEN + "【仙人掌射手技能】发射穿透闪电弹！");
        return true;
    }

    // ===== 寒冰菇 =====
    public boolean isIceShroom(ItemStack item) {
        return item != null && item.getType() == Material.BLUE_ICE
                && item.getItemMeta() != null
                && item.getItemMeta().getPersistentDataContainer().has(iceShroomKey, PersistentDataType.BYTE);
    }

    public void useIceShroom(Player player) {
        long now = System.currentTimeMillis();
        long last = iceShroomCooldowns.getOrDefault(player.getUniqueId(), 0L);
        long remaining = 30_000L - (now - last);
        if (remaining > 0) {
            player.sendActionBar(ChatColor.YELLOW + "【寒冰菇】冷却中，还需 "
                    + Math.max(1, (remaining + 999) / 1000) + " 秒");
            return;
        }
        iceShroomCooldowns.put(player.getUniqueId(), now);
        Location loc = player.getLocation();
        for (Entity entity : loc.getWorld().getNearbyEntities(loc, 7.5, 7.5, 7.5)) {
            if (!(entity instanceof Mob mob) || !isMonster(mob)) {
                continue;
            }
            mob.damage(1.0, player);
            mob.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 200, 1, false, true, true));
            mob.setAI(false);
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (mob.isValid() && !mob.isDead()) {
                    mob.setAI(true);
                }
            }, 60L);
        }
        player.sendMessage(ChatColor.AQUA + "【寒冰菇】冻结生效！");
    }

    // ===== 大喷菇 =====
    public boolean isBigPuffShroom(ItemStack item) {
        return item != null && item.getType() == Material.AMETHYST_CLUSTER
                && item.getItemMeta() != null
                && item.getItemMeta().getPersistentDataContainer().has(bigPuffKey, PersistentDataType.BYTE);
    }

    public int bigPuffDamageLevel(ItemStack item) {
        return item == null || item.getItemMeta() == null ? 0
                : item.getItemMeta().getPersistentDataContainer()
                .getOrDefault(bigPuffDamageKey, PersistentDataType.INTEGER, 0);
    }

    public void fireBigPuffShroom(Player player) {
        long now = System.currentTimeMillis();
        long last = bigPuffCooldowns.getOrDefault(player.getUniqueId(), 0L);
        long remaining = 1500L - (now - last);
        if (remaining > 0) {
            player.sendActionBar(ChatColor.YELLOW + "【大喷菇】冷却中，还需 "
                    + Math.max(1, (remaining + 999) / 1000) + " 秒");
            return;
        }
        bigPuffCooldowns.put(player.getUniqueId(), now);
        ItemStack held = player.getInventory().getItemInMainHand();
        double damage = rangedWeaponDamage(8.0,
                EquipmentCatalog.Kind.BIG_PUFFSHROOM, bigPuffDamageLevel(held));
        Location eye = player.getEyeLocation();
        org.bukkit.util.Vector dir = player.getLocation().getDirection().clone().setY(0).normalize();
        if (dir.lengthSquared() < 0.01) {
            dir = new org.bukkit.util.Vector(1, 0, 0);
        }
        java.util.Set<UUID> hit = new HashSet<>();
        for (double t = 0.5; t <= 15.0; t += 0.5) {
            Location point = eye.clone().add(dir.clone().multiply(t));
            player.getWorld().spawnParticle(Particle.PORTAL, point, 24, 0.5, 0.5, 0.5, 0);
            for (Entity entity : point.getWorld().getNearbyEntities(point, 1.5, 1.5, 1.5)) {
                if (!(entity instanceof Mob mob) || !isMonster(mob) || !hit.add(mob.getUniqueId())) {
                    continue;
                }
                mob.damage(damage, player);
            }
        }
        player.sendActionBar(ChatColor.GREEN + "【大喷菇】喷射！");
    }

    // ===== 小喷菇 =====
    public boolean isSmallPuffShroom(ItemStack item) {
        return item != null && item.getType() == Material.BROWN_MUSHROOM
                && item.getItemMeta() != null
                && item.getItemMeta().getPersistentDataContainer().has(smallPuffKey, PersistentDataType.BYTE);
    }

    public int smallPuffDamageLevel(ItemStack item) {
        return item == null || item.getItemMeta() == null ? 0
                : item.getItemMeta().getPersistentDataContainer()
                .getOrDefault(smallPuffDamageKey, PersistentDataType.INTEGER, 0);
    }

    public void fireSmallPuffShroom(Player player) {
        long now = System.currentTimeMillis();
        long last = smallPuffCooldowns.getOrDefault(player.getUniqueId(), 0L);
        long remaining = 1500L - (now - last);
        if (remaining > 0) {
            player.sendActionBar(ChatColor.YELLOW + "【小喷菇】冷却中，还需 "
                    + Math.max(1, (remaining + 999) / 1000) + " 秒");
            return;
        }
        smallPuffCooldowns.put(player.getUniqueId(), now);
        ItemStack held = player.getInventory().getItemInMainHand();
        int level = smallPuffDamageLevel(held);
        double damage = rangedWeaponDamage(5.0, EquipmentCatalog.Kind.SMALL_PUFFSHROOM, level);
        int witherAmp = 2 + level / 3;
        int bulletCount = 1 + level / 3;
        Location eye = player.getEyeLocation();
        Location origin = eye.clone();
        for (int b = 0; b < bulletCount; b++) {
            org.bukkit.util.Vector dir = player.getLocation().getDirection().clone().normalize();
            dir.add(new org.bukkit.util.Vector(
                    (Math.random() - 0.5) * 0.4,
                    (Math.random() - 0.5) * 0.2,
                    (Math.random() - 0.5) * 0.4)).normalize();
            Snowball ball = eye.getWorld().spawn(eye, Snowball.class, s -> {
                s.setItem(new ItemStack(Material.PURPLE_STAINED_GLASS));
                s.setVelocity(dir.multiply(2.0));
                s.setShooter(player);
                s.setGravity(false);
                s.getPersistentDataContainer().set(smallPuffBulletKey, PersistentDataType.BYTE, (byte) 1);
                s.getPersistentDataContainer().set(shooterDamageKey, PersistentDataType.DOUBLE, damage);
                s.getPersistentDataContainer().set(smallPuffWitherAmpKey, PersistentDataType.INTEGER, witherAmp);
            });
            trackSmallPuffRange(ball, origin);
        }
        player.sendActionBar(ChatColor.GREEN + "【小喷菇】发射！");
    }

    private void trackSmallPuffRange(Snowball ball, Location origin) {
        Bukkit.getScheduler().runTaskTimer(plugin, task -> {
            if (!ball.isValid() || ball.isDead()) {
                task.cancel();
                return;
            }
            if (ball.getLocation().distance(origin) > 15.0) {
                ball.remove();
                task.cancel();
            }
        }, 1L, 1L);
    }

    // ===== 胆小菇 =====
    public boolean isTimidShroom(ItemStack item) {
        return item != null && item.getType() == Material.AMETHYST_SHARD
                && item.getItemMeta() != null
                && item.getItemMeta().getPersistentDataContainer().has(timidKey, PersistentDataType.BYTE);
    }

    public int timidDamageLevel(ItemStack item) {
        return item == null || item.getItemMeta() == null ? 0
                : item.getItemMeta().getPersistentDataContainer()
                .getOrDefault(timidDamageKey, PersistentDataType.INTEGER, 0);
    }

    public void fireTimidShroom(Player player) {
        long now = System.currentTimeMillis();
        long last = timidLastFire.getOrDefault(player.getUniqueId(), 0L);
        long currentCd = timidCurrentCooldown.getOrDefault(player.getUniqueId(), 1500L);
        long remaining = currentCd - (now - last);
        if (remaining > 0) {
            player.sendActionBar(ChatColor.YELLOW + "【胆小菇】冷却中，还需 "
                    + Math.max(1, (remaining + 999) / 1000) + " 秒");
            return;
        }
        ItemStack held = player.getInventory().getItemInMainHand();
        int level = timidDamageLevel(held);
        int tier3 = level / 3;
        long minCd = Math.max(50L, 300L - 50L * tier3);
        long baseCd = Math.max(minCd, 1500L - 150L * tier3);
        BukkitTask pending = timidResetTasks.remove(player.getUniqueId());
        if (pending != null) {
            pending.cancel();
        }
        int consecutive = pending != null
                ? timidConsecutive.getOrDefault(player.getUniqueId(), 0) + 1 : 0;
        long newCd = Math.max(minCd, baseCd - 50L * consecutive);
        timidConsecutive.put(player.getUniqueId(), consecutive);
        timidCurrentCooldown.put(player.getUniqueId(), newCd);
        timidLastFire.put(player.getUniqueId(), now);
        BukkitTask reset = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            timidResetTasks.remove(player.getUniqueId());
            timidConsecutive.put(player.getUniqueId(), 0);
            timidCurrentCooldown.put(player.getUniqueId(), baseCd);
        }, (newCd + 500L + 49L) / 50L);
        timidResetTasks.put(player.getUniqueId(), reset);
        double damage = 5.0;
        Location eye = player.getEyeLocation();
        org.bukkit.util.Vector dir = player.getLocation().getDirection().clone().normalize();
        eye.getWorld().spawn(eye, Snowball.class, s -> {
            s.setItem(new ItemStack(Material.PURPLE_STAINED_GLASS));
            s.setVelocity(dir.multiply(1.8));
            s.setShooter(player);
            s.setGravity(false);
            s.getPersistentDataContainer().set(shooterProjectileKey, PersistentDataType.BYTE, (byte) 1);
            s.getPersistentDataContainer().set(shooterDamageKey, PersistentDataType.DOUBLE, damage);
        });
        player.sendActionBar(ChatColor.GREEN + "【胆小菇】发射！");
    }

    private void tickWolfAggro() {
        for (UUID wolfId : playerWolves.values()) {
            Entity entity = Bukkit.getEntity(wolfId);
            if (!(entity instanceof Wolf wolf) || !wolf.isValid() || wolf.isDead()) {
                continue;
            }
            LivingEntity target = wolf.getTarget();
            if (target instanceof Mob targetMob && targetMob.isValid() && !targetMob.isDead()
                    && isMonster(targetMob)) {
                continue;
            }
            for (Entity nearby : wolf.getWorld().getNearbyEntities(wolf.getLocation(), 12, 12, 12)) {
                if (nearby instanceof Mob mob && isMonster(mob)) {
                    wolf.setTarget(mob);
                    break;
                }
            }
        }
    }

    public boolean isNut(ItemStack item) {
        return item != null && item.getItemMeta() != null
                && item.getItemMeta().getPersistentDataContainer().has(nutKey, PersistentDataType.BYTE);
    }

    public boolean isBigNut(ItemStack item) {
        return item != null && item.getItemMeta() != null
                && item.getItemMeta().getPersistentDataContainer().has(bigNutKey, PersistentDataType.BYTE);
    }

    public void useNut(Player player, boolean big) {
        String key = (big ? "big_nut" : "nut") + ":" + player.getUniqueId();
        long now = System.currentTimeMillis();
        long last = skillCooldowns.getOrDefault(key, 0L);
        long remaining = 30_000L - (now - last);
        if (remaining > 0) {
            player.sendActionBar(ChatColor.YELLOW + "【" + (big ? "高坚果" : "坚果") + "】冷却中，还需 "
                    + Math.max(1, (remaining + 999) / 1000) + " 秒");
            return;
        }
        skillCooldowns.put(key, now);
        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 200, big ? 4 : 1, false, true, true));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 200, big ? 4 : 2, false, true, true));
        if (big) {
            for (Entity entity : player.getWorld().getNearbyEntities(player.getLocation(), 5, 5, 5)) {
                if (entity instanceof Player nearby && !nearby.equals(player)) {
                    nearby.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 200, 0, false, true, true));
                }
            }
        }
        player.sendMessage(ChatColor.GREEN + "【" + (big ? "高坚果" : "坚果") + "】获得 10 秒增益！");
    }

    private void tickShooterHeal() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            boolean hasTier4 = false;
            for (ItemStack item : player.getInventory().getContents()) {
                if (item == null || item.getType() != Material.DISPENSER || item.getItemMeta() == null) {
                    continue;
                }
                int branch = item.getItemMeta().getPersistentDataContainer()
                        .getOrDefault(shooterBranchKey, PersistentDataType.INTEGER, BRANCH_NONE);
                int tier = item.getItemMeta().getPersistentDataContainer()
                        .getOrDefault(shooterTierKey, PersistentDataType.INTEGER, -1);
                if ((branch == BRANCH_FIRE || branch == BRANCH_ICE) && tier >= 4) {
                    hasTier4 = true;
                    break;
                }
            }
            if (hasTier4 && player.getHealth() < player.getMaxHealth()) {
                player.setHealth(Math.min(player.getMaxHealth(), player.getHealth() + 5.0));
            }
        }
    }

    public boolean hasCactusHit(Entity projectile, Entity monster) {
        Set<UUID> hit = cactusHitMonsters.computeIfAbsent(projectile.getUniqueId(), k -> new HashSet<>());
        return hit.contains(monster.getUniqueId());
    }

    public void markCactusHit(Entity projectile, Entity monster) {
        cactusHitMonsters.computeIfAbsent(projectile.getUniqueId(), k -> new HashSet<>())
                .add(monster.getUniqueId());
    }

    public void removeCactusTracker(Entity projectile) {
        cactusHitMonsters.remove(projectile.getUniqueId());
    }

    public void recordPlayerDamage(Mob mob, Player player, double damage) {
        damageLogs.computeIfAbsent(mob.getUniqueId(), k -> new HashMap<>())
                .merge(player.getUniqueId(), damage, Double::sum);
        addPlayerScore(player, damage);
    }

    public void addPlayerScore(Player player, double damage) {
        if (player == null) {
            return;
        }
        playerScores.merge(player.getUniqueId(), damage, Double::sum);
    }

    public void addPlayerKill(Player player) {
        if (player == null) {
            return;
        }
        playerKills.merge(player.getUniqueId(), 1, Integer::sum);
    }

    private void broadcastPlayerScores() {
        List<Object[]> rows = new ArrayList<>();
        for (UUID uuid : participants) {
            double damage = playerScores.getOrDefault(uuid, 0.0);
            int kills = playerKills.getOrDefault(uuid, 0);
            int total = (int) Math.floor(damage) + kills * 20;
            String name = Bukkit.getOfflinePlayer(uuid).getName();
            if (name == null) {
                name = uuid.toString().substring(0, 8);
            }
            rows.add(new Object[]{total, kills, damage, name});
        }
        rows.sort((a, b) -> Integer.compare((int) b[0], (int) a[0]));
        Bukkit.broadcastMessage(ChatColor.GOLD + "======== 本局个人评分 ========");
        if (rows.isEmpty()) {
            Bukkit.broadcastMessage(ChatColor.GRAY + "本局无参与玩家");
        } else {
            int rank = 1;
            for (Object[] row : rows) {
                Bukkit.broadcastMessage(ChatColor.YELLOW + String.valueOf(rank) + ". " + row[3]
                        + ChatColor.WHITE + "：" + row[0] + " 分"
                        + ChatColor.GRAY + "（伤害 " + (int) Math.floor((double) row[2])
                        + " · 击杀 " + row[1] + "）");
                rank++;
            }
        }
        Bukkit.broadcastMessage(ChatColor.GOLD + "============================");
        playerScores.clear();
        playerKills.clear();
    }

    private UpgradeInfo shooterUpgradeInfo(Player player, int slot) {
        int slotBranch = shooterBranchForSlot(slot);
        WeaponRef found = findWeapon(player, KIND_SHOOTER);
        if (found == null) {
            return new UpgradeInfo(true, false, "无", "豌豆射手", 25, Material.BARRIER, Material.DISPENSER, "");
        }
        int branch = shooterBranch(found.stack());
        int tier = shooterTier(found.stack());
        if (branch == BRANCH_NONE) {
            return new UpgradeInfo(true, false, "豌豆射手", shooterName(slotBranch, 1), 50,
                    Material.DISPENSER, Material.DISPENSER, "");
        }
        if (branch != slotBranch) {
            return new UpgradeInfo(false, false, "", "", 0, Material.BARRIER, null, "已选择其他分支");
        }
        if (tier >= maxShooterTier(branch)) {
            return new UpgradeInfo(true, true, shooterName(branch, tier), "", 0, Material.DISPENSER, null, "");
        }
        return new UpgradeInfo(true, false, shooterName(branch, tier), shooterName(branch, tier + 1),
                shooterPrice(branch, tier + 1), Material.DISPENSER, Material.DISPENSER, "");
    }

    public void handleShooterUpgrade(Player player, int slot) {
        int slotBranch = shooterBranchForSlot(slot);
        WeaponRef found = findWeapon(player, KIND_SHOOTER);
        if (found == null) {
            if (!tryPay(player, 25)) {
                player.sendMessage(ChatColor.RED + "【装备升级】货币不足");
                playPurchaseFail(player);
                return;
            }
            addItemToPlayer(player, createShooterItem(BRANCH_NONE, 0));
            player.sendMessage(ChatColor.GREEN + "【装备升级】已获得豌豆射手");
            playPurchaseSuccess(player);
            return;
        }
        int branch = shooterBranch(found.stack());
        int tier = shooterTier(found.stack());
        if (branch == BRANCH_NONE) {
            if (!tryPay(player, 50)) {
                player.sendMessage(ChatColor.RED + "【装备升级】货币不足");
                playPurchaseFail(player);
                return;
            }
            replaceShooter(player, found, createShooterItem(slotBranch, 1));
            player.sendMessage(ChatColor.GREEN + "【装备升级】已选择" + shooterName(slotBranch, 1) + "分支");
            playPurchaseSuccess(player);
            return;
        }
        if (branch != slotBranch) {
            player.sendMessage(ChatColor.RED + "【装备升级】已选择其他分支，无法更换");
            playPurchaseFail(player);
            return;
        }
        if (tier >= maxShooterTier(branch)) {
            player.sendMessage(ChatColor.GOLD + "【装备升级】该分支已满级");
            playPurchaseFail(player);
            return;
        }
        int price = shooterPrice(branch, tier + 1);
        if (!tryPay(player, price)) {
            player.sendMessage(ChatColor.RED + "【装备升级】货币不足");
            playPurchaseFail(player);
            return;
        }
        replaceShooter(player, found, createShooterItem(branch, tier + 1));
        player.sendMessage(ChatColor.GREEN + "【装备升级】已升级：" + shooterName(branch, tier)
                + " → " + shooterName(branch, tier + 1));
        playPurchaseSuccess(player);
    }

    private void replaceShooter(Player player, WeaponRef found, ItemStack next) {
        if (found.slot() == -1) {
            player.getInventory().setItemInMainHand(next);
        } else {
            player.getInventory().setItem(found.slot(), next);
        }
    }

    private void addItemToPlayer(Player player, ItemStack item) {
        Map<Integer, ItemStack> leftover = player.getInventory().addItem(item);
        for (ItemStack left : leftover.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), left);
        }
    }

    public ItemStack createCherryBomb() {
        ItemStack stack = new ItemStack(Material.RED_WOOL);
        ItemMeta meta = stack.getItemMeta();
        meta.setItemName("樱桃炸弹");
        meta.setLore(List.of(
                ChatColor.GRAY + "主手右键：以爆炸方式杀死自己，并在原地产生爆炸",
                ChatColor.GRAY + "伤害为 TNT 的 2 倍，范围同 TNT，冷却 30 秒"));
        meta.getPersistentDataContainer().set(cherryBombKey, PersistentDataType.BYTE, (byte) 1);
        stack.setItemMeta(meta);
        return stack;
    }

    public ItemStack createDestroyShroom() {
        ItemStack stack = new ItemStack(Material.BLACK_WOOL);
        ItemMeta meta = stack.getItemMeta();
        meta.setItemName("毁灭菇");
        meta.setLore(List.of(
                ChatColor.GRAY + "主手右键：以爆炸方式杀死自己，并在原地产生爆炸",
                ChatColor.GRAY + "伤害为 TNT 的 5 倍，范围为 TNT 的 5 倍，冷却 2 分钟"));
        meta.getPersistentDataContainer().set(destroyShroomKey, PersistentDataType.BYTE, (byte) 1);
        stack.setItemMeta(meta);
        return stack;
    }

    public boolean isCherryBomb(ItemStack item) {
        return item != null && item.getItemMeta() != null
                && item.getItemMeta().getPersistentDataContainer().has(cherryBombKey, PersistentDataType.BYTE);
    }

    public boolean isDestroyShroom(ItemStack item) {
        return item != null && item.getItemMeta() != null
                && item.getItemMeta().getPersistentDataContainer().has(destroyShroomKey, PersistentDataType.BYTE);
    }

    public void fireBomb(Player player, boolean destroy) {
        long now = System.currentTimeMillis();
        Map<UUID, Long> table = destroy ? destroyCooldowns : cherryCooldowns;
        long last = table.getOrDefault(player.getUniqueId(), 0L);
        long cooldown = destroy ? 120_000L : 30_000L;
        long remaining = cooldown - (now - last);
        String name = destroy ? "毁灭菇" : "樱桃炸弹";
        if (remaining > 0) {
            long seconds = (remaining + 999) / 1000;
            player.sendActionBar(ChatColor.YELLOW + "【" + name + "】冷却中，还需 " + seconds + " 秒");
            return;
        }
        table.put(player.getUniqueId(), now);
        Location loc = player.getLocation();
        double radius = destroy ? 30.0 : 6.0;
        double mult = destroy ? 5.0 : 2.0;
        applyBombAoE(player, loc, destroy, radius, mult);
        Bukkit.getScheduler().runTaskLater(plugin,
                () -> applyBombAoE(player, loc, destroy, radius, mult), 10L);
        player.setLastDamageCause(new EntityDamageEvent(player,
                EntityDamageEvent.DamageCause.BLOCK_EXPLOSION, Double.MAX_VALUE));
        player.setHealth(0);
        player.sendMessage(ChatColor.RED + "【" + name + "】你被炸死了！");
    }

    private void applyBombAoE(Player player, Location loc, boolean destroy, double radius, double mult) {
        loc.getWorld().createExplosion(loc, 0.0f, false, false);
        double tntCenter = 65.0;
        for (Entity entity : loc.getWorld().getNearbyEntities(loc, radius, radius, radius)) {
            if (!(entity instanceof LivingEntity living) || living.equals(player) || living.isDead()) {
                continue;
            }
            if (entity instanceof Villager villager && isDave(villager)) {
                continue;
            }
            if (entity instanceof Player other) {
                if (!destroy) {
                    continue;
                }
                String team = getTeamName(player);
                if (team == null || !team.equals(getTeamName(other))) {
                    continue;
                }
            }
            double dist = living.getLocation().distance(loc);
            if (dist > radius) {
                continue;
            }
            double dmg = (1.0 - dist / radius) * tntCenter * mult;
            if (dmg > 0) {
                if (entity instanceof Mob mob && isMonster(mob)) {
                    bombKills.put(mob.getUniqueId(), player.getUniqueId());
                    damageLogs.computeIfAbsent(mob.getUniqueId(), k -> new HashMap<>())
                            .merge(player.getUniqueId(), dmg, Double::sum);
                    addPlayerScore(player, dmg);
                }
                living.damage(dmg);
            }
        }
    }

    public void returnLoyaltyTridents(Player player) {
        for (World world : Bukkit.getWorlds()) {
            for (Trident trident : world.getEntitiesByClass(Trident.class)) {
                if (!trident.isValid() || trident.isDead()) {
                    continue;
                }
                if (!player.equals(trident.getShooter())) {
                    continue;
                }
                ItemStack item = trident.getItem();
                if (item == null || !item.containsEnchantment(Enchantment.LOYALTY)) {
                    continue;
                }
                Map<Integer, ItemStack> leftover = player.getInventory().addItem(item.clone());
                for (ItemStack left : leftover.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), left);
                }
                trident.remove();
            }
        }
    }

    public ItemStack createMenuClock() {
        ItemStack stack = new ItemStack(Material.CLOCK);
        ItemMeta meta = stack.getItemMeta();
        Component name = Component.text()
                .append(Component.text("主", TextColor.color(0xFFD700)))
                .append(Component.text("菜", TextColor.color(0xFFB300)))
                .append(Component.text("单", TextColor.color(0xFF8800)))
                .decorate(TextDecoration.BOLD)
                .build();
        meta.itemName(name);
        meta.setLore(List.of(ChatColor.GRAY + "右键打开主菜单"));
        meta.getPersistentDataContainer().set(menuClockKey, PersistentDataType.BYTE, (byte) 1);
        stack.setItemMeta(meta);
        return stack;
    }

    public boolean isMenuClock(ItemStack item) {
        return item != null && item.getItemMeta() != null
                && item.getItemMeta().getPersistentDataContainer().has(menuClockKey, PersistentDataType.BYTE);
    }

    public boolean isMenuRedstone(ItemStack item) {
        return item != null && item.getItemMeta() != null
                && item.getItemMeta().getPersistentDataContainer().has(menuRedstoneKey, PersistentDataType.BYTE);
    }

    public boolean isMenuItem(ItemStack item) {
        return isMenuClock(item) || isMenuRedstone(item);
    }

    public void applyLobbyItems(Player player) {
        if (player.getGameMode() == GameMode.SPECTATOR) {
            player.getInventory().setItem(8, createSpectatorExitItem());
            removeStaleMenuRedstone(player);
            return;
        }
        player.getInventory().setItem(8, createMenuClock());
        removeStaleMenuRedstone(player);
        removeShopReturnItems(player);
    }

    private void removeStaleMenuRedstone(Player player) {
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (isMenuRedstone(item)) {
                player.getInventory().setItem(i, null);
            }
        }
    }

    public String killNearestDave(Player center) {
        Villager nearest = null;
        double nearestSq = Double.MAX_VALUE;
        for (World world : Bukkit.getWorlds()) {
            if (!center.getWorld().equals(world)) {
                continue;
            }
            for (Villager villager : allDaves(world)) {
                if (villager.isDead()) {
                    continue;
                }
                double distanceSq = villager.getLocation().distanceSquared(center.getLocation());
                if (distanceSq < nearestSq) {
                    nearestSq = distanceSq;
                    nearest = villager;
                }
            }
        }
        if (nearest == null) {
            return "没有存活的戴夫";
        }
        String team = getOwnerTeam(nearest);
        nearest.setHealth(0);
        return team == null
                ? ChatColor.GOLD + "已击杀最近的戴夫"
                : ChatColor.GOLD + "已击杀最近的戴夫（" + displayName(team) + " 队）";
    }

    public void sendWelcome(Player player) {
        player.sendTitle("§6斗§e蛐§a蛐§b服§d务§6器§e欢§a迎§b您", "§7斗蛐蛐 PvE 服务器", 10, 70, 10);
    }

    public void enable() {
        loadConfig();
        chestManager = new TeamChestManager(plugin, this);
        brewingManager = new BrewingStandManager(plugin, brewingWorld, brewingX, brewingY, brewingZ);
        brewingManager.load();
        retargetTask = Bukkit.getScheduler().runTaskTimer(plugin, this::retargetAllMonsters, 0L, RETARGET_INTERVAL);
        refreshTask = Bukkit.getScheduler().runTaskTimer(plugin, this::refreshBossBars, 0L, REFRESH_INTERVAL);
        lobbyTask = Bukkit.getScheduler().runTaskTimer(plugin, this::refreshLobbyBar, 0L, 20L);
        gameTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tickGame, 0L, 20L);
        currencyTask = Bukkit.getScheduler().runTaskTimer(plugin, this::consolidateAllCurrencies, 0L, 20L);
        spawnTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tickSpawner, 0L, 20L);
        lobbyBuffTask = Bukkit.getScheduler().runTaskTimer(plugin, this::applyLobbyResistance, 0L, 20L);
        brewingFuelTask = Bukkit.getScheduler().runTaskTimer(plugin, brewingManager::refillFuel, 0L, 20L);
        groundCleanupTask = Bukkit.getScheduler().runTaskTimer(plugin, this::cleanupGroundEntities, 0L, 200L);
        groundItemCleanupTask = Bukkit.getScheduler().runTaskTimer(plugin, this::cleanupGroundItems, 0L, 1200L);
        wolfRespawnTask = Bukkit.getScheduler().runTaskTimer(plugin, this::refreshWolfRespawns, 0L, 20L);
        wolfRideTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tickWolfAggro, 0L, 20L);
        shooterHealTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tickShooterHeal, 0L, 100L);
        refreshBossBars();
        pvzMode = new PvzMode(plugin, this);
        pvzMode.enable();
    }

    public PvzMode pvzMode() {
        return pvzMode;
    }

    public boolean isPvzPlayer(UUID uuid) {
        return pvzPlayers.contains(uuid);
    }

    /** 经典模式生命周期是否可以处理该玩家；PVZ 玩家由 PvzMode 独立管理。 */
    boolean isClassicPlayer(Player player) {
        return player != null && !isPvzPlayer(player.getUniqueId());
    }

    public void setPvzPlayer(UUID uuid, boolean on) {
        if (on) {
            pvzPlayers.add(uuid);
        } else {
            pvzPlayers.remove(uuid);
        }
    }

    public void disable() {
        if (retargetTask != null) {
            retargetTask.cancel();
        }
        if (refreshTask != null) {
            refreshTask.cancel();
        }
        if (lobbyTask != null) {
            lobbyTask.cancel();
        }
        if (gameTask != null) {
            gameTask.cancel();
        }
        if (currencyTask != null) {
            currencyTask.cancel();
        }
        if (spawnTask != null) {
            spawnTask.cancel();
        }
        if (lobbyBuffTask != null) {
            lobbyBuffTask.cancel();
        }
        if (brewingFuelTask != null) {
            brewingFuelTask.cancel();
        }
        if (groundCleanupTask != null) {
            groundCleanupTask.cancel();
        }
        if (groundItemCleanupTask != null) {
            groundItemCleanupTask.cancel();
        }
        if (wolfRespawnTask != null) {
            wolfRespawnTask.cancel();
        }
        if (wolfRideTask != null) {
            wolfRideTask.cancel();
        }
        if (shooterHealTask != null) {
            shooterHealTask.cancel();
        }
        if (chestManager != null) {
            chestManager.saveAll();
        }
        if (brewingManager != null) {
            brewingManager.save();
        }
        if (lobbyBar != null) {
            lobbyBar.removeAll();
            lobbyBar = null;
        }
        if (autoStartBar != null) {
            autoStartBar.removeAll();
            autoStartBar = null;
        }
        for (BossBar bar : bars.values()) {
            bar.removeAll();
        }
        bars.clear();
        pendingCreates.clear();
        if (pvzMode != null) {
            pvzMode.disable();
            pvzMode = null;
        }
    }

    public boolean isDave(Entity entity) {
        return entity instanceof Villager
                && entity.getScoreboardTags().contains("rz")
                && entity.getScoreboardTags().contains("dave");
    }

    public boolean isMonster(Entity entity) {
        return entity instanceof Mob
                && entity.getScoreboardTags().contains("rz")
                && entity.getScoreboardTags().contains("monster");
    }

    public void markPending(Player player) {
        pendingCreates.put(player.getUniqueId(), System.currentTimeMillis());
    }

    public void markUnready(Player player) {
        readyPlayers.remove(player.getUniqueId());
    }

    public boolean isReady(Player player) {
        return readyPlayers.contains(player.getUniqueId());
    }

    public void setTeamPreference(Player player, String teamId) {
        if (teamId == null || TEAM_IDS.contains(teamId)) {
            teamPreferences.put(player.getUniqueId(), teamId);
        }
    }

    public String teamPreference(Player player) {
        return teamPreferences.get(player.getUniqueId());
    }

    public int teamPreferenceCount(String teamId) {
        int count = 0;
        for (String preferred : teamPreferences.values()) {
            if (teamId.equals(preferred)) {
                count++;
            }
        }
        return count;
    }

    public int readyCount() {
        int count = 0;
        for (UUID id : readyPlayers) {
            Player player = Bukkit.getPlayer(id);
            if (player != null && player.isOnline()) {
                count++;
            }
        }
        return count;
    }

    public boolean setReady(Player player, boolean ready) {
        if (isPvzPlayer(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "【PVZ】你正在 PVZ 模式中，无法准备经典模式");
            return false;
        }
        return ready ? readyPlayers.add(player.getUniqueId()) : readyPlayers.remove(player.getUniqueId());
    }

    public void handleDaveSpawn(Villager dave) {
        if (getOwnerTeam(dave) != null) {
            return;
        }
        UUID creator = findPendingCreator(dave);
        if (creator == null) {
            plugin.getLogger().info("检测到未绑定的戴夫，跳过队伍绑定");
            return;
        }
        pendingCreates.remove(creator);
        Player player = Bukkit.getPlayer(creator);
        if (player == null) {
            return;
        }
        String team = getTeamName(player);
        if (team == null) {
            dave.remove();
        player.sendMessage(ChatColor.GOLD + "【戴夫】你没有加入任何队伍，请先用 /team join <队伍> 加入队伍再生成戴夫");
            return;
        }
        if (hasLivingDave(team)) {
            dave.remove();
        player.sendMessage(ChatColor.GOLD + "【戴夫】你所在的队伍已有一个存活的戴夫，无法重复生成");
            return;
        }
        dave.getPersistentDataContainer().set(ownerTeamKey, PersistentDataType.STRING, team);
        dave.setCustomName(teamColor(team) + "戴夫");
        createBar(dave, team);
        player.sendMessage(ChatColor.GOLD + "【戴夫】已生成戴夫，归属队伍：" + team + "（生命 100）");
    }

    public void retarget(Mob mob) {
        if (!isMonster(mob)) {
            return;
        }
        if (mob.getScoreboardTags().contains("big_boss")) {
            BossAttacker recent = bossAttackers.get(mob.getUniqueId());
            if (recent != null && System.currentTimeMillis() - recent.timestamp() <= 10_000L) {
                Player attacker = Bukkit.getPlayer(recent.attackerId());
                if (attacker != null && attacker.isOnline() && !attacker.isDead()) {
                    AttributeInstance follow = mob.getAttribute(Attribute.FOLLOW_RANGE);
                    if (follow != null && follow.getBaseValue() < FOLLOW_RANGE) {
                        follow.setBaseValue(FOLLOW_RANGE);
                    }
                    if (mob.getTarget() == null || !mob.getTarget().equals(attacker)) {
                        mob.setTarget(attacker);
                    }
                    return;
                }
            }
        }
        Villager dave = null;
        for (String tag : mob.getScoreboardTags()) {
            if (tag.startsWith("boss_team_")) {
                dave = teamDave(tag.substring("boss_team_".length()));
                break;
            }
        }
        if (dave == null) {
            dave = nearestDave(mob);
        }
        if (dave == null) {
            return;
        }
        if (mob instanceof Warden warden) {
            warden.setAnger(dave, 150);
        }
        AttributeInstance follow = mob.getAttribute(Attribute.FOLLOW_RANGE);
        if (follow != null && follow.getBaseValue() < FOLLOW_RANGE) {
            follow.setBaseValue(FOLLOW_RANGE);
        }
        if (mob.getTarget() == null || !mob.getTarget().equals(dave)) {
            mob.setTarget(dave);
        }
    }

    public void handleDaveDeath(Villager dave) {
        cleanupNearDave(dave);
        String team = getOwnerTeam(dave);
        BossBar bar = bars.remove(dave.getUniqueId());
        if (bar != null) {
            bar.removeAll();
        }
        if (team == null) {
            return;
        }
        Bukkit.broadcastMessage(ChatColor.GOLD + "【戴夫】" + displayName(team) + " 队的戴夫阵亡了！该队被淘汰");
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (team.equals(getTeamName(player))) {
                player.sendTitle("§c游戏结束", "本队失败", 10, 60, 10);
                player.getInventory().clear();
                player.setGameMode(GameMode.SPECTATOR);
            }
        }
        Set<String> surviving = survivingTeams();
        if (surviving.size() == 1) {
            String winner = surviving.iterator().next();
            Bukkit.broadcastMessage(ChatColor.GOLD + "【戴夫】" + displayName(winner) + " 队获胜！");
            if (teamsAtGameStart >= 2) {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (winner.equals(getTeamName(player))) {
                        player.sendTitle("§6我§e们§a是§b冠§d军", "斗蛐蛐 PvE 冠军", 10, 80, 10);
                    }
                }
            }
            endGame();
        } else if (surviving.isEmpty()) {
            Bukkit.broadcastMessage(ChatColor.GOLD + "【戴夫】本局结束：没有幸存队伍");
            endGame();
        }
    }

    public void handleDamage(EntityDamageByEntityEvent event) {
        if (isPvzCombatEntity(event.getDamager()) || isPvzCombatEntity(event.getEntity())) {
            return;
        }
        if (event.getDamager() instanceof Warden warden && isMonster(warden)) {
            event.setDamage(event.getDamage() * DamageMath.WARDEN_ATTACK_MULTIPLIER);
        }
        if (event.getDamager() instanceof Creeper creeper && isMonster(creeper)) {
            event.setDamage(event.getDamage() * DamageMath.CREEPER_ATTACK_MULTIPLIER);
        }
        if (event.getDamager() instanceof Wither wither && isMonster(wither)) {
            event.setDamage(event.getDamage() * DamageMath.WITHER_ATTACK_MULTIPLIER);
        }
        if (event.getDamager() instanceof WitherSkull skull
                && skull.getShooter() instanceof Wither wither && isMonster(wither)) {
            event.setDamage(event.getDamage() * DamageMath.WITHER_SKULL_ATTACK_MULTIPLIER);
        }
        if (event.getEntity() instanceof Wither boss
                && boss.getScoreboardTags().contains("big_boss")) {
            event.setDamage(event.getDamage() * DamageMath.BIG_BOSS_INCOMING_MULTIPLIER);
        }
        if (event.getDamager() instanceof Projectile projectile
                && projectile.getShooter() instanceof Mob shooterMob
                && isMonster(shooterMob)
                && !shooterMob.getScoreboardTags().contains("mini_boss")
                && !shooterMob.getScoreboardTags().contains("big_boss")) {
            event.setDamage(event.getDamage() * DamageMath.REGULAR_MOB_PROJECTILE_MULTIPLIER);
        }
        if (event.getDamager() instanceof Player attackerPlayer) {
            ItemStack held = attackerPlayer.getInventory().getItemInMainHand();
            if (held != null && isMeleeWeapon(held.getType())) {
                int bonus = weaponDamageLevel(held);
                EquipmentCatalog.Kind heldKind = heldWeaponKind(held);
                double meleeBonus = heldKind == null ? bonus : rangedBonusForLevel(heldKind, bonus);
                if (meleeBonus > 0) {
                    event.setDamage(event.getDamage() + meleeBonus);
                }
            }
        }
        if (event.getDamager() instanceof LightningStrike strike
                && strike.getPersistentDataContainer().has(pluginLightningKey, PersistentDataType.BYTE)
                && (event.getEntity() instanceof Player
                || isDave(event.getEntity())
                || (event.getEntity() instanceof Wolf wolf && isPetWolf(wolf)))) {
            event.setCancelled(true);
            return;
        }
        Player attacker = resolvePlayerDamager(event.getDamager());
        if (attacker != null && event.getEntity() instanceof Mob mob
                && mob.getScoreboardTags().contains("big_boss")) {
            bossAttackers.put(mob.getUniqueId(), new BossAttacker(attacker.getUniqueId(), System.currentTimeMillis()));
        }
        if (attacker != null && event.getEntity() instanceof Mob mob && isMonster(mob)) {
            damageLogs.computeIfAbsent(mob.getUniqueId(), k -> new HashMap<>())
                    .merge(attacker.getUniqueId(), event.getDamage(), Double::sum);
            addPlayerScore(attacker, event.getDamage());
        }
        if (attacker != null && event.getDamager() instanceof Player
                && event.getEntity() instanceof Mob mob && isMonster(mob)) {
            ItemStack held = attacker.getInventory().getItemInMainHand();
            if (held != null && held.getType() == Material.MACE
                    && held.containsEnchantment(Enchantment.WIND_BURST)) {
                Player p = attacker;
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (!p.isOnline()) {
                        return;
                    }
                    double fall = p.getFallDistance();
                    double targetY = Math.min(2.5, Math.max(0.8, 0.6 + 0.3 * fall));
                    org.bukkit.util.Vector vel = p.getVelocity();
                    if (vel.getY() < targetY) {
                        p.setVelocity(vel.setY(targetY));
                    }
                    grantWindMaceFallImmunity(p);
                }, 1L);
            }
        }
        if (event.getDamager() instanceof Wolf wolf && isPetWolf(wolf)
                && event.getEntity() instanceof Mob mob && isMonster(mob)) {
            UUID owner = ownerOfWolf(wolf);
            if (owner != null) {
                damageLogs.computeIfAbsent(mob.getUniqueId(), k -> new HashMap<>())
                        .merge(owner, event.getDamage(), Double::sum);
                Player ownerPlayer = Bukkit.getPlayer(owner);
                addPlayerScore(ownerPlayer, event.getDamage());
            }
        }
        if (attacker != null && event.getEntity() instanceof Wolf wolf && isPetWolf(wolf)) {
            event.setCancelled(true);
            return;
        }
        if (event.getDamager() instanceof Wolf wolf && isPetWolf(wolf)
                && event.getEntity() instanceof Villager villager && isDave(villager)) {
            event.setCancelled(true);
            return;
        }
        if (event.getDamager() instanceof Wolf wolf && isPetWolf(wolf)
                && event.getEntity() instanceof Mob mob && isMonster(mob)) {
            UUID owner = ownerOfWolf(wolf);
            if (owner != null) {
                wolfKills.put(mob.getUniqueId(), owner);
            }
        }
        if (attacker != null && event.getEntity() instanceof Player && !pvpEnabled) {
            event.setCancelled(true);
            attacker.sendMessage(ChatColor.RED + "【PVP】当前禁止玩家互相伤害");
            return;
        }
        if (!(event.getEntity() instanceof Villager villager) || !isDave(villager)) {
            return;
        }
        String team = getOwnerTeam(villager);
        if (team == null) {
            return;
        }
        if (attacker != null && team.equals(getTeamName(attacker))) {
            event.setCancelled(true);
            attacker.sendMessage(ChatColor.GOLD + "【戴夫】不能伤害自己队的戴夫");
            return;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (team.equals(getTeamName(player))) {
                player.sendMessage(ChatColor.GOLD + "歪比八卜");
            }
        }
        lastDaveHit.put(villager.getUniqueId(), System.currentTimeMillis());
        BossBar bar = bars.get(villager.getUniqueId());
        if (bar != null) {
            bar.setTitle("§c戴夫 ⚠ 正在被攻击");
        }
    }

    private boolean isPvzCombatEntity(Entity entity) {
        if (entity == null) {
            return false;
        }
        if (entity instanceof Player player) {
            return isPvzPlayer(player.getUniqueId());
        }
        if (entity.getScoreboardTags().contains(PvzMode.TAG_MONSTER)) {
            return true;
        }
        if (entity instanceof Projectile projectile
                && projectile.getShooter() instanceof Entity shooter) {
            return isPvzCombatEntity(shooter);
        }
        return false;
    }

    public boolean isPvpEnabled() {
        return pvpEnabled;
    }

    public boolean togglePvp() {
        pvpEnabled = !pvpEnabled;
        Bukkit.broadcastMessage(ChatColor.GOLD + "【PVP】玩家互伤已" + (pvpEnabled ? "开启" : "关闭"));
        return pvpEnabled;
    }

    private static void clearExperience(Player player) {
        player.setTotalExperience(0);
        player.setLevel(0);
        player.setExp(0);
    }

    public void applyStewEffect(Player player) {
        boolean buff = ThreadLocalRandom.current().nextDouble() < 0.30;
        List<PotionEffectType> pool = buff ? STEW_BUFFS : STEW_DEBUFFS;
        PotionEffectType type = pool.get(ThreadLocalRandom.current().nextInt(pool.size()));
        player.addPotionEffect(new PotionEffect(type, 200, 0, false, true, true));
        player.sendMessage(ChatColor.GOLD + "【谜之炖菜】你获得了" + (buff ? "增益" : "减益")
                + "：" + effectDisplayName(type) + "（10 秒）");
    }

    public void channelingStrike(Player player, Location location) {
        World world = location.getWorld();
        if (world == null || world.isThundering()) {
            return;
        }
        for (Villager dave : allDaves(world)) {
            if (!dave.isDead() && dave.getLocation().distanceSquared(location) < 9.0) {
                player.sendMessage(ChatColor.GOLD + "【引雷】附近有戴夫，引雷已取消");
                return;
            }
        }
        LightningStrike strike = world.strikeLightning(location);
        if (strike != null) {
            strike.getPersistentDataContainer().set(pluginLightningKey, PersistentDataType.BYTE, (byte) 1);
            for (Entity entity : world.getNearbyEntities(location, 4, 4, 4)) {
                if (entity instanceof Mob mob && isMonster(mob)) {
                    damageLogs.computeIfAbsent(mob.getUniqueId(), k -> new HashMap<>())
                            .merge(player.getUniqueId(), 5.0, Double::sum);
                }
            }
        }
    }

    private static String effectDisplayName(PotionEffectType type) {
        return switch (type.getKey().getKey()) {
            case "strength" -> "力量";
            case "speed" -> "速度";
            case "haste" -> "急迫";
            case "resistance" -> "抗性提升";
            case "jump_boost" -> "跳跃提升";
            case "regeneration" -> "再生";
            case "fire_resistance" -> "防火";
            case "weakness" -> "虚弱";
            case "slowness" -> "缓慢";
            case "mining_fatigue" -> "挖掘疲劳";
            case "hunger" -> "饥饿";
            case "poison" -> "中毒";
            case "nausea" -> "反胃";
            case "blindness" -> "失明";
            default -> type.getKey().getKey();
        };
    }

    public void collectKillDrops(List<ItemStack> drops, Player killer) {
        if (drops == null || drops.isEmpty()) {
            return;
        }
        List<ItemStack> currency = new ArrayList<>();
        for (ItemStack item : drops) {
            if (isCurrency(item, ShopCurrency.SILVER) || isCurrency(item, ShopCurrency.GOLD) || isCurrency(item, ShopCurrency.DIAMOND)) {
                currency.add(item);
            }
        }
        if (currency.isEmpty()) {
            return;
        }
        if (deathMode) {
            drops.removeAll(currency);
            return;
        }
        if (killer == null) {
            return;
        }
        drops.removeAll(currency);
        for (ItemStack item : currency) {
            Map<Integer, ItemStack> leftover = killer.getInventory().addItem(item);
            for (ItemStack rest : leftover.values()) {
                killer.getWorld().dropItemNaturally(killer.getLocation(), rest);
            }
        }
    }

    public void removeVanillaDrops(List<ItemStack> drops) {
        if (drops == null) {
            return;
        }
        drops.removeIf(item -> !isCurrency(item, ShopCurrency.SILVER)
                && !isCurrency(item, ShopCurrency.GOLD)
                && !isCurrency(item, ShopCurrency.DIAMOND)
                && !isCurrency(item, ShopCurrency.STAR));
    }

    public boolean purchase(Player player, ShopItem item) {
        int priceSilver = item.price() * DamageMath.currencyMultiplier(item.currency());
        long totalSilver = totalSilverValue(player);
        if (totalSilver < priceSilver) {
            player.sendMessage(ChatColor.RED + "【商店】" + item.name() + " 需要 " + item.price()
                    + " " + item.currency().displayName() + "（折合 " + priceSilver + " 银币），"
                    + "你的货币总值 " + totalSilver + " 银币，不足");
            playPurchaseFail(player);
            return false;
        }
        if (item.action() == ShopItem.ShopAction.DAVE_HEAL && !canBuyDaveHeal(player)) {
            playPurchaseFail(player);
            return false;
        }
        if (item.action() == ShopItem.ShopAction.DAVE_RESISTANCE && !canBuyDaveResistance(player)) {
            playPurchaseFail(player);
            return false;
        }
        if (item.action() == ShopItem.ShopAction.WOLF && hasWolf(player)) {
            player.sendMessage(ChatColor.RED + "【宠物】你已拥有一条狼，无法再购买");
            playPurchaseFail(player);
            return false;
        }
        if ((item.action() == ShopItem.ShopAction.WOLF_HEALTH
                || item.action() == ShopItem.ShopAction.WOLF_DAMAGE
                || item.action() == ShopItem.ShopAction.WOLF_SPEED) && !hasWolf(player)) {
            player.sendMessage(ChatColor.RED + "【宠物】请先购买狼再升级属性");
            playPurchaseFail(player);
            return false;
        }
        if (item.action() == ShopItem.ShopAction.WOLF_SPEED && wolfSpeedCount(player) >= 10) {
            player.sendMessage(ChatColor.RED + "【宠物】狼移速升级已达上限（10 次）");
            playPurchaseFail(player);
            return false;
        }
        if (!tryPay(player, priceSilver)) {
            player.sendMessage(ChatColor.RED + "【商店】扣除货币失败，请重试");
            playPurchaseFail(player);
            return false;
        }
        switch (item.action()) {
            case LEVEL -> {
                player.setLevel(player.getLevel() + 1);
                player.sendMessage(ChatColor.GREEN + "【商店】交易成功：" + item.name()
                        + "（当前等级 " + player.getLevel() + "）");
                playPurchaseSuccess(player);
                return true;
            }
            case DAVE_HEAL -> {
                applyDaveHeal(player);
                playPurchaseSuccess(player);
                return true;
            }
            case DAVE_RESISTANCE -> {
                applyDaveResistance(player);
                playPurchaseSuccess(player);
                return true;
            }
            case WOLF -> {
                if (buyWolf(player)) {
                    playPurchaseSuccess(player);
                } else {
                    playPurchaseFail(player);
                }
                return true;
            }
            case WOLF_HEALTH -> {
                upgradeWolfHealth(player);
                playPurchaseSuccess(player);
                return true;
            }
            case WOLF_DAMAGE -> {
                upgradeWolfDamage(player);
                playPurchaseSuccess(player);
                return true;
            }
            case WOLF_SPEED -> {
                upgradeWolfSpeed(player);
                playPurchaseSuccess(player);
                return true;
            }
            default -> { }
        }
        ItemStack product = item.product().clone();
        Map<Integer, ItemStack> leftover = player.getInventory().addItem(product);
        if (!leftover.isEmpty()) {
            player.getWorld().dropItemNaturally(player.getLocation(), product);
            player.sendMessage(ChatColor.YELLOW + "【商店】背包已满，" + item.name() + " 掉落在你脚下");
        } else {
            player.sendMessage(ChatColor.GREEN + "【商店】交易成功：" + item.name() + "（已自动扣款找零）");
        }
        playPurchaseSuccess(player);
        return true;
    }

    boolean tryPay(Player player, int priceSilver) {
        if (totalSilverValue(player) < priceSilver) {
            return false;
        }
        Map<ShopCurrency, Integer> available = new HashMap<>();
        for (ShopCurrency currency : new ShopCurrency[]{ShopCurrency.DIAMOND, ShopCurrency.GOLD, ShopCurrency.SILVER}) {
            int count = countCurrency(player, currency);
            if (count > 0) {
                available.put(currency, count);
            }
        }
        DamageMath.PaymentPlan plan = DamageMath.paymentPlan(priceSilver, available);
        for (Map.Entry<ShopCurrency, Integer> entry : plan.taken().entrySet()) {
            takeCurrencyUpTo(player, entry.getKey(), entry.getValue());
        }
        if (plan.changeSilver() > 0) {
            giveChange(player, plan.changeSilver());
        }
        return true;
    }

    private void playPurchaseSuccess(Player player) {
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
    }

    private void playPurchaseFail(Player player) {
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
    }

    private record UpgradeInfo(boolean valid, boolean maxed, String currentName, String nextName,
                               int price, Material icon, Material nextMaterial, String error) {
    }

    private static final Material[] ARMOR_NEXT = {
            Material.CHAINMAIL_HELMET, Material.IRON_HELMET, Material.DIAMOND_HELMET, Material.NETHERITE_HELMET
    };
    private static final Material[] ARMOR_NEXT_CHEST = {
            Material.CHAINMAIL_CHESTPLATE, Material.IRON_CHESTPLATE, Material.DIAMOND_CHESTPLATE, Material.NETHERITE_CHESTPLATE
    };
    private static final Material[] ARMOR_NEXT_LEGS = {
            Material.CHAINMAIL_LEGGINGS, Material.IRON_LEGGINGS, Material.DIAMOND_LEGGINGS, Material.NETHERITE_LEGGINGS
    };
    private static final Material[] ARMOR_NEXT_BOOTS = {
            Material.CHAINMAIL_BOOTS, Material.IRON_BOOTS, Material.DIAMOND_BOOTS, Material.NETHERITE_BOOTS
    };
    private static final int[] PRICE_SMALL = {12, 25, 50, 100};
    private static final int[] PRICE_LARGE = {15, 35, 70, 140};
    private static final int[] PRICE_WEAPON = {25, 50, 100};
    private static final int[] PRICE_SPEAR = {10, 20, 40};

    public ItemStack buildEquipmentUpgradeButton(Player player, EquipmentCatalog.Kind kind) {
        UpgradeInfo info = upgradeInfo(player, kind);
        ItemStack stack = new ItemStack(info.icon() == null ? Material.BARRIER : info.icon());
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return stack;
        }
        String name = (info.valid() || info.maxed()) ? "升级 " + info.currentName() : info.error();
        meta.setItemName(name);
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "适用：" + kind.display() + "（" + (isArmor(kind) ? "穿戴部位" : "主手或背包中等级最高的对应武器") + "）");
        if (info.maxed()) {
            lore.add(ChatColor.GOLD + "已满级");
        } else if (info.valid()) {
            lore.add(ChatColor.GRAY + "当前等级：" + info.currentName());
            lore.add(ChatColor.YELLOW + "下一等级：" + info.nextName());
            lore.add(ChatColor.AQUA + "所需货币：" + info.price() + " 银币");
            lore.add(ChatColor.GREEN + "点击升级");
        } else {
            lore.add(ChatColor.RED + info.error());
        }
        meta.setLore(lore);
        stack.setItemMeta(meta);
        return stack;
    }

    public ItemStack buildShooterBranchButton(Player player, int slot) {
        UpgradeInfo info = shooterUpgradeInfo(player, slot);
        ItemStack stack = new ItemStack(info.icon() == null ? Material.BARRIER : info.icon());
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return stack;
        }
        String branchName = switch (shooterBranchForSlot(slot)) {
            case BRANCH_FIRE -> "火豌";
            case BRANCH_NORMAL -> "普豌";
            case BRANCH_SNIPER -> "狙击";
            default -> "冰豌";
        };
        meta.setItemName(branchName + "升级");
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "分支：" + branchName);
        if (info.maxed()) {
            lore.add(ChatColor.GOLD + "已满级");
        } else if (info.valid()) {
            lore.add(ChatColor.GRAY + "当前等级：" + info.currentName());
            lore.add(ChatColor.YELLOW + "下一等级：" + info.nextName());
            lore.add(ChatColor.AQUA + "所需货币：" + info.price() + " 银币");
            lore.add(ChatColor.GREEN + "点击升级");
        } else {
            lore.add(ChatColor.RED + info.error());
        }
        meta.setLore(lore);
        stack.setItemMeta(meta);
        return stack;
    }

    public void handleEquipmentUpgradeClick(Player player, EquipmentCatalog.Kind kind) {
        if (kind == EquipmentCatalog.Kind.SHOOTER) {
            handleShooterUpgrade(player, shooterSlotForBranch(currentShooterBranch(player)));
            return;
        }
        UpgradeInfo info = upgradeInfo(player, kind);
        if (!info.valid()) {
            player.sendMessage(ChatColor.RED + "【装备升级】" + info.error());
            playPurchaseFail(player);
            return;
        }
        if (info.maxed()) {
            player.sendMessage(ChatColor.GOLD + "【装备升级】该部位已满级");
            playPurchaseFail(player);
            return;
        }
        if (totalSilverValue(player) < info.price()) {
            player.sendMessage(ChatColor.RED + "【装备升级】货币不足");
            playPurchaseFail(player);
            return;
        }
        if (!tryPay(player, info.price())) {
            player.sendMessage(ChatColor.RED + "【装备升级】扣除货币失败，请重试");
            playPurchaseFail(player);
            return;
        }
        if (!isArmor(kind)) {
            int wk = weaponKindForEquipment(kind);
            WeaponRef found = findWeapon(player, wk);
            if (found == null) {
                ItemStack base = wk == KIND_SHOOTER ? createShooterItem(0) : unbreakableWeapon(ironVariant(wk));
                Map<Integer, ItemStack> leftover = player.getInventory().addItem(base);
                for (ItemStack left : leftover.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), left);
                }
            } else {
                ItemStack next;
                if (wk == KIND_SHOOTER) {
                    int curTier = shooterTier(found.stack());
                    next = createShooterItem(Math.min(2, curTier + 1));
                } else {
                    next = new ItemStack(info.nextMaterial());
                    ItemMeta nextMeta = next.getItemMeta();
                    nextMeta.setUnbreakable(true);
                    next.setItemMeta(nextMeta);
                }
                copyEnchants(found.stack(), next);
                copyDamageUpgrade(found.stack(), next);
                if (found.slot() == -1) {
                    player.getInventory().setItemInMainHand(next);
                } else {
                    player.getInventory().setItem(found.slot(), next);
                }
            }
        } else {
            replaceUpgraded(player, kind, info.nextMaterial());
        }
        player.sendMessage(ChatColor.GREEN + "【装备升级】已升级：" + info.currentName() + " → " + info.nextName());
        playPurchaseSuccess(player);
    }

    public ItemStack buildEnchantButton(Player player, EquipmentCatalog.Kind kind,
                                        EquipmentCatalog.EnchantEntry entry) {
        ItemStack stack = new ItemStack(Material.ENCHANTED_BOOK);
        ItemMeta meta = stack.getItemMeta();
        ItemStack target = findEquipmentForKind(player, kind);
        int current = target == null || target.getItemMeta() == null
                ? 0 : target.getItemMeta().getEnchantLevel(entry.enchantment());
        String display = enchantDisplayName(entry.enchantment());
        meta.setItemName(display);
        List<String> lore = new ArrayList<>();
        if (current >= entry.maxLevel()) {
            lore.add(ChatColor.GOLD + "已满级（" + current + " 级）");
        } else {
            int price = EquipmentCatalog.enchantPrice(entry, current + 1);
            lore.add(ChatColor.GRAY + "当前等级：" + current + " → " + (current + 1));
            lore.add(ChatColor.AQUA + "所需货币：" + price + " 银币");
            lore.add(ChatColor.GREEN + "点击升级");
        }
        if (target == null) {
            lore.add(ChatColor.RED + "请先手持/穿戴对应" + kind.display());
        }
        meta.setLore(lore);
        stack.setItemMeta(meta);
        return stack;
    }

    public void handleEnchantClick(Player player, EquipmentCatalog.Kind kind,
                                   EquipmentCatalog.EnchantEntry entry) {
        ItemStack target = findEquipmentForKind(player, kind);
        if (target == null || target.getItemMeta() == null) {
            player.sendMessage(ChatColor.RED + "【附魔升级】请先持有/穿戴" + kind.display());
            playPurchaseFail(player);
            return;
        }
        int current = target.getItemMeta().getEnchantLevel(entry.enchantment());
        if (current >= entry.maxLevel()) {
            player.sendMessage(ChatColor.GOLD + "【附魔升级】" + enchantDisplayName(entry.enchantment()) + " 已满级");
            playPurchaseFail(player);
            return;
        }
        int price = EquipmentCatalog.enchantPrice(entry, current + 1);
        if (totalSilverValue(player) < price) {
            player.sendMessage(ChatColor.RED + "【附魔升级】货币不足");
            playPurchaseFail(player);
            return;
        }
        if (!tryPay(player, price)) {
            player.sendMessage(ChatColor.RED + "【附魔升级】扣除货币失败，请重试");
            playPurchaseFail(player);
            return;
        }
        ItemMeta meta = target.getItemMeta();
        meta.addEnchant(entry.enchantment(), current + 1, true);
        target.setItemMeta(meta);
        player.sendMessage(ChatColor.GREEN + "【附魔升级】" + enchantDisplayName(entry.enchantment())
                + " → " + (current + 1) + " 级");
        playPurchaseSuccess(player);
    }

    public ItemStack buildWeaponDamageButton(Player player, EquipmentCatalog.Kind kind) {
        ItemStack stack = new ItemStack(Material.DIAMOND);
        ItemMeta meta = stack.getItemMeta();
        ItemStack target = findEquipmentForKind(player, kind);
        int level = target == null ? 0 : weaponDamageLevelFor(kind, target);
        int maxLevel = damageMaxForKind(kind);
        int next = level + 1;
        int costSilver = next <= 5 ? 50 : (level >= 10 ? 200 : 100);
        String costText = next <= 5 ? "5 金币" : (level >= 10 ? "2 钻币" : "1 钻币");
        boolean tierReady = weaponDamageTierReady(kind, target);
        List<String> lore = new ArrayList<>();
        if (kind == EquipmentCatalog.Kind.TIMID_SHROOM) {
            int tier3 = level / 3;
            double minCd = Math.max(0.05, 0.3 - 0.05 * tier3);
            double baseCd = Math.max(minCd, 1.5 - 0.15 * tier3);
            meta.setItemName("胆小菇升级（当前等级 " + level + "）");
            lore.add(ChatColor.GRAY + "升级不再增加伤害，改为降低冷却");
            if (tierReady && level < maxLevel) {
                lore.add(ChatColor.GRAY + "当前：基础冷却 " + formatBonus(baseCd)
                        + " 秒 / 最低冷却 " + formatBonus(minCd) + " 秒");
                lore.add(ChatColor.AQUA + "所需货币：" + costText);
                lore.add(ChatColor.GREEN + "点击升级");
            } else if (level >= maxLevel) {
                lore.add(ChatColor.GOLD + "已满级");
            }
        } else {
            double bonus = rangedBonusForLevel(kind, level);
            double nextBonus = rangedBonusForLevel(kind, next);
            meta.setItemName("武器攻击力提升（当前 +" + formatBonus(bonus) + "）");
            if (!tierReady) {
                lore.add(ChatColor.RED + "需先将武器材质升至最高（下界合金）");
            } else if (level >= maxLevel) {
                lore.add(ChatColor.GOLD + "已满级（+" + formatBonus(bonus) + " 伤害）");
            } else {
                lore.add(ChatColor.GRAY + "当前加成：+" + formatBonus(bonus) + " 伤害");
                lore.add(ChatColor.AQUA + "所需货币：" + costText
                        + "（+" + formatBonus(nextBonus - bonus) + " 伤害）");
                lore.add(ChatColor.GREEN + "点击升级");
            }
        }
        if (!tierReady) {
            // 材质未满级时上方已提示
        } else if (target == null && kind != EquipmentCatalog.Kind.TIMID_SHROOM) {
            lore.add(ChatColor.RED + "请先持有/穿戴对应" + kind.display());
        }
        meta.setLore(lore);
        stack.setItemMeta(meta);
        return stack;
    }

    public void handleWeaponDamageClick(Player player, EquipmentCatalog.Kind kind) {
        ItemStack target = findEquipmentForKind(player, kind);
        if (target == null) {
            player.sendMessage(ChatColor.RED + "【武器强化】请先持有/穿戴" + kind.display());
            playPurchaseFail(player);
            return;
        }
        if (!weaponDamageTierReady(kind, target)) {
            player.sendMessage(ChatColor.RED + "【武器强化】需先将武器材质升至最高（下界合金）");
            playPurchaseFail(player);
            return;
        }
        int level = weaponDamageLevelFor(kind, target);
        int maxLevel = damageMaxForKind(kind);
        if (level >= maxLevel) {
            player.sendMessage(ChatColor.GOLD + "【武器强化】该武器攻击力已满级");
            playPurchaseFail(player);
            return;
        }
        int next = level + 1;
        int costSilver = next <= 5 ? 50 : (level >= 10 ? 200 : 100);
        String costText = next <= 5 ? "5 金币" : (level >= 10 ? "2 钻币" : "1 钻币");
        if (totalSilverValue(player) < costSilver) {
            player.sendMessage(ChatColor.RED + "【武器强化】货币不足，需要 " + costText);
            playPurchaseFail(player);
            return;
        }
        if (!tryPay(player, costSilver)) {
            player.sendMessage(ChatColor.RED + "【武器强化】扣除货币失败，请重试");
            playPurchaseFail(player);
            return;
        }
        ItemMeta meta = target.getItemMeta();
        NamespacedKey damageKey = damageKeyForKind(kind);
        meta.getPersistentDataContainer().set(
                damageKey == null ? weaponDamageKey : damageKey,
                PersistentDataType.INTEGER, next);
        target.setItemMeta(meta);
        if (kind == EquipmentCatalog.Kind.CACTUS_SHOOTER
                || kind == EquipmentCatalog.Kind.BIG_PUFFSHROOM
                || kind == EquipmentCatalog.Kind.SMALL_PUFFSHROOM
                || kind == EquipmentCatalog.Kind.TIMID_SHROOM) {
            // 仙人掌射手伤害以 PDC 记录，由射击逻辑读取，不添加近战属性修饰符
        } else {
            applyWeaponDamageModifier(target);
        }
        double bonus = rangedBonusForLevel(kind, level);
        double nextBonus = rangedBonusForLevel(kind, next);
        player.sendMessage(ChatColor.GREEN + "【武器强化】伤害加成 +"
                + formatBonus(nextBonus - bonus) + " → +" + formatBonus(nextBonus)
                + "（本次消耗 " + costText + "）");
        playPurchaseSuccess(player);
    }

    private static int weaponDamageLevelFor(EquipmentCatalog.Kind kind, ItemStack item) {
        NamespacedKey key = damageKeyForKind(kind);
        return key == null ? weaponDamageLevel(item)
                : item == null || item.getItemMeta() == null ? 0
                : item.getItemMeta().getPersistentDataContainer()
                .getOrDefault(key, PersistentDataType.INTEGER, 0);
    }

    private static NamespacedKey damageKeyForKind(EquipmentCatalog.Kind kind) {
        return switch (kind) {
            case CACTUS_SHOOTER -> cactusDamageKey;
            case BIG_PUFFSHROOM -> bigPuffDamageKey;
            case SMALL_PUFFSHROOM -> smallPuffDamageKey;
            case TIMID_SHROOM -> timidDamageKey;
            default -> null;
        };
    }

    private static int damageMaxForKind(EquipmentCatalog.Kind kind) {
        return 15;
    }

    public static double rangedBonusForLevel(EquipmentCatalog.Kind kind, int level) {
        if (kind == EquipmentCatalog.Kind.BOW || kind == EquipmentCatalog.Kind.SMALL_PUFFSHROOM) {
            return level <= 5 ? level : 5 + (level - 5) * 2.0;
        }
        if (kind == EquipmentCatalog.Kind.CROSSBOW
                || kind == EquipmentCatalog.Kind.CACTUS_SHOOTER
                || kind == EquipmentCatalog.Kind.BIG_PUFFSHROOM) {
            return level <= 5 ? level * 0.5 : 2.5 + (level - 5);
        }
        if (kind == EquipmentCatalog.Kind.SWORD) {
            return level <= 5 ? level * 0.5 : 2.5 + (level - 5);
        }
        return level;
    }

    /** 远程武器最终伤害 = 基础伤害 + 等级加成（纯计算，便于测试）。 */
    public static double rangedWeaponDamage(double baseDamage, EquipmentCatalog.Kind kind, int level) {
        return baseDamage + rangedBonusForLevel(kind, level);
    }

    /** 重锤风暴击飞后 3 秒内免疫坠落伤害。 */
    void grantWindMaceFallImmunity(Player player) {
        windMaceFallImmunity.put(player.getUniqueId(), System.currentTimeMillis() + 3000L);
    }

    public boolean isWindMaceFallImmune(Player player) {
        Long expiry = windMaceFallImmunity.get(player.getUniqueId());
        if (expiry == null) {
            return false;
        }
        if (System.currentTimeMillis() > expiry) {
            windMaceFallImmunity.remove(player.getUniqueId());
            return false;
        }
        return true;
    }

    private static String formatBonus(double value) {
        if (value == Math.floor(value) && !Double.isInfinite(value)) {
            return String.valueOf((int) value);
        }
        return String.valueOf(value);
    }

    private boolean weaponDamageTierReady(EquipmentCatalog.Kind kind, ItemStack target) {
        if (kind == EquipmentCatalog.Kind.CACTUS_SHOOTER
                || kind == EquipmentCatalog.Kind.BIG_PUFFSHROOM
                || kind == EquipmentCatalog.Kind.SMALL_PUFFSHROOM
                || kind == EquipmentCatalog.Kind.TIMID_SHROOM) {
            return true;
        }
        if (kind == EquipmentCatalog.Kind.BOW
                || kind == EquipmentCatalog.Kind.CROSSBOW
                || kind == EquipmentCatalog.Kind.TRIDENT
                || kind == EquipmentCatalog.Kind.MACE) {
            return true;
        }
        if (target == null) {
            return false;
        }
        int wk = weaponKindForEquipment(kind);
        return weaponTier(target, wk) >= maxWeaponTier(wk);
    }

    public EquipmentCatalog.Kind heldWeaponKind(ItemStack item) {
        if (item == null) {
            return null;
        }
        Material m = item.getType();
        if (m == Material.IRON_SWORD || m == Material.DIAMOND_SWORD || m == Material.NETHERITE_SWORD
                || m == Material.STONE_SWORD) {
            return EquipmentCatalog.Kind.SWORD;
        }
        if (m == Material.IRON_AXE || m == Material.DIAMOND_AXE || m == Material.NETHERITE_AXE
                || m == Material.STONE_AXE) {
            return EquipmentCatalog.Kind.AXE;
        }
        if (m == Material.IRON_SPEAR || m == Material.DIAMOND_SPEAR || m == Material.NETHERITE_SPEAR
                || m == Material.WOODEN_SPEAR || m == Material.STONE_SPEAR || m == Material.GOLDEN_SPEAR
                || m == Material.COPPER_SPEAR) {
            return EquipmentCatalog.Kind.SPEAR;
        }
        if (m == Material.BOW) {
            return EquipmentCatalog.Kind.BOW;
        }
        if (m == Material.CROSSBOW) {
            return EquipmentCatalog.Kind.CROSSBOW;
        }
        if (m == Material.TRIDENT) {
            return EquipmentCatalog.Kind.TRIDENT;
        }
        if (m == Material.MACE) {
            return EquipmentCatalog.Kind.MACE;
        }
        if (m == Material.CACTUS && item.getItemMeta() != null
                && item.getItemMeta().getPersistentDataContainer().has(cactusShooterKey, PersistentDataType.BYTE)) {
            return EquipmentCatalog.Kind.CACTUS_SHOOTER;
        }
        return null;
    }

    private static int weaponDamageLevel(ItemStack item) {
        if (item == null || item.getItemMeta() == null) {
            return 0;
        }
        return item.getItemMeta().getPersistentDataContainer()
                .getOrDefault(weaponDamageKey, PersistentDataType.INTEGER, 0);
    }

    private static final java.util.UUID WEAPON_DAMAGE_MODIFIER_UUID =
            java.util.UUID.nameUUIDFromBytes("dave_weapon_damage_modifier".getBytes());

    private static void applyWeaponDamageModifier(ItemStack item) {
        if (item == null || item.getItemMeta() == null) {
            return;
        }
        ItemMeta meta = item.getItemMeta();
        meta.removeAttributeModifier(Attribute.ATTACK_DAMAGE, new AttributeModifier(
                WEAPON_DAMAGE_MODIFIER_UUID, "dave_weapon_damage", 0,
                AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.MAINHAND));
        double attackSpeed = vanillaAttackSpeed(item.getType());
        if (attackSpeed > 0) {
            Multimap<Attribute, AttributeModifier> modifiers = meta.getAttributeModifiers();
            if (modifiers != null) {
                modifiers.removeAll(Attribute.ATTACK_SPEED);
                modifiers.put(Attribute.ATTACK_SPEED, new AttributeModifier(
                        java.util.UUID.nameUUIDFromBytes("attack_speed_lock".getBytes()), "attack_speed_lock",
                        attackSpeed,
                        AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.MAINHAND));
                meta.setAttributeModifiers(modifiers);
            }
        }
        item.setItemMeta(meta);
    }

    private static boolean isMeleeWeapon(Material material) {
        return switch (material) {
            case IRON_SWORD, DIAMOND_SWORD, NETHERITE_SWORD, STONE_SWORD, WOODEN_SWORD, GOLDEN_SWORD,
                    IRON_AXE, DIAMOND_AXE, NETHERITE_AXE, STONE_AXE, WOODEN_AXE, GOLDEN_AXE,
                    IRON_SPEAR, DIAMOND_SPEAR, NETHERITE_SPEAR, WOODEN_SPEAR, STONE_SPEAR, GOLDEN_SPEAR, COPPER_SPEAR,
                    TRIDENT, MACE -> true;
            default -> false;
        };
    }

    private static double vanillaAttackSpeed(Material material) {
        return switch (material) {
            case IRON_SWORD, DIAMOND_SWORD, NETHERITE_SWORD, STONE_SWORD, WOODEN_SWORD, GOLDEN_SWORD -> 1.6;
            case IRON_AXE, DIAMOND_AXE, NETHERITE_AXE, STONE_AXE, WOODEN_AXE, GOLDEN_AXE -> 1.0;
            case IRON_SPEAR, DIAMOND_SPEAR, NETHERITE_SPEAR, WOODEN_SPEAR, STONE_SPEAR, GOLDEN_SPEAR, COPPER_SPEAR -> 1.1;
            case TRIDENT -> 1.1;
            case MACE -> 0.6;
            default -> -1;
        };
    }

    private static void copyDamageUpgrade(ItemStack old, ItemStack next) {
        if (old == null || old.getItemMeta() == null || next == null) {
            return;
        }
        int level = weaponDamageLevel(old);
        if (level <= 0) {
            return;
        }
        ItemMeta meta = next.getItemMeta();
        meta.getPersistentDataContainer().set(weaponDamageKey, PersistentDataType.INTEGER, level);
        next.setItemMeta(meta);
        applyWeaponDamageModifier(next);
    }

    public boolean tryWeaponSkill(Player player, EquipmentCatalog.Kind kind, boolean rightClick) {
        ItemStack held = player.getInventory().getItemInMainHand();
        return tryWeaponSkill(player, kind, rightClick, held);
    }

    public boolean tryWeaponSkill(Player player, EquipmentCatalog.Kind kind, boolean rightClick, ItemStack weapon) {
        if (kind == EquipmentCatalog.Kind.SHOOTER) {
            return false;
        }
        if (kind == EquipmentCatalog.Kind.SWORD) {
            if (!rightClick) {
                return false;
            }
        } else if (kind == EquipmentCatalog.Kind.AXE) {
            // 斧子：右键劈砍、Q 键投掷
        } else {
            if (rightClick) {
                return false;
            }
        }
        int requiredLevel = (kind == EquipmentCatalog.Kind.AXE && rightClick) ? 10 : 5;
        if (weapon == null || weaponDamageLevel(weapon) < requiredLevel) {
            return false;
        }
        if (kind == EquipmentCatalog.Kind.SWORD || kind == EquipmentCatalog.Kind.AXE
                || kind == EquipmentCatalog.Kind.SPEAR) {
            if (!weaponDamageTierReady(kind, weapon)) {
                player.sendMessage(ChatColor.RED + "【武器技能】需先将武器材质升至最高（下界合金）");
                return true;
            }
        }
        int skillTier = weaponDamageLevel(weapon) >= 15 ? 15
                : weaponDamageLevel(weapon) >= 10 ? 10 : 5;
        String skillKey = player.getUniqueId() + ":" + kind.name()
                + (kind == EquipmentCatalog.Kind.AXE ? (rightClick ? ":slam" : ":throw") : "");
        long now = System.currentTimeMillis();
        long cooldownMs = switch (kind) {
            case SWORD -> skillTier >= 15 ? 10_000L : skillTier >= 10 ? 15_000L : 17_000L;
            case TRIDENT -> skillTier >= 15 ? 15_000L : 20_000L;
            case CROSSBOW, BOW -> skillTier >= 15 ? 10_000L : 20_000L;
            case AXE -> rightClick ? (skillTier >= 15 ? 4_000L : 7_000L) : 3_000L;
            case MACE -> skillTier >= 15 ? 5_000L : 10_000L;
            case SPEAR -> skillTier >= 15 ? 1_500L : 3_000L;
            default -> 0L;
        };
        long last = skillCooldowns.getOrDefault(skillKey, 0L);
        long remaining = cooldownMs - (now - last);
        if (remaining > 0) {
            player.sendActionBar(ChatColor.YELLOW + "【" + kind.display() + "技能】冷却中，还需 "
                    + Math.max(1, (remaining + 999) / 1000) + " 秒");
            return true;
        }
        skillCooldowns.put(skillKey, now);
        switch (kind) {
            case SWORD -> startSwordSlash(player, skillTier);
            case AXE -> {
                if (rightClick) {
                    startAxeSlam(player, skillTier);
                } else {
                    startAxeThrow(player);
                }
            }
            case TRIDENT -> startTridentStorm(player, skillTier);
            case BOW -> startBowExplosiveArrows(player, skillTier);
            case CROSSBOW -> startCrossbowRapidFire(player, skillTier);
            case MACE -> startMaceExplosion(player, skillTier);
            case SPEAR -> startSpearDash(player, skillTier);
            default -> { }
        }
        return true;
    }

    private void startAxeThrow(Player player) {
        Location eye = player.getEyeLocation();
        org.bukkit.util.Vector dir = player.getLocation().getDirection().clone().normalize();
        Snowball bullet = eye.getWorld().spawn(eye, Snowball.class, s -> {
            s.setItem(new ItemStack(Material.NETHERITE_AXE));
            s.setVelocity(dir.multiply(2.0));
            s.setShooter(player);
            s.setGravity(true);
            s.getPersistentDataContainer().set(axeBulletKey, PersistentDataType.BYTE, (byte) 1);
            s.getPersistentDataContainer().set(shooterDamageKey, PersistentDataType.DOUBLE, 15.0);
        });
        trackAxePierce(player, bullet);
        player.sendMessage(ChatColor.GREEN + "【斧技能】投掷飞斧！");
    }

    private void trackAxePierce(Player player, Snowball bullet) {
        Bukkit.getScheduler().runTaskTimer(plugin, task -> {
            if (!bullet.isValid() || bullet.isDead()) {
                task.cancel();
                return;
            }
            for (Entity entity : bullet.getWorld().getNearbyEntities(bullet.getLocation(), 1.5, 1.5, 1.5)) {
                if (!(entity instanceof Mob mob) || !isMonster(mob)) {
                    continue;
                }
                if (hasCactusHit(bullet, mob)) {
                    continue;
                }
                markCactusHit(bullet, mob);
                recordPlayerDamage(mob, player, 15.0);
                mob.damage(15.0, player);
            }
        }, 1L, 1L);
    }

    private void startMaceExplosion(Player player, int tier) {
        Location loc = player.getLocation();
        player.getWorld().spawnParticle(Particle.EXPLOSION, loc, 1, 0, 0, 0, 0);
        double radius = tier >= 15 ? 13.0 : tier >= 10 ? 7.0 : 4.0;
        double centerDamage = 65.0 * (tier >= 15 ? 3.0 : tier >= 10 ? 1.5 : 0.75);
        for (Entity entity : player.getWorld().getNearbyEntities(loc, radius, radius, radius)) {
            if (!(entity instanceof Mob mob) || !isMonster(mob)) {
                continue;
            }
            double dist = mob.getLocation().distance(loc);
            double dmg = (1.0 - dist / radius) * centerDamage;
            if (dmg <= 0) {
                continue;
            }
            damageLogs.computeIfAbsent(mob.getUniqueId(), k -> new HashMap<>())
                    .merge(player.getUniqueId(), dmg, Double::sum);
            mob.damage(dmg, player);
            mob.setVelocity(mob.getLocation().toVector().subtract(loc.toVector())
                    .normalize().multiply(1.5).setY(0.5));
        }
        player.sendMessage(ChatColor.GREEN + "【重锤技能】爆发！");
    }

    private void startSwordSlash(Player player, int tier) {
        double damage = tier >= 10 ? 10.0 : 5.0;
        int iterations = tier >= 15 ? 14 : tier >= 10 ? 10 : 7;
        player.sendMessage(ChatColor.GREEN + "【剑技能】剑刃风暴开启（" + (iterations / 2) + " 秒）");
        for (int i = 0; i < iterations; i++) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!player.isOnline()) {
                    return;
                }
                player.getWorld().spawnParticle(Particle.SWEEP_ATTACK, player.getLocation().add(0, 1, 0), 20, 2, 1, 2, 0);
                for (Entity entity : player.getWorld().getNearbyEntities(player.getLocation(), 5, 5, 5)) {
                    if (!(entity instanceof Mob mob) || !isMonster(mob)) {
                        continue;
                    }
                    damageLogs.computeIfAbsent(mob.getUniqueId(), k -> new HashMap<>())
                            .merge(player.getUniqueId(), damage, Double::sum);
                    mob.damage(damage, player);
                    mob.setVelocity(mob.getLocation().toVector().subtract(player.getLocation().toVector())
                            .normalize().multiply(1.2).setY(0.4));
                }
            }, i * 10L);
        }
    }

    private void startAxeSlam(Player player, int tier) {
        double damage = tier >= 15 ? 100.0 : 50.0;
        double damageRadius = tier >= 15 ? 6.5 : 4.5;
        player.sendMessage(ChatColor.GREEN + "【斧技能】跃起！落地造成范围伤害");
        player.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, 20, 10, false, true, true));
        boolean[] slammed = {false};
        Bukkit.getScheduler().runTaskTimer(plugin, task -> {
            if (!player.isOnline()) {
                task.cancel();
                return;
            }
            if (player.isOnGround()) {
                task.cancel();
                if (!slammed[0]) {
                    slammed[0] = true;
                    slamLanding(player, damage, damageRadius);
                }
            }
        }, 20L, 1L);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline() && !slammed[0]) {
                slammed[0] = true;
                slamLanding(player, damage, damageRadius);
            }
        }, 60L);
    }

    private void slamLanding(Player player, double damage, double damageRadius) {
        Location loc = player.getLocation();
        player.getWorld().spawnParticle(Particle.EXPLOSION, loc, 1, 0, 0, 0, 0);
        player.getWorld().spawnParticle(Particle.BLOCK, loc.add(0, 0.1, 0), 60, 3, 0.1, 3, 0,
                Material.DIRT.createBlockData());
        player.getWorld().spawnParticle(Particle.SWEEP_ATTACK, loc, 30, 3, 0.2, 3, 0);
        for (Entity entity : player.getWorld().getNearbyEntities(
                player.getLocation(), damageRadius, damageRadius, damageRadius)) {
            if (!(entity instanceof Mob mob) || !isMonster(mob)) {
                continue;
            }
            damageLogs.computeIfAbsent(mob.getUniqueId(), k -> new HashMap<>())
                    .merge(player.getUniqueId(), damage, Double::sum);
            mob.damage(damage, player);
        }
        // 击飞范围：13×13（半径 6.5），不重复造成伤害
        for (Entity entity : player.getWorld().getNearbyEntities(player.getLocation(), 6.5, 6.5, 6.5)) {
            if (!(entity instanceof Mob mob) || !isMonster(mob)) {
                continue;
            }
            mob.setVelocity(mob.getLocation().toVector().subtract(player.getLocation().toVector())
                    .normalize().multiply(2.0).setY(0.8));
        }
    }

    private void startTridentStorm(Player player, int tier) {
        int bolts = tier >= 15 ? 30 : tier >= 10 ? 20 : 10;
        player.sendMessage(ChatColor.GREEN + "【三叉戟技能】雷电领域开启（5 秒）");
        for (int i = 0; i < 10; i++) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!player.isOnline()) {
                    return;
                }
                for (int s = 0; s < bolts; s++) {
                    double x = player.getLocation().getX() + (ThreadLocalRandom.current().nextDouble() * 18 - 9);
                    double z = player.getLocation().getZ() + (ThreadLocalRandom.current().nextDouble() * 18 - 9);
                    LightningStrike strike = player.getWorld().strikeLightning(new Location(
                            player.getWorld(), x, player.getLocation().getY(), z));
                    if (strike != null) {
                        strike.getPersistentDataContainer().set(pluginLightningKey, PersistentDataType.BYTE, (byte) 1);
                    }
                    for (Entity entity : player.getWorld().getNearbyEntities(
                            new Location(player.getWorld(), x, player.getLocation().getY(), z), 4, 4, 4)) {
                        if (entity instanceof org.bukkit.entity.Wolf wolf && isPetWolf(wolf)) {
                            continue;
                        }
                        if (entity instanceof Mob mob && isMonster(mob)) {
                            damageLogs.computeIfAbsent(mob.getUniqueId(), k -> new HashMap<>())
                                    .merge(player.getUniqueId(), 5.0, Double::sum);
                            addPlayerScore(player, 5.0);
                        }
                    }
                }
            }, i * 10L);
        }
    }

    private void startBowExplosiveArrows(Player player, int tier) {
        int durationMs = tier >= 10 ? 10_000 : 5_000;
        double radius = tier >= 15 ? 7.0 : 4.0;
        bowExplosiveExpiry.put(player.getUniqueId(), System.currentTimeMillis() + durationMs);
        bowExplosiveCenterDamage.put(player.getUniqueId(), DamageMath.bowExplosionCenterDamage(tier));
        bowExplosiveRadius.put(player.getUniqueId(), radius);
        player.sendMessage(ChatColor.GREEN + "【弓技能】爆炸箭开启（" + (durationMs / 1000)
                + " 秒，命中产生爆炸）");
    }

    private void startCrossbowRapidFire(Player player, int tier) {
        int durationSeconds = tier >= 10 ? 10 : 5;
        long interval = tier >= 15 ? 2L : 4L;
        int arrowCount = (int) (durationSeconds * 20L / interval);
        player.sendMessage(ChatColor.GREEN + "【弩技能】快速装填开启（" + durationSeconds + " 秒）");
        Set<UUID> arrows = new HashSet<>();
        crossbowSkillArrows.put(player.getUniqueId(), arrows);
        for (int i = 0; i < arrowCount; i++) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!player.isOnline()) {
                    return;
                }
                Arrow arrow = spawnSkillArrow(player, 6.0);
                if (arrow != null) {
                    arrows.add(arrow.getUniqueId());
                }
            }, i * interval);
        }
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Set<UUID> spawned = crossbowSkillArrows.remove(player.getUniqueId());
            if (spawned != null) {
                for (UUID id : spawned) {
                    Entity arrow = Bukkit.getEntity(id);
                    if (arrow != null && arrow.isValid()) {
                        arrow.remove();
                    }
                }
            }
        }, durationSeconds * 20L);
    }

    private Arrow spawnSkillArrow(Player player, double damage) {
        Arrow arrow = player.getWorld().spawn(player.getEyeLocation(), Arrow.class, a -> {
            a.setShooter(player);
            a.setVelocity(player.getLocation().getDirection().normalize().multiply(2.0));
            a.setGravity(false);
            a.setDamage(damage);
            a.setPickupStatus(org.bukkit.entity.AbstractArrow.PickupStatus.DISALLOWED);
            a.setRotation(player.getLocation().getYaw(), player.getLocation().getPitch());
        });
        arrow.getPersistentDataContainer().set(skillArrowKey, PersistentDataType.BYTE, (byte) 1);
        return arrow;
    }

    private void startSpearDash(Player player, int tier) {
        double damage = tier >= 10 ? 30.0 : 15.0;
        player.sendMessage(ChatColor.GREEN + "【长矛技能】突进！");
        org.bukkit.util.Vector dir = player.getLocation().getDirection().clone().setY(0).normalize();
        player.setVelocity(dir.clone().multiply(DamageMath.SPEAR_DASH_VELOCITY));
        for (int i = 1; i <= 5; i++) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!player.isOnline()) {
                    return;
                }
                Location from = player.getLocation();
                player.getWorld().spawnParticle(Particle.SWEEP_ATTACK, from, 8, 1, 0.2, 1, 0);
                player.getWorld().spawnParticle(Particle.CRIT, from, 10, 1, 0.2, 1, 0);
                for (Entity entity : player.getWorld().getNearbyEntities(from, 2.5, 2.5, 2.5)) {
                    if (!(entity instanceof Mob mob) || !isMonster(mob)) {
                        continue;
                    }
                    damageLogs.computeIfAbsent(mob.getUniqueId(), k -> new HashMap<>())
                            .merge(player.getUniqueId(), damage, Double::sum);
                    mob.damage(damage, player);
                    mob.setVelocity(mob.getLocation().toVector().subtract(player.getLocation().toVector())
                            .normalize().multiply(1.6).setY(0.5));
                }
            }, i * 2L);
        }
    }

    public boolean isBowExplosiveActive(Player player) {
        Long expiry = bowExplosiveExpiry.get(player.getUniqueId());
        if (expiry == null) {
            return false;
        }
        if (System.currentTimeMillis() > expiry) {
            bowExplosiveExpiry.remove(player.getUniqueId());
            bowExplosiveCenterDamage.remove(player.getUniqueId());
            bowExplosiveRadius.remove(player.getUniqueId());
            return false;
        }
        return true;
    }

    public void handleExplosiveArrowHit(Arrow arrow) {
        if (!(arrow.getShooter() instanceof Player shooter) || !arrow.getPersistentDataContainer()
                .has(explosiveArrowKey, PersistentDataType.BYTE)) {
            return;
        }
        Location loc = arrow.getLocation();
        arrow.getWorld().createExplosion(loc, 0.0f, false, false);
        double radius = bowExplosiveRadius.getOrDefault(shooter.getUniqueId(), 4.0);
        double centerDamage = bowExplosiveCenterDamage.getOrDefault(shooter.getUniqueId(), 20.0);
        for (Entity entity : arrow.getWorld().getNearbyEntities(loc, radius, radius, radius)) {
            if (!(entity instanceof Mob mob) || !isMonster(mob)) {
                continue;
            }
            double dist = mob.getLocation().distance(loc);
            double dmg = (1.0 - dist / radius) * centerDamage;
            if (dmg <= 0) {
                continue;
            }
            damageLogs.computeIfAbsent(mob.getUniqueId(), k -> new HashMap<>())
                    .merge(shooter.getUniqueId(), dmg, Double::sum);
            mob.damage(dmg, shooter);
        }
    }

    private ItemStack findEquipmentForKind(Player player, EquipmentCatalog.Kind kind) {
        if (isArmor(kind)) {
            return switch (kind) {
                case CHESTPLATE -> player.getInventory().getChestplate();
                case LEGGINGS -> player.getInventory().getLeggings();
                case BOOTS -> player.getInventory().getBoots();
                default -> player.getInventory().getHelmet();
            };
        }
        WeaponRef found = findWeapon(player, weaponKindForEquipment(kind));
        return found == null ? null : found.stack();
    }

    static String enchantDisplayName(Enchantment enchantment) {
        String key = enchantment.getKey().getKey();
        return switch (key) {
            case "sharpness" -> "锋利";
            case "smite" -> "亡灵杀手";
            case "bane_of_arthropods" -> "截肢杀手";
            case "fire_aspect" -> "火焰附加";
            case "knockback" -> "击退";
            case "sweeping_edge" -> "横扫之刃";
            case "unbreaking" -> "耐久";
            case "mending" -> "经验修补";
            case "protection" -> "保护";
            case "blast_protection" -> "爆炸保护";
            case "projectile_protection" -> "弹射保护";
            case "thorns" -> "荆棘";
            case "swift_sneak" -> "迅捷潜行";
            case "power" -> "力量";
            case "punch" -> "冲击";
            case "flame" -> "火矢";
            case "infinity" -> "无限";
            case "piercing" -> "穿透";
            case "quick_charge" -> "快速装填";
            case "multishot" -> "多重射击";
            case "loyalty" -> "忠诚";
            case "impaling" -> "穿刺";
            case "channeling" -> "引雷";
            case "density" -> "致密";
            case "breach" -> "破甲";
            case "wind_burst" -> "风爆";
            case "lunge" -> "突刺";
            case "feather_falling" -> "摔落保护";
            default -> key;
        };
    }

    private UpgradeInfo upgradeInfo(Player player, EquipmentCatalog.Kind kind) {
        if (isArmor(kind)) {
            Material current;
            Material[] nextTable;
            int[] priceTable;
            switch (kind) {
                case CHESTPLATE -> {
                    current = player.getInventory().getChestplate() == null ? null : player.getInventory().getChestplate().getType();
                    nextTable = ARMOR_NEXT_CHEST;
                    priceTable = PRICE_LARGE;
                }
                case LEGGINGS -> {
                    current = player.getInventory().getLeggings() == null ? null : player.getInventory().getLeggings().getType();
                    nextTable = ARMOR_NEXT_LEGS;
                    priceTable = PRICE_LARGE;
                }
                case BOOTS -> {
                    current = player.getInventory().getBoots() == null ? null : player.getInventory().getBoots().getType();
                    nextTable = ARMOR_NEXT_BOOTS;
                    priceTable = PRICE_SMALL;
                }
                default -> {
                    current = player.getInventory().getHelmet() == null ? null : player.getInventory().getHelmet().getType();
                    nextTable = ARMOR_NEXT;
                    priceTable = PRICE_SMALL;
                }
            }
            int tier = armorTier(current);
            if (tier < 0) {
                return new UpgradeInfo(false, false, "未穿戴", "", 0, Material.BARRIER, null, "请先穿戴该部位盔甲");
            }
            if (tier >= 4) {
                return new UpgradeInfo(true, true, materialName(current), "", 0, current, null, "");
            }
            return new UpgradeInfo(true, false, materialName(current), materialName(nextTable[tier]),
                    priceTable[tier], current, nextTable[tier], "");
        }
        int wk = weaponKindForEquipment(kind);
        if (wk == KIND_SHOOTER) {
            return shooterUpgradeInfo(player, shooterSlotForBranch(currentShooterBranch(player)));
        }
        WeaponRef found = findWeapon(player, wk);
        if (found == null) {
            String baseName = wk == KIND_SHOOTER ? "豌豆射手" : "铁";
            int basePrice = wk == KIND_SPEAR ? PRICE_SPEAR[0]
                    : wk == KIND_SWORD ? (int) Math.ceil(PRICE_WEAPON[0] * 1.2) : PRICE_WEAPON[0];
            return new UpgradeInfo(true, false, "无", baseName, basePrice,
                    Material.BARRIER, wk == KIND_SHOOTER ? Material.DISPENSER : ironVariant(wk), "");
        }
        Material current = found.stack().getType();
        int tier = weaponTier(found.stack(), wk);
        if (tier >= maxWeaponTier(wk)) {
            return new UpgradeInfo(true, true, materialName(found.stack(), wk), "", 0, current, null, "");
        }
        Material next = weaponNext(found.stack(), wk);
        String nextName = wk == KIND_SHOOTER ? shooterName(tier + 1) : materialName(next);
        int price = wk == KIND_SPEAR ? PRICE_SPEAR[tier]
                : wk == KIND_SWORD ? (int) Math.ceil(PRICE_WEAPON[tier] * 1.2) : PRICE_WEAPON[tier];
        return new UpgradeInfo(true, false, materialName(found.stack(), wk), nextName,
                price, current, next, "");
    }

    private record WeaponRef(ItemStack stack, int slot) {
    }

    private static final int KIND_SWORD = 0;
    private static final int KIND_AXE = 1;
    private static final int KIND_SPEAR = 2;
    private static final int KIND_SHOOTER = 3;
    private static final int KIND_BOW = 4;
    private static final int KIND_CROSSBOW = 5;
    private static final int KIND_TRIDENT = 6;
    private static final int KIND_MACE = 7;
    private static final int KIND_CACTUS = 8;
    private static final int KIND_BIG_PUFF = 9;
    private static final int KIND_SMALL_PUFF = 10;
    private static final int KIND_TIMID = 11;

    private static int weaponKindForEquipment(EquipmentCatalog.Kind kind) {
        return switch (kind) {
            case AXE -> KIND_AXE;
            case SPEAR -> KIND_SPEAR;
            case SHOOTER -> KIND_SHOOTER;
            case BOW -> KIND_BOW;
            case CROSSBOW -> KIND_CROSSBOW;
            case TRIDENT -> KIND_TRIDENT;
            case MACE -> KIND_MACE;
            case CACTUS_SHOOTER -> KIND_CACTUS;
            case BIG_PUFFSHROOM -> KIND_BIG_PUFF;
            case SMALL_PUFFSHROOM -> KIND_SMALL_PUFF;
            case TIMID_SHROOM -> KIND_TIMID;
            default -> KIND_SWORD;
        };
    }

    private static boolean isArmor(EquipmentCatalog.Kind kind) {
        return kind == EquipmentCatalog.Kind.HELMET
                || kind == EquipmentCatalog.Kind.CHESTPLATE
                || kind == EquipmentCatalog.Kind.LEGGINGS
                || kind == EquipmentCatalog.Kind.BOOTS;
    }

    private static int shooterSlotForBranch(int branch) {
        return branch == BRANCH_FIRE ? EquipmentCatalog.SHOOTER_FIRE_SLOT
                : branch == BRANCH_NORMAL ? EquipmentCatalog.SHOOTER_NORMAL_SLOT
                : EquipmentCatalog.SHOOTER_ICE_SLOT;
    }

    private int currentShooterBranch(Player player) {
        WeaponRef found = findWeapon(player, KIND_SHOOTER);
        if (found == null) {
            return BRANCH_NONE;
        }
        return shooterBranch(found.stack());
    }

    private static Material ironVariant(int kind) {
        return switch (kind) {
            case KIND_SWORD -> Material.IRON_SWORD;
            case KIND_AXE -> Material.IRON_AXE;
            default -> Material.IRON_SPEAR;
        };
    }

    private static ItemStack unbreakableWeapon(Material material) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        meta.setUnbreakable(true);
        stack.setItemMeta(meta);
        return stack;
    }

    private WeaponRef findWeapon(Player player, int kind) {
        int bestTier = -1;
        WeaponRef best = null;
        ItemStack main = player.getInventory().getItemInMainHand();
        if (main != null && isWeaponType(main, kind)) {
            best = new WeaponRef(main, -1);
            bestTier = weaponTier(main, kind);
        }
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item == null || item.getType() == Material.AIR || !isWeaponType(item, kind)) {
                continue;
            }
            int tier = weaponTier(item, kind);
            if (tier > bestTier) {
                bestTier = tier;
                best = new WeaponRef(item, i);
            }
        }
        return best;
    }

    private static boolean isWeaponType(ItemStack item, int kind) {
        if (item == null) {
            return false;
        }
        if (kind == KIND_SHOOTER) {
            return (item.getType() == Material.DISPENSER || item.getType() == Material.SPYGLASS)
                    && item.getItemMeta() != null
                    && item.getItemMeta().getPersistentDataContainer()
                    .getOrDefault(shooterTierKey, PersistentDataType.INTEGER, -1) >= 0;
        }
        Material material = item.getType();
        return switch (kind) {
            case KIND_SWORD -> material == Material.STONE_SWORD || material == Material.IRON_SWORD
                    || material == Material.DIAMOND_SWORD || material == Material.NETHERITE_SWORD;
            case KIND_AXE -> material == Material.STONE_AXE || material == Material.IRON_AXE
                    || material == Material.DIAMOND_AXE || material == Material.NETHERITE_AXE;
            case KIND_SPEAR -> material == Material.WOODEN_SPEAR || material == Material.STONE_SPEAR
                    || material == Material.GOLDEN_SPEAR || material == Material.COPPER_SPEAR
                    || material == Material.IRON_SPEAR || material == Material.DIAMOND_SPEAR
                    || material == Material.NETHERITE_SPEAR;
            case KIND_BOW -> material == Material.BOW;
            case KIND_CROSSBOW -> material == Material.CROSSBOW;
            case KIND_TRIDENT -> material == Material.TRIDENT;
            case KIND_MACE -> material == Material.MACE;
            case KIND_CACTUS -> material == Material.CACTUS
                    && item.getItemMeta() != null
                    && item.getItemMeta().getPersistentDataContainer()
                    .has(cactusShooterKey, PersistentDataType.BYTE);
            case KIND_BIG_PUFF -> material == Material.AMETHYST_CLUSTER
                    && item.getItemMeta() != null
                    && item.getItemMeta().getPersistentDataContainer()
                    .has(bigPuffKey, PersistentDataType.BYTE);
            case KIND_SMALL_PUFF -> material == Material.BROWN_MUSHROOM
                    && item.getItemMeta() != null
                    && item.getItemMeta().getPersistentDataContainer()
                    .has(smallPuffKey, PersistentDataType.BYTE);
            case KIND_TIMID -> material == Material.AMETHYST_SHARD
                    && item.getItemMeta() != null
                    && item.getItemMeta().getPersistentDataContainer()
                    .has(timidKey, PersistentDataType.BYTE);
            default -> false;
        };
    }

    private static int armorTier(Material material) {
        if (material == null) {
            return -1;
        }
        String name = material.name();
        if (!name.endsWith("_HELMET") && !name.endsWith("_CHESTPLATE")
                && !name.endsWith("_LEGGINGS") && !name.endsWith("_BOOTS")) {
            return -1;
        }
        return switch (material) {
            case LEATHER_HELMET, LEATHER_CHESTPLATE, LEATHER_LEGGINGS, LEATHER_BOOTS -> 0;
            case CHAINMAIL_HELMET, CHAINMAIL_CHESTPLATE, CHAINMAIL_LEGGINGS, CHAINMAIL_BOOTS -> 1;
            case IRON_HELMET, IRON_CHESTPLATE, IRON_LEGGINGS, IRON_BOOTS -> 2;
            case DIAMOND_HELMET, DIAMOND_CHESTPLATE, DIAMOND_LEGGINGS, DIAMOND_BOOTS -> 3;
            case NETHERITE_HELMET, NETHERITE_CHESTPLATE, NETHERITE_LEGGINGS, NETHERITE_BOOTS -> 4;
            default -> -1;
        };
    }

    private static int weaponTier(ItemStack item, int kind) {
        if (kind == KIND_SHOOTER) {
            return item.getItemMeta() == null ? -1
                    : item.getItemMeta().getPersistentDataContainer()
                    .getOrDefault(shooterTierKey, PersistentDataType.INTEGER, -1);
        }
        if (kind == KIND_CACTUS) {
            return item.getItemMeta() == null ? -1
                    : item.getItemMeta().getPersistentDataContainer()
                    .getOrDefault(cactusDamageKey, PersistentDataType.INTEGER, 0);
        }
        if (kind == KIND_BIG_PUFF || kind == KIND_SMALL_PUFF || kind == KIND_TIMID) {
            NamespacedKey damageKey = kind == KIND_BIG_PUFF ? bigPuffDamageKey
                    : kind == KIND_SMALL_PUFF ? smallPuffDamageKey : timidDamageKey;
            return item.getItemMeta() == null ? -1
                    : item.getItemMeta().getPersistentDataContainer()
                    .getOrDefault(damageKey, PersistentDataType.INTEGER, 0);
        }
        Material material = item.getType();
        if (kind == KIND_BOW || kind == KIND_CROSSBOW || kind == KIND_TRIDENT || kind == KIND_MACE) {
            return 0;
        }
        if (kind == KIND_SPEAR) {
            return switch (material) {
                case WOODEN_SPEAR, STONE_SPEAR, GOLDEN_SPEAR, COPPER_SPEAR -> 0;
                case IRON_SPEAR -> 1;
                case DIAMOND_SPEAR -> 2;
                case NETHERITE_SPEAR -> 3;
                default -> -1;
            };
        }
        return switch (material) {
            case STONE_SWORD, STONE_AXE -> 0;
            case IRON_SWORD, IRON_AXE -> 1;
            case DIAMOND_SWORD, DIAMOND_AXE -> 2;
            case NETHERITE_SWORD, NETHERITE_AXE -> 3;
            default -> -1;
        };
    }

    private static Material weaponNext(ItemStack item, int kind) {
        if (kind == KIND_SHOOTER) {
            return Material.DISPENSER;
        }
        Material material = item.getType();
        if (kind == KIND_SPEAR) {
            return switch (material) {
                case IRON_SPEAR -> Material.DIAMOND_SPEAR;
                case DIAMOND_SPEAR -> Material.NETHERITE_SPEAR;
                default -> material;
            };
        }
        boolean sword = kind == KIND_SWORD;
        return switch (material) {
            case STONE_SWORD, STONE_AXE -> sword ? Material.IRON_SWORD : Material.IRON_AXE;
            case IRON_SWORD, IRON_AXE -> sword ? Material.DIAMOND_SWORD : Material.DIAMOND_AXE;
            case DIAMOND_SWORD, DIAMOND_AXE -> sword ? Material.NETHERITE_SWORD : Material.NETHERITE_AXE;
            default -> material;
        };
    }

    private static String materialName(Material material) {
        return switch (material) {
            case LEATHER_HELMET, LEATHER_CHESTPLATE, LEATHER_LEGGINGS, LEATHER_BOOTS -> "皮革";
            case CHAINMAIL_HELMET, CHAINMAIL_CHESTPLATE, CHAINMAIL_LEGGINGS, CHAINMAIL_BOOTS -> "锁链";
            case IRON_HELMET, IRON_CHESTPLATE, IRON_LEGGINGS, IRON_BOOTS,
                    IRON_SWORD, IRON_AXE -> "铁";
            case DIAMOND_HELMET, DIAMOND_CHESTPLATE, DIAMOND_LEGGINGS, DIAMOND_BOOTS,
                    DIAMOND_SWORD, DIAMOND_AXE -> "钻石";
            case NETHERITE_HELMET, NETHERITE_CHESTPLATE, NETHERITE_LEGGINGS, NETHERITE_BOOTS,
                    NETHERITE_SWORD, NETHERITE_AXE -> "下界合金";
            case STONE_SWORD, STONE_AXE -> "石";
            case WOODEN_SPEAR -> "木";
            case STONE_SPEAR -> "石";
            case GOLDEN_SPEAR -> "金";
            case COPPER_SPEAR -> "铜";
            case IRON_SPEAR -> "铁";
            case DIAMOND_SPEAR -> "钻石";
            case NETHERITE_SPEAR -> "下界合金";
            default -> material.name();
        };
    }

    private static String materialName(ItemStack stack, int kind) {
        if (kind == KIND_SHOOTER) {
            int tier = stack == null || stack.getItemMeta() == null ? 0
                    : stack.getItemMeta().getPersistentDataContainer()
                    .getOrDefault(shooterTierKey, PersistentDataType.INTEGER, 0);
            return shooterName(tier);
        }
        return materialName(stack == null ? null : stack.getType());
    }

    private static String shooterName(int tier) {
        return tier >= 2 ? "机枪射手" : tier == 1 ? "双发射手" : "豌豆射手";
    }

    private static int maxWeaponTier(int kind) {
        if (kind == KIND_SHOOTER) {
            return 2;
        }
        if (kind == KIND_BOW || kind == KIND_CROSSBOW || kind == KIND_TRIDENT || kind == KIND_MACE) {
            return 0;
        }
        return 3;
    }

    private void replaceUpgraded(Player player, EquipmentCatalog.Kind kind, Material nextMaterial) {
        ItemStack next = new ItemStack(nextMaterial);
        ItemMeta nextMeta = next.getItemMeta();
        nextMeta.setUnbreakable(true);
        next.setItemMeta(nextMeta);
        ItemStack old = switch (kind) {
            case CHESTPLATE -> player.getInventory().getChestplate();
            case LEGGINGS -> player.getInventory().getLeggings();
            case BOOTS -> player.getInventory().getBoots();
            default -> player.getInventory().getHelmet();
        };
        copyEnchants(old, next);
        switch (kind) {
            case CHESTPLATE -> player.getInventory().setChestplate(next);
            case LEGGINGS -> player.getInventory().setLeggings(next);
            case BOOTS -> player.getInventory().setBoots(next);
            default -> player.getInventory().setHelmet(next);
        }
    }

    private static void copyEnchants(ItemStack old, ItemStack next) {
        if (old == null || !old.hasItemMeta()) {
            return;
        }
        ItemMeta oldMeta = old.getItemMeta();
        ItemMeta nextMeta = next.getItemMeta();
        for (org.bukkit.enchantments.Enchantment enchantment : oldMeta.getEnchants().keySet()) {
            nextMeta.addEnchant(enchantment, oldMeta.getEnchantLevel(enchantment), true);
        }
        next.setItemMeta(nextMeta);
    }

    private void giveChange(Player player, int silverChange) {
        if (silverChange <= 0) {
            return;
        }
        int diamonds = silverChange / 100;
        int rest = silverChange % 100;
        int gold = rest / 10;
        int silver = rest % 10;
        if (diamonds > 0) {
            addCurrency(player, ShopCurrency.DIAMOND, diamonds, true);
        }
        if (gold > 0) {
            addCurrency(player, ShopCurrency.GOLD, gold, true);
        }
        if (silver > 0) {
            addCurrency(player, ShopCurrency.SILVER, silver, true);
        }
    }

    void addCurrency(Player player, ShopCurrency currency, int amount) {
        addCurrency(player, currency, amount, false);
    }

    private void addCurrency(Player player, ShopCurrency currency, int amount, boolean allowInDeathMode) {
        if (player == null || amount <= 0) {
            return;
        }
        if (deathMode && !allowInDeathMode && currency != ShopCurrency.STAR) {
            return;
        }
        ItemStack stack = new ItemStack(currency.material(), amount);
        ItemMeta meta = stack.getItemMeta();
        meta.setItemName(currency.displayName());
        stack.setItemMeta(meta);
        Map<Integer, ItemStack> leftover = player.getInventory().addItem(stack);
        for (ItemStack item : leftover.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), item);
        }
    }

    private void grantStartingDiamonds(Player player, int amount) {
        ItemStack diamonds = new ItemStack(ShopCurrency.DIAMOND.material(), amount);
        ItemMeta meta = diamonds.getItemMeta();
        meta.setItemName(ShopCurrency.DIAMOND.displayName());
        diamonds.setItemMeta(meta);
        Map<Integer, ItemStack> leftover = player.getInventory().addItem(diamonds);
        for (ItemStack item : leftover.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), item);
        }
        player.sendMessage(ChatColor.GOLD + "【死战模式】获得 " + amount + " 钻币（休整发放），本局无怪物货币掉落！");
    }

    private void grantDeathRestDiamonds() {
        if (!deathMode) {
            return;
        }
        int amount = DamageMath.deathRestDiamonds(deathRestDiamondTier);
        for (UUID uuid : participants) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) {
                grantStartingDiamonds(p, amount);
            }
        }
    }

    private long totalSilverValue(Player player) {
        long total = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (isCurrency(item, ShopCurrency.SILVER)) {
                total += item.getAmount();
            } else if (isCurrency(item, ShopCurrency.GOLD)) {
                total += item.getAmount() * 10L;
            } else if (isCurrency(item, ShopCurrency.DIAMOND)) {
                total += item.getAmount() * 100L;
            }
        }
        return total;
    }

    private int takeCurrencyUpTo(Player player, ShopCurrency currency, int maxAmount) {
        int removed = 0;
        for (int i = 0; i < player.getInventory().getSize() && removed < maxAmount; i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (!isCurrency(item, currency)) {
                continue;
            }
            int take = Math.min(item.getAmount(), maxAmount - removed);
            item.setAmount(item.getAmount() - take);
            if (item.getAmount() <= 0) {
                player.getInventory().setItem(i, null);
            } else {
                player.getInventory().setItem(i, item);
            }
            removed += take;
        }
        return removed;
    }

    public void refreshBossBars() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.addScoreboardTag("player");
            if (isPlaying(player)) {
                int hp = (int) Math.ceil(player.getHealth());
                int max = (int) player.getMaxHealth();
                player.setCustomName(player.getDisplayName() + " §c❤ " + hp + "/" + max);
                player.setCustomNameVisible(true);
            } else {
                player.setCustomName(null);
                player.setCustomNameVisible(false);
            }
        }
        long now = System.currentTimeMillis();
        pendingCreates.entrySet().removeIf(entry -> now - entry.getValue() > PENDING_TTL_MS);

        Set<UUID> alive = new HashSet<>();
        if (gameRunning || !bars.isEmpty()) {
            for (World world : Bukkit.getWorlds()) {
                for (Villager villager : allDaves(world)) {
                if (villager.isDead()) {
                    continue;
                }
                alive.add(villager.getUniqueId());
                String team = getOwnerTeam(villager);
                if (team == null) {
                    continue;
                }
                BossBar bar = bars.get(villager.getUniqueId());
                if (bar == null) {
                    bar = Bukkit.createBossBar("戴夫", BarColor.RED, BarStyle.SOLID);
                    bars.put(villager.getUniqueId(), bar);
                }
                double health = Math.max(0.0, villager.getHealth());
                bar.setProgress(Math.min(1.0, health / MAX_HEALTH));
                Long last = lastDaveHit.get(villager.getUniqueId());
                if (last != null && now - last < 3000L) {
                    bar.setTitle("§c戴夫 ⚠ 正在被攻击");
                } else {
                    bar.setTitle("戴夫");
                }
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        if (team.equals(getTeamName(player))) {
                            bar.addPlayer(player);
                        } else {
                            bar.removePlayer(player);
                        }
                    }
                }
            }
        }
        Iterator<Map.Entry<UUID, BossBar>> iterator = bars.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, BossBar> entry = iterator.next();
            if (!alive.contains(entry.getKey())) {
                entry.getValue().removeAll();
                iterator.remove();
            }
        }
        refreshBossBossBars();
    }

    private void refreshBossBossBars() {
        double radiusSq = BOSS_BAR_RADIUS * BOSS_BAR_RADIUS;
        Iterator<Map.Entry<UUID, BossBar>> iterator = bossBars.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, BossBar> entry = iterator.next();
            Entity entity = Bukkit.getEntity(entry.getKey());
            if (!(entity instanceof Mob boss) || boss.isDead()) {
                entry.getValue().removeAll();
                iterator.remove();
                continue;
            }
            double max = boss.getMaxHealth();
            if (max <= 0) {
                max = 1;
            }
            entry.getValue().setProgress(Math.min(1.0, Math.max(0.0, boss.getHealth() / max)));
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getWorld().equals(boss.getWorld())
                        && player.getLocation().distanceSquared(boss.getLocation()) <= radiusSq) {
                    entry.getValue().addPlayer(player);
                } else {
                    entry.getValue().removePlayer(player);
                }
            }
        }
    }

    private Villager teamDave(String team) {
        for (World world : Bukkit.getWorlds()) {
            for (Villager villager : allDaves(world)) {
                if (!villager.isDead() && team.equals(getOwnerTeam(villager))) {
                    return villager;
                }
            }
        }
        return null;
    }

    public int bindNearby(String team, double radius, Player center) {
        int count = 0;
        for (World world : Bukkit.getWorlds()) {
            for (Villager villager : allDaves(world)) {
                if (villager.isDead() || getOwnerTeam(villager) != null) {
                    continue;
                }
                if (center != null
                        && (!center.getWorld().equals(world)
                            || center.getLocation().distanceSquared(villager.getLocation()) > radius * radius)) {
                    continue;
                }
                if (hasLivingDave(team)) {
                    continue;
                }
                villager.getPersistentDataContainer().set(ownerTeamKey, PersistentDataType.STRING, team);
                createBar(villager, team);
                count++;
            }
        }
        return count;
    }

    public int killTeamDave(String team) {
        int count = 0;
        for (World world : Bukkit.getWorlds()) {
            for (Villager villager : allDaves(world)) {
                if (villager.isDead() || !team.equals(getOwnerTeam(villager))) {
                    continue;
                }
                villager.setHealth(0);
                count++;
            }
        }
        return count;
    }

    public String balanceTeams() {
        Scoreboard main = Bukkit.getScoreboardManager().getMainScoreboard();
        for (String id : TEAM_IDS) {
            Team team = main.getTeam(id);
            if (team == null) {
                team = main.registerNewTeam(id);
            }
            team.setDisplayName(teamColor(id) + TEAM_DISPLAYS.get(id));
            team.setColor(teamColor(id));
            for (String entry : team.getEntries()) {
                team.removeEntry(entry);
            }
        }
        List<Player> ready = new ArrayList<>();
        for (UUID id : readyPlayers) {
            Player player = Bukkit.getPlayer(id);
            if (player != null && player.isOnline()) {
                ready.add(player);
            }
        }
        int readyCount = ready.size();
        int assigned = Math.min(readyCount, MAX_PARTICIPANTS);
        int teamsUsed = assigned >= 12 ? 4 : Math.max(1, Math.min(4, (assigned + 4) / 5));
        Set<String> preferredTeams = new HashSet<>();
        int maxPreferredIndex = -1;
        for (int i = 0; i < assigned; i++) {
            String preferred = teamPreferences.get(ready.get(i).getUniqueId());
            if (preferred != null) {
                preferredTeams.add(preferred);
                int idx = TEAM_IDS.indexOf(preferred);
                if (idx > maxPreferredIndex) {
                    maxPreferredIndex = idx;
                }
            }
        }
        teamsUsed = Math.max(teamsUsed, preferredTeams.size());
        teamsUsed = Math.max(teamsUsed, maxPreferredIndex + 1);
        teamsUsed = Math.max(1, Math.min(4, teamsUsed));
        List<Player> randomPool = new ArrayList<>();
        int[] teamSizes = new int[TEAM_IDS.size()];
        for (int i = 0; i < assigned; i++) {
            Player player = ready.get(i);
            String preferred = teamPreferences.get(player.getUniqueId());
            if (preferred != null) {
                int prefIndex = TEAM_IDS.indexOf(preferred);
                if (prefIndex >= 0 && prefIndex < teamsUsed && teamSizes[prefIndex] < 5) {
                    main.getTeam(TEAM_IDS.get(prefIndex)).addEntry(player.getName());
                    teamSizes[prefIndex]++;
                    continue;
                }
            }
            randomPool.add(player);
        }
        for (int i = 0; i < randomPool.size(); i++) {
            int best = 0;
            for (int t = 1; t < teamsUsed; t++) {
                if (teamSizes[t] < teamSizes[best]) {
                    best = t;
                }
            }
            main.getTeam(TEAM_IDS.get(best)).addEntry(randomPool.get(i).getName());
            teamSizes[best]++;
        }
        StringBuilder result = new StringBuilder("自动分队完成：已分配 ")
                .append(assigned).append(" 人参赛，使用 ").append(teamsUsed).append(" 队（");
        for (int i = 0; i < teamsUsed; i++) {
            Team team = main.getTeam(TEAM_IDS.get(i));
            if (i > 0) {
                result.append("，");
            }
            result.append(TEAM_DISPLAYS.get(TEAM_IDS.get(i))).append(team.getEntries().size()).append("人");
        }
        result.append("）");
        if (readyCount > assigned) {
            result.append("；其余 ").append(readyCount - assigned).append(" 名准备玩家保持等待，下一局自动进入");
        }
        return result.toString();
    }

    public void startGame() {
        if (gameRunning) {
            Bukkit.broadcastMessage(ChatColor.GOLD + "【戴夫】游戏已经开始，请耐心等待");
            return;
        }
        Bukkit.broadcastMessage(ChatColor.GOLD + "【戴夫】" + balanceTeams());
        restActive = false;
        activeGameTicks = 0;
        waveIndex = 0;
        autoStartBracket = 0;
        autoStartCountdown = 0;
        autoStartReadyCount = 0;
        bombKills.clear();
        damageLogs.clear();
        bossAttackers.clear();
        if (autoStartBar != null) {
            autoStartBar.removeAll();
            autoStartBar = null;
        }
        spawningEnabled = false;
        spawnSeconds = 0;
        spawnCounterTicks = 0;
        setGameScore(1);
        participants.clear();
        gameParticipantSet.clear();
        teamPreferences.clear();
        clearTeamBuffs();
        clearPets();
        clearAllChests();
        if (brewingManager != null) {
            brewingManager.clearAll();
        }
        Scoreboard main = Bukkit.getScoreboardManager().getMainScoreboard();
        int teamsWithPlayers = 0;
        for (String id : TEAM_IDS) {
            Team team = main.getTeam(id);
            if (team == null) {
                continue;
            }
            if (!team.getEntries().isEmpty()) {
                teamsWithPlayers++;
            }
            for (String entry : team.getEntries()) {
                Player member = Bukkit.getPlayerExact(entry);
                if (member != null && member.isOnline() && !isPvzPlayer(member.getUniqueId())) {
                    member.setGameMode(GameMode.ADVENTURE);
                    member.getInventory().clear();
                    clearExperience(member);
                    clearPotionEffects(member);
                    TeamDef def = teamDefs.get(id);
                    if (def != null) {
                        teleportToPlayArea(member, def);
                    }
                    giveStarterGear(member, id);
                    participants.add(member.getUniqueId());
                    gameParticipantSet.add(member.getUniqueId());
                    refreshPlayerListName(member);
                }
            }
        }
        clearAllDaves();
        int spawned = 0;
        for (String id : TEAM_IDS) {
            Team team = main.getTeam(id);
            if (team == null || team.getEntries().isEmpty()) {
                continue;
            }
            TeamDef def = teamDefs.get(id);
            if (def == null) {
                continue;
            }
            spawnDave(def, id);
            spawned++;
        }
        if (spawned == 0) {
            Bukkit.broadcastMessage(ChatColor.GOLD + "【戴夫】没有有队员的队伍，未生成戴夫");
            return;
        }
        gameRunning = true;
        teamsAtGameStart = teamsWithPlayers;
        gameId = UUID.randomUUID();
        disconnectSnapshots.clear();
        playerScores.clear();
        playerKills.clear();
        int deathVotes = 0;
        int normalVotes = 0;
        for (UUID uuid : participants) {
            if (Boolean.TRUE.equals(modeVotes.get(uuid))) {
                deathVotes++;
            } else {
                normalVotes++;
            }
        }
        deathMode = deathVotes > normalVotes;
        int startingDiamonds = 30;
        if (deathMode) {
            int low = lowEconomyVotes();
            int mid = midEconomyVotes();
            int high = highEconomyVotes();
            if (low + mid + high > 0) {
                if (low > mid && low > high) {
                    startingDiamonds = 15;
                } else if (high > mid && high > low) {
                    startingDiamonds = 50;
                }
            }
        }
        gameStartParticipants.clear();
        gameStartParticipants.addAll(participants);
        deathRestDiamondTier = startingDiamonds;
        spawnIntervalStartTicks = deathMode
                ? (startingDiamonds == 15 ? 400 : startingDiamonds == 50 ? 160 : 300)
                : SPAWN_INTERVAL_START_TICKS;
        String spawnNote = deathMode
                ? "，刷怪间隔 " + (startingDiamonds == 15 ? 20 : startingDiamonds == 50 ? 8 : 15) + " 秒"
                : "";
        Bukkit.broadcastMessage(ChatColor.GOLD + "【戴夫】本局模式："
                + (deathMode ? "死战模式（开局 " + startingDiamonds + " 钻币，无货币掉落，阵亡休整期复活"
                + spawnNote + "）" : "正常模式")
                + ChatColor.GRAY + "（正常 " + normalVotes + " 票 / 死战 " + deathVotes + " 票）");
        buildGameScoreboard();
        gameStartMillis = System.currentTimeMillis();
        daveSlowAuraTeams.clear();
        daveBuffs.clear();
        waveBuffMultiplier = 1.0;
        waveSpeedMultiplier = 1.0;
        refreshSpawnPointActives();
        phaseStartMillis = System.currentTimeMillis();
        Bukkit.broadcastMessage(ChatColor.GOLD + "【戴夫】游戏开始！已生成 " + spawned + " 个戴夫，10 秒后开启刷怪");
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendTitle("§c游戏即将在十秒后开启", "", 10, 60, 10);
            refreshPlayerListName(player);
            if (participants.contains(player.getUniqueId())) {
                assignGameScoreboard(player);
            }
        }
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (gameRunning) {
                if (deathMode) {
                    openingRest = true;
                    restActive = true;
                    restEndTick = Bukkit.getCurrentTick() + 1200L;
                    grantDeathRestDiamonds();
                    Bukkit.broadcastMessage(ChatColor.GOLD + "【戴夫】死战模式：进入 1 分钟开局休整，"
                            + "发放 " + DamageMath.deathRestDiamonds(deathRestDiamondTier)
                            + " 钻币，可用货币升级装备");
                } else {
                    startWave(0);
                }
            }
        }, 200L);
    }

    public void endGame() {
        gameRunning = false;
        gameId = null;
        disconnectSnapshots.clear();
        deathMode = false;
        deathRestDiamondTier = 30;
        gameStartParticipants.clear();
        openingRest = false;
        spawnIntervalStartTicks = SPAWN_INTERVAL_START_TICKS;
        modeVotes.clear();
        economyVotes.clear();
        waitingRespawn.clear();
        restActive = false;
        activeGameTicks = 0;
        waveIndex = 0;
        autoStartBracket = 0;
        autoStartCountdown = 0;
        autoStartReadyCount = 0;
        bombKills.clear();
        damageLogs.clear();
        bossAttackers.clear();
        if (autoStartBar != null) {
            autoStartBar.removeAll();
            autoStartBar = null;
        }
        teamPreferences.clear();
        for (Player player : Bukkit.getOnlinePlayers()) {
            markUnready(player);
        }
        clearTeamBuffs();
        clearPets();
        clearAllChests();
        if (brewingManager != null) {
            brewingManager.clearAll();
        }
        clearAllDaves();
        clearAllBossBars();
        resetGameScoreboard();
        daveSlowAuraTeams.clear();
        daveBuffs.clear();
        waveBuffMultiplier = 1.0;
        waveSpeedMultiplier = 1.0;
        thoroughCleanup();
        setGameScore(0);
        spawningEnabled = false;
        spawnSeconds = 0;
        spawnCounterTicks = 0;
        broadcastPlayerScores();
        Bukkit.broadcastMessage(ChatColor.GOLD + "【戴夫】游戏结束，将在五秒后返回大厅");
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (isClassicPlayer(player)) {
                    clearExperience(player);
                    clearPotionEffects(player);
                }
            }
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (!isClassicPlayer(player)) {
                    continue;
                }
                if (player.isOp() && !participants.contains(player.getUniqueId())) {
                    refreshPlayerListName(player);
                    continue;
                }
                player.getInventory().clear();
                player.setGameMode(GameMode.ADVENTURE);
                applyLobbyItems(player);
                refreshPlayerListName(player);
                teleportToLobby(player);
            }
            Scoreboard main = Bukkit.getScoreboardManager().getMainScoreboard();
            for (Player player : Bukkit.getOnlinePlayers()) {
                Team team = main.getEntryTeam(player.getName());
                if (team != null) {
                    team.removeEntry(player.getName());
                }
            }
            participants.clear();
        }, 100L);
    }

    public void releaseBackupDancers(Zombie king) {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        Objective ownerObj = scoreboard.getObjective("rz.backup_dancer_skeleton.owner");
        Objective animObj = scoreboard.getObjective("rz.backup_dancer_skeleton.animation");
        if (ownerObj == null || animObj == null) {
            return;
        }
        Objective uidObj = scoreboard.getObjective("rz.monster_uid");
        int kingUid = uidObj == null ? 0 : uidObj.getScore(king.getUniqueId().toString()).getScore();
        for (World world : Bukkit.getWorlds()) {
            for (Skeleton skeleton : world.getEntitiesByClass(Skeleton.class)) {
                if (!skeleton.getScoreboardTags().contains("backup_dancer_skeleton")) {
                    continue;
                }
                if (ownerObj.getScore(skeleton.getUniqueId().toString()).getScore() != kingUid) {
                    continue;
                }
                skeleton.getScoreboardTags().remove("has_owner");
                skeleton.setAI(true);
                animObj.getScore(skeleton.getUniqueId().toString()).setScore(0);
            }
        }
    }

    private void tickGame() {
        if (!gameRunning) {
            return;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            refreshPlayerListName(player);
        }
        refreshTeamBuffs();
        refreshDaveSlowAuras();
        refreshDaveBuffs();
        if (Bukkit.getCurrentTick() % 60 == 0) {
            applyWaveBuffAndMeleeTick();
        }
        if (Bukkit.getCurrentTick() % 200 == 0) {
            tickWardenSlams();
        }
        if (Bukkit.getCurrentTick() % 400 == 0) {
            tickWitherMeteors();
        }
        if (deathMode && Bukkit.getCurrentTick() % 20 == 0) {
            cleanupDeathModeCurrency();
        }
        updateGameSidebar();
        long now = Bukkit.getCurrentTick();
        if (restActive) {
            respawnWaitingPlayers();
            if (now >= restEndTick) {
                if (openingRest) {
                    openingRest = false;
                    restActive = false;
                    startWave(0);
                } else {
                    int next = waveIndex + 1;
                    if (next >= WAVE_SEQUENCE.length) {
                        declareVictory();
                    } else {
                        startWave(next);
                    }
                }
            }
            return;
        }
        if (waveIndex == WAVE_SEQUENCE.length - 1 && WAVE_SEQUENCE[waveIndex] == WAVE_BIGBOSS) {
            if (!hasLivingBigBoss()
                    && System.currentTimeMillis() - phaseStartMillis >= 10_000L) {
                declareVictory();
            }
            return;
        }
        if (WAVE_SEQUENCE[waveIndex] == WAVE_MINIBOSS && !hasLivingMiniBoss()
                && System.currentTimeMillis() - phaseStartMillis >= 10_000L) {
            startRest(now);
            return;
        }
        if (System.currentTimeMillis() - phaseStartMillis >= REST_INTERVAL_MILLIS) {
            startRest(now);
        }
    }

    private boolean hasLivingMiniBoss() {
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity.isValid()
                        && entity.getScoreboardTags().contains("mini_boss")) {
                    return true;
                }
            }
        }
        return false;
    }

    private void applyWaveBuff(Mob mob, double mult, double speedMult) {
        PersistentDataContainer pdc = mob.getPersistentDataContainer();
        double stored = pdc.getOrDefault(waveBuffMultKey, PersistentDataType.DOUBLE, 1.0);
        double storedSpeed = pdc.getOrDefault(waveSpeedMultKey, PersistentDataType.DOUBLE, 1.0);
        if (Math.abs(stored - mult) < 0.001 && Math.abs(storedSpeed - speedMult) < 0.001) {
            return;
        }
        boolean rangeBand = Math.abs(mult - 1.25) < 0.001 || Math.abs(mult - 1.5) < 0.001;
        boolean rangedMob = mob.getScoreboardTags().contains("skeleton")
                || mob.getScoreboardTags().contains("stray")
                || mob.getScoreboardTags().contains("blaze")
                || mob.getScoreboardTags().contains("witch")
                || mob.getScoreboardTags().contains("football_skeleton")
                || mob.getScoreboardTags().contains("black_football_skeleton");
        boolean meleeBoost = rangeBand && !rangedMob
                && !mob.getScoreboardTags().contains("mini_boss")
                && !mob.getScoreboardTags().contains("big_boss");
        if (meleeBoost) {
            pdc.set(waveMeleeBoostKey, PersistentDataType.BYTE, (byte) 1);
        } else {
            pdc.remove(waveMeleeBoostKey);
        }
        AttributeInstance atkAttr = mob.getAttribute(Attribute.ATTACK_DAMAGE);
        AttributeInstance spdAttr = mob.getAttribute(Attribute.MOVEMENT_SPEED);
        double baseHp = pdc.getOrDefault(waveBaseHpKey, PersistentDataType.DOUBLE, mob.getMaxHealth());
        double baseAtk = pdc.getOrDefault(waveBaseAtkKey, PersistentDataType.DOUBLE,
                atkAttr == null ? 0 : atkAttr.getBaseValue());
        double baseSpd = pdc.getOrDefault(waveBaseSpdKey, PersistentDataType.DOUBLE,
                spdAttr == null ? 0 : spdAttr.getBaseValue());
        pdc.set(waveBaseHpKey, PersistentDataType.DOUBLE, baseHp);
        pdc.set(waveBaseAtkKey, PersistentDataType.DOUBLE, baseAtk);
        pdc.set(waveBaseSpdKey, PersistentDataType.DOUBLE, baseSpd);
        pdc.set(waveBuffMultKey, PersistentDataType.DOUBLE, mult);
        pdc.set(waveSpeedMultKey, PersistentDataType.DOUBLE, speedMult);
        mob.setMaxHealth(baseHp * mult);
        mob.setHealth(mob.getMaxHealth());
        if (atkAttr != null) {
            atkAttr.setBaseValue(baseAtk * mult);
        }
        if (spdAttr != null) {
            spdAttr.setBaseValue(baseSpd * speedMult);
        }
    }

    private void applyWaveBuffAndMeleeTick() {
        boolean buffActive = waveBuffMultiplier != 1.0 || waveSpeedMultiplier != 1.0;
        double mult = waveBuffMultiplier;
        double speedMult = waveSpeedMultiplier;
        long tick = Bukkit.getCurrentTick();
        for (World world : Bukkit.getWorlds()) {
            for (Mob mob : world.getEntitiesByClass(Mob.class)) {
                if (buffActive && isMonster(mob)
                        && !mob.getScoreboardTags().contains("mini_boss")
                        && !mob.getScoreboardTags().contains("big_boss")) {
                    applyWaveBuff(mob, mult, speedMult);
                }
                if (mob.getPersistentDataContainer().has(waveMeleeBoostKey, PersistentDataType.BYTE)) {
                    if (!mob.isValid() || mob.isDead()) {
                        meleeReachLastAttack.remove(mob.getUniqueId());
                        continue;
                    }
                    LivingEntity target = mob.getTarget();
                    if (target == null || target.isDead()) {
                        continue;
                    }
                    double dist = mob.getLocation().distance(target.getLocation());
                    if (dist <= 2.0 || dist > 2.5) {
                        continue;
                    }
                    Long last = meleeReachLastAttack.get(mob.getUniqueId());
                    if (last != null && tick - last < 20) {
                        continue;
                    }
                    meleeReachLastAttack.put(mob.getUniqueId(), tick);
                    mob.attack(target);
                }
            }
        }
    }

    private void tickWardenSlams() {
        for (World world : Bukkit.getWorlds()) {
            for (Mob mob : world.getEntitiesByClass(Mob.class)) {
                if (!(mob instanceof Warden warden)
                        || !warden.getScoreboardTags().contains("mini_boss")
                        || !warden.isValid() || warden.isDead()) {
                    continue;
                }
                if (!wardenSlamActive.contains(warden.getUniqueId())) {
                    startWardenSlam(warden);
                }
            }
        }
    }

    private void startWardenSlam(Warden warden) {
        wardenSlamActive.add(warden.getUniqueId());
        warden.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, 20, 8, false, true, true));
        boolean[] slammed = {false};
        Bukkit.getScheduler().runTaskTimer(plugin, task -> {
            if (!warden.isValid() || warden.isDead()) {
                task.cancel();
                wardenSlamActive.remove(warden.getUniqueId());
                return;
            }
            if (warden.isOnGround()) {
                task.cancel();
                wardenSlamActive.remove(warden.getUniqueId());
                if (!slammed[0]) {
                    slammed[0] = true;
                    wardenSlamLanding(warden);
                }
            }
        }, 20L, 1L);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (warden.isValid() && !warden.isDead() && !slammed[0]) {
                slammed[0] = true;
                wardenSlamLanding(warden);
            }
            wardenSlamActive.remove(warden.getUniqueId());
        }, 60L);
    }

    private void wardenSlamLanding(Warden warden) {
        Location loc = warden.getLocation();
        World world = warden.getWorld();
        world.spawnParticle(Particle.EXPLOSION, loc, 1, 0, 0, 0, 0);
        world.spawnParticle(Particle.BLOCK, loc.clone().add(0, 0.1, 0),
                60, 3, 0.1, 3, 0, Material.DIRT.createBlockData());
        world.spawnParticle(Particle.SWEEP_ATTACK, loc, 30, 3, 0.2, 3, 0);
        for (Entity entity : world.getNearbyEntities(loc,
                DamageMath.WARDEN_SLAM_DAMAGE_RADIUS, DamageMath.WARDEN_SLAM_DAMAGE_RADIUS,
                DamageMath.WARDEN_SLAM_DAMAGE_RADIUS)) {
            if (entity instanceof Player target && target.isOnline()) {
                target.damage(DamageMath.WARDEN_SLAM_DAMAGE, warden);
            }
        }
        for (Entity entity : world.getNearbyEntities(loc,
                DamageMath.WARDEN_SLAM_KNOCKBACK_RADIUS, DamageMath.WARDEN_SLAM_KNOCKBACK_RADIUS,
                DamageMath.WARDEN_SLAM_KNOCKBACK_RADIUS)) {
            if (entity instanceof Player target && target.isOnline()) {
                target.setVelocity(target.getLocation().toVector().subtract(loc.toVector())
                        .normalize().multiply(DamageMath.WARDEN_SLAM_KNOCKBACK_STRENGTH)
                        .setY(DamageMath.WARDEN_SLAM_KNOCKBACK_UP));
            }
        }
    }

    private void tickWitherMeteors() {
        for (World world : Bukkit.getWorlds()) {
            for (Mob mob : world.getEntitiesByClass(Mob.class)) {
                if (!(mob instanceof Wither wither)
                        || !wither.getScoreboardTags().contains("big_boss")
                        || !wither.isValid() || wither.isDead()) {
                    continue;
                }
                spawnWitherMeteor(wither);
            }
        }
    }

    private void spawnWitherMeteor(Wither wither) {
        World world = wither.getWorld();
        Location target = wither.getLocation();
        Location start = target.clone().add(0, DamageMath.METEOR_START_HEIGHT, 0);
        BlockDisplay display = world.spawn(start, BlockDisplay.class, d -> {
            d.setBlock(Bukkit.createBlockData(Material.MAGMA_BLOCK));
            d.setTransformation(new Transformation(
                    new org.joml.Vector3f(0, 0, 0),
                    new org.joml.Quaternionf(0, 0, 0, 1),
                    new org.joml.Vector3f(20, 20, 20),
                    new org.joml.Quaternionf(0, 0, 0, 1)));
            d.setGravity(false);
        });
        Bukkit.getScheduler().runTaskTimer(plugin, task -> {
            if (!display.isValid()) {
                task.cancel();
                return;
            }
            Location dl = display.getLocation();
            if (dl.getY() <= target.getY() + 1) {
                display.remove();
                task.cancel();
                world.spawnParticle(Particle.EXPLOSION, target, 1, 0, 0, 0, 0);
                for (Entity entity : world.getNearbyEntities(target,
                        DamageMath.METEOR_RADIUS, DamageMath.METEOR_RADIUS, DamageMath.METEOR_RADIUS)) {
                    if (entity instanceof Player p && p.isOnline()) {
                        p.damage(DamageMath.METEOR_DAMAGE, wither);
                    } else if (entity instanceof Wolf wolf && isPetWolf(wolf)) {
                        wolf.damage(DamageMath.METEOR_DAMAGE, wither);
                    }
                }
            } else {
                display.teleport(dl.clone().subtract(0, DamageMath.METEOR_FALL_PER_TICK, 0));
            }
        }, 0L, 1L);
    }

    private boolean hasLivingBigBoss() {
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity.getScoreboardTags().contains("big_boss") && entity.isValid()) {
                    return true;
                }
            }
        }
        return false;
    }

    private void startRest(long now) {
        restActive = true;
        restEndTick = now + REST_DURATION_TICKS;
        setGameScore(0);
        grantDeathRestDiamonds();
        spawningEnabled = false;
        killAllMonsters();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (isPlaying(player)) {
                player.sendTitle("§a我们安全了...暂时...", "§730 秒休整时间", 10, 60, 10);
            }
        }
        Bukkit.broadcastMessage(ChatColor.GREEN + "【休整】怪物已被清除，30 秒休整时间，刷怪暂停");
    }

    private void startWave(int index) {
        waveIndex = index;
        restActive = false;
        phaseStartMillis = System.currentTimeMillis();
        setGameScore(1);
        int waveNumber = index + 1;
        if (waveNumber >= 5 && waveNumber <= 7) {
            waveBuffMultiplier = 1.25;
            waveSpeedMultiplier = 1.2;
        } else if (waveNumber >= 9 && waveNumber <= 11) {
            waveBuffMultiplier = 1.5;
            waveSpeedMultiplier = 1.3;
        } else {
            waveBuffMultiplier = 1.0;
            waveSpeedMultiplier = 1.0;
        }
        refreshSpawnPointActives();
        int type = WAVE_SEQUENCE[index];
        if (type == WAVE_NORMAL) {
            spawningEnabled = true;
            spawnCounterTicks = 0;
            spawnWave();
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (isPlaying(player)) {
                    player.sendTitle("§e第 " + (index + 1) + " 波 来袭", "§7守护戴夫！", 10, 40, 10);
                }
            }
            Bukkit.broadcastMessage(ChatColor.GOLD + "【戴夫】第 " + (index + 1) + " 波：怪物来袭！");
        } else {
            spawningEnabled = false;
            spawnBosses(type, index);
            String bossName = type == WAVE_MINIBOSS ? "小boss" : "大boss";
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (isPlaying(player)) {
                    player.sendTitle("§c" + bossName + " 来袭", "§7注意！", 10, 40, 10);
                }
            }
            Bukkit.broadcastMessage(ChatColor.RED + "【戴夫】第 " + (index + 1) + " 波：" + bossName + " 来袭！");
        }
    }

    private void spawnBosses(int type, int waveIndex) {
        boolean mini = type == WAVE_MINIBOSS;
        boolean giantBossWave = mini && waveIndex == WAVE_GIANT_BOSS_INDEX;
        for (String teamId : TEAM_IDS) {
            if (!hasLivingDave(teamId)) {
                continue;
            }
            TeamDef def = teamDefs.get(teamId);
            if (def == null) {
                continue;
            }
            Location loc = bossPointForTeam(teamId, def);
            if (giantBossWave) {
                spawnGiantBoss(teamId, loc);
                continue;
            }
            String mmName = mini ? "RZWarden" : "RZWither";
            String displayName = mini ? "监守者" : "凋灵";
            MythicSpawnResult result = spawnMythicMob(mmName, loc);
            if (result.entity() != null) {
                tagBoss(result.entity(), mini, teamId, displayName);
                applyBossStats(result.entity(), mini);
                scaleBossHealth(result.entity(), teamId);
                plugin.getLogger().info("Boss 生成成功: " + mmName + " @ " + loc);
            } else if (!result.mayHaveSpawned()) {
                Mob boss = spawnBossVanilla(loc, mini);
                if (boss != null) {
                    tagBoss(boss, mini, teamId, displayName);
                    scaleBossHealth(boss, teamId);
                }
            } else {
                plugin.getLogger().warning("Boss 可能已生成但取实体失败，安排补标签: " + mmName);
                final boolean miniFinal = mini;
                final String teamIdFinal = teamId;
                final String displayFinal = displayName;
                final Location bossLoc = loc;
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    Mob boss = findUntaggedBoss(bossLoc, miniFinal);
                    if (boss != null) {
                        tagBoss(boss, miniFinal, teamIdFinal, displayFinal);
                        applyBossStats(boss, miniFinal);
                        scaleBossHealth(boss, teamIdFinal);
                        plugin.getLogger().info("补标签成功: " + mmName + " @ " + bossLoc);
                    } else {
                        plugin.getLogger().warning("补标签未找到实体: " + mmName + " @ " + bossLoc);
                    }
                }, 5L);
            }
        }
    }

    private Mob findUntaggedBoss(Location loc, boolean mini) {
        for (Entity entity : loc.getWorld().getNearbyEntities(loc, 40, 40, 40)) {
            if (!(entity instanceof Mob mob) || mob.getScoreboardTags().contains("boss")) {
                continue;
            }
            if (mini ? mob instanceof Warden : mob instanceof Wither) {
                return mob;
            }
        }
        return null;
    }

    private void tagBoss(Mob boss, boolean mini, String teamId, String displayName) {
        boss.addScoreboardTag("rz");
        boss.addScoreboardTag("monster");
        boss.addScoreboardTag("boss");
        boss.addScoreboardTag(mini ? "mini_boss" : "big_boss");
        boss.addScoreboardTag("boss_team_" + teamId);
        boss.setRemoveWhenFarAway(false);
        boss.setPersistent(true);
        createBossBar(boss, mini, displayName);
    }

    private void spawnGiantBoss(String teamId, Location loc) {
        dropLootTickets(loc, "giant_heavy", "狂暴巨人僵尸", 1);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Mob boss = findGiantBoss(loc);
            if (boss != null) {
                tagBoss(boss, true, teamId, "狂暴巨人僵尸");
                scaleBossHealth(boss, teamId);
                plugin.getLogger().info("Boss 生成成功: RZGiantHeavy @ " + loc);
            } else {
                plugin.getLogger().warning("狂暴巨人生成后未找到实体: " + teamId);
            }
        }, 5L);
    }

    private void scaleBossHealth(Mob boss, String team) {
        int players = countPlayingPlayers(team);
        double mult = switch (players) {
            case 1 -> 0.4;
            case 2 -> 0.5;
            case 3 -> 0.7;
            case 4 -> 0.8;
            default -> 1.0;
        };
        double base = boss.getMaxHealth();
        boss.setMaxHealth(base * mult);
        boss.setHealth(base * mult);
    }

    private Mob findGiantBoss(Location loc) {
        for (Entity entity : loc.getWorld().getNearbyEntities(loc, 12, 12, 12)) {
            if (entity instanceof Mob mob
                    && mob.getScoreboardTags().contains("rz")
                    && mob.getScoreboardTags().contains("monster")
                    && mob.getScoreboardTags().contains("giant_heavy")) {
                return mob;
            }
        }
        return null;
    }

    private void createBossBar(Mob boss, boolean mini, String displayName) {
        BossBar bar = Bukkit.createBossBar(
                (mini ? ChatColor.RED : ChatColor.DARK_PURPLE) + displayName,
                mini ? BarColor.RED : BarColor.PURPLE,
                BarStyle.SOLID);
        bossBars.put(boss.getUniqueId(), bar);
    }

    private void clearAllBossBars() {
        for (BossBar bar : bossBars.values()) {
            bar.removeAll();
        }
        bossBars.clear();
    }

    private Mob spawnBossVanilla(Location loc, boolean mini) {
        World world = loc.getWorld();
        if (world == null) {
            return null;
        }
        try {
            if (mini) {
                return world.spawn(loc, Warden.class, w -> {
                    w.setMaxHealth(1000);
                    w.setHealth(1000);
                });
            }
            return world.spawn(loc, Wither.class, w -> {
                w.setMaxHealth(1000);
                w.setHealth(1000);
                setAttribute(w, Attribute.SCALE, 3.0);
            });
        } catch (Exception e) {
            plugin.getLogger().warning("Boss 原版生成失败: " + e.getMessage());
            return null;
        }
    }

    private void applyBossStats(Mob boss, boolean mini) {
        if (mini) {
            boss.setMaxHealth(1000);
            boss.setHealth(1000);
        } else if (boss instanceof Wither wither) {
            wither.setMaxHealth(1000);
            wither.setHealth(1000);
            setAttribute(wither, Attribute.SCALE, 5.0);
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (wither.isValid()) {
                    setAttribute(wither, Attribute.SCALE, 5.0);
                }
            }, 1L);
        }
    }

    public String jumpToWave(int wave) {
        if (!gameRunning) {
            return "游戏未运行，无法跳波";
        }
        if (wave < 1 || wave > WAVE_SEQUENCE.length) {
            return "波次范围 1-" + WAVE_SEQUENCE.length;
        }
        startWave(wave - 1);
        return "已跳到第 " + wave + " 波";
    }

    private Location bossPointForTeam(String teamId, TeamDef def) {
        World world = Bukkit.getWorld(def.world());
        if (world == null && !Bukkit.getWorlds().isEmpty()) {
            world = Bukkit.getWorlds().get(0);
        }
        Location center = new Location(world, def.x(), def.y(), def.z());
        double radiusSq = (double) spawnPointTeamRadius * spawnPointTeamRadius;
        Entity best = null;
        double bestSq = Double.MAX_VALUE;
        for (Entity entity : world.getEntities()) {
            if (!entity.getScoreboardTags().contains("rz")
                    || !entity.getScoreboardTags().contains("boss_point")) {
                continue;
            }
            double d = entity.getLocation().distanceSquared(center);
            if (d < radiusSq && d < bestSq) {
                bestSq = d;
                best = entity;
            }
        }
        if (best != null) {
            return best.getLocation();
        }
        return new Location(world, def.x() + 0.5, def.y(), def.z() + 0.5);
    }

    private void declareVictory() {
        Set<String> surviving = survivingTeams();
        if (!surviving.isEmpty()) {
            Bukkit.broadcastMessage(ChatColor.GOLD + "【戴夫】所有波次已完成，存活队伍获胜！");
            for (String team : surviving) {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (team.equals(getTeamName(player))) {
                        player.sendTitle("§6我们是冠军", "§e斗蛐蛐 PvE 冠军", 10, 80, 10);
                    }
                }
            }
        }
        endGame();
    }

    private void killAllMonsters() {
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity instanceof Mob mob && !(mob instanceof Villager)) {
                    if (mob.getScoreboardTags().contains(PvzMode.TAG_MONSTER)) {
                        continue;
                    }
                    if (mob instanceof Wolf wolf && isPetWolf(wolf)) {
                        continue;
                    }
                    mob.remove();
                }
            }
        }
    }

    private void clearDroppedItems() {
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity instanceof Item item) {
                    item.remove();
                }
            }
        }
    }

    private record ChunkCoord(World world, int x, int z) {
    }

    private void thoroughCleanup() {
        Set<ChunkCoord> chunks = new HashSet<>();
        for (TeamDef def : teamDefs.values()) {
            World world = Bukkit.getWorld(def.world());
            if (world == null && !Bukkit.getWorlds().isEmpty()) {
                world = Bukkit.getWorlds().get(0);
            }
            if (world == null) {
                continue;
            }
            addAreaChunks(chunks, world, def.x(), def.z());
        }
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (!entity.getScoreboardTags().contains("rz")) {
                    continue;
                }
                if (entity.getScoreboardTags().contains("summon_point")
                        || entity.getScoreboardTags().contains("boss_point")) {
                    addAreaChunks(chunks, world,
                            entity.getLocation().getBlockX(), entity.getLocation().getBlockZ());
                }
            }
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (isClassicPlayer(player)) {
                addAreaChunks(chunks, player.getWorld(),
                        player.getLocation().getBlockX(), player.getLocation().getBlockZ());
            }
        }
        for (ChunkCoord c : chunks) {
            c.world().addPluginChunkTicket(c.x(), c.z(), plugin);
        }
        killAllMonsters();
        clearDroppedItems();
        for (ChunkCoord c : chunks) {
            c.world().removePluginChunkTicket(c.x(), c.z(), plugin);
        }
    }

    private void addAreaChunks(Set<ChunkCoord> chunks, World world, int blockX, int blockZ) {
        int x0 = (blockX - cleanupRadius) >> 4;
        int x1 = (blockX + cleanupRadius) >> 4;
        int z0 = (blockZ - cleanupRadius) >> 4;
        int z1 = (blockZ + cleanupRadius) >> 4;
        for (int cx = x0; cx <= x1; cx++) {
            for (int cz = z0; cz <= z1; cz++) {
                chunks.add(new ChunkCoord(world, cx, cz));
            }
        }
    }

    private void consolidateAllCurrencies() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (isClassicPlayer(player)) {
                consolidateCurrencies(player);
            }
        }
    }

    void consolidateCurrencies(Player player) {
        normalizeLegacyCurrencies(player);
        for (int i = 0; i < 64; i++) {
            int silver = countCurrency(player, ShopCurrency.SILVER);
            if (silver >= 10) {
                takeCurrency(player, ShopCurrency.SILVER, 10);
                addCurrency(player, ShopCurrency.GOLD, 1, true);
                continue;
            }
            int gold = countCurrency(player, ShopCurrency.GOLD);
            if (gold >= 10) {
                takeCurrency(player, ShopCurrency.GOLD, 10);
                addCurrency(player, ShopCurrency.DIAMOND, 1, true);
                continue;
            }
            break;
        }
    }

    private void normalizeLegacyCurrencies(Player player) {
        for (ShopCurrency currency : ShopCurrency.values()) {
            for (int i = 0; i < player.getInventory().getSize(); i++) {
                ItemStack item = player.getInventory().getItem(i);
                if (item == null || item.getType() != currency.material() || !isCurrency(item, currency)) {
                    continue;
                }
                ItemMeta meta = item.getItemMeta();
                if (meta != null && meta.hasItemName()) {
                    continue;
                }
                int amount = item.getAmount();
                player.getInventory().setItem(i, null);
                addCurrency(player, currency, amount);
            }
        }
    }

    public boolean isPlaying(Player player) {
        if (!gameRunning) {
            return false;
        }
        String team = getTeamName(player);
        return team != null && hasLivingDave(team);
    }

    public void voteMode(Player player, boolean death) {
        modeVotes.put(player.getUniqueId(), death);
        player.sendMessage(ChatColor.GREEN + "【模式投票】你已投票给「"
                + (death ? "死战模式" : "正常模式") + "」");
    }

    public Boolean modeVoteOf(Player player) {
        return modeVotes.get(player.getUniqueId());
    }

    public void voteEconomy(Player player, int level) {
        economyVotes.put(player.getUniqueId(), level);
        player.sendMessage(ChatColor.GREEN + "【经济投票】你已投票给「"
                + (level == 0 ? "低经济局（15 钻）" : level == 1 ? "中经济局（30 钻）" : "高经济局（50 钻）") + "」");
    }

    public Integer economyVoteOf(Player player) {
        return economyVotes.get(player.getUniqueId());
    }

    public int lowEconomyVotes() {
        return countEconomyVotes(0);
    }

    public int midEconomyVotes() {
        return countEconomyVotes(1);
    }

    public int highEconomyVotes() {
        return countEconomyVotes(2);
    }

    private int countEconomyVotes(int level) {
        int count = 0;
        for (Integer vote : economyVotes.values()) {
            if (vote != null && vote == level) {
                count++;
            }
        }
        return count;
    }

    public int normalModeVotes() {
        int count = 0;
        for (Boolean vote : modeVotes.values()) {
            if (!Boolean.TRUE.equals(vote)) {
                count++;
            }
        }
        return count;
    }

    public int deathModeVotes() {
        int count = 0;
        for (Boolean vote : modeVotes.values()) {
            if (Boolean.TRUE.equals(vote)) {
                count++;
            }
        }
        return count;
    }

    public boolean isDeathMode() {
        return deathMode;
    }

    public boolean isCurrencyItem(ItemStack item) {
        return isCurrencyMaterial(item);
    }

    public static boolean isCurrencyMaterial(ItemStack item) {
        if (item == null) {
            return false;
        }
        Material material = item.getType();
        return material == Material.IRON_NUGGET
                || material == Material.GOLD_NUGGET
                || material == Material.DIAMOND;
    }

    public void markWaitingRespawn(Player player) {
        waitingRespawn.add(player.getUniqueId());
    }

    public boolean isBossWave() {
        if (waveIndex < 0 || waveIndex >= WAVE_SEQUENCE.length) {
            return false;
        }
        return WAVE_SEQUENCE[waveIndex] == WAVE_MINIBOSS
                || WAVE_SEQUENCE[waveIndex] == WAVE_BIGBOSS;
    }

    private void respawnWaitingPlayers() {
        if (waitingRespawn.isEmpty()) {
            return;
        }
        for (UUID uuid : new HashSet<>(waitingRespawn)) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null || !player.isOnline()) {
                continue;
            }
            Location respawn = daveRespawnLocation(player);
            if (respawn == null) {
                continue;
            }
            player.teleport(respawn);
            player.setGameMode(GameMode.ADVENTURE);
            player.sendMessage(ChatColor.AQUA + "【复活】休整期已到，你已复活！");
            waitingRespawn.remove(uuid);
        }
    }

    private void cleanupDeathModeCurrency() {
        if (!deathMode) {
            return;
        }
        for (World world : Bukkit.getWorlds()) {
            for (Item item : world.getEntitiesByClass(Item.class)) {
                if (!isCurrencyMaterial(item.getItemStack())) {
                    continue;
                }
                item.remove();
            }
        }
    }

    public boolean hasPlayingPlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (isPlaying(player)) {
                return true;
            }
        }
        return false;
    }

    public boolean isGameRunning() {
        return gameRunning;
    }

    public void handlePlayerQuit(Player player) {
        if (!gameRunning || gameId == null || getTeamName(player) == null || !hasLivingDave(getTeamName(player))) {
            disconnectSnapshots.remove(player.getUniqueId());
            return;
        }
        ItemStack[] inventory = cloneContents(player.getInventory().getContents());
        ItemStack[] enderChest = cloneContents(player.getEnderChest().getContents());
        disconnectSnapshots.put(player.getUniqueId(), new DisconnectSnapshot(
                gameId, System.currentTimeMillis(), inventory, enderChest,
                player.getTotalExperience(), player.getLevel(), player.getExp(),
                List.copyOf(player.getActivePotionEffects())));
    }

    public boolean restoreOnRejoin(Player player) {
        DisconnectSnapshot snapshot = disconnectSnapshots.remove(player.getUniqueId());
        if (snapshot == null || gameId == null || !snapshot.gameId().equals(gameId)) {
            if (gameParticipantSet.contains(player.getUniqueId())) {
                clearRejoinStaleState(player);
                return false;
            }
            return false;
        }
        String team = getTeamName(player);
        if (team == null || !hasLivingDave(team)) {
            clearRejoinStaleState(player);
            return false;
        }
        if (System.currentTimeMillis() - snapshot.disconnectedAt() > 60_000L) {
            clearRejoinStaleState(player);
            return false;
        }
        player.getInventory().clear();
        player.getInventory().setContents(snapshot.inventory());
        player.getEnderChest().clear();
        player.getEnderChest().setContents(snapshot.enderChest());
        clearExperience(player);
        player.setTotalExperience(snapshot.totalExperience());
        player.setLevel(snapshot.level());
        player.setExp(snapshot.expProgress());
        clearPotionEffects(player);
        for (PotionEffect effect : snapshot.effects()) {
            player.addPotionEffect(effect);
        }
        player.setGameMode(GameMode.ADVENTURE);
        participants.add(player.getUniqueId());
        gameParticipantSet.add(player.getUniqueId());
        applyLobbyItems(player);
        TeamDef def = teamDefs.get(team);
        if (def != null) {
            teleportToPlayArea(player, def);
        }
        refreshPlayerListName(player);
        refreshSpawnPointActives();
        refreshBossBars();
        player.sendMessage(ChatColor.GREEN + "【回服】已恢复断线前的个人物品，回到游玩区域");
        return true;
    }

    private static void clearRejoinStaleState(Player player) {
        player.getInventory().clear();
        player.getEnderChest().clear();
        clearExperience(player);
        clearPotionEffects(player);
    }

    private static ItemStack[] cloneContents(ItemStack[] contents) {
        ItemStack[] copy = new ItemStack[contents.length];
        for (int i = 0; i < contents.length; i++) {
            copy[i] = contents[i] == null ? null : contents[i].clone();
        }
        return copy;
    }

    public void returnToLobby(Player player) {
        if (pvzMode != null && pvzMode.isPlaying(player)) {
            pvzMode.returnPlayerToLobby(player);
            return;
        }
        if (isPlaying(player)) {
            String team = getTeamName(player);
            if (team != null) {
                Scoreboard main = Bukkit.getScoreboardManager().getMainScoreboard();
                Team teamObj = main.getTeam(team);
                if (teamObj != null) {
                    teamObj.removeEntry(player.getName());
                }
            }
            participants.remove(player.getUniqueId());
        }
        markUnready(player);
        player.getInventory().clear();
        player.getEnderChest().clear();
        player.setGameMode(GameMode.ADVENTURE);
        applyLobbyItems(player);
        refreshPlayerListName(player);
        teleportToLobby(player);
    }

    public Villager livingDaveOf(Player player) {
        String team = getTeamName(player);
        if (team == null) {
            return null;
        }
        for (World world : Bukkit.getWorlds()) {
            for (Villager villager : allDaves(world)) {
                if (!villager.isDead() && team.equals(getOwnerTeam(villager))) {
                    return villager;
                }
            }
        }
        return null;
    }

    public void scheduleForceRespawn(Player player) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                player.spigot().respawn();
            }
        }, 5L);
    }

    public void scheduleReturnToAdventure(Player player) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline() && isPlaying(player)) {
                Location respawn = daveRespawnLocation(player);
                if (respawn != null) {
                    player.teleport(respawn);
                }
                player.setGameMode(GameMode.ADVENTURE);
                player.sendMessage(ChatColor.AQUA + "【复活】你已复活，继续战斗！");
            }
        }, 200L);
    }

    public Location daveRespawnLocation(Player player) {
        Villager dave = livingDaveOf(player);
        if (dave == null) {
            return null;
        }
        String team = getTeamName(player);
        double zOffset = ("red".equals(team) || "yellow".equals(team)) ? -3.0 : 3.0;
        return dave.getLocation().add(0.5, 1, zOffset);
    }

    private void buildGameScoreboard() {
        Scoreboard main = Bukkit.getScoreboardManager().getMainScoreboard();
        gameScoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        for (Team team : main.getTeams()) {
            Team copy = gameScoreboard.registerNewTeam(team.getName());
            copy.setColor(team.getColor());
            copy.setPrefix(team.getPrefix());
            copy.setSuffix(team.getSuffix());
            for (String entry : team.getEntries()) {
                copy.addEntry(entry);
            }
        }
        gameSidebar = gameScoreboard.registerNewObjective("davepve_info", "dummy", "斗蛐蛐");
        gameSidebar.setDisplaySlot(DisplaySlot.SIDEBAR);
    }

    private void assignGameScoreboard(Player player) {
        if (gameScoreboard != null) {
            player.setScoreboard(gameScoreboard);
        }
    }

    private void resetGameScoreboard() {
        Scoreboard main = Bukkit.getScoreboardManager().getMainScoreboard();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (isClassicPlayer(player)) {
                player.setScoreboard(main);
            }
        }
        gameScoreboard = null;
        gameSidebar = null;
    }

    private void updateGameSidebar() {
        if (!gameRunning || gameSidebar == null) {
            return;
        }
        Scoreboard main = Bukkit.getScoreboardManager().getMainScoreboard();
        for (Team team : main.getTeams()) {
            Team copy = gameScoreboard.getTeam(team.getName());
            if (copy == null) {
                continue;
            }
            for (String entry : team.getEntries()) {
                if (!copy.hasEntry(entry)) {
                    copy.addEntry(entry);
                }
            }
        }
        for (String score : gameScoreboard.getEntries()) {
            gameScoreboard.resetScores(score);
        }
        int wave = Math.min(waveIndex + 1, WAVE_SEQUENCE.length);
        gameSidebar.getScore(ChatColor.GOLD + "第 " + ChatColor.RED + wave + ChatColor.GOLD + " 波").setScore(5);
        String phase;
        if (restActive) {
            phase = ChatColor.GREEN + "休整中";
        } else if (WAVE_SEQUENCE[waveIndex] == WAVE_BIGBOSS) {
            phase = ChatColor.DARK_PURPLE + "大boss来袭";
        } else if (WAVE_SEQUENCE[waveIndex] == WAVE_MINIBOSS) {
            phase = ChatColor.LIGHT_PURPLE + "小boss来袭";
        } else {
            phase = ChatColor.YELLOW + "出怪中";
        }
        gameSidebar.getScore(phase).setScore(4);
        long remainingSec = restActive
                ? Math.max(0, (restEndTick - Bukkit.getCurrentTick()) / 20)
                : Math.max(0, (REST_INTERVAL_MILLIS - (System.currentTimeMillis() - phaseStartMillis)) / 1000);
        gameSidebar.getScore(ChatColor.AQUA + "下一阶段 " + remainingSec + " 秒").setScore(3);
        long totalSec = Math.max(0, (System.currentTimeMillis() - gameStartMillis) / 1000);
        gameSidebar.getScore(ChatColor.AQUA + "总时长 " + formatTime(totalSec)).setScore(2);
    }

    private static String formatTime(long sec) {
        return String.format(Locale.ROOT, "%02d:%02d", sec / 60, sec % 60);
    }

    public void joinMidGame(Player player) {
        if (!gameRunning) {
            player.sendMessage(ChatColor.YELLOW + "【加入】游戏未开始，请先 /davepve ready 准备");
            return;
        }
        if (getTeamName(player) != null || participants.contains(player.getUniqueId())) {
            player.sendMessage(ChatColor.YELLOW + "【加入】你已经在游玩中");
            return;
        }
        if (participants.size() >= MAX_PARTICIPANTS) {
            player.sendMessage(ChatColor.RED + "【加入】本局参赛人数已满（20 人）");
            return;
        }
        String team = pickMidGameTeam();
        if (team == null) {
            player.sendMessage(ChatColor.RED + "【加入】当前没有可加入的队伍（需要存在存活戴夫的队伍）");
            return;
        }
        Scoreboard main = Bukkit.getScoreboardManager().getMainScoreboard();
        Team scoreTeam = main.getTeam(team);
        if (scoreTeam != null) {
            scoreTeam.addEntry(player.getName());
        }
        player.getInventory().clear();
        player.getEnderChest().clear();
        clearExperience(player);
        clearPotionEffects(player);
        player.setGameMode(GameMode.ADVENTURE);
        TeamDef def = teamDefs.get(team);
        if (def != null) {
            teleportToPlayArea(player, def);
        }
        giveStarterGear(player, team);
        participants.add(player.getUniqueId());
        gameParticipantSet.add(player.getUniqueId());
        assignGameScoreboard(player);
        refreshPlayerListName(player);
        refreshSpawnPointActives();
        player.sendMessage(ChatColor.GREEN + "【加入】你已加入" + displayName(team) + "，守护本队戴夫！");
    }

    private String pickMidGameTeam() {
        String best = null;
        int bestCount = Integer.MAX_VALUE;
        for (String id : TEAM_IDS) {
            if (!hasLivingDave(id)) {
                continue;
            }
            int count = countPlayingPlayers(id);
            if (count < bestCount) {
                bestCount = count;
                best = id;
            }
        }
        return best;
    }

    public void startSpectate(Player player) {
        Player target = null;
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (isPlaying(p)) {
                target = p;
                break;
            }
        }
        player.setGameMode(GameMode.SPECTATOR);
        player.getInventory().clear();
        applySpectatorItems(player);
        if (target != null) {
            player.teleport(target.getLocation());
            player.sendMessage(ChatColor.LIGHT_PURPLE + "【旁观】已进入全局观战，可自由移动；打开背包可退出观战");
        } else {
            player.sendMessage(ChatColor.LIGHT_PURPLE + "【旁观】当前没有游玩中的玩家，你仍处于旁观模式");
        }
    }

    public void exitSpectate(Player player) {
        player.getInventory().clear();
        player.setGameMode(GameMode.ADVENTURE);
        applyLobbyItems(player);
        refreshPlayerListName(player);
        teleportToLobby(player);
        player.sendMessage(ChatColor.LIGHT_PURPLE + "【旁观】你已退出观战，返回大厅");
    }

    public ItemStack createSpectatorExitItem() {
        ItemStack stack = new ItemStack(Material.BARRIER);
        ItemMeta meta = stack.getItemMeta();
        meta.setItemName("退出观战");
        meta.setLore(List.of(ChatColor.YELLOW + "输入 /lb 返回大厅退出观战"));
        meta.getPersistentDataContainer().set(spectatorExitKey, PersistentDataType.BYTE, (byte) 1);
        stack.setItemMeta(meta);
        return stack;
    }

    public boolean isSpectatorExitItem(ItemStack item) {
        return item != null && item.getItemMeta() != null
                && item.getItemMeta().getPersistentDataContainer().has(spectatorExitKey, PersistentDataType.BYTE);
    }

    private void applySpectatorItems(Player player) {
        player.getInventory().setItem(8, createSpectatorExitItem());
    }

    public boolean buyWolf(Player player) {
        if (hasWolf(player)) {
            player.sendMessage(ChatColor.RED + "【宠物】你已拥有一条狼，无法再购买");
            return false;
        }
        Wolf wolf = player.getWorld().spawn(player.getLocation(), Wolf.class, w -> {
            w.setOwner(player);
            w.setTamed(true);
            w.setCustomName(player.getName() + " 的狼");
            w.setCustomNameVisible(true);
            w.setPersistent(true);
        });
        if (wolf == null) {
            player.sendMessage(ChatColor.RED + "【宠物】生成狼失败");
            return false;
        }
        playerWolves.put(player.getUniqueId(), wolf.getUniqueId());
        applyWolfBuffs(player);
        return true;
    }

    public Wolf wolfOf(Player player) {
        UUID wolfId = playerWolves.get(player.getUniqueId());
        if (wolfId == null) {
            return null;
        }
        Entity wolf = Bukkit.getEntity(wolfId);
        return wolf instanceof Wolf w && w.isValid() && !w.isDead() ? w : null;
    }

    public boolean isPetWolf(Wolf wolf) {
        return playerWolves.containsValue(wolf.getUniqueId());
    }

    public UUID ownerOfWolf(Wolf wolf) {
        for (Map.Entry<UUID, UUID> entry : playerWolves.entrySet()) {
            if (entry.getValue().equals(wolf.getUniqueId())) {
                return entry.getKey();
            }
        }
        return null;
    }

    public Player resolveMonsterKiller(Mob mob) {
        Player killer = mob.getKiller();
        if (killer != null) {
            return killer;
        }
        UUID bombOwner = bombKills.remove(mob.getUniqueId());
        if (bombOwner != null) {
            return Bukkit.getPlayer(bombOwner);
        }
        UUID owner = wolfKills.remove(mob.getUniqueId());
        if (owner != null) {
            return Bukkit.getPlayer(owner);
        }
        Map<UUID, Double> damage = damageLogs.get(mob.getUniqueId());
        if (damage != null && !damage.isEmpty()) {
            UUID best = null;
            double bestDamage = 0;
            for (Map.Entry<UUID, Double> entry : damage.entrySet()) {
                if (entry.getValue() > bestDamage) {
                    bestDamage = entry.getValue();
                    best = entry.getKey();
                }
            }
            if (best != null) {
                return Bukkit.getPlayer(best);
            }
        }
        return null;
    }

    public void upgradeWolfHealth(Player player) {
        int[] buffs = wolfBuffs.computeIfAbsent(player.getUniqueId(), k -> new int[3]);
        buffs[0] += 1;
        applyWolfBuffs(player);
        player.sendMessage(ChatColor.GREEN + "【宠物】狼生命上限 +2（当前加成 +" + (buffs[0] * 2) + "）");
    }

    public void upgradeWolfDamage(Player player) {
        int[] buffs = wolfBuffs.computeIfAbsent(player.getUniqueId(), k -> new int[3]);
        buffs[1] += 1;
        applyWolfBuffs(player);
        player.sendMessage(ChatColor.GREEN + "【宠物】狼攻击伤害 +1（当前加成 +" + buffs[1] + "）");
    }

    public void upgradeWolfSpeed(Player player) {
        int[] buffs = wolfBuffs.computeIfAbsent(player.getUniqueId(), k -> new int[3]);
        buffs[2] = Math.min(10, buffs[2] + 1);
        applyWolfBuffs(player);
        player.sendMessage(ChatColor.GREEN + "【宠物】狼移动速度 +0.1 倍（当前加成 +" + (buffs[2] * 0.1) + " 倍）");
    }

    private int wolfSpeedCount(Player player) {
        int[] buffs = wolfBuffs.get(player.getUniqueId());
        return buffs == null ? 0 : buffs[2];
    }

    private void applyWolfBuffs(Player player) {
        Wolf wolf = wolfOf(player);
        if (wolf == null) {
            return;
        }
        int[] buffs = wolfBuffs.getOrDefault(player.getUniqueId(), new int[3]);
        wolf.setMaxHealth(20.0 + buffs[0] * 2);
        wolf.setHealth(wolf.getMaxHealth());
        AttributeInstance atk = wolf.getAttribute(Attribute.ATTACK_DAMAGE);
        if (atk != null) {
            atk.setBaseValue(4.0 + buffs[1]);
        }
        AttributeInstance spd = wolf.getAttribute(Attribute.MOVEMENT_SPEED);
        if (spd != null) {
            spd.setBaseValue(0.3 * (1.0 + buffs[2] * 0.1));
        }
    }

    public boolean hasWolf(Player player) {
        if (wolfRespawns.containsKey(player.getUniqueId())) {
            return true;
        }
        UUID wolfId = playerWolves.get(player.getUniqueId());
        if (wolfId == null) {
            return false;
        }
        Entity wolf = Bukkit.getEntity(wolfId);
        if (wolf instanceof Wolf w && w.isValid() && !w.isDead() && player.equals(w.getOwner())) {
            return true;
        }
        playerWolves.remove(player.getUniqueId());
        return false;
    }

    public void handleWolfDeath(Wolf wolf) {
        if (wolf.getOwner() instanceof Player player) {
            playerWolves.remove(player.getUniqueId());
            wolfRespawns.put(player.getUniqueId(), System.currentTimeMillis() + 20_000L);
        } else {
            playerWolves.entrySet().removeIf(entry -> entry.getValue().equals(wolf.getUniqueId()));
        }
    }

    private void refreshWolfRespawns() {
        if (wolfRespawns.isEmpty()) {
            return;
        }
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<UUID, Long>> it = wolfRespawns.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Long> entry = it.next();
            if (now < entry.getValue()) {
                continue;
            }
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player == null || !player.isOnline()) {
                continue;
            }
            Wolf wolf = player.getWorld().spawn(player.getLocation(), Wolf.class, w -> {
                w.setOwner(player);
                w.setTamed(true);
                w.setCustomName(player.getName() + " 的狼");
                w.setCustomNameVisible(true);
                w.setPersistent(true);
            });
            if (wolf != null) {
                playerWolves.put(player.getUniqueId(), wolf.getUniqueId());
                applyWolfBuffs(player);
            }
            it.remove();
        }
    }

    private void clearPets() {
        for (UUID wolfId : playerWolves.values()) {
            Entity wolf = Bukkit.getEntity(wolfId);
            if (wolf != null) {
                wolf.remove();
            }
        }
        playerWolves.clear();
        wolfRespawns.clear();
        wolfBuffs.clear();
    }

    public boolean canBuyDaveHeal(Player player) {
        long now = System.currentTimeMillis();
        long last = daveHealCooldowns.getOrDefault(player.getUniqueId(), 0L);
        if (now - last < 1500L) {
            player.sendMessage(ChatColor.YELLOW + "【戴夫增益】购买冷却中，请稍候");
            return false;
        }
        Villager dave = livingDaveOf(player);
        if (dave == null) {
            player.sendMessage(ChatColor.RED + "【戴夫增益】本队戴夫不存在");
            return false;
        }
        if (dave.getHealth() >= MAX_HEALTH) {
            player.sendMessage(ChatColor.GOLD + "【戴夫增益】戴夫已满血，未扣除金币");
            return false;
        }
        return true;
    }

    public void applyDaveHeal(Player player) {
        Villager dave = livingDaveOf(player);
        if (dave == null) {
            return;
        }
        double heal = Math.min(10.0, MAX_HEALTH - dave.getHealth());
        dave.setHealth(Math.min(MAX_HEALTH, dave.getHealth() + heal));
        daveHealCooldowns.put(player.getUniqueId(), System.currentTimeMillis());
        player.sendMessage(ChatColor.GREEN + "【戴夫增益】戴夫恢复 " + (int) Math.ceil(heal) + " 点生命");
    }

    public boolean canBuyDaveResistance(Player player) {
        if (livingDaveOf(player) == null) {
            player.sendMessage(ChatColor.RED + "【戴夫增益】本队戴夫不存在");
            return false;
        }
        return true;
    }

    public void applyDaveResistance(Player player) {
        Villager dave = livingDaveOf(player);
        if (dave != null) {
            dave.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 1200, 0, false, true, true));
            player.sendMessage(ChatColor.GREEN + "【戴夫增益】戴夫获得 60 秒抗性提升 I");
        }
    }

    public Inventory openTeamChest(String team) {
        return chestManager == null ? null : chestManager.getChest(team);
    }

    public boolean isOwnedStand(Location loc) {
        return brewingManager != null && brewingManager.isOwnedStand(loc);
    }

    public boolean isShopReturnItem(ItemStack item) {
        return item != null && item.getItemMeta() != null
                && item.getItemMeta().getPersistentDataContainer().has(shopReturnKey, PersistentDataType.BYTE);
    }

    private void removeShopReturnItems(Player player) {
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (isShopReturnItem(item)) {
                player.getInventory().setItem(i, null);
            }
        }
    }

    public void buyTeamBuff(Player player, PotionEffectType type) {
        String team = getTeamName(player);
        if (team == null) {
            player.sendMessage(ChatColor.RED + "【星币商店】你还没有队伍，无法购买团队效果");
            playPurchaseFail(player);
            return;
        }
        if (teamBuffs.getOrDefault(team, Map.of()).containsKey(type)) {
            player.sendMessage(ChatColor.YELLOW + "【星币商店】本队已购买该效果，不可重复购买");
            playPurchaseFail(player);
            return;
        }
        if (countCurrency(player, ShopCurrency.STAR) < 1) {
            player.sendMessage(ChatColor.RED + "【星币商店】星币不足，需要 1 星币");
            playPurchaseFail(player);
            return;
        }
        takeCurrency(player, ShopCurrency.STAR, 1);
        teamBuffs.computeIfAbsent(team, k -> new HashMap<>()).put(type, 1);
        applyTeamBuff(team, type);
        player.sendMessage(ChatColor.GREEN + "【星币商店】已购买：" + effectDisplayName(type)
                + "，本队所有队员获得永久效果（死亡不清除）");
        playPurchaseSuccess(player);
    }

    public void buyTeamMovement(Player player) {
        String team = getTeamName(player);
        if (team == null) {
            player.sendMessage(ChatColor.RED + "【星币商店】你还没有队伍，无法购买团队效果");
            playPurchaseFail(player);
            return;
        }
        if (teamBuffs.getOrDefault(team, Map.of()).containsKey(PotionEffectType.SPEED)) {
            player.sendMessage(ChatColor.YELLOW + "【星币商店】本队已购买该效果，不可重复购买");
            playPurchaseFail(player);
            return;
        }
        if (countCurrency(player, ShopCurrency.STAR) < 1) {
            player.sendMessage(ChatColor.RED + "【星币商店】星币不足，需要 1 星币");
            playPurchaseFail(player);
            return;
        }
        takeCurrency(player, ShopCurrency.STAR, 1);
        Map<PotionEffectType, Integer> buffs = teamBuffs.computeIfAbsent(team, k -> new HashMap<>());
        buffs.put(PotionEffectType.SPEED, 2);
        buffs.put(PotionEffectType.JUMP_BOOST, 3);
        applyTeamBuff(team, PotionEffectType.SPEED);
        applyTeamBuff(team, PotionEffectType.JUMP_BOOST);
        player.sendMessage(ChatColor.GREEN + "【星币商店】已购买：玩家移动升级，"
                + "本队所有玩家与宠物获得永久的 速度 III 与 跳跃提升 IV（死亡不清除）");
        playPurchaseSuccess(player);
    }

    public void buyTeamHaste(Player player) {
        String team = getTeamName(player);
        if (team == null) {
            player.sendMessage(ChatColor.RED + "【星币商店】你还没有队伍，无法购买团队效果");
            playPurchaseFail(player);
            return;
        }
        if (teamBuffs.getOrDefault(team, Map.of()).containsKey(PotionEffectType.HASTE)) {
            player.sendMessage(ChatColor.YELLOW + "【星币商店】本队已购买该效果，不可重复购买");
            playPurchaseFail(player);
            return;
        }
        if (countCurrency(player, ShopCurrency.STAR) < 1) {
            player.sendMessage(ChatColor.RED + "【星币商店】星币不足，需要 1 星币");
            playPurchaseFail(player);
            return;
        }
        takeCurrency(player, ShopCurrency.STAR, 1);
        teamBuffs.computeIfAbsent(team, k -> new HashMap<>()).put(PotionEffectType.HASTE, 2);
        applyTeamBuff(team, PotionEffectType.HASTE);
        player.sendMessage(ChatColor.GREEN + "【星币商店】已购买：玩家攻速升级，"
                + "本队所有玩家与宠物获得永久的 急迫 III（死亡不清除）");
        playPurchaseSuccess(player);
    }

    private void applyTeamBuff(String team, PotionEffectType type) {
        int amplifier = teamBuffs.getOrDefault(team, Map.of()).getOrDefault(type, 1);
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (team.equals(getTeamName(player))) {
                player.addPotionEffect(new PotionEffect(type, Integer.MAX_VALUE, amplifier, false, true, true));
                Wolf wolf = wolfOf(player);
                if (wolf != null) {
                    wolf.addPotionEffect(new PotionEffect(type, Integer.MAX_VALUE, amplifier, false, true, true));
                }
            }
        }
    }

    private void refreshTeamBuffs() {
        if (!gameRunning || teamBuffs.isEmpty()) {
            return;
        }
        for (Map.Entry<String, Map<PotionEffectType, Integer>> entry : teamBuffs.entrySet()) {
            for (PotionEffectType type : entry.getValue().keySet()) {
                applyTeamBuff(entry.getKey(), type);
            }
        }
    }

    private void clearTeamBuffs() {
        teamBuffs.clear();
    }

    public void buyDaveSlowAura(Player player) {
        String team = getTeamName(player);
        if (team == null) {
            player.sendMessage(ChatColor.RED + "【星币商店】你还没有队伍，无法购买戴夫光环");
            playPurchaseFail(player);
            return;
        }
        if (livingDaveOf(player) == null) {
            player.sendMessage(ChatColor.RED + "【星币商店】本队戴夫不存在，无法购买");
            playPurchaseFail(player);
            return;
        }
        if (daveSlowAuraTeams.contains(team)) {
            player.sendMessage(ChatColor.YELLOW + "【星币商店】本队已购买缓速光环，不可重复购买");
            playPurchaseFail(player);
            return;
        }
        if (countCurrency(player, ShopCurrency.STAR) < 1) {
            player.sendMessage(ChatColor.RED + "【星币商店】星币不足，需要 1 星币");
            playPurchaseFail(player);
            return;
        }
        takeCurrency(player, ShopCurrency.STAR, 1);
        daveSlowAuraTeams.add(team);
        player.sendMessage(ChatColor.GREEN + "【星币商店】已购买：戴夫缓速光环，本队戴夫 10 格内怪物持续缓速");
        playPurchaseSuccess(player);
    }

    public void buyDaveBuff(Player player, PotionEffectType type) {
        String team = getTeamName(player);
        if (team == null) {
            player.sendMessage(ChatColor.RED + "【星币商店】你还没有队伍，无法购买戴夫效果");
            playPurchaseFail(player);
            return;
        }
        if (livingDaveOf(player) == null) {
            player.sendMessage(ChatColor.RED + "【星币商店】本队戴夫不存在，无法购买");
            playPurchaseFail(player);
            return;
        }
        if (daveBuffs.getOrDefault(team, Map.of()).containsKey(type)) {
            player.sendMessage(ChatColor.YELLOW + "【星币商店】本队已购买该效果，不可重复购买");
            playPurchaseFail(player);
            return;
        }
        if (countCurrency(player, ShopCurrency.STAR) < 1) {
            player.sendMessage(ChatColor.RED + "【星币商店】星币不足，需要 1 星币");
            playPurchaseFail(player);
            return;
        }
        takeCurrency(player, ShopCurrency.STAR, 1);
        int amplifier = 1;
        daveBuffs.computeIfAbsent(team, k -> new HashMap<>()).put(type, amplifier);
        applyDaveBuff(team);
        player.sendMessage(ChatColor.GREEN + "【星币商店】已购买：戴夫" + effectDisplayName(type) + "（本局永久）");
        playPurchaseSuccess(player);
    }

    private void applyDaveBuff(String team) {
        Villager dave = teamDave(team);
        if (dave == null) {
            return;
        }
        for (Map.Entry<PotionEffectType, Integer> entry : daveBuffs.getOrDefault(team, Map.of()).entrySet()) {
            dave.addPotionEffect(new PotionEffect(entry.getKey(), Integer.MAX_VALUE, entry.getValue(), false, true, true));
        }
    }

    private void refreshDaveBuffs() {
        if (!gameRunning || daveBuffs.isEmpty()) {
            return;
        }
        for (String team : daveBuffs.keySet()) {
            if (daveBuffs.get(team).containsKey(PotionEffectType.REGENERATION)) {
                Villager dave = teamDave(team);
                if (dave != null && !dave.isDead() && dave.getHealth() < dave.getMaxHealth()) {
                    dave.setHealth(Math.min(dave.getMaxHealth(), dave.getHealth() + 2.0));
                }
            }
            applyDaveBuff(team);
        }
    }

    private void refreshDaveSlowAuras() {
        if (!gameRunning || daveSlowAuraTeams.isEmpty()) {
            return;
        }
        for (String team : daveSlowAuraTeams) {
            Villager dave = teamDave(team);
            if (dave == null || dave.isDead()) {
                continue;
            }
            for (Entity entity : dave.getWorld().getNearbyEntities(dave.getLocation(), 15, 15, 15)) {
                if (entity instanceof Mob mob && isMonster(mob)) {
                    mob.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 200, 1, false, true, true));
                }
            }
        }
    }

    private static void clearPotionEffects(Player player) {
        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }
    }

    public void saveTeamChest(String team, Inventory inventory) {
        if (chestManager != null) {
            chestManager.saveChest(team, inventory);
        }
    }

    public int teamChestSize() {
        return teamChestSize;
    }

    public String infoSummary() {
        StringBuilder builder = new StringBuilder("准备玩家：" + readyCount() + " 人\n队伍：");
        Scoreboard main = Bukkit.getScoreboardManager().getMainScoreboard();
        for (String id : TEAM_IDS) {
            Team team = main.getTeam(id);
            int size = team == null ? 0 : team.getEntries().size();
            builder.append(TEAM_DISPLAYS.get(id)).append("=").append(size).append("人 ");
        }
        builder.append("\n戴夫状态：");
        for (World world : Bukkit.getWorlds()) {
            for (Villager villager : allDaves(world)) {
                String team = getOwnerTeam(villager);
                builder.append("\n- HP=").append((int) Math.ceil(villager.getHealth()));
                if (team == null) {
                    builder.append("（未绑定）");
                } else {
                    BossBar bar = bars.get(villager.getUniqueId());
                    builder.append(" 队伍=").append(team);
                    builder.append(" 血条=").append(bar == null ? "无" : String.format("%.2f", bar.getProgress()));
                }
            }
        }
        return builder.toString();
    }

    private void spawnDave(TeamDef def, String teamId) {
        World world = Bukkit.getWorld(def.world());
        if (world == null && !Bukkit.getWorlds().isEmpty()) {
            world = Bukkit.getWorlds().get(0);
        }
        if (world == null) {
            return;
        }
        float yaw = ("red".equals(teamId) || "yellow".equals(teamId)) ? 180f : 0f;
        Location location = new Location(world, def.x() + 0.5, def.y(), def.z() + 0.5, yaw, 0f);
        Villager villager = world.spawn(location, Villager.class, v -> {
            v.setAI(false);
            v.setPersistent(true);
            v.setRemoveWhenFarAway(false);
            v.setMaxHealth(MAX_HEALTH);
            v.setHealth(MAX_HEALTH);
            v.setCustomName(teamColor(teamId) + "戴夫");
            v.setCustomNameVisible(true);
            v.addScoreboardTag("rz");
            v.addScoreboardTag("dave");
            v.getPersistentDataContainer().set(ownerTeamKey, PersistentDataType.STRING, teamId);
        });
        createBar(villager, teamId);
    }

    private void clearAllDaves() {
        for (World world : Bukkit.getWorlds()) {
            for (Villager villager : allDaves(world)) {
                BossBar bar = bars.remove(villager.getUniqueId());
                if (bar != null) {
                    bar.removeAll();
                }
                villager.remove();
            }
        }
    }

    private void clearAllChests() {
        if (chestManager != null) {
            chestManager.clearAllTeamChests();
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (isClassicPlayer(player)) {
                player.getEnderChest().clear();
            }
        }
    }

    private void cleanupNearDave(Villager dave) {
        double radiusSq = (double) cleanupRadius * cleanupRadius;
        for (Entity entity : dave.getWorld().getNearbyEntities(dave.getLocation(), cleanupRadius, cleanupRadius, cleanupRadius)) {
            if (entity instanceof Item
                    || (entity instanceof Mob mob && !(mob instanceof Villager))) {
                if (entity instanceof Wolf wolf && isPetWolf(wolf)) {
                    continue;
                }
                entity.remove();
            }
        }
    }

    private void refreshLobbyBar() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (isClassicPlayer(player) && !isPlaying(player)) {
                player.setFoodLevel(20);
                player.setSaturation(5f);
            }
        }
        int online = 0;
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (isClassicPlayer(player)) {
                online++;
            }
        }
        int ready = readyCount();
        if (gameRunning || online == 0) {
            if (lobbyBar != null) {
                lobbyBar.removeAll();
                lobbyBar = null;
            }
            if (autoStartBar != null) {
                autoStartBar.removeAll();
                autoStartBar = null;
            }
            return;
        }
        int bracket;
        long target;
        if (ready >= 10) {
            bracket = 2;
            target = 200L;
        } else if (ready >= 1) {
            bracket = 1;
            target = 3600L;
        } else {
            bracket = 0;
            target = 0L;
        }
        if (bracket == 0) {
            if (autoStartBracket != 0) {
                autoStartBracket = 0;
                autoStartCountdown = 0;
                autoStartReadyCount = 0;
                Bukkit.broadcastMessage(ChatColor.YELLOW + "【自动开始】准备人数不足，自动开始已取消");
            }
        } else {
            if (autoStartBracket != bracket) {
                autoStartBracket = bracket;
                autoStartCountdown = target;
                autoStartReadyCount = ready;
                Bukkit.broadcastMessage(ChatColor.YELLOW + "【自动开始】准备人数 " + ready + " 人，"
                        + (bracket == 2 ? "10 秒" : "3 分钟") + "后自动开始（管理员可手动 /davepve start）");
            } else if (bracket == 2 && ready != autoStartReadyCount) {
                autoStartCountdown = 200L;
                autoStartReadyCount = ready;
                Bukkit.broadcastMessage(ChatColor.YELLOW + "【自动开始】准备人数变化为 " + ready
                        + " 人，重新开始 10 秒倒计时");
            }
            autoStartCountdown -= 20L;
            if (autoStartCountdown <= 0L) {
                autoStartCountdown = 0L;
                autoStartBracket = 0;
                autoStartReadyCount = 0;
                startGame();
                return;
            }
        }
        if (autoStartBracket != 0) {
            if (autoStartBar == null) {
                autoStartBar = Bukkit.createBossBar("", BarColor.YELLOW, BarStyle.SOLID);
            }
            long remainingSec = Math.max(1, (autoStartCountdown + 19) / 20);
            autoStartBar.setTitle(ChatColor.GOLD + "游戏将在 " + remainingSec + " 秒后开始");
            autoStartBar.setProgress(Math.min(1.0, (double) autoStartCountdown / target));
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (isClassicPlayer(player) && isReady(player) && !isPlaying(player)) {
                    autoStartBar.addPlayer(player);
                } else {
                    autoStartBar.removePlayer(player);
                }
            }
        } else if (autoStartBar != null) {
            autoStartBar.removeAll();
            autoStartBar = null;
        }
        if (lobbyBar == null) {
            lobbyBar = Bukkit.createBossBar("", BarColor.GREEN, BarStyle.SOLID);
        }
        lobbyBar.setTitle("已准备：" + ready + " / " + online);
        lobbyBar.setProgress(online == 0 ? 0.0 : Math.min(1.0, (double) ready / online));
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (isClassicPlayer(player)) {
                lobbyBar.addPlayer(player);
                applyLobbyItems(player);
                refreshPlayerListName(player);
            } else {
                lobbyBar.removePlayer(player);
            }
        }
    }

    private void applyLobbyResistance() {
        World world = Bukkit.getWorld(lobbyWorld);
        if (world == null && !Bukkit.getWorlds().isEmpty()) {
            world = Bukkit.getWorlds().get(0);
        }
        if (world == null) {
            return;
        }
        Location lobby = new Location(world, lobbyX, lobbyY, lobbyZ);
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!isClassicPlayer(player) || isPlaying(player)) {
                continue;
            }
            if (!player.getWorld().equals(world)
                    || player.getLocation().distanceSquared(lobby) > 60.0 * 60.0) {
                removeLobbyResistance(player);
            } else {
                player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 100, 4, false, true, true));
            }
        }
    }

    private static void removeLobbyResistance(Player player) {
        for (PotionEffect effect : player.getActivePotionEffects()) {
            if (effect.getType() == PotionEffectType.RESISTANCE && effect.getAmplifier() >= 4) {
                player.removePotionEffect(PotionEffectType.RESISTANCE);
                break;
            }
        }
    }

    private void refreshSpawnPointActives() {
        double radiusSq = (double) spawnPointTeamRadius * spawnPointTeamRadius;
        Map<String, List<Entity>> owned = new HashMap<>();
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (!entity.getScoreboardTags().contains("rz")
                        || !entity.getScoreboardTags().contains("summon_point")
                        || entity.getScoreboardTags().contains("boss_point")) {
                    continue;
                }
                String owner = null;
                double bestSq = radiusSq;
                for (String id : TEAM_IDS) {
                    Villager dave = teamDave(id);
                    if (dave == null || !dave.getWorld().equals(world)) {
                        continue;
                    }
                    double d = dave.getLocation().distanceSquared(entity.getLocation());
                    if (d < bestSq) {
                        bestSq = d;
                        owner = id;
                    }
                }
                if (owner == null) {
                    entity.addScoreboardTag("disabled");
                    continue;
                }
                owned.computeIfAbsent(owner, k -> new ArrayList<>()).add(entity);
            }
        }
        for (Map.Entry<String, List<Entity>> entry : owned.entrySet()) {
            String team = entry.getKey();
            Villager dave = teamDave(team);
            if (dave == null) {
                for (Entity point : entry.getValue()) {
                    point.addScoreboardTag("disabled");
                }
                continue;
            }
            entry.getValue().sort(Comparator.comparingDouble(e ->
                    e.getLocation().distanceSquared(dave.getLocation())));
            int active = countPlayingPlayers(team);
            for (int i = 0; i < entry.getValue().size(); i++) {
                if (i < active) {
                    entry.getValue().get(i).removeScoreboardTag("disabled");
                } else {
                    entry.getValue().get(i).addScoreboardTag("disabled");
                }
            }
        }
    }

    private int countPlayingPlayers(String team) {
        int count = 0;
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (team.equals(getTeamName(player)) && isPlaying(player)) {
                count++;
            }
        }
        return count;
    }

    private void cleanupGroundEntities() {
        for (World world : Bukkit.getWorlds()) {
            for (ExperienceOrb orb : world.getEntitiesByClass(ExperienceOrb.class)) {
                orb.remove();
            }
            for (Arrow arrow : world.getEntitiesByClass(Arrow.class)) {
                if (arrow.isInBlock()) {
                    arrow.remove();
                }
            }
            for (SpectralArrow arrow : world.getEntitiesByClass(SpectralArrow.class)) {
                if (arrow.isInBlock()) {
                    arrow.remove();
                }
            }
        }
    }

    private void cleanupGroundItems() {
        for (World world : Bukkit.getWorlds()) {
            for (Item item : world.getEntitiesByClass(Item.class)) {
                item.remove();
            }
        }
    }

    // ==================== 刷怪点系统（v1.21.0 由插件接管） ====================

    public void spawnPointCreate(Player player) {
        Location loc = player.getLocation();
        loc.getWorld().spawn(loc, ArmorStand.class, stand -> {
            stand.setInvisible(true);
            stand.setGravity(false);
            stand.setInvulnerable(true);
            stand.setSmall(true);
            stand.setDisabledSlots(EquipmentSlot.HAND, EquipmentSlot.OFF_HAND,
                    EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET);
            stand.setCanPickupItems(false);
            stand.setSilent(true);
            stand.setCustomName(ChatColor.YELLOW + "刷怪点");
            stand.setCustomNameVisible(false);
            stand.addScoreboardTag("rz");
            stand.addScoreboardTag("summon_point");
        });
        player.sendMessage(ChatColor.GREEN + "【刷怪点】已创建刷怪点（隐形标记）");
    }

    public void spawnPointDelete(Player player) {
        Location loc = player.getLocation();
        ArmorStand nearest = null;
        double nearestSq = SPAWN_POINT_DELETE_RADIUS * SPAWN_POINT_DELETE_RADIUS;
        for (Entity entity : loc.getWorld().getNearbyEntities(loc, SPAWN_POINT_DELETE_RADIUS, SPAWN_POINT_DELETE_RADIUS, SPAWN_POINT_DELETE_RADIUS)) {
            if (!(entity instanceof ArmorStand stand) || !isSummonPoint(stand)) {
                continue;
            }
            double d = entity.getLocation().distanceSquared(loc);
            if (d < nearestSq) {
                nearestSq = d;
                nearest = stand;
            }
        }
        if (nearest == null) {
            player.sendMessage(ChatColor.RED + "【刷怪点】4 格内没有刷怪点");
            return;
        }
        nearest.remove();
        player.sendMessage(ChatColor.GREEN + "【刷怪点】已删除最近的刷怪点");
    }

    public void spawnPointList(Player player) {
        int count = 0;
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (!isSummonPoint(entity)) {
                    continue;
                }
                count++;
                Location base = entity.getLocation();
                world.spawnParticle(Particle.LAVA, base.clone().add(0, 0.3, 0), 15, 0.25, 0.15, 0.25, 0);
                world.spawnParticle(Particle.END_ROD, base.clone().add(0, 0.8, 0), 6, 0, 0, 0, 0);
                world.spawnParticle(Particle.END_ROD, base.clone().add(0, 1.4, 0), 6, 0, 0, 0, 0);
                world.spawnParticle(Particle.END_ROD, base.clone().add(0, 2.0, 0), 6, 0, 0, 0, 0);
                world.spawnParticle(Particle.END_ROD, base.clone().add(0, 2.6, 0), 6, 0, 0, 0, 0);
            }
        }
        player.sendMessage(ChatColor.GREEN + "【刷怪点】当前共有 " + count + " 个刷怪点（发光粒子已标记位置）");
    }

    public void spawnPointBossCreate(Player player) {
        Location loc = player.getLocation();
        loc.getWorld().spawn(loc, ArmorStand.class, stand -> {
            stand.setInvisible(true);
            stand.setGravity(false);
            stand.setInvulnerable(true);
            stand.setSmall(true);
            stand.setCustomName(ChatColor.RED + "boss点");
            stand.setCustomNameVisible(false);
            stand.addScoreboardTag("rz");
            stand.addScoreboardTag("boss_point");
        });
        player.sendMessage(ChatColor.GREEN + "【boss点】已创建 boss 刷怪点（隐形标记）");
    }

    public void spawnPointBossDelete(Player player) {
        Location loc = player.getLocation();
        ArmorStand nearest = null;
        double nearestSq = SPAWN_POINT_DELETE_RADIUS * SPAWN_POINT_DELETE_RADIUS;
        for (Entity entity : loc.getWorld().getNearbyEntities(loc, SPAWN_POINT_DELETE_RADIUS, SPAWN_POINT_DELETE_RADIUS, SPAWN_POINT_DELETE_RADIUS)) {
            if (!(entity instanceof ArmorStand stand)
                    || !entity.getScoreboardTags().contains("rz")
                    || !entity.getScoreboardTags().contains("boss_point")) {
                continue;
            }
            double d = entity.getLocation().distanceSquared(loc);
            if (d < nearestSq) {
                nearestSq = d;
                nearest = stand;
            }
        }
        if (nearest == null) {
            player.sendMessage(ChatColor.RED + "【boss点】4 格内没有 boss 刷怪点");
            return;
        }
        nearest.remove();
        player.sendMessage(ChatColor.GREEN + "【boss点】已删除最近的 boss 刷怪点");
    }

    public void spawnPointBossList(Player player) {
        int count = 0;
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (!entity.getScoreboardTags().contains("rz")
                        || !entity.getScoreboardTags().contains("boss_point")) {
                    continue;
                }
                count++;
                Location base = entity.getLocation();
                world.spawnParticle(Particle.SOUL_FIRE_FLAME, base.clone().add(0, 0.3, 0), 10, 0.25, 0.15, 0.25, 0);
                world.spawnParticle(Particle.END_ROD, base.clone().add(0, 0.8, 0), 6, 0, 0, 0, 0);
                world.spawnParticle(Particle.END_ROD, base.clone().add(0, 1.4, 0), 6, 0, 0, 0, 0);
            }
        }
        player.sendMessage(ChatColor.GREEN + "【boss点】当前共有 " + count + " 个 boss 刷怪点（发光粒子已标记位置）");
    }

    public void spawnPointStart(Player player) {
        spawningEnabled = true;
        spawnSeconds = 0;
        spawnCounterTicks = 0;
        setGameScore(1);
        player.sendMessage(ChatColor.GREEN + "【刷怪点】开始刷怪：每 25 秒各刷 1 只，频率逐渐加快（最低 3 秒）");
    }

    public void spawnPointStop(Player player) {
        spawningEnabled = false;
        setGameScore(0);
        player.sendMessage(ChatColor.GREEN + "【刷怪点】已停止刷怪");
    }

    public void handleTriggerCommand(Player player, String command) {
        if (command.startsWith("trigger rz.dave.create")) {
            markPending(player);
            Scoreboard main = Bukkit.getScoreboardManager().getMainScoreboard();
            Objective objective = main.getObjective("rz.dave.create");
            if (objective != null) {
                objective.getScore(player.getName()).setScore(1);
            }
            return;
        }
        if (command.startsWith("trigger rz.sp.boss.create")) {
            spawnPointBossCreate(player);
        } else if (command.startsWith("trigger rz.sp.boss.delete")) {
            spawnPointBossDelete(player);
        } else if (command.startsWith("trigger rz.sp.boss.list")) {
            spawnPointBossList(player);
        } else if (command.startsWith("trigger rz.sp.create")) {
            spawnPointCreate(player);
        } else if (command.startsWith("trigger rz.sp.delete")) {
            spawnPointDelete(player);
        } else if (command.startsWith("trigger rz.sp.list")) {
            spawnPointList(player);
        } else if (command.startsWith("trigger rz.sp.start")) {
            spawnPointStart(player);
        } else if (command.startsWith("trigger rz.sp.stop")) {
            spawnPointStop(player);
        }
    }

    boolean isSummonPoint(Entity entity) {
        return entity.getScoreboardTags().contains("rz")
                && entity.getScoreboardTags().contains("summon_point");
    }

    private void tickSpawner() {
        if (!spawningEnabled || restActive) {
            return;
        }
        refreshSpawnPointActives();
        spawnSeconds++;
        int interval = Math.max(SPAWN_INTERVAL_MIN_TICKS,
                spawnIntervalStartTicks - (spawnSeconds / SPAWN_ACCELERATE_SECONDS) * SPAWN_INTERVAL_STEP_TICKS);
        spawnCounterTicks += 20;
        if (spawnCounterTicks >= interval) {
            spawnCounterTicks -= interval;
            spawnWave();
        }
    }

    private void spawnWave() {
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (!isSummonPoint(entity)
                        || entity.getScoreboardTags().contains("disabled")
                        || entity.getScoreboardTags().contains("boss_point")) {
                    continue;
                }
                spawnBlindBoxZombie(entity.getLocation());
            }
        }
    }

    private void spawnBlindBoxZombie(Location loc) {
        World world = loc.getWorld();
        if (world == null) {
            return;
        }
        Zombie zombie = world.spawn(loc, Zombie.class, z -> {
            z.setBaby(false);
            z.setCanPickupItems(false);
            z.getEquipment().setHelmet(new ItemStack(Material.HAY_BLOCK));
            z.getEquipment().setChestplate(new ItemStack(Material.AIR));
            z.getEquipment().setLeggings(new ItemStack(Material.AIR));
            z.getEquipment().setBoots(new ItemStack(Material.AIR));
            z.getEquipment().setItemInMainHand(new ItemStack(Material.AIR));
            z.getEquipment().setItemInOffHand(new ItemStack(Material.AIR));
            z.getEquipment().setHelmetDropChance(0f);
            z.getEquipment().setChestplateDropChance(0f);
            z.getEquipment().setLeggingsDropChance(0f);
            z.getEquipment().setBootsDropChance(0f);
            z.getEquipment().setItemInMainHandDropChance(0f);
            z.getEquipment().setItemInOffHandDropChance(0f);
            z.setMaxHealth(20);
            z.setHealth(20);
            z.setCustomName("盲盒僵尸");
            z.setCustomNameVisible(true);
            z.addScoreboardTag("rz");
            z.addScoreboardTag("monster");
            z.addScoreboardTag("random_zombie");
            z.setRemoveWhenFarAway(false);
            z.setPersistent(true);
            AttributeInstance atk = z.getAttribute(Attribute.ATTACK_DAMAGE);
            if (atk != null) {
                atk.setBaseValue(atk.getBaseValue() * 0.5);
            }
        });
        if (zombie != null) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (zombie.isValid()) {
                    zombie.getPersistentDataContainer().remove(waveBaseHpKey);
                    zombie.setMaxHealth(20);
                    zombie.setHealth(20);
                    zombie.getEquipment().setHelmet(new ItemStack(Material.HAY_BLOCK));
                    zombie.getEquipment().setChestplate(null);
                    zombie.getEquipment().setLeggings(null);
                    zombie.getEquipment().setBoots(null);
                    zombie.getEquipment().setItemInMainHand(null);
                    zombie.getEquipment().setItemInOffHand(null);
                    zombie.getEquipment().setHelmetDropChance(0f);
                    zombie.getEquipment().setChestplateDropChance(0f);
                    zombie.getEquipment().setLeggingsDropChance(0f);
                    zombie.getEquipment().setBootsDropChance(0f);
                    zombie.getEquipment().setItemInMainHandDropChance(0f);
                    zombie.getEquipment().setItemInOffHandDropChance(0f);
                }
            }, 1L);
        }
    }

    public void handleBlindBoxDeath(Mob mob) {
        if (mob.getPersistentDataContainer().has(blindBoxOpenedKey, PersistentDataType.BYTE)) {
            plugin.getLogger().warning("盲盒重复触发，已忽略: " + mob.getUniqueId());
            return;
        }
        mob.getPersistentDataContainer().set(blindBoxOpenedKey, PersistentDataType.BYTE, (byte) 1);
        plugin.getLogger().info("盲盒开启: " + mob.getUniqueId() + " @ " + mob.getLocation());
        spawnRandomMonster(mob.getLocation());
    }

    public void handleSlimeDeath(Mob mob) {
        String type = null;
        for (String tag : mob.getScoreboardTags()) {
            if (tag.equals("large_slime") || tag.equals("medium_slime") || tag.equals("small_slime")) {
                type = tag;
                break;
            }
        }
        if (type == null) {
            return;
        }
        Player killer = mob.getKiller();
        addPlayerKill(killer);
        ThreadLocalRandom random = ThreadLocalRandom.current();
        if (type.equals("small_slime")) {
            grantCurrencyWithAssists(mob, killer, ShopCurrency.SILVER, random.nextInt(5, 10));
            return;
        }
        if (!deathMode && killer != null && random.nextDouble() < 0.2) {
            addCurrency(killer, ShopCurrency.SILVER, 1);
        }
        boolean large = type.equals("large_slime");
        String childTag = large ? "medium_slime" : "small_slime";
        int childSize = large ? 2 : 1;
        double childHealth = large ? 4 : 1;
        String childName = large ? "中型破碎者跳跳" : "小型破碎者跳跳";
        for (int i = 0; i < 2; i++) {
            spawnSlime(mob.getLocation(), childTag, childSize, childHealth, childName);
        }
    }

    private void spawnSlime(Location loc, String tag, int size, double health, String name) {
        World world = loc.getWorld();
        if (world == null) {
            return;
        }
        Location spawnLoc = loc.clone().add(
                ThreadLocalRandom.current().nextDouble(-0.5, 0.5), 0,
                ThreadLocalRandom.current().nextDouble(-0.5, 0.5));
        world.spawn(spawnLoc, Slime.class, s -> {
            s.setSize(size);
            s.addScoreboardTag("rz");
            s.addScoreboardTag("monster");
            s.addScoreboardTag(tag);
            s.setMaxHealth(health);
            s.setHealth(health);
            s.setCustomName(name);
            s.setCustomNameVisible(true);
            s.setRemoveWhenFarAway(false);
            s.setPersistent(true);
        });
    }

    private void spawnRandomMonster(Location loc) {
        if (ThreadLocalRandom.current().nextDouble() < GIANT_CHANCE) {
            plugin.getLogger().info("盲盒随机结果: giant 迁移=false（独立概率） @ " + loc);
            dropSummonTicket(GIANT_ENTRY, loc);
            return;
        }
        if (ThreadLocalRandom.current().nextDouble() < BLACK_FOOTBALL_CHANCE) {
            plugin.getLogger().info("盲盒随机结果: black_football_skeleton 迁移=false（独立概率） @ " + loc);
            dropSummonTicket(BLACK_FOOTBALL_ENTRY, loc);
            return;
        }
        int total = 0;
        for (MonsterEntry entry : MONSTER_POOL) {
            total += entry.weight();
        }
        int roll = ThreadLocalRandom.current().nextInt(total);
        MonsterEntry chosen = null;
        for (MonsterEntry entry : MONSTER_POOL) {
            roll -= entry.weight();
            if (roll < 0) {
                chosen = entry;
                break;
            }
        }
        if (chosen == null) {
            return;
        }
        plugin.getLogger().info("盲盒随机结果: " + chosen.key()
                + " 迁移=" + chosen.migrated() + " @ " + loc);
        if (chosen.migrated()) {
            spawnMigrated(chosen, loc);
        } else {
            dropSummonTicket(chosen, loc);
        }
    }

    private Mob spawnMigrated(MonsterEntry entry, Location loc) {
        String mmName = MYTHIC_MOB_NAMES.get(entry.key());
        if (mmName == null) {
            Mob mob = spawnVanillaMonster(entry, loc);
            if (mob != null) {
                tagMonster(mob, entry.key());
            }
            return mob;
        }
        MythicSpawnResult result = spawnMythicMob(mmName, loc);
        if (result.entity() != null) {
            tagMonster(result.entity(), entry.key());
            applyMigratedExtras(result.entity(), entry);
            plugin.getLogger().info("MythicMobs 生成成功: " + entry.key() + " @ " + loc);
            return result.entity();
        }
        if (result.mayHaveSpawned()) {
            plugin.getLogger().warning("MythicMobs 可能已生成但取实体失败，安排补标签: " + entry.key());
            final MonsterEntry entryFinal = entry;
            final Location spawnLoc = loc;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                Mob found = findUntaggedMigrated(spawnLoc, entryFinal.key());
                if (found != null) {
                    tagMonster(found, entryFinal.key());
                    applyMigratedExtras(found, entryFinal);
                    plugin.getLogger().info("迁移怪补标签成功: " + entryFinal.key() + " @ " + spawnLoc);
                } else {
                    plugin.getLogger().warning("迁移怪补标签未找到: " + entryFinal.key() + " @ " + spawnLoc);
                }
            }, 5L);
            return null;
        }
        plugin.getLogger().warning("MythicMobs 召唤失败，回退原版生成: " + entry.key());
        Mob mob = spawnVanillaMonster(entry, loc);
        if (mob != null) {
            tagMonster(mob, entry.key());
            plugin.getLogger().info("原版回退生成成功: " + entry.key() + " @ " + loc);
        }
        return mob;
    }

    private Mob findUntaggedMigrated(Location loc, String key) {
        Class<? extends Mob> clazz = migratedClass(key);
        for (Entity entity : loc.getWorld().getNearbyEntities(loc, 20, 20, 20)) {
            if (!clazz.isInstance(entity) || !(entity instanceof Mob mob)) {
                continue;
            }
            if (mob.getScoreboardTags().contains("rz") || mob.getScoreboardTags().contains("monster")) {
                continue;
            }
            return mob;
        }
        return null;
    }

    private static Class<? extends Mob> migratedClass(String key) {
        return switch (key) {
            case "normal_zombie", "iron_armor_zombie", "small_zombie" -> Zombie.class;
            case "skeleton" -> Skeleton.class;
            case "stray" -> Stray.class;
            case "creeper" -> Creeper.class;
            case "blaze" -> Blaze.class;
            case "large_slime" -> Slime.class;
            case "witch" -> Witch.class;
            default -> Mob.class;
        };
    }

    private record MythicSpawnResult(Mob entity, boolean mayHaveSpawned) {
    }

    private MythicSpawnResult spawnMythicMob(String mobName, Location loc) {
        try {
            Class<?> clazz = Class.forName("io.lumine.mythic.bukkit.MythicBukkit");
            Object inst = clazz.getMethod("inst").invoke(null);
            if (inst == null) {
                return new MythicSpawnResult(null, false);
            }
            Object helper = inst.getClass().getMethod("getAPIHelper").invoke(inst);
            if (helper == null) {
                return new MythicSpawnResult(null, false);
            }
            Method spawn = null;
            for (Method method : helper.getClass().getMethods()) {
                if (method.getName().equals("spawnMythicMob") && method.getParameterCount() == 2
                        && method.getParameterTypes()[0] == String.class) {
                    spawn = method;
                    break;
                }
            }
            if (spawn == null) {
                return new MythicSpawnResult(null, false);
            }
            Object active = spawn.invoke(helper, mobName, loc);
            if (active == null) {
                return new MythicSpawnResult(null, false);
            }
            Object entity = null;
            for (Method method : active.getClass().getMethods()) {
                if (method.getName().equals("getEntity") && method.getParameterCount() == 0) {
                    entity = method.invoke(active);
                    break;
                }
            }
            if (entity instanceof Mob mob) {
                return new MythicSpawnResult(mob, true);
            }
            if (entity != null) {
                Object bukkitEntity = toBukkitEntity(entity);
                if (bukkitEntity instanceof Mob mob) {
                    return new MythicSpawnResult(mob, true);
                }
            }
            return new MythicSpawnResult(null, true);
        } catch (Throwable t) {
            plugin.getLogger().warning("MythicMobs 反射调用失败: " + t.getMessage());
            return new MythicSpawnResult(null, false);
        }
    }

    private Object toBukkitEntity(Object abstractEntity) {
        try {
            Method getBukkit = abstractEntity.getClass().getMethod("getBukkitEntity");
            Object bukkit = getBukkit.invoke(abstractEntity);
            if (bukkit != null) {
                return bukkit;
            }
        } catch (Throwable ignored) {
        }
        try {
            Class<?> adapter = Class.forName("io.lumine.mythic.bukkit.BukkitAdapter");
            for (Method method : adapter.getMethods()) {
                if (method.getName().equals("adapt") && method.getParameterCount() == 1
                        && method.getParameterTypes()[0].isAssignableFrom(abstractEntity.getClass())) {
                    return method.invoke(null, abstractEntity);
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private void tagMonster(Mob mob, String key) {
        mob.addScoreboardTag("rz");
        mob.addScoreboardTag("monster");
        mob.addScoreboardTag(key);
        mob.setRemoveWhenFarAway(false);
        mob.setPersistent(true);
    }

    private void applyMigratedExtras(Mob mob, MonsterEntry entry) {
        applyMigratedEquipment(mob, entry.key());
        switch (entry.key()) {
            case "small_zombie" -> {
                if (mob instanceof Zombie zombie) {
                    zombie.setBaby(true);
                }
            }
            case "iron_armor_zombie", "blaze" -> armorUnbreakable(mob.getEquipment());
            case "large_slime" -> {
                armorUnbreakable(mob.getEquipment());
                if (mob instanceof Slime slime) {
                    slime.setSize(3);
                    slime.setMaxHealth(25);
                    slime.setHealth(25);
                }
            }
            default -> { }
        }
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (mob.isValid()) {
                applyMigratedEquipment(mob, entry.key());
                if (entry.key().equals("small_zombie") && mob instanceof Zombie zombie) {
                    zombie.setBaby(true);
                }
            }
        }, 1L);
    }

    private static void applyMigratedEquipment(Mob mob, String key) {
        EntityEquipment equipment = mob.getEquipment();
        if (equipment == null) {
            return;
        }
        switch (key) {
            case "normal_zombie", "small_zombie", "skeleton", "stray" -> {
                equipment.setHelmet(new ItemStack(Material.STONE_BUTTON));
                equipment.setHelmetDropChance(0f);
                if (key.equals("skeleton") || key.equals("stray")) {
                    equipment.setItemInMainHand(new ItemStack(Material.BOW));
                    equipment.setItemInMainHandDropChance(0f);
                }
            }
            default -> { }
        }
    }

    private static void setAttribute(Mob mob, Attribute attribute, double value) {
        AttributeInstance instance = mob.getAttribute(attribute);
        if (instance != null) {
            instance.setBaseValue(value);
        }
    }

    private static void armorUnbreakable(EntityEquipment equipment) {
        for (EquipmentSlot slot : new EquipmentSlot[]{
                EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack stack = equipment.getItem(slot);
            if (stack == null || stack.getType() == Material.AIR) {
                stack = ironPiece(slot);
                equipment.setItem(slot, stack);
            }
            ItemMeta meta = stack.getItemMeta();
            if (meta != null) {
                meta.setUnbreakable(true);
                stack.setItemMeta(meta);
                equipment.setItem(slot, stack);
            }
            equipment.setDropChance(slot, 0f);
        }
    }

    private static ItemStack ironPiece(EquipmentSlot slot) {
        return switch (slot) {
            case HEAD -> new ItemStack(Material.IRON_HELMET);
            case CHEST -> new ItemStack(Material.IRON_CHESTPLATE);
            case LEGS -> new ItemStack(Material.IRON_LEGGINGS);
            case FEET -> new ItemStack(Material.IRON_BOOTS);
            default -> new ItemStack(Material.AIR);
        };
    }

    private Mob spawnVanillaMonster(MonsterEntry entry, Location loc) {
        World world = loc.getWorld();
        if (world == null) {
            return null;
        }
        try {
            switch (entry.key()) {
                case "normal_zombie":
                    return world.spawn(loc, Zombie.class, z -> {
                        z.setBaby(false);
                        z.setCanPickupItems(false);
                        z.getEquipment().setHelmet(new ItemStack(Material.STONE_BUTTON));
                        z.getEquipment().setHelmetDropChance(0f);
                    });
                case "iron_armor_zombie":
                    return world.spawn(loc, Zombie.class, z -> {
                        z.setBaby(false);
                        z.setCanPickupItems(false);
                        armorUnbreakable(z.getEquipment());
                    });
                case "small_zombie":
                    return world.spawn(loc, Zombie.class, z -> {
                        z.setBaby(true);
                        z.setCanPickupItems(false);
                        z.getEquipment().setHelmet(new ItemStack(Material.STONE_BUTTON));
                        z.getEquipment().setHelmetDropChance(0f);
                        z.setMaxHealth(15);
                        z.setHealth(15);
                    });
                case "skeleton":
                    return world.spawn(loc, Skeleton.class, s -> {
                        s.setCanPickupItems(false);
                        s.getEquipment().setHelmet(new ItemStack(Material.STONE_BUTTON));
                        s.getEquipment().setHelmetDropChance(0f);
                        s.getEquipment().setItemInMainHand(new ItemStack(Material.BOW));
                        s.getEquipment().setItemInMainHandDropChance(0f);
                    });
                case "stray":
                    return world.spawn(loc, Stray.class, s -> {
                        s.setCanPickupItems(false);
                        s.getEquipment().setHelmet(new ItemStack(Material.STONE_BUTTON));
                        s.getEquipment().setHelmetDropChance(0f);
                        s.getEquipment().setItemInMainHand(new ItemStack(Material.BOW));
                        s.getEquipment().setItemInMainHandDropChance(0f);
                    });
                case "creeper":
                    return world.spawn(loc, Creeper.class, c -> c.setCanPickupItems(false));
                case "blaze":
                    return world.spawn(loc, Blaze.class, b -> armorUnbreakable(b.getEquipment()));
                case "large_slime":
                    return world.spawn(loc, Slime.class, s -> {
                        s.setSize(1);
                        armorUnbreakable(s.getEquipment());
                        setAttribute(s, Attribute.SCALE, 5.0);
                        setAttribute(s, Attribute.JUMP_STRENGTH, 0.72);
                        setAttribute(s, Attribute.SAFE_FALL_DISTANCE, 6.0);
                        s.setMaxHealth(25);
                        s.setHealth(25);
                    });
                case "witch":
                    return world.spawn(loc, Witch.class, w -> { });
                default:
                    return null;
            }
        } catch (Exception e) {
            plugin.getLogger().warning("原版回退生成失败: " + entry.key() + " " + e.getMessage());
            return null;
        }
    }

    private void dropSummonTicket(MonsterEntry entry, Location loc) {
        dropLootTickets(loc, entry.key(), entry.displayName(), 1);
    }

    private void dropLootTickets(Location loc, String tableName, String displayName, int count) {
        World world = loc.getWorld();
        if (world == null) {
            return;
        }
        double x = loc.getX();
        double y = loc.getY();
        double z = loc.getZ();
        for (int i = 0; i < count; i++) {
            String loot = String.format(Locale.ROOT,
                    "execute positioned %.2f %.2f %.2f run loot spawn ~ ~ ~ loot rz:monsters/%s", x, y, z, tableName);
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), loot);
            makeTicketInvulnerable(world, x, y, z, displayName);
        }
    }

    private void makeTicketInvulnerable(World world, double x, double y, double z, String displayName) {
        Location center = new Location(world, x, y, z);
        Item best = null;
        double bestSq = Double.MAX_VALUE;
        for (Entity entity : world.getNearbyEntities(center, 3, 3, 3)) {
            if (!(entity instanceof Item item)
                    || item.getItemStack().getType() != Material.WHEAT) {
                continue;
            }
            ItemMeta meta = item.getItemStack().getItemMeta();
            String name = meta == null ? null : meta.getItemName();
            if (!displayName.equals(name)) {
                continue;
            }
            double distSq = item.getLocation().distanceSquared(center);
            if (distSq < bestSq) {
                bestSq = distSq;
                best = item;
            }
        }
        if (best != null) {
            best.setInvulnerable(true);
        } else {
            plugin.getLogger().warning("召唤券生成后未找到物品: " + displayName + " @ " + x + "," + y + "," + z);
        }
    }

    public void grantMonsterDrops(Mob mob, Player killer) {
        if (!isMonster(mob)) {
            return;
        }
        ShopCurrency currency = monsterCoinCurrency(mob);
        int amount = monsterCoinAmount(mob);
        grantCurrencyWithAssists(mob, killer != null ? killer : resolveMonsterKiller(mob), currency, amount);
        if (mob.getScoreboardTags().contains("blaze") && ThreadLocalRandom.current().nextDouble() < 0.1) {
            Player resolved = killer != null ? killer : resolveMonsterKiller(mob);
            if (resolved != null) {
                resolved.getWorld().dropItemNaturally(mob.getLocation(), new ItemStack(Material.BLAZE_ROD));
            }
        }
    }

    public void grantMiniBossStar(Mob mob, Player killer) {
        Player resolvedKiller = killer != null ? killer : resolveMonsterKiller(mob);
        damageLogs.remove(mob.getUniqueId());
        if (deathMode && resolvedKiller != null && !gameStartParticipants.contains(resolvedKiller.getUniqueId())) {
            return;
        }
        if (resolvedKiller != null) {
            addCurrency(resolvedKiller, ShopCurrency.STAR, 1);
            return;
        }
        ItemStack star = new ItemStack(Material.NETHER_STAR, 1);
        ItemMeta meta = star.getItemMeta();
        if (meta != null) {
            meta.setItemName(ShopCurrency.STAR.displayName());
            star.setItemMeta(meta);
        }
        mob.getWorld().dropItemNaturally(mob.getLocation(), star);
    }

    public void grantDancingZombieDrops(Mob mob, Player killer) {
        int amount = ThreadLocalRandom.current().nextInt(1, 3);
        grantCurrencyWithAssists(mob, killer, ShopCurrency.GOLD, amount);
    }

    public int dancingZombieCoinAmount() {
        return ThreadLocalRandom.current().nextInt(1, 3);
    }

    public int monsterCoinAmount(Mob mob) {
        String key = null;
        for (String tag : mob.getScoreboardTags()) {
            if (MYTHIC_MOB_NAMES.containsKey(tag)) {
                key = tag;
                break;
            }
        }
        if (key == null) {
            return 0;
        }
        ThreadLocalRandom random = ThreadLocalRandom.current();
        return switch (key) {
            case "normal_zombie", "small_zombie", "skeleton", "stray", "creeper", "witch" ->
                    random.nextInt(5, 10);
            case "iron_armor_zombie" -> random.nextInt(1, 3);
            case "blaze" -> random.nextInt(2, 5);
            default -> 0;
        };
    }

    public ShopCurrency monsterCoinCurrency(Mob mob) {
        String key = null;
        for (String tag : mob.getScoreboardTags()) {
            if (MYTHIC_MOB_NAMES.containsKey(tag)) {
                key = tag;
                break;
            }
        }
        if (key == null) {
            return null;
        }
        return switch (key) {
            case "iron_armor_zombie", "blaze" -> ShopCurrency.GOLD;
            default -> ShopCurrency.SILVER;
        };
    }

    public void grantCurrencyWithAssists(Mob mob, Player killer, ShopCurrency currency, int amount) {
        if (currency == null || amount <= 0) {
            return;
        }
        if (deathMode && currency != ShopCurrency.STAR) {
            return;
        }
        Player resolvedKiller = killer != null ? killer : resolveMonsterKiller(mob);
        if (deathMode && resolvedKiller != null && !gameStartParticipants.contains(resolvedKiller.getUniqueId())) {
            damageLogs.remove(mob.getUniqueId());
            return;
        }
        if (resolvedKiller != null) {
            addCurrency(resolvedKiller, currency, amount);
        }
        Map<UUID, Double> damage = damageLogs.remove(mob.getUniqueId());
        if (damage == null) {
            return;
        }
        int assistAmount = Math.max(1, amount / 2);
        for (Map.Entry<UUID, Double> entry : damage.entrySet()) {
            if (entry.getValue() <= 10.0) {
                continue;
            }
            if (resolvedKiller != null && entry.getKey().equals(resolvedKiller.getUniqueId())) {
                continue;
            }
            Player assister = Bukkit.getPlayer(entry.getKey());
            if (assister != null && assister.isOnline()
                    && (!deathMode || gameStartParticipants.contains(assister.getUniqueId()))) {
                addCurrency(assister, currency, assistAmount);
            }
        }
    }

    private void setGameScore(int value) {
        Scoreboard main = Bukkit.getScoreboardManager().getMainScoreboard();
        Objective objective = main.getObjective("rz");
        if (objective != null) {
            objective.getScore("game").setScore(value);
        }
    }

    private void giveStarterGear(Player player, String teamId) {
        Color color = teamDyeColor(teamId);
        player.getInventory().setHelmet(dyedArmor(Material.LEATHER_HELMET, color));
        player.getInventory().setChestplate(dyedArmor(Material.LEATHER_CHESTPLATE, color));
        player.getInventory().setLeggings(dyedArmor(Material.LEATHER_LEGGINGS, color));
        player.getInventory().setBoots(dyedArmor(Material.LEATHER_BOOTS, color));
        ItemStack sword = new ItemStack(Material.STONE_SWORD);
        ItemMeta swordMeta = sword.getItemMeta();
        swordMeta.setUnbreakable(true);
        sword.setItemMeta(swordMeta);
        player.getInventory().addItem(sword);
        player.getInventory().addItem(new ItemStack(Material.BREAD, 16));
    }

    private static ItemStack dyedArmor(Material material, Color color) {
        ItemStack stack = new ItemStack(material);
        LeatherArmorMeta meta = (LeatherArmorMeta) stack.getItemMeta();
        meta.setColor(color);
        meta.setUnbreakable(true);
        stack.setItemMeta(meta);
        return stack;
    }

    private static Color teamDyeColor(String teamId) {
        return switch (teamId) {
            case "red" -> Color.RED;
            case "blue" -> Color.BLUE;
            case "yellow" -> Color.YELLOW;
            case "green" -> Color.fromRGB(0x006600);
            default -> Color.WHITE;
        };
    }

    private void loadConfig() {
        plugin.getDataFolder().mkdirs();
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            World world = Bukkit.getWorld("world");
            int sx = 0;
            int sy = 80;
            int sz = 0;
            if (world != null) {
                sx = world.getSpawnLocation().getBlockX();
                sy = world.getSpawnLocation().getBlockY();
                sz = world.getSpawnLocation().getBlockZ();
            }
            YamlConfiguration defaults = new YamlConfiguration();
            defaults.options().header("戴夫 PvE 配置：队伍固定为红/蓝/黄/绿四队，填写每队戴夫生成坐标。\nworld 为世界名，x/y/z 为方块坐标。");
            defaults.set("team-chest-size", 27);
            defaults.set("dave-death-cleanup-radius", 37);
            defaults.set("spawn-point-team-radius", 48);
            defaults.set("brewing-room.world", "world");
            defaults.set("brewing-room.x", 10000);
            defaults.set("brewing-room.y", 1);
            defaults.set("brewing-room.z", 10000);
            defaults.set("lobby.world", "world");
            defaults.set("lobby.x", sx);
            defaults.set("lobby.y", sy);
            defaults.set("lobby.z", sz);
            for (String id : TEAM_IDS) {
                String path = "teams." + id + ".";
                defaults.set(path + "display", TEAM_DISPLAYS.get(id));
                defaults.set(path + "world", "world");
                defaults.set(path + "x", sx);
                defaults.set(path + "y", sy);
                defaults.set(path + "z", sz);
                defaults.set(path + "play.x", sx);
                defaults.set(path + "play.y", sy);
                defaults.set(path + "play.z", sz);
            }
            try {
                defaults.save(configFile);
            } catch (IOException e) {
                plugin.getLogger().warning("写入默认配置失败: " + e.getMessage());
            }
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        teamChestSize = config.getInt("team-chest-size", 27);
        cleanupRadius = config.getInt("dave-death-cleanup-radius", 37);
        spawnPointTeamRadius = config.getInt("spawn-point-team-radius", 48);
        brewingWorld = config.getString("brewing-room.world", "world");
        brewingX = config.getInt("brewing-room.x", 10000);
        brewingY = config.getInt("brewing-room.y", 1);
        brewingZ = config.getInt("brewing-room.z", 10000);
        World defaultWorld = Bukkit.getWorld("world");
        int sx = 0;
        int sy = 80;
        int sz = 0;
        if (defaultWorld != null) {
            sx = defaultWorld.getSpawnLocation().getBlockX();
            sy = defaultWorld.getSpawnLocation().getBlockY();
            sz = defaultWorld.getSpawnLocation().getBlockZ();
        }
        lobbyWorld = config.getString("lobby.world", "world");
        lobbyX = config.getInt("lobby.x", sx);
        lobbyY = config.getInt("lobby.y", sy);
        lobbyZ = config.getInt("lobby.z", sz);
        for (String id : TEAM_IDS) {
            String path = "teams." + id + ".";
            String display = config.getString(path + "display", TEAM_DISPLAYS.get(id));
            String worldName = config.getString(path + "world", "world");
            int x = config.getInt(path + "x", 0);
            int y = config.getInt(path + "y", 80);
            int z = config.getInt(path + "z", 0);
            int playX = config.getInt(path + "play.x", x);
            int playY = config.getInt(path + "play.y", y);
            int playZ = config.getInt(path + "play.z", z);
            teamDefs.put(id, new TeamDef(id, display, worldName, x, y, z, playX, playY, playZ));
        }
    }

    private static ChatColor teamColor(String id) {
        return switch (id) {
            case "red" -> ChatColor.RED;
            case "blue" -> ChatColor.BLUE;
            case "yellow" -> ChatColor.YELLOW;
            case "green" -> ChatColor.DARK_GREEN;
            default -> ChatColor.WHITE;
        };
    }

    public String displayName(String teamId) {
        return TEAM_DISPLAYS.getOrDefault(teamId, teamId);
    }

    public void refreshPlayerListName(Player player) {
        if (pvzMode != null && pvzMode.isPlaying(player)) {
            pvzMode.refreshListName(player);
            return;
        }
        String colored;
        if (isPlaying(player)) {
            String team = getTeamName(player);
            colored = teamColor(team) + player.getName();
        } else if (player.isOp()) {
            colored = rainbowName(player.getName());
        } else {
            colored = ChatColor.GREEN + player.getName();
        }
        player.setPlayerListName(colored);
        player.setDisplayName(colored);
    }

    public void teleportToLobby(Player player) {
        World world = Bukkit.getWorld(lobbyWorld);
        if (world == null && !Bukkit.getWorlds().isEmpty()) {
            world = Bukkit.getWorlds().get(0);
        }
        if (world == null) {
            return;
        }
        player.teleport(new Location(world, lobbyX + 0.5, lobbyY, lobbyZ + 0.5));
    }

    private void teleportToPlayArea(Player player, TeamDef def) {
        World world = Bukkit.getWorld(def.world());
        if (world == null && !Bukkit.getWorlds().isEmpty()) {
            world = Bukkit.getWorlds().get(0);
        }
        if (world == null) {
            return;
        }
        player.teleport(new Location(world, def.playX() + 0.5, def.playY(), def.playZ() + 0.5));
    }

    private static String rainbowName(String name) {
        ChatColor[] colors = {
                ChatColor.RED, ChatColor.GOLD, ChatColor.YELLOW,
                ChatColor.GREEN, ChatColor.AQUA, ChatColor.LIGHT_PURPLE
        };
        StringBuilder result = new StringBuilder();
        int index = 0;
        for (char c : name.toCharArray()) {
            result.append(colors[index % colors.length]).append(c);
            index++;
        }
        return result.toString();
    }

    private UUID findPendingCreator(Villager dave) {
        long now = System.currentTimeMillis();
        UUID nearest = null;
        double nearestSq = PENDING_NEAR_DISTANCE * PENDING_NEAR_DISTANCE;
        UUID latest = null;
        long latestTime = Long.MIN_VALUE;
        for (Map.Entry<UUID, Long> entry : pendingCreates.entrySet()) {
            if (now - entry.getValue() > PENDING_TTL_MS) {
                continue;
            }
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player != null && player.isOnline()) {
                double distanceSq = player.getLocation().distanceSquared(dave.getLocation());
                if (distanceSq < nearestSq) {
                    nearestSq = distanceSq;
                    nearest = entry.getKey();
                }
            }
            if (entry.getValue() > latestTime) {
                latestTime = entry.getValue();
                latest = entry.getKey();
            }
        }
        return nearest != null ? nearest : latest;
    }

    private String getOwnerTeam(Entity dave) {
        return dave.getPersistentDataContainer().get(ownerTeamKey, PersistentDataType.STRING);
    }

    public String getTeamName(Player player) {
        Team team = player.getScoreboard().getEntryTeam(player.getName());
        if (team == null) {
            Scoreboard main = Bukkit.getScoreboardManager().getMainScoreboard();
            team = main.getEntryTeam(player.getName());
        }
        return team == null ? null : team.getName();
    }

    private boolean hasLivingDave(String team) {
        for (World world : Bukkit.getWorlds()) {
            for (Villager villager : allDaves(world)) {
                if (!villager.isDead() && team.equals(getOwnerTeam(villager))) {
                    return true;
                }
            }
        }
        return false;
    }

    private Set<String> survivingTeams() {
        Set<String> result = new HashSet<>();
        for (World world : Bukkit.getWorlds()) {
            for (Villager villager : allDaves(world)) {
                if (villager.isDead()) {
                    continue;
                }
                String team = getOwnerTeam(villager);
                if (team != null) {
                    result.add(team);
                }
            }
        }
        return result;
    }

    private Villager nearestDave(Entity from) {
        Villager best = null;
        double bestSq = Double.MAX_VALUE;
        for (Villager villager : allDaves(from.getWorld())) {
            if (villager.isDead()) {
                continue;
            }
            double distanceSq = from.getLocation().distanceSquared(villager.getLocation());
            if (distanceSq < bestSq) {
                bestSq = distanceSq;
                best = villager;
            }
        }
        return best;
    }

    private void createBar(Villager dave, String team) {
        BossBar bar = Bukkit.createBossBar("戴夫", BarColor.RED, BarStyle.SOLID);
        bars.put(dave.getUniqueId(), bar);
        double health = Math.max(0.0, dave.getHealth());
        bar.setProgress(Math.min(1.0, health / MAX_HEALTH));
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (team.equals(getTeamName(player))) {
                bar.addPlayer(player);
            }
        }
    }

    private void retargetAllMonsters() {
        for (World world : Bukkit.getWorlds()) {
            for (Mob mob : allMonsters(world)) {
                retarget(mob);
            }
        }
    }

    private List<Villager> allDaves(World world) {
        List<Villager> result = new ArrayList<>();
        for (Villager villager : world.getEntitiesByClass(Villager.class)) {
            if (isDave(villager)) {
                result.add(villager);
            }
        }
        return result;
    }

    private List<Mob> allMonsters(World world) {
        List<Mob> result = new ArrayList<>();
        for (Mob mob : world.getEntitiesByClass(Mob.class)) {
            if (isMonster(mob)) {
                result.add(mob);
            }
        }
        return result;
    }

    private static Player resolvePlayerDamager(Entity damager) {
        if (damager instanceof Player player) {
            return player;
        }
        if (damager instanceof Projectile projectile && projectile.getShooter() instanceof Player player) {
            return player;
        }
        return null;
    }

    public static int countCurrency(Player player, ShopCurrency currency) {
        int total = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (isCurrency(item, currency)) {
                total += item.getAmount();
            }
        }
        return total;
    }

    private static boolean takeCurrency(Player player, ShopCurrency currency, int amount) {
        int remaining = amount;
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (!isCurrency(item, currency)) {
                continue;
            }
            int take = Math.min(item.getAmount(), remaining);
            item.setAmount(item.getAmount() - take);
            if (item.getAmount() <= 0) {
                player.getInventory().setItem(i, null);
            } else {
                player.getInventory().setItem(i, item);
            }
            remaining -= take;
            if (remaining <= 0) {
                return true;
            }
        }
        return false;
    }

    private static boolean isCurrency(ItemStack item, ShopCurrency currency) {
        if (item == null || item.getType() != currency.material()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }
        if (meta.hasItemName() && currency.displayName().equals(meta.getItemName())) {
            return true;
        }
        String display = ChatColor.stripColor(meta.getDisplayName());
        return display != null && display.contains(currency.displayName());
    }
}
