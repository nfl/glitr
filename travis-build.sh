#!/bin/bash
set -ev
BUILD_COMMAND="./gradlew assemble && ./gradlew check -i"
if [ "${TRAVIS_PULL_REQUEST}" = "false" ] && [ "${TRAVIS_BRANCH}" = "master" ]; then
    echo "Building on master"
    BUILD_COMMAND="./gradlew assemble && ./gradlew check && ./gradlew bintrayUpload -x check --info"
fi
bash -c "${BUILD_COMMAND}"
