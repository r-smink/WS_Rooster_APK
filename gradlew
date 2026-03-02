#!/usr/bin/env sh

# Simplified Gradle wrapper script
DIR="$(dirname "$0")"
APP_BASE_NAME=`basename "$0"`
GRADLE_WRAPPER_JAR="$DIR/gradle/wrapper/gradle-wrapper.jar"
JAVA_EXE="java"

if [ ! -f "$GRADLE_WRAPPER_JAR" ]; then
  echo "Gradle wrapper JAR not found: $GRADLE_WRAPPER_JAR" >&2
  exit 1
fi

exec "$JAVA_EXE" -Dorg.gradle.appname=$APP_BASE_NAME -classpath "$GRADLE_WRAPPER_JAR" org.gradle.wrapper.GradleWrapperMain "$@"
