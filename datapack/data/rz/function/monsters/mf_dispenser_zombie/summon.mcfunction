# 生成中频发射器僵尸实体（头戴发射器）
summon minecraft:zombie ~ ~ ~ {equipment:{chest:{id: "minecraft:leather_chestplate", count: 1, components: {dyed_color:16701501}},head:{id: "minecraft:dispenser", count: 1}},drop_chances:{feet:0f,legs:0f,chest:0f,head:0f},Tags: ["rz", "monster", "mf_dispenser_zombie"], IsBaby: 0b, CanPickUpLoot: 0b, DeathLootTable: "rz:coin", CustomName:{text:"中频发射器僵尸"}}

execute as @e[tag=rz,tag=mf_dispenser_zombie,sort=nearest,limit=1] at @s facing entity @e[tag=player,sort=nearest,limit=1] eyes run tp @s ~ ~ ~ ~ ~