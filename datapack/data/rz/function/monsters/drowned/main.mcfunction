# 溺尸蓄力冲刺：播放音效、按玩家方位计算速度、冲刺攻击玩家
playsound minecraft:item.trident.riptide_1 hostile @a ~ ~ ~

execute store result score @s rz.drowned.player_pos run data get entity @p Pos[0]
execute store result score @s rz.drowned.pos run data get entity @s Pos[0]
scoreboard players operation @s rz.drowned.player_pos -= @s rz.drowned.pos
execute store result entity @s Motion[0] double 0.5 run scoreboard players get @s rz.drowned.player_pos

data modify entity @s Motion[1] set value 0.8d

execute store result score @s rz.drowned.player_pos run data get entity @p Pos[2]
execute store result score @s rz.drowned.pos run data get entity @s Pos[2]
scoreboard players operation @s rz.drowned.player_pos -= @s rz.drowned.pos
execute store result entity @s Motion[2] double 0.5 run scoreboard players get @s rz.drowned.player_pos

damage @p 7 minecraft:trident by @s


item replace entity @s weapon.offhand with minecraft:air
tag @s add used_trident
attribute @s minecraft:movement_speed base set 0.23
attribute @s minecraft:attack_damage base set 3
