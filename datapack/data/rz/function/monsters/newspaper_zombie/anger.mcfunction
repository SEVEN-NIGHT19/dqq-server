# 读报僵尸暴怒：移除吸收生命、变慢、扔报纸、加速
data modify entity @s AbsorptionAmount set value 0f
effect give @s minecraft:slowness 2 255 true
effect give @s minecraft:resistance 2 5 true
item replace entity @s weapon.mainhand with minecraft:air
playsound minecraft:block.grass.break hostile @a ~ ~ ~
playsound minecraft:block.grass.break hostile @a ~ ~ ~
playsound minecraft:block.grass.break hostile @a ~ ~ ~
attribute @s minecraft:movement_speed base set 0.4
tag @s add in_anger
