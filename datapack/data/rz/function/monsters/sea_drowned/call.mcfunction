# 海洋使徒：随机召唤守卫者/远古守卫者/热带鱼
execute store result score @s rz.sea_drowned run random value 1..15
execute unless score @s rz.sea_drowned matches 15 run loot spawn ~ ~ ~ loot rz:monsters/guardian
execute if score @s rz.sea_drowned matches ..2 run summon minecraft:tropical_fish ~ ~ ~
execute if score @s rz.sea_drowned matches ..4 run summon minecraft:tropical_fish ~ ~ ~
execute if score @s rz.sea_drowned matches ..6 run summon minecraft:tropical_fish ~ ~ ~
execute if score @s rz.sea_drowned matches 15 run loot spawn ~ ~ ~ loot rz:monsters/elder_guardian
scoreboard players reset @s rz.sea_drowned