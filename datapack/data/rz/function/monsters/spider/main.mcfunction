# 相位蜘蛛：每 120 刻传送到最近玩家面前
scoreboard players add @s rz.spider 1
execute if score @s rz.spider matches 120.. at @a[tag=player,sort=nearest,distance=..50,limit=1] anchored eyes run tp @s ^ ^ ^-3
execute if score @s rz.spider matches 120.. run scoreboard players reset @s rz.spider