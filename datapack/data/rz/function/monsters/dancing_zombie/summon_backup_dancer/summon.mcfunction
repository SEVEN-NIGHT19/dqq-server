# 检测舞王四周可落地点，并分别向四个方向生成伴舞骷髅
execute positioned ~1 ~ ~ if function rz:things/detect_target_point run function rz:monsters/dancing_zombie/summon_backup_dancer/east
execute positioned ~1 ~-1 ~ if function rz:things/detect_target_point run function rz:monsters/dancing_zombie/summon_backup_dancer/east
execute positioned ~1 ~1 ~ if function rz:things/detect_target_point run function rz:monsters/dancing_zombie/summon_backup_dancer/east

execute positioned ~-1 ~ ~ if function rz:things/detect_target_point run function rz:monsters/dancing_zombie/summon_backup_dancer/west
execute positioned ~-1 ~-1 ~ if function rz:things/detect_target_point run function rz:monsters/dancing_zombie/summon_backup_dancer/west
execute positioned ~-1 ~1 ~ if function rz:things/detect_target_point run function rz:monsters/dancing_zombie/summon_backup_dancer/west

execute positioned ~ ~ ~1 if function rz:things/detect_target_point run function rz:monsters/dancing_zombie/summon_backup_dancer/south
execute positioned ~ ~-1 ~1 if function rz:things/detect_target_point run function rz:monsters/dancing_zombie/summon_backup_dancer/south
execute positioned ~ ~1 ~1 if function rz:things/detect_target_point run function rz:monsters/dancing_zombie/summon_backup_dancer/south

execute positioned ~ ~ ~-1 if function rz:things/detect_target_point run function rz:monsters/dancing_zombie/summon_backup_dancer/north
execute positioned ~ ~-1 ~-1 if function rz:things/detect_target_point run function rz:monsters/dancing_zombie/summon_backup_dancer/north
execute positioned ~ ~1 ~-1 if function rz:things/detect_target_point run function rz:monsters/dancing_zombie/summon_backup_dancer/north