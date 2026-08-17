# 巨人斧头展示体动画管理：随乘客存活而存活
execute on vehicle on passengers run tag @s add detecting
execute if entity @s[tag=!detecting] run kill
tag @s remove detecting