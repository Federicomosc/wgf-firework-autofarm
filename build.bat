@echo off
setlocal enabledelayedexpansion

echo ==========================================
echo   WGF Firework Autofarm - Build Tool
echo ==========================================
echo.

REM Verifica Java 17
java -version 2>&1 | findstr "17\." >nul
if errorlevel 1 (
    java -version 2>&1 | findstr "openjdk version \"17" >nul
    if errorlevel 1 (
        echo [ERRORE] Java 17 non trovato!
        echo Installa Java 17 da: https://adoptium.net/
        echo.
        pause
        exit /b 1
    )
)
echo [OK] Java 17 trovato.

REM Pulizia cache Gradle (FIX crash JVM)
echo.
echo [INFO] Pulizia cache Gradle in corso...
rmdir /s /q .gradle 2>nul
rmdir /s /q build 2>nul
rmdir /s /q "%USERPROFILE%\.gradle\daemon\8.5" 2>nul
echo [OK] Cache pulita.

REM Scarica Gradle 8.5 se non esiste
if not exist "gradle-8.5\bin\gradle.bat" (
    echo.
    echo [INFO] Download Gradle 8.5 in corso...
    echo Attendi 1-2 minuti...
    powershell -Command "Invoke-WebRequest -Uri 'https://services.gradle.org/distributions/gradle-8.5-bin.zip' -OutFile 'gradle.zip' -UseBasicParsing"
    if errorlevel 1 (
        echo [ERRORE] Download fallito. Controlla la connessione internet.
        pause
        exit /b 1
    )
    echo [OK] Download completato. Estrazione...
    powershell -Command "Expand-Archive -Path 'gradle.zip' -DestinationPath '.' -Force"
    del gradle.zip
    echo [OK] Gradle pronto.
)

echo.
echo [INFO] Avvio build...
echo [INFO] Se sembra bloccato, sta scaricando le librerie. Aspetta...
echo [INFO] Chiudi tutte le altre applicazioni per liberare RAM.
echo.

REM Esegui build SENZA daemon (risparmia RAM) e con log
gradle-8.5\bin\gradle.bat build --no-daemon --stacktrace > build-log.txt 2>&1

if errorlevel 1 (
    echo.
    echo [ERRORE] Build fallita!
    echo.
    echo Ho salvato il log completo in: build-log.txt
    echo.
    type build-log.txt
    echo.
    pause
    exit /b 1
)

echo.
echo ==========================================
echo   BUILD COMPLETATA CON SUCCESSO!
echo ==========================================
echo.
echo Il tuo JAR si trova in:
echo   build\libs\wgf-firework-autofarm-1.0.0.jar
echo.
echo Copia quel file in:
echo   %%appdata%%\.minecraft\mods\
echo.
pause
