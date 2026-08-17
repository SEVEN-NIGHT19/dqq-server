# 定时苦力怕：倒计时结束传送至随机玩家面前并引爆
scoreboard players add @s rz.time_creeper 1

data modify entity @s[scores={rz.time_creeper=201..}] powered set value 0b
execute if score @s rz.time_creeper matches 201.. at @e[tag=player,sort=random,limit=1,distance=..30] anchored eyes run tp ^ ^ ^1
data modify entity @s[scores={rz.time_creeper=201..}] Fuse set value 0s