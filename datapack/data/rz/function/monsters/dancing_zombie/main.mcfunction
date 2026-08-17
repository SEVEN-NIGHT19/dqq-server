# 舞王僵尸动画控制：前后移动、转身、动画 41 时召唤伴舞骷髅
scoreboard players add @s rz.dancing_zombie.animation 1

data modify entity @s[scores={rz.dancing_zombie.animation=..40}] Motion[0] set from entity @s equipment.head.components."minecraft:custom_data".motion[0]
data modify entity @s[scores={rz.dancing_zombie.animation=..40}] Motion[2] set from entity @s equipment.head.components."minecraft:custom_data".motion[2]
data modify entity @s[scores={rz.dancing_zombie.animation=..70}] Rotation set from entity @s equipment.head.components."minecraft:custom_data".rotation

data modify entity @s[scores={rz.dancing_zombie.animation=41}] equipment.head.components."minecraft:custom_data".rotation[1] set value 0f
attribute @s[scores={rz.dancing_zombie.animation=41}] minecraft:movement_speed base set 0
execute if score @s rz.dancing_zombie.animation matches 41 run function rz:monsters/dancing_zombie/summon_backup_dancer/summon

attribute @s[scores={rz.dancing_zombie.animation=70}] minecraft:movement_speed base set 0.23

data modify entity @s[scores={rz.dancing_zombie.animation=131..170}] Motion[0] set value 0d
data modify entity @s[scores={rz.dancing_zombie.animation=131..170}] Motion[2] set value 0d
scoreboard players operation @s[scores={rz.dancing_zombie.animation=131..170}] rz.dancing_zombie.rotation = @s rz.dancing_zombie.animation
scoreboard players remove @s[scores={rz.dancing_zombie.animation=131..170}] rz.dancing_zombie.rotation 130
execute store result entity @s[scores={rz.dancing_zombie.animation=131..150}] Rotation[0] float 36 run scoreboard players get @s rz.dancing_zombie.rotation
execute store result entity @s[scores={rz.dancing_zombie.animation=151..170}] Rotation[0] float -36 run scoreboard players get @s rz.dancing_zombie.rotation
scoreboard players set @s[scores={rz.dancing_zombie.animation=170..}] rz.dancing_zombie.animation 70