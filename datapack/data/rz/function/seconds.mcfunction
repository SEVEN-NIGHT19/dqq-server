# 每 1 秒执行一次：给高血量怪物显示粒子特效，并检测溺尸是否靠近玩家
schedule function rz:seconds 1s replace

execute as @e[tag=rz.LIGHT_YELLOW] at @s anchored eyes rotated 0 0 run particle minecraft:angry_villager ^ ^1 ^
execute as @e[tag=rz.YELLOW] at @s anchored eyes rotated 0 0 run particle minecraft:dust{color:[1d,1d,0d],scale:3f} ^ ^1 ^

execute as @e[tag=rz,tag=drowned,tag=!used_trident] at @s if entity @e[tag=player,sort=nearest,limit=1,distance=..2] run function rz:monsters/drowned/main