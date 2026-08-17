# 生成巨人僵尸实体：附加斧头展示体、骑乘小僵尸、隐形本体
summon minecraft:zombie ~ ~ ~ {equipment:{feet:{id: "minecraft:leather_boots", count: 1},legs:{id: "minecraft:leather_leggings", count: 1, components: {dyed_color:1908001}},chest:{id: "minecraft:leather_chestplate",count: 1, components: {dyed_color:3847130}},head:{id: "minecraft:zombie_head", count: 1}},drop_chances:{feet:0f,legs:0f,chest:0f,head:0f},Tags: ["rz", "monster", "giant"], IsBaby: 0b, CanPickUpLoot: 0b, Health: 200f, attributes: [{id:"minecraft:max_health",base:200d}, {id:"minecraft:scale",base:2d}, {id:"minecraft:movement_speed",base:0.15d}, {id:"minecraft:attack_damage",base:0d}, {id:"minecraft:knockback_resistance",base:1d}], DeathLootTable: "rz:diamond_coin", CustomName:{text:"巨人僵尸"}}

execute as @e[tag=rz,tag=giant,tag=!summoned,sort=nearest,limit=1] run effect give @s minecraft:invisibility infinite 0 true
execute as @e[tag=rz,tag=giant,tag=!summoned,sort=nearest,limit=1] at @s summon minecraft:item_display run data merge entity @s {Tags:["rz","giant.animation"],item:{id:"minecraft:wooden_axe",count:1},item_display:firstperson_righthand,transformation:{scale:[3f,3f,3f],translation:[-0.8f,0f,2.425f]},Rotation:[0f,90f]}
execute as @e[tag=rz,tag=giant,tag=!summoned,sort=nearest,limit=1] at @s run ride @e[tag=giant.animation,sort=nearest,limit=1] mount @s

execute as @e[tag=rz,tag=giant,tag=!summoned,sort=nearest,limit=1] on passengers at @s run function rz:monsters/small_zombie/summon
execute as @e[tag=rz,tag=giant,tag=!summoned,sort=nearest,limit=1] on passengers at @s run ride @e[tag=rz,tag=normal_little_zombie,sort=nearest,limit=1] mount @s
tag @e[tag=rz,tag=giant,tag=!summoned,sort=nearest,limit=1] add summoned
