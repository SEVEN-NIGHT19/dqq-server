# 下载 DavePvE 测试依赖（JUnit Console Standalone + MockBukkit）到本目录的 test-libs\
$ErrorActionPreference = "Stop"

$junitVersion = "1.14.4"
$mockbukkitArtifact = "mockbukkit-v1.21"
$mockbukkitVersion = "4.110.0"

$libs = Join-Path $PSScriptRoot "test-libs"
New-Item -ItemType Directory -Path $libs -Force | Out-Null

$urls = @(
    "https://repo1.maven.org/maven2/org/junit/platform/junit-platform-console-standalone/$junitVersion/junit-platform-console-standalone-$junitVersion.jar",
    "https://repo1.maven.org/maven2/org/mockbukkit/mockbukkit/$mockbukkitArtifact/$mockbukkitVersion/$mockbukkitArtifact-$mockbukkitVersion.jar"
)

foreach ($url in $urls) {
    $file = Join-Path $libs ([System.IO.Path]::GetFileName($url))
    if (Test-Path -LiteralPath $file) {
        Write-Host "已存在，跳过: $file"
        continue
    }
    Write-Host "下载 $url"
    Invoke-WebRequest -Uri $url -OutFile $file -UseBasicParsing
}

Write-Host "完成。测试依赖位于 $libs"
