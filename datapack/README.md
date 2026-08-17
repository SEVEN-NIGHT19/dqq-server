# 斗蛐蛐数据包（Minecraft Java 1.21.11 版）

作者：bailongma_ABAC

本数据包原为 Minecraft Java 1.20.6（pack_format 41）编写，现已升级为 **1.21.11**（pack_format 94.1）兼容版本，功能保持不变。

## 使用方法

把整个 `dqq-mc_datapack-blm` 文件夹（或压缩成 zip 后改名为 `.zip`/`.datapack`）放入存档的 `datapacks` 文件夹，进入世界后数据包会自动加载（见 `pack.mcmeta` 的 `#minecraft:load` 标签）。

## 刷怪点系统（已移交 DavePvE 插件）

刷怪点的创建/删除/列表/开始/停止已由配套的 DavePvE 插件接管（`/trigger rz.sp.*` 由插件直接处理），数据包不再包含刷怪循环与召唤券随机表。部分怪物已迁移至 MythicMobs（`plugins/MythicMobs/Mobs/RZMonsters.yml`），由插件在刷怪点直接生成；未迁移的特殊怪物仍由数据包召唤券链生成。

### 用法（直接在聊天栏输入）

| 命令 | 功能 |
| --- | --- |
| `/trigger rz.sp.create set 1` | 在脚下创建一个刷怪点（隐形盔甲架标记） |
| `/trigger rz.sp.start set 1` | 开始刷怪：所有刷怪点统一频率刷盲盒僵尸，初始 20 秒/只 |
| `/trigger rz.sp.stop set 1` | 停止刷怪（已刷出的怪物保留） |
| `/trigger rz.sp.delete set 1` | 删除 4 格内最近的刷怪点 |
| `/trigger rz.sp.list set 1` | 显示刷怪点数量并用粒子标记位置 |

### 规则说明

- 每个刷怪点每轮刷 1 只盲盒僵尸（掉落随机怪物）；多个刷怪点共用同一循环，频率完全一致，且可同时存在多个。
- 频率随时间加快：每过 60 秒加快 1 秒，最低 3 秒/只；每次 `start` 都会重置为 20 秒起步。
- 刷怪点标记为隐形盔甲架（无敌、无重力、小体型，显示"刷怪点"名牌）。
- 只有已加载区块内的刷怪点会生效；删除刷怪点不会清除已刷出的怪物。
- 相关文件位于 `data/rz/function/spawnpoint/`：`create` / `start` / `stop` / `delete` / `list` / `second`。
- 配套插件通过计分板 `rz` 上的 `auto_start` / `auto_stop` / `auto_resume` 信号（tick 检测）自动开启/关闭/恢复刷怪，不再从控制台执行函数（避免刷屏）；手动 `/trigger rz.sp.start/stop` 同样调用对应函数；休整结束后用 `auto_resume` 按当前频率恢复（不重置加速进度）。

## 戴夫系统（数据包部分）

| 命令 | 功能 |
| --- | --- |
| `/trigger rz.dave.create set 1` | 在脚下生成村民“戴夫”（100 生命、原地不动、不自然消失） |

- 戴夫的队伍归属、怪物仇恨、团队血条、淘汰与胜负逻辑由配套的 Paper 插件 `DavePvE` 管理；纯原版/Fabric 环境下戴夫照常生成，但上述玩法不生效。
- 每队最多一个存活戴夫；玩家需先用原版 `/team` 加入队伍后再生成。
- 相关文件位于 `data/rz/function/dave/`：`create`。
- 插件提供完整比赛流程命令（详见插件说明）：
  - `/davepve ready` / `/davepve unready`：全体玩家可用的准备/取消准备；
  - `/davepve balance` / `/davepve start`：管理员自动分队（红/蓝/黄/绿，每队最多 5 人）与开局（清箱子、按配置坐标生成戴夫、自动开启刷怪）；
  - `/davepve kill <队伍名>`：管理员直接淘汰某队戴夫。

## 经济系统（三种货币）

怪物死亡会掉落三种货币（均为改名物品，可被商店识别与消费）：

