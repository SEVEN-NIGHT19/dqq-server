# 高频发射器僵尸：每 12 刻连发 4 支箭并播放发射音效
execute if score @s rz.hf_dispenser_zombie matches 12.. run scoreboard players set @s rz.hf_dispenser_zombie -18
scoreboard players add @s rz.hf_dispenser_zombie 1

execute if score @s rz.hf_dispenser_zombie matches 3 anchored eyes run playsound minecraft:block.dispenser.launch hostile @a ^ ^ ^
execute if score @s rz.hf_dispenser_zombie matches 6 anchored eyes run playsound minecraft:block.dispenser.launch hostile @a ^ ^ ^
execute if score @s rz.hf_dispenser_zombie matches 9 anchored eyes run playsound minecraft:block.dispenser.launch hostile @a ^ ^ ^
execute if score @s rz.hf_dispenser_zombie matches 12 anchored eyes run playsound minecraft:block.dispenser.launch hostile @a ^ ^ ^

execute if score @s rz.hf_dispenser_zombie matches 3 anchored eyes positioned ^ ^ ^.3 run function rz:things/arrow/summon
execute if score @s rz.hf_dispenser_zombie matches 6 anchored eyes positioned ^ ^ ^.3 run function rz:things/arrow/summon
execute if score @s rz.hf_dispenser_zombie matches 9 anchored eyes positioned ^ ^ ^.3 run function rz:things/arrow/summon
execute if score @s rz.hf_dispenser_zombie matches 12 anchored eyes positioned ^ ^ ^.3 run function rz:things/arrow/summon