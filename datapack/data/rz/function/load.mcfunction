# 文件用途：数据包加载时自动执行一次，完成世界初始化
# 1) 设置游戏规则（已改为 1.21.11 的 snake_case 名称；raids 含义与旧版 disableRaids 相反，故用 false 保持禁袭击）
gamerule show_advancement_messages false
gamerule command_block_output false
gamerule raids false
gamerule advance_time false
gamerule entity_drops false
gamerule immediate_respawn true
gamerule spawn_phantoms false
gamerule limited_crafting true
gamerule spawn_mobs false
gamerule spawn_patrols false
gamerule spawn_wandering_traders false
gamerule spread_vines false
gamerule spawn_wardens false
gamerule advance_weather false
gamerule keep_inventory true
gamerule mob_griefing false
gamerule players_nether_portal_creative_delay 1200
gamerule players_nether_portal_default_delay 1200
gamerule respawn_radius 0
gamerule spectators_generate_chunks false

tellraw @a ["[\u00a7e斗蛐蛐\u00a7f] \u00a7a数据包已加载"]
tellraw @a ["[\u00a7e斗蛐蛐\u00a7f] \u00a7a祝您游玩快乐!"]

# 2) 创建计分板（记录怪物血量、UID、各怪物动画/计时、玩家受伤等）
scoreboard objectives add rz dummy
scoreboard objectives add rz.monster_uid dummy
scoreboard objectives add rz.arrow dummy

scoreboard objectives add rz.hf_dispenser_zombie dummy
scoreboard objectives add rz.mf_dispenser_zombie dummy
scoreboard objectives add rz.lf_dispenser_zombie dummy
scoreboard objectives add rz.drowned.pos dummy
scoreboard objectives add rz.drowned.player_pos dummy
scoreboard objectives add rz.drowned.motion dummy
scoreboard objectives add rz.giant.health dummy
scoreboard objectives add rz.giant.half_max_health dummy
scoreboard objectives add rz.giant.normal_little_zombie dummy
scoreboard objectives add rz.giant.normal_little_zombie.pos dummy
scoreboard objectives add rz.giant.normal_little_zombie.pos2 dummy
scoreboard objectives add rz.giant.normal_little_zombie.motion dummy
scoreboard objectives add rz.giant.animation dummy
scoreboard objectives add rz.time_creeper dummy
scoreboard objectives add rz.newspaper_zombie dummy
scoreboard objectives add rz.slime dummy
scoreboard objectives add rz.spider dummy
scoreboard objectives add rz.spider_queen dummy
scoreboard objectives add rz.spider_egg dummy
scoreboard objectives add rz.sea_drowned dummy
scoreboard objectives add rz.dancing_zombie.pos dummy
scoreboard objectives add rz.dancing_zombie.pos2 dummy
scoreboard objectives add rz.dancing_zombie.rotation dummy
scoreboard objectives add rz.dancing_zombie.animation dummy
scoreboard objectives add rz.backup_dancer_skeleton.owner dummy
scoreboard objectives add rz.backup_dancer_skeleton.animation dummy
scoreboard objectives add rz.backup_dancer_skeleton.rotation dummy
scoreboard objectives add rz.hurt minecraft.custom:minecraft.damage_taken
scoreboard objectives add rz.temp dummy

# 戴夫 trigger 计分板与游戏状态（刷怪点系统已由 DavePvE 插件接管）
scoreboard objectives add rz.dave.create trigger
scoreboard players set game rz 0
scoreboard players enable @a rz.dave.create
tag @a add player

# 3) 放置用于中转装备耐久的箱子（固定坐标）
setblock -23 43 -19 chest

# 4) 启动周期循环函数
function rz:seconds

function rz:monsters/large_slime/main
function rz:monsters/medium_slime/main
