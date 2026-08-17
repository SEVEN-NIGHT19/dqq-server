# 编译 DavePvE 插件源码与测试，并用 JUnit Console 运行全部逻辑测试
param(
    [string]$ProjectRoot = "C:\Users\Test\Documents\Codex\2026-08-04\n"
)

$ErrorActionPreference = "Stop"

$javac = "C:\Users\Test\AppData\Local\Temp\jdk21\jdk-21.0.6+7\bin\javac.exe"
$java = "C:\Users\Test\AppData\Local\Temp\jdk21\jdk-21.0.6+7\bin\java.exe"

$pluginSrc = Join-Path $ProjectRoot "work\dave-plugin\src\com\rz\dave"
$pluginOut = Join-Path $ProjectRoot "work\dave-plugin\build\classes"
$testSrc = Join-Path $ProjectRoot "work\dave-plugin\src\test\java"
$testOut = Join-Path $ProjectRoot "work\dave-plugin\build\test-classes"
$serverLibs = Join-Path $ProjectRoot "work\mctest-paper\libraries"
$testLibs = Join-Path $PSScriptRoot "test-libs"

if (-not (Test-Path -LiteralPath $testSrc)) { throw "未找到测试源码目录: $testSrc" }
if (-not (Get-ChildItem -LiteralPath $testLibs -Filter *.jar -ErrorAction SilentlyContinue)) {
    throw "缺少测试依赖，请先运行 download-test-libs.ps1"
}

# classpath：服务器全部依赖 jar + 测试 jar + 插件/测试 class 输出
$cp = @()
$cp += Get-ChildItem -LiteralPath $serverLibs -Recurse -Filter *.jar | ForEach-Object { $_.FullName }
$cp += Get-ChildItem -LiteralPath $testLibs -Filter *.jar | ForEach-Object { $_.FullName }
$classPath = ($cp + @($pluginOut, $testOut)) -join ";"

New-Item -ItemType Directory -Path $testOut -Force | Out-Null

# 1) 编译插件源码（含 UTF-8 中文，必须 -encoding UTF-8）
Write-Host "==> 编译插件源码"
$pluginFiles = Get-ChildItem -LiteralPath $pluginSrc -Filter *.java | ForEach-Object { $_.FullName }
& $javac -encoding UTF-8 -cp ($cp -join ";") -d $pluginOut @pluginFiles
if ($LASTEXITCODE -ne 0) { throw "插件编译失败" }

# 2) 编译测试
Write-Host "==> 编译测试"
$testFiles = Get-ChildItem -LiteralPath $testSrc -Recurse -Filter *.java | ForEach-Object { $_.FullName }
& $javac -encoding UTF-8 -cp $classPath -d $testOut @testFiles
if ($LASTEXITCODE -ne 0) { throw "测试编译失败" }

# 3) 运行测试
$consoleJar = Get-ChildItem -LiteralPath $testLibs -Filter "junit-platform-console-standalone-*.jar" | Select-Object -First 1
Write-Host "==> 运行测试"
& $java -jar $consoleJar.FullName --class-path $classPath --scan-class-path
exit $LASTEXITCODE
