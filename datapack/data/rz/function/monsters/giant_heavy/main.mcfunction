# 狂暴巨人僵尸AI：半血后抛掷阶段、索敌戴夫、挥斧造成3x3范围30点伤害
execute if entity @s[tag=!throwed] on passengers on passengers on vehicle on vehicle run tag @s add detecting
execute if entity @s[tag=!throwed,tag=!detecting] run tag @s add throwed
tag @s remove detecting

execute if data entity @s[tag=!got_half_max_health] {HurtTime:10s} store result score @s rz.giant.half_max_health run attribute @s minecraft:max_health base get 0.5
execute if data entity @s[tag=!got_half_max_health] {HurtTime:10s} run tag @s add got_half_max_health
execute if data entity @s[tag=!throwing,tag=!throwed] {HurtTime:10s} store result score @s rz.giant.health run data get entity @s Health
execute if data entity @s[tag=!throwing,tag=!throwed] {HurtTime:10s} if score @s rz.giant.health <= @s rz.giant.half_max_health run tag @s add throwing

scoreboard players add @s[tag=throwing] rz.giant.normal_little_zombie 1
effect give @s[scores={rz.giant.normal_little_zombie=1}] minecraft:slowness 1 255 true

execute if entity @s[scores={rz.giant.normal_little_zombie=10}] on passengers on passengers at @s anchored eyes run function rz:monsters/giant/normal_little_zombie

tag @s[scores={rz.giant.normal_little_zombie=20..}] add throwed
tag @s[scores={rz.giant.normal_little_zombie=20..}] remove throwing
scoreboard players reset @s[scores={rz.giant.normal_little_zombie=20..}] rz.giant.normal_little_zombie

execute on passengers at @s positioned ~ ~-3.425 ~ run data modify entity @s Rotation[0] set from entity @e[tag=rz,tag=giant_heavy,sort=nearest,limit=1] Rotation[0]
execute if entity @s[tag=!attacking,tag=!throwing] rotated ~ 0 positioned ^ ^ ^1 if entity @e[tag=dave,distance=..1.5] run effect give @s minecraft:slowness 1 255 true
execute if entity @s[tag=!attacking,tag=!throwing] rotated ~ 0 positioned ^ ^ ^1 if entity @e[tag=dave,distance=..1.5] run tag @s add attacking
scoreboard players add @s[tag=attacking] rz.giant.animation 1

execute if score @s rz.giant.animation matches 1 on passengers run data merge entity @s {start_interpolation:0,interpolation_duration:15}
execute if score @s rz.giant.animation matches 1 on passengers run data merge entity @s {transformation:{right_rotation:[-0.63667f,0f,0f,0.77111f],translation:[-0.8f,1.5f,0.925f]}}

execute if score @s rz.giant.animation matches 30 on passengers run data merge entity @s {start_interpolation:0,interpolation_duration:2}
execute if score @s rz.giant.animation matches 30 on passengers run data merge entity @s {transformation:{right_rotation:[0.56165f,0f,0f,0.82737f],translation:[-0.8f,1.5f,2.925f]}}

execute if score @s rz.giant.animation matches 34 rotated ~ 0 positioned ^ ^ ^1 as @e[tag=player,distance=..1.5] run damage @s 30 minecraft:mob_attack by @e[tag=rz,tag=giant_heavy,sort=nearest,limit=1]
execute if score @s rz.giant.animation matches 34 rotated ~ 0 positioned ^ ^ ^1 as @e[tag=dave,distance=..1.5] run damage @s 30 minecraft:mob_attack by @e[tag=rz,tag=giant_heavy,sort=nearest,limit=1]
execute if score @s rz.giant.animation matches 34 run playsound minecraft:entity.player.attack.crit hostile @a ~ ~ ~ 2 1.5
execute if score @s rz.giant.animation matches 40 on passengers run data merge entity @s {start_interpolation:0,interpolation_duration:10}
execute if score @s rz.giant.animation matches 40 on passengers run data merge entity @s {transformation:{right_rotation:[0f,0f,0f,1f],translation:[-0.8f,0f,2.425f]}}

tag @s[scores={rz.giant.animation=60..}] remove attacking
scoreboard players reset @s[scores={rz.giant.animation=60..}] rz.giant.animation
