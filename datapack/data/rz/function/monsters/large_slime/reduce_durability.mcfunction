# 大型破碎者跳跳：磨损玩家身上装备的耐久（借用固定坐标的箱子中转）
item replace block -23 43 -19 container.0 from entity @s armor.head
data modify storage rz:durability_head Damage set from block -23 43 -19 Items[{Slot:0b}].components.minecraft:damage
execute store result score @s rz.temp run data get storage rz:durability_head Damage
scoreboard players add @s rz.temp 10
execute store result block -23 43 -19 Items[{Slot:0b}].components.minecraft:damage int 1 run scoreboard players get @s rz.temp
item replace entity @s armor.head from block -23 43 -19 container.0

item replace block -23 43 -19 container.1 from entity @s armor.chest
data modify storage rz:durability_chest Damage set from block -23 43 -19 Items[{Slot:1b}].components.minecraft:damage
execute store result score @s rz.temp run data get storage rz:durability_chest Damage
scoreboard players add @s rz.temp 20
execute store result block -23 43 -19 Items[{Slot:1b}].components.minecraft:damage int 1 run scoreboard players get @s rz.temp
item replace entity @s armor.chest from block -23 43 -19 container.1

item replace block -23 43 -19 container.2 from entity @s armor.legs
data modify storage rz:durability_legs Damage set from block -23 43 -19 Items[{Slot:2b}].components.minecraft:damage
execute store result score @s rz.temp run data get storage rz:durability_legs Damage
scoreboard players add @s rz.temp 15
execute store result block -23 43 -19 Items[{Slot:2b}].components.minecraft:damage int 1 run scoreboard players get @s rz.temp
item replace entity @s armor.legs from block -23 43 -19 container.2

item replace block -23 43 -19 container.3 from entity @s armor.feet
data modify storage rz:durability_feet Damage set from block -23 43 -19 Items[{Slot:3b}].components.minecraft:damage
execute store result score @s rz.temp run data get storage rz:durability_feet Damage
scoreboard players add @s rz.temp 10
execute store result block -23 43 -19 Items[{Slot:3b}].components.minecraft:damage int 1 run scoreboard players get @s rz.temp
item replace entity @s armor.feet from block -23 43 -19 container.3