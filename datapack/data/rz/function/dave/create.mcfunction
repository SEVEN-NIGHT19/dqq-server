# 戴夫 - 创建：在玩家脚下生成村民"戴夫"（100 生命、原地不动、不自然消失）
summon minecraft:villager ~ ~ ~ {NoAI:1b,Health:100f,attributes:[{id:"minecraft:max_health",base:100d}],CustomName:{text:"戴夫"},CustomNameVisible:1b,PersistenceRequired:1b,Tags:["rz","dave"]}
tellraw @s [{"text":"[\u00a7e戴夫\u00a7f] "},{"text":"已生成戴夫（100 生命）","color":"green"}]

# 复位并重新启用 trigger
scoreboard players reset @s rz.dave.create
scoreboard players enable @s rz.dave.create
