# 怪物ID二分查找入口：按怪物ID分派到 0-16 或 17-33 区间继续查找
execute if score id rz matches 0..16 run return run function rz:monsters/id/0_16/if
execute if score id rz matches 17..33 run function rz:monsters/id/17_33/if
execute if score id rz matches 34 run return run function rz:monsters/giant_heavy/summon
