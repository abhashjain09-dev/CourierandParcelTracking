# Courier and Parcel Tracking System - Standalone PowerShell Launcher
# Works with or without Maven installed!

$ErrorActionPreference = "Stop"

Write-Host "========================================================" -ForegroundColor Cyan
Write-Host "  Courier and Parcel Tracking System (Desktop GUI)" -ForegroundColor Cyan
Write-Host "========================================================" -ForegroundColor Cyan
Write-Host ""

# 1. Verify Java compiler & runtime
$javaCmd = Get-Command java -ErrorAction SilentlyContinue
$javacCmd = Get-Command javac -ErrorAction SilentlyContinue

if (-not $javaCmd) {
    Write-Host "[ERROR] Java runtime ('java') is not found on your system PATH!" -ForegroundColor Red
    Write-Host "Please install Java JDK (17 or 21) from: https://www.oracle.com/java/technologies/downloads/" -ForegroundColor Yellow
    exit 1
}

# 2. Check if Maven ('mvn') is installed
$mvnCmd = Get-Command mvn -ErrorAction SilentlyContinue
if ($mvnCmd) {
    Write-Host "[INFO] Apache Maven detected in PATH. Compiling and launching via Maven..." -ForegroundColor Green
    mvn clean compile exec:java -Dexec.mainClass="com.courier.Main"
    exit $LASTEXITCODE
}

# 3. Fallback: Standalone compile & run without Maven
Write-Host "[INFO] Maven ('mvn') is not on PATH." -ForegroundColor Yellow
Write-Host "[INFO] Using direct standalone Java compiler..." -ForegroundColor Green

if (-not $javacCmd) {
    Write-Host "[ERROR] Java compiler ('javac') was not found in PATH." -ForegroundColor Red
    Write-Host "Ensure your JDK's bin folder is added to your PATH environment variable." -ForegroundColor Yellow
    exit 1
}

$libDir = Join-Path $PSScriptRoot "lib"
$outDir = Join-Path $PSScriptRoot "target\classes"

if (-not (Test-Path $libDir)) { New-Item -ItemType Directory -Path $libDir | Out-Null }
if (-not (Test-Path $outDir)) { New-Item -ItemType Directory -Path $outDir | Out-Null }

$sqliteJar       = Join-Path $libDir "sqlite-jdbc-3.45.1.0.jar"
$flatlafJar      = Join-Path $libDir "flatlaf-3.4.1.jar"
$slf4jApiJar     = Join-Path $libDir "slf4j-api-2.0.12.jar"
$slf4jSimpleJar  = Join-Path $libDir "slf4j-simple-2.0.12.jar"

[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12

if (-not (Test-Path $sqliteJar)) {
    Write-Host "[INFO] Downloading SQLite JDBC driver to lib\..." -ForegroundColor Gray
    try {
        Invoke-WebRequest -Uri "https://repo1.maven.org/maven2/org/xerial/sqlite-jdbc/3.45.1.0/sqlite-jdbc-3.45.1.0.jar" -OutFile $sqliteJar -UseBasicParsing
    } catch {
        Write-Host "[WARNING] Could not download SQLite JDBC: $_" -ForegroundColor Yellow
    }
}

if (-not (Test-Path $flatlafJar)) {
    Write-Host "[INFO] Downloading FlatLaf modern theme to lib\..." -ForegroundColor Gray
    try {
        Invoke-WebRequest -Uri "https://repo1.maven.org/maven2/com/formdev/flatlaf/3.4.1/flatlaf-3.4.1.jar" -OutFile $flatlafJar -UseBasicParsing
    } catch {
        Write-Host "[WARNING] Could not download FlatLaf: $_" -ForegroundColor Yellow
    }
}

if (-not (Test-Path $slf4jApiJar)) {
    Write-Host "[INFO] Downloading SLF4J API logging library to lib\..." -ForegroundColor Gray
    try {
        Invoke-WebRequest -Uri "https://repo1.maven.org/maven2/org/slf4j/slf4j-api/2.0.12/slf4j-api-2.0.12.jar" -OutFile $slf4jApiJar -UseBasicParsing
    } catch {
        Write-Host "[WARNING] Could not download SLF4J API: $_" -ForegroundColor Yellow
    }
}

if (-not (Test-Path $slf4jSimpleJar)) {
    Write-Host "[INFO] Downloading SLF4J Simple logger to lib\..." -ForegroundColor Gray
    try {
        Invoke-WebRequest -Uri "https://repo1.maven.org/maven2/org/slf4j/slf4j-simple/2.0.12/slf4j-simple-2.0.12.jar" -OutFile $slf4jSimpleJar -UseBasicParsing
    } catch {
        Write-Host "[WARNING] Could not download SLF4J Simple: $_" -ForegroundColor Yellow
    }
}

# Collect Java sources
$sources = Get-ChildItem -Path (Join-Path $PSScriptRoot "src\main\java") -Filter "*.java" -Recurse | ForEach-Object { $_.FullName }

Write-Host "[INFO] Compiling Java source files..." -ForegroundColor Gray
$cp = "$libDir\*"
& javac -encoding UTF-8 -cp $cp -d $outDir $sources

if ($LASTEXITCODE -ne 0) {
    Write-Host "[ERROR] Compilation failed. Please check the compiler errors above." -ForegroundColor Red
    exit 1
}

Write-Host "[SUCCESS] Compilation completed successfully!" -ForegroundColor Green
Write-Host "[INFO] Launching Desktop GUI..." -ForegroundColor Cyan
Write-Host ""

& java -cp "$outDir;$libDir\*" com.courier.Main
