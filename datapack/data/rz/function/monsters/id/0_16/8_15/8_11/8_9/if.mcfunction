# 怪物ID二分查找：按怪物ID逐步缩小范围，最终调用对应怪物的召唤函数
execute if score id rz matches 8 run return run function rz:monsters/time_creeper/summon
function rz:monsters/lf_dispenser_zombie/summon