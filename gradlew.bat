@rem
@rem Copyright 2015 the original author or authors.
@rem Gradle startup script for Windows.
@rem
@if "%DEBUG%" == "" @echo off
@rem Set local scope for the variables with windows NT shell
if "%OS%"=="Windows_NT" setlocal

set APP_NAME=Gradle
set WRAPPER_JAR="%APP_HOME%\gradle\wrapper\gradle-wrapper.jar"

@rem Execute Gradle
"%JAVA_HOME%\bin\java.exe" -jar %WRAPPER_JAR% %*
