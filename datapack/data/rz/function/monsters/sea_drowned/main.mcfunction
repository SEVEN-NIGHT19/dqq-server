# 海洋使徒：计时器满 140 时召唤援军
scoreboard players add @s rz.sea_drowned 1
execute if score @s rz.sea_drowned matches 140.. run function rz:monsters/sea_drowned/call