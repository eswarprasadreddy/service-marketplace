@echo off
setlocal
for %%i in ("%~dp0.") do set "MAVEN_PROJECTBASEDIR=%%~fi"
if not defined JAVA_HOME (
  for /f "delims=" %%i in ('where javac 2^>nul') do (
    if not defined JAVA_HOME for %%j in ("%%i") do set "JAVA_HOME=%%~dpj.."
  )
)
if defined JAVA_HOME set "PATH=%JAVA_HOME%\bin;%PATH%"
if not exist "%MAVEN_PROJECTBASEDIR%\.mvn\wrapper\maven-wrapper.jar" (
  echo Maven Wrapper files are missing. 1>&2
  exit /b 1
)
java "-Dmaven.multiModuleProjectDirectory=%MAVEN_PROJECTBASEDIR%" -classpath "%MAVEN_PROJECTBASEDIR%\.mvn\wrapper\maven-wrapper.jar" org.apache.maven.wrapper.MavenWrapperMain %*
endlocal
