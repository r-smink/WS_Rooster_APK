@ECHO OFF
SET DIR=%~dp0
SET APP_BASE_NAME=%~n0
SET GRADLE_WRAPPER_JAR=%DIR%\gradle\wrapper\gradle-wrapper.jar

IF NOT EXIST "%GRADLE_WRAPPER_JAR%" (
  ECHO Gradle wrapper JAR not found: %GRADLE_WRAPPER_JAR%
  EXIT /B 1
)

java -Dorg.gradle.appname=%APP_BASE_NAME% -classpath "%GRADLE_WRAPPER_JAR%" org.gradle.wrapper.GradleWrapperMain %*
