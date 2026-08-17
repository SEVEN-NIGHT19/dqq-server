# 怪物ID二分查找：按ID区间继续二分
execute if score id rz matches ..7 run return run function rz:monsters/id/0_16/0_7/if
execute if score id rz matches 8..15 run return run function rz:monsters/id/0_16/8_15/if
function rz:monsters/backup_dancer_skeleton/summon