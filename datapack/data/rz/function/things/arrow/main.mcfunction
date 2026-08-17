# 箭展示体飞行逻辑：每刻前进、命中玩家或撞墙时消失
tp ^ ^ ^.6
scoreboard players add @s rz.arrow 1

execute positioned ~-.25 ~-.25 ~-.25 if entity @e[tag=player,sort=nearest,limit=1,dx=-.5,dy=-.5,dz=-.5] run return run function rz:things/arrow/hit
execute if entity @s[scores={rz.arrow=70..}] run return run kill
execute unless block ~ ~ ~ #rz:no_collision_box run kill