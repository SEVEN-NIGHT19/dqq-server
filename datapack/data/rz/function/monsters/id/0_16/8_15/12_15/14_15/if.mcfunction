# 怪物ID二分查找：按怪物ID逐步缩小范围，最终调用对应怪物的召唤函数
execute if score id rz matches 14 run return run function rz:monsters/black_football_skeleton/summon
function rz:monsters/dancing_zombie/summon