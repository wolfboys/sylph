@if "%DEBUG%" == "" @echo off
@rem ##########################################################################
@rem
@rem  sylph startup script for Windows
@rem
@rem ##########################################################################

cd %DIRNAME%..
call "etc/sylph/sylph-env.bat"

@rem Set local scope for the variables with windows NT shell
if "%OS%"=="Windows_NT" setlocal

set DIRNAME=%~dp0
if "%DIRNAME%" == "" set DIRNAME=.
set APP_BASE_NAME=%~n0
set APP_HOME=%DIRNAME%..

@rem Add default JVM options here. You can also use JAVA_OPTS and SYLPH_OPTS to pass JVM options to this script.
set DEFAULT_JVM_OPTS="-Dconfig=etc/sylph/sylph.properties" "-Dlog4j.file=etc/sylph/sylph-log4j.properties"

@rem Find java.exe
if defined JAVA_HOME goto findJavaFromJavaHome

set JAVA_EXE=java.exe
%JAVA_EXE% -version >NUL 2>&1
if "%ERRORLEVEL%" == "0" goto init

echo.
echo ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH.
echo.
echo Please set the JAVA_HOME variable in your environment to match the
echo location of your Java installation.

goto fail

:findJavaFromJavaHome
set JAVA_HOME=%JAVA_HOME:"=%
set JAVA_EXE=%JAVA_HOME%/bin/java.exe

if exist "%JAVA_EXE%" goto init

echo.
echo ERROR: JAVA_HOME is set to an invalid directory: %JAVA_HOME%
echo.
echo Please set the JAVA_HOME variable in your environment to match the
echo location of your Java installation.

goto fail

:init
@rem Get command-line arguments, handling Windows variants

if not "%OS%" == "Windows_NT" goto win9xME_args

:win9xME_args
@rem Slurp the command line arguments.
set CMD_LINE_ARGS=
set _SKIP=2

:win9xME_args_slurp
if "x%~1" == "x" goto execute

set CMD_LINE_ARGS=%*

:execute
@rem Setup the command line

set CLASSPATH=lib/*:

@rem Execute sylph
"%JAVA_EXE%" %DEFAULT_JVM_OPTS% %JAVA_OPTS% %SYLPH_OPTS%  -classpath lib\* com.github.harbby.sylph.main.SylphMaster %CMD_LINE_ARGS%

:end
@rem End local scope for the variables with windows NT shell
if "%ERRORLEVEL%"=="0" goto mainEnd

:fail
rem Set variable SYLPH_EXIT_CONSOLE if you need the _script_ return code instead of
rem the _cmd.exe /c_ return code!
if  not "" == "%SYLPH_EXIT_CONSOLE%" exit 1
pause
exit /b 1

:mainEnd
if "%OS%"=="Windows_NT" endlocal

:omega
