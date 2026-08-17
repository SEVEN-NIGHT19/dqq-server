# 虫卵：120 刻后孵化出 5 只小蜘蛛并消失
scoreboard players add @s rz.spider_egg 1
execute if score @s rz.spider_egg matches 120.. run loot spawn ~ ~ ~ loot rz:monsters/spider_ling_normal
execute if score @s rz.spider_egg matches 120.. run loot spawn ~ ~ ~ loot rz:monsters/spider_ling_normal
execute if score @s rz.spider_egg matches 120.. run loot spawn ~ ~ ~ loot rz:monsters/spider_ling_normal
execute if score @s rz.spider_egg matches 120.. run loot spawn ~ ~ ~ loot rz:monsters/spider_ling_normal
execute if score @s rz.spider_egg matches 120.. run loot spawn ~ ~ ~ loot rz:monsters/spider_ling_poison
execute if score @s rz.spider_egg matches 120.. run kill @s