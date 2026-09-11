#!/usr/bin/env sh

set -e

# The JAVA_HOME environment variable must be set to a JDK 8 installation.
export JAVA_HOME="/nix/store/5c7kl8zxmzgll7lw6c98hifqzpz4fz4v-openjdk-8u362-ga/lib/openjdk"

# We need to use the gradle wrapper from the project.
if [ -f "gradle/wrapper/gradle-wrapper.jar" ]; then
  # Add execute permission to the gradlew script
  chmod +x gradlew

  # Run the gradle wrapper
  exec "$JAVA_HOME/bin/java" -cp "gradle/wrapper/gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain "$@"
else
  echo "Error: gradle-wrapper.jar not found."
  exit 1
fi
