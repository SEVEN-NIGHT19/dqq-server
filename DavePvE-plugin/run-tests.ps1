# DavePvE 构建与测试入口（Maven 版）
# 依赖由 pom.xml 管理（paper-api provided + junit-jupiter/mockbukkit test），
# 编译、测试全部走 Apache Maven；产物 target/dave-plugin.jar。
param(
    [string]$ProjectRoot = "C:\Users\Test\Documents\Codex\2026-08-04\n"
)

$ErrorActionPreference = "Stop"

$mvn = Join-Path $ProjectRoot "work\tools\apache-maven-3.9.9\bin\mvn.cmd"
$jdk = Join-Path $ProjectRoot "work\tools\jdk-21.0.6+7"
$pom = Join-Path $ProjectRoot "work\dave-plugin\pom.xml"

if (-not (Test-Path -LiteralPath $mvn)) {
    throw "未找到 Maven: $mvn（应位于 work\tools\apache-maven-3.9.9，随项目自带）"
}
if (-not (Test-Path -LiteralPath (Join-Path $jdk "bin\java.exe"))) {
    throw "未找到完整版 JDK 21: $jdk（Maven 需要完整版，含 extnet.dll；不要用精简版 JDK）"
}

$env:JAVA_HOME = $jdk
Write-Host "==> mvn clean test (pom: $pom)"
& $mvn -f $pom -B -q clean test
exit $LASTEXITCODE