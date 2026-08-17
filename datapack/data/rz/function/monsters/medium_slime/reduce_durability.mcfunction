# 中型破碎者跳跳：磨损玩家主手武器的耐久
item replace block -23 43 -19 container.4 from entity @s weapon.mainhand
data modify storage rz:durability_mainhand Damage set from block -23 43 -19 Items[{Slot:4b}].components.minecraft:damage
execute store result score @s rz.temp run data get storage rz:durability_mainhand Damage
scoreboard players add @s rz.temp 25
execute store result block -23 43 -19 Items[{Slot:4b}].components.minecraft:damage int 1 run scoreboard players get @s rz.temp
item replace entity @s weapon.mainhand from block -23 43 -19 container.4