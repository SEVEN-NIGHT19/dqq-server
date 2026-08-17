# 生成高频发射器僵尸实体（头戴发射器）
summon minecraft:zombie ~ ~ ~ {equipment:{chest:{id: "minecraft:leather_chestplate", count: 1, components: {dyed_color:11546150}},head:{id: "minecraft:dispenser", count: 1}},drop_chances:{feet:0f,legs:0f,chest:0f,head:0f},Tags: ["rz", "monster", "hf_dispenser_zombie"], IsBaby: 0b, CanPickUpLoot: 0b, DeathLootTable: "rz:gold_coin", CustomName:{text:"高频发射器僵尸"}}

execute as @e[tag=rz,tag=hf_dispenser_zombie,sort=nearest,limit=1] at @s facing entity @e[tag=player,sort=nearest,limit=1] eyes run tp @s ~ ~ ~ ~ ~