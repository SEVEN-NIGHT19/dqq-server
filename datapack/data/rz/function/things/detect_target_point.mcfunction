# 检测当前方块是否可站立（头顶无碰撞、脚下有方块），供伴舞骷髅定位使用
execute if block ~ ~ ~ #rz:no_collision_box unless block ~ ~-1 ~ #rz:no_collision_box run return 1
return fail