| 货币 | 物品 | 掉落来源 |
| --- | --- | --- |
| 银币 | 铁粒（改名"银币"） | 普通怪物 |
| 金币 | 金粒（改名"金币"） | 精英怪物 |
| 钻币 | 钻石（改名"钻币"） | Boss 怪物 |

- 兑换比例（由配套插件商店提供兑换）：1 金 = 10 银，1 钻 = 10 金。
- 按怪物强度分级：普通怪（普通僵尸、骷髅、苦力怕、史莱姆等）掉银币；精英怪（铁甲僵尸、舞王僵尸、读报僵尸、蜘蛛女王、定时苦力怕等）掉金币；Boss（巨人僵尸、远古守卫者）掉钻币。
- 烈焰人专属战利品 `rz:blaze_coin` 现掉落 2-5 金币 + 小概率烈焰棒。

## 版本升级做了什么

1. **pack.mcmeta**：`pack_format: 41` → `min_format: 94.1 / max_format: 94.1`（1.21.9+ 的新写法）。
2. **目录改名**（1.21 起旧目录名不再生效）：
   - `data/rz/functions` → `data/rz/function`
   - `data/rz/loot_tables` → `data/rz/loot_table`
   - `data/minecraft/tags/functions` → `data/minecraft/tags/function`
   - `data/rz/tags/blocks` → `data/rz/tags/block`
3. **游戏规则改名**（1.21.11 全部改为 snake_case）：如 `announceAdvancements` → `show_advancement_messages`、`doDaylightCycle` → `advance_time`、`keepInventory` → `keep_inventory` 等，共 20 条。注意 `disableRaids true` 改为 `raids false`（新规则含义与旧规则相反，取值已翻转以保持"禁止袭击"的效果）。
4. **召唤 NBT 适配 1.21.5+**：
   - `ArmorItems`/`HandItems` 合并为 `equipment:{feet/legs/chest/head/mainhand/offhand:...}`；
   - `ArmorDropChances`/`HandDropChances` 合并为 `drop_chances:{...}`；
   - `Attributes:[{Base,Name}]` → `attributes:[{id:"minecraft:xxx",base}]`（去掉 `generic.` 前缀）；
   - 实体 NBT 字段仍以驼峰写法为准（`Health`、`Tags`、`CustomName`、`IsBaby`、`NoAI`、`DeathLootTable`、`Motion`、`Rotation`、`Pos`、`Fuse`、`HurtTime`、`Size`、`AbsorptionAmount`、`Invulnerable`、`Invisible`、`Small` 等）；1.21.5 起新引入的结构字段使用小写：`equipment`、`drop_chances`、`attributes`、`fall_distance`、`item`、`item_display`、`transformation`；
   - 物品组件：`"minecraft:unbreakable":{}` → `unbreakable:{}`、`"minecraft:dyed_color":{rgb:N}` → `dyed_color:N`；
   - 文本组件改为对象：`CustomName:{text:"中文"}`（不能再用 JSON 字符串 `'{"text":"中文"}'`，否则名字会显示成原始 JSON）。
5. **命令适配**：`attribute @s minecraft:generic.xxx` → `minecraft:xxx`；`data ... Health/Attributes[{Name:...}].Base` 等路径同步改为新字段名。

## 顺手修复的原数据包问题

