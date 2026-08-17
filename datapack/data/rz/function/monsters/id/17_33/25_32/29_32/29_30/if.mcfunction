# 怪物ID二分查找：按怪物ID逐步缩小范围，最终调用对应怪物的召唤函数
execute if score id rz matches 29 run return run function rz:monsters/spider_ling_poison/summon
function rz:monsters/guardian/summon