# 读报僵尸：吸收生命低于阈值时触发暴怒
execute store result score @s rz.newspaper_zombie run data get entity @s AbsorptionAmount
execute if score @s rz.newspaper_zombie matches ..1019 run function rz:monsters/newspaper_zombie/anger
