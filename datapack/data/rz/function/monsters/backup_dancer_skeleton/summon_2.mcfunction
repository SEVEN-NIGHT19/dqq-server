# 生成伴舞骷髅并绑定主人、禁用AI、设置动画初始状态
execute positioned ~ ~-1.5 ~ run function rz:monsters/backup_dancer_skeleton/summon
execute positioned ~ ~-1.5 ~ run data modify entity @e[tag=rz,tag=backup_dancer_skeleton,sort=nearest,limit=1] NoAI set value 1b
execute positioned ~ ~-1.5 ~ run scoreboard players set @e[tag=rz,tag=backup_dancer_skeleton,sort=nearest,limit=1] rz.backup_dancer_skeleton.animation -30
execute positioned ~ ~-1.5 ~ run scoreboard players operation @e[tag=rz,tag=backup_dancer_skeleton,sort=nearest,limit=1] rz.backup_dancer_skeleton.owner = @e[tag=rz,tag=dancing_zombie,sort=nearest,limit=1] rz.monster_uid
execute positioned ~ ~-1.5 ~ run tag @e[tag=rz,tag=backup_dancer_skeleton,sort=nearest,limit=1] add has_owner