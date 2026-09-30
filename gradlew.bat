@echo off
setlocal
set DUI_WRAPPER_DIR=%~dp0
if defined JAVA_HOME (set "DUI_JAVA=%JAVA_HOME%\bin\java.exe") else (set "DUI_JAVA=java.exe")
"%DUI_JAVA%" -classpath "%DUI_WRAPPER_DIR%gradle\wrapper\gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain %*
endlocal
