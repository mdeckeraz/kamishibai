#!/bin/bash

# Build and deploy to App Engine
./gradlew clean bootJar appengineDeploy
