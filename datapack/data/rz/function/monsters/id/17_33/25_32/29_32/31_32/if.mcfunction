# 怪物ID二分查找：按怪物ID逐步缩小范围，最终调用对应怪物的召唤函数
execute if score id rz matches 31 run return run function rz:monsters/elder_guardian/summon
function rz:monsters/sea_drowned/summon