- 史莱姆战利品表引用了不存在的表：`slime/large.json` 的 `rz:smile_coin` → `rz:slime_coin`；`slime/medium.json` 的 `rz:small_coin` → `rz:slime_coin`。
- `small_slime/summon` 的标签写错：`smile_slime` → `small_slime`。
- `tick.mcfunction` 拼写错误：`mediun_slime` → `medium_slime`。
- `medium_slime/main` 伤害判定写成了 `tag=large_slime`，改为 `tag=medium_slime`。
- `spider_egg/summon` 中 `spreadplayers ~ ~ ...` 使用了非法相对坐标，已删除该行。
- `drowned/summon` 的物品里 `count` 键重复，已删除一个。
- `giant/summon` 调用了不存在的 `rz:monsters/normal_little_zombie/summon`，改为实际的 `rz:monsters/small_zombie/summon`。
- `iron_armor_zombie/summon` 的标签误写为 `normal_zombie`（会与普通僵尸冲突），改为 `iron_armor_zombie`。
- 方块标签 `no_collision_box` 里的 `minecraft:cave_vines_head` / `cave_vines_body` 在 1.21.11 不存在（1.17 起已合并为 `cave_vines` + `cave_vines_plant`），改为 `cave_vines` + `cave_vines_plant`。
- 自定义伤害类型 `rz:arrow` 的 JSON 里 `effects: "hurt"` 与 `death_message_type: "default"` 在 1.21.11 不再是合法值（省略即默认），已移除这两个字段。
- 实体 NBT 字段名修正：此前误将全部字段改为小写（`health`/`tags`/`custom_name`/`is_baby` 等），这些在 1.21.11 会被静默丢弃（怪物失去标签、血量、名字，刷怪点盔甲架不隐形），现已恢复为驼峰写法，并同步修正 `data get/modify entity` 的数据路径（`Health`/`Motion`/`Rotation`/`Pos`/`Fuse`/`AbsorptionAmount` 等）。
- 展示实体变换格式修正：`transformation` 的 `right_rotation`/`left_rotation` 由旧格式 `{angle,axis}` 改为四元数 `[x,y,z,w]`（影响箭的朝向与巨人斧头动画）。
- 刷怪循环修复：`cycle2` 宏里的 `spreadplayers` 距离参数取自 `storage rz:summon.spread_range`，该值不能为 0（spreadplayers 要求 ≥1），否则整个宏函数实例化失败、循环不执行；初始值已改为 1（该功能默认关闭，无实际影响）。

## 目录结构与文件说明

### 顶层文件

| 文件 | 用途 |
| --- | --- |
| `pack.mcmeta` | 数据包元信息：声明兼容 1.21.11（format 94.1） |
| `pack.png` | 数据包图标 |
| `ids.json` | 作者的怪物 ID 对照表（游戏不会读取，仅作参考） |
| `writer.pyz` | 作者编写数据包用的辅助脚本（游戏不会读取） |
| `README.md` | 本说明文件 |

### 核心逻辑（`data/rz/function/`）

| 文件 | 用途 |
| --- | --- |
| `load.mcfunction` | 加载时执行一次：设置 20 条游戏规则、创建全部计分板、放置装备耐久中转箱、启动循环函数 |
| `tick.mcfunction` | 每游戏刻执行：检测地上的召唤物品，驱动所有怪物的 AI 主循环 |
| `seconds.mcfunction` | 每秒执行：高血量怪物粒子特效 + 溺尸蓄力检测 |
| `monster/cycle.mcfunction` | 怪物生成循环入口（宏函数） |
| `monster/cycle2.mcfunction` | 怪物生成循环主体：按间隔在召唤点掉落随机怪物物品 |
| `monster/random_health.mcfunction` | 随机化新怪物的最大生命值（1～10 倍） |
| `monster/summon.mcfunction` | 召唤物品落地处理：读取 ID → 二分查表召唤 → 初始化 |
| `monsters/id/**` | 怪物 ID 二分查找树：按 ID 依次缩小范围并调用对应召唤函数 |
| `things/detect_target_point.mcfunction` | 判断当前方块是否可站立（供伴舞骷髅定位） |
| `things/arrow/**` | 发射器僵尸射出的箭：生成、飞行、命中判定 |

### 怪物（`data/rz/function/monsters/`）

| 文件夹/文件 | 用途 |
| --- | --- |
| `normal_zombie/summon` | 普通僵尸 |
| `random_zombie/summon` | 盲盒僵尸（掉落随机怪物） |
| `iron_armor_zombie/summon` | 铁甲僵尸（全套铁装备） |
| `small_zombie/summon` | 小僵尸（同时是巨人抛出的小僵尸） |
| `drowned/summon` + `main` | 撑杆溺尸：蓄力后向玩家冲刺攻击 |
| `creeper/summon` | 苦力怕 |
| `time_creeper/summon` + `main` | 定时苦力怕：倒计时后传送到玩家面前引爆 |
| `skeleton/summon` | 骷髅（持弓） |
| `stray/summon` | 流浪者（持弓） |
| `football_skeleton/summon` | 橄榄球骷髅 |
| `black_football_skeleton/summon` | 黑橄榄球骷髅 |
| `dancing_zombie/summon` + `main` | 舞王僵尸：移动、转身、召唤伴舞 |
| `dancing_zombie/summon_backup_dancer/**` | 在东西南北四个方向生成伴舞骷髅 |
| `backup_dancer_skeleton/summon` + `main` | 伴舞骷髅：生成、动画、跟随主人 |
| `giant/summon` + `main` | 巨人僵尸：半血抛小僵尸、斧头挥砍动画 |
| `giant/normal_little_zombie.mcfunction` | 巨人抛出小僵尸的弹道控制 |
| `giant/animation.mcfunction` | 巨人斧头展示体的存活管理 |
| `blaze/summon` | 气球烈焰人 |
| `small_slime/summon` / `medium_slime/**` / `large_slime/**` | 小型/中型/大型破碎者跳跳（伤害 + 磨损装备耐久） |
| `small_magma_cube/summon` / `medium_magma_cube/**` / `large_magma_cube/**` | 小型/中型/大型地狱破碎者（失明/挖掘疲劳） |
| `spider/summon` + `main` | 相位蜘蛛：定时传送到玩家面前 |
| `spider_ling_normal/summon` | 普通小蜘蛛 |
| `spider_ling_poison/summon` | 剧毒小蜘蛛 |
| `spider_queen/summon` + `main` | 蜘蛛女王：定期产卵 |
| `spider_egg/summon` + `main` | 虫卵：定时孵化小蜘蛛 |
| `guardian/summon` / `elder_guardian/summon` | 守卫者 / 远古守卫者 |
| `sea_drowned/summon` + `main` + `call` | 海洋使徒：定期召唤守卫者/远古守卫者/热带鱼 |
| `witch/summon` | 女巫 |
| `newspaper_zombie/summon` + `main` + `anger` | 读报僵尸：吸收生命耗尽后暴怒 |
| `hf_dispenser_zombie/**` / `mf_dispenser_zombie/**` / `lf_dispenser_zombie/**` | 高/中/低频发射器僵尸：定时向玩家射箭 |

### 战利品表（`data/rz/loot_table/`）

JSON 不能写注释（游戏会拒收未知字段），故在此集中说明。所有 `monsters/*.json` 都是"召唤券"表：掉落一个小麦物品，通过 `set_custom_data` 写入怪物 ID，拾取后由 tick 检测并召唤对应怪物。

| 文件 | 用途 |
| --- | --- |
| `coin.json` | 通用"斗蛐蛐币"（铁粒改名），普通怪物掉落 |
| `slime_coin.json` | 史莱姆币：小概率掉落 1～2 枚 |
| `blaze_coin.json` | 烈焰人币：2～5 枚金币 + 小概率烈焰棒 |
| `gold_coin.json` | 金币：精英怪物掉落 2～5 枚 |
| `diamond_coin.json` | 钻币：Boss 怪物掉落 1～3 枚 |
| `random.json` | 盲盒：按权重随机指向任意怪物召唤表（盲盒僵尸掉落） |
| `monsters/<怪物名>.json` | 每种怪物的召唤券（怪物名对应 ids.json 的 ID） |
| `slime/large.json` | 大型破碎者跳跳的击杀掉落：史莱姆币 + 中型破碎者召唤券 |
| `slime/medium.json` | 中型破碎者跳跳的击杀掉落：史莱姆币 + 小型破碎者召唤券 |
| `magma_cube/large.json` | 大型地狱破碎者的击杀掉落：通用币 + 中型岩浆怪召唤券 |
| `magma_cube/medium.json` | 中型地狱破碎者的击杀掉落：通用币 + 小型岩浆怪召唤券 |

### 其他数据（`data/`）

| 文件 | 用途 |
| --- | --- |
| `rz/damage_type/arrow.json` | 自定义伤害类型"箭"（用于箭对玩家造成伤害） |
| `minecraft/tags/damage_type/bypasses_cooldown.json` | 让"箭"伤害无视受击冷却 |
| `rz/tags/block/no_collision_box.json` | 无碰撞方块列表：箭穿过这些方块不消失，撞墙才消失 |
| `minecraft/tags/function/load.json` | 声明加载函数 `rz:load` |
| `minecraft/tags/function/tick.json` | 声明每刻函数 `rz:tick` |
