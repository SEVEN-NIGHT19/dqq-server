# 大型破碎者跳跳：定时循环并伤害 2.8 格内的玩家
schedule function rz:monsters/large_slime/main 5t replace
execute as @a[tag=player,distance=2.8] run damage @s 6 minecraft:mob_attack by @e[tag=monster,tag=large_slime,sort=nearest,limit=1]