# 生成箭展示体并设置朝向与攻击者绑定
execute summon minecraft:item_display run data merge entity @s {Tags: ["rz", "arrow", "summoning"], item: {id: "minecraft:arrow", count: 1}, transformation: {right_rotation: [0f, 0.70711f, 0f, 0.70711f], left_rotation: [0.38268f, 0f, 0f, 0.92388f]}}
execute as @e[tag=rz,tag=arrow,tag=summoning] at @s run data modify entity @s Rotation set from entity @e[tag=rz,tag=monster,sort=nearest,limit=1] Rotation
execute as @e[tag=rz,tag=arrow,tag=summoning] at @s run scoreboard players operation @s rz.monster_uid = @e[tag=rz,tag=monster,sort=nearest,limit=1] rz.monster_uid

tag @e[tag=rz,tag=arrow,tag=summoning] remove summoning