@echo off
setlocal

set GIT_BASH="C:\Program Files\Git\bin\bash.exe"

if not exist %GIT_BASH% (
    echo Git Bash not found.
    pause
    exit /b 1
)

%GIT_BASH% "%~dp0autopush.sh"

pause