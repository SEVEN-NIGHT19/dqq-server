# 巨人抛出的小僵尸运动控制（计算朝向与速度）
execute on vehicle anchored eyes rotated ~ 0 run summon minecraft:marker ^ ^ ^1 {Tags:["rz","giant.normal_little_zombie.motion"]}

execute store result score @s rz.giant.normal_little_zombie.pos2 run data get entity @s Pos[0]
execute store result score @s rz.giant.normal_little_zombie.pos rotated ~ 0 positioned ^ ^ ^1 run data get entity @e[tag=rz,tag=giant.normal_little_zombie.motion,sort=nearest,limit=1] Pos[0]
scoreboard players operation @s rz.giant.normal_little_zombie.pos2 -= @s rz.giant.normal_little_zombie.pos
execute store result entity @s Motion[0] double 1 run scoreboard players get @s rz.giant.normal_little_zombie.pos2

data modify entity @s Motion[1] set value 0.5d

execute store result score @s rz.giant.normal_little_zombie.pos2 run data get entity @s Pos[2]
execute store result score @s rz.giant.normal_little_zombie.pos rotated ~ 0 positioned ^ ^ ^1 run data get entity @e[tag=rz,tag=giant.normal_little_zombie.motion,sort=nearest,limit=1] Pos[2]
scoreboard players operation @s rz.giant.normal_little_zombie.pos2 -= @s rz.giant.normal_little_zombie.pos
execute store result entity @s Motion[2] double 1 run scoreboard players get @s rz.giant.normal_little_zombie.pos2

kill @e[tag=rz,tag=giant.normal_little_zombie.motion]
ride @s dismount