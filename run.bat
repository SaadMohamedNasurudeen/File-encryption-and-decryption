@echo off
setlocal

:: Set directory to the project folder
cd /d "%~dp0"

:: Check for Java executable (prefer javaw to avoid keeping a console window open)
set "JAVAW_BIN=javaw.exe"
if exist "C:\Program Files\Java\jdk-18.0.2.1\bin\javaw.exe" (
    set "JAVAW_BIN=C:\Program Files\Java\jdk-18.0.2.1\bin\javaw.exe"
) else if defined JAVA_HOME (
    if exist "%JAVA_HOME%\bin\javaw.exe" (
        set "JAVAW_BIN=%JAVA_HOME%\bin\javaw.exe"
    )
)

:: Check if JAR exists; if not, build it with Maven
set "JAR_PATH=%~dp0target\file-crypto-tool-1.0.0.jar"
if not exist "%JAR_PATH%" (
    echo Building application...
    call mvn package -DskipTests
)

:: Launch the GUI in the background without keeping a console window open
start "" "%JAVAW_BIN%" -jar "%JAR_PATH%"
exit
