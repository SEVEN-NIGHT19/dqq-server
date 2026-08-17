# 每游戏刻执行：检测地上的召唤物品并执行召唤，驱动所有怪物的 AI 主循环
execute as @e[type=minecraft:item,nbt={Item:{components:{"minecraft:custom_data":{rz:1b,summon:1b}}}}] at @s run function rz:monster/summon

execute as @e[tag=rz,tag=newspaper_zombie,tag=!in_anger,nbt={HurtTime:5s}] at @s run function rz:monsters/newspaper_zombie/main
execute as @e[tag=rz,tag=hf_dispenser_zombie] at @s run function rz:monsters/hf_dispenser_zombie/main
execute as @e[tag=rz,tag=mf_dispenser_zombie] at @s run function rz:monsters/mf_dispenser_zombie/main
execute as @e[tag=rz,tag=lf_dispenser_zombie] at @s run function rz:monsters/lf_dispenser_zombie/main
execute as @e[tag=rz,tag=giant,tag=summoned] at @s run function rz:monsters/giant/main
execute as @e[tag=rz,tag=giant.animation] at @s run function rz:monsters/giant/animation
execute as @e[tag=rz,tag=giant_heavy,tag=summoned] at @s run function rz:monsters/giant_heavy/main
execute as @e[tag=rz,tag=giant_heavy.animation] at @s run function rz:monsters/giant/animation
execute as @e[tag=rz,tag=time_creeper] at @s run function rz:monsters/time_creeper/main
execute as @a[scores={rz.hurt=1..}] at @s if entity @e[tag=large_slime,distance=..5] run function rz:monsters/large_slime/reduce_durability
execute as @a[scores={rz.hurt=1..}] at @s if entity @e[tag=medium_slime,distance=..5] run function rz:monsters/medium_slime/reduce_durability
execute as @a[scores={rz.hurt=1..}] at @s if entity @e[tag=large_magma_cube,distance=..5] run function rz:monsters/large_magma_cube/main
execute as @a[scores={rz.hurt=1..}] at @s if entity @e[tag=medium_magma_cube,distance=..5] run function rz:monsters/medium_magma_cube/main
scoreboard players reset @a rz.hurt
execute as @e[tag=rz,tag=spider] at @s run function rz:monsters/spider/main
execute as @e[tag=rz,tag=dancing_zombie] at @s run function rz:monsters/dancing_zombie/main
execute as @e[tag=rz,tag=backup_dancer_skeleton] at @s run function rz:monsters/backup_dancer_skeleton/main
execute as @e[tag=rz,tag=arrow,tag=!summoning] at @s run function rz:things/arrow/main
execute as @e[tag=rz,tag=spider_queen] at @s run function rz:monsters/spider_queen/main
execute as @e[tag=rz,tag=spider_egg] at @s run function rz:monsters/spider_egg/main
execute as @e[tag=rz,tag=sea_drowned] at @s run function rz:monsters/sea_drowned/main

# 戴夫 trigger 分发（刷怪点 trigger 已由 DavePvE 插件直接处理）
execute as @a[scores={rz.dave.create=1}] at @s run function rz:dave/create
# 确保新加入的玩家也可用戴夫 trigger
scoreboard players enable @a rz.dave.create
tag @a add player
