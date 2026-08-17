# 生成舞王僵尸实体，并计算其面朝玩家方向和移动向量
summon minecraft:zombie ~ ~ ~ {equipment:{feet:{id: "minecraft:leather_boots", count: 1, components: {dyed_color:11546150}},legs:{id: "minecraft:leather_leggings", count: 1, components: {dyed_color:11546150}},chest:{id: "minecraft:leather_chestplate", count: 1, components: {dyed_color:11546150}},head:{id: "minecraft:leather_helmet", count: 1, components: {unbreakable:{}, dyed_color:1908001, "minecraft:custom_data": {rotation: [0f, 20f], motion: [0d, 0d, 0d]}}}},drop_chances:{feet:0f,legs:0f,chest:0f,head:0f},Tags: ["rz", "monster", "dancing_zombie"], IsBaby: 0b, CanPickUpLoot: 0b, Health: 35f, attributes: [{id:"minecraft:max_health",base:35d}, {id:"minecraft:movement_speed",base:0.3d}], DeathLootTable: "", CustomName:{text:"舞王僵尸"}}

execute as @e[tag=rz,tag=dancing_zombie,sort=nearest,limit=1] at @s facing entity @e[tag=player,sort=nearest,limit=1] eyes run tp @s ~ ~ ~ ~180 ~

execute as @e[tag=rz,tag=dancing_zombie,sort=nearest,limit=1] store result entity @s equipment.head.components."minecraft:custom_data".rotation[0] float 1 run data get entity @s Rotation[0]

execute at @e[tag=rz,tag=dancing_zombie,sort=nearest,limit=1] rotated ~ 0 run summon minecraft:marker ^ ^ ^-.2 {Tags: ["rz", "dancing_zombie.facing"]}

execute as @e[tag=rz,tag=dancing_zombie,sort=nearest,limit=1] store result score @s rz.dancing_zombie.pos run data get entity @s Pos[0] 10
execute as @e[tag=rz,tag=dancing_zombie,sort=nearest,limit=1] at @s rotated ~ 0 positioned ^ ^ ^-.2 store result score @s rz.dancing_zombie.pos2 run data get entity @e[tag=rz,tag=dancing_zombie.facing,sort=nearest,limit=1] Pos[0] 10
execute as @e[tag=rz,tag=dancing_zombie,sort=nearest,limit=1] store result entity @s equipment.head.components."minecraft:custom_data".motion[0] double 0.1 run scoreboard players operation @s rz.dancing_zombie.pos2 -= @s rz.dancing_zombie.pos

execute as @e[tag=rz,tag=dancing_zombie,sort=nearest,limit=1] store result score @s rz.dancing_zombie.pos run data get entity @s Pos[2] 10
execute as @e[tag=rz,tag=dancing_zombie,sort=nearest,limit=1] at @s rotated ~ 0 positioned ^ ^ ^-.2 store result score @s rz.dancing_zombie.pos2 run data get entity @e[tag=rz,tag=dancing_zombie.facing,sort=nearest,limit=1] Pos[2] 10
execute as @e[tag=rz,tag=dancing_zombie,sort=nearest,limit=1] store result entity @s equipment.head.components."minecraft:custom_data".motion[2] double 0.1 run scoreboard players operation @s rz.dancing_zombie.pos2 -= @s rz.dancing_zombie.pos

kill @e[tag=rz,tag=dancing_zombie.facing]
