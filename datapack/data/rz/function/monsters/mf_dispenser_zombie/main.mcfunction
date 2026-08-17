# 中频发射器僵尸：每 6/12 刻发射箭并播放音效
execute if score @s rz.mf_dispenser_zombie matches 12.. run scoreboard players set @s rz.mf_dispenser_zombie -18
scoreboard players add @s rz.mf_dispenser_zombie 1

execute if score @s rz.mf_dispenser_zombie matches 6 anchored eyes run playsound minecraft:block.dispenser.launch hostile @a ^ ^ ^
execute if score @s rz.mf_dispenser_zombie matches 12 anchored eyes run playsound minecraft:block.dispenser.launch hostile @a ^ ^ ^

execute if score @s rz.mf_dispenser_zombie matches 6 anchored eyes positioned ^ ^ ^.3 run function rz:things/arrow/summon
execute if score @s rz.mf_dispenser_zombie matches 12 anchored eyes positioned ^ ^ ^.3 run function rz:things/arrow/summon