# 低频发射器僵尸：每 12 刻发射 1 支箭并播放音效
execute if score @s rz.lf_dispenser_zombie matches 12.. run scoreboard players set @s rz.lf_dispenser_zombie -18
scoreboard players add @s rz.lf_dispenser_zombie 1

execute if score @s rz.lf_dispenser_zombie matches 12 anchored eyes run playsound minecraft:block.dispenser.launch hostile @a ^ ^ ^

execute if score @s rz.lf_dispenser_zombie matches 12 anchored eyes positioned ^ ^ ^.3 run function rz:things/arrow/summon