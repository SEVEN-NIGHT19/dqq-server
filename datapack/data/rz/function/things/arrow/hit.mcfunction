# 箭命中怪物后对玩家造成伤害并清除箭
execute as @e[tag=rz,tag=monster,sort=nearest,limit=1] if score @s rz.monster_uid = @e[tag=rz,tag=arrow,sort=nearest,limit=1] rz.monster_uid run damage @e[tag=player,sort=nearest,limit=1] 1.5 rz:arrow by @s
kill @s