# 中型破碎者跳跳：定时循环并伤害 2.8 格内的玩家
schedule function rz:monsters/medium_slime/main 5t replace
execute as @a[tag=player,distance=2.8] run damage @s 3 minecraft:mob_attack by @e[tag=monster,tag=medium_slime,sort=nearest,limit=1]