# 怪物ID二分查找：按怪物ID逐步缩小范围，最终调用对应怪物的召唤函数
execute if score id rz matches 12 run return run function rz:monsters/newspaper_zombie/summon
function rz:monsters/football_skeleton/summon