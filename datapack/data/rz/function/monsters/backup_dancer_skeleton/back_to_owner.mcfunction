# 伴舞骷髅回到舞王僵尸身边（检测主人位置并传送）
execute if score @s rz.monster_uid = @e[tag=rz,tag=backup_dancer_skeleton,sort=nearest,limit=1,tag=has_owner] rz.backup_dancer_skeleton.owner run tag @e[tag=rz,tag=backup_dancer_skeleton,sort=nearest,limit=1,tag=has_owner] add found_owner
execute at @s run tp @e[tag=found_owner] @s
execute at @s positioned ~1 ~ ~ if function rz:things/detect_target_point run tp @e[tag=found_owner,tag=east] ~ ~ ~
execute at @s positioned ~1 ~-1 ~ if function rz:things/detect_target_point run tp @e[tag=found_owner,tag=east] ~ ~ ~
execute at @s positioned ~1 ~1 ~ if function rz:things/detect_target_point run tp @e[tag=found_owner,tag=east] ~ ~ ~

execute at @s positioned ~-1 ~ ~ if function rz:things/detect_target_point run tp @e[tag=found_owner,tag=west] ~ ~ ~
execute at @s positioned ~-1 ~-1 ~ if function rz:things/detect_target_point run tp @e[tag=found_owner,tag=west] ~ ~ ~
execute at @s positioned ~-1 ~1 ~ if function rz:things/detect_target_point run tp @e[tag=found_owner,tag=west] ~ ~ ~

execute at @s positioned ~ ~ ~1 if function rz:things/detect_target_point run tp @e[tag=found_owner,tag=south] ~ ~ ~
execute at @s positioned ~ ~-1 ~1 if function rz:things/detect_target_point run tp @e[tag=found_owner,tag=south] ~ ~ ~
execute at @s positioned ~ ~1 ~1 if function rz:things/detect_target_point run tp @e[tag=found_owner,tag=south] ~ ~ ~

execute at @s positioned ~ ~ ~-1 if function rz:things/detect_target_point run tp @e[tag=found_owner,tag=snorh] ~ ~ ~
execute at @s positioned ~ ~-1 ~-1 if function rz:things/detect_target_point run tp @e[tag=found_owner,tag=snorh] ~ ~ ~
execute at @s positioned ~ ~1 ~-1 if function rz:things/detect_target_point run tp @e[tag=found_owner,tag=snorh] ~ ~ ~
tag @e[tag=found_owner] remove found_owner