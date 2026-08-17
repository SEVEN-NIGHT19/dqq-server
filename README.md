# DavePvE Server Content

斗蛐蛐 PvE 服务器可公开发布内容合集。

## 仓库内容

```
DavePvE-plugin/   DavePvE 插件源码（Java + plugin.yml + 测试）
datapack/         斗蛐蛐数据包（Minecraft 1.21.11）
mythicmobs/       MythicMobs 自定义怪物配置 RZMonsters.yml
```

> 第三方插件本体（WorldEdit、WorldGuard、DecentHolograms、MythicMobs 等）不包含在本仓库中，请到各自官方渠道获取。
> 服务器部署/更新脚本（依赖 MCSM 面板凭据）不包含在本仓库中。

## DavePvE 插件

- 目标服务器：Paper 1.21.11 / api-version 1.21 / JDK 21
- 主类：`com.rz.dave.DavePvEPlugin`
- 核心逻辑：`com.rz.dave.DaveManager`
- 新模式：植物大战僵尸模式（`com.rz.dave.PvzMode`）

### 构建与测试

项目使用手工 javac，无 Gradle/Maven。

```powershell
# 首次准备测试依赖
powershell -ExecutionPolicy Bypass -File download-test-libs.ps1

# 编译并运行测试
powershell -ExecutionPolicy Bypass -File run-tests.ps1
```

## 说明

- 源代码中不含服务器面板密码、RCON 密码等敏感信息。
- 若需部署到服务器，请按项目内 `AGENTS.md` / `PENDING_DEPLOY.md` 流程执行。