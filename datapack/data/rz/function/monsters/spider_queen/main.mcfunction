# 蜘蛛女王：每 200 刻产下一枚虫卵
scoreboard players add @s rz.spider_queen 1
execute if score @s rz.spider_queen matches 200.. at @s run loot spawn ~ ~ ~ loot rz:monsters/spider_egg
execute if score @s rz.spider_queen matches 200.. run scoreboard players reset @s rz.spider_queen