# 伴舞骷髅动画控制：落地、停步、转身面向舞王
scoreboard players add @s rz.backup_dancer_skeleton.animation 1
tp @s[scores={rz.backup_dancer_skeleton.animation=..0}] ~ ~.05 ~
data remove entity @s no_ai
execute if entity @s[tag=has_owner] unless entity @e[tag=rz,tag=dancing_zombie] run tag @s remove has_owner

execute if entity @s[tag=has_owner,scores={rz.backup_dancer_skeleton.animation=61}] as @e[tag=rz,tag=dancing_zombie] run function rz:monsters/backup_dancer_skeleton/back_to_owner

data modify entity @s[tag=has_owner,scores={rz.backup_dancer_skeleton.animation=61..100}] Motion[0] set value 0d
data modify entity @s[tag=has_owner,scores={rz.backup_dancer_skeleton.animation=61..100}] Motion[2] set value 0d
scoreboard players operation @s[tag=has_owner,scores={rz.backup_dancer_skeleton.animation=61..100}] rz.backup_dancer_skeleton.rotation = @s rz.backup_dancer_skeleton.animation
scoreboard players remove @s[tag=has_owner,scores={rz.backup_dancer_skeleton.animation=61..100}] rz.backup_dancer_skeleton.rotation 60
execute store result entity @s[tag=has_owner,scores={rz.backup_dancer_skeleton.animation=61..80}] Rotation[0] float 36 run scoreboard players get @s rz.backup_dancer_skeleton.rotation
execute store result entity @s[tag=has_owner,scores={rz.backup_dancer_skeleton.animation=81..100}] Rotation[0] float -36 run scoreboard players get @s rz.backup_dancer_skeleton.rotation
scoreboard players set @s[tag=has_owner,scores={rz.backup_dancer_skeleton.animation=100..}] rz.backup_dancer_skeleton.animation 0
