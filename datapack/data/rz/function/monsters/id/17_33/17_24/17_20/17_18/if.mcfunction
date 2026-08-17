# 怪物ID二分查找：按怪物ID逐步缩小范围，最终调用对应怪物的召唤函数
execute if score id rz matches 17 run return run function rz:monsters/giant/summon
# 气球烈焰人已迁移至插件，不再由数据包召唤
