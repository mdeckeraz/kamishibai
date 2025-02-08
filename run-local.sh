#!/bin/bash

# Build the application
./gradlew clean build -x test

# Run with dev profile
java -jar -Dspring.profiles.active=dev build/libs/kamishibai-0.0.1-SNAPSHOT.jar
