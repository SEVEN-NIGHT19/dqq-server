# 召唤物品落地后执行：读取物品里的怪物ID、查表召唤对应怪物、初始化血量与UID并清除物品
execute store result score id rz run data get entity @s Item.components."minecraft:custom_data".id
function rz:monsters/id/search
scoreboard players set @s rz 0
execute as @e[tag=rz,tag=monster,sort=nearest,limit=1] if entity @s[tag=full_max_health] run scoreboard players set @e[tag=rz,tag=summon,limit=1,sort=nearest] rz 1

scoreboard players operation @e[tag=rz,tag=monster,sort=nearest,limit=1] rz.monster_uid = uid rz.monster_uid
scoreboard players add uid rz.monster_uid 1

tag @e[tag=rz,tag=monster,tag=full_max_health,sort=nearest,limit=1] add rz.YELLOW

kill
