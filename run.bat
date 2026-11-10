@echo off
setlocal enabledelayedexpansion

:: Set directory to the project folder
cd /d "%~dp0"

:: 1. Locate Java executable (prefer javaw.exe for silent GUI launch, fallback to java.exe)
set "JAVAW_BIN="

:: Check JAVA_HOME first
if defined JAVA_HOME (
    if exist "%JAVA_HOME%\bin\javaw.exe" set "JAVAW_BIN=%JAVA_HOME%\bin\javaw.exe"
    if not defined JAVAW_BIN if exist "%JAVA_HOME%\bin\java.exe" set "JAVAW_BIN=%JAVA_HOME%\bin\java.exe"
)

:: Search standard Program Files Java installations (JDK 17, 18, 21, etc.)
if not defined JAVAW_BIN (
    for /d %%D in ("C:\Program Files\Java\jdk*") do (
        if exist "%%D\bin\javaw.exe" set "JAVAW_BIN=%%D\bin\javaw.exe"
    )
)

:: Fallback to PATH
if not defined JAVAW_BIN (
    where javaw.exe >nul 2>&1
    if !errorlevel! equ 0 set "JAVAW_BIN=javaw.exe"
)
if not defined JAVAW_BIN (
    where java.exe >nul 2>&1
    if !errorlevel! equ 0 set "JAVAW_BIN=java.exe"
)

:: If Java was not found anywhere, alert the user and pause
if not defined JAVAW_BIN (
    echo ==========================================================
    echo ERROR: Java is not installed or not found in system PATH.
    echo Please install Java 17 or higher from https://adoptium.net
    echo ==========================================================
    pause
    exit /b 1
)

:: 2. Check if the compiled JAR exists
set "JAR_PATH=%~dp0target\file-crypto-tool-1.0.0.jar"
if exist "%JAR_PATH%" (
    start "" "%JAVAW_BIN%" -jar "%JAR_PATH%"
    exit /b 0
)

:: If JAR is missing, attempt to build it
echo Application package not found at:
echo   %JAR_PATH%
echo.
echo Attempting to build application...

:: Locate Maven executable
set "MVN_BIN="
where mvn >nul 2>&1
if !errorlevel! equ 0 set "MVN_BIN=mvn"

if not defined MVN_BIN (
    for /d %%M in ("C:\Program Files\apache-maven*") do (
        if exist "%%M\bin\mvn.cmd" set "MVN_BIN=%%M\bin\mvn.cmd"
    )
)

if defined MVN_BIN (
    echo Using Maven: !MVN_BIN!
    call "!MVN_BIN!" package -DskipTests
    if exist "%JAR_PATH%" (
        echo Build successful. Starting application...
        start "" "%JAVAW_BIN%" -jar "%JAR_PATH%"
        exit /b 0
    )
)

:: Fallback: Direct compilation with javac if Maven is unavailable
echo Maven not detected. Attempting direct compilation using javac...
set "JAVAC_BIN="
if defined JAVA_HOME (
    if exist "%JAVA_HOME%\bin\javac.exe" set "JAVAC_BIN=%JAVA_HOME%\bin\javac.exe"
)
if not defined JAVAC_BIN (
    for /d %%D in ("C:\Program Files\Java\jdk*") do (
        if exist "%%D\bin\javac.exe" set "JAVAC_BIN=%%D\bin\javac.exe"
    )
)
if not defined JAVAC_BIN (
    where javac.exe >nul 2>&1
    if !errorlevel! equ 0 set "JAVAC_BIN=javac.exe"
)

if defined JAVAC_BIN (
    if not exist "%~dp0target\classes" mkdir "%~dp0target\classes"
    dir /s /b "%~dp0src\main\java\*.java" > "%~dp0target\sources.txt"
    "!JAVAC_BIN!" -d "%~dp0target\classes" @"%~dp0target\sources.txt"
    del "%~dp0target\sources.txt" >nul 2>&1
    if exist "%~dp0target\classes\Main.class" (
        echo Compilation successful! Launching...
        start "" "%JAVAW_BIN%" -cp "%~dp0target\classes" Main
        exit /b 0
    )
)

echo.
echo ==========================================================
echo ERROR: Could not run or compile the application.
echo Please ensure Java JDK 17+ and Apache Maven are installed.
echo ==========================================================
pause
exit /b 1
