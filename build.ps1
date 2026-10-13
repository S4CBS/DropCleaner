# Build script for Drop Cleaner (Minecraft 1.21.1 / NeoForge 21.1.255)
$ErrorActionPreference = "Stop"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path

Write-Host ">>> Building Drop Cleaner..." -ForegroundColor Cyan

$classesDir = Join-Path $ScriptDir "build\classes"
$libsDir = Join-Path $ScriptDir "build\libs"
$jarPath = Join-Path $libsDir "DropCleaner-1.21.1-1.0.0.jar"

if (Test-Path (Join-Path $ScriptDir "build")) {
    Remove-Item -Recurse -Force (Join-Path $ScriptDir "build")
}
New-Item -ItemType Directory -Force -Path $classesDir | Out-Null
New-Item -ItemType Directory -Force -Path $libsDir | Out-Null

$clientJar = "$env:APPDATA\.minecraft\libraries\net\minecraft\client\1.21.1-20240808.144430\client-1.21.1-20240808.144430-srg.jar"
$libJars = (Get-ChildItem -Path "$env:APPDATA\.minecraft\libraries" -Recurse -Filter "*.jar" | Select-Object -ExpandProperty FullName) -join ";"
$classpath = "$clientJar;$libJars"

$sources = (Get-ChildItem -Path "$ScriptDir\src\main\java" -Recurse -Filter "*.java" | Select-Object -ExpandProperty FullName)

Write-Host ">>> Compiling Java sources..." -ForegroundColor Yellow
& javac -proc:none -encoding UTF-8 -cp $classpath -d $classesDir $sources

if ($LASTEXITCODE -ne 0) {
    Write-Error "Compilation failed!"
    exit 1
}

Write-Host ">>> Copying resources..." -ForegroundColor Yellow
Copy-Item -Path "$ScriptDir\src\main\resources\*" -Destination $classesDir -Recurse -Force

Write-Host ">>> Packaging JAR..." -ForegroundColor Yellow
$prevDir = Get-Location
try {
    Set-Location $classesDir
    & jar cf $jarPath *
} finally {
    Set-Location $prevDir
}

Write-Host ">>> JAR created: $jarPath" -ForegroundColor Green

$modsDir = "$env:APPDATA\.minecraft\mods"
if (Test-Path $modsDir) {
    $targetJar = Join-Path $modsDir "DropCleaner-1.21.1-1.0.0.jar"
    Copy-Item -Path $jarPath -Destination $targetJar -Force
    Write-Host ">>> Installed to: $targetJar" -ForegroundColor Green
